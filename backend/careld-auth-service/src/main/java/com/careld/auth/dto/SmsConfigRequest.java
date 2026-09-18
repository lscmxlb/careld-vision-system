package com.careld.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 短信配置保存请求
 */
@Data
@Schema(description = "短信配置保存请求")
public class SmsConfigRequest {

    @Schema(description = "是否启用真实短信发送")
    private Boolean enabled;

    @Schema(description = "阿里云 AccessKey ID")
    private String accessKeyId;

    /** 留空表示不修改已保存的 Secret */
    @Schema(description = "阿里云 AccessKey Secret，留空则不修改")
    private String accessKeySecret;

    @Schema(description = "短信签名")
    private String signName;

    @Schema(description = "短信模板 Code")
    private String templateCode;

    @Schema(description = "短信模板中验证码的变量名")
    private String templateParam;

    @Schema(description = "开启短信通知")
    private Boolean enableNotice;

    @Schema(description = "预约提醒提前小时数")
    private Integer appointmentReminderHours;
}
