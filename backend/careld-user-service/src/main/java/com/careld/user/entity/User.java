package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String avatarUrl;
    private Integer userType;
    private Long storeId;
    private Long hqId;
    private Long centerId;
    private Long agentId;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginFailCount;
    private LocalDateTime lockTime;

    // ===== 以下为聚合字段，不映射数据库列（供前端展示）=====
    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String agentName;

    @TableField(exist = false)
    private String centerName;
}
