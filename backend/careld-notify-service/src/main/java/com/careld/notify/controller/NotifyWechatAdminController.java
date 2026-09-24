package com.careld.notify.controller;

import com.careld.common.log.OperationLog;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.notify.dto.WechatConfigRequest;
import com.careld.notify.dto.WechatConfigVO;
import com.careld.notify.dto.WechatProbeVO;
import com.careld.notify.dto.WechatTestRequest;
import com.careld.notify.service.WechatConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台「系统设置」：公众号（模板消息 / 订阅通知）凭据配置与联调
 */
@Tag(name = "微信通知配置", description = "总部维护公众号凭据与模板，支持探针与测试发送")
@RestController
@RequestMapping("/api/v1/notify/wechat/config")
@RequiredArgsConstructor
public class NotifyWechatAdminController {

    private final WechatConfigService configService;

    @Operation(summary = "查询微信通知配置（AppSecret 只回掩码）")
    @GetMapping
    @RequirePermission("settings:view")
    public Result<WechatConfigVO> getConfig() {
        return Result.success(configService.getConfig());
    }

    @OperationLog(module = "notify", action = "wechat-config", description = "保存微信公众号通知配置")
    @Operation(summary = "保存微信通知配置")
    @PutMapping
    @RequirePermission("settings:view")
    public Result<WechatConfigVO> saveConfig(@RequestBody WechatConfigRequest request) {
        configService.saveConfig(request);
        return Result.success(configService.getConfig());
    }

    @Operation(summary = "探针：取 access_token、拉取模板列表并判定模板归属与字段名")
    @GetMapping("/probe")
    @RequirePermission("settings:view")
    public Result<WechatProbeVO> probe() {
        return Result.success(configService.probe());
    }

    @OperationLog(module = "notify", action = "wechat-server-token", description = "重新生成公众号服务器配置 Token")
    @Operation(summary = "重新生成服务器配置 Token（需同步替换公众平台「服务器配置」里的 Token）")
    @PostMapping("/server-token")
    @RequirePermission("settings:view")
    public Result<WechatConfigVO> regenerateServerToken() {
        return Result.success(configService.regenerateServerToken());
    }

    @OperationLog(module = "notify", action = "wechat-test", description = "测试发送微信模板消息")
    @Operation(summary = "测试发送：向指定 openid 真实发送一条模板消息，返回微信原始响应")
    @PostMapping("/test")
    @RequirePermission("settings:view")
    public Result<Map<String, Object>> testSend(@Valid @RequestBody WechatTestRequest request) {
        return Result.success(configService.testSend(request));
    }
}
