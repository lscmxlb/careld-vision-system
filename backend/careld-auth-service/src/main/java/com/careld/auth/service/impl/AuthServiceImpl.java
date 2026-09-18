package com.careld.auth.service.impl;

import com.careld.auth.dto.CaptchaResponse;
import com.careld.auth.dto.LoginRequest;
import com.careld.auth.dto.LoginResponse;
import com.careld.auth.dto.DeviceLoginRequest;
import com.careld.auth.dto.SmsLoginRequest;
import com.careld.auth.entity.MedicalStaff;
import com.careld.auth.entity.User;
import com.careld.auth.mapper.MedicalStaffMapper;
import com.careld.auth.mapper.PermissionMapper;
import com.careld.auth.mapper.SmsCodeMapper;
import com.careld.auth.mapper.UserMapper;
import com.careld.auth.service.AuthService;
import com.careld.auth.service.AliyunSmsClient;
import com.careld.auth.service.SmsConfigService;
import com.careld.common.exception.BusinessException;
import com.careld.common.security.JwtUtil;
import com.alibaba.fastjson2.JSON;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final MedicalStaffMapper medicalStaffMapper;
    private final PermissionMapper permissionMapper;
    private final SmsCodeMapper smsCodeMapper;
    private final SmsConfigService smsConfigService;
    private final AliyunSmsClient aliyunSmsClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final int SMS_CODE_EXPIRE_MINUTES = 5;
    private static final int SMS_DAILY_LIMIT = 10;
    private static final int SMS_MAX_WRONG_ATTEMPTS = 5;
    private static final String SMS_FREQ_KEY = "careld:auth:sms:freq";
    private static final String SMS_DAILY_KEY = "careld:auth:sms:daily";
    private static final String SMS_FAIL_KEY = "careld:auth:sms:fail";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户；user_info 无匹配时回退医务人员表（医务人员以手机号作为登录账号）
        User user = null;
        MedicalStaff staff = null;
        if (request.getLoginType() != null && request.getLoginType() == 2) {
            // 手机号登录
            user = userMapper.selectByPhone(request.getPhone());
            if (user == null) {
                staff = request.getPhone() == null ? null : medicalStaffMapper.selectEnabledByPhone(request.getPhone());
            }
            if (user == null && staff == null) {
                throw new BusinessException(1001, "手机号或密码错误");
            }
        } else {
            // 用户名登录
            user = userMapper.selectByUsername(request.getUsername());
            if (user == null) {
                staff = request.getUsername() == null ? null : medicalStaffMapper.selectEnabledByPhone(request.getUsername());
            }
            if (user == null && staff == null) {
                throw new BusinessException(1001, "用户名或密码错误");
            }
        }

        // 医务人员账号：验证密码并签发 userType=6 的门店身份
        if (staff != null) {
            if (!passwordEncoder.matches(request.getPassword(), staff.getLoginPassword())) {
                throw new BusinessException(1001, "用户名或密码错误");
            }
            return generateStaffTokenResponse(staff);
        }

        // 检查状态
        if (user.getStatus() != 1) {
            throw new BusinessException(1002, "账号已被禁用");
        }

        // 检查锁定
        if (user.getLockTime() != null && user.getLockTime().plusMinutes(2).isAfter(LocalDateTime.now())) {
            throw new BusinessException(1002, "账号已被锁定，请2分钟后重试");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 增加失败次数
            int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(failCount);
            if (failCount >= 5) {
                user.setLockTime(LocalDateTime.now());
            }
            userMapper.updateById(user);
            throw new BusinessException(1001, "用户名或密码错误");
        }

        // 清除失败次数
        user.setLoginFailCount(0);
        user.setLockTime(null);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成Token
        return generateTokenResponse(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // 验证刷新令牌
        if (!JwtUtil.validateToken(jwtSecret, refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效或已过期");
        }

        Long userId = JwtUtil.getUserId(jwtSecret, refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(401, "用户不存在或已被禁用");
        }

        return generateTokenResponse(user);
    }

    @Override
    public void logout(String token) {
        // 将Token加入黑名单（可选，使用Redis）
        log.info("用户登出");
    }

    @Override
    public LoginResponse deviceLogin(DeviceLoginRequest request) {
        // TV设备登录逻辑（简化版）
        log.info("TV设备登录: storeCode={}, deviceCode={}", request.getStoreCode(), request.getDeviceCode());
        
        // 创建设备用户Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("storeCode", request.getStoreCode());
        claims.put("deviceCode", request.getDeviceCode());
        claims.put("userType", 2); // 门店类型
        claims.put("isDevice", true);

        String accessToken = JwtUtil.generateAccessToken(jwtSecret, claims);
        String refreshToken = JwtUtil.generateRefreshToken(jwtSecret, claims);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(7200L);
        response.setTokenType("Bearer");

        return response;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public CaptchaResponse generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 30);
        String code = captcha.getCode();
        String captchaKey = IdUtil.fastSimpleUUID();
        // 存入 Redis，5 分钟过期
        stringRedisTemplate.opsForValue().set("careld:auth:captcha:" + captchaKey, code, Duration.ofMinutes(5));

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaKey(captchaKey);
        response.setCaptchaImage("data:image/png;base64," + captcha.getImageBase64());
        return response;
    }

    @Override
    public void sendSmsCode(String phone) {
        doSendSmsCode(phone, false);
    }

    @Override
    public void sendTestSms(String phone) {
        doSendSmsCode(phone, true);
    }

    /**
     * @param forceReal 管理后台测试发送：忽略启用开关，直接使用已保存的配置真实发送
     */
    private void doSendSmsCode(String phone, boolean forceReal) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(1000, "手机号格式不正确");
        }

        // 60 秒频控
        String freqKey = SMS_FREQ_KEY + ":" + phone;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(freqKey, "1", Duration.ofSeconds(60));
        if (ok == null || !ok) {
            throw new BusinessException(1003, "发送过于频繁，请 60 秒后重试");
        }

        // 同一手机号每日发送上限
        String dailyKey = SMS_DAILY_KEY + ":" + phone + ":" + LocalDate.now();
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyKey);
        if (dailyCount != null && dailyCount == 1) {
            stringRedisTemplate.expire(dailyKey, Duration.ofHours(24));
        }
        if (dailyCount != null && dailyCount > SMS_DAILY_LIMIT) {
            throw new BusinessException(1003, "该手机号今日验证码发送次数已达上限（" + SMS_DAILY_LIMIT + " 次）");
        }

        SmsConfigService.SmsRuntimeConfig runtime = smsConfigService.loadRuntime();
        boolean realSend = (forceReal || runtime.enabled()) && runtime.usable();
        if (!realSend) {
            if (forceReal) {
                throw new BusinessException(1005, "请先在管理后台保存完整的阿里云短信配置（AccessKey ID/Secret、签名、模板 Code）");
            }
            if (runtime.enabled()) {
                throw new BusinessException(1005, "短信服务已启用但配置不完整，请联系管理员");
            }
            if (!smsConfigService.isMockFallback()) {
                throw new BusinessException(1005, "短信服务未配置，请联系管理员");
            }
        }

        String code = realSend ? generateSmsCode() : "123456";
        if (realSend) {
            try {
                aliyunSmsClient.sendVerifyCode(phone, code, runtime);
            } catch (BusinessException e) {
                // 发送失败不占用频控与当日额度，便于修正配置后立即重试
                stringRedisTemplate.delete(freqKey);
                stringRedisTemplate.opsForValue().decrement(dailyKey);
                throw e;
            }
        } else {
            log.info("[SMS-MOCK] 手机号 {} 验证码: {}", phone, code);
        }

        smsCodeMapper.insertCode(phone, code, LocalDateTime.now().plusMinutes(SMS_CODE_EXPIRE_MINUTES));
        stringRedisTemplate.delete(SMS_FAIL_KEY + ":" + phone);
    }

    private static String generateSmsCode() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
    }

    @Override
    public LoginResponse smsLogin(SmsLoginRequest request) {
        String phone = request.getPhone();
        String stored = smsCodeMapper.selectLatestValid(phone);
        if (stored == null || !stored.equals(request.getCode())) {
            // 连错 5 次作废当前验证码，防止暴力猜码
            String failKey = SMS_FAIL_KEY + ":" + phone;
            Long fails = stringRedisTemplate.opsForValue().increment(failKey);
            if (fails != null && fails == 1) {
                stringRedisTemplate.expire(failKey, Duration.ofMinutes(10));
            }
            if (fails != null && fails >= SMS_MAX_WRONG_ATTEMPTS) {
                smsCodeMapper.invalidateAll(phone);
                stringRedisTemplate.delete(failKey);
                throw new BusinessException(1004, "验证码错误次数过多，该验证码已失效，请重新获取");
            }
            throw new BusinessException(1004, "验证码错误或已过期");
        }
        stringRedisTemplate.delete(SMS_FAIL_KEY + ":" + phone);
        smsCodeMapper.markUsed(phone, request.getCode());

        // 手机号匹配既有账号；未注册则自动创建家长账号（user_type=3）
        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            user = new User();
            user.setUsername(request.getPhone());
            user.setPassword(passwordEncoder.encode(IdUtil.fastSimpleUUID()));
            user.setRealName("家长" + request.getPhone().substring(7));
            user.setPhone(request.getPhone());
            user.setUserType(3);
            user.setStatus(1);
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            if (user.getStatus() != 1) {
                throw new BusinessException(1002, "账号已被禁用");
            }
            // 家长端仅允许家长账号登录：其他身份登录会因数据权限与建档来源语义错乱
            if (user.getUserType() == null || user.getUserType() != 3) {
                throw new BusinessException(1006, "该手机号不是家长账号，请使用家长手机号登录");
            }
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
        return generateTokenResponse(user);
    }

    /**
     * 医务人员 Token 响应：userType=6（医务人员），数据权限按其所属门店隔离；
     * userId 为 medical_staff 主键（与 user_info 无关联）
     */
    private LoginResponse generateStaffTokenResponse(MedicalStaff staff) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", staff.getId());
        claims.put("username", staff.getPhone());
        claims.put("userType", 6);
        claims.put("storeId", staff.getStoreId());
        claims.put("permissions", Collections.emptyList());

        String accessToken = JwtUtil.generateAccessToken(jwtSecret, claims);
        String refreshToken = JwtUtil.generateRefreshToken(jwtSecret, claims);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(7200L);
        response.setTokenType("Bearer");

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(staff.getId());
        userInfo.setUsername(staff.getPhone());
        userInfo.setRealName(staff.getName());
        userInfo.setPhone(staff.getPhone());
        userInfo.setUserType(6);
        userInfo.setStaffRole(staff.getStaffRole());
        userInfo.setStoreId(staff.getStoreId());
        userInfo.setRoles(List.of("medical_staff"));
        userInfo.setPermissions(Collections.emptyList());
        response.setUser(userInfo);

        return response;
    }

    /**
     * 生成Token响应
     */
    private LoginResponse generateTokenResponse(User user) {
        // 查询用户实际权限列表
        List<String> permissions = buildPermissionKeys(user.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType());
        claims.put("storeId", user.getStoreId());
        claims.put("centerId", user.getCenterId());
        claims.put("agentId", user.getAgentId());
        claims.put("deptId", user.getDeptId());
        claims.put("permissions", permissions);

        String accessToken = JwtUtil.generateAccessToken(jwtSecret, claims);
        String refreshToken = JwtUtil.generateRefreshToken(jwtSecret, claims);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(7200L);
        response.setTokenType("Bearer");

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setPhone(user.getPhone());
        userInfo.setUserType(user.getUserType());
        userInfo.setStoreId(user.getStoreId());
        userInfo.setCenterId(user.getCenterId());
        userInfo.setAgentId(user.getAgentId());
        userInfo.setDeptId(user.getDeptId());
        userInfo.setJobTitle(user.getJobTitle());
        userInfo.setRoles(List.of("user"));
        userInfo.setPermissions(permissions);
        response.setUser(userInfo);

        return response;
    }

    /**
     * 从数据库构建用户权限标识列表
     * 逻辑与 user-service 的 PermissionServiceImpl 一致
     */
    private List<String> buildPermissionKeys(Long userId) {
        List<Map<String, Object>> rows = permissionMapper.selectPermissionsByUserId(userId);
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        Set<String> permissions = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String permKey = (String) row.get("permission_key");
            if (permKey == null) continue;

            // 添加权限标识本身
            permissions.add(permKey);

            // 获取菜单类型: 1目录 2菜单 3按钮
            Object menuTypeObj = row.get("menu_type");
            int menuType = 0;
            if (menuTypeObj instanceof Number) {
                menuType = ((Number) menuTypeObj).intValue();
            }

            // 按钮级(type=3)权限的permission_key本身就是完整权限（如 organization:center:create），
            // 不需要从actions派生，直接跳过
            if (menuType == 3) continue;

            // 菜单级(type=2)权限从actions派生操作权限
            // 菜单的permission_key以:view结尾，如 organization:center:view
            String actionsJson = (String) row.get("actions");
            if (actionsJson != null && !actionsJson.isBlank()) {
                try {
                    List<String> actions = JSON.parseArray(actionsJson, String.class);
                    for (String action : actions) {
                        if ("view".equals(action)) {
                            permissions.add(permKey);
                        } else {
                            // organization:center:view -> organization:center:create
                            String actionKey = permKey.replaceAll(":view$", ":" + action);
                            if (!actionKey.equals(permKey)) {
                                permissions.add(actionKey);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析actions失败: {}", actionsJson);
                }
            }
        }

        return new ArrayList<>(permissions);
    }
}
