package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 医务人员（医生/医生助理），医院端自行管理，手机号 + 密码登录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_staff")
public class MedicalStaff extends BaseEntity {
    private Long storeId;
    private String name;
    /** 手机号（登录账号，医院内唯一） */
    private String phone;
    /** 0未知 1男 2女 */
    private Integer gender;
    /** 1=医生 2=医生助理 */
    private Integer staffRole;
    private String loginPassword;
    private Integer status;
}
