package com.careld.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 医务人员（医院端自行管理，手机号 + 密码登录），仅映射登录所需字段
 */
@Data
@TableName("medical_staff")
public class MedicalStaff {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private String name;
    /** 手机号（登录账号，医院内唯一） */
    private String phone;
    private Integer staffRole;
    private String loginPassword;
    private Integer status;
}
