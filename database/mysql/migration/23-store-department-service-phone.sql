-- 科室新增"服务电话"：
-- 基础信息页「科室管理」手动录入，用于展示医院科室的服务联系方式
ALTER TABLE store_department
    ADD COLUMN service_phone VARCHAR(32) NULL COMMENT '服务电话' AFTER dept_type;
