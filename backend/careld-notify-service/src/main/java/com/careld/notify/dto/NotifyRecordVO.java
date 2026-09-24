package com.careld.notify.dto;

import com.careld.common.notify.NotifyEventTypes;
import com.careld.notify.entity.NotifyRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知记录列表项（在记录基础上补展示用文案）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通知记录")
public class NotifyRecordVO extends NotifyRecord {

    @Schema(description = "通知类型文案")
    private String eventLabel;

    @Schema(description = "渠道文案")
    private String channelLabel;

    @Schema(description = "结果文案")
    private String statusLabel;

    public static NotifyRecordVO of(NotifyRecord record) {
        NotifyRecordVO vo = new NotifyRecordVO();
        vo.setId(record.getId());
        vo.setStoreId(record.getStoreId());
        vo.setTaskId(record.getTaskId());
        vo.setEventType(record.getEventType());
        vo.setChannel(record.getChannel());
        vo.setChildId(record.getChildId());
        vo.setChildName(record.getChildName());
        vo.setRecipient(record.getRecipient());
        vo.setTitle(record.getTitle());
        vo.setContent(record.getContent());
        vo.setStatus(record.getStatus());
        vo.setFee(record.getFee());
        vo.setFailReason(record.getFailReason());
        vo.setRemark(record.getRemark());
        vo.setSentAt(record.getSentAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setEventLabel(NotifyEventTypes.label(record.getEventType()));
        vo.setChannelLabel(record.getChannel() != null && record.getChannel() == 2 ? "微信消息" : "手机短信");
        boolean success = record.getStatus() != null && record.getStatus() == 1;
        vo.setStatusLabel(success ? "成功" : "失败");
        return vo;
    }
}
