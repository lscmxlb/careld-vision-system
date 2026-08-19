package com.careld.user.controller;

import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.common.security.UserContext;
import com.careld.user.dto.MenuRequest;
import com.careld.user.entity.Menu;
import com.careld.user.service.MenuService;
import com.careld.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单控制器
 */
@Tag(name = "菜单管理", description = "菜单CRUD和权限查询")
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final PermissionService permissionService;

    @Operation(summary = "当前用户菜单和权限")
    @GetMapping("/my")
    public Result<Map<String, Object>> getMyMenus() {
        Long userId = UserContext.getCurrentUserId();
        List<Menu> menus = permissionService.getUserMenuTree(userId);
        List<String> permissions = permissionService.getPermissionKeysByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("menus", menus);
        result.put("permissions", permissions);
        return Result.success(result);
    }

    @Operation(summary = "菜单树（管理用）")
    @GetMapping
    @RequirePermission("settings:menu:view")
    public Result<List<Menu>> getMenuTree() {
        return Result.success(menuService.getMenuTree());
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @RequirePermission("settings:menu:create")
    public Result<Long> createMenu(@RequestBody MenuRequest request) {
        return Result.success(menuService.createMenu(request));
    }

    @Operation(summary = "修改菜单")
    @PutMapping("/{id}")
    @RequirePermission("settings:menu:update")
    public Result<Void> updateMenu(@PathVariable Long id, @RequestBody MenuRequest request) {
        menuService.updateMenu(id, request);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @RequirePermission("settings:menu:delete")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }
}
