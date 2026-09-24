package com.careld.notify.channel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.careld.notify.service.WechatPayConfigService.PayRuntime;

/**
 * 微信支付 v3（Native 扫码）客户端
 *
 * <p>下单 / 查单走 {@code api.mch.weixin.qq.com}，请求用商户 API 私钥（apiclient_key.pem）
 * 做 SHA256-RSA2048 签名；支付结果回调先验签、再用 APIv3 密钥 AES-256-GCM 解密，全部通过才可信。
 * 验签公钥按配置择一：填了微信支付公钥（pub_key.pem）用公钥模式，否则下载微信平台证书（新商户
 * 平台已不再下发平台证书，只有微信支付公钥，此时 {@code /v3/certificates} 返回 RESOURCE_NOT_EXISTS）。
 * 平台证书内存缓存，缺失时按需拉取（验签失败一律拒绝，宁可依赖主动查单）。</p>
 */
@Slf4j
@Component
public class WechatPayClient {

    private static final String HOST = "https://api.mch.weixin.qq.com";
    private static final String NATIVE_PATH = "/v3/pay/transactions/native";
    private static final String CERT_PATH = "/v3/certificates";
    private static final String ORDER_PATH = "/v3/pay/transactions/out-trade-no/";
    /** 平台证书缓存有效期（微信按 12 小时轮换） */
    private static final long PLATFORM_KEY_TTL_MS = 6 * 3600 * 1000L;

    private final RestTemplate restTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, PublicKey> platformKeys = new ConcurrentHashMap<>();
    private final Map<String, CachedPrivateKey> privateKeyCache = new ConcurrentHashMap<>();
    private final Map<String, CachedPublicKey> publicKeyCache = new ConcurrentHashMap<>();
    private volatile long platformKeysFetchedAt;

