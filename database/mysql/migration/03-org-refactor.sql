-- ============================================================
-- Careld 视力养护系统 - 组织架构重构迁移脚本
-- 日期: 2026-08-08
-- 说明: 新建总部/运营中心/代理商/设备类型表，扩展现有表
-- ============================================================

SET NAMES utf8mb4;
USE careld_vision;

-- ============================================================
-- 1. 新建：总部表 brand_hq
-- ============================================================
CREATE TABLE IF NOT EXISTS brand_hq (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '总部ID',
    brand_name VARCHAR(100) NOT NULL COMMENT '品牌/总部名称',
    contact_name VARCHAR(50) COMMENT '业务负责人姓名',
    contact_phone VARCHAR(20) COMMENT '业务负责人电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    address VARCHAR(255) COMMENT '总部地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌总部表';

-- ============================================================
-- 2. 新建：运营中心表 ops_center
-- ============================================================
CREATE TABLE IF NOT EXISTS ops_center (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '运营中心ID',
    center_code VARCHAR(50) NOT NULL COMMENT '运营中心编码',
    center_name VARCHAR(100) NOT NULL COMMENT '运营中心名称',
    hq_id BIGINT UNSIGNED NOT NULL COMMENT '所属总部ID',
    contact_name VARCHAR(50) COMMENT '业务负责人姓名',
    contact_phone VARCHAR(20) COMMENT '业务负责人电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    region VARCHAR(255) COMMENT '负责区域',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_center_code (center_code),
    KEY idx_hq_id (hq_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运营中心表';

-- ============================================================
-- 3. 新建：代理商表 agent
-- ============================================================
CREATE TABLE IF NOT EXISTS agent (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '代理商ID',
    agent_code VARCHAR(50) NOT NULL COMMENT '代理商编码',
    agent_name VARCHAR(100) NOT NULL COMMENT '代理商名称',
    center_id BIGINT UNSIGNED NOT NULL COMMENT '所属运营中心ID',
    contact_name VARCHAR(50) COMMENT '业务负责人姓名',
    contact_phone VARCHAR(20) COMMENT '业务负责人电话',
    contact_email VARCHAR(100) COMMENT '联系邮箱',
    region VARCHAR(255) COMMENT '负责区域',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_code (agent_code),
    KEY idx_center_id (center_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理商表';

-- ============================================================
-- 4. 新建：设备类型表 device_type
-- ============================================================
CREATE TABLE IF NOT EXISTS device_type (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备类型ID',
    type_code VARCHAR(50) NOT NULL COMMENT '类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    description VARCHAR(500) COMMENT '类型描述',
    default_service_life INT DEFAULT 36 COMMENT '默认使用寿命(月)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_code (type_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备类型表';

-- ============================================================
-- 5. 扩展：sys_user 表新增字段
-- ============================================================
ALTER TABLE sys_user ADD COLUMN hq_id BIGINT UNSIGNED DEFAULT NULL COMMENT '所属总部ID' AFTER store_id;
ALTER TABLE sys_user ADD COLUMN center_id BIGINT UNSIGNED DEFAULT NULL COMMENT '所属运营中心ID' AFTER hq_id;
ALTER TABLE sys_user ADD COLUMN agent_id BIGINT UNSIGNED DEFAULT NULL COMMENT '所属代理商ID' AFTER center_id;
ALTER TABLE sys_user ADD KEY idx_hq_id (hq_id);
ALTER TABLE sys_user ADD KEY idx_center_id (center_id);
ALTER TABLE sys_user ADD KEY idx_agent_id (agent_id);

-- ============================================================
-- 6. 扩展：store_info 表新增字段
-- ============================================================
ALTER TABLE store_info ADD COLUMN agent_id BIGINT UNSIGNED DEFAULT NULL COMMENT '所属代理商ID' AFTER store_code;
ALTER TABLE store_info ADD COLUMN join_date DATE DEFAULT NULL COMMENT '加盟时间' AFTER open_time;
ALTER TABLE store_info ADD COLUMN bed_count INT DEFAULT 0 COMMENT '床位数' AFTER join_date;
ALTER TABLE store_info ADD KEY idx_agent_id (agent_id);

-- ============================================================
-- 7. 扩展：store_tv_device 表新增字段
-- ============================================================
ALTER TABLE store_tv_device ADD COLUMN device_type_id BIGINT UNSIGNED DEFAULT NULL COMMENT '设备类型ID' AFTER device_code;
ALTER TABLE store_tv_device ADD COLUMN device_sn VARCHAR(100) DEFAULT NULL COMMENT '设备ID/序列号(手动输入)' AFTER device_type_id;
ALTER TABLE store_tv_device ADD COLUMN service_life_months INT DEFAULT NULL COMMENT '使用寿命(月)' AFTER app_version;
ALTER TABLE store_tv_device ADD COLUMN install_date DATE DEFAULT NULL COMMENT '安装日期' AFTER service_life_months;
ALTER TABLE store_tv_device ADD COLUMN expire_date DATE DEFAULT NULL COMMENT '到期日期' AFTER install_date;
ALTER TABLE store_tv_device ADD COLUMN warning_days INT DEFAULT 30 COMMENT '提前预警天数' AFTER expire_date;
ALTER TABLE store_tv_device ADD UNIQUE KEY uk_device_sn (device_sn);
ALTER TABLE store_tv_device ADD KEY idx_device_type_id (device_type_id);
ALTER TABLE store_tv_device ADD KEY idx_expire_date (expire_date);

-- ============================================================
-- 完成
-- ============================================================
SELECT '✅ 迁移脚本执行完成' AS result;
