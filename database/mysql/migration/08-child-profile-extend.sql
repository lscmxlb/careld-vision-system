-- =============================================================
-- 08-child-profile-extend.sql
-- 儿童档案扩展字段：家长姓名/关系/家庭地址/所在学校/分娩方式/日常作息
-- 注意：MySQL 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报错
-- =============================================================
USE careld_vision;

ALTER TABLE child_profile
    ADD COLUMN parent_name VARCHAR(64) DEFAULT NULL COMMENT '家长姓名' AFTER phone_mask,
    ADD COLUMN relation VARCHAR(20) DEFAULT NULL COMMENT '与儿童关系：妈妈/爸爸等' AFTER parent_name,
    ADD COLUMN home_address VARCHAR(255) DEFAULT NULL COMMENT '家庭地址' AFTER gender,
    ADD COLUMN school VARCHAR(100) DEFAULT NULL COMMENT '所在学校' AFTER home_address,
    ADD COLUMN delivery_type VARCHAR(20) DEFAULT NULL COMMENT '分娩方式：顺产/剖宫产' AFTER school,
    ADD COLUMN bedtime VARCHAR(10) DEFAULT NULL COMMENT '日常作息-休息时间(HH:mm)' AFTER delivery_type,
    ADD COLUMN wake_time VARCHAR(10) DEFAULT NULL COMMENT '日常作息-起床时间(HH:mm)' AFTER bedtime;
