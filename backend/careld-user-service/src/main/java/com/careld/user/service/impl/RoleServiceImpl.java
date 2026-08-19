package com.careld.user.service.impl;

import com.alibaba.fastjson2.JSON;
import com.careld.common.exception.BusinessException;
import com.careld.user.dto.RolePermissionRequest;
import com.careld.user.dto.RoleRequest;
import com.careld.user.entity.Role;
import com.careld.user.entity.RoleMenu;
import com.careld.user.mapper.RoleMapper;
import com.careld.user.mapper.RoleMenuMapper;
import com.careld.user.service.PermissionService;
import com.careld.user.service.RoleService;
import com.careld.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionService permissionService;
    private final JdbcTemplate jdbcTemplate;

    /** 受保护的角色编码，不可删除 */
    private static final Set<String> PROTECTED_ROLE_CODES = Set.of("super_admin", "hq_admin");

    @Override
    public List<Role> listRoles() {
        return roleMapper.selectAllRoles();
    }

    @Override
    public Role getRoleById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(404, "角色不存在");
        return role;
    }

    @Override
    public List<RoleMenu> getRoleMenus(Long roleId) {
        return roleMenuMapper.selectByRoleId(roleId);
    }

    @Override
    @Transactional
    public Long createRole(RoleRequest request) {
        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setRoleDesc(request.getRoleDesc());
        role.setUserType(request.getUserType() != null ? request.getUserType() : 1);
        role.setDataScope(request.getDataScope() != null ? request.getDataScope() : 1);
        role.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        role.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional
    public void updateRole(Long id, RoleRequest request) {
        Role role = getRoleById(id);
        if (request.getRoleName() != null) role.setRoleName(request.getRoleName());
        if (request.getRoleDesc() != null) role.setRoleDesc(request.getRoleDesc());
        if (request.getUserType() != null) role.setUserType(request.getUserType());
        if (request.getDataScope() != null) role.setDataScope(request.getDataScope());
        if (request.getSortOrder() != null) role.setSortOrder(request.getSortOrder());
        if (request.getStatus() != null) role.setStatus(request.getStatus());
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        if (PROTECTED_ROLE_CODES.contains(role.getRoleCode())) {
            throw new BusinessException(400, "系统内置角色不可删除");
        }
        // 检查是否有用户在使用
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserIdList(id);
        // 实际需要查 role_id = id 的用户
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE role_id = ?", Integer.class, id);
        if (userCount != null && userCount > 0) {
            throw new BusinessException(400, "该角色下还有" + userCount + "个用户，无法删除");
        }
        // 删除角色-菜单关联
        roleMenuMapper.deleteByRoleId(id);
        // 删除角色
        roleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void saveRolePermissions(Long roleId, RolePermissionRequest request) {
        Role role = getRoleById(roleId);

        // 先删除旧权限
        roleMenuMapper.deleteByRoleId(roleId);

        // 批量插入新权限
        if (request.getRoleMenus() != null) {
            for (RolePermissionRequest.RoleMenuItem item : request.getRoleMenus()) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(item.getMenuId());
                rm.setActions(JSON.toJSONString(item.getActions()));
                roleMenuMapper.insert(rm);
            }
        }

        // 清除该角色下所有用户的权限缓存
        List<Long> userIds = jdbcTemplate.queryForList(
                "SELECT user_id FROM sys_user_role WHERE role_id = ?", Long.class, roleId);
        for (Long userId : userIds) {
            permissionService.clearPermissionCache(userId);
        }
    }
}
