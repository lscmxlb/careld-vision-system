package com.careld.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.user.dto.UserCreateRequest;
import com.careld.user.dto.UserResponse;
import com.careld.user.entity.User;
import com.careld.user.mapper.UserMapper;
import com.careld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public Long createUser(UserCreateRequest request) {
        // 检查用户名是否存在
        User existUser = userMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUserType(request.getUserType());
        user.setStoreId(request.getStoreId());
        user.setStatus(1);

        userMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional
    public void updateUser(Long id, UserCreateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStoreId(request.getStoreId());

        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return null;
        }
        return convertToResponse(user);
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        return getUserById(userId);
    }

    @Override
    public Page<UserResponse> listUsers(Integer userType, Long storeId, String keyword, Integer page, Integer size) {
        Page<User> pageParam = new Page<>(page, size);
        Page<User> userPage = userMapper.selectUserPage(pageParam, userType, storeId, keyword);

        List<UserResponse> records = userPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        Page<UserResponse> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public List<UserResponse> listByStoreId(Long storeId) {
        List<User> users = userMapper.selectByStoreId(storeId);
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void changeMyPassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(4003, "原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(400, "新密码至少6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setUserType(user.getUserType());
        response.setStoreId(user.getStoreId());
        response.setStatus(user.getStatus());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
