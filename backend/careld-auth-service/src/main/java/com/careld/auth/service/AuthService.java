package com.careld.auth.service;

import com.careld.auth.dto.LoginRequest;
import com.careld.auth.dto.LoginResponse;
import com.careld.auth.dto.DeviceLoginRequest;
import com.careld.auth.entity.User;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * TV设备登录
     */
    LoginResponse deviceLogin(DeviceLoginRequest request);

    /**
     * 根据ID获取用户
     */
    User getUserById(Long userId);
}
