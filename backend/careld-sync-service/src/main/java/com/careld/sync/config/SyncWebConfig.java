package com.careld.sync.config;

import com.careld.common.security.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器注册：显式排序，保证 JwtAuthFilter 先解析 token 填充 UserContext，
 * GuardFilter 再校验登录态/设备 token。若仅依赖自动注册，两者 order 同为
 * LOWEST_PRECEDENCE，执行顺序不可控。
 */
@Configuration
public class SyncWebConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter jwtAuthFilter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
        registration.setOrder(100);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public GuardFilter guardFilter() {
        // 以 Spring Bean 方式注册，保证 GuardFilter 的 @Value jwt.secret 注入生效
        return new GuardFilter();
    }

    @Bean
    public FilterRegistrationBean<GuardFilter> guardFilterRegistration(GuardFilter guardFilter) {
        FilterRegistrationBean<GuardFilter> registration = new FilterRegistrationBean<>(guardFilter);
        registration.setOrder(200);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
