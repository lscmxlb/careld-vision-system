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

    @JsonProperty("timeSlotStart")
    private LocalTime reserveTimeStart;

    @JsonProperty("timeSlotEnd")
    private LocalTime reserveTimeEnd;
    private Integer reserveType;
    private Integer status;
    private Integer source;
    private String remark;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    /** 开始养护时间 */
    private LocalDateTime startTime;
    /** 执行医生/助理 */
    private Long executorId;
    private String executorName;
    /** 是否爽约 */
    private Integer noShowFlag;
    /** 爽约次数是否已退还 */
    private Integer refundFlag;
    /** 关联每日时段（新链路） */
    private Long slotId;
    /** 预约操作人姓名（门店医生/员工） */
    private String operatorName;
    /** 取消操作人账户ID */
    private Long cancelOperatorId;
    /** 取消操作人姓名 */
    private String cancelOperatorName;
    /** 是否已调整（1=已调整） */
    private Integer adjustFlag;
    /** 调整操作人账户ID */
    private Long adjustOperatorId;
    /** 调整操作人姓名 */
    private String adjustOperatorName;

    // 以下字段不映射数据库列，用于前端展示
    @TableField(exist = false)
    private String childName;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String technicianName;
}
