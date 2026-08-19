package com.careld.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 菜单创建/更新请求
 */
@Data
@Schema(description = "菜单创建/更新请求")
public class MenuRequest {
    @Schema(description = "父菜单ID(0=顶级)")
    private Long parentId;
    @Schema(description = "菜单名称")
    private String menuName;
    @Schema(description = "类型:1目录 2菜单 3按钮")
    private Integer menuType;
    @Schema(description = "路由路径")
    private String menuPath;
    @Schema(description = "图标")
    private String menuIcon;
    @Schema(description = "权限标识")
    private String permissionKey;
    @Schema(description = "排序")
    private Integer sortOrder;
    @Schema(description = "是否可见:0隐藏 1显示")
    private Integer visible;
}
