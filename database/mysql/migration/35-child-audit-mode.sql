-- 基础信息新增"儿童档案"审核方式配置：
-- 0=需要审核（家长建档后需医院审核通过才能使用，默认）；1=自动审核（家长提交后系统自动审核通过）
-- NOT NULL DEFAULT 0 会自动为存量行回填
ALTER TABLE appointment_config
    ADD COLUMN child_audit_mode TINYINT NOT NULL DEFAULT 0 COMMENT '儿童档案审核方式 0=需要审核 1=自动审核';
