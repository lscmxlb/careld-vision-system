package com.careld.notify.service;

import com.careld.common.security.AesUtil;
import com.careld.notify.mapper.NotifySourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 通知通道运行时配置（总部在管理后台维护、存于 sys_config）
 *
 * <p>短信沿用 auth-service 的阿里云凭据键，另加通知模板键（验证码模板与通知模板不同）；
 * 微信模板消息使用 wechat.mp.* 三键。凭据不完整时通道走模拟发送（业务决策：
 * 未配置凭据时模拟发送成功并计费，备注「模拟通道」；总部配上真实凭据后自动切换真实发送）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyChannelRuntimeService {

    public static final String KEY_NOTICE_SMS_ENABLED = "notice.enableSms";
    public static final String KEY_SMS_ACCESS_KEY_ID = "sms.aliyun.accessKeyId";
    public static final String KEY_SMS_ACCESS_KEY_SECRET = "sms.aliyun.accessKeySecret";
    public static final String KEY_SMS_SIGN_NAME = "sms.aliyun.signName";
    public static final String KEY_SMS_NOTICE_TEMPLATE_CODE = "sms.aliyun.noticeTemplateCode";
    public static final String KEY_SMS_NOTICE_TEMPLATE_PARAM = "sms.aliyun.noticeTemplateParam";
    public static final String KEY_SMS_NOTICE_TEMPLATE_FIELDS = "sms.aliyun.noticeTemplateFields";
    public static final String KEY_SMS_NOTICE_TEMPLATE_ROLES = "sms.aliyun.noticeTemplateRoles";
    /** 通知模板适用的事件类型（逗号分隔）；留空表示所有事件共用 */
    public static final String KEY_SMS_NOTICE_TEMPLATE_EVENTS = "sms.aliyun.noticeTemplateEvents";
    /**
     * 单个事件的独立短信模板键前缀，后接事件类型与后缀：
     * {@code sms.aliyun.eventTemplate.<event>.code|fields|roles}
     *
     * <p>各事件模板文案固定且互不相同（预约成功、预约取消、预约调整、建档、养护完成），
     * 单套主模板无法覆盖，故按事件单独配置；未配置的事件回落到主模板。</p>
     */
    public static final String KEY_SMS_EVENT_TEMPLATE_PREFIX = "sms.aliyun.eventTemplate.";
    public static final String KEY_WECHAT_APP_ID = "wechat.mp.appId";
    public static final String KEY_WECHAT_APP_SECRET = "wechat.mp.appSecret";
    public static final String KEY_WECHAT_TEMPLATE_ID = "wechat.mp.templateId";
    public static final String KEY_WECHAT_TEMPLATE_TYPE = "wechat.mp.templateType";
    public static final String KEY_WECHAT_FIELDS = "wechat.mp.fields";
    public static final String KEY_WECHAT_FIELD_ROLES = "wechat.mp.fieldRoles";
    public static final String KEY_WECHAT_ACCOUNT_NAME = "wechat.mp.accountName";

    /** 默认字段映射：摘要 / 儿童姓名 / 通知类型 / 备注 */
    public static final String DEFAULT_WECHAT_FIELDS = "first,keyword1,keyword2,remark";
    /** 默认字段语义（与字段映射一一对应）：summary摘要 childName儿童姓名 noticeTitle通知类型 remark备注 noticeTime业务时间 noticeStatus状态短语 */
    public static final String DEFAULT_WECHAT_FIELD_ROLES = "summary,childName,noticeTitle,remark";

    /** 合法字段语义取值（联调按模板内容逐位指定） */
    public static final List<String> FIELD_ROLE_NAMES = List.of(
            "summary", "childName", "noticeTitle", "noticeTime", "noticeStatus", "storeName", "remark");

    public static final List<String> DEFAULT_WECHAT_FIELD_ROLES_LIST = List.of(
            DEFAULT_WECHAT_FIELD_ROLES.split(","));

    public static final String MOCK_REMARK = "模拟通道";

    /** 通知短信模板变量可用的语义取值（配合多变量模板，与模板变量名按顺序一一对应） */
    public static final List<String> SMS_ROLE_NAMES = List.of(
            "content", "storeName", "childName", "noticeTitle", "noticeTime", "noticeStatus",
            "reserveDate", "timeRange", "timeStart", "timeEnd",
            "reason", "oldReserveDate", "oldTimeRange");

    private final NotifySourceMapper sourceMapper;

    @Value("${encryption.key:careld-vision-encrypt-key-32byte}")
    private String encryptionKey;

    /** 短信通道运行配置：开关 + 凭据 + 通知模板 */
    public SmsRuntime loadSmsRuntime() {
        boolean enabled = Boolean.parseBoolean(value(KEY_NOTICE_SMS_ENABLED, "false"));
        String accessKeyId = value(KEY_SMS_ACCESS_KEY_ID, "");
        String secret = decryptQuietly(value(KEY_SMS_ACCESS_KEY_SECRET, ""));
        String signName = value(KEY_SMS_SIGN_NAME, "");
        String templateCode = value(KEY_SMS_NOTICE_TEMPLATE_CODE, "");
        String templateParam = value(KEY_SMS_NOTICE_TEMPLATE_PARAM, "content");
        String templateFields = value(KEY_SMS_NOTICE_TEMPLATE_FIELDS, "");
        String templateRoles = value(KEY_SMS_NOTICE_TEMPLATE_ROLES, "");
        String templateEvents = value(KEY_SMS_NOTICE_TEMPLATE_EVENTS, "");
        return new SmsRuntime(enabled, accessKeyId, secret, signName, templateCode,
                templateParam, templateFields, templateRoles, templateEvents);
    }

    /**
     * 按事件取短信运行配置：该事件单独配了模板 Code 就用它，否则回落到主模板
     *
     * <p>事件模板的变量映射与主模板互不影响；主模板的「适用事件」白名单只约束回落时的那些事件。</p>
     */
    public SmsRuntime loadSmsRuntimeFor(String eventType) {
        SmsRuntime main = loadSmsRuntime();
        if (!StringUtils.hasText(eventType)) {
            return main;
        }
        String prefix = KEY_SMS_EVENT_TEMPLATE_PREFIX + eventType + ".";
        String templateCode = value(prefix + "code", "");
        if (!StringUtils.hasText(templateCode)) {
            return main;
        }
        return new SmsRuntime(main.enabled(), main.accessKeyId(), main.accessKeySecret(), main.signName(),
                templateCode, main.noticeTemplateParam(),
                value(prefix + "fields", ""), value(prefix + "roles", ""), eventType);
    }

    /** 微信模板消息运行配置 */
    public WechatRuntime loadWechatRuntime() {
        return new WechatRuntime(
                value(KEY_WECHAT_APP_ID, ""),
                decryptQuietly(value(KEY_WECHAT_APP_SECRET, "")),
                value(KEY_WECHAT_TEMPLATE_ID, ""),
                value(KEY_WECHAT_TEMPLATE_TYPE, ""),
                value(KEY_WECHAT_FIELDS, DEFAULT_WECHAT_FIELDS),
                value(KEY_WECHAT_FIELD_ROLES, DEFAULT_WECHAT_FIELD_ROLES));
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
            log.warn("通道凭据解密失败，按未配置处理: {}", e.getMessage());
            return "";
        }
    }

    /** 模拟通道服务号名称（家长端展示） */
    public String officialAccountName() {
        return value(KEY_WECHAT_ACCOUNT_NAME, "可尔欧得视力养护");
    }

    /**
     * 短信运行配置
     *
     * @param enabled 是否开启短信通道（notice.enableSms）
     * @param noticeTemplateParam 单变量模板的变量名（未配置变量列表时生效，默认 content）
     * @param noticeTemplateFields 多变量模板的变量名（逗号分隔，按模板正文顺序）
     * @param noticeTemplateRoles  变量语义（逗号分隔，与 noticeTemplateFields 一一对应）
     * @param noticeTemplateEvents 本模板适用的事件类型（逗号分隔）；留空表示所有事件共用
     */
    public record SmsRuntime(boolean enabled, String accessKeyId, String accessKeySecret,
                             String signName, String noticeTemplateCode, String noticeTemplateParam,
                             String noticeTemplateFields, String noticeTemplateRoles,
                             String noticeTemplateEvents) {

        /** 是否具备真实发送条件（开关开启且凭据、模板齐全），否则走模拟发送 */
        public boolean realSendReady() {
            return enabled && StringUtils.hasText(accessKeyId) && StringUtils.hasText(accessKeySecret)
                    && StringUtils.hasText(signName) && StringUtils.hasText(noticeTemplateCode);
        }

        /**
         * 本模板是否适用于该业务事件
         *
         * <p>模板文案是固定的一句话（如「您已经成功预约…」），套用到其它事件会发出语义错误的短信，
         * 因此真实发送前按事件类型核对；留空视为未限定。</p>
         */
        public boolean appliesTo(String eventType) {
            if (!StringUtils.hasText(noticeTemplateEvents)) {
                return true;
            }
            return splitCsv(noticeTemplateEvents).contains(eventType);
        }

        /** 模板变量名列表：配了多变量按顺序取，否则用单变量名 */
        public List<String> fieldList() {
            if (StringUtils.hasText(noticeTemplateFields)) {
                return splitCsv(noticeTemplateFields);
            }
            return List.of(StringUtils.hasText(noticeTemplateParam) ? noticeTemplateParam.trim() : "content");
        }

        /** 模板变量语义列表：缺省为整段正文 */
        public List<String> roleList() {
            return StringUtils.hasText(noticeTemplateRoles) ? splitCsv(noticeTemplateRoles) : List.of("content");
        }
    }

    private static List<String> splitCsv(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

    /**
     * 微信模板消息运行配置
     *
     * @param templateType template=公众号模板消息 / subscribe=公众号订阅通知（空按模板消息处理）
     * @param fields       逗号分隔的模板字段名
     * @param fieldRoles   逗号分隔的字段语义，与 fields 一一对应
     */
    public record WechatRuntime(String appId, String appSecret, String templateId,
                                String templateType, String fields, String fieldRoles) {

        public boolean realSendReady() {
            return StringUtils.hasText(appId) && StringUtils.hasText(appSecret) && StringUtils.hasText(templateId);
        }

        public boolean isSubscribe() {
            return "subscribe".equalsIgnoreCase(templateType == null ? "" : templateType.trim());
        }

        public List<String> fieldList() {
            String raw = StringUtils.hasText(fields) ? fields : DEFAULT_WECHAT_FIELDS;
            return Arrays.stream(raw.split(",")).map(String::trim).toList();
        }

        public List<String> roleList() {
            String raw = StringUtils.hasText(fieldRoles) ? fieldRoles : DEFAULT_WECHAT_FIELD_ROLES;
            return Arrays.stream(raw.split(",")).map(String::trim).toList();
        }
    }
}
