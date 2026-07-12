package com.careld.auth.service.impl;

import com.careld.auth.dto.LoginRequest;
import com.careld.auth.dto.LoginResponse;
import com.careld.auth.dto.DeviceLoginRequest;
import com.careld.auth.entity.User;
import com.careld.auth.mapper.UserMapper;
import com.careld.auth.service.AuthService;
import com.careld.common.exception.BusinessException;
import com.careld.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        User user = null;
        if (request.getLoginType() != null && request.getLoginType() == 2) {
            // 手机号登录
            user = userMapper.selectByPhone(request.getPhone());
            if (user == null) {
                throw new BusinessException(1001, "手机号或密码错误");
            }
        } else {
            // 用户名登录
            user = userMapper.selectByUsername(request.getUsername());
            if (user == null) {
                throw new BusinessException(1001, "用户名或密码错误");
            }
        }

        // 检查状态
        if (user.getStatus() != 1) {
            throw new BusinessException(1002, "账号已被禁用");
        }

        // 检查锁定
        if (user.getLockTime() != null && user.getLockTime().plusMinutes(30).isAfter(LocalDateTime.now())) {
            throw new BusinessException(1002, "账号已被锁定，请30分钟后重试");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 增加失败次数
            int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(failCount);
            if (failCount >= 5) {
                user.setLockTime(LocalDateTime.now());
            }
            userMapper.updateById(user);
            throw new BusinessException(1001, "用户名或密码错误");
        }

        // 清除失败次数
        user.setLoginFailCount(0);
        user.setLockTime(null);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成Token
        return generateTokenResponse(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!JwtUtil.validateToken(jwtSecret, refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }

        Long userId = JwtUtil.getUserId(jwtSecret, refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(401, "用户不存在或已被禁用");
        }

        return generateTokenResponse(user);
    }

    @Override
    public void logout(String token) {
        // 将Token加入黑名单（可选，使用Redis）
        log.info("用户登出");
    }

    @Override
    public LoginResponse deviceLogin(DeviceLoginRequest request) {
        // TV设备登录逻辑（简化版）
        log.info("TV设备登录: storeCode={}, deviceCode={}", request.getStoreCode(), request.getDeviceCode());
        
        // 创建设备用户Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("storeCode", request.getStoreCode());
        claims.put("deviceCode", request.getDeviceCode());
        claims.put("userType", 2); // 门店类型
        claims.put("isDevice", true);

        String accessToken = JwtUtil.generateAccessToken(jwtSecret, claims);
        String refreshToken = JwtUtil.generateRefreshToken(jwtSecret, claims);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(7200L);
        response.setTokenType("Bearer");

        return response;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 生成Token响应
     */
    private LoginResponse generateTokenResponse(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType());
        claims.put("storeId", user.getStoreId());
        claims.put("deptId", user.getDeptId());

        String accessToken = JwtUtil.generateAccessToken(jwtSecret, claims);
        String refreshToken = JwtUtil.generateRefreshToken(jwtSecret, claims);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(7200L);
        response.setTokenType("Bearer");

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setUserType(user.getUserType());
        userInfo.setStoreId(user.getStoreId());
        userInfo.setDeptId(user.getDeptId());
        userInfo.setJobTitle(user.getJobTitle());
        userInfo.setRoles(List.of("user"));
        userInfo.setPermissions(List.of("*"));
        response.setUser(userInfo);

        return response;
    }
}
