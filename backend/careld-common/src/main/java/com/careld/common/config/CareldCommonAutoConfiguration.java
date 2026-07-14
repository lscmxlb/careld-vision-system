package com.careld.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.careld.common.exception.GlobalExceptionHandler;
import com.careld.common.security.AuthenticationSetter;
import com.careld.common.security.JwtAuthFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Common模块自动配置
 * 将全局异常处理器、字段自动填充处理器、JWT 鉴权过滤器、MyBatis-Plus 分页插件等公共组件
 * 自动注册到所有引用此模块的服务中
 */
@Configuration
@Import({GlobalExceptionHandler.class, CareldMetaObjectHandler.class})
public class CareldCommonAutoConfiguration {

    /**
     * JWT 鉴权过滤器
     *
     * <p>无 Spring Security 的服务通过 servlet 过滤器自动生效，
     * 引入了 Spring Security 的服务在各自 SecurityConfig 中通过 addFilterBefore 纳入安全链。</p>
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthFilter.class)
    public JwtAuthFilter jwtAuthFilter(ObjectProvider<AuthenticationSetter> authenticationSetterProvider) {
        return new JwtAuthFilter(authenticationSetterProvider);
    }

    /**
     * MyBatis-Plus 分页插件（统一为所有服务启用分页）
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
