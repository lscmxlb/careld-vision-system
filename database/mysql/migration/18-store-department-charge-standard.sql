-- 科室新增"收费标准"：
-- 编辑科室时手动录入（单位元），用于儿童档案-预约授权时自动计算缴费金额
ALTER TABLE store_department
    ADD COLUMN charge_standard DECIMAL(10,2) NULL COMMENT '收费标准(元)' AFTER dept_type;
