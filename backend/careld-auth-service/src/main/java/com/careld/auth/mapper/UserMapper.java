package com.careld.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted_at IS NULL")
    User selectByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted_at IS NULL")
    User selectByPhone(String phone);

    /**
     * 根据主键查询用户：显式 SELECT *（本库 sys_user 无 dept_id 等实体字段，
     * MyBatis-Plus 全列查询会报 Unknown column）
     */
    @Select("SELECT * FROM sys_user WHERE id = #{id} AND deleted_at IS NULL")
    User selectUserById(Long id);
}
