package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-菜单关联实体
 */
@Data
@TableName("sys_role_menu")
public class RoleMenu {
    private Long id;
    private Long roleId;
    private Long menuId;
    private String actions;  // JSON array string, e.g. ["view","create","update","delete"]
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Menu menu;
}
