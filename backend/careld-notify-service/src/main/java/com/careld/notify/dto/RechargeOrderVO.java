package com.careld.notify.dto;

import com.careld.notify.entity.StoreRechargeOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值订单（医院端扫码支付 / 管理后台充值记录共用）
 */
@Data
@Schema(description = "充值订单")
public class RechargeOrderVO {

    @Schema(description = "商户订单号")
    private String orderNo;

    @Schema(description = "支付金额（元）")
    private BigDecimal amount;

    @Schema(description = "订单状态：0待支付 1已支付 2已关闭")
    private Integer status;

    @Schema(description = "Native 支付二维码链接（待支付时返回）")
    private String codeUrl;

    @Schema(description = "微信支付账单号")
    private String transactionId;

    @Schema(description = "微信交易状态")
    private String tradeState;

    @Schema(description = "是否模拟支付")
    private Boolean mock;

    @Schema(description = "订单过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "支付成功时间")
    private LocalDateTime paidAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    // ===== 管理后台充值记录列表展示 =====

    @Schema(description = "支付医院ID")
    private Long storeId;

    @Schema(description = "支付医院名称")
    private String storeName;

    public static RechargeOrderVO of(StoreRechargeOrder order) {
        RechargeOrderVO vo = new RechargeOrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setCodeUrl(order.getCodeUrl());
        vo.setTransactionId(order.getTransactionId());
        vo.setTradeState(order.getTradeState());
        vo.setMock(order.getMockFlag() != null && order.getMockFlag() == 1);
        vo.setExpireAt(order.getExpireAt());
        vo.setPaidAt(order.getPaidAt());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setStoreId(order.getStoreId());
        vo.setStoreName(order.getStoreName());
        return vo;
    }
}
