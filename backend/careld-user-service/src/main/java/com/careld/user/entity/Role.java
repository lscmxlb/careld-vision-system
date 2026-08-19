package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体
 */
@Data
@TableName("sys_role")
public class Role {
    private Long id;
    private String roleCode;
    private String roleName;
    private String roleDesc;
    private Integer userType;
    private Integer dataScope;   // 1全部 2本中心及下级 3本代理商及下级 4本医院 5个人
    private Integer sortOrder;
    private Integer status;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
