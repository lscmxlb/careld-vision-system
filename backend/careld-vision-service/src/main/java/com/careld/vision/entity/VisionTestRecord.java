package com.careld.vision.entity;
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
}
