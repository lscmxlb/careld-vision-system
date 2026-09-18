-- 预约记录 v2 流程不依赖旧 schedule_id，允许为空
ALTER TABLE reserve_order MODIFY COLUMN schedule_id bigint unsigned NULL COMMENT '排班ID';
