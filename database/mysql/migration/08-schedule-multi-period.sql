-- ============================================================
-- 迁移 08：排班规则多时段 + 例外日备注
-- 1) 新增 schedule_rule_period：每条规则在"周一~五 / 周六日"下可配置多个不重复时段
-- 2) schedule_rule_exception 增加 remark 列：例外日支持分批并注明原因
-- 3) 存量单时段规则自动迁移为 period 记录
-- ============================================================

USE careld_vision;

-- 1. 规则时段子表
CREATE TABLE IF NOT EXISTS schedule_rule_period (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    rule_id BIGINT UNSIGNED NOT NULL COMMENT '关联排班规则',
    day_type TINYINT NOT NULL COMMENT '日类型:1周一~五 2周六日',
    start_time TIME NOT NULL COMMENT '时段开始',
    end_time TIME NOT NULL COMMENT '时段结束',
    capacity INT NOT NULL DEFAULT 1 COMMENT '每小时接待上限',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班规则时段（每规则每周类型可多条）';

-- 2. 例外日备注
ALTER TABLE schedule_rule_exception
    ADD COLUMN remark VARCHAR(255) NULL COMMENT '例外说明（该批例外日的原因）' AFTER exception_date;

-- 3. 存量单时段迁移为 period 记录（幂等：仅当该规则尚无 period 时）
INSERT INTO schedule_rule_period (rule_id, day_type, start_time, end_time, capacity)
SELECT r.id, 1, r.weekday_start_time, r.weekday_end_time, r.weekday_capacity
FROM schedule_rule r
WHERE r.weekday_start_time IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM schedule_rule_period p WHERE p.rule_id = r.id AND p.day_type = 1);

INSERT INTO schedule_rule_period (rule_id, day_type, start_time, end_time, capacity)
SELECT r.id, 2, r.weekend_start_time, r.weekend_end_time, r.weekend_capacity
FROM schedule_rule r
WHERE r.weekend_start_time IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM schedule_rule_period p WHERE p.rule_id = r.id AND p.day_type = 2);
