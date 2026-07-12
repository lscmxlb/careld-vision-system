-- ============================================================
-- Careld 视力养护系统 - 测试数据
-- ============================================================
-- 用途: 开发/测试环境快速初始化业务数据
-- 使用方法: mysql -u root -p careld_vision < test-data.sql
-- 注意: 必须在 init.sql 执行后执行
-- ============================================================

USE careld_vision;

-- ============================================================
-- 1. 角色数据 (如 init.sql 已插入则跳过)
-- ============================================================
INSERT INTO sys_role (role_code, role_name, role_desc, user_type, status) VALUES
('super_admin',    '超级管理员',   '系统超级管理员',     1, 1),
('ops_admin',      '运营管理员',   '总部运营人员',       1, 1),
('store_manager',  '门店店长',     '门店管理人员',       2, 1),
('store_doctor',   '门店医护',     '门店医护人员',       2, 1),
('parent',         '家长',         '家长用户',           3, 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- ============================================================
-- 2. 用户数据
-- ============================================================
-- 密码统一使用 BCrypt 加密，明文均为: Careld@2024
-- BCrypt hash: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG
INSERT INTO sys_user (id, username, password, real_name, phone, email, user_type, store_id, status) VALUES
-- 总部运营人员
(1,  'admin',         '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '超级管理员', '13800000001', 'admin@careld.com',         1, NULL, 1),
(2,  'ops_zhangsan',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '张三',       '13800000002', 'zhangsan@careld.com',     1, NULL, 1),
(3,  'ops_lisi',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '李四',       '13800000003', 'lisi@careld.com',         1, NULL, 1),

-- 北京朝阳门店(STORE001) 员工
(10, 'store001_mgr',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '王建国',     '13800138001', 'wjg@careld.com',          2, 1,    1),
(11, 'store001_doc1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '赵医生',     '13800138002', 'zys@careld.com',          2, 1,    1),
(12, 'store001_doc2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '孙护士',     '13800138003', 'shs@careld.com',          2, 1,    1),

-- 上海浦东门店(STORE002) 员工
(20, 'store002_mgr',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '陈店长',     '13900239001', 'cdz@careld.com',          2, 2,    1),
(21, 'store002_doc1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '刘医生',     '13900239002', 'lys@careld.com',          2, 2,    1),

-- 广州天河门店(STORE003) 员工
(30, 'store003_mgr',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '林店长',     '13700337001', 'lkd@careld.com',          2, 3,    1),
(31, 'store003_doc1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '黄医生',     '13700337002', 'hys@careld.com',          2, 3,    1),

-- 家长用户
(100, 'parent_liu',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '刘妈妈',     '13600136001', 'liuma@163.com',           3, NULL, 1),
(101, 'parent_chen',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '陈爸爸',     '13600136002', 'chenba@qq.com',           3, NULL, 1),
(102, 'parent_wang',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '王妈妈',     '13600136003', 'wangma@gmail.com',        3, NULL, 1),
(103, 'parent_zhao',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '赵爸爸',     '13600136004', 'zhaoba@126.com',          3, NULL, 1),
(104, 'parent_sun',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5HsxXQCd7N1yC3z0Xc4yXqG', '孙妈妈',     '13600136005', 'sunma@163.com',           3, NULL, 1)
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- ============================================================
-- 3. 用户角色关联
-- ============================================================
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1,  1),  -- admin -> super_admin
(2,  2),  -- ops_zhangsan -> ops_admin
(3,  2),  -- ops_lisi -> ops_admin
(10, 3),  -- store001_mgr -> store_manager
(11, 4),  -- store001_doc1 -> store_doctor
(12, 4),  -- store001_doc2 -> store_doctor
(20, 3),  -- store002_mgr -> store_manager
(21, 4),  -- store002_doc1 -> store_doctor
(30, 3),  -- store003_mgr -> store_manager
(31, 4),  -- store003_doc1 -> store_doctor
(100, 5), (101, 5), (102, 5), (103, 5), (104, 5)  -- parents
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- ============================================================
-- 4. 门店数据
-- ============================================================
INSERT INTO store_info (id, store_code, store_name, province_code, province_name, city_code, city_name, district_code, district_name, address, longitude, latitude, contact_name, contact_phone, business_hours, network_type, status, open_time) VALUES
(1, 'STORE001', '北京朝阳门店',   '110000', '北京市', '110100', '北京市', '110105', '朝阳区', '建国路88号',           116.4613, 39.9214, '王建国', '13800138001', '09:00-21:00', 1, 1, '2024-03-15'),
(2, 'STORE002', '上海浦东门店',   '310000', '上海市', '310100', '上海市', '310115', '浦东新区', '陆家嘴环路128号',     121.5057, 31.2389, '陈店长', '13900239001', '09:00-21:00', 1, 1, '2024-05-20'),
(3, 'STORE003', '广州天河门店',   '440000', '广东省', '440100', '广州市', '440106', '天河区', '天河路385号',          113.3291, 23.1375, '林店长', '13700337001', '09:30-21:30', 1, 1, '2024-06-10'),
(4, 'STORE004', '深圳福田门店',   '440000', '广东省', '440300', '深圳市', '440304', '福田区', '福华一路88号',         114.0583, 22.5228, '周店长', '13500435001', '09:00-21:00', 1, 1, '2024-07-01'),
(5, 'STORE005', '杭州西湖门店',   '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '文三路268号',          120.1275, 30.2721, '吴店长', '13400534001', '09:00-20:30', 1, 1, '2024-08-15'),
(6, 'STORE006', '成都锦江门店',   '510000', '四川省', '510100', '成都市', '510104', '锦江区', '红星路三段1号',       104.0803, 30.6556, '郑店长', '13300633001', '09:30-21:00', 2, 1, '2024-09-01'),
(7, 'STORE007', '武汉武昌门店',   '420000', '湖北省', '420100', '武汉市', '420106', '武昌区', '中南路99号',          114.3423, 30.5456, '冯店长', '13200732001', '09:00-21:00', 1, 2, '2024-10-01'),  -- status=2 暂停营业
(8, 'STORE008', '南京鼓楼门店',   '320000', '江苏省', '320100', '南京市', '320106', '鼓楼区', '中山北路30号',        118.7874, 32.0631, '陆店长', '13100831001', '09:00-21:00', 1, 1, '2024-11-15')
ON DUPLICATE KEY UPDATE store_code = VALUES(store_code);

-- ============================================================
-- 5. TV设备数据
-- ============================================================
INSERT INTO store_tv_device (id, device_code, device_name, store_id, android_version, screen_resolution, screen_size, app_version, calibration_status, last_online_time, last_sync_time, status, bind_time) VALUES
(1, 'TV-STORE001-001', '朝阳店大厅TV-1', 1, '12', '3840x2160', 65.0, '1.2.0', 1, NOW() - INTERVAL 1 HOUR,  NOW() - INTERVAL 1 HOUR,  1, '2024-03-15 10:00:00'),
(2, 'TV-STORE001-002', '朝阳店检测室TV', 1, '12', '3840x2160', 55.0, '1.2.0', 1, NOW() - INTERVAL 2 HOUR,  NOW() - INTERVAL 2 HOUR,  1, '2024-03-16 14:00:00'),
(3, 'TV-STORE002-001', '浦东店大厅TV',   2, '13', '3840x2160', 65.0, '1.2.0', 1, NOW() - INTERVAL 30 MIN, NOW() - INTERVAL 30 MIN, 1, '2024-05-20 10:00:00'),
(4, 'TV-STORE003-001', '天河店大厅TV',   3, '11', '1920x1080', 55.0, '1.2.0', 1, NOW() - INTERVAL 3 HOUR,  NOW() - INTERVAL 3 HOUR,  1, '2024-06-10 10:00:00'),
(5, 'TV-STORE004-001', '福田店大厅TV',   4, '12', '3840x2160', 65.0, '1.2.0', 0, NOW() - INTERVAL 1 DAY,   NULL,                   1, '2024-07-01 10:00:00'),  -- 未校准
(6, 'TV-STORE005-001', '西湖店大厅TV',   5, '13', '3840x2160', 55.0, '1.2.0', 1, NOW() - INTERVAL 4 HOUR,  NOW() - INTERVAL 4 HOUR,  1, '2024-08-15 10:00:00'),
(7, 'TV-STORE006-001', '锦江店大厅TV',   6, '12', '1920x1080', 50.0, '1.1.9', 1, NOW() - INTERVAL 5 HOUR,  NOW() - INTERVAL 5 HOUR,  1, '2024-09-01 10:00:00'),
(8, 'TV-STORE008-001', '鼓楼店大厅TV',   8, '13', '3840x2160', 65.0, '1.2.0', 1, NOW() - INTERVAL 1 HOUR,  NOW() - INTERVAL 1 HOUR,  1, '2024-11-15 10:00:00')
ON DUPLICATE KEY UPDATE device_code = VALUES(device_code);

-- ============================================================
-- 6. 儿童档案数据
-- ============================================================
-- name_mask/phone_mask 为脱敏显示, name_encrypted/phone_encrypted 模拟加密存储
INSERT INTO child_profile (id, child_code, store_id, name_encrypted, name_mask, phone_encrypted, phone_mask, birth_date, gender, eye_condition, medical_history, allergy_info, audit_status, parent_user_id, status) VALUES
(1,  'CHILD20240001', 1, 'ENC:张小明',   '张*明', 'ENC:13600136001', '136****6001', '2018-05-15', 1, '轻度近视，左眼4.6 右眼4.7',     '无',   '无',    1, 100, 1),
(2,  'CHILD20240002', 1, 'ENC:刘思涵',   '刘*涵', 'ENC:13600136002', '136****6002', '2019-08-22', 0, '弱视，需要定期训练',             '无',   '青霉素过敏', 1, 101, 1),
(3,  'CHILD20240003', 1, 'ENC:王浩宇',   '王*宇', 'ENC:13600136003', '136****6003', '2017-11-03', 1, '散光100度',                     '无',   '无',    1, 102, 1),
(4,  'CHILD20240004', 2, 'ENC:赵雨欣',   '赵*欣', 'ENC:13600136004', '136****6004', '2020-02-14', 0, '正常视力，预防性检查',           '无',   '海鲜过敏', 1, 103, 1),
(5,  'CHILD20240005', 2, 'ENC:陈子轩',   '陈*轩', 'ENC:13600136005', '136****6005', '2016-09-30', 1, '中度近视，左眼4.3 右眼4.4',     '斜视手术史', '无', 1, 104, 1),
(6,  'CHILD20240006', 2, 'ENC:孙佳怡',   '孙*怡', 'ENC:13800138101', '138****8101', '2018-12-08', 0, '轻度远视',                       '无',   '无',    1, NULL, 1),
(7,  'CHILD20240007', 3, 'ENC:周子豪',   '周*豪', 'ENC:13800138102', '138****8102', '2019-04-18', 1, '假性近视，需要训练恢复',         '无',   '花粉过敏', 1, NULL, 1),
(8,  'CHILD20240008', 3, 'ENC:吴思琪',   '吴*琪', 'ENC:13800138103', '138****8103', '2020-07-25', 0, '正常',                           '无',   '无',    0, NULL, 1),  -- 待审核
(9,  'CHILD20240009', 4, 'ENC:郑博文',   '郑*文', 'ENC:13800138104', '138****8104', '2017-01-12', 1, '高度近视，左眼4.0 右眼4.1',     '无',   '无',    1, NULL, 1),
(10, 'CHILD20240010', 4, 'ENC:冯雅婷',   '冯*婷', 'ENC:13800138105', '138****8105', '2019-10-05', 0, '弱视，右眼矫正中',               '无',   '乳糖不耐受', 1, NULL, 1),
(11, 'CHILD20240011', 5, 'ENC:钱俊杰',   '钱*杰', 'ENC:13800138106', '138****8106', '2018-03-28', 1, '正常视力',                       '无',   '无',    1, NULL, 1),
(12, 'CHILD20240012', 5, 'ENC:何美玲',   '何*玲', 'ENC:13800138107', '138****8107', '2020-06-15', 0, '散光50度',                       '无',   '无',    1, NULL, 1),
(13, 'CHILD20240013', 1, 'ENC:林晨阳',   '林*阳', 'ENC:13800138108', '138****8108', '2019-02-20', 1, '轻度近视，正在养护中',           '无',   '无',    1, NULL, 1),
(14, 'CHILD20240014', 2, 'ENC:黄诗涵',   '黄*涵', 'ENC:13800138109', '138****8109', '2017-07-08', 0, '中度远视，需定期复查',           '无',   '尘螨过敏', 1, NULL, 1),
(15, 'CHILD20240015', 3, 'ENC:杨子墨',   '杨*墨', 'ENC:13800138110', '138****8110', '2021-01-30', 1, '正常',                           '无',   '无',    2, NULL, 1)  -- 已驳回
ON DUPLICATE KEY UPDATE child_code = VALUES(child_code);

-- ============================================================
-- 7. 养护排班数据 (未来7天)
-- ============================================================
INSERT INTO schedule_info (store_id, schedule_date, technician_id, technician_name, time_slot_start, time_slot_end, max_capacity, reserved_count, status) VALUES
-- 北京朝阳门店排班
(1, CURDATE(),             11, '赵医生', '09:00', '10:00', 3, 2, 1),
(1, CURDATE(),             11, '赵医生', '10:00', '11:00', 3, 1, 1),
(1, CURDATE(),             11, '赵医生', '14:00', '15:00', 3, 0, 1),
(1, CURDATE(),             12, '孙护士', '09:00', '10:00', 3, 3, 2),  -- 已满
(1, CURDATE(),             12, '孙护士', '10:00', '11:00', 3, 2, 1),
(1, CURDATE() + INTERVAL 1 DAY, 11, '赵医生', '09:00', '10:00', 3, 0, 1),
(1, CURDATE() + INTERVAL 1 DAY, 11, '赵医生', '10:00', '11:00', 3, 0, 1),
(1, CURDATE() + INTERVAL 1 DAY, 12, '孙护士', '14:00', '15:00', 3, 0, 1),
(1, CURDATE() + INTERVAL 2 DAY, 11, '赵医生', '09:00', '10:00', 3, 0, 1),
(1, CURDATE() + INTERVAL 3 DAY, 12, '孙护士', '09:00', '10:00', 3, 0, 1),

-- 上海浦东门店排班
(2, CURDATE(),             21, '刘医生', '09:00', '10:00', 4, 2, 1),
(2, CURDATE(),             21, '刘医生', '10:00', '11:00', 4, 1, 1),
(2, CURDATE(),             21, '刘医生', '14:00', '15:00', 4, 0, 1),
(2, CURDATE() + INTERVAL 1 DAY, 21, '刘医生', '09:00', '10:00', 4, 0, 1),
(2, CURDATE() + INTERVAL 2 DAY, 21, '刘医生', '09:00', '10:00', 4, 0, 1),

-- 广州天河门店排班
(3, CURDATE(),             31, '黄医生', '09:30', '10:30', 3, 1, 1),
(3, CURDATE(),             31, '黄医生', '10:30', '11:30', 3, 0, 1),
(3, CURDATE() + INTERVAL 1 DAY, 31, '黄医生', '09:30', '10:30', 3, 0, 1)
;

-- ============================================================
-- 8. 预约订单数据
-- ============================================================
INSERT INTO reserve_order (order_no, store_id, schedule_id, child_id, parent_name, parent_phone, reserve_date, reserve_time_start, reserve_time_end, reserve_type, status, source) VALUES
-- 已完成订单
('RO2024070001', 1, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=1 AND s.technician_id=11 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:00' LIMIT 1) t), 1, '刘妈妈', '13600136001', CURDATE() - INTERVAL 7 DAY, '09:00', '10:00', 1, 3, 1),
('RO2024070002', 1, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=1 AND s.technician_id=11 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:00' LIMIT 1) t), 2, '陈爸爸', '13600136002', CURDATE() - INTERVAL 7 DAY, '09:00', '10:00', 1, 3, 1),
('RO2024070003', 1, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=1 AND s.technician_id=12 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:00' LIMIT 1) t), 3, '王妈妈', '13600136003', CURDATE() - INTERVAL 5 DAY, '09:00', '10:00', 2, 3, 2),

-- 今日待到店/服务中
('RO2024070004', 1, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=1 AND s.technician_id=11 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:00' LIMIT 1) t), 1, '刘妈妈', '13600136001', CURDATE(), '09:00', '10:00', 1, 0, 1),  -- 待到店
('RO2024070005', 1, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=1 AND s.technician_id=11 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:00' LIMIT 1) t), 13, '林妈妈', '13800138108', CURDATE(), '09:00', '10:00', 1, 1, 1),  -- 已到店
('RO2024070006', 1, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=1 AND s.technician_id=12 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:00' LIMIT 1) t), 2, '陈爸爸', '13600136002', CURDATE(), '09:00', '10:00', 1, 2, 1),  -- 服务中

-- 已取消订单
('RO2024070007', 1, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=1 AND s.technician_id=11 AND s.schedule_date=CURDATE() AND s.time_slot_start='10:00' LIMIT 1) t), 3, '王妈妈', '13600136003', CURDATE(), '10:00', '11:00', 1, 4, 1),

-- 上海门店订单
('RO2024070008', 2, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=2 AND s.technician_id=21 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:00' LIMIT 1) t), 4, '赵爸爸', '13600136004', CURDATE(), '09:00', '10:00', 1, 0, 1),
('RO2024070009', 2, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=2 AND s.technician_id=21 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:00' LIMIT 1) t), 5, '孙妈妈', '13600136005', CURDATE(), '09:00', '10:00', 1, 3, 1),
('RO2024070010', 2, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=2 AND s.technician_id=21 AND s.schedule_date=CURDATE() AND s.time_slot_start='10:00' LIMIT 1) t), 6, '匿名家长', '13800138101', CURDATE(), '10:00', '11:00', 2, 0, 2),

-- 广州门店订单
('RO2024070011', 3, (SELECT id FROM (SELECT s.id FROM schedule_info s WHERE s.store_id=3 AND s.technician_id=31 AND s.schedule_date=CURDATE() AND s.time_slot_start='09:30' LIMIT 1) t), 7, '周妈妈', '13800138102', CURDATE(), '09:30', '10:30', 1, 1, 1)
;

-- 更新取消订单的取消原因和时间
UPDATE reserve_order SET cancel_reason = '家长临时有事', cancelled_at = NOW() - INTERVAL 2 HOUR, status = 4 WHERE order_no = 'RO2024070007';
UPDATE reserve_order SET completed_at = NOW() - INTERVAL 3 HOUR WHERE order_no IN ('RO2024070001', 'RO2024070002', 'RO2024070003');
UPDATE reserve_order SET completed_at = NOW() - INTERVAL 1 HOUR WHERE order_no = 'RO2024070009';

-- ============================================================
-- 9. 视力检测记录
-- ============================================================
INSERT INTO vision_test_record (record_code, child_id, store_id, device_id, reserve_id, test_type, eye_type, vision_level, vision_decimal, screen_size, test_distance, lighting_condition, tester_name, tester_id, sync_source) VALUES
-- 张小明的检测记录 (养护前后对比)
('VR2024070001', 1, 1, 2, NULL, 1, 1, '4.6', 0.4, 55.0, 5.0, '标准', '赵医生', 11, 1),
('VR2024070002', 1, 1, 2, NULL, 1, 2, '4.7', 0.5, 55.0, 5.0, '标准', '赵医生', 11, 1),
('VR2024070003', 1, 1, 2, NULL, 2, 1, '4.7', 0.5, 55.0, 5.0, '标准', '赵医生', 11, 1),
('VR2024070004', 1, 1, 2, NULL, 2, 2, '4.8', 0.6, 55.0, 5.0, '标准', '赵医生', 11, 1),

-- 刘思涵的检测记录
('VR2024070005', 2, 1, 1, NULL, 1, 1, '4.3', 0.2, 65.0, 5.0, '标准', '赵医生', 11, 1),
('VR2024070006', 2, 1, 1, NULL, 1, 2, '4.4', 0.25, 65.0, 5.0, '标准', '赵医生', 11, 1),
('VR2024070007', 2, 1, 1, NULL, 2, 1, '4.4', 0.25, 65.0, 5.0, '标准', '孙护士', 12, 1),
('VR2024070008', 2, 1, 1, NULL, 2, 2, '4.5', 0.3, 65.0, 5.0, '标准', '孙护士', 12, 1),

-- 王浩宇的检测记录
('VR2024070009', 3, 1, 2, NULL, 1, 1, '4.7', 0.5, 55.0, 5.0, '标准', '孙护士', 12, 1),
('VR2024070010', 3, 1, 2, NULL, 1, 2, '4.8', 0.6, 55.0, 5.0, '标准', '孙护士', 12, 1),

-- 赵雨欣(上海)
('VR2024070011', 4, 2, 3, NULL, 1, 1, '5.0', 1.0, 65.0, 5.0, '标准', '刘医生', 21, 1),
('VR2024070012', 4, 2, 3, NULL, 1, 2, '5.0', 1.0, 65.0, 5.0, '标准', '刘医生', 21, 1),

-- 陈子轩(上海，中度近视)
('VR2024070013', 5, 2, 3, NULL, 1, 1, '4.3', 0.2, 65.0, 5.0, '标准', '刘医生', 21, 1),
('VR2024070014', 5, 2, 3, NULL, 1, 2, '4.4', 0.25, 65.0, 5.0, '标准', '刘医生', 21, 1),
('VR2024070015', 5, 2, 3, NULL, 2, 1, '4.4', 0.25, 65.0, 5.0, '标准', '刘医生', 21, 1),
('VR2024070016', 5, 2, 3, NULL, 2, 2, '4.5', 0.3, 65.0, 5.0, '标准', '刘医生', 21, 1),

-- 周子豪(广州)
('VR2024070017', 7, 3, 4, NULL, 1, 1, '4.5', 0.3, 55.0, 5.0, '标准', '黄医生', 31, 1),
('VR2024070018', 7, 3, 4, NULL, 1, 2, '4.6', 0.4, 55.0, 5.0, '标准', '黄医生', 31, 1),

-- 郑博文(深圳，高度近视)
('VR2024070019', 9, 4, 5, NULL, 1, 1, '4.0', 0.1, 65.0, 5.0, '标准', '周店长', NULL, 2),
('VR2024070020', 9, 4, 5, NULL, 1, 2, '4.1', 0.12, 65.0, 5.0, '标准', '周店长', NULL, 2)
;

-- ============================================================
-- 10. 数据同步日志
-- ============================================================
INSERT INTO sync_log (device_id, store_id, sync_type, sync_batch_id, record_count, success_count, fail_count, status, start_time, end_time, duration_ms) VALUES
(1, 1, 1, 'BATCH-20240700-001', 5, 5, 0, 1, NOW() - INTERVAL 2 HOUR,  NOW() - INTERVAL 2 HOUR + INTERVAL 3 SECOND,  3200),
(2, 1, 1, 'BATCH-20240700-002', 3, 3, 0, 1, NOW() - INTERVAL 1 HOUR,  NOW() - INTERVAL 1 HOUR + INTERVAL 2 SECOND,  2100),
(3, 2, 1, 'BATCH-20240700-003', 8, 8, 0, 1, NOW() - INTERVAL 30 MIN, NOW() - INTERVAL 30 MIN + INTERVAL 5 SECOND, 5400),
(4, 3, 1, 'BATCH-20240700-004', 2, 2, 0, 1, NOW() - INTERVAL 3 HOUR,  NOW() - INTERVAL 3 HOUR + INTERVAL 1 SECOND,  1200),
(5, 4, 2, 'BATCH-20240700-005', 10, 10, 0, 1, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR + INTERVAL 8 SECOND, 8100),
(6, 5, 1, 'BATCH-20240700-006', 4, 4, 0, 1, NOW() - INTERVAL 5 HOUR,  NOW() - INTERVAL 5 HOUR + INTERVAL 3 SECOND,  3500),
(7, 6, 1, 'BATCH-20240700-007', 6, 5, 1, 2, NOW() - INTERVAL 6 HOUR,  NOW() - INTERVAL 6 HOUR + INTERVAL 10 SECOND, 10200),
(8, 8, 1, 'BATCH-20240700-008', 3, 3, 0, 1, NOW() - INTERVAL 1 HOUR,  NOW() - INTERVAL 1 HOUR + INTERVAL 2 SECOND,  2500)
;

-- ============================================================
-- 11. 系统操作日志
-- ============================================================
INSERT INTO sys_operation_log (log_type, user_id, user_type, user_name, store_id, module, action, description, request_method, request_url, ip_address, execute_time, status) VALUES
(2, 1,   1, 'admin',        NULL, 'auth',    'login',  '管理员登录系统',         'POST', '/api/v1/auth/login',       '192.168.1.100', 120, 1),
(1, 1,   1, 'admin',        NULL, 'store',   'create', '创建门店: 北京朝阳门店', 'POST', '/api/v1/stores',           '192.168.1.100', 85,  1),
(1, 2,   1, 'ops_zhangsan', NULL, 'child',   'audit',  '审核通过儿童档案: CHILD20240001', 'PUT', '/api/v1/children/1/audit', '192.168.1.101', 65,  1),
(1, 10,  2, 'store001_mgr', 1,    'schedule','create', '创建排班: 赵医生 09:00-10:00', 'POST', '/api/v1/schedules',      '192.168.1.110', 45,  1),
(1, 11,  2, 'store001_doc1',1,    'vision',  'create', '录入视力检测: 张小明 左眼4.6',   'POST', '/api/v1/vision/tests',   '192.168.1.111', 78,  1),
(1, 100, 3, 'parent_liu',   NULL, 'reserve', 'create', '家长预约: 刘妈妈 朝阳店',        'POST', '/api/v1/reserves',       '10.0.0.100',    156, 1),
(2, 101, 3, 'parent_chen',  NULL, 'auth',    'login',  '家长登录',                     'POST', '/api/v1/auth/login',     '10.0.0.101',    98,  1),
(1, 1,   1, 'admin',        NULL, 'user',    'create', '创建用户: ops_lisi',             'POST', '/api/v1/users',          '192.168.1.100', 52,  1),
(3, NULL,NULL, NULL,         NULL, 'auth',    'error',  '登录失败: 用户名不存在',         'POST', '/api/v1/auth/login',     '10.0.0.200',    15,  0),
(1, 20,  2, 'store002_mgr', 2,    'device',  'bind',   '绑定TV设备: TV-STORE002-001',   'POST', '/api/v1/devices/bind',   '192.168.2.100', 230, 1)
;

-- ============================================================
-- 完成
-- ============================================================
SELECT CONCAT('✅ 测试数据插入完成！',
  '\n  用户: ', (SELECT COUNT(*) FROM sys_user), ' 条',
  '\n  门店: ', (SELECT COUNT(*) FROM store_info), ' 条',
  '\n  设备: ', (SELECT COUNT(*) FROM store_tv_device), ' 条',
  '\n  儿童档案: ', (SELECT COUNT(*) FROM child_profile), ' 条',
  '\n  排班: ', (SELECT COUNT(*) FROM schedule_info), ' 条',
  '\n  预约: ', (SELECT COUNT(*) FROM reserve_order), ' 条',
  '\n  视力记录: ', (SELECT COUNT(*) FROM vision_test_record), ' 条',
  '\n  同步日志: ', (SELECT COUNT(*) FROM sync_log), ' 条',
  '\n  操作日志: ', (SELECT COUNT(*) FROM sys_operation_log), ' 条'
) AS result;
