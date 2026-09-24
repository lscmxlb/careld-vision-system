package com.careld.notify.dto;

import lombok.Data;

/**
 * 微信通知配置请求（管理后台系统设置）
 */
@Data
public class WechatConfigRequest {

    private String appId;

    /** 留空表示不修改已保存的 AppSecret */
    private String appSecret;

    private String templateId;

    /** template=公众号模板消息 / subscribe=公众号订阅通知 */
    private String templateType;

    /** 模板字段名映射（逗号分隔，顺序即模板展示顺序），如 thing2,thing20,time44,phrase46 */
    private String fields;

    /**
     * 字段语义（与 fields 一一对应，逗号分隔），决定每个字段填什么内容：
     * summary=通知正文 / childName=儿童姓名 / noticeTitle=通知类型 / noticeTime=业务时间
     * / noticeStatus=状态短语 / storeName=医院名称 / remark=固定说明
     */
    private String fieldRoles;

    private String accountName;

    /** 回调公网地址前缀（如 http://39.162.49.28），留空保持不变 */
    private String publicBaseUrl;

    /** 留空保持不变；填新的则覆盖服务器配置 Token */
    private String serverToken;
}
