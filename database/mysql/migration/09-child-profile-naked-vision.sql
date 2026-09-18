-- =============================================================
-- 09-child-profile-naked-vision.sql
-- 儿童档案新增裸眼视力字段（建档时填写，选填）：双眼/左眼/右眼
-- 注意：MySQL 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报错
-- =============================================================
USE careld_vision;

ALTER TABLE child_profile
    ADD COLUMN naked_vision_both VARCHAR(20) DEFAULT NULL COMMENT '裸眼视力-双眼' AFTER eye_condition,
    ADD COLUMN naked_vision_left VARCHAR(20) DEFAULT NULL COMMENT '裸眼视力-左眼' AFTER naked_vision_both,
    ADD COLUMN naked_vision_right VARCHAR(20) DEFAULT NULL COMMENT '裸眼视力-右眼' AFTER naked_vision_left;
