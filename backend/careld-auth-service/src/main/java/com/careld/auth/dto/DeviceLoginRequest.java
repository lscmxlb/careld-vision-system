package com.careld.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * TV设备登录请求
 */
@Data
@Schema(description = "TV设备登录请求")
public class DeviceLoginRequest {

    @NotBlank(message = "门店编码不能为空")
    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeCode;

    @NotBlank(message = "设备码不能为空")
    @Schema(description = "设备唯一码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceCode;

    @Schema(description = "APK版本号")
    private String appVersion;

    @Schema(description = "Android系统版本")
    private String androidVersion;

    @Schema(description = "屏幕分辨率")
    private String screenResolution;

    @Schema(description = "屏幕尺寸(英寸)")
    private Double screenSize;
}
