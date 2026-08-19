package com.careld.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.common.result.PageResult;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.common.security.DataScopeHelper;
import com.careld.user.dto.UserCreateRequest;
import com.careld.user.dto.UserResponse;
import com.careld.user.service.PermissionService;
import com.careld.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户CRUD操作")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser(@RequestAttribute("userId") Long userId) {
        UserResponse userResp = userService.getCurrentUser(userId);
        // 注入实际权限列表
        List<String> perms = permissionService.getPermissionKeysByUserId(userId);
        userResp.setPermissions(perms);
        return Result.success(userResp);
    }

    @Operation(summary = "修改本人密码")
    @PostMapping("/me/password")
    public Result<Void> changeMyPassword(@RequestAttribute("userId") Long userId,
                                          @RequestBody Map<String, String> params) {
        userService.changeMyPassword(userId, params.get("oldPassword"), params.get("newPassword"));
        return Result.success();
    }

    @Operation(summary = "用户列表")
    @GetMapping
    @RequirePermission("user:view")
    public Result<PageResult<UserResponse>> listUsers(
            @Parameter(description = "用户类型") @RequestParam(value = "userType", required = false) Integer userType,
            @Parameter(description = "医院ID") @RequestParam(value = "storeId", required = false) Long storeId,
            @Parameter(description = "运营中心ID") @RequestParam(value = "centerId", required = false) Long centerId,
            @Parameter(description = "代理商ID") @RequestParam(value = "agentId", required = false) Long agentId,
            @Parameter(description = "状态") @RequestParam(value = "status", required = false) Integer status,
            @Parameter(description = "关键词") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(value = "size", defaultValue = "20") Integer size) {
        // 数据权限：根据当前用户类型自动注入组织过滤条件
        Long effectiveStoreId = DataScopeHelper.resolveStoreId(storeId);
        Long effectiveCenterId = DataScopeHelper.resolveCenterId(centerId);
        Long effectiveAgentId = DataScopeHelper.resolveAgentId(agentId);
        Page<UserResponse> result = userService.listUsers(userType, effectiveStoreId, effectiveCenterId, effectiveAgentId, status, keyword, page, size);
        return Result.success(PageResult.of(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<UserResponse> getUser(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @RequirePermission("user:create")
    public Result<Long> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @RequirePermission("user:update")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserCreateRequest request) {
        userService.updateUser(id, request);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @RequirePermission("user:delete")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    @RequirePermission("user:resetPwd")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> params) {
        userService.resetPassword(id, params.get("newPassword"));
        return Result.success();
    }

    @Operation(summary = "更新状态")
    @PatchMapping("/{id}/status")
    @RequirePermission("user:toggleStatus")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        userService.updateStatus(id, params.get("status"));
        return Result.success();
    }

    @Operation(summary = "门店用户列表")
    @GetMapping("/store/{storeId}")
    public Result<List<UserResponse>> listByStore(@PathVariable Long storeId) {
        return Result.success(userService.listByStoreId(storeId));
    }

    // ===== 用户角色管理 =====

    @Operation(summary = "查看用户角色")
    @GetMapping("/{id}/roles")
    @RequirePermission("user:view")
    public Result<List<Map<String, Object>>> getUserRoles(@PathVariable Long id) {
        List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                "SELECT r.* FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = ? AND r.status = 1",
                id);
        return Result.success(roles);
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/{id}/roles")
    @RequirePermission("user:update")
    public Result<Void> assignUserRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        List<Long> roleIds = params.get("roleIds");
        if (roleIds == null) roleIds = List.of();

        // 先删除旧角色
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", id);

        // 插入新角色
        for (Long roleId : roleIds) {
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", id, roleId);
        }

        // 清除权限缓存
        permissionService.clearPermissionCache(id);

        return Result.success();
    }
}
