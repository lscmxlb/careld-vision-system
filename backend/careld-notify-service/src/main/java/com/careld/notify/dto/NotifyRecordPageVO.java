package com.careld.notify.dto;

import com.careld.common.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 通知记录分页结果：列表 + 当前查询条件下的费用总额 + 账户余额
 */
@Data
@Schema(description = "通知记录分页结果")
public class NotifyRecordPageVO {

    @Schema(description = "记录列表")
    private List<NotifyRecordVO> list;

    @Schema(description = "分页信息")
    private PageResult.Pagination pagination;

    @Schema(description = "当前查询条件下的费用总额（元）")
    private BigDecimal filterFee;

    @Schema(description = "累计消费（元）")
    private BigDecimal totalFee;

    @Schema(description = "可用余额（元）")
    private BigDecimal balance;

    @Schema(description = "累计充值（元）")
    private BigDecimal totalRecharge;

    @Schema(description = "短信单价（元/条）")
    private BigDecimal smsUnitPrice;
}
