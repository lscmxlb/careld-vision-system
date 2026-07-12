-- Careld视力养护系统数据库初始化脚本
-- MySQL 8.0

-- 创建数据库
CREATE DATABASE IF NOT EXISTS careld_vision 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE careld_vision;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(64) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(128) COMMENT '邮箱',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    user_type TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型:1总部运营 2门店医护 3家长',
    store_id BIGINT UNSIGNED COMMENT '所属门店ID(门店医护)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) COMMENT '最后登录IP',
    login_fail_count INT DEFAULT 0 COMMENT '登录失败次数',
    lock_time DATETIME COMMENT '锁定时间',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    updated_by BIGINT UNSIGNED COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME COMMENT '删除时间(软删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_store_id (store_id),
    KEY idx_user_type (user_type),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_desc VARCHAR(256) COMMENT '角色描述',
    user_type TINYINT NOT NULL COMMENT '适用用户类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 门店表
CREATE TABLE IF NOT EXISTS store_info (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '门店ID',
    store_code VARCHAR(32) NOT NULL COMMENT '门店编码',
    store_name VARCHAR(128) NOT NULL COMMENT '门店名称',
    province_code VARCHAR(16) COMMENT '省份编码',
    province_name VARCHAR(64) COMMENT '省份名称',
    city_code VARCHAR(16) COMMENT '城市编码',
    city_name VARCHAR(64) COMMENT '城市名称',
    district_code VARCHAR(16) COMMENT '区县编码',
    district_name VARCHAR(64) COMMENT '区县名称',
    address VARCHAR(256) COMMENT '详细地址',
    longitude DECIMAL(10,7) COMMENT '经度',
    latitude DECIMAL(10,7) COMMENT '纬度',
    contact_name VARCHAR(64) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    business_hours VARCHAR(64) COMMENT '营业时间',
    network_type TINYINT DEFAULT 1 COMMENT '网络类型:1宽带 2 4G 3混合',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用 2暂停',
    open_time DATE COMMENT '开业时间',
    remark TEXT COMMENT '备注',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    updated_by BIGINT UNSIGNED COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_code (store_code),
    KEY idx_province (province_code),
    KEY idx_city (city_code),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店表';

-- TV设备表
CREATE TABLE IF NOT EXISTS store_tv_device (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备唯一码',
    device_name VARCHAR(128) COMMENT '设备名称',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属门店ID',
    android_version VARCHAR(32) COMMENT 'Android系统版本',
    screen_resolution VARCHAR(32) COMMENT '屏幕分辨率',
    screen_size DECIMAL(4,1) COMMENT '屏幕尺寸(英寸)',
    app_version VARCHAR(32) COMMENT 'APK版本号',
    calibration_status TINYINT DEFAULT 0 COMMENT '校准状态:0未校准 1已校准',
    calibration_data JSON COMMENT '校准数据',
    last_online_time DATETIME COMMENT '最后在线时间',
    last_sync_time DATETIME COMMENT '最后同步时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    bind_time DATETIME COMMENT '绑定时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (device_code),
    KEY idx_store_id (store_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TV设备表';

-- 儿童电子档案表
CREATE TABLE IF NOT EXISTS child_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '档案ID',
    child_code VARCHAR(32) COMMENT '档案编号',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属门店ID',
    name_encrypted VARCHAR(256) NOT NULL COMMENT '姓名(加密)',
    name_mask VARCHAR(64) COMMENT '姓名脱敏显示',
    phone_encrypted VARCHAR(256) COMMENT '家长手机号(加密)',
    phone_mask VARCHAR(20) COMMENT '手机号脱敏显示',
    birth_date DATE COMMENT '出生日期',
    gender TINYINT COMMENT '性别:0女 1男',
    id_card_encrypted VARCHAR(512) COMMENT '身份证号(加密)',
    eye_condition TEXT COMMENT '眼部状况',
    medical_history TEXT COMMENT '病史',
    allergy_info TEXT COMMENT '过敏史',
    family_history TEXT COMMENT '家族眼病史',
    audit_status TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态:0待审核 1已通过 2已驳回',
    audit_remark TEXT COMMENT '审核备注',
    audited_by BIGINT UNSIGNED COMMENT '审核人',
    audited_at DATETIME COMMENT '审核时间',
    parent_user_id BIGINT UNSIGNED COMMENT '绑定家长用户ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    updated_by BIGINT UNSIGNED COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_child_code (child_code),
    KEY idx_store_id (store_id),
    KEY idx_audit_status (audit_status),
    KEY idx_parent_user (parent_user_id),
    KEY idx_created_at (created_at),
    FULLTEXT KEY ft_name (name_mask)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='儿童电子档案表';

-- 养护排班表
CREATE TABLE IF NOT EXISTS schedule_info (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排班ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    schedule_date DATE NOT NULL COMMENT '排班日期',
    technician_id BIGINT UNSIGNED NOT NULL COMMENT '技师ID',
    technician_name VARCHAR(64) COMMENT '技师姓名',
    time_slot_start TIME NOT NULL COMMENT '时段开始',
    time_slot_end TIME NOT NULL COMMENT '时段结束',
    max_capacity INT NOT NULL DEFAULT 1 COMMENT '最大预约数',
    reserved_count INT NOT NULL DEFAULT 0 COMMENT '已预约数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0取消 1正常 2已满',
    remark TEXT COMMENT '备注',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_store_date (store_id, schedule_date),
    KEY idx_technician (technician_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养护排班表';

-- 预约订单表
CREATE TABLE IF NOT EXISTS reserve_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    order_no VARCHAR(64) NOT NULL COMMENT '预约单号',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    schedule_id BIGINT UNSIGNED NOT NULL COMMENT '排班ID',
    child_id BIGINT UNSIGNED NOT NULL COMMENT '儿童档案ID',
    parent_name VARCHAR(64) COMMENT '家长姓名',
    parent_phone VARCHAR(20) COMMENT '家长电话',
    reserve_date DATE NOT NULL COMMENT '预约日期',
    reserve_time_start TIME COMMENT '预约开始时间',
    reserve_time_end TIME COMMENT '预约结束时间',
    reserve_type TINYINT DEFAULT 1 COMMENT '预约类型:1普通养护 2首次体验',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0待到店 1已到店 2服务中 3已完成 4已取消',
    source TINYINT DEFAULT 1 COMMENT '来源:1门店录入 2家长预约',
    remark TEXT COMMENT '备注',
    cancel_reason TEXT COMMENT '取消原因',
    cancelled_at DATETIME COMMENT '取消时间',
    completed_at DATETIME COMMENT '完成时间',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_store_id (store_id),
    KEY idx_child_id (child_id),
    KEY idx_schedule_id (schedule_id),
    KEY idx_status (status),
    KEY idx_reserve_date (reserve_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约订单表';

-- 视力检测记录表
CREATE TABLE IF NOT EXISTS vision_test_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    record_code VARCHAR(64) COMMENT '记录编号',
    child_id BIGINT UNSIGNED NOT NULL COMMENT '儿童档案ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    device_id BIGINT UNSIGNED COMMENT 'TV设备ID',
    reserve_id BIGINT UNSIGNED COMMENT '关联预约ID',
    test_type TINYINT NOT NULL COMMENT '检测类型:1养护前 2养护后',
    eye_type TINYINT NOT NULL COMMENT '眼睛:1左眼 2右眼',
    vision_level VARCHAR(10) NOT NULL COMMENT '视力值(如4.8)',
    vision_decimal DECIMAL(3,2) COMMENT '视力小数表示',
    screen_size DECIMAL(4,1) COMMENT '屏幕尺寸(英寸)',
    test_distance DECIMAL(3,1) COMMENT '检测距离(米)',
    lighting_condition VARCHAR(64) COMMENT '光线条件',
    tester_name VARCHAR(64) COMMENT '检测医护姓名',
    tester_id BIGINT UNSIGNED COMMENT '检测医护ID',
    sync_source TINYINT DEFAULT 1 COMMENT '来源:1TV同步 2手动录入',
    device_local_id VARCHAR(64) COMMENT 'TV端本地记录ID',
    synced_at DATETIME COMMENT '同步时间',
    remark TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_code (record_code),
    KEY idx_child_id (child_id),
    KEY idx_store_id (store_id),
    KEY idx_device_id (device_id),
    KEY idx_test_type (test_type),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视力检测记录表';

-- 数据同步日志表
CREATE TABLE IF NOT EXISTS sync_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    device_id BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    sync_type TINYINT NOT NULL COMMENT '同步类型:1上传 2下发',
    sync_batch_id VARCHAR(64) COMMENT '同步批次号',
    record_count INT DEFAULT 0 COMMENT '记录总数',
    success_count INT DEFAULT 0 COMMENT '成功数',
    fail_count INT DEFAULT 0 COMMENT '失败数',
    status TINYINT DEFAULT 0 COMMENT '状态:0进行中 1成功 2失败',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms INT COMMENT '耗时(毫秒)',
    error_code VARCHAR(32) COMMENT '错误码',
    error_msg TEXT COMMENT '错误信息',
    request_data JSON COMMENT '请求数据摘要',
    response_data JSON COMMENT '响应数据摘要',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_store_id (store_id),
    KEY idx_sync_type (sync_type),
    KEY idx_status (status),
    KEY idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据同步日志表';

-- 系统操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    log_type TINYINT DEFAULT 1 COMMENT '日志类型:1操作 2登录 3异常',
    user_id BIGINT UNSIGNED COMMENT '操作用户ID',
    user_type TINYINT COMMENT '用户类型',
    user_name VARCHAR(64) COMMENT '用户名',
    store_id BIGINT UNSIGNED COMMENT '门店ID',
    module VARCHAR(64) COMMENT '功能模块',
    action VARCHAR(64) COMMENT '操作类型',
    description VARCHAR(256) COMMENT '操作描述',
    request_method VARCHAR(16) COMMENT '请求方法',
    request_url VARCHAR(512) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_data TEXT COMMENT '响应数据',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT 'User-Agent',
    device_type VARCHAR(32) COMMENT '设备类型',
    execute_time INT COMMENT '执行时长(ms)',
    status TINYINT DEFAULT 1 COMMENT '状态:0失败 1成功',
    error_msg TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_store_id (store_id),
    KEY idx_module (module),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- 初始化角色数据
INSERT INTO sys_role (role_code, role_name, role_desc, user_type) VALUES
('super_admin', '超级管理员', '系统超级管理员', 1),
('ops_admin', '运营管理员', '总部运营人员', 1),
('store_manager', '门店店长', '门店管理人员', 2),
('store_doctor', '门店医护', '门店医护人员', 2),
('parent', '家长', '家长用户', 3)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 初始化管理员账号 (密码: admin123)
INSERT INTO sys_user (username, password, real_name, user_type, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '超级管理员', 1, 1)
ON DUPLICATE KEY UPDATE username = username;

-- 初始化门店
INSERT INTO store_info (store_code, store_name, province_name, city_name, address, contact_name, contact_phone, status) VALUES
('STORE001', '北京朝阳门店', '北京市', '北京市', '建国路88号', '王店长', '13800138000', 1)
ON DUPLICATE KEY UPDATE store_code = store_code;

-- 提交事务
COMMIT;
