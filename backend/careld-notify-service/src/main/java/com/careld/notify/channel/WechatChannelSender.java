package com.careld.notify.channel;

import com.alibaba.fastjson2.JSON;
import com.careld.notify.service.NotifyChannelRuntimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信模板消息通道发送
 *
 * <p>未配置公众号凭据时按「模拟通道」返回成功（调用方备注「模拟通道」）；
 * access_token 内存缓存，避免每次发送都换取。公众号模板消息与订阅通知的
 * 发送接口不同（见 {@code wechat.mp.templateType}），字段名由公众号模板自身决定
 * （见 {@code wechat.mp.fields}），联调期由 {@link #rawSend} 回传微信原始响应便于排错。</p>
 */
@Slf4j
@Component
public class WechatChannelSender {

    public static final String TYPE_TEMPLATE = "template";
    public static final String TYPE_SUBSCRIBE = "subscribe";

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    /** 模板消息发送 */
    private static final String TEMPLATE_SEND_URL =
            "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";
    /** 订阅通知发送 */
    private static final String SUBSCRIBE_SEND_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/bizsend?access_token=%s";
    /** 公众号私有模板列表（模板消息） */
    private static final String PRIVATE_TEMPLATE_URL =
            "https://api.weixin.qq.com/cgi-bin/template/get_all_private_template?access_token=%s";
    /** 公众号订阅通知模板列表 */
    private static final String SUBSCRIBE_TEMPLATE_URL =
            "https://api.weixin.qq.com/wxaapi/newtmpl/gettemplate?access_token=%s";
    /** 关注者 openid 列表（联调期取真实 openid 做测试发送） */
    private static final String USER_GET_URL =
            "https://api.weixin.qq.com/cgi-bin/user/get?access_token=%s";
    /** 创建带参二维码（关注即带 scene，用于扫码绑定 openid） */
    private static final String QRCODE_CREATE_URL =
            "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=%s";
    /** 带参二维码图片（ticket 换图，家长端直接 <img> 加载） */
    private static final String QRCODE_SHOW_URL = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";
    /** JS-SDK 临时票据（订阅授权页 wx.config 签名用） */
    private static final String JSAPI_TICKET_URL =
            "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=%s&type=jsapi";
    /** 网页授权：code 换 openid（snsapi_base 静默授权） */
    private static final String OAUTH_ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
    /** 网页授权入口（授权后带 code 跳回 redirect_uri，需 redirect_uri 域名已在公众平台配置） */
    private static final String OAUTH_AUTHORIZE_URL =
            "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s"
                    + "&response_type=code&scope=snsapi_base&state=%s#wechat_redirect";

    private final RestTemplate restTemplate;

    private volatile String cachedToken;
    private volatile long tokenExpireAt;
    private volatile String cachedTicket;
    private volatile long ticketExpireAt;

    public WechatChannelSender() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    public SendOutcome send(String openid, TemplateValues values, NotifyChannelRuntimeService.WechatRuntime runtime) {
        if (!runtime.realSendReady()) {
            log.info("[WECHAT][MOCK] openid={} title={}", tail(openid), values.title());
            return SendOutcome.success(NotifyChannelRuntimeService.MOCK_REMARK);
        }
        Map<String, Object> response = sendRaw(openid, values, runtime);
        Object errcode = response.get("errcode");
        if (errcode != null && !"0".equals(String.valueOf(errcode))) {
            String errmsg = String.valueOf(response.get("errmsg"));
            log.error("[WECHAT] 模板消息发送失败 openid={} errcode={} errmsg={}", tail(openid), errcode, errmsg);
            return SendOutcome.failure("微信模板消息发送失败：" + errcode + " " + errmsg);
        }
        log.info("[WECHAT] 模板消息发送成功 openid={} template={}", tail(openid), runtime.templateId());
        return SendOutcome.success("微信公众号");
    }

    /** 真实发送并返回微信原始响应（联调期用于查看 errcode/errmsg，业务链路失败时也走到这里） */
    public Map<String, Object> sendRaw(String openid, TemplateValues values,
                                       NotifyChannelRuntimeService.WechatRuntime runtime) {
        Map<String, Object> tokenResponse = ensureToken(runtime, false);
        String token = tokenResponse.get("access_token") == null ? null : tokenResponse.get("access_token").toString();
        if (token == null) {
            // 取 token 阶段就失败（如 IP 白名单 40164），原样回传便于定位
            tokenResponse.put("_stage", "get_access_token");
            return tokenResponse;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("touser", openid);
            body.put("template_id", runtime.templateId());
            body.put("data", templateData(values, runtime));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    String.format(sendUrl(runtime), token),
                    new HttpEntity<>(JSON.toJSONString(body), headers),
                    Map.class);
            Map<String, Object> result = response == null ? new LinkedHashMap<>() : new LinkedHashMap<>(response);
            result.put("_stage", "send");
            result.put("_api", TYPE_SUBSCRIBE.equalsIgnoreCase(runtime.templateType()) ? "subscribe/bizsend" : "template/send");
            result.put("_requestBody", JSON.toJSONString(body));
            return result;
        } catch (Exception e) {
            log.error("[WECHAT] 模板消息发送异常 openid={}", tail(openid), e);
            Map<String, Object> failure = new LinkedHashMap<>();
            failure.put("errcode", -1);
            failure.put("errmsg", "调用微信接口异常：" + e.getMessage());
            failure.put("_stage", "send");
            return failure;
        }
    }

    /**
     * 模板数据（一条通知的全部可用取值）
     *
     * @param title     通知类型（如「预约成功」）
     * @param childName 儿童姓名（脱敏）
     * @param content   通知正文
     * @param storeName 医院名称
     * @param timeText  业务时间（预约/养护时间，如「2026年9月25日 10:00」）
     * @param statusText 状态短语（如「已预约」，≤5 个汉字）
     */
    public record TemplateValues(String title, String childName, String content,
                                 String storeName, String timeText, String statusText) {
    }

    /** 按「字段语义」组装 data：字段名与语义一一对应（见 wechat.mp.fields / wechat.mp.fieldRoles） */
    private Map<String, Object> templateData(TemplateValues values, NotifyChannelRuntimeService.WechatRuntime runtime) {
        List<String> fields = runtime.fieldList();
        List<String> roles = runtime.roleList();
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            String name = fields.get(i);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            String role = i < roles.size() ? roles.get(i) : "summary";
            data.put(name.trim(), value(valueForRole(role, values)));
        }
        return data;
    }

    private String valueForRole(String role, TemplateValues values) {
        return switch (role == null ? "" : role.trim()) {
            case "childName" -> values.childName() == null ? "" : values.childName();
            case "noticeTitle" -> values.title() == null ? "" : values.title();
            case "noticeTime" -> StringUtils.hasText(values.timeText()) ? values.timeText() : today();
            case "noticeStatus" -> StringUtils.hasText(values.statusText()) ? values.statusText() : "已办理";
            case "storeName" -> values.storeName() == null ? "" : values.storeName();
            case "remark" -> "来自" + (values.storeName() == null ? "" : values.storeName()) + "，如需帮助请联系医院。";
            default -> values.content() == null ? "" : values.content();
        };
    }

    private String today() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日"));
    }

    private String sendUrl(NotifyChannelRuntimeService.WechatRuntime runtime) {
        return TYPE_SUBSCRIBE.equalsIgnoreCase(runtime.templateType()) ? SUBSCRIBE_SEND_URL : TEMPLATE_SEND_URL;
    }

    /** 换取 access_token（返回微信原始响应，供探针展示 errcode/errmsg） */
    public Map<String, Object> rawToken(String appId, String appSecret) {
        return get(String.format(TOKEN_URL, encode(appId), encode(appSecret)));
    }

    /** 公众号私有模板列表（模板消息） */
    public Map<String, Object> rawPrivateTemplates(String token) {
        return get(String.format(PRIVATE_TEMPLATE_URL, token));
    }

    /** 公众号订阅通知模板列表 */
    public Map<String, Object> rawSubscribeTemplates(String token) {
        return get(String.format(SUBSCRIBE_TEMPLATE_URL, token));
    }

    /** 关注者 openid 列表（返回微信原始响应，含 data.openid / total） */
    public Map<String, Object> rawFollowers(String token) {
        return get(String.format(USER_GET_URL, encode(token)));
    }

    /** 创建带参二维码：scene 走扫码绑定，返回微信原始响应（含 ticket） */
    public Map<String, Object> rawCreateQrCode(String token, String scene, int expireSeconds) {
        try {
            Map<String, Object> sceneInfo = new LinkedHashMap<>();
            sceneInfo.put("scene_str", scene);
            Map<String, Object> actionInfo = new LinkedHashMap<>();
            actionInfo.put("scene", sceneInfo);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("expire_seconds", expireSeconds);
            body.put("action_name", "QR_STR_SCENE");
            body.put("action_info", actionInfo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    String.format(QRCODE_CREATE_URL, token),
                    new HttpEntity<>(JSON.toJSONString(body), headers),
                    Map.class);
            return response == null ? new LinkedHashMap<>() : new LinkedHashMap<>(response);
        } catch (Exception e) {
            log.warn("[WECHAT] 创建带参二维码失败 scene={}: {}", scene, e.getMessage());
            Map<String, Object> failure = new LinkedHashMap<>();
            failure.put("errcode", -1);
            failure.put("errmsg", "创建带参二维码异常：" + e.getMessage());
            return failure;
        }
    }

    /** ticket 换二维码图片地址（家长端直接加载该地址） */
    public static String qrImageUrl(String ticket) {
        return QRCODE_SHOW_URL + encode(ticket);
    }

    /** 网页授权入口 URL：用户在微信内打开后，微信带 code 跳回 redirectUri */
    public static String oauthAuthorizeUrl(String appId, String redirectUri, String state) {
        return String.format(OAUTH_AUTHORIZE_URL, encode(appId), encode(redirectUri), encode(state));
    }

    /** code 换 openid（返回微信原始响应，含 openid / errcode / errmsg） */
    public Map<String, Object> rawOauthAccessToken(String appId, String appSecret, String code) {
        return get(String.format(OAUTH_ACCESS_TOKEN_URL, encode(appId), encode(appSecret), encode(code)));
    }

    /** JS-SDK 配置签名：sha1(jsapi_ticket=&noncestr=&timestamp=&url=)，url 为当前页面地址（不含 # 及其后） */
    public static String jsapiSignature(String ticket, String nonceStr, long timestamp, String url) {
        String raw = "jsapi_ticket=" + ticket + "&noncestr=" + nonceStr
                + "&timestamp=" + timestamp + "&url=" + url;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("JS-SDK 签名计算失败：" + e.getMessage(), e);
        }
    }

    /** jsapi_ticket（内存缓存，微信侧 7200 秒有效；取不到返回 null） */
    public synchronized String jsapiTicket(NotifyChannelRuntimeService.WechatRuntime runtime) {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedTicket) && now < ticketExpireAt) {
            return cachedTicket;
        }
        Map<String, Object> tokenResponse = ensureToken(runtime, false);
        Object token = tokenResponse.get("access_token");
        if (token == null) {
            log.warn("[WECHAT] 取 jsapi_ticket 失败：access_token 不可用 errcode={}", tokenResponse.get("errcode"));
            return null;
        }
        Map<String, Object> response = get(String.format(JSAPI_TICKET_URL, token));
        Object ticket = response.get("ticket");
        if (ticket == null) {
            log.warn("[WECHAT] 取 jsapi_ticket 失败 errcode={} errmsg={}", response.get("errcode"), response.get("errmsg"));
            return null;
        }
        cachedTicket = ticket.toString();
        Object expiresIn = response.get("expires_in");
        long ttlSeconds = expiresIn == null ? 7200L : Long.parseLong(expiresIn.toString());
        ticketExpireAt = now + Math.max(60L, ttlSeconds - 300L) * 1000L;
        return cachedTicket;
    }

    /** 取 access_token（返回微信原始响应，业务链路复用缓存） */
    public Map<String, Object> token(NotifyChannelRuntimeService.WechatRuntime runtime) {
        return ensureToken(runtime, false);
    }

    /** 从模板 content（{{first.DATA}} 形式）解析字段名，顺序即模板展示顺序 */
    public static List<String> parseFieldNames(String content) {
        List<String> names = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            return names;
        }
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\.DATA\\s*}}").matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String url) {
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response == null ? new LinkedHashMap<>() : new LinkedHashMap<>(response);
        } catch (Exception e) {
            log.warn("[WECHAT] 调用微信接口失败 url={}: {}", url.replaceAll("secret=[^&]+", "secret=***"), e.getMessage());
            Map<String, Object> failure = new LinkedHashMap<>();
            failure.put("errcode", -1);
            failure.put("errmsg", "调用微信接口异常：" + e.getMessage());
            return failure;
        }
    }

    private synchronized Map<String, Object> ensureToken(NotifyChannelRuntimeService.WechatRuntime runtime, boolean forceRefresh) {
        long now = System.currentTimeMillis();
        if (!forceRefresh && StringUtils.hasText(cachedToken) && now < tokenExpireAt) {
            Map<String, Object> cached = new LinkedHashMap<>();
            cached.put("access_token", cachedToken);
            cached.put("_cached", true);
            return cached;
        }
        Map<String, Object> response = rawToken(runtime.appId(), runtime.appSecret());
        Object token = response.get("access_token");
        if (token != null) {
            cachedToken = token.toString();
            Object expiresIn = response.get("expires_in");
            long ttlSeconds = expiresIn == null ? 7200L : Long.parseLong(expiresIn.toString());
            tokenExpireAt = now + Math.max(60L, ttlSeconds - 300L) * 1000L;
        }
        return response;
    }

    private Map<String, String> value(String text) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("value", text == null ? "" : text);
        return item;
    }

    private static String encode(String text) {
        return URLEncoder.encode(text == null ? "" : text, StandardCharsets.UTF_8);
    }

    private String tail(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "-";
        }
        return openid.length() <= 6 ? openid : openid.substring(openid.length() - 6);
    }
}
