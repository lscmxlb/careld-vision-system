package com.careld.sync.config;

import com.alibaba.fastjson2.JSON;
import com.careld.common.result.Result;
import com.careld.common.security.JwtUtil;
import com.careld.common.security.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 登录校验过滤器（sync-service 轻量鉴权，与 schedule-service GuardFilter 同构）
 *
 * <p>TV 设备不携带用户 token，而是先经 /auth/device-login 换取设备 token
 * （claims 仅含 storeCode/deviceCode/isDevice，无 userId，JwtAuthFilter 不会填充
 * UserContext）。因此本过滤器在 UserContext 为空时再解析一次 token：
 * 有效设备 token（isDevice=true）放行，否则返回 401。</p>
 */
public class GuardFilter extends OncePerRequestFilter {

    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/doc.html", "/v3/api-docs", "/swagger-ui", "/webjars", "/actuator", "/error");

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || isWhitelisted(request.getRequestURI())
                || UserContext.get() != null
                || isDeviceToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.error(401, "未登录或登录已过期，请重新登录")));
    }

    private boolean isDeviceToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return false;
        }
        try {
            Claims claims = JwtUtil.parseToken(jwtSecret, header.substring("Bearer ".length()).trim());
            Object isDevice = claims.get("isDevice");
            return Boolean.TRUE.equals(isDevice) || "true".equalsIgnoreCase(String.valueOf(isDevice));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isWhitelisted(String uri) {
        for (String prefix : WHITELIST_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
