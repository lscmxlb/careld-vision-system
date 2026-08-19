package com.careld.common.security;

import com.careld.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限校验 AOP 切面
 *
 * <p>拦截标注了 {@link RequirePermission} 的方法，从 {@link UserContext} 获取当前用户权限列表，
 * 校验是否包含所需权限。无权限时抛出 {@code BusinessException(403)}。</p>
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String permissionKey = requirePermission.value();
        List<String> userPermissions = UserContext.getPermissions();

        if (userPermissions == null || userPermissions.isEmpty()) {
            throw new BusinessException(403, "无操作权限: " + permissionKey);
        }

        // 超级权限通配符
        if (userPermissions.contains("*")) {
            return joinPoint.proceed();
        }

        if (!userPermissions.contains(permissionKey)) {
            log.debug("权限校验失败: 用户权限={}, 需要权限={}", userPermissions, permissionKey);
            throw new BusinessException(403, "无操作权限: " + permissionKey);
        }

        return joinPoint.proceed();
    }
}
