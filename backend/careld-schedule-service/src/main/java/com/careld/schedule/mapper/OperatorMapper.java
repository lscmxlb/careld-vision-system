package com.careld.schedule.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 预约操作人姓名解析（医务人员登录用户名为手机号，sys_user 取真实姓名）
 */
@Mapper
public interface OperatorMapper {

    @Select("SELECT real_name FROM sys_user WHERE username = #{username} AND deleted_at IS NULL LIMIT 1")
    String selectSysUserRealName(@Param("username") String username);

    @Select("SELECT name FROM medical_staff WHERE phone = #{phone} AND status = 1 AND deleted_at IS NULL ORDER BY id LIMIT 1")
    String selectStaffName(@Param("phone") String phone);

    @Select("SELECT COALESCE(real_name, username) FROM sys_user WHERE id = #{id} AND deleted_at IS NULL LIMIT 1")
    String selectSysUserDisplayNameById(@Param("id") Long id);

    @Select("SELECT name FROM medical_staff WHERE id = #{id} AND deleted_at IS NULL LIMIT 1")
    String selectStaffNameById(@Param("id") Long id);
}
