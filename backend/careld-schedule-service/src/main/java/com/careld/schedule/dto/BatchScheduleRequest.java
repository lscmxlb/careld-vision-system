package com.careld.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量创建排班请求
 */
@Data
@Schema(description = "批量创建排班请求")
public class BatchScheduleRequest {

    @Schema(description = "起始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "技师ID")
    private Long technicianId;

    @Schema(description = "门店ID（为空时取当前登录用户所属门店）")
    private Long storeId;

    @Schema(description = "时间段")
    private List<TimeSlot> timeSlots;

    @Schema(description = "生效星期（1=周一 … 7=周日），为空表示每天")
    private List<Integer> weekDays;

    @Data
    @Schema(description = "时间段")
    public static class TimeSlot {
        @Schema(description = "开始时间 HH:mm")
        private String start;
        @Schema(description = "结束时间 HH:mm")
        private String end;
        @Schema(description = "最大容量")
        private Integer capacity;
    }
}
