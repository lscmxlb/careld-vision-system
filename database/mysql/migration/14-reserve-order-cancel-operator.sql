-- 记录取消预约操作人（账户ID与解析后的姓名）
ALTER TABLE reserve_order ADD COLUMN cancel_operator_id BIGINT UNSIGNED NULL COMMENT '取消操作人账户ID';
ALTER TABLE reserve_order ADD COLUMN cancel_operator_name VARCHAR(64) NULL COMMENT '取消操作人姓名';
