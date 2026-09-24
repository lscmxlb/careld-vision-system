package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 总部给医院短信账户赠送试用额度（管理后台「医院列表 → 短信试用」）
 */
@Data
@Schema(description = "短信试用赠送请求")
public class TrialGrantRequest {

    @NotNull(message = "缺少医院ID")
    @Schema(description = "医院ID")
    private Long storeId;

    @NotNull(message = "请输入赠送金额")
    @DecimalMin(value = "1", message = "赠送金额不能低于 1 元")
    @DecimalMax(value = "10000", message = "赠送金额不能超过 10000 元")
    @Schema(description = "赠送金额（元）", example = "50")
    private BigDecimal amount;
}
