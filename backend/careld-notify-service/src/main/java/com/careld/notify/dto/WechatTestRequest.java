package com.careld.notify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信测试发送请求（联调期用真实 openid 验证模板消息）
 */
@Data
public class WechatTestRequest {

    @NotBlank(message = "请填写接收测试消息的 openid")
    private String openid;

    /** 可选：覆盖测试正文（默认用各通知类型的示例文案） */
    private String content;
}
