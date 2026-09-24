-- 养护提醒：预约开始前 N 分钟再次发送预约成功短信
-- 1) 新增系统参数 notice.careReminderMinutes（默认 90 分钟），替代未被使用的 notice.appointmentReminderHours
-- 2) 已开通短信的医院默认勾选「养护提醒」通知类型（care_reminder）
INSERT IGNORE INTO sys_config (config_key, config_value, config_group, remark) VALUES
    ('notice.careReminderMinutes', '90', 'notice', '养护提醒提前分钟数（预约开始前再次发送预约成功短信）');

DELETE FROM sys_config WHERE config_key = 'notice.appointmentReminderHours';

UPDATE store_notify_config
SET enabled_types = CONCAT_WS(',', NULLIF(enabled_types, ''), 'care_reminder')
WHERE sms_enabled = 1
  AND deleted_at IS NULL
  AND enabled_types NOT LIKE '%care_reminder%';
