package com.careld.notify.controller;

import com.careld.common.exception.BusinessException;
import com.careld.common.result.Result;
import com.careld.common.security.UserContext;
import com.careld.notify.service.WechatOauthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 家长端微信授权：网页授权绑定 openid + 订阅授权页配置
 *
 * <p>网页授权回调地址（redirect_uri）必须在公众平台「网页授权域名」下，
 * 订阅授权页必须在「JS接口安全域名」下，两者都是备案域名且仅支持 80/443。</p>
 */
@RestController
@RequestMapping("/api/v1/notify/wechat")
@RequiredArgsConstructor
@Tag(name = "微信授权", description = "家长端网页授权绑定与订阅授权页配置")
public class NotifyWechatOauthController {

    private final WechatOauthService oauthService;

    @Operation(summary = "订阅授权页配置（JS-SDK 签名，供 H5 静态页 wx.config 使用）")
    @GetMapping("/subscribe-config")
    public Result<Map<String, Object>> subscribeConfig(@RequestParam(name = "url", required = false) String url) {
        return Result.success(oauthService.subscribeConfig(url));
    }

    @Operation(summary = "取网页授权入口地址（家长端在微信内打开后静默绑定 openid）")
    @GetMapping("/oauth/url")
    public Result<Map<String, Object>> oauthUrl(@RequestParam(name = "redirect", required = false) String redirect) {
        currentParentId();
        return Result.success(oauthService.authorizeUrl(redirect));
    }

    @Operation(summary = "网页授权回调：code 换 openid 并绑定到当前家长")
    @PostMapping("/oauth/bind")
    public Result<Map<String, Object>> oauthBind(@RequestBody(required = false) Map<String, String> body) {
        Long userId = currentParentId();
        String code = body == null ? null : body.get("code");
        return Result.success(oauthService.bindByCode(userId, code));
    }

    private Long currentParentId() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        Integer userType = UserContext.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new BusinessException(1006, "仅家长账号可绑定微信公众号");
        }
        return userId;
    }
}
