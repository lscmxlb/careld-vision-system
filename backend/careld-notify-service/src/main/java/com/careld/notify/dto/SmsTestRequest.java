package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 通知短信测试发送请求
 */
@Data
@Schema(description = "通知短信测试发送请求")
public class SmsTestRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "接收测试短信的手机号")
    private String phone;

    @Schema(description = "true=仅预览模板变量取值不实际发送")
    private Boolean dryRun;

    /** 各事件模板文案不同，测试时按事件取对应模板；留空按预约成功事件 */
    @Schema(description = "通知事件类型：reserve_created（默认）/care_reminder/reserve_cancelled/"
            + "reserve_adjusted/child_created/care_completed")
    private String eventType;
}
