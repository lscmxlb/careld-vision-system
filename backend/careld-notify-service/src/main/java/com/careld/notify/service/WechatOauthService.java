package com.careld.notify.service;

import com.careld.common.exception.BusinessException;
import com.careld.notify.channel.WechatChannelSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 公众号网页授权绑定 + 订阅授权页配置
 *
 * <p>真实公众号只能发「订阅通知」，而订阅通知要求家长先授权（一次性订阅，一次授权下发一条）。
 * 授权弹窗只能在微信客户端内由 H5 拉起：页面先走网页授权（snsapi_base）静默拿到 openid 并绑定到
 * 当前登录家长，再用 JS-SDK 开放标签 {@code <wx-open-subscribe>} 弹出订阅授权。
 * 该页面须部署在公众平台配置好的「网页授权域名 / JS接口安全域名」下（备案域名，仅 80/443）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatOauthService {

    /** 家长端订阅授权页（静态页，登录态沿用家长端 localStorage） */
    public static final String AUTH_PAGE_PATH = "/static/wechat-auth.html";

    private final NotifyChannelRuntimeService runtimeService;
    private final WechatChannelSender wechatSender;
    private final WechatBindService bindService;

    /** 订阅授权页所需参数：JS-SDK 签名 + 模板信息（页面 wx.config 后渲染 wx-open-subscribe） */
    public Map<String, Object> subscribeConfig(String pageUrl) {
        Map<String, Object> vo = new LinkedHashMap<>();
        NotifyChannelRuntimeService.WechatRuntime runtime = runtimeService.loadWechatRuntime();
        vo.put("appId", runtime.appId());
        vo.put("templateId", runtime.templateId());
        vo.put("accountName", runtimeService.officialAccountName());
        vo.put("realSendReady", runtime.realSendReady());
        vo.put("authPageUrl", bindService.publicBaseUrl() + AUTH_PAGE_PATH);
        if (!StringUtils.hasText(pageUrl)
                || !(pageUrl.startsWith("http://") || pageUrl.startsWith("https://"))) {
            vo.put("hint", "页面地址不合法：需以 http(s):// 开头且不含 # 片段");
            return vo;
        }
        if (!runtime.realSendReady()) {
            vo.put("hint", "公众号凭据尚未配置完整（AppID / AppSecret / 模板 ID）");
            return vo;
        }
        String ticket = wechatSender.jsapiTicket(runtime);
        if (ticket == null) {
            vo.put("hint", "获取 jsapi_ticket 失败：请确认本服务器公网 IP 在公众号 IP 白名单内");
            return vo;
        }
        long timestamp = System.currentTimeMillis() / 1000;
        String nonceStr = randomNonce();
        vo.put("timestamp", timestamp);
        vo.put("nonceStr", nonceStr);
        vo.put("signature", WechatChannelSender.jsapiSignature(ticket, nonceStr, timestamp, pageUrl));
        vo.put("jsApiList", List.of("checkJsApi"));
        vo.put("openTagList", List.of("wx-open-subscribe"));
        return vo;
    }

    /** 网页授权入口：返回 open.weixin.qq.com 链接（微信内打开后带 code 跳回 redirectUri） */
    public Map<String, Object> authorizeUrl(String redirectUri) {
        NotifyChannelRuntimeService.WechatRuntime runtime = runtimeService.loadWechatRuntime();
        if (!StringUtils.hasText(runtime.appId())) {
            throw new BusinessException(1005, "公众号 AppID 尚未配置，无法发起微信授权");
        }
        String target = StringUtils.hasText(redirectUri)
                ? redirectUri.trim()
                : bindService.publicBaseUrl() + AUTH_PAGE_PATH;
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            throw new BusinessException(1005, "授权回调地址需以 http(s):// 开头");
        }
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("url", WechatChannelSender.oauthAuthorizeUrl(runtime.appId(), target, randomNonce()));
        vo.put("redirectUri", target);
        return vo;
    }

    /** 网页授权 code 换 openid，并绑定到当前登录家长 */
    public Map<String, Object> bindByCode(Long userId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(1005, "缺少微信授权 code，请在微信内重新打开该页面");
        }
        NotifyChannelRuntimeService.WechatRuntime runtime = runtimeService.loadWechatRuntime();
        if (!StringUtils.hasText(runtime.appId()) || !StringUtils.hasText(runtime.appSecret())) {
            throw new BusinessException(1005, "公众号凭据尚未配置完整，无法完成微信绑定");
        }
        Map<String, Object> response = wechatSender.rawOauthAccessToken(
                runtime.appId(), runtime.appSecret(), code.trim());
        Object openid = response.get("openid");
        if (openid == null) {
            String errcode = String.valueOf(response.get("errcode"));
            String errmsg = String.valueOf(response.get("errmsg"));
            log.warn("[WECHAT-OAUTH] code 换 openid 失败 userId={} errcode={} errmsg={}", userId, errcode, errmsg);
            throw new BusinessException(1005, "微信授权失败：" + errcode + " " + errmsg
                    + "（redirect_uri 域名需与公众平台「网页授权域名」一致，code 只能用一次）");
        }
        bindService.bindOpenid(userId, openid.toString());
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("bound", true);
        vo.put("openidTail", tail(openid.toString()));
        vo.put("boundAt", LocalDateTime.now());
        return vo;
    }

    private String randomNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String tail(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "-";
        }
        return openid.length() <= 6 ? openid : openid.substring(openid.length() - 6);
    }
}
