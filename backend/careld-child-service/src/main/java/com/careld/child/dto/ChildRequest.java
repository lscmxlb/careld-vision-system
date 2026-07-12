package com.careld.child.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建/更新儿童档案请求 DTO
 * 匹配前端字段名（name/phone），后端负责映射到实体的 nameMask/phoneMask
 */
@Data
@Schema(description = "儿童档案请求")
public class ChildRequest {

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "性别:0女 1男")
    private Integer gender;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "眼部状况")
    private String eyeCondition;

    @Schema(description = "病史")
    private String medicalHistory;

    @Schema(description = "过敏史")
    private String allergyInfo;

    @Schema(description = "家族眼病史")
    private String familyHistory;
}
