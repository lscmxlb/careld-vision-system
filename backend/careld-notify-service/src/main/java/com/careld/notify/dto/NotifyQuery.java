package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 通知记录查询条件
 */
@Data
@Schema(description = "通知记录查询条件")
public class NotifyQuery {

    @Schema(description = "医院ID（门店维护类用户由后端强制覆盖）")
    private Long storeId;

    @Schema(description = "儿童姓名关键字")
    private String childNameLike;

    @Schema(description = "姓名检索命中的儿童ID集合（后端按明文/掩码匹配解析）")
    private List<Long> childIds;

    @Schema(description = "通知类型")
    private String eventType;

    @Schema(description = "通知渠道：1短信 2微信")
    private Integer channel;

    @Schema(description = "是否成功：1成功 0失败")
    private Integer status;

    @Schema(description = "开始日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Schema(description = "页码", example = "1")
    private long page = 1;

    @Schema(description = "每页大小", example = "20")
    private long size = 20;

    @Schema(hidden = true)
    private LocalDateTime startTime;

    @Schema(hidden = true)
    private LocalDateTime endTime;

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

    @Schema(hidden = true)
    public long getOffset() {
        long safePage = page < 1 ? 1 : page;
        return (safePage - 1) * getSafeSize();
    }

    @Schema(hidden = true)
    public long getSafeSize() {
        if (size < 1) {
            return 20;
        }
        return size > 200 ? 200 : size;
    }
}
