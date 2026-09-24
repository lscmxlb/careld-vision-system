-- 各事件独立短信模板：预约取消 / 预约调整 / 建档成功 / 养护完成
-- 主模板（sms.aliyun.noticeTemplateCode）只承载「预约成功」与复用同一文案的「养护提醒」，
-- 其它事件文案各不相同，按 sms.aliyun.eventTemplate.<event>.code|fields|roles 单独配置；
-- fields 按模板正文里 ${变量} 出现的顺序，roles 为系统提供的变量语义（一一对应）。
INSERT IGNORE INTO sys_config (config_key, config_value, config_group, remark) VALUES
    ('sms.aliyun.eventTemplate.reserve_cancelled.code', 'SMS_512331110', 'sms', '预约取消短信模板 Code'),
    ('sms.aliyun.eventTemplate.reserve_cancelled.fields', 'date,time,hist,of', 'sms', '预约取消模板变量名'),
    ('sms.aliyun.eventTemplate.reserve_cancelled.roles', 'reserveDate,timeRange,storeName,reason', 'sms', '预约取消模板变量含义'),
    ('sms.aliyun.eventTemplate.reserve_adjusted.code', 'SMS_512201091', 'sms', '预约调整短信模板 Code'),
    ('sms.aliyun.eventTemplate.reserve_adjusted.fields', 'date,time,date1,time1', 'sms', '预约调整模板变量名'),
    ('sms.aliyun.eventTemplate.reserve_adjusted.roles', 'oldReserveDate,oldTimeRange,reserveDate,timeRange', 'sms', '预约调整模板变量含义'),
    ('sms.aliyun.eventTemplate.child_created.code', 'SMS_512510216', 'sms', '建档成功短信模板 Code'),
    ('sms.aliyun.eventTemplate.child_created.fields', 'name,hist', 'sms', '建档成功模板变量名'),
    ('sms.aliyun.eventTemplate.child_created.roles', 'childName,storeName', 'sms', '建档成功模板变量含义'),
    ('sms.aliyun.eventTemplate.care_completed.code', 'SMS_512480326', 'sms', '养护完成短信模板 Code'),
    ('sms.aliyun.eventTemplate.care_completed.fields', 'name,hist', 'sms', '养护完成模板变量名'),
    ('sms.aliyun.eventTemplate.care_completed.roles', 'childName,storeName', 'sms', '养护完成模板变量含义');
