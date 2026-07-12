package com.careld.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted_at IS NULL")
    User selectByUsername(String username);

    /**
     * 条件查询用户列表
     */
    @Select("<script>" +
            "SELECT u.*, s.store_name as storeName FROM sys_user u " +
            "LEFT JOIN store_info s ON u.store_id = s.id " +
            "WHERE u.deleted_at IS NULL " +
            "<if test='userType != null'> AND u.user_type = #{userType} </if>" +
            "<if test='storeId != null'> AND u.store_id = #{storeId} </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (u.username LIKE CONCAT('%',#{keyword},'%') OR u.real_name LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "ORDER BY u.created_at DESC" +
            "</script>")
    Page<User> selectUserPage(Page<User> page, @Param("userType") Integer userType,
                              @Param("storeId") Long storeId, @Param("keyword") String keyword);

    /**
     * 查询门店用户
     */
    @Select("SELECT * FROM sys_user WHERE store_id = #{storeId} AND deleted_at IS NULL")
    List<User> selectByStoreId(Long storeId);
}
