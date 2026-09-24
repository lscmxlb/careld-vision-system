-- 微信公众号扫码绑定：
-- 家长端「我的 → 微信公众号」展示带参二维码（scene 随机），家长扫码关注后微信推送
-- subscribe / SCAN 事件到 notify-service 回调，按 scene 匹配票据并自动写入 sys_user.wechat_openid。

CREATE TABLE IF NOT EXISTS wechat_bind_ticket (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    scene      VARCHAR(64) NOT NULL COMMENT '带参二维码 scene 值（随机，回调按此匹配家长）',
    user_id    BIGINT      NOT NULL COMMENT '家长用户ID',
    status     TINYINT     NOT NULL DEFAULT 0 COMMENT '0待扫码 1已绑定 2已失效',
    openid     VARCHAR(64) NULL COMMENT '扫码关注者 openid',
    expire_at  DATETIME    NOT NULL COMMENT '二维码过期时间',
    created_by BIGINT      NULL COMMENT '创建人',
    updated_by BIGINT      NULL COMMENT '更新人',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME    NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wechat_bind_scene (scene),
    KEY idx_wechat_bind_user (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '微信公众号扫码绑定票据';
