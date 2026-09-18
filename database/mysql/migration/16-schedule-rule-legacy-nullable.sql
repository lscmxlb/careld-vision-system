-- 排班规则支持"只配置工作日"或"只配置周六日"：
-- 未配置的日类型不再冗余写入主表旧列，需放开这 6 个旧列的非空约束（旧列为列表展示兼容字段，
-- 多时段明细以 schedule_rule_period 为准；容量列保留默认值 1 以兼容未显式赋值的写入）
ALTER TABLE schedule_rule
    MODIFY COLUMN weekday_start_time TIME NULL COMMENT '周一~五接待起始时间（未配置工作日时段时为空）',
    MODIFY COLUMN weekday_end_time   TIME NULL COMMENT '周一~五接待结束时间（未配置工作日时段时为空）',
    MODIFY COLUMN weekday_capacity   INT  NULL DEFAULT 1 COMMENT '周一~五每小时接待上限（未配置工作日时段时为空）',
    MODIFY COLUMN weekend_start_time TIME NULL COMMENT '周六日接待起始时间（未配置周六日时段时为空）',
    MODIFY COLUMN weekend_end_time   TIME NULL COMMENT '周六日接待结束时间（未配置周六日时段时为空）',
    MODIFY COLUMN weekend_capacity   INT  NULL DEFAULT 1 COMMENT '周六日每小时接待上限（未配置周六日时段时为空）';
