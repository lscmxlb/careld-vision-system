package com.careld.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户同意记录（合规留痕）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_consent")
@Schema(description = "用户同意记录")
public class UserConsent extends BaseEntity {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "同意类型：privacy_policy=隐私政策")
    private String consentType;

    @Schema(description = "同意的政策版本号")
    private String policyVersion;

    @Schema(description = "同意时间")
    private LocalDateTime agreedAt;
}
