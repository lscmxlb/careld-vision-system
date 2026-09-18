package com.careld.vision.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 养护记录（由 schedule-service 在开始/完成养护事务内写入，本服务提供查询）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("care_record")
public class CareRecord extends BaseEntity {
    private Long appointmentId;
    private Long childId;
    private Long storeId;
    private LocalDate careDate;
    private String timeSlot;
    private String visionBeforeLeft;
    private String visionBeforeRight;
    private String visionBeforeBoth;
    private String visionAfterLeft;
    private String visionAfterRight;
    private String visionAfterBoth;
    private Long executorId;
    private String executorName;
    private Integer status;

    // ===== 以下为聚合字段，不映射数据库列（JOIN child_profile 供展示/筛选）=====
    /** 儿童姓名（child_profile.name_mask；接口返回前尝试解密为全名） */
    @TableField(exist = false)
    private String childName;

    /** 家长姓名（child_profile.parent_name） */
    @TableField(exist = false)
    private String parentName;

    /** 家长手机号（child_profile.phone_mask，脱敏） */
    @TableField(exist = false)
    private String parentPhone;

    /** 儿童手机号（child_profile.phone_mask，脱敏） */
    @TableField(exist = false)
    private String childPhone;

    /** 儿童性别（child_profile.gender，1=男 0=女） */
    @TableField(exist = false)
    private Integer childGender;

    /** 该儿童累计已完成养护次数（status=2） */
    @TableField(exist = false)
    private Integer careCount;

    /** 档案剩余可约次数（child_profile.remaining_count） */
    @TableField(exist = false)
    private Integer remainingCount;

    /** 建档时裸眼双眼视力（child_profile.naked_vision_both） */
    @TableField(exist = false)
    private String nakedVisionBoth;

    /** 本次养护对应预约单备注（reserve_order.remark） */
    @TableField(exist = false)
    private String remark;

    /** 姓名密文（仅内部解密用，响应前清空） */
    @TableField(exist = false)
    private String childNameEncrypted;
}
