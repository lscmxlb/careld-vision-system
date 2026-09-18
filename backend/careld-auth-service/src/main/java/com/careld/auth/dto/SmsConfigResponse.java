package com.careld.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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

    @Schema(description = "未启用真实发送时验证码是否回退固定 123456")
    private Boolean mockFallback;

    @Schema(description = "开启短信通知")
    private Boolean enableNotice;

    @Schema(description = "预约提醒提前小时数")
    private Integer appointmentReminderHours;
}
