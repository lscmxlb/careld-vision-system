-- 短信服务配置：管理后台"系统设置→系统参数→短信配置"读写，auth-service 发送验证码时读取
-- 未启用真实发送时（sms.verify.enabled=false），验证码回退固定 123456（仅开发/测试环境使用）
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key   VARCHAR(128)    NOT NULL COMMENT '配置键',
    config_value TEXT                     COMMENT '配置值',
    config_group VARCHAR(64)     NOT NULL DEFAULT 'default' COMMENT '配置分组',
    remark       VARCHAR(255)             COMMENT '说明',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置表';

INSERT IGNORE INTO sys_config (config_key, config_value, config_group, remark) VALUES
    ('sms.verify.enabled', 'false', 'sms', '短信验证码真实发送开关：开启后调用阿里云短信发送'),
    ('sms.aliyun.accessKeyId', '', 'sms', '阿里云 AccessKey ID'),
    ('sms.aliyun.accessKeySecret', '', 'sms', '阿里云 AccessKey Secret（AES 加密存储）'),
    ('sms.aliyun.signName', '', 'sms', '阿里云短信签名'),
    ('sms.aliyun.templateCode', '', 'sms', '阿里云短信模板 Code'),
    ('sms.aliyun.templateParam', 'code', 'sms', '短信模板中验证码的变量名'),
    ('notice.enableSms', 'true', 'notice', '开启短信通知（预约提醒等）'),
    ('notice.appointmentReminderHours', '2', 'notice', '预约提醒提前小时数');
