package com.careld.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录响应
 */
@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "过期时间(秒)")
    private Long expiresIn;

    @Schema(description = "令牌类型")
    private String tokenType;

    @Schema(description = "用户信息")
    private UserInfo user;

    @Data
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID")
        private Long id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "真实姓名")
        private String realName;

        @Schema(description = "用户类型")
        private Integer userType;

        @Schema(description = "门店ID")
        private Long storeId;

        @Schema(description = "运营中心ID")
        private Long centerId;

        @Schema(description = "代理商ID")
        private Long agentId;

        @Schema(description = "科室ID")
        private Long deptId;

        @Schema(description = "职称")
        private String jobTitle;

        @Schema(description = "角色列表")
        private List<String> roles;

        @Schema(description = "权限列表")
        private List<String> permissions;
    }
}
