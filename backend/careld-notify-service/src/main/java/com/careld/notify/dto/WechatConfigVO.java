package com.careld.notify.dto;

import lombok.Data;

/**
 * 微信通知配置（AppSecret 只回显掩码）
 */
@Data
public class WechatConfigVO {

    private String appId;
    private boolean appSecretConfigured;
    private String appSecretMasked;
    private String templateId;
    /** template=公众号模板消息 / subscribe=公众号订阅通知 */
    private String templateType;
    private String fields;
    /** 字段语义（与 fields 一一对应）：summary/childName/noticeTitle/noticeTime/noticeStatus/storeName/remark */
    private String fieldRoles;
    private String accountName;
    /** 凭据与模板是否齐全（齐全后真实发送，否则模拟通道） */
    private boolean realSendReady;
    /** 公众号回调公网地址（公众平台「服务器配置」URL 填这个） */
    private String callbackUrl;
    /** 公众号服务器配置 Token（公众平台「服务器配置」Token 填这个） */
    private String serverToken;
    /** 回调公网地址前缀（可在管理后台修改） */
    private String publicBaseUrl;
}
