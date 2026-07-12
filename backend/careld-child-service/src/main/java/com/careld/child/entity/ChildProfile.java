package com.careld.child.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("child_profile")
public class ChildProfile extends BaseEntity {
    private String childCode;
    private Long storeId;
    private String nameEncrypted;
    private String nameMask;
    private String phoneEncrypted;
    private String phoneMask;
    private LocalDate birthDate;
    private Integer gender;
    private String eyeCondition;
    private String medicalHistory;
    private String allergyInfo;
    private String familyHistory;
    private Integer auditStatus;
    private String auditRemark;
    private Long auditedBy;
    private LocalDateTime auditedAt;
    private Long parentUserId;
    private Integer status;
}
