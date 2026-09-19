package com.careld.child.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 儿童养护服务次数记录
 * change_type: 1=授予 2=预约扣减 3=取消退还 4=爽约退还 5=爽约不退还
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("child_service_record")
public class ChildServiceRecord extends BaseEntity {
    private Long childId;
    private Long storeId;
    private Integer changeType;
    private Integer changeCount;
    private Long appointmentId;
    /** 缴费金额（元），授予时必填 */
    private BigDecimal paymentAmount;
    /** 缴费方式：自费/医保/其他，授予时必填 */
    private String paymentMethod;
    /** 开单医生（medical_staff.id），授予时必填 */
    private Long doctorId;
    /** 开单医生姓名快照（展示用） */
    private String doctorName;
    private Long operatorId;
    private String remark;
    /** 该次变更后的可用次数（非落库字段，列表接口回溯计算） */
    @TableField(exist = false)
    private Integer remainingAfter;
    /** 关联预约的日期/时段（非落库字段，列表接口按 appointment_id 回填，供家长端展示预约明细） */
    @TableField(exist = false)
    private String reserveDate;
    @TableField(exist = false)
    private String timeSlotStart;
    @TableField(exist = false)
    private String timeSlotEnd;
}
