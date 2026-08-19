package com.careld.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 权限查询Mapper（auth-service 内部使用，直接查库）
 */
@Mapper
public interface PermissionMapper {

    /**
     * 查询用户所有权限（菜单+操作）
     * 返回每条记录包含: id, permission_key, menu_type, actions(JSON字符串)
     */
    @Select("SELECT DISTINCT m.id, m.permission_key, m.menu_type, rm.actions " +
            "FROM sys_user_role ur " +
            "JOIN sys_role_menu rm ON ur.role_id = rm.role_id " +
            "JOIN sys_menu m ON rm.menu_id = m.id " +
            "WHERE ur.user_id = #{userId} AND m.status = 1 AND m.permission_key IS NOT NULL")
    List<Map<String, Object>> selectPermissionsByUserId(Long userId);
}
