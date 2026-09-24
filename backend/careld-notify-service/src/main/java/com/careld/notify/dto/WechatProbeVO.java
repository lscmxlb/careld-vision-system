package com.careld.notify.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 微信通道探针结果：拉取公众号模板列表并判定当前模板归属与字段名
 */
@Data
public class WechatProbeVO {

    private String appId;
    /** access_token 是否取到 */
    private boolean tokenOk;
    private Object tokenErrcode;
    private String tokenErrmsg;
    /** 取 token 失败时的处置建议（如 IP 白名单） */
    private String hint;

    private String configuredTemplateId;
    private String configuredTemplateType;

    /** 模板消息（/cgi-bin/template/get_all_private_template） */
    private List<Map<String, Object>> templateMessageTemplates;
    /** 订阅通知（/wxaapi/newtmpl/gettemplate） */
    private List<Map<String, Object>> subscribeTemplates;

    /** 命中归属：template / subscribe / none */
    private String matchedType;
    private String matchedTitle;
    private String matchedContent;
    /** 从模板 content 解析出的字段名（顺序即模板展示顺序） */
    private List<String> matchedFields;

    /** 已关注公众号的用户 openid（用于联调期选一个真实 openid 做测试发送） */
    private List<String> followers;
    private Integer followerTotal;
    /** 拉取关注者失败时的说明（如未认证无权限） */
    private String followerHint;
}
