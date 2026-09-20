-- 取消预约规则优化：取消原因分为「家长原因 / 医院原因」，并记录是否返还预约次数
-- cancel_reason 语义变更为「备注」（非必填）；refund_flag 沿用为「预约次数是否已退还」

ALTER TABLE reserve_order
    ADD COLUMN cancel_reason_type TINYINT NULL COMMENT '取消原因类型：1家长原因 2医院原因' AFTER cancel_reason;

ALTER TABLE reserve_order
    MODIFY COLUMN refund_flag TINYINT NOT NULL DEFAULT 0 COMMENT '预约次数是否已退还：1已退还 0未退还（取消/爽约）';
