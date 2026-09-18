package com.careld.schedule.config;

import com.alibaba.fastjson2.JSON;
import com.careld.common.result.Result;
import com.careld.common.security.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 登录校验过滤器（schedule-service 轻量鉴权）
 *
 * <p>本服务未引入 Spring Security，JwtAuthFilter 对无效/缺失 token 的请求会放行，
 * 导致匿名请求以 null 身份进入业务逻辑（列表静默返回空、创建报"医院不能为空"）。
 * 该过滤器在 JwtAuthFilter 之后执行：UserContext 为空即视为未登录，返回 401，
 * 前端 axios 拦截器据此清理登录态并跳转登录页。</p>
 */
public class GuardFilter extends OncePerRequestFilter {

    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/doc.html", "/v3/api-docs", "/swagger-ui", "/webjars", "/actuator", "/error");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || isWhitelisted(request.getRequestURI())
                || UserContext.get() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.error(401, "未登录或登录已过期，请重新登录")));
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
