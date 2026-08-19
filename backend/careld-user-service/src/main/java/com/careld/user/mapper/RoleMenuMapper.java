package com.careld.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.user.entity.RoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-菜单关联Mapper
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

    @Select("SELECT rm.*, m.menu_name, m.menu_type, m.menu_path, m.menu_icon, m.permission_key, m.parent_id, m.sort_order, m.visible, m.status " +
            "FROM sys_role_menu rm " +
            "LEFT JOIN sys_menu m ON rm.menu_id = m.id " +
            "WHERE rm.role_id IN (${roleIds}) AND m.status = 1 " +
            "ORDER BY m.sort_order ASC, m.id ASC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "roleId", column = "role_id"),
        @Result(property = "menuId", column = "menu_id"),
        @Result(property = "actions", column = "actions"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "menu.id", column = "menu_id"),
        @Result(property = "menu.menuName", column = "menu_name"),
        @Result(property = "menu.menuType", column = "menu_type"),
        @Result(property = "menu.menuPath", column = "menu_path"),
        @Result(property = "menu.menuIcon", column = "menu_icon"),
        @Result(property = "menu.permissionKey", column = "permission_key"),
        @Result(property = "menu.parentId", column = "parent_id"),
        @Result(property = "menu.sortOrder", column = "sort_order"),
        @Result(property = "menu.visible", column = "visible"),
        @Result(property = "menu.status", column = "status")
    })
    List<RoleMenu> selectByRoleIds(String roleIds);

    @Select("SELECT rm.*, m.menu_name, m.menu_type, m.menu_path, m.menu_icon, m.permission_key, m.parent_id, m.sort_order, m.visible, m.status " +
            "FROM sys_role_menu rm " +
            "LEFT JOIN sys_menu m ON rm.menu_id = m.id " +
            "WHERE rm.role_id = #{roleId} AND m.status = 1 " +
            "ORDER BY m.sort_order ASC, m.id ASC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "roleId", column = "role_id"),
        @Result(property = "menuId", column = "menu_id"),
        @Result(property = "actions", column = "actions"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "menu.id", column = "menu_id"),
        @Result(property = "menu.menuName", column = "menu_name"),
        @Result(property = "menu.menuType", column = "menu_type"),
        @Result(property = "menu.menuPath", column = "menu_path"),
        @Result(property = "menu.menuIcon", column = "menu_icon"),
        @Result(property = "menu.permissionKey", column = "permission_key"),
        @Result(property = "menu.parentId", column = "parent_id"),
        @Result(property = "menu.sortOrder", column = "sort_order"),
        @Result(property = "menu.visible", column = "visible"),
        @Result(property = "menu.status", column = "status")
    })
    List<RoleMenu> selectByRoleId(Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(Long roleId);
}
