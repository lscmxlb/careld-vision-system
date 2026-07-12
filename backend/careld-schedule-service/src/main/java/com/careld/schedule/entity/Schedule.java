package com.careld.schedule.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_info")
public class Schedule extends BaseEntity {
    private Long storeId;
    private Long deptId;
    private LocalDate scheduleDate;
    private Long technicianId;
    private String technicianName;
    private LocalTime timeSlotStart;
    private LocalTime timeSlotEnd;
    private Integer maxCapacity;
    private Integer reservedCount;
    private Integer status;
    private String remark;
}
