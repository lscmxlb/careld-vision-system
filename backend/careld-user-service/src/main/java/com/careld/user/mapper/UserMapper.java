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
     * 根据手机号查询（医务人员改号时校验是否与普通账号撞号）
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted_at IS NULL LIMIT 1")
    User selectByPhone(String phone);

    /**
     * 手机号码查询：按手机号或用户名（家长账号用户名为手机号）模糊匹配，含组织名称
     */
    @Select("SELECT u.*, s.store_name as storeName, a.agent_name as agentName, c.center_name as centerName " +
            "FROM sys_user u " +
            "LEFT JOIN store_info s ON u.store_id = s.id " +
            "LEFT JOIN agent a ON u.agent_id = a.id AND a.deleted_at IS NULL " +
            "LEFT JOIN ops_center c ON u.center_id = c.id AND c.deleted_at IS NULL " +
            "WHERE u.deleted_at IS NULL " +
            "AND (u.phone LIKE CONCAT('%', #{phone}, '%') OR u.username LIKE CONCAT('%', #{phone}, '%')) " +
            "ORDER BY u.id")
    List<User> selectByPhoneOrUsernameLike(@Param("phone") String phone);

    /**
     * 条件查询用户列表
     * 数据权限说明：
     * - centerId/agentId 过滤同时匹配用户自身组织字段及具store链（家长通过医院→代理商→中心向上追溯）
     * - excludePeerType：排除平级用户类型（仅保留 currentUserId 自己）
     * - excludeHq：完全排除总部用户（非总部角色不可见总部用户）
     * - onlyUserId：仅返回指定用户（家长只能看自己）
     */
    @Select("<script>" +
            "SELECT u.*, s.store_name as storeName, a.agent_name as agentName, c.center_name as centerName " +
            "FROM sys_user u " +
            "LEFT JOIN store_info s ON u.store_id = s.id " +
            "LEFT JOIN agent a ON u.agent_id = a.id AND a.deleted_at IS NULL " +
            "LEFT JOIN ops_center c ON u.center_id = c.id AND c.deleted_at IS NULL " +
            "WHERE u.deleted_at IS NULL " +
            "<if test='userType != null'> AND u.user_type = #{userType} </if>" +
            "<if test='storeId != null'> AND u.store_id = #{storeId} </if>" +
            "<if test='centerId != null'> " +
            "AND (u.center_id = #{centerId} " +
            "OR u.store_id IN (SELECT st.id FROM store_info st JOIN agent ag ON st.agent_id = ag.id " +
            "WHERE ag.center_id = #{centerId} AND st.deleted_at IS NULL)) " +
            "</if>" +
            "<if test='agentId != null'> " +
            "AND (u.agent_id = #{agentId} " +
            "OR u.store_id IN (SELECT st.id FROM store_info st WHERE st.agent_id = #{agentId} AND st.deleted_at IS NULL)) " +
            "</if>" +
            "<if test='status != null'> AND u.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (u.username LIKE CONCAT('%',#{keyword},'%') OR u.real_name LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='excludePeerType != null'> AND (u.user_type != #{excludePeerType} OR u.id = #{currentUserId}) </if>" +
            "<if test='excludeHq'> AND u.user_type != 1 </if>" +
            "<if test='onlyUserId != null'> AND u.id = #{onlyUserId} </if>" +
            "ORDER BY u.created_at DESC" +
            "</script>")
    Page<User> selectUserPage(Page<User> page, @Param("userType") Integer userType,
                              @Param("storeId") Long storeId, @Param("keyword") String keyword,
                              @Param("centerId") Long centerId, @Param("agentId") Long agentId,
                              @Param("status") Integer status,
                              @Param("excludePeerType") Integer excludePeerType,
                              @Param("currentUserId") Long currentUserId,
                              @Param("excludeHq") boolean excludeHq,
                              @Param("onlyUserId") Long onlyUserId);

    /**
     * 查询门店用户
     */
    @Select("SELECT * FROM sys_user WHERE store_id = #{storeId} AND deleted_at IS NULL")
    List<User> selectByStoreId(Long storeId);
}
