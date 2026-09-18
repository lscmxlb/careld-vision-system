package com.careld.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.exception.BusinessException;
import com.careld.user.dto.UserCreateRequest;
import com.careld.user.dto.UserResponse;
import com.careld.user.entity.MedicalStaff;
import com.careld.user.entity.User;
import com.careld.user.mapper.MedicalStaffMapper;
import com.careld.user.mapper.UserMapper;
import com.careld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final MedicalStaffMapper medicalStaffMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 绑定医院时自动补全组织链（agentId/centerId），保证数据完整性和列表展示
     */
    private void enrichOrgChain(User user) {
        if (user.getStoreId() == null || (user.getAgentId() != null && user.getCenterId() != null)) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.agent_id AS agentId, a.center_id AS centerId FROM store_info s " +
                        "LEFT JOIN agent a ON s.agent_id = a.id " +
                        "WHERE s.id = ? AND s.deleted_at IS NULL", user.getStoreId());
        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            if (user.getAgentId() == null && row.get("agentId") != null) {
                user.setAgentId(((Number) row.get("agentId")).longValue());
            }
            if (user.getCenterId() == null && row.get("centerId") != null) {
                user.setCenterId(((Number) row.get("centerId")).longValue());
            }
        }
    }

    @Override
    @Transactional
    public Long createUser(UserCreateRequest request) {
        // 创建用户时用户名必填
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        // 创建用户时密码必填
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(400, "密码不能为空");
        }
        // 创建用户时用户类型必填
        if (request.getUserType() == null) {
            throw new BusinessException(400, "用户类型不能为空");
        }
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
        user.setCenterId(request.getCenterId());
        user.setAgentId(request.getAgentId());
        user.setStoreId(request.getStoreId());
        // 绑定医院时自动补全组织链
        enrichOrgChain(user);
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
        user.setUserType(request.getUserType());
        user.setCenterId(request.getCenterId());
        user.setAgentId(request.getAgentId());
        user.setStoreId(request.getStoreId());
        // 绑定医院时自动补全组织链
        enrichOrgChain(user);

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
        UserResponse response = getUserById(userId);
        // 详情查询不含门店名（非表字段），补齐供前端抬头展示
        if (response != null && response.getStoreName() == null && response.getStoreId() != null) {
            response.setStoreName(selectStoreName(response.getStoreId()));
        }
        return response;
    }

    @Override
    public UserResponse getMedicalStaffCurrentUser(Long staffId) {
        MedicalStaff staff = medicalStaffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException(404, "医务人员不存在");
        }
        UserResponse response = new UserResponse();
        response.setId(staff.getId());
        response.setUsername(staff.getPhone());
        response.setRealName(staff.getName());
        response.setPhone(staff.getPhone());
        response.setUserType(6);
        response.setStaffRole(staff.getStaffRole());
        response.setStoreId(staff.getStoreId());
        response.setStoreName(selectStoreName(staff.getStoreId()));
        response.setStatus(staff.getStatus());
        response.setCreatedAt(staff.getCreatedAt());
        response.setRoles(List.of("medical_staff"));
        response.setPermissions(Collections.emptyList());
        return response;
    }

    private String selectStoreName(Long storeId) {
        if (storeId == null) {
            return null;
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT store_name FROM store_info WHERE id = ? AND deleted_at IS NULL", String.class, storeId);
        return names.isEmpty() ? null : names.get(0);
    }

    @Override
    public Page<UserResponse> listUsers(Integer userType, Long storeId, Long centerId, Long agentId, Integer status, String keyword, Integer page, Integer size,
                                        Integer excludePeerType, Long currentUserId, boolean excludeHq, Long onlyUserId) {
        Page<User> pageParam = new Page<>(page, size);
        Page<User> userPage = userMapper.selectUserPage(pageParam, userType, storeId, keyword, centerId, agentId, status,
                excludePeerType, currentUserId, excludeHq, onlyUserId);

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

    @Override
    @Transactional
    public void changeMyStaffPassword(Long staffId, String oldPassword, String newPassword) {
        MedicalStaff staff = medicalStaffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException(404, "医务人员不存在");
        }
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, staff.getLoginPassword())) {
            throw new BusinessException(4003, "原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(400, "新密码至少6位");
        }
        staff.setLoginPassword(passwordEncoder.encode(newPassword));
        medicalStaffMapper.updateById(staff);
    }

    @Override
    @Transactional
    public void updateMyStaffPhone(Long staffId, String phone) {
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BusinessException(400, "手机号格式不正确");
        }
        MedicalStaff staff = medicalStaffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException(404, "医务人员不存在");
        }
        if (phone.equals(staff.getPhone())) {
            return;
        }
        MedicalStaff dup = medicalStaffMapper.selectByStoreAndPhone(staff.getStoreId(), phone);
        if (dup != null && !dup.getId().equals(staffId)) {
            throw new BusinessException(400, "该手机号已存在于本院医务人员中");
        }
        // 与普通账号撞号会导致登录串号（登录按手机号先查 sys_user）
        if (userMapper.selectByPhone(phone) != null) {
            throw new BusinessException(400, "该手机号已被其他账号使用");
        }
        staff.setPhone(phone);
        medicalStaffMapper.updateById(staff);
    }

    @Override
    public void updateMyRealName(Long userId, String realName) {
        if (realName == null || realName.isBlank()) {
            throw new BusinessException(400, "姓名不能为空");
        }
        String name = realName.trim();
        if (name.length() > 20) {
            throw new BusinessException(400, "姓名不能超过20个字");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setRealName(name);
        userMapper.updateById(user);
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setUserType(user.getUserType());
        response.setCenterId(user.getCenterId());
        response.setCenterName(user.getCenterName());
        response.setAgentId(user.getAgentId());
        response.setAgentName(user.getAgentName());
        response.setStoreId(user.getStoreId());
        response.setStoreName(user.getStoreName());
        response.setStatus(user.getStatus());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
