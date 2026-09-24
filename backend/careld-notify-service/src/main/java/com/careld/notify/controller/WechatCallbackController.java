package com.careld.notify.controller;

import com.careld.notify.service.WechatBindService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信公众平台「服务器配置」回调（匿名访问，公众平台直接请求）
 *
 * <p>GET 用于公众平台校验 URL 有效性（签名通过原样回显 echostr）；
 * POST 接收关注/取关等事件（明文模式 XML），关注事件带带参二维码 scene 时自动绑定 openid。
 * 该地址须为 80/443 端口的公网入口（当前由 nginx 反代到本服务 8288）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notify/wechat/callback")
@RequiredArgsConstructor
@Tag(name = "微信公众号回调", description = "公众平台服务器配置回调（关注事件）")
public class WechatCallbackController {

    private final WechatBindService bindService;

    @GetMapping
    @Operation(summary = "公众平台服务器配置校验")
    public ResponseEntity<String> verify(@RequestParam(name = "signature", required = false) String signature,
                                         @RequestParam(name = "timestamp", required = false) String timestamp,
                                         @RequestParam(name = "nonce", required = false) String nonce,
                                         @RequestParam(name = "echostr", required = false) String echostr) {
        if (!bindService.verifySignature(signature, timestamp, nonce)) {
            log.warn("[WECHAT-BIND] 服务器配置校验失败：签名不匹配（Token 与公众平台不一致？）");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("signature mismatch");
        }
        return ResponseEntity.ok(echostr == null ? "" : echostr);
    }

    @PostMapping
    @Operation(summary = "接收微信事件推送")
    public ResponseEntity<String> receive(@RequestBody(required = false) String body) {
        String reply = bindService.handleEvent(body == null ? "" : body);
        return ResponseEntity.ok(reply);
    }
}
