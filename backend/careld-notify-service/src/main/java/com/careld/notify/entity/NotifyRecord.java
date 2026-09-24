package com.careld.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通知发送记录（一次发送一行，短信与微信各一行）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_record")
@Schema(description = "通知发送记录")
public class NotifyRecord extends BaseEntity {

    @Schema(description = "医院ID")
    private Long storeId;

    @Schema(description = "来源任务ID")
    private Long taskId;

    @Schema(description = "通知类型")
    private String eventType;

    @Schema(description = "通知渠道：1短信 2微信模板消息")
    private Integer channel;

    @Schema(description = "儿童ID")
    private Long childId;

    @Schema(description = "儿童姓名（脱敏快照）")
    private String childName;

    @Schema(description = "通知对象（手机号脱敏 / 微信openid尾号）")
    private String recipient;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "是否成功：1成功 0失败")
    private Integer status;

    @Schema(description = "本条收费金额（元）")
    private BigDecimal fee;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "备注（如：模拟通道）")
    private String remark;

    @Schema(description = "发送时间")
    private LocalDateTime sentAt;
}
