package com.careld.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * 用户列表
     */
    Page<UserResponse> listUsers(Integer userType, Long storeId, String keyword, Integer page, Integer size);

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
}
