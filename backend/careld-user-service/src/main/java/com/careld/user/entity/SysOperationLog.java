package com.careld.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统操作日志
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "日志类型:1操作 2登录 3异常")
    private Integer logType;

    private Long userId;

    private Integer userType;

    private String userName;

    private Long storeId;

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

    @Schema(description = "状态:0失败 1成功")
    private Integer status;

    private String errorMsg;

    private LocalDateTime createdAt;
}
