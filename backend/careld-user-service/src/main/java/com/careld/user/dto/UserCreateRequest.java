package com.careld.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户创建请求
 */
@Data
@Schema(description = "用户创建请求")
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @NotNull(message = "用户类型不能为空")
    @Schema(description = "用户类型:1总部运营 2门店医护 3家长", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer userType;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
