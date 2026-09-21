package com.careld.vision.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视力检测配对记录：同一儿童 + 预约 + 检测阶段 + 检测时间 的左右眼两行合并为一行，供医院端列表展示
 */
@Data
public class VisionRecordGroup {

    private Long childId;
    private String childName;
    private String childNameEncrypted;
    private String storeName;
    private Long reserveId;
    /** 1=养护前 2=养护后 */
    private Integer testType;
    /** 检测时间（取 created_at） */
    private LocalDateTime testTime;
    private String leftEye;
    private String rightEye;
    private String testerName;
    private String remark;
    /** 改善情况：养护后 - 养护前（双眼平均），形如 +0.1 / -0.1 / 0.0 */
    private String improvement;

    public String getBeforeAfter() {
        return testType != null && testType == 1 ? "before" : "after";
    }
}
