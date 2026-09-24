package com.careld.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 医院端通知服务配置（每院一行）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_notify_config")
@Schema(description = "医院端通知服务配置")
public class StoreNotifyConfig extends BaseEntity {

    @Schema(description = "医院ID")
    private Long storeId;

    @Schema(description = "手机短信通知开关：1开 0关")
    private Integer smsEnabled;

    @Schema(description = "微信消息通知开关：1开 0关")
    private Integer wechatEnabled;

    @Schema(description = "启用的通知类型，逗号分隔")
    private String enabledTypes;
}
