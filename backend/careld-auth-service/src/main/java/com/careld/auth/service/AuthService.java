package com.careld.auth.service;

import com.careld.auth.dto.CaptchaResponse;
import com.careld.auth.dto.LoginRequest;
import com.careld.auth.dto.LoginResponse;
import com.careld.auth.dto.DeviceLoginRequest;
import com.careld.auth.dto.ParentChangePhoneRequest;
import com.careld.auth.dto.ParentLoginRequest;
import com.careld.auth.dto.ParentRegisterRequest;
import com.careld.auth.dto.ParentResetPasswordRequest;
import com.careld.auth.dto.SmsLoginRequest;
import com.careld.auth.entity.User;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * TV设备登录
     */
    LoginResponse deviceLogin(DeviceLoginRequest request);

    /**
     * 根据ID获取用户
     */
    User getUserById(Long userId);

    /**
     * 生成图形验证码
     */
    CaptchaResponse generateCaptcha();

    /**
     * 发送短信验证码（60 秒频控，5 分钟有效；启用真实发送时走阿里云短信）
     */
    void sendSmsCode(String phone);

    /**
     * 管理后台测试发送：使用已保存的阿里云配置真实发送一条验证码
     */
    void sendTestSms(String phone);

    /**
     * 家长端手机号+验证码登录（未注册手机号自动注册家长账号）
     */
    LoginResponse smsLogin(SmsLoginRequest request);

    /**
     * 家长端手机号+密码登录（仅家长账号；未注册/其它身份给出明确提示）
     */
    LoginResponse parentLogin(ParentLoginRequest request);

    /**
     * 家长端注册：手机号+短信验证码+用户名称+登录密码，注册成功即签发登录态
     */
    LoginResponse parentRegister(ParentRegisterRequest request);

    /**
     * 家长端忘记密码：手机号+短信验证码重新设置登录密码
     */
    void parentResetPassword(ParentResetPasswordRequest request);

    /**
     * 家长端修改手机号：新手机号+短信验证码（旧手机号不验证），同步登录账号
     */
    void parentChangePhone(Long userId, ParentChangePhoneRequest request);
}
