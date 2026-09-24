package com.careld.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 短信配置查询响应（AccessKey Secret 不回明文）
 */
@Data
@Schema(description = "短信配置")
public class SmsConfigResponse {

    @Schema(description = "是否启用真实短信发送")
    private Boolean enabled;

    @Schema(description = "阿里云 AccessKey ID")
    private String accessKeyId;

    @Schema(description = "AccessKey Secret 是否已配置")
    private Boolean accessKeySecretConfigured;

    @Schema(description = "AccessKey Secret 掩码（仅末 4 位）")
    private String accessKeySecretMasked;

    @Schema(description = "短信签名")
    private String signName;

    @Schema(description = "短信模板 Code")
    private String templateCode;

    @Schema(description = "短信模板中验证码的变量名")
    private String templateParam;

    @Schema(description = "业务通知短信模板 Code")
    private String noticeTemplateCode;

    @Schema(description = "业务通知短信模板的变量名")
    private String noticeTemplateParam;

    @Schema(description = "业务通知短信模板的变量名列表（逗号分隔，按模板正文顺序）")
    private String noticeTemplateFields;

    @Schema(description = "变量语义（逗号分隔，与变量名列表一一对应）")
    private String noticeTemplateRoles;

    @Schema(description = "本模板适用的通知事件（逗号分隔）；留空表示所有事件共用")
    private String noticeTemplateEvents;

    @Schema(description = "未启用真实发送时验证码是否回退固定 123456")
    private Boolean mockFallback;

    @Schema(description = "开启短信通知")
    private Boolean enableNotice;

    @Schema(description = "养护提醒提前分钟数（预约开始前再次发送预约成功短信）")
    private Integer careReminderMinutes;

    @Schema(description = "其它事件的独立短信模板（预约取消/预约调整/建档成功/养护完成）")
    private List<EventTemplate> eventTemplates;

    @Data
    @Schema(description = "单个事件的短信模板")
    public static class EventTemplate {

        @Schema(description = "事件类型：reserve_cancelled/reserve_adjusted/child_created/care_completed")
        private String eventType;

        @Schema(description = "模板 Code，空表示该事件不发短信")
        private String templateCode;

        @Schema(description = "模板变量名列表（逗号分隔，按模板正文顺序）")
        private String templateFields;

        @Schema(description = "变量语义（逗号分隔，与变量名列表一一对应）")
        private String templateRoles;
    }
}
