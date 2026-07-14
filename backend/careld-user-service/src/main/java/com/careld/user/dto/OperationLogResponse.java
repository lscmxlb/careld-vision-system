package com.careld.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志响应（含门店名称）
 */
@Data
@Schema(description = "操作日志")
public class OperationLogResponse {

    private Long id;
    private Integer logType;
    private Long userId;
    private String userName;
    private Long storeId;
    private String storeName;
    private String module;
    private String action;
    private String description;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseData;
    private String ipAddress;
    private String userAgent;
    private String deviceType;
    private Integer executeTime;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createdAt;
}
