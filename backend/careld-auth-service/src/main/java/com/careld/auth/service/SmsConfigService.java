package com.careld.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.careld.auth.dto.SmsConfigRequest;
import com.careld.auth.dto.SmsConfigResponse;
import com.careld.auth.entity.SysConfig;
import com.careld.auth.mapper.SysConfigMapper;
import com.careld.common.exception.BusinessException;
import com.careld.common.notify.NotifyEventTypes;
import com.careld.common.security.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
    public static final String KEY_NOTICE_TEMPLATE_CODE = "sms.aliyun.noticeTemplateCode";
    public static final String KEY_NOTICE_TEMPLATE_PARAM = "sms.aliyun.noticeTemplateParam";
    public static final String KEY_NOTICE_TEMPLATE_FIELDS = "sms.aliyun.noticeTemplateFields";
    public static final String KEY_NOTICE_TEMPLATE_ROLES = "sms.aliyun.noticeTemplateRoles";
    public static final String KEY_NOTICE_TEMPLATE_EVENTS = "sms.aliyun.noticeTemplateEvents";
    /** 单个事件的独立短信模板键前缀（后接事件类型与 .code/.fields/.roles） */
    public static final String KEY_EVENT_TEMPLATE_PREFIX = "sms.aliyun.eventTemplate.";
    public static final String KEY_NOTICE_ENABLED = "notice.enableSms";
    /** 养护提醒提前量（分钟）：预约开始前多久再次发送预约成功短信 */
    public static final String KEY_CARE_REMINDER_MINUTES = "notice.careReminderMinutes";

    private static final String DEFAULT_TEMPLATE_PARAM = "code";
    private static final String DEFAULT_NOTICE_TEMPLATE_PARAM = "content";
    private static final int DEFAULT_CARE_REMINDER_MINUTES = 90;
    private static final int MIN_CARE_REMINDER_MINUTES = 5;
    private static final int MAX_CARE_REMINDER_MINUTES = 1440;
    private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    /** 业务通知事件类型（与 notify-service NotifyEventTypes 一致） */
    private static final Set<String> NOTICE_EVENT_TYPES = Set.of(
            "child_created", "reserve_created", "reserve_cancelled", "reserve_adjusted",
            "care_reminder", "care_completed");
    /** 需要独立模板的事件（各自文案不同；预约成功由主模板承载、养护提醒复用主模板） */
    private static final List<String> EVENT_TEMPLATE_EVENTS = List.of(
            "reserve_cancelled", "reserve_adjusted", "child_created", "care_completed");
    /** 变量语义可选值（与 notify-service NotifyChannelRuntimeService.SMS_ROLE_NAMES 一致） */
    private static final List<String> NOTICE_ROLE_NAMES = List.of(
            "content", "storeName", "childName", "noticeTitle", "noticeTime", "noticeStatus",
            "reserveDate", "timeRange", "timeStart", "timeEnd",
            "reason", "oldReserveDate", "oldTimeRange");

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
        response.setNoticeTemplateCode(value(KEY_NOTICE_TEMPLATE_CODE, ""));
        response.setNoticeTemplateParam(value(KEY_NOTICE_TEMPLATE_PARAM, DEFAULT_NOTICE_TEMPLATE_PARAM));
        response.setNoticeTemplateFields(value(KEY_NOTICE_TEMPLATE_FIELDS, ""));
        response.setNoticeTemplateRoles(value(KEY_NOTICE_TEMPLATE_ROLES, ""));
        response.setNoticeTemplateEvents(value(KEY_NOTICE_TEMPLATE_EVENTS, ""));
        response.setEventTemplates(EVENT_TEMPLATE_EVENTS.stream().map(eventType -> {
            SmsConfigResponse.EventTemplate item = new SmsConfigResponse.EventTemplate();
            item.setEventType(eventType);
            item.setTemplateCode(value(KEY_EVENT_TEMPLATE_PREFIX + eventType + ".code", ""));
            item.setTemplateFields(value(KEY_EVENT_TEMPLATE_PREFIX + eventType + ".fields", ""));
            item.setTemplateRoles(value(KEY_EVENT_TEMPLATE_PREFIX + eventType + ".roles", ""));
            return item;
        }).toList());
        response.setMockFallback(mockFallback);
        response.setEnableNotice(Boolean.parseBoolean(value(KEY_NOTICE_ENABLED, "true")));
        response.setCareReminderMinutes(
                parseInt(value(KEY_CARE_REMINDER_MINUTES, String.valueOf(DEFAULT_CARE_REMINDER_MINUTES)),
                        DEFAULT_CARE_REMINDER_MINUTES));
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
        String noticeTemplateCode = trim(request.getNoticeTemplateCode());
        String noticeTemplateParam = isNotBlank(request.getNoticeTemplateParam())
                ? request.getNoticeTemplateParam().trim() : DEFAULT_NOTICE_TEMPLATE_PARAM;
        if (!TEMPLATE_PARAM_PATTERN.matcher(noticeTemplateParam).matches()) {
            throw new BusinessException(1005, "通知模板变量名只能包含字母、数字、下划线，且不能以数字开头");
        }
        String noticeTemplateFields = trim(request.getNoticeTemplateFields());
        String noticeTemplateRoles = trim(request.getNoticeTemplateRoles());
        validateMapping(noticeTemplateFields, noticeTemplateRoles, "通知模板");
        String noticeTemplateEvents = trim(request.getNoticeTemplateEvents());
        if (isNotBlank(noticeTemplateEvents)) {
            List<String> invalid = Arrays.stream(noticeTemplateEvents.split(","))
                    .map(String::trim).filter(item -> !NOTICE_EVENT_TYPES.contains(item)).toList();
            if (!invalid.isEmpty()) {
                throw new BusinessException(1005, "通知模板适用事件含未知类型：" + String.join("、", invalid)
                        + "；可选 " + String.join("/", NOTICE_EVENT_TYPES));
            }
        }
        List<SmsConfigRequest.EventTemplate> eventTemplates = request.getEventTemplates();
        if (eventTemplates != null) {
            for (SmsConfigRequest.EventTemplate item : eventTemplates) {
                String eventType = trim(item.getEventType());
                if (!EVENT_TEMPLATE_EVENTS.contains(eventType)) {
                    throw new BusinessException(1005, "不支持单独配置模板的事件：" + eventType
                            + "；可选 " + String.join("/", EVENT_TEMPLATE_EVENTS));
                }
                String eventCode = trim(item.getTemplateCode());
                String eventFields = trim(item.getTemplateFields());
                String eventRoles = trim(item.getTemplateRoles());
                validateMapping(eventFields, eventRoles, NotifyEventTypes.label(eventType));
                if (isNotBlank(eventFields) && !isNotBlank(eventCode)) {
                    throw new BusinessException(1005, NotifyEventTypes.label(eventType)
                            + "：填写变量映射前请先填写模板 Code");
                }
            }
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
        saveValue(KEY_NOTICE_TEMPLATE_CODE, noticeTemplateCode);
        saveValue(KEY_NOTICE_TEMPLATE_PARAM, noticeTemplateParam);
        saveValue(KEY_NOTICE_TEMPLATE_FIELDS, noticeTemplateFields);
        saveValue(KEY_NOTICE_TEMPLATE_ROLES, noticeTemplateRoles);
        saveValue(KEY_NOTICE_TEMPLATE_EVENTS, noticeTemplateEvents);
        if (eventTemplates != null) {
            for (SmsConfigRequest.EventTemplate item : eventTemplates) {
                String prefix = KEY_EVENT_TEMPLATE_PREFIX + trim(item.getEventType()) + ".";
                saveValue(prefix + "code", trim(item.getTemplateCode()));
                saveValue(prefix + "fields", trim(item.getTemplateFields()));
                saveValue(prefix + "roles", trim(item.getTemplateRoles()));
            }
        }
        if (request.getEnableNotice() != null) {
            saveValue(KEY_NOTICE_ENABLED, String.valueOf(request.getEnableNotice()));
        }
        if (request.getCareReminderMinutes() != null) {
            int minutes = request.getCareReminderMinutes();
            if (minutes < MIN_CARE_REMINDER_MINUTES || minutes > MAX_CARE_REMINDER_MINUTES) {
                throw new BusinessException(1005, "养护提醒提前时间需在 " + MIN_CARE_REMINDER_MINUTES
                        + " ~ " + MAX_CARE_REMINDER_MINUTES + " 分钟之间");
            }
            saveValue(KEY_CARE_REMINDER_MINUTES, String.valueOf(minutes));
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

    /**
     * 校验模板变量映射：变量名与变量含义成对出现、数量一致、语义取值合法
     *
     * @param label 出错提示中展示的模板名称（如「预约取消」）
     */
    private void validateMapping(String fields, String roles, String label) {
        if (isNotBlank(fields) != isNotBlank(roles)) {
            throw new BusinessException(1005, label + "：模板变量列表与变量含义需要成对填写");
        }
        if (!isNotBlank(fields)) {
            return;
        }
        String[] fieldArr = Arrays.stream(fields.split(",")).map(String::trim).toArray(String[]::new);
        String[] roleArr = Arrays.stream(roles.split(",")).map(String::trim).toArray(String[]::new);
        if (fieldArr.length != roleArr.length) {
            throw new BusinessException(1005, label + "：模板变量列表与变量含义数量不一致");
        }
        for (int i = 0; i < fieldArr.length; i++) {
            if (!TEMPLATE_PARAM_PATTERN.matcher(fieldArr[i]).matches()) {
                throw new BusinessException(1005, label + "：模板变量名只能包含字母、数字、下划线，且不能以数字开头（"
                        + fieldArr[i] + "）");
            }
            if (!NOTICE_ROLE_NAMES.contains(roleArr[i])) {
                throw new BusinessException(1005, label + "：变量含义含未知取值（" + roleArr[i] + "）；可选 "
                        + String.join("/", NOTICE_ROLE_NAMES));
            }
        }
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
