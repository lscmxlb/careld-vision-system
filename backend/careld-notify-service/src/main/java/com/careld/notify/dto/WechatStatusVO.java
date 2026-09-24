package com.careld.notify.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家长微信公众号绑定状态
 */
@Data
@Schema(description = "微信公众号绑定状态")
public class WechatStatusVO {

    @Schema(description = "是否已绑定")
    private boolean bound;

    @Schema(description = "openid 尾号（展示用）")
    private String openidTail;

    @Schema(description = "绑定时间")
    private LocalDateTime boundAt;

    @Schema(description = "公众号名称")
    private String officialAccountName;
}
