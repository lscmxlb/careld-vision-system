package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 通知服务配置保存请求
 */
@Data
@Schema(description = "通知服务配置")
public class NotifyConfigRequest {

    @Schema(description = "手机短信通知开关：1开 0关")
    private Integer smsEnabled;

    @Schema(description = "微信消息通知开关：1开 0关")
    private Integer wechatEnabled;

    @Schema(description = "启用的通知类型（可多选，可空）")
    private List<String> enabledTypes;
}
