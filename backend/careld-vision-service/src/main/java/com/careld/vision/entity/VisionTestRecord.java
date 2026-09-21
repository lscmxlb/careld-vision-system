package com.careld.vision.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vision_test_record")
public class VisionTestRecord extends BaseEntity {
    private String recordCode;
    private Long childId;
    private Long storeId;
    private Long deptId;
    private Long deviceId;
    private Long reserveId;
    private Integer testType;
    private Integer eyeType;
    private String visionLevel;
    private BigDecimal visionDecimal;
    private BigDecimal screenSize;
    private BigDecimal testDistance;
    private String lightingCondition;
    private String testerName;
    private Long testerId;
    private Integer syncSource;
    private String deviceLocalId;
    private LocalDateTime syncedAt;
    private String remark;

    // ===== 关联查询回显字段（非表字段） =====
    @TableField(exist = false)
    private String childName;
    @TableField(exist = false)
    private String childNameEncrypted;
    @TableField(exist = false)
    private String storeName;
    /** 检测时间：表中无独立列，取 created_at */
    @TableField(exist = false)
    private LocalDateTime testTime;
}
