package com.careld.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色权限分配请求
 */
@Data
@Schema(description = "角色权限分配请求")
public class RolePermissionRequest {

    @Schema(description = "角色-菜单权限列表")
    private List<RoleMenuItem> roleMenus;

    @Data
    @Schema(description = "角色-菜单项")
    public static class RoleMenuItem {
        @Schema(description = "菜单ID")
        private Long menuId;
        @Schema(description = "允许的操作列表")
        private List<String> actions;
    }
}
