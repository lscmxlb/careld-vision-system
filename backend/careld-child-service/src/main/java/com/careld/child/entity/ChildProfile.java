package com.careld.child.entity;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private String parentName;
    private String relation;
    private LocalDate birthDate;
    private Integer gender;
    private String homeAddress;
    private String school;
    private String deliveryType;
    private String bedtime;
    private String wakeTime;
    private String eyeCondition;
    private String nakedVisionBoth;
    private String nakedVisionLeft;
    private String nakedVisionRight;
    private String medicalHistory;
    private String allergyInfo;
    private String familyHistory;
    private Integer auditStatus;
    private String auditRemark;
    private Long auditedBy;
    private LocalDateTime auditedAt;
    private Long parentUserId;
    /** 主治医生（medical_staff.id），医生建档/编辑必选；家长自建档案审核通过时必选 */
    private Long doctorId;
    /** 主治医生姓名快照（展示用） */
    private String doctorName;
    private Integer sourceType;
    private Long sourceUserId;
    private Integer remainingCount;
    private Integer status;

    // ===== 以下为聚合/派生字段，不映射数据库列（供前端展示）=====
    /** 门店名称（JOIN store_info） */
    @TableField(exist = false)
    private String storeName;

    /** 门店服务电话（取自门店科室，与医院端基础信息同源） */
    @TableField(exist = false)
    private String storeServicePhone;

    /** 年龄（由 birth_date 计算） */
    @TableField(exist = false)
    private Integer age;

    /** 养护次数（care_record 计数，含养护中：status 1/2） */
    @TableField(exist = false)
    private Integer careCount;

    /** 兼容字段：等同于 nameMask */
    @TableField(exist = false)
    private String name;

    /** 兼容字段：等同于 phoneMask */
    @TableField(exist = false)
    private String phone;

    /** 最近一次左眼视力（JOIN vision_test_record eye_type=1） */
    @TableField(exist = false)
    private String lastLeftEye;

    /** 最近一次右眼视力（JOIN vision_test_record eye_type=2） */
    @TableField(exist = false)
    private String lastRightEye;

    /** 最近检测时间 */
    @TableField(exist = false)
    private LocalDateTime lastTestTime;

    /** 最近视力检测汇总（前端 lastVisionTest） */
    @TableField(exist = false)
    private LastVisionTest lastVisionTest;

    @Data
    public static class LastVisionTest {
        private String testTime;
        private String leftEye;
        private String rightEye;

        public LastVisionTest(LocalDateTime testTime, String leftEye, String rightEye) {
            this.testTime = testTime == null ? null : testTime.toString();
            this.leftEye = leftEye;
            this.rightEye = rightEye;
        }
    }
}

