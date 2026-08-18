-- ============================================================
-- Careld 视力养护系统 - 数据迁移脚本（现有数据迁移到新架构）
-- 日期: 2026-08-08
-- 前置: 必须先执行 03-org-refactor.sql
-- ============================================================

SET NAMES utf8mb4;
USE careld_vision;

-- ============================================================
-- 1. 插入默认总部
-- ============================================================
INSERT INTO brand_hq (id, brand_name, contact_name, contact_phone, contact_email, address, status) VALUES
(1, 'Careld可尔欧得总部', '超级管理员', '13800000001', 'admin@careld.com', '北京市朝阳区建国路88号', 1)
ON DUPLICATE KEY UPDATE brand_name = VALUES(brand_name);

-- ============================================================
-- 2. 插入运营中心（按区域划分）
-- ============================================================
INSERT INTO ops_center (id, center_code, center_name, hq_id, contact_name, contact_phone, contact_email, region, status) VALUES
(1, 'CENTER_NORTH',  '华北运营中心', 1, '张三', '13800000002', 'zhangsan@careld.com', '北京、武汉', 1),
(2, 'CENTER_EAST',   '华东运营中心', 1, '李四', '13800000003', 'lisi@careld.com',     '上海、杭州、南京', 1),
(3, 'CENTER_SOUTH',  '华南运营中心', 1, '王建国', '13800138001', 'wjg@careld.com',   '广州、深圳', 1),
(4, 'CENTER_WEST',   '西南运营中心', 1, '陈店长', '13900239001', 'cdz@careld.com',    '成都', 1)
ON DUPLICATE KEY UPDATE center_name = VALUES(center_name);

-- ============================================================
-- 3. 插入代理商（对应各门店所在城市）
-- ============================================================
INSERT INTO agent (id, agent_code, agent_name, center_id, contact_name, contact_phone, contact_email, region, status) VALUES
(1, 'AGENT_BJ_CY',   '北京朝阳代理商',     1, '王建国', '13800138001', 'wjg@careld.com',    '北京市朝阳区', 1),
(2, 'AGENT_SH_PD',   '上海浦东代理商',     2, '陈店长', '13900239001', 'cdz@careld.com',    '上海市浦东新区', 1),
(3, 'AGENT_GZ_TH',   '广州天河代理商',     3, '林店长', '13700337001', 'lkd@careld.com',    '广州市天河区', 1),
(4, 'AGENT_SZ_FT',   '深圳福田代理商',     3, '周店长', '13500435001', 'zhou@careld.com',  '深圳市福田区', 1),
(5, 'AGENT_HZ_XH',   '杭州西湖代理商',     2, '吴店长', '13400534001', 'wu@careld.com',     '杭州市西湖区', 1),
(6, 'AGENT_CD_JJ',   '成都锦江代理商',     4, '郑店长', '13300633001', 'zheng@careld.com',  '成都市锦江区', 1),
(7, 'AGENT_WH_WC',   '武汉武昌代理商',     1, '冯店长', '13200732001', 'feng@careld.com',   '武汉市武昌区', 1),
(8, 'AGENT_NJ_GL',   '南京鼓楼代理商',     2, '陆店长', '13100831001', 'lu@careld.com',     '南京市鼓楼区', 1)
ON DUPLICATE KEY UPDATE agent_name = VALUES(agent_name);

-- ============================================================
-- 4. 更新门店关联代理商 + 加盟时间 + 床位数
-- ============================================================
UPDATE store_info SET agent_id = 1, join_date = '2024-03-15', bed_count = 10 WHERE store_code = 'STORE001';
UPDATE store_info SET agent_id = 2, join_date = '2024-05-20', bed_count = 8  WHERE store_code = 'STORE002';
UPDATE store_info SET agent_id = 3, join_date = '2024-06-10', bed_count = 6  WHERE store_code = 'STORE003';
UPDATE store_info SET agent_id = 4, join_date = '2024-07-01', bed_count = 5  WHERE store_code = 'STORE004';
UPDATE store_info SET agent_id = 5, join_date = '2024-08-15', bed_count = 4  WHERE store_code = 'STORE005';
UPDATE store_info SET agent_id = 6, join_date = '2024-09-01', bed_count = 6  WHERE store_code = 'STORE006';
UPDATE store_info SET agent_id = 7, join_date = '2024-10-01', bed_count = 3  WHERE store_code = 'STORE007';
UPDATE store_info SET agent_id = 8, join_date = '2024-11-15', bed_count = 5  WHERE store_code = 'STORE008';

-- ============================================================
-- 5. 更新用户关联层级
-- ============================================================
-- 总部人员
UPDATE sys_user SET hq_id = 1 WHERE id IN (1, 2, 3);
-- 北京朝阳门店员工 -> 代理商1
UPDATE sys_user SET agent_id = 1, center_id = 1 WHERE id IN (10, 11, 12);
-- 上海浦东门店员工 -> 代理商2
UPDATE sys_user SET agent_id = 2, center_id = 2 WHERE id IN (20, 21);
-- 广州天河门店员工 -> 代理商3
UPDATE sys_user SET agent_id = 3, center_id = 3 WHERE id IN (30, 31);

-- ============================================================
-- 6. 插入默认设备类型
-- ============================================================
INSERT INTO device_type (type_code, type_name, description, default_service_life, status) VALUES
('TV_TERMINAL',    'TV终端',       '门店电视终端设备，用于视力表显示', 60, 1),
('VISION_TESTER',  '视力检测仪',   '专业视力检测设备',                 36, 1),
('CARE_DEVICE',    '养护仪',       '视力养护治疗设备',                 48, 1),
('SCREENING_UNIT', '筛查设备',     '便携式视力筛查设备',               24, 1)
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name);

-- ============================================================
-- 7. 更新现有设备：补充设备类型、序列号、寿命信息
-- ============================================================
-- TV终端类型ID
SET @tv_type_id = (SELECT id FROM device_type WHERE type_code = 'TV_TERMINAL' LIMIT 1);

UPDATE store_tv_device SET 
    device_type_id = @tv_type_id,
    device_sn = CONCAT('SN-', LPAD(id, 6, '0')),
    service_life_months = 60,
    install_date = bind_time,
    expire_date = DATE_ADD(bind_time, INTERVAL 60 MONTH),
    warning_days = 30
WHERE device_sn IS NULL;

-- ============================================================
-- 完成
-- ============================================================
SELECT CONCAT('✅ 数据迁移完成！',
  '\n  总部: ', (SELECT COUNT(*) FROM brand_hq), ' 条',
  '\n  运营中心: ', (SELECT COUNT(*) FROM ops_center), ' 条',
  '\n  代理商: ', (SELECT COUNT(*) FROM agent), ' 条',
  '\n  设备类型: ', (SELECT COUNT(*) FROM device_type), ' 条',
  '\n  门店已关联代理商: ', (SELECT COUNT(*) FROM store_info WHERE agent_id IS NOT NULL), ' 条',
  '\n  用户已关联层级: ', (SELECT COUNT(*) FROM sys_user WHERE hq_id IS NOT NULL OR agent_id IS NOT NULL), ' 条',
  '\n  设备已补充类型: ', (SELECT COUNT(*) FROM store_tv_device WHERE device_type_id IS NOT NULL), ' 条'
) AS result;
