package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 通知服务配置（返回前端：通知类型为数组）
 */
@Data
@Schema(description = "通知服务配置")
public class NotifyConfigVO {

    @Schema(description = "医院ID")
    private Long storeId;

    @Schema(description = "手机短信通知开关：1开 0关")
    private Integer smsEnabled;

    @Schema(description = "微信消息通知开关：1开 0关")
    private Integer wechatEnabled;

    @Schema(description = "已启用的通知类型")
    private List<String> enabledTypes;
}
