-- 项目体验卡：新增「绑定时间」列
-- 卡被家长兑换后状态置为「已绑定」(status=2) 并记录绑定时间；开始养护核销时状态置为「已使用」(status=1)
ALTER TABLE trial_card
    ADD COLUMN bound_at DATETIME NULL COMMENT '绑定时间（兑换时写入）' AFTER status;
