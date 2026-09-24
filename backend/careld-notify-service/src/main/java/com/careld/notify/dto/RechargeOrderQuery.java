package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 充值记录查询条件（管理后台「系统设置 → 充值记录」）
 */
@Data
@Schema(description = "充值记录查询条件")
public class RechargeOrderQuery {

    @Schema(description = "支付医院ID，空表示全部")
    private Long storeId;

    @Schema(description = "订单状态：0待支付 1已支付 2已关闭")
    private Integer status;

    @Schema(description = "关键字：匹配商户订单号或微信支付账单号")
    private String keyword;

    @Schema(description = "开始日期（含，yyyy-MM-dd）")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "结束日期（含，yyyy-MM-dd）")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Schema(hidden = true)
    private LocalDateTime startTime;

    @Schema(hidden = true)
    private LocalDateTime endTime;

    @Schema(description = "页码（从 1 开始）")
    private Integer page = 1;

    @Schema(description = "每页大小")
    private Integer size = 20;

    /** 按日查询时默认包含当日整天 */
    public LocalDateTime getStartTime() {
        if (startTime != null) {
            return startTime;
        }
        return startDate == null ? null : startDate.atStartOfDay();
    }

    public LocalDateTime getEndTime() {
        if (endTime != null) {
            return endTime;
        }
        return endDate == null ? null : endDate.atTime(LocalTime.of(23, 59, 59));
    }

    public int getOffset() {
        int safePage = page == null || page < 1 ? 1 : page;
        return (safePage - 1) * getSafeSize();
    }

    public int getSafeSize() {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 200);
    }
}
