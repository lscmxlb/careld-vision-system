package com.careld.common.config;

import com.careld.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Common模块自动配置
 * 将全局异常处理器、字段自动填充处理器等公共组件自动注册到所有引用此模块的服务中
 */
@Configuration
@Import({GlobalExceptionHandler.class, CareldMetaObjectHandler.class})
public class CareldCommonAutoConfiguration {
}
