package com.careld.auth.controller;

import com.careld.auth.dto.SmsConfigRequest;
import com.careld.auth.dto.SmsConfigResponse;
import com.careld.auth.dto.SmsTestRequest;
import com.careld.auth.service.AuthService;
import com.careld.auth.service.SmsConfigService;
import com.careld.common.result.Result;
import com.careld.common.security.RequirePermission;
import com.careld.common.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 短信服务配置（管理后台系统设置）
 */
@Tag(name = "短信配置", description = "阿里云短信配置读写与测试发送")
@RestController
@RequestMapping("/api/v1/auth/sms/config")
@RequiredArgsConstructor
public class SmsConfigController {

    private final SmsConfigService smsConfigService;
    private final AuthService authService;

    @Operation(summary = "查询短信配置（Secret 不回明文）")
    @GetMapping
    @RequirePermission("settings:view")
    public Result<SmsConfigResponse> getConfig() {
        return Result.success(smsConfigService.getConfig());
    }

    @OperationLog(module = "auth", action = "sms-config", description = "保存短信服务配置")
    @Operation(summary = "保存短信配置")
    @PutMapping
    @RequirePermission("settings:view")
    public Result<Void> saveConfig(@RequestBody SmsConfigRequest request) {
        smsConfigService.saveConfig(request);
        return Result.success();
    }

    @OperationLog(module = "auth", action = "sms-test", description = "测试发送短信")
    @Operation(summary = "测试发送：使用已保存的配置真实发送一条验证码")
    @PostMapping("/test")
    @RequirePermission("settings:view")
    public Result<Void> testSend(@Valid @RequestBody SmsTestRequest request) {
        authService.sendTestSms(request.getPhone());
        return Result.success();
    }
}
