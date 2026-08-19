package com.careld.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色创建/更新请求
 */
@Data
@Schema(description = "角色创建/更新请求")
public class RoleRequest {
    @Schema(description = "角色编码")
    private String roleCode;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "角色描述")
    private String roleDesc;
    @Schema(description = "适用用户类型")
    private Integer userType;
    @Schema(description = "数据范围:1全部 2本中心及下级 3本代理商及下级 4本医院 5个人")
    private Integer dataScope;
    @Schema(description = "排序")
    private Integer sortOrder;
    @Schema(description = "状态:0禁用 1启用")
    private Integer status;
}