    public WechatPayClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(factory);
        // 微信 API 的 4xx/5xx 也带 JSON 错误体，原样解析而不是抛异常
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });
    }

    // ==================== 下单 / 查单 ====================

    /** Native 下单：返回 code_url 供医院端渲染二维码 */
    public NativeOrderResult createNativeOrder(PayRuntime runtime, String orderNo, String description, int totalFen) {
        JSONObject body = new JSONObject();
        body.put("appid", runtime.appId());
        body.put("mchid", runtime.mchId());
        body.put("description", description);
        body.put("out_trade_no", orderNo);
        body.put("notify_url", runtime.notifyUrl());
        JSONObject amount = new JSONObject();
        amount.put("total", totalFen);
        amount.put("currency", "CNY");
        body.put("amount", amount);

        ApiResponse response = call(runtime, HttpMethod.POST, NATIVE_PATH, body.toJSONString());
        if (!response.ok()) {
            return new NativeOrderResult(false, null, null, response.error());
        }
        JSONObject json = response.json();
        String codeUrl = json.getString("code_url");
        if (!StringUtils.hasText(codeUrl)) {
            return new NativeOrderResult(false, null, null, "微信未返回 code_url：" + response.raw());
        }
        return new NativeOrderResult(true, codeUrl, json.getString("prepay_id"), null);
    }

    /** 查单：按商户订单号查微信侧交易状态（回调丢失时的兜底） */
    public QueryOrderResult queryOrder(PayRuntime runtime, String orderNo) {
        String path = ORDER_PATH + orderNo + "?mchid=" + runtime.mchId();
        ApiResponse response = call(runtime, HttpMethod.GET, path, "");
        if (!response.ok()) {
            return QueryOrderResult.failure(response.error());
        }
        JSONObject json = response.json();
        String tradeState = json.getString("trade_state");
        String payerOpenid = null;
        JSONObject payer = json.getJSONObject("payer");
        if (payer != null) {
            payerOpenid = payer.getString("openid");
        }
        return new QueryOrderResult(true, tradeState, json.getString("transaction_id"), payerOpenid,
                json.getString("success_time"), null);
    }

    // ==================== 回调验签与解密 ====================

    /**
     * 回调验签：用微信支付公钥（按配置的 publicKeyId 匹配回调头 serial）或微信平台证书校验签名
     *
     * @return 验签通过返回空字符串，失败返回原因
     */
    public String verifyCallback(PayRuntime runtime, String timestamp, String nonce, String signature, String serial, String body) {
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature) || !StringUtils.hasText(serial)) {
            return "回调缺少验签头";
        }
        PublicKey publicKey = resolveVerifyKey(runtime, serial);
        if (publicKey == null) {
            return "未取得微信验签公钥（serial=" + serial + "），无法验签";
        }
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature)) ? "" : "回调签名不匹配";
        } catch (Exception e) {
            return "回调验签异常：" + e.getMessage();
        }
    }

    /** 解密回调 resource（AES-256-GCM，密钥为 APIv3 密钥） */
    public String decryptResource(PayRuntime runtime, String ciphertext, String nonce, String associatedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(runtime.apiV3Key().getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("回调解密失败：" + e.getMessage(), e);
        }
    }

    // ==================== 诊断 ====================

    /** 联调诊断：私钥可读 + 验签公钥可用（公钥模式读文件，平台证书模式尝试下载） */
    public Map<String, Object> probe(PayRuntime runtime) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mchId", runtime.mchId());
        result.put("appId", runtime.appId());
        result.put("notifyUrl", runtime.notifyUrl());
        result.put("realReady", runtime.realReady());
        result.put("verifyMode", StringUtils.hasText(runtime.publicKeyPath()) ? "微信支付公钥" : "平台证书");
        try {
            loadPrivateKey(runtime.privateKeyPath());
            result.put("privateKeyOk", true);
            result.put("privateKeyMessage", "私钥读取成功");
        } catch (Exception e) {
            result.put("privateKeyOk", false);
            result.put("privateKeyMessage", e.getMessage());
        }
        if (StringUtils.hasText(runtime.publicKeyPath())) {
            result.put("publicKeyId", runtime.publicKeyId());
            try {
                loadPublicKey(runtime.publicKeyPath());
                result.put("publicKeyOk", true);
                result.put("publicKeyMessage", "微信支付公钥读取成功");
            } catch (Exception e) {
                result.put("publicKeyOk", false);
                result.put("publicKeyMessage", e.getMessage());
            }
            result.put("certificateCount", 0);
            result.put("certificateMessage", "使用微信支付公钥验签，无需下载平台证书");
            return result;
        }
        result.put("publicKeyOk", false);
        result.put("publicKeyMessage", "未配置微信支付公钥");
        if (!StringUtils.hasText(runtime.mchId()) || !StringUtils.hasText(runtime.apiV3Key())
                || !StringUtils.hasText(runtime.serialNo()) || !StringUtils.hasText(runtime.privateKeyPath())) {
            result.put("certificateCount", 0);
            result.put("certificateMessage", "凭据未配置齐备，跳过平台证书下载");
            return result;
        }
        try {
            Map<String, PublicKey> keys = fetchPlatformKeys(runtime);
            result.put("certificateCount", keys.size());
            result.put("certificateMessage", keys.isEmpty() ? "未下载到平台证书" : "平台证书下载成功");
        } catch (Exception e) {
            result.put("certificateCount", 0);
            result.put("certificateMessage", "平台证书下载失败：" + e.getMessage());
        }
        return result;
    }

    // ==================== 内部实现 ====================

    private ApiResponse call(PayRuntime runtime, HttpMethod method, String path, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "careld-vision-notify/1.0");
            headers.set("Authorization", authorization(runtime, method.name(), path, body));
            ResponseEntity<String> response = restTemplate.exchange(
                    HOST + path, method, new HttpEntity<>(StringUtils.hasText(body) ? body : null, headers), String.class);
            String raw = response.getBody() == null ? "" : response.getBody();
            if (!response.getStatusCode().is2xxSuccessful()) {
                return ApiResponse.failure("HTTP " + response.getStatusCode().value() + " " + shorten(raw));
            }
            return ApiResponse.success(raw);
        } catch (Exception e) {
            log.warn("[PAY] 微信支付接口调用失败 path={}: {}", path, e.getMessage());
            return ApiResponse.failure("调用微信支付接口失败：" + e.getMessage());
        }
    }

    /** 构造 WECHATPAY2-SHA256-RSA2048 授权头 */
    private String authorization(PayRuntime runtime, String method, String path, String body) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = randomNonce();
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(loadPrivateKey(runtime.privateKeyPath()));
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());
        return "WECHATPAY2-SHA256-RSA2048 mchid=\"" + runtime.mchId() + "\",nonce_str=\"" + nonce
                + "\",signature=\"" + signature + "\",timestamp=\"" + timestamp
                + "\",serial_no=\"" + runtime.serialNo() + "\"";
    }

    /**
     * 取回调验签公钥：配置了微信支付公钥即用公钥文件（回调头 serial 与配置的公钥 ID 比对，
     * 未配置公钥 ID 时直接采用）；否则回退下载平台证书。
     */
    private PublicKey resolveVerifyKey(PayRuntime runtime, String serial) {
        if (StringUtils.hasText(runtime.publicKeyPath())) {
            if (StringUtils.hasText(runtime.publicKeyId()) && !runtime.publicKeyId().equals(serial)) {
                log.warn("[PAY] 回调凭据ID {} 与配置的微信支付公钥ID {} 不一致，改用平台证书验签", serial, runtime.publicKeyId());
            } else {
                try {
                    return loadPublicKey(runtime.publicKeyPath());
                } catch (Exception e) {
                    log.warn("[PAY] 微信支付公钥读取失败，改用平台证书验签: {}", e.getMessage());
                }
            }
        }
        return resolvePlatformKey(runtime, serial);
    }

    /** 取平台证书公钥：优先内存缓存，缓存缺失/过期/未命中时重新下载 */
    private PublicKey resolvePlatformKey(PayRuntime runtime, String serial) {
        PublicKey cached = platformKeys.get(serial);
        boolean expired = System.currentTimeMillis() - platformKeysFetchedAt > PLATFORM_KEY_TTL_MS;
        if (cached != null && !expired) {
            return cached;
        }
        try {
            Map<String, PublicKey> keys = fetchPlatformKeys(runtime);
            return keys.get(serial);
        } catch (Exception e) {
            log.warn("[PAY] 平台证书下载失败，沿用缓存: {}", e.getMessage());
            return cached;
        }
    }

    private synchronized Map<String, PublicKey> fetchPlatformKeys(PayRuntime runtime) {
        ApiResponse response = call(runtime, HttpMethod.GET, CERT_PATH, "");
        if (!response.ok()) {
            throw new IllegalStateException(response.error());
        }
        JSONArray data = response.json().getJSONArray("data");
        Map<String, PublicKey> keys = new LinkedHashMap<>();
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                JSONObject encrypted = item.getJSONObject("encrypt_certificate");
                if (encrypted == null) {
                    continue;
                }
                try {
                    String pem = decryptResource(runtime, encrypted.getString("ciphertext"),
                            encrypted.getString("nonce"), encrypted.getString("associated_data"));
                    PublicKey publicKey = parseCertificate(pem).getPublicKey();
                    keys.put(item.getString("serial_no"), publicKey);
                } catch (Exception e) {
                    log.warn("[PAY] 解析平台证书失败 serial={}: {}", item.getString("serial_no"), e.getMessage());
                }
            }
        }
        platformKeys.clear();
        platformKeys.putAll(keys);
        platformKeysFetchedAt = System.currentTimeMillis();
        return keys;
    }

    private X509Certificate parseCertificate(String pem) throws Exception {
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        try (ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(base64))) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        if (!StringUtils.hasText(path)) {
            throw new IllegalStateException("未配置商户 API 私钥文件路径");
        }
        Path file = Path.of(path.trim());
        if (!Files.isReadable(file)) {
            throw new IllegalStateException("私钥文件不可读：" + path);
        }
        long modified = Files.getLastModifiedTime(file).toMillis();
        CachedPrivateKey cached = privateKeyCache.get(path);
        if (cached != null && cached.modified() == modified) {
            return cached.key();
        }
        String pem = Files.readString(file);
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        privateKeyCache.put(path, new CachedPrivateKey(modified, key));
        return key;
    }

    /** 读取微信支付公钥文件（pub_key.pem，X.509 SubjectPublicKeyInfo） */
    private PublicKey loadPublicKey(String path) throws Exception {
        if (!StringUtils.hasText(path)) {
            throw new IllegalStateException("未配置微信支付公钥文件路径");
        }
        Path file = Path.of(path.trim());
        if (!Files.isReadable(file)) {
            throw new IllegalStateException("微信支付公钥文件不可读：" + path);
        }
        long modified = Files.getLastModifiedTime(file).toMillis();
        CachedPublicKey cached = publicKeyCache.get(path);
        if (cached != null && cached.modified() == modified) {
            return cached.key();
        }
        String pem = Files.readString(file);
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        publicKeyCache.put(path, new CachedPublicKey(modified, key));
        return key;
    }

    private String randomNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static String shorten(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.length() > 300 ? raw.substring(0, 300) : raw;
    }

    private record CachedPrivateKey(long modified, PrivateKey key) {
    }

    private record CachedPublicKey(long modified, PublicKey key) {
    }

    private record ApiResponse(int statusCode, String raw, String error) {

        static ApiResponse success(String raw) {
            return new ApiResponse(200, raw, "");
        }

        static ApiResponse failure(String error) {
            return new ApiResponse(0, null, error);
        }

        boolean ok() {
            return statusCode == 200;
        }

        JSONObject json() {
            return JSON.parseObject(raw == null || raw.isBlank() ? "{}" : raw);
        }
    }

    /** Native 下单结果 */
    public record NativeOrderResult(boolean ok, String codeUrl, String prepayId, String error) {
    }

    /** 查单结果 */
    public record QueryOrderResult(boolean ok, String tradeState, String transactionId,
                                   String payerOpenid, String successTime, String error) {

        static QueryOrderResult failure(String error) {
            return new QueryOrderResult(false, null, null, null, null, error);
        }

        /** 微信返回的成功时间（RFC3339）转本地时间 */
        public java.time.LocalDateTime paidAt() {
            if (!StringUtils.hasText(successTime)) {
                return java.time.LocalDateTime.now();
            }
            try {
                return OffsetDateTime.parse(successTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
            } catch (Exception e) {
                return java.time.LocalDateTime.now();
            }
        }
    }
}
