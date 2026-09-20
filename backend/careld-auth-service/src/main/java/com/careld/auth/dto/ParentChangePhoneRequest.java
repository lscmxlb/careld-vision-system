package com.careld.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 家长端修改手机号：新手机号+验证码（旧手机号不验证）
 */
@Data
@Schema(description = "家长端修改手机号请求")
public class ParentChangePhoneRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "新手机号（需通过短信验证）")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "新手机号收到的短信验证码")
    private String code;
}
