package com.careld.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 家长端注册请求（注册成功即签发登录态）
 */
@Data
@Schema(description = "家长端注册请求")
public class ParentRegisterRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "短信验证码")
    private String code;

    @NotBlank(message = "用户名称不能为空")
    @Size(max = 20, message = "用户名称不能超过20个字")
    @Schema(description = "用户名称（建档时自动作为家长姓名）")
    private String realName;

    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为6~20位")
    @Schema(description = "登录密码")
    private String password;
}
