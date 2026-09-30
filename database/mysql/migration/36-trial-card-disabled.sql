-- 体验卡状态扩展：新增 3=已禁用
-- 1) trial_card.status 列注释更新（0未兑换 1已使用 2已绑定 3已禁用）
--    配套功能：列表操作列「禁用/启用」按钮 +「体验卡管理」按编号区间批量禁用（仅未兑换可禁用）

ALTER TABLE trial_card
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0未兑换 1已使用 2已绑定 3已禁用';
