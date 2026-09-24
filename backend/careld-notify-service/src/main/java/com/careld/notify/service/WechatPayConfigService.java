package com.careld.notify.service;

import com.careld.common.exception.BusinessException;
import com.careld.common.security.AesUtil;
import com.careld.notify.dto.PayConfigRequest;
import com.careld.notify.dto.PayConfigVO;
import com.careld.notify.mapper.NotifySourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信支付（Native 扫码）配置：总部在管理后台维护，存于 sys_config
 *
 * <p>所需材料：商户号 mchId、APIv3 密钥、商户 API 证书序列号、apiclient_key.pem 私钥文件
 * （上传到服务器后填路径）、微信支付公钥文件（pub_key.pem，商户平台「API安全」下载）与其公钥 ID、
 * AppID（须与商户号完成绑定）、HTTPS 回调地址 notifyUrl。
 * 全部就绪后医院端「通知服务 → 续费」即为真实微信扫码支付。</p>
 *
 * <p>回调验签支持两种模式：配置了微信支付公钥即按公钥模式验签（新商户只有公钥、无平台证书），
 * 未配置则回退下载微信平台证书。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPayConfigService {

    public static final String KEY_PAY_ENABLED = "wechat.pay.enabled";
    public static final String KEY_PAY_MCH_ID = "wechat.pay.mchId";
    public static final String KEY_PAY_API_V3_KEY = "wechat.pay.apiV3Key";
    public static final String KEY_PAY_SERIAL_NO = "wechat.pay.serialNo";
    public static final String KEY_PAY_PRIVATE_KEY_PATH = "wechat.pay.privateKeyPath";
    public static final String KEY_PAY_PUBLIC_KEY_PATH = "wechat.pay.publicKeyPath";
    public static final String KEY_PAY_PUBLIC_KEY_ID = "wechat.pay.publicKeyId";
    public static final String KEY_PAY_APP_ID = "wechat.pay.appId";
    public static final String KEY_PAY_NOTIFY_URL = "wechat.pay.notifyUrl";
    public static final String KEY_PAY_MOCK_ENABLED = "wechat.pay.mockEnabled";

    private static final String GROUP = "wechat_pay";
    /** 支付结果回调路径（对外需 HTTPS，由 nginx 转发到 notify-service） */
    public static final String NOTIFY_PATH = "/api/v1/notify/pay/callback/wechat";

    private final NotifySourceMapper sourceMapper;
    private final WechatBindService bindService;

    @Value("${encryption.key:careld-vision-encrypt-key-32byte}")
    private String encryptionKey;

    /** 支付运行配置（APIv3 密钥解密后返回） */
    public PayRuntime loadRuntime() {
        String appId = value(KEY_PAY_APP_ID, "");
        if (!StringUtils.hasText(appId)) {
            // 未单独配置支付 AppID 时复用公众号 AppID（通常与商户号同主体并已绑定）
            appId = value(NotifyChannelRuntimeService.KEY_WECHAT_APP_ID, "");
        }
        return new PayRuntime(
                Boolean.parseBoolean(value(KEY_PAY_ENABLED, "false")),
                value(KEY_PAY_MCH_ID, ""),
                decryptQuietly(value(KEY_PAY_API_V3_KEY, "")),
                value(KEY_PAY_SERIAL_NO, ""),
                value(KEY_PAY_PRIVATE_KEY_PATH, ""),
                appId,
                value(KEY_PAY_NOTIFY_URL, ""),
                Boolean.parseBoolean(value(KEY_PAY_MOCK_ENABLED, "false")),
                value(KEY_PAY_PUBLIC_KEY_PATH, ""),
                value(KEY_PAY_PUBLIC_KEY_ID, ""));
    }

    public PayConfigVO getConfig() {
        PayRuntime runtime = loadRuntime();
        PayConfigVO vo = new PayConfigVO();
        vo.setEnabled(runtime.enabled());
        vo.setMchId(runtime.mchId());
        String cipher = value(KEY_PAY_API_V3_KEY, "");
        vo.setApiV3KeyConfigured(StringUtils.hasText(cipher));
        vo.setApiV3KeyMasked(StringUtils.hasText(cipher) ? mask(runtime.apiV3Key()) : "");
        vo.setSerialNo(runtime.serialNo());
        vo.setPrivateKeyPath(runtime.privateKeyPath());
        vo.setPublicKeyPath(runtime.publicKeyPath());
        vo.setPublicKeyId(runtime.publicKeyId());
        vo.setAppId(runtime.appId());
        vo.setNotifyUrl(runtime.notifyUrl());
        vo.setSuggestedNotifyUrl(suggestedNotifyUrl());
        vo.setMockEnabled(runtime.mockEnabled());
        vo.setRealReady(runtime.realReady());
        vo.setNotReadyReason(notReadyReason(runtime));
        return vo;
    }

    public void saveConfig(PayConfigRequest request) {
        save(KEY_PAY_ENABLED, String.valueOf(Boolean.TRUE.equals(request.getEnabled())));
        save(KEY_PAY_MCH_ID, trim(request.getMchId()));
        // 留空表示不修改已保存的 APIv3 密钥
        if (StringUtils.hasText(request.getApiV3Key())) {
            String key = request.getApiV3Key().trim();
            if (key.length() != 32) {
                throw new BusinessException(1005, "APIv3 密钥为 32 位字符，请核对商户平台「APIv3 密钥」");
            }
            save(KEY_PAY_API_V3_KEY, AesUtil.encrypt(key, encryptionKey));
        }
        save(KEY_PAY_SERIAL_NO, trim(request.getSerialNo()));
        save(KEY_PAY_PRIVATE_KEY_PATH, trim(request.getPrivateKeyPath()));
        save(KEY_PAY_PUBLIC_KEY_PATH, trim(request.getPublicKeyPath()));
        save(KEY_PAY_PUBLIC_KEY_ID, trim(request.getPublicKeyId()));
        save(KEY_PAY_APP_ID, trim(request.getAppId()));
        save(KEY_PAY_NOTIFY_URL, trim(request.getNotifyUrl()));
        save(KEY_PAY_MOCK_ENABLED, String.valueOf(Boolean.TRUE.equals(request.getMockEnabled())));
    }

    /** 回调地址建议值：由公众平台回调域名前缀拼装（与公众号回调同域） */
    public String suggestedNotifyUrl() {
        String base = bindService.publicBaseUrl();
        if (!StringUtils.hasText(base)) {
            return "";
        }
        return base.replaceAll("/+$", "") + NOTIFY_PATH;
    }

    /** 未就绪原因（配置页提示用） */
    private String notReadyReason(PayRuntime runtime) {
        if (!runtime.enabled()) {
            return "未启用真实支付";
        }
        if (!StringUtils.hasText(runtime.mchId())) {
            return "未填写微信支付商户号";
        }
        if (!StringUtils.hasText(runtime.apiV3Key())) {
            return "未填写 APIv3 密钥";
        }
        if (!StringUtils.hasText(runtime.serialNo())) {
            return "未填写商户 API 证书序列号";
        }
        if (!StringUtils.hasText(runtime.privateKeyPath())) {
            return "未配置商户 API 私钥（apiclient_key.pem）路径";
        }
        if (!StringUtils.hasText(runtime.publicKeyPath())) {
            return "未配置微信支付公钥（商户平台-API安全下载 pub_key.pem 后填路径）";
        }
        if (!StringUtils.hasText(runtime.appId())) {
            return "未配置 AppID（可在公众号配置中填写，或单独为此配置）";
        }
        if (!StringUtils.hasText(runtime.notifyUrl())) {
            return "未配置 HTTPS 支付回调地址";
        }
        return "";
    }

    private void save(String key, String value) {
        sourceMapper.upsertConfig(key, value == null ? "" : value, GROUP, null);
    }

    private String value(String key, String defaultValue) {
        try {
            String value = sourceMapper.selectConfigValue(key);
            return StringUtils.hasText(value) ? value : defaultValue;
        } catch (Exception e) {
            log.warn("读取系统配置失败 key={}: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    private String decryptQuietly(String cipher) {
        if (!StringUtils.hasText(cipher)) {
            return "";
        }
        try {
            return AesUtil.decrypt(cipher, encryptionKey);
        } catch (Exception e) {
            log.warn("微信支付凭据解密失败，按未配置处理: {}", e.getMessage());
            return "";
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String mask(String secret) {
        if (!StringUtils.hasText(secret)) {
            return "";
        }
        if (secret.length() <= 4) {
            return "****";
        }
        return "****" + secret.substring(secret.length() - 4);
    }

    /**
     * 微信支付运行配置
     *
     * @param apiV3Key      32 位 APIv3 密钥（用于回调解密与平台证书下载）
     * @param publicKeyPath 微信支付公钥文件路径（pub_key.pem，公钥模式验签用；为空则回退平台证书）
     * @param publicKeyId   微信支付公钥 ID（形如 PUB_KEY_ID_…，用于与回调头 Wechatpay-Serial 比对）
     */
    public record PayRuntime(boolean enabled, String mchId, String apiV3Key, String serialNo,
                             String privateKeyPath, String appId, String notifyUrl, boolean mockEnabled,
                             String publicKeyPath, String publicKeyId) {

        /** 真实支付条件是否齐备（私钥/公钥文件可读性在调用时校验） */
        public boolean realReady() {
            return enabled && StringUtils.hasText(mchId) && StringUtils.hasText(apiV3Key)
                    && StringUtils.hasText(serialNo) && StringUtils.hasText(privateKeyPath)
                    && StringUtils.hasText(appId) && StringUtils.hasText(notifyUrl)
                    && StringUtils.hasText(publicKeyPath);
        }
    }
}
