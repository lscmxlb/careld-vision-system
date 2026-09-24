package com.careld.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.careld.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 微信公众号扫码绑定票据（一次「展示二维码」一行）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_bind_ticket")
@Schema(description = "微信公众号扫码绑定票据")
public class WechatBindTicket extends BaseEntity {

    @Schema(description = "带参二维码 scene 值")
    private String scene;

    @Schema(description = "家长用户ID")
    private Long userId;

    @Schema(description = "0待扫码 1已绑定 2已失效")
    private Integer status;

    @Schema(description = "扫码关注者 openid")
    private String openid;

    @Schema(description = "二维码过期时间")
    private LocalDateTime expireAt;
}
