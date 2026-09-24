-- 微信支付（Native 扫码）续费：充值订单流水表 + 存量医院余额清零
--
-- 业务规则：
--   1. 每笔充值对应一条充值订单，支付成功后按订单金额累加医院短信账户余额；
--   2. 回调与主动查单可能重复触发入账，靠订单状态（0待支付 → 1已支付）保证幂等；
--   3. 医院开通时余额为 0，充值记录从 0 开始。
CREATE TABLE IF NOT EXISTS store_recharge_order (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    store_id       BIGINT        NOT NULL COMMENT '支付医院ID',
    order_no       VARCHAR(32)   NOT NULL COMMENT '商户订单号（微信支付 out_trade_no）',
    amount         DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
    status         TINYINT       NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付 1已支付 2已关闭/支付失败',
    prepay_id      VARCHAR(64)   NULL COMMENT '微信支付预支付交易会话标识',
    code_url       VARCHAR(512)  NULL COMMENT 'Native 支付二维码链接',
    transaction_id VARCHAR(64)   NULL COMMENT '微信支付账单号（transaction_id）',
    trade_state    VARCHAR(32)   NULL COMMENT '微信交易状态（SUCCESS/NOTPAY/CLOSED/...）',
    payer_openid   VARCHAR(64)   NULL COMMENT '支付人 openid',
    paid_at        DATETIME      NULL COMMENT '支付成功时间',
    expire_at      DATETIME      NULL COMMENT '订单过期时间',
    mock_flag      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否模拟支付：1是 0否（仅联调期使用）',
    created_by     BIGINT        NULL COMMENT '创建人（发起充值的医院端用户）',
    updated_by     BIGINT        NULL COMMENT '更新人',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at     DATETIME      NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_recharge_order_no (order_no),
    KEY idx_recharge_store (store_id, created_at),
    KEY idx_recharge_txn (transaction_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '医院短信服务充值订单（微信扫码支付）';

-- 存量医院余额清零（此前为演示用初始 100 元；医院开通时余额为 0）
UPDATE store_notify_account
SET balance = 0.00, total_fee = 0.00, total_recharge = 0.00, updated_at = NOW()
WHERE deleted_at IS NULL;
