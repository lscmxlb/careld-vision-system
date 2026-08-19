package com.careld.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(description = "用户实体")
public class User extends BaseEntity {

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "密码(BCrypt加密)")
    private String password;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "用户类型:1总部运营 2门店店长 3门店医生 4门店护士 5门店技师 6家长")
    private Integer userType;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "所属运营中心ID")
    private Long centerId;

    @Schema(description = "所属代理商ID")
    private Long agentId;

    @Schema(description = "所属科室ID")
    private Long deptId;

    @Schema(description = "职称")
    private String jobTitle;

    @Schema(description = "执业证书编号")
    private String licenseNo;

    @Schema(description = "状态:0禁用 1启用")
    private Integer status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "登录失败次数")
    private Integer loginFailCount;

    @Schema(description = "锁定时间")
    private LocalDateTime lockTime;
}
