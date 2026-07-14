package com.careld.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设备绑定请求
 */
@Data
@Schema(description = "设备绑定请求")
public class DeviceBindRequest {

    @Schema(description = "门店ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @Schema(description = "设备唯一码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;
}
