package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信支付配置（管理后台「系统设置」维护）
 */
@Data
@Schema(description = "微信支付配置")
public class PayConfigVO {

    @Schema(description = "是否启用真实微信支付")
    private Boolean enabled;

    @Schema(description = "微信支付商户号")
    private String mchId;

    @Schema(description = "APIv3 密钥是否已配置")
    private Boolean apiV3KeyConfigured;

    @Schema(description = "APIv3 密钥掩码")
    private String apiV3KeyMasked;

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

    @Schema(description = "回调地址（网络可达时由系统按回调域名前缀自动拼装）")
    private String suggestedNotifyUrl;

    @Schema(description = "是否开启模拟支付（仅联调期使用，正式运营必须关闭）")
    private Boolean mockEnabled;

    @Schema(description = "真实支付条件是否齐备")
    private Boolean realReady;

    @Schema(description = "未就绪原因")
    private String notReadyReason;
}
