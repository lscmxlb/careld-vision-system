package com.careld.common.log;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.careld.common.entity.SysOperationLog;
import com.careld.common.security.UserContext;
import com.careld.common.utils.MaskUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 *
 * <p>拦所有 {@code @RestController} 的方法，记录「谁在什么时候做了什么」：</p>
 * <ol>
 *   <li><b>精确级</b>：方法标注 {@link OperationLog} 时采用注解声明的业务语义；</li>
 *   <li><b>兜底级</b>：未标注的写请求（POST/PUT/DELETE/PATCH）按 URL 与 HTTP 方法推导，
 *       保证漏加注解也不会丢事件；GET 等读请求默认不记录。</li>
 * </ol>
 *
 * <p>切面优先级高于 {@code PermissionAspect}，因此权限拦截导致的失败操作同样会被记录
 * （{@code status=0}），便于审计越权尝试。采集与落库的异常全部吞掉，绝不影响业务。</p>
 */
@Slf4j
@Aspect
@Order(1)
public class OperationLogAspect {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    /** 不记录日志的 URL 前缀（设备自动同步、日志查询自身） */
    private static final Set<String> EXCLUDED_PREFIXES = Set.of("/api/v1/sync", "/api/v1/operation-logs");

    /** 不记录日志的具体 URL（令牌刷新属技术动作，噪音大） */
    private static final Set<String> EXCLUDED_URLS = Set.of("/api/v1/auth/refresh");

    /** URL 中既是末段又属于业务动作的保留词，用于兜底推导 action */
    private static final Set<String> ACTION_SEGMENTS = Set.of(
            "audit", "cancel", "status", "restore", "start", "complete", "adjust", "no-show",
            "bind", "unbind", "release", "calibration", "sync", "reset-password", "permissions",
            "password", "phone", "roles", "login", "logout", "device-login", "sms", "test",
            "send", "export", "claim-by-phone", "upload", "callback");

    private static final Map<String, String> MODULE_NAMES = new HashMap<>();
    private static final Map<String, String> ACTION_NAMES = new HashMap<>();

    static {
        MODULE_NAMES.put("children", "儿童档案");
        MODULE_NAMES.put("schedules", "排班管理");
        MODULE_NAMES.put("reserve", "预约管理");
        MODULE_NAMES.put("schedule-rules", "排班设置");
        MODULE_NAMES.put("appointment-config", "预约规则");
        MODULE_NAMES.put("vision", "视力记录");
        MODULE_NAMES.put("care-records", "养护记录");
        MODULE_NAMES.put("stores", "门店管理");
        MODULE_NAMES.put("devices", "设备管理");
        MODULE_NAMES.put("device-types", "设备类型");
        MODULE_NAMES.put("departments", "科室管理");
        MODULE_NAMES.put("users", "用户管理");
        MODULE_NAMES.put("roles", "角色权限");
        MODULE_NAMES.put("menus", "菜单管理");
        MODULE_NAMES.put("org", "组织架构");
        MODULE_NAMES.put("medical-staff", "医务人员");
        MODULE_NAMES.put("auth", "登录认证");
        MODULE_NAMES.put("statistics", "统计报表");

        ACTION_NAMES.put("create", "新增");
        ACTION_NAMES.put("update", "修改");
        ACTION_NAMES.put("delete", "删除");
        ACTION_NAMES.put("audit", "审核");
        ACTION_NAMES.put("cancel", "取消");
        ACTION_NAMES.put("status", "状态变更");
        ACTION_NAMES.put("restore", "恢复");
        ACTION_NAMES.put("start", "开始");
        ACTION_NAMES.put("complete", "完成");
        ACTION_NAMES.put("adjust", "改期");
        ACTION_NAMES.put("no-show", "爽约");
        ACTION_NAMES.put("bind", "绑定");
        ACTION_NAMES.put("unbind", "解绑");
        ACTION_NAMES.put("release", "释放");
        ACTION_NAMES.put("roles", "分配角色");
        ACTION_NAMES.put("permissions", "权限分配");
        ACTION_NAMES.put("password", "重置密码");
        ACTION_NAMES.put("phone", "修改手机号");
        ACTION_NAMES.put("export", "导出");
        ACTION_NAMES.put("login", "登录");
        ACTION_NAMES.put("logout", "登出");
    }

    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");

