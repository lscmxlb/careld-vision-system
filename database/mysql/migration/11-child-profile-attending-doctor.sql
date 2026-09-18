-- 11. 儿童档案增加主治医生（医生建档/编辑必选；家长自建档案审核通过时必选）
-- doctor_id 指向 medical_staff.id，doctor_name 为姓名快照（列表展示用）

ALTER TABLE child_profile
    ADD COLUMN doctor_id BIGINT UNSIGNED NULL COMMENT '主治医生ID' AFTER parent_user_id,
    ADD COLUMN doctor_name VARCHAR(50) NULL COMMENT '主治医生姓名快照' AFTER doctor_id;
