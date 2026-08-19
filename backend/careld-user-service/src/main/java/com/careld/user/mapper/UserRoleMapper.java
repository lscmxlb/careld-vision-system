package com.careld.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户-角色关联Mapper
 */
@Mapper
public interface UserRoleMapper {

    @Select("SELECT r.id FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.status = 1")
    List<Long> selectRoleIdsByUserId(Long userId);

    @Select("SELECT r.* FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.status = 1")
    List<Map<String, Object>> selectRolesByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM sys_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int exists(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT ur.role_id FROM sys_user_role ur WHERE ur.user_id = #{userId}")
    List<Long> selectRoleIdsByUserIdList(Long userId);
}