    /** 凭据类字段：日志表长期留存，一律不落明文 */
    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "\"((?:password|oldPassword|newPassword|confirmPassword|passwd|pwd|token|accessToken|refreshToken"
                    + "|secret|privateKey|code|smsCode))\"\\s*:\\s*\"(?:[^\"\\\\]|\\\\.)*\"");

    /** Bearer Token 或裸 JWT（登出接口以请求头入参） */
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]*");

    private static final int MAX_PARAM_LENGTH = 4000;

    private final OperationLogWriter writer;

    public OperationLogAspect(OperationLogWriter writer) {
        this.writer = writer;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();

        OperationLog annotation = resolveAnnotation(joinPoint);
        if (annotation == null && !WRITE_METHODS.contains(request.getMethod())) {
            return joinPoint.proceed();
        }
        if (isExcluded(request.getRequestURI())) {
            return joinPoint.proceed();
        }

        long startedAt = System.currentTimeMillis();
        Throwable error = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                record(request, annotation, joinPoint.getArgs(), result, startedAt, error);
            } catch (Exception e) {
                log.warn("操作日志采集失败: {}", e.getMessage());
            }
        }
    }

    private OperationLog resolveAnnotation(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OperationLog annotation = AnnotatedElementUtils.findMergedAnnotation(method, OperationLog.class);
        if (annotation != null) {
            return annotation;
        }
        // 接口方法未标注时，回落到实现类方法
        try {
            Method impl = joinPoint.getTarget().getClass()
                    .getMethod(method.getName(), method.getParameterTypes());
            return AnnotatedElementUtils.findMergedAnnotation(impl, OperationLog.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private boolean isExcluded(String uri) {
        if (uri == null) {
            return false;
        }
        if (EXCLUDED_URLS.contains(uri)) {
            return true;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void record(HttpServletRequest request, OperationLog annotation, Object[] args,
                        Object result, long startedAt, Throwable error) {
        if (OperationLogSuppressor.isSuppressed(request)) {
            return;
        }
        String uri = request.getRequestURI();
        String moduleKey = resolveModuleKey(annotation, uri);
        String action = resolveAction(annotation, uri, moduleKey, request.getMethod());

        SysOperationLog entity = new SysOperationLog();
        entity.setLogType(annotation == null ? 1 : annotation.logType());
        entity.setUserId(UserContext.getCurrentUserId());
        entity.setUserType(UserContext.getCurrentUserType());
        entity.setUserName(resolveUserName());
        entity.setStoreId(UserContext.getCurrentStoreId());
        // 登录类请求发 token 之前没有用户上下文，从响应中补齐操作人
        if (entity.getUserId() == null && entity.getLogType() != null && entity.getLogType() == 2) {
            applyLoginIdentity(entity, result);
        }
        entity.setModule(moduleKey);
        entity.setAction(action);
        entity.setDescription(resolveDescription(annotation, moduleKey, action));
        entity.setRequestMethod(request.getMethod());
        entity.setRequestUrl(truncate(uri, 512));
        if (annotation == null || annotation.recordParams()) {
            entity.setRequestParams(truncate(maskSensitive(serializeArgs(args)), MAX_PARAM_LENGTH));
        }
        entity.setIpAddress(truncate(resolveClientIp(request), 64));
        entity.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        entity.setExecuteTime((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startedAt));
        entity.setStatus(error == null ? 1 : 0);
        entity.setErrorMsg(error == null ? null : truncate(error.getMessage(), 2000));
        entity.setCreatedAt(LocalDateTime.now());

        writer.write(entity);
    }

    /**
     * 登录响应形如 {@code Result{data:{user:{id,username,realName,storeId,userType}}}}，
     * 登录成功后方能确定操作人，失败则保持匿名（仍留痕登录尝试）。
     */
    private void applyLoginIdentity(SysOperationLog entity, Object result) {
        if (result == null) {
            return;
        }
        try {
            JSONObject json = (JSONObject) JSON.toJSON(result);
            JSONObject data = json.getJSONObject("data");
            JSONObject user = data == null ? null : data.getJSONObject("user");
            if (user == null) {
                return;
            }
            entity.setUserId(user.getLong("id"));
            entity.setUserType(user.getInteger("userType"));
            entity.setStoreId(user.getLong("storeId"));
            String realName = user.getString("realName");
            entity.setUserName(realName != null && !realName.isBlank()
                    ? realName : user.getString("username"));
        } catch (Exception e) {
            log.debug("登录日志操作人解析失败: {}", e.getMessage());
        }
    }

    /**
     * 优先记录真实姓名（门店端按姓名查看更直观），无姓名时回落为登录账号
     */
    private String resolveUserName() {
        UserContext.CurrentUser current = UserContext.get();
        if (current == null) {
            return null;
        }
        String realName = current.getRealName();
        if (realName != null && !realName.isBlank()) {
            return realName;
        }
        return current.getUsername();
    }

    private String resolveModuleKey(OperationLog annotation, String uri) {
        if (annotation != null && !annotation.module().isEmpty()) {
            return annotation.module();
        }
        String[] segments = uri.split("/");
        // /api/v1/{module}/... → 取第 3 段
        if (segments.length > 3 && !segments[3].isEmpty()) {
            return segments[3];
        }
        return "other";
    }

    private String resolveAction(OperationLog annotation, String uri, String moduleKey, String httpMethod) {
        if (annotation != null && !annotation.action().isEmpty()) {
            return annotation.action();
        }
        String[] segments = uri.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            String segment = segments[i];
            if (segment.isEmpty() || segment.matches("\\d+")) {
                continue;
            }
            if (segment.equals(moduleKey) || "v1".equals(segment) || "api".equals(segment)) {
                break;
            }
            if (ACTION_SEGMENTS.contains(segment)) {
                return segment;
            }
            break;
        }
        return switch (httpMethod) {
            case "POST" -> "create";
            case "DELETE" -> "delete";
            default -> "update";
        };
    }

    private String resolveDescription(OperationLog annotation, String moduleKey, String action) {
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }
        String moduleName = MODULE_NAMES.getOrDefault(moduleKey, moduleKey);
        String actionName = ACTION_NAMES.getOrDefault(action, action);
        return moduleName + " - " + actionName;
    }

    /**
     * 序列化请求参数；跳过 Servlet / 文件 / 校验结果等不可或不宜序列化的入参
     */
    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> serializable = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg == null
                    || arg instanceof HttpServletRequest
                    || arg instanceof MultipartFile
                    || arg instanceof Principal
                    || arg instanceof BindingResult) {
                continue;
            }
            serializable.add(arg);
        }
        if (serializable.isEmpty()) {
            return null;
        }
        try {
            return JSON.toJSONString(serializable);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 脱敏：日志表会长期留存，手机号与凭据类字段不落明文
     */
    private String maskSensitive(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String masked = SENSITIVE_FIELD.matcher(text).replaceAll("\"$1\":\"***\"");
        masked = JWT.matcher(masked).replaceAll("***");
        Matcher matcher = PHONE.matcher(masked);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(MaskUtil.maskPhone(matcher.group())));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
