package com.careld.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户创建/更新请求
 */
@Data
@Schema(description = "用户创建/更新请求")
public class UserCreateRequest {

    @Schema(description = "用户名（创建时必填）")
    private String username;

    @Schema(description = "密码（创建时必填，更新时忽略）")
    private String password;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "用户类型:1总部 2门店维护 3家长 4运营中心 5代理商")
    private Integer userType;

    @Schema(description = "运营中心ID")
    private Long centerId;

    @Schema(description = "代理商ID")
    private Long agentId;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
