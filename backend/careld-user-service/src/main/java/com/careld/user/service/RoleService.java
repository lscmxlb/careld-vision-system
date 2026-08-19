package com.careld.user.service;

import com.careld.user.dto.RolePermissionRequest;
import com.careld.user.dto.RoleRequest;
import com.careld.user.entity.Role;
import com.careld.user.entity.RoleMenu;

import java.util.List;

/**
 * 角色服务
 */
public interface RoleService {

    List<Role> listRoles();

    Role getRoleById(Long id);

    List<RoleMenu> getRoleMenus(Long roleId);

    Long createRole(RoleRequest request);

    void updateRole(Long id, RoleRequest request);

    void deleteRole(Long id);

    void saveRolePermissions(Long roleId, RolePermissionRequest request);
}
