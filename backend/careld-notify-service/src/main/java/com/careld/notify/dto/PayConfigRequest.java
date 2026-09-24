package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信支付配置保存请求
 */
@Data
@Schema(description = "微信支付配置保存请求")
public class PayConfigRequest {

    @Schema(description = "是否启用真实微信支付")
    private Boolean enabled;

    @Schema(description = "微信支付商户号")
    private String mchId;

    @Schema(description = "APIv3 密钥（留空表示不修改已保存的值）")
    private String apiV3Key;

    @Schema(description = "商户 API 证书序列号")
    private String serialNo;

    @Schema(description = "商户 API 私钥文件路径（apiclient_key.pem）")
    private String privateKeyPath;

    @Schema(description = "微信支付公钥文件路径（pub_key.pem，公钥模式验签用）")
    private String publicKeyPath;

    @Schema(description = "微信支付公钥 ID（形如 PUB_KEY_ID_…）")
    private String publicKeyId;

    @Schema(description = "支付使用的 AppID（留空时复用公众号 AppID）")
    private String appId;

    @Schema(description = "支付结果回调地址（HTTPS）")
    private String notifyUrl;

    @Schema(description = "是否开启模拟支付（仅联调期使用）")
    private Boolean mockEnabled;
}
