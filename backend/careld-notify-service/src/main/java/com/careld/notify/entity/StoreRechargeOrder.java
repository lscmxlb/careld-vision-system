package com.careld.notify.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 医院短信服务充值订单（微信扫码支付）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_recharge_order")
@Schema(description = "医院充值订单")
public class StoreRechargeOrder extends BaseEntity {

    @Schema(description = "支付医院ID")
    private Long storeId;

    @Schema(description = "商户订单号（微信支付 out_trade_no）")
    private String orderNo;

    @Schema(description = "支付金额（元）")
    private BigDecimal amount;

    @Schema(description = "订单状态：0待支付 1已支付 2已关闭")
    private Integer status;

    @Schema(description = "微信支付预支付交易会话标识")
    private String prepayId;

    @Schema(description = "Native 支付二维码链接")
    private String codeUrl;

    @Schema(description = "微信支付账单号")
    private String transactionId;

    @Schema(description = "微信交易状态")
    private String tradeState;

    @Schema(description = "支付人 openid")
    private String payerOpenid;

    @Schema(description = "支付成功时间")
    private LocalDateTime paidAt;

    @Schema(description = "订单过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "是否模拟支付：1是 0否")
    private Integer mockFlag;

    @TableField(exist = false)
    @Schema(description = "支付医院名称")
    private String storeName;
}
