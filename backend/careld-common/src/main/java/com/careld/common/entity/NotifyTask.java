package com.careld.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知发送任务
 *
 * <p>业务事件（建档成功 / 预约成功 / 取消 / 调整 / 养护完成）在各自事务内写入一行，
 * 由 careld-notify-service 定时消费并落「通知记录」。事件方只负责写入，
 * 不感知通道配置与计费，通知失败不回滚业务。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_task")
@Schema(description = "通知发送任务")
public class NotifyTask extends BaseEntity {

    @Schema(description = "医院ID")
    private Long storeId;

    @Schema(description = "儿童ID")
    private Long childId;

    @Schema(description = "通知类型：child_created/reserve_created/reserve_cancelled/reserve_adjusted/care_completed/other")
    private String eventType;

    @Schema(description = "关联业务ID（预约ID等）")
    private Long refId;

    @Schema(description = "业务快照JSON")
    private String payload;

    @Schema(description = "状态：0待发送 1已完成 2失败 3已跳过 9处理中")
    private Integer status;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "处理备注")
    private String remark;
}
