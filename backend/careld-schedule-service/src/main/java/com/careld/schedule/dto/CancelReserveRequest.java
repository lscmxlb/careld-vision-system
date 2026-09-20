package com.careld.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取消预约请求
 */
@Data
@Schema(description = "取消预约请求")
public class CancelReserveRequest {

    @Schema(description = "备注（非必填）")
    private String cancelReason;

    @Schema(description = "取消原因类型：1家长原因 2医院原因（非必填）")
    private Integer cancelReasonType;

    @Schema(description = "是否返还预约次数：1返还 0不返还（为空按返还处理，兼容家长端/管理端）")
    private Integer refundFlag;
}
