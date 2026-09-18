-- 10. 次数流水增加缴费信息与开单医生（授予次数时必填）
-- payment_method: 自费/医保/其他
-- doctor_id 指向 medical_staff.id，doctor_name 为姓名快照（流水展示用，医生改名不影响历史流水）

ALTER TABLE child_service_record
    ADD COLUMN payment_amount DECIMAL(10,2) NULL COMMENT '缴费金额（元）' AFTER appointment_id,
    ADD COLUMN payment_method VARCHAR(20) NULL COMMENT '缴费方式：自费/医保/其他' AFTER payment_amount,
    ADD COLUMN doctor_id BIGINT UNSIGNED NULL COMMENT '开单医生ID' AFTER payment_method,
    ADD COLUMN doctor_name VARCHAR(50) NULL COMMENT '开单医生姓名快照' AFTER doctor_id;
