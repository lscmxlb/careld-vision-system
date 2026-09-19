-- 医务人员最后登录时间：医院活跃统计（有医生或医生助理登录即为活跃）
ALTER TABLE medical_staff
    ADD COLUMN last_login_time DATETIME NULL COMMENT '最后登录时间（登录成功写入）' AFTER status;
