-- 通知服务（短信 + 微信模板消息）：
-- 1) store_notify_config   医院端「系统设置 → 通知服务」的两个通道开关与通知类型勾选（每院一行）
-- 2) notify_task           业务事件（建档/预约/取消/调整/养护完成）产生的待发送任务，由 notify-service 定时消费
-- 3) notify_record         实际发送记录（含是否成功、收费金额、备注），医院端通知记录列表数据源
-- 4) store_notify_account  医院短信费用账户（可用余额 / 累计消费 / 累计充值）
-- 5) sys_user 增加微信公众号绑定列（家长端「我的 → 微信公众号」）

-- 1. 医院端通知服务配置
CREATE TABLE IF NOT EXISTS store_notify_config (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    store_id      BIGINT       NOT NULL COMMENT '医院ID',
    sms_enabled   TINYINT      NOT NULL DEFAULT 0 COMMENT '手机短信通知开关：1开 0关',
    wechat_enabled TINYINT     NOT NULL DEFAULT 0 COMMENT '微信消息通知开关：1开 0关',
    enabled_types VARCHAR(255) NOT NULL DEFAULT '' COMMENT '启用的通知类型，逗号分隔：child_created建档成功,reserve_created预约成功,reserve_cancelled预约取消,reserve_adjusted预约调整,care_completed养护完成,other其它服务',
    created_by    BIGINT       NULL COMMENT '创建人',
    updated_by    BIGINT       NULL COMMENT '更新人',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at    DATETIME     NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notify_config_store (store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '医院端通知服务配置';

-- 2. 通知任务（业务事件触发，待发送队列）
CREATE TABLE IF NOT EXISTS notify_task (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    store_id    BIGINT       NOT NULL COMMENT '医院ID',
    child_id    BIGINT       NOT NULL COMMENT '儿童ID',
    event_type  VARCHAR(40)  NOT NULL COMMENT '通知类型',
    ref_id      BIGINT       NULL COMMENT '关联业务ID（预约ID等）',
    payload     TEXT         NULL COMMENT '业务快照JSON（日期/时段/取消原因/视力值等）',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0待发送 1已完成 2失败 3已跳过 9处理中',
    retry_count INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    remark      VARCHAR(255) NULL COMMENT '处理备注（跳过/失败原因）',
    created_by  BIGINT       NULL COMMENT '创建人',
    updated_by  BIGINT       NULL COMMENT '更新人',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at  DATETIME     NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_notify_task_status (status, id),
    KEY idx_notify_task_store (store_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知发送任务';

-- 3. 通知记录
CREATE TABLE IF NOT EXISTS notify_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    store_id    BIGINT       NOT NULL COMMENT '医院ID',
    task_id     BIGINT       NULL COMMENT '来源任务ID',
    event_type  VARCHAR(40)  NOT NULL COMMENT '通知类型',
    channel     TINYINT      NOT NULL COMMENT '通知渠道：1短信 2微信模板消息',
    child_id    BIGINT       NULL COMMENT '儿童ID',
    child_name  VARCHAR(64)  NULL COMMENT '儿童姓名（脱敏快照）',
    recipient   VARCHAR(64)  NULL COMMENT '通知对象（手机号脱敏 / 微信openid尾号）',
    title       VARCHAR(64)  NULL COMMENT '通知标题',
    content     VARCHAR(500) NULL COMMENT '通知内容',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功：1成功 0失败',
    fee         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '本条收费金额（元，短信0.10/条，微信0）',
    fail_reason VARCHAR(255) NULL COMMENT '失败原因',
    remark      VARCHAR(255) NULL COMMENT '备注（如：模拟通道）',
    sent_at     DATETIME     NOT NULL COMMENT '发送时间',
    created_by  BIGINT       NULL COMMENT '创建人',
    updated_by  BIGINT       NULL COMMENT '更新人',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at  DATETIME     NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_notify_record_store (store_id, sent_at),
    KEY idx_notify_record_child (child_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知发送记录';

-- 4. 医院短信费用账户
CREATE TABLE IF NOT EXISTS store_notify_account (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    store_id       BIGINT        NOT NULL COMMENT '医院ID',
    balance        DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额（元）',
    total_fee      DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费（元）',
    total_recharge DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计充值（元）',
    created_by     BIGINT        NULL COMMENT '创建人',
    updated_by     BIGINT        NULL COMMENT '更新人',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at     DATETIME      NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notify_account_store (store_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '医院通知费用账户';

-- 5. 家长微信公众号绑定
ALTER TABLE sys_user
    ADD COLUMN wechat_openid VARCHAR(64) NULL COMMENT '微信公众号 openid（家长关注公众号后绑定）' AFTER phone,
    ADD COLUMN wechat_bound_at DATETIME NULL COMMENT '微信公众号绑定时间' AFTER wechat_openid;

-- 6. 存量医院初始化：配置行（两开关默认关闭、五个通知类型默认勾选）+ 演示账户（初始 100 元）
INSERT INTO store_notify_config (store_id, sms_enabled, wechat_enabled, enabled_types)
SELECT s.id, 0, 0, 'child_created,reserve_created,reserve_cancelled,reserve_adjusted,care_completed'
FROM store_info s
WHERE NOT EXISTS (SELECT 1 FROM store_notify_config c WHERE c.store_id = s.id);

INSERT INTO store_notify_account (store_id, balance, total_fee, total_recharge)
SELECT s.id, 100.00, 0.00, 100.00
FROM store_info s
WHERE NOT EXISTS (SELECT 1 FROM store_notify_account a WHERE a.store_id = s.id);
