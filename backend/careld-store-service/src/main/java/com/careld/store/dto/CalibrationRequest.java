package com.careld.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 设备校准请求
 */
@Data
@Schema(description = "设备校准请求")
public class CalibrationRequest {

    @Schema(description = "校准状态:0未校准 1已校准")
    private Integer calibrationStatus;

    @Schema(description = "校准数据(JSON 对象)")
    private Map<String, Object> calibrationData;
}
