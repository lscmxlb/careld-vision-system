package com.careld.common.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务操作日志注解
 *
 * <p>标注在 Controller 方法上，声明该操作的业务语义（模块 / 动作 / 中文描述 / 日志类型）。
 * 未标注的写请求（POST/PUT/DELETE/PATCH）由 {@link OperationLogAspect} 兜底记录，
 * 保证业务事件不会漏记。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 功能模块，如 child、schedule */
    String module();

    /** 操作类型，如 create、audit、cancel */
    String action();

    /** 操作描述（中文），如「审核儿童档案」 */
    String description() default "";

    /** 日志类型：1 操作 2 登录 3 异常 */
    int logType() default 1;

    /** 是否记录请求参数（手机号等敏感信息会自动脱敏） */
    boolean recordParams() default true;
}
