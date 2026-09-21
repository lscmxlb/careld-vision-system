package com.careld.vision.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视力检测记录查询条件：前半部分为前端入参，后半部分为服务端解析出的数据权限、姓名匹配与日期区间
 */
@Data
public class VisionQuery {

    // ===== 前端入参 =====
    private Long storeId;
    private Long childId;
    /** 儿童姓名（支持掩码或全名模糊匹配） */
    private String childName;
    /** 关键词（记录编号 或 儿童姓名） */
    private String keyword;
    /** 1=养护前 2=养护后 */
    private Integer testType;
    /** 1=左眼 2=右眼 0=双眼 */
    private Integer eyeType;
    /** yyyy-MM-dd */
    private String startDate;
    /** yyyy-MM-dd */
    private String endDate;
    private Integer page = 1;
    private Integer size = 20;

    // ===== 服务端解析 =====
    private Long effectiveStoreId;
    private List<Long> childIds;
    private List<Long> keywordChildIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer offset;
}
