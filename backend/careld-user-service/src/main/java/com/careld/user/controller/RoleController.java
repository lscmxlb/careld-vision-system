package com.careld.user.controller;

import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.user.dto.RolePermissionRequest;
import com.careld.user.dto.RoleRequest;
import com.careld.user.entity.Role;
import com.careld.user.entity.RoleMenu;
import com.careld.user.service.RoleService;
import com.careld.common.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色控制器
 */
@Tag(name = "角色管理", description = "角色CRUD和权限分配")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表")
    @GetMapping
    public Result<List<Role>> listRoles() {
        return Result.success(roleService.listRoles());
    }

    @Operation(summary = "角色详情（含权限）")
    @GetMapping("/{id}")
    @RequirePermission("settings:role:view")
    public Result<Map<String, Object>> getRoleDetail(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        List<RoleMenu> roleMenus = roleService.getRoleMenus(id);

        Map<String, Object> result = new HashMap<>();
        result.put("role", role);
        result.put("roleMenus", roleMenus);
        return Result.success(result);
    }

    @OperationLog(module = "roles", action = "create", description = "新增角色")
    @Operation(summary = "新增角色")
    @PostMapping
    @RequirePermission("settings:role:create")
    public Result<Long> createRole(@RequestBody RoleRequest request) {
        return Result.success(roleService.createRole(request));
    }

    @OperationLog(module = "roles", action = "update", description = "修改角色")
    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    @RequirePermission("settings:role:update")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        roleService.updateRole(id, request);
        return Result.success();
    }

    @OperationLog(module = "roles", action = "delete", description = "删除角色")
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @RequirePermission("settings:role:delete")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @OperationLog(module = "roles", action = "permissions", description = "角色权限分配")
    @Operation(summary = "分配角色权限")
    @PutMapping("/{id}/permissions")
    @RequirePermission("settings:role:update")
    public Result<Void> saveRolePermissions(@PathVariable Long id, @RequestBody RolePermissionRequest request) {
        roleService.saveRolePermissions(id, request);
        return Result.success();
    }
}
