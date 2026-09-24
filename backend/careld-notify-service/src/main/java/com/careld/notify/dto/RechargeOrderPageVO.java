package com.careld.notify.dto;

import com.careld.common.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充值记录分页结果（管理后台「系统设置 → 充值记录」）
 */
@Data
@Schema(description = "充值记录分页结果")
public class RechargeOrderPageVO {

    @Schema(description = "充值记录列表")
    private List<RechargeOrderVO> list;

    @Schema(description = "分页信息")
    private PageResult.Pagination pagination;

    @Schema(description = "当前查询条件下实际收入合计（元，仅真实微信扫码支付）")
    private BigDecimal actualIncome;
}
