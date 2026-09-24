package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 家长端「微信公众号」绑定二维码
 */
@Data
@Schema(description = "扫码绑定二维码")
public class WechatBindQrVO {

    @Schema(description = "公众号凭据是否齐全（齐全才返回真实二维码）")
    private boolean configured;

    @Schema(description = "带参二维码 scene（随机）")
    private String scene;

    @Schema(description = "二维码图片地址（微信官方 showqrcode）")
    private String qrImageUrl;

    @Schema(description = "二维码有效期（秒）")
    private int expireSeconds;

    @Schema(description = "未能生成真实二维码时的说明")
    private String hint;
}
