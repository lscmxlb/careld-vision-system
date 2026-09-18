-- 记录门店端创建预约的操作人姓名
ALTER TABLE reserve_order ADD COLUMN operator_name VARCHAR(50) NULL COMMENT '预约操作人姓名';
