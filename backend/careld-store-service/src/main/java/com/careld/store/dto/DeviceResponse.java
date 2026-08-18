package com.careld.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TV 设备响应 DTO（含门店名称、设备类型名称，供前端展示）
 */
@Data
@Schema(description = "TV 设备信息")
public class DeviceResponse {

    @Schema(description = "设备ID")
    private Long id;

    @Schema(description = "设备唯一码")
    private String deviceCode;

    @Schema(description = "设备类型ID")
    private Long deviceTypeId;

    @Schema(description = "设备类型名称")
    private String deviceTypeName;

    @Schema(description = "设备SN（手动输入）")
    private String deviceSn;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "所属门店名称")
    private String storeName;

    @Schema(description = "Android系统版本")
    private String androidVersion;

    @Schema(description = "屏幕分辨率")
    private String screenResolution;

    @Schema(description = "屏幕尺寸(英寸)")
    private java.math.BigDecimal screenSize;

    @Schema(description = "APK版本号")
    private String appVersion;

    @Schema(description = "维护日期")
    private LocalDate maintenanceDate;

    @Schema(description = "安装日期")
    private LocalDate installDate;

    @Schema(description = "到期日期")
    private LocalDate expireDate;

    @Schema(description = "预警天数(提前N天)")
    private Integer warningDays;

    @Schema(description = "到期状态: 0正常 1即将到期 2已到期")
    private Integer expireStatus;

    @Schema(description = "校准状态:0未校准 1已校准")
    private Integer calibrationStatus;

    @Schema(description = "校准数据(JSON)")
    private Object calibrationData;

    @Schema(description = "最后在线时间")
    private LocalDateTime lastOnlineTime;

    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "状态:0禁用 1启用")
    private Integer status;

    @Schema(description = "绑定时间")
    private LocalDateTime bindTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
