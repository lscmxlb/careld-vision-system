package com.careld.store.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_tv_device")
public class TvDevice extends BaseEntity {
    private String deviceCode;
    private Long deviceTypeId;
    private String deviceSn;
    private String deviceName;
    private Long storeId;
    private String androidVersion;
    private String screenResolution;
    private BigDecimal screenSize;
    private String appVersion;
    private java.time.LocalDate maintenanceDate;
    private java.time.LocalDate installDate;
    private java.time.LocalDate expireDate;
    private Integer warningDays;
    private Integer calibrationStatus;
    private String calibrationData;
    private java.time.LocalDateTime lastOnlineTime;
    private java.time.LocalDateTime lastSyncTime;
    private Integer status;
    private java.time.LocalDateTime bindTime;
}
