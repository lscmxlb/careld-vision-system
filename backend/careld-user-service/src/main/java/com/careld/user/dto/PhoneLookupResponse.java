package com.careld.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 手机号码查询结果（管理后台「用户管理 → 手机号码查询」）
 * 同一号码可能同时命中多个身份（历史撞号：家长账号与医务人员账号同号）
 */
@Data
@Schema(description = "手机号码查询结果")
public class PhoneLookupResponse {

    @Schema(description = "查询的手机号码")
    private String phone;

    @Schema(description = "是否已注册")
    private boolean found;

    @Schema(description = "命中的身份列表")
    private List<Identity> identities = new ArrayList<>();

    @Data
    @Schema(description = "命中身份")
    public static class Identity {

        @Schema(description = "来源：sys_user=系统账号，medical_staff=医务人员")
        private String source;

        @Schema(description = "记录ID")
        private Long id;

        @Schema(description = "登录账号（医务人员为手机号）")
        private String username;

        @Schema(description = "姓名")
        private String realName;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "用户类型：1总部 2医院维护 3家长 4运营中心 5代理商 6医务人员")
        private Integer userType;

        @Schema(description = "用户类型名称")
        private String userTypeName;

        @Schema(description = "医务人员角色：1医师 2医生助理")
        private Integer staffRole;

        @Schema(description = "医务人员角色名称")
        private String staffRoleName;

        @Schema(description = "角色名称列表")
        private List<String> roleNames = new ArrayList<>();

        @Schema(description = "运营中心名称")
        private String centerName;

        @Schema(description = "代理商名称")
        private String agentName;

        @Schema(description = "所属医院名称")
        private String storeName;

        @Schema(description = "状态：1启用 0禁用")
        private Integer status;

        @Schema(description = "注册时间")
        private LocalDateTime createdAt;

        @Schema(description = "最后登录时间")
        private LocalDateTime lastLoginTime;

        @Schema(description = "儿童档案数量（仅家长）")
        private Integer childCount;
    }
}
