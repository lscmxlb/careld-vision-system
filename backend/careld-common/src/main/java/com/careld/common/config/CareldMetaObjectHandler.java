package com.careld.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.careld.common.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * 处理 BaseEntity 中标记了 FieldFill.INSERT / FieldFill.INSERT_UPDATE 的字段
 */
@Slf4j
@Component
public class CareldMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createdBy", Long.class, getCurrentUserId());
        this.strictInsertFill(metaObject, "updatedBy", Long.class, getCurrentUserId());
        // deletedAt 不设值，依赖数据库 DEFAULT NULL
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, getCurrentUserId());
    }

    /**
     * 获取当前登录用户 ID（由 JwtAuthFilter 填充到 UserContext）
     */
    private Long getCurrentUserId() {
        return UserContext.getCurrentUserId();
    }
}
