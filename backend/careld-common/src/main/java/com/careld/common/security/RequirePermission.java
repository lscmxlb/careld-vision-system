package com.careld.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 *
 * <p>标注在 Controller 方法上，AOP 切面拦截并校验当前用户是否拥有指定权限。
 * 权限标识格式：{@code 模块:资源:操作}，如 {@code organization:center:create}。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /**
     * 权限标识，如 "organization:center:create"
     */
    String value();
}
