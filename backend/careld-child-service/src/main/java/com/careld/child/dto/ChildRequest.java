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

    @Schema(description = "家长姓名")
    private String parentName;

    @Schema(description = "与儿童关系：妈妈/爸爸等")
    private String relation;

    @Schema(description = "主治医生ID（medical_staff）")
    private Long doctorId;

    @Schema(description = "主治医生姓名快照")
    private String doctorName;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "性别:0女 1男")
    private Integer gender;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "家庭地址")
    private String homeAddress;

    @Schema(description = "所在学校")
    private String school;

    @Schema(description = "分娩方式：顺产/剖宫产")
    private String deliveryType;

    @Schema(description = "日常作息-休息时间(HH:mm)")
    private String bedtime;

    @Schema(description = "日常作息-起床时间(HH:mm)")
    private String wakeTime;

    @Schema(description = "眼部状况")
    private String eyeCondition;

    @Schema(description = "裸眼视力-双眼")
    private String nakedVisionBoth;

    @Schema(description = "裸眼视力-左眼")
    private String nakedVisionLeft;

    @Schema(description = "裸眼视力-右眼")
    private String nakedVisionRight;

    @Schema(description = "病史")
    private String medicalHistory;

    @Schema(description = "过敏史")
    private String allergyInfo;

    @Schema(description = "家族眼病史")
    private String familyHistory;
}
