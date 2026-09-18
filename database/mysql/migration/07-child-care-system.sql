-- =====================================================================
-- 儿童养护服务管理系统改造（spec: docs/02-architecture/05-child-care-service-spec.md）
-- 迁移脚本：07-child-care-system.sql
-- 说明：MySQL 8.0，不使用 ADD COLUMN IF NOT EXISTS（MySQL 不支持）
-- =====================================================================

-- 1. 儿童档案表：来源标记 + 剩余可约次数 ---------------------------------
ALTER TABLE child_profile
    ADD COLUMN source_type TINYINT NOT NULL DEFAULT 2 COMMENT '来源：1=家长添加 2=医生添加' AFTER parent_user_id,
    ADD COLUMN source_user_id BIGINT NULL COMMENT '来源医生用户ID（source_type=2时记录）' AFTER source_type,
    ADD COLUMN remaining_count INT NOT NULL DEFAULT 0 COMMENT '剩余可约次数（冗余，事务内维护）' AFTER source_user_id;

-- 存量档案视为医院侧录入，来源医生未知
UPDATE child_profile SET source_type = 2 WHERE source_user_id IS NULL;

-- 2. 服务次数记录表 -------------------------------------------------------
CREATE TABLE IF NOT EXISTS child_service_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    child_id BIGINT UNSIGNED NOT NULL COMMENT '儿童档案ID',
    store_id BIGINT UNSIGNED NULL COMMENT '所属医院',
    change_type TINYINT NOT NULL COMMENT '1=授予 2=预约扣减 3=取消退还 4=爽约退还 5=爽约不退还',
    change_count INT NOT NULL COMMENT '变更数量（授予为正，扣减为负）',
    appointment_id BIGINT UNSIGNED NULL COMMENT '关联预约订单ID',
    operator_id BIGINT UNSIGNED NULL COMMENT '操作人',
    remark VARCHAR(255) NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_child_id (child_id),
    KEY idx_appointment_id (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='儿童养护服务次数记录';

-- 3. 排班规则表（区间 + 周类型） ------------------------------------------
CREATE TABLE IF NOT EXISTS schedule_rule (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    store_id BIGINT UNSIGNED NOT NULL COMMENT '医院',
    start_date DATE NOT NULL COMMENT '区间起始日期',
    end_date DATE NOT NULL COMMENT '区间结束日期',
    weekday_start_time TIME NOT NULL COMMENT '周一~五接待起始时间',
    weekday_end_time TIME NOT NULL COMMENT '周一~五接待结束时间',
    weekday_capacity INT NOT NULL DEFAULT 1 COMMENT '周一~五每小时接待上限',
    weekend_start_time TIME NOT NULL COMMENT '周六日接待起始时间',
    weekend_end_time TIME NOT NULL COMMENT '周六日接待结束时间',
    weekend_capacity INT NOT NULL DEFAULT 1 COMMENT '周六日每小时接待上限',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_store_date (store_id, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班规则（自然日区间）';

CREATE TABLE IF NOT EXISTS schedule_rule_exception (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    rule_id BIGINT UNSIGNED NOT NULL COMMENT '关联排班规则',
    exception_date DATE NOT NULL COMMENT '例外日（不适用区间规则）',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_date (rule_id, exception_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班规则例外日';

-- 4. 每日可约时段（物化生成，用于并发控制） -------------------------------
CREATE TABLE IF NOT EXISTS schedule_slot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    store_id BIGINT UNSIGNED NOT NULL,
    slot_date DATE NOT NULL COMMENT '日期',
    slot_start_time TIME NOT NULL COMMENT '时段开始（整点间隔）',
    slot_end_time TIME NOT NULL COMMENT '时段结束',
    max_capacity INT NOT NULL DEFAULT 1 COMMENT '接待上限',
    booked_count INT NOT NULL DEFAULT 0 COMMENT '已约人数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=开放 0=关闭',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_date_time (store_id, slot_date, slot_start_time),
    KEY idx_slot_date (slot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日可约时段（排班规则物化）';

-- 5. 预约规则配置表（单医院单行） -----------------------------------------
CREATE TABLE IF NOT EXISTS appointment_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    store_id BIGINT UNSIGNED NOT NULL,
    parent_cancel_hours INT NOT NULL DEFAULT 24 COMMENT '家长可取消的提前小时数',
    doctor_cancel_hours INT NOT NULL DEFAULT 2 COMMENT '医生可取消的提前小时数',
    no_show_buffer_minutes INT NOT NULL DEFAULT 15 COMMENT '预约开始后超过该分钟数未开始视为可标记爽约',
    auto_complete_hours DECIMAL(3,1) NOT NULL DEFAULT 1.5 COMMENT '开始养护后自动完成小时数',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL COMMENT '逻辑删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约规则配置';

-- 6. 预约订单表改造：养护执行与爽约字段 -----------------------------------
ALTER TABLE reserve_order
    ADD COLUMN start_time DATETIME NULL COMMENT '开始养护时间' AFTER completed_at,
    ADD COLUMN executor_id BIGINT UNSIGNED NULL COMMENT '执行医生/助理用户ID' AFTER start_time,
    ADD COLUMN executor_name VARCHAR(64) NULL COMMENT '执行人姓名冗余' AFTER executor_id,
    ADD COLUMN no_show_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否爽约标记' AFTER executor_name,
    ADD COLUMN refund_flag TINYINT NOT NULL DEFAULT 0 COMMENT '爽约次数是否已退还' AFTER no_show_flag,
    ADD COLUMN slot_id BIGINT UNSIGNED NULL COMMENT '关联每日时段ID（新链路）' AFTER schedule_id;

-- 预约状态迁移：1待到店→1已预约；2已到店/3服务中→2养护中；4已完成→3已完成；5已取消→4已取消
UPDATE reserve_order SET status = 2 WHERE status IN (2, 3);
UPDATE reserve_order SET status = 3 WHERE status = 4;
UPDATE reserve_order SET status = 4 WHERE status = 5;

-- 7. 养护记录表 -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS care_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT UNSIGNED NOT NULL COMMENT '关联预约（1:1）',
    child_id BIGINT UNSIGNED NOT NULL COMMENT '儿童',
    store_id BIGINT UNSIGNED NULL COMMENT '医院',
    care_date DATE NOT NULL COMMENT '养护日期',
    time_slot VARCHAR(20) NULL COMMENT '时段（如 09:30-10:30）',
    vision_before_left VARCHAR(20) NULL COMMENT '养护前视力-左眼',
    vision_before_right VARCHAR(20) NULL COMMENT '养护前视力-右眼',
    vision_before_both VARCHAR(20) NULL COMMENT '养护前视力-双眼',
    vision_after_left VARCHAR(20) NULL COMMENT '养护后视力-左眼',
    vision_after_right VARCHAR(20) NULL COMMENT '养护后视力-右眼',
    vision_after_both VARCHAR(20) NULL COMMENT '养护后视力-双眼',
    executor_id BIGINT UNSIGNED NULL COMMENT '执行医生/助理',
    executor_name VARCHAR(64) NULL COMMENT '执行人姓名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=养护中 2=已完成',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment (appointment_id),
    KEY idx_child_id (child_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='养护记录';

-- 8. 短信验证码表 ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS sms_code (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(10) NOT NULL,
    scene TINYINT NOT NULL DEFAULT 1 COMMENT '1=注册/登录',
    expire_at DATETIME NOT NULL,
    used TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_phone (phone, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码';

-- 9. 医务人员表 -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS medical_staff (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属医院',
    name VARCHAR(64) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号（登录账号）',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
    staff_role TINYINT NOT NULL DEFAULT 1 COMMENT '1=医生 2=医生助理',
    login_password VARCHAR(128) NOT NULL COMMENT '登录密码（BCrypt）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_phone (store_id, phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医务人员（医生/医生助理）';
