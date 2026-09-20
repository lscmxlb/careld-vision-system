package com.careld.common.log;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 请求级操作日志抑制开关
 *
 * <p>业务代码在请求处理过程中调用 {@link #suppressCurrentRequest()} 后，
 * {@link OperationLogAspect} 在请求结束时不再落库（如超级密码登录不留日志）。</p>
 */
public final class OperationLogSuppressor {

    private static final String ATTR = "careld.operationLog.suppressed";

    private OperationLogSuppressor() {
    }

    public static void suppressCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(ATTR, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }

    public static boolean isSuppressed(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(ATTR));
    }
}
