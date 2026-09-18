package com.careld.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.careld.auth.dto.SmsConfigRequest;
import com.careld.auth.dto.SmsConfigResponse;
import com.careld.auth.entity.SysConfig;
import com.careld.auth.mapper.SysConfigMapper;
import com.careld.common.exception.BusinessException;
import com.careld.common.security.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 短信服务配置：读写 sys_config，并向发送逻辑提供解密后的运行时配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsConfigService {

    public static final String KEY_VERIFY_ENABLED = "sms.verify.enabled";
    public static final String KEY_ACCESS_KEY_ID = "sms.aliyun.accessKeyId";
    public static final String KEY_ACCESS_KEY_SECRET = "sms.aliyun.accessKeySecret";
    public static final String KEY_SIGN_NAME = "sms.aliyun.signName";
    public static final String KEY_TEMPLATE_CODE = "sms.aliyun.templateCode";
    public static final String KEY_TEMPLATE_PARAM = "sms.aliyun.templateParam";
    public static final String KEY_NOTICE_ENABLED = "notice.enableSms";
    public static final String KEY_NOTICE_REMINDER_HOURS = "notice.appointmentReminderHours";

    private static final String DEFAULT_TEMPLATE_PARAM = "code";
    private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final SysConfigMapper sysConfigMapper;

    @Value("${encryption.key:careld-vision-encrypt-key-32byte}")
    private String encryptionKey;

    /** 未启用真实发送时是否允许 mock 验证码（生产环境设为 false） */
    @Value("${careld.sms.mock:true}")
    private boolean mockFallback;

    public SmsConfigResponse getConfig() {
        SmsConfigResponse response = new SmsConfigResponse();
        response.setEnabled(Boolean.parseBoolean(value(KEY_VERIFY_ENABLED, "false")));
        response.setAccessKeyId(value(KEY_ACCESS_KEY_ID, ""));

        String cipher = value(KEY_ACCESS_KEY_SECRET, "");
        String secret = isNotBlank(cipher) ? decryptQuietly(cipher) : "";
        response.setAccessKeySecretConfigured(isNotBlank(cipher));
        response.setAccessKeySecretMasked(secret == null ? "******" : mask(secret));

        response.setSignName(value(KEY_SIGN_NAME, ""));
        response.setTemplateCode(value(KEY_TEMPLATE_CODE, ""));
        response.setTemplateParam(value(KEY_TEMPLATE_PARAM, DEFAULT_TEMPLATE_PARAM));
        response.setMockFallback(mockFallback);
        response.setEnableNotice(Boolean.parseBoolean(value(KEY_NOTICE_ENABLED, "true")));
        response.setAppointmentReminderHours(parseInt(value(KEY_NOTICE_REMINDER_HOURS, "2"), 2));
        return response;
    }

    public void saveConfig(SmsConfigRequest request) {
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        String accessKeyId = trim(request.getAccessKeyId());
        String signName = trim(request.getSignName());
        String templateCode = trim(request.getTemplateCode());
        String templateParam = isNotBlank(request.getTemplateParam())
                ? request.getTemplateParam().trim() : DEFAULT_TEMPLATE_PARAM;

        if (!TEMPLATE_PARAM_PATTERN.matcher(templateParam).matches()) {
            throw new BusinessException(1005, "模板变量名只能包含字母、数字、下划线，且不能以数字开头");
        }
        if (enabled) {
            boolean secretReady = isNotBlank(request.getAccessKeySecret())
                    || isNotBlank(value(KEY_ACCESS_KEY_SECRET, ""));
            if (!isNotBlank(accessKeyId) || !secretReady || !isNotBlank(signName) || !isNotBlank(templateCode)) {
                throw new BusinessException(1005, "启用真实发送前请先填写完整的阿里云短信配置（AccessKey ID/Secret、签名、模板 Code）");
            }
        }

        saveValue(KEY_VERIFY_ENABLED, String.valueOf(enabled));
        saveValue(KEY_ACCESS_KEY_ID, accessKeyId);
        // 留空表示不修改已保存的 Secret
        if (isNotBlank(request.getAccessKeySecret())) {
            saveValue(KEY_ACCESS_KEY_SECRET, AesUtil.encrypt(request.getAccessKeySecret().trim(), encryptionKey));
        }
        saveValue(KEY_SIGN_NAME, signName);
        saveValue(KEY_TEMPLATE_CODE, templateCode);
        saveValue(KEY_TEMPLATE_PARAM, templateParam);
        if (request.getEnableNotice() != null) {
            saveValue(KEY_NOTICE_ENABLED, String.valueOf(request.getEnableNotice()));
        }
        if (request.getAppointmentReminderHours() != null) {
            saveValue(KEY_NOTICE_REMINDER_HOURS, String.valueOf(request.getAppointmentReminderHours()));
        }
    }

    /** 发送验证码时读取运行时配置（Secret 已解密） */
    public SmsRuntimeConfig loadRuntime() {
        String secret = decryptQuietly(value(KEY_ACCESS_KEY_SECRET, ""));
        return new SmsRuntimeConfig(
                Boolean.parseBoolean(value(KEY_VERIFY_ENABLED, "false")),
                value(KEY_ACCESS_KEY_ID, ""),
                secret == null ? "" : secret,
                value(KEY_SIGN_NAME, ""),
                value(KEY_TEMPLATE_CODE, ""),
                value(KEY_TEMPLATE_PARAM, DEFAULT_TEMPLATE_PARAM));
    }

    public boolean isMockFallback() {
        return mockFallback;
    }

    private void saveValue(String key, String value) {
        SysConfig existing = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        SysConfig config = existing == null ? new SysConfig() : existing;
        config.setConfigKey(key);
        config.setConfigValue(value == null ? "" : value);
        if (existing == null) {
            config.setConfigGroup(key.startsWith("notice.") ? "notice" : "sms");
            sysConfigMapper.insert(config);
        } else {
            sysConfigMapper.updateById(config);
        }
    }

    private String value(String key, String defaultValue) {
        String value = sysConfigMapper.selectValue(key);
        return value == null ? defaultValue : value;
    }

    private String decryptQuietly(String cipher) {
        try {
            return AesUtil.decrypt(cipher, encryptionKey);
        } catch (Exception e) {
            log.warn("短信 AccessKeySecret 解密失败，可能是加密密钥已变更: {}", e.getMessage());
            return null;
        }
    }

    private static String mask(String secret) {
        if (!isNotBlank(secret)) return "";
        if (secret.length() <= 4) return "****";
        return "****" + secret.substring(secret.length() - 4);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 运行时短信配置 */
    public record SmsRuntimeConfig(boolean enabled, String accessKeyId, String accessKeySecret,
                                   String signName, String templateCode, String templateParam) {

        public boolean usable() {
            return isNotBlank(accessKeyId) && isNotBlank(accessKeySecret)
                    && isNotBlank(signName) && isNotBlank(templateCode);
        }
    }
}
