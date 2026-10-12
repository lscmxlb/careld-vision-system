-- 用户同意记录（合规留痕：家长端注册时对《隐私政策》的明示同意，记录时间与版本）
CREATE TABLE IF NOT EXISTS user_consent (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT      NOT NULL COMMENT '用户ID（sys_user.id）',
    consent_type   VARCHAR(32) NOT NULL COMMENT '同意类型：privacy_policy=隐私政策',
    policy_version VARCHAR(16) NOT NULL COMMENT '同意的政策版本号',
    agreed_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同意时间',
    created_by     BIGINT      NULL COMMENT '创建人',
    updated_by     BIGINT      NULL COMMENT '更新人',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at     DATETIME    NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_user_consent_user (user_id, consent_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户同意记录';
