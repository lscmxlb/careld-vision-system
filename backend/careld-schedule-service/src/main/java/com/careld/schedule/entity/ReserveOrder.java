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
    /** 备注（取消时填写，非必填） */
    private String cancelReason;
    /** 取消原因类型：1家长原因 2医院原因 */
    private Integer cancelReasonType;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    /** 开始养护时间 */
    private LocalDateTime startTime;
    /** 执行医生/助理 */
    private Long executorId;
    private String executorName;
    /** 是否爽约 */
    private Integer noShowFlag;
    /** 预约次数是否已退还（1=已退还/返还，0=未退还；取消/爽约） */
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

    /** 儿童性别（1=男 0=女） */
    @TableField(exist = false)
    private Integer childGender;

    /** 儿童年龄（按出生日期计算，岁） */
    @TableField(exist = false)
    private Integer childAge;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String technicianName;
}
