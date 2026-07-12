package com.careld.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reserve_order")
public class ReserveOrder extends BaseEntity {
    private String orderNo;
    private Long storeId;
    private Long scheduleId;
    private Long childId;
    private String parentName;
    private String parentPhone;

    @JsonProperty("scheduleDate")
    private LocalDate reserveDate;

    private LocalTime reserveTimeStart;
    private LocalTime reserveTimeEnd;
    private Integer reserveType;
    private Integer status;
    private Integer source;
    private String remark;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;

    // 以下字段不映射数据库列，用于前端展示
    @TableField(exist = false)
    private String childName;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String technicianName;
}
