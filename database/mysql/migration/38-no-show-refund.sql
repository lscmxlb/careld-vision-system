-- 预约规则新增"爽约次数是否返还"配置：
-- 0=否（默认，爽约按现规则扣除次数）；1=是（爽约视为自动取消，退还预约次数）
ALTER TABLE appointment_config
    ADD COLUMN no_show_refund TINYINT NOT NULL DEFAULT 0 COMMENT '爽约次数是否返还 0=否（按现规则扣除） 1=是（视为自动取消，不扣次数）';
