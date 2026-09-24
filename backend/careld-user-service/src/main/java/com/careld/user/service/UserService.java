package com.careld.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.user.dto.PhoneLookupResponse;
import com.careld.user.dto.UserCreateRequest;
import com.careld.user.dto.UserResponse;
import com.careld.user.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 创建用户
     */
    Long createUser(UserCreateRequest request);

    /**
     * 更新用户
     */
    void updateUser(Long id, UserCreateRequest request);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 获取用户详情
     */
    UserResponse getUserById(Long id);

    /**
     * 获取当前用户信息
     */
    UserResponse getCurrentUser(Long userId);

    /**
     * 获取当前医务人员信息（token userType=6 时 userId 为 medical_staff 主键，与 sys_user 无关联）
     */
    UserResponse getMedicalStaffCurrentUser(Long staffId);

    /**
     * 用户列表
     */
    Page<UserResponse> listUsers(Integer userType, Long storeId, Long centerId, Long agentId, Integer status, String keyword, Integer page, Integer size,
                                  Integer excludePeerType, Long currentUserId, boolean excludeHq, Long onlyUserId);

    /**
     * 重置密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 启用/禁用用户
     */
    void updateStatus(Long id, Integer status);

    /**
     * 根据门店查询用户
     */
    List<UserResponse> listByStoreId(Long storeId);

    /**
     * 修改本人密码（校验旧密码）
     */
    void changeMyPassword(Long userId, String oldPassword, String newPassword);

    /**
     * 修改本人姓名（家长建档时自动同步真实姓名到账号）
     */
    void updateMyRealName(Long userId, String realName);

    /**
     * 修改医务人员本人密码（校验旧密码）
     */
    void changeMyStaffPassword(Long staffId, String oldPassword, String newPassword);

    /**
     * 修改医务人员本人手机号（登录账号，店内唯一）
     */
    void updateMyStaffPhone(Long staffId, String phone);

    /**
     * 手机号码查询：查该号码是否已注册及注册身份/角色（含系统账号与医务人员两类来源）
     */
    PhoneLookupResponse lookupByPhone(String phone);
}
