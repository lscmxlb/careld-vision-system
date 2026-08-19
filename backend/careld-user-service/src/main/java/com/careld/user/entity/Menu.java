package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class Menu extends BaseEntity {
    private Long parentId;
    private String menuName;
    private Integer menuType;       // 1目录 2菜单 3按钮
    private String menuPath;
    private String menuIcon;
    private String permissionKey;
    private Integer sortOrder;
    private Integer visible;
    private Integer status;

    @TableField(exist = false)
    private List<Menu> children;
}
