package com.careld.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 每日可约时段（由排班规则物化生成，用于并发控制人数上限）
 */
@Data
@TableName("schedule_slot")
public class ScheduleSlot {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private Long storeId;
    private LocalDate slotDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private Integer maxCapacity;
    private Integer bookedCount;
    /** 1=开放 0=关闭 */
    private Integer status;
}
