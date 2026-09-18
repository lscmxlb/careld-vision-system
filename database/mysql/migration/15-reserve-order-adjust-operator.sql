-- 记录预约调整操作人与调整标记
ALTER TABLE reserve_order ADD COLUMN adjust_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否已调整（1=已调整）';
ALTER TABLE reserve_order ADD COLUMN adjust_operator_id BIGINT UNSIGNED NULL COMMENT '调整操作人账户ID';
ALTER TABLE reserve_order ADD COLUMN adjust_operator_name VARCHAR(64) NULL COMMENT '调整操作人姓名';
