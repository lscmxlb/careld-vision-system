package com.careld.notify.channel;

/**
 * 单条通知的发送结果
 *
 * @param success    是否发送成功
 * @param remark     成功备注（模拟通道会标注「模拟通道」）
 * @param failReason 失败原因
 */
public record SendOutcome(boolean success, String remark, String failReason) {

    public static SendOutcome success(String remark) {
        return new SendOutcome(true, remark, null);
    }

    public static SendOutcome failure(String failReason) {
        return new SendOutcome(false, null, failReason);
    }
}
