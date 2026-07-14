package com.careld.common.security;

/**
 * 安全上下文设置器（可选 SPI）
 *
 * <p>用于在引入了 Spring Security 的服务（auth-service / user-service）中，
 * 将 {@link UserContext.CurrentUser} 同步写入 Spring Security 的
 * {@code SecurityContextHolder}，使 {@code authenticated()} 鉴权通过。
 * 未引入 Spring Security 的服务无需提供该实现，过滤器会跳过。</p>
 *
 * <p>实现需注册为 Spring Bean，过滤器按 {@code ObjectProvider} 可选注入。</p>
 */
public interface AuthenticationSetter {

    /**
     * 将当前用户写入安全上下文（如 SecurityContextHolder）
     *
     * @param user 当前登录用户（非空，已由过滤器解析成功）
     */
    void setAuthentication(UserContext.CurrentUser user);

    /**
     * 清理安全上下文
     */
    void clearAuthentication();
}
