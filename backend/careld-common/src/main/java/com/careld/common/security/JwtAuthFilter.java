package com.careld.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 鉴权过滤器
 *
 * <p>从 {@code Authorization: Bearer <token>} 解析 JWT，成功后：
 * <ol>
 *   <li>填充 {@link UserContext}（供 MetaObjectHandler 自动填充 createdBy/updatedBy）；</li>
 *   <li>设置 request attribute（userId / userType / storeId / deptId / username），供 {@code @RequestAttribute} 使用；</li>
 *   <li>若存在 {@link AuthenticationSetter}（仅 Spring Security 服务），同步写入安全上下文。</li>
 * </ol>
 * 无 token 或解析失败时放行，由后续安全链（如 Spring Security）决定是否 403。</p>
 *
 * <p>作为 {@code @Bean} 注册后，无 Spring Security 的服务由 servlet 自动注册该过滤器；
 * 引入了 Spring Security 的服务在 SecurityConfig 中通过 {@code addFilterBefore} 加入安全链。</p>
 */
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USER_TYPE = "userType";
    public static final String ATTR_STORE_ID = "storeId";
    public static final String ATTR_DEPT_ID = "deptId";
    public static final String ATTR_USERNAME = "username";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectProvider<AuthenticationSetter> authenticationSetterProvider;

    public JwtAuthFilter(ObjectProvider<AuthenticationSetter> authenticationSetterProvider) {
        this.authenticationSetterProvider = authenticationSetterProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = JwtUtil.parseToken(jwtSecret, token);
                Long userId = claims.get("userId") == null ? null : Long.valueOf(claims.get("userId").toString());
                Integer userType = claims.get("userType") == null ? null : Integer.valueOf(claims.get("userType").toString());
                Long storeId = claims.get("storeId") == null ? null : Long.valueOf(claims.get("storeId").toString());
                Long deptId = claims.get("deptId") == null ? null : Long.valueOf(claims.get("deptId").toString());
                String username = claims.get("username") == null ? null : claims.get("username").toString();

                if (userId != null) {
                    UserContext.CurrentUser user = new UserContext.CurrentUser(userId, userType, storeId, deptId, username);
                    UserContext.set(user);

                    request.setAttribute(ATTR_USER_ID, userId);
                    if (userType != null) {
                        request.setAttribute(ATTR_USER_TYPE, userType);
                    }
                    if (storeId != null) {
                        request.setAttribute(ATTR_STORE_ID, storeId);
                    }
                    if (deptId != null) {
                        request.setAttribute(ATTR_DEPT_ID, deptId);
                    }
                    if (username != null) {
                        request.setAttribute(ATTR_USERNAME, username);
                    }

                    AuthenticationSetter setter = authenticationSetterProvider.getIfAvailable();
                    if (setter != null) {
                        setter.setAuthentication(user);
                    }
                }
            } catch (Exception e) {
                // token 无效/过期：清理可能残留的上下文，放行由安全链处理
                log.debug("JWT 解析失败: {}", e.getMessage());
                UserContext.clear();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            AuthenticationSetter setter = authenticationSetterProvider.getIfAvailable();
            if (setter != null) {
                setter.clearAuthentication();
            }
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
