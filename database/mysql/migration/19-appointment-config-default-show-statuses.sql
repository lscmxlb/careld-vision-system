-- 预约规则新增"预约记录默认显示选择"：
-- 医院端预约记录页进入时按勾选的状态过滤列表（1=已预约,2=养护中,3=已完成,4=已取消,5=已爽约）
-- NOT NULL DEFAULT '1,2,3,4,5' 会自动为存量行回填（即默认全选）
ALTER TABLE appointment_config
    ADD COLUMN default_show_statuses VARCHAR(32) NOT NULL DEFAULT '1,2,3,4,5' COMMENT '预约记录默认显示的状态，逗号分隔，1已预约2养护中3已完成4已取消5已爽约';
