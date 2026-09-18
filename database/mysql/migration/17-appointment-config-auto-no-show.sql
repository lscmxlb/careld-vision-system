-- 预约规则新增"爽约自动标记时长"：
-- 预约时段结束后超过该小时数仍未开始养护且未手动处理，系统自动标记为爽约（不退还次数）
-- NOT NULL DEFAULT 12 会自动为存量行回填默认值
ALTER TABLE appointment_config
    ADD COLUMN auto_no_show_hours INT NOT NULL DEFAULT 12 COMMENT '时段结束后自动标记爽约的小时数';
