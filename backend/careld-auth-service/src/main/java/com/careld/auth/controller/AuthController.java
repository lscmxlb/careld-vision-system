package com.careld.auth.controller;

import com.careld.auth.dto.LoginRequest;
import com.careld.auth.dto.LoginResponse;
import com.careld.auth.dto.DeviceLoginRequest;
import com.careld.auth.dto.ParentChangePhoneRequest;
import com.careld.auth.dto.ParentLoginRequest;
import com.careld.auth.dto.ParentRegisterRequest;
import com.careld.auth.dto.ParentResetPasswordRequest;
import com.careld.auth.dto.SmsLoginRequest;
import com.careld.auth.service.AuthService;
import com.careld.common.result.Result;
import com.careld.common.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户登录、登出、Token刷新")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @OperationLog(module = "auth", action = "login", description = "用户登录", logType = 2)
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "Token刷新")
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.replace("Bearer ", "");
        return Result.success(authService.refreshToken(token));
    }

    @OperationLog(module = "auth", action = "logout", description = "退出登录", logType = 2)
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        authService.logout(accessToken);
        return Result.success();
    }

    @OperationLog(module = "auth", action = "device-login", description = "TV设备登录", logType = 2)
    @Operation(summary = "TV设备登录")
    @PostMapping("/device-login")
    public Result<LoginResponse> deviceLogin(@Valid @RequestBody DeviceLoginRequest request) {
        return Result.success(authService.deviceLogin(request));
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<com.careld.auth.dto.CaptchaResponse> captcha() {
        return Result.success(authService.generateCaptcha());
    }

    @OperationLog(module = "auth", action = "sms-send", description = "发送短信验证码", logType = 2)
    @Operation(summary = "发送短信验证码（家长端登录）")
    @PostMapping("/sms/send")
    public Result<Void> sendSmsCode(@RequestBody Map<String, String> body) {
        authService.sendSmsCode(body.get("phone"));
        return Result.success();
    }

    @OperationLog(module = "auth", action = "sms-login", description = "短信验证码登录", logType = 2)
    @Operation(summary = "手机号+验证码登录（未注册自动注册家长账号）")
    @PostMapping("/sms/login")
    public Result<LoginResponse> smsLogin(@Valid @RequestBody SmsLoginRequest request) {
        return Result.success(authService.smsLogin(request));
    }

    @OperationLog(module = "auth", action = "parent-login", description = "家长端密码登录", logType = 2)
    @Operation(summary = "家长端手机号+密码登录")
    @PostMapping("/parent/login")
    public Result<LoginResponse> parentLogin(@Valid @RequestBody ParentLoginRequest request) {
        return Result.success(authService.parentLogin(request));
    }

    @OperationLog(module = "auth", action = "parent-register", description = "家长端注册", logType = 2)
    @Operation(summary = "家长端注册（手机号+验证码+用户名称+登录密码）")
    @PostMapping("/parent/register")
    public Result<LoginResponse> parentRegister(@Valid @RequestBody ParentRegisterRequest request) {
        return Result.success(authService.parentRegister(request));
    }

    @OperationLog(module = "auth", action = "parent-reset-password", description = "家长端重置密码")
    @Operation(summary = "家长端忘记密码（手机号+验证码重新设置密码）")
    @PostMapping("/parent/reset-password")
    public Result<Void> parentResetPassword(@Valid @RequestBody ParentResetPasswordRequest request) {
        authService.parentResetPassword(request);
        return Result.success();
    }

    @OperationLog(module = "auth", action = "parent-change-phone", description = "家长端修改手机号")
    @Operation(summary = "家长端修改手机号（新手机号+验证码，旧手机号不验证）")
    @PostMapping("/parent/change-phone")
    public Result<Void> parentChangePhone(@RequestAttribute("userId") Long userId,
                                          @Valid @RequestBody ParentChangePhoneRequest request) {
        authService.parentChangePhone(userId, request);
        return Result.success();
    }
}
