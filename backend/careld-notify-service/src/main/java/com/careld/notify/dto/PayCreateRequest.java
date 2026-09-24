package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 医院端发起充值（微信扫码支付下单）
 */
@Data
@Schema(description = "充值下单请求")
public class PayCreateRequest {

    @NotNull(message = "请输入充值金额")
    @DecimalMin(value = "1", message = "单次充值金额不能低于 1 元")
    @DecimalMax(value = "10000", message = "单次充值金额不能超过 10000 元")
    @Schema(description = "充值金额（元）", example = "100")
    private BigDecimal amount;
}
