package com.careld.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图形验证码响应
 */
@Data
@Schema(description = "图形验证码")
public class CaptchaResponse {

    @Schema(description = "验证码缓存 key（登录时回传）")
    private String captchaKey;

    @Schema(description = "验证码图片 base64（可直接用于 <img src>）")
    private String captchaImage;
}
