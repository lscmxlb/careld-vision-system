-- ============================================================
-- Careld 视力养护系统 - 设备释放功能迁移脚本
-- 日期: 2026-08-19
-- 说明: 允许 store_tv_device.store_id 为 NULL，支持"设为空闲"功能
-- ============================================================

SET NAMES utf8mb4;
USE careld_vision;

-- 允许 store_id 为 NULL，空闲设备 store_id 为 NULL
ALTER TABLE store_tv_device MODIFY store_id BIGINT UNSIGNED DEFAULT NULL COMMENT '所属门店ID，空闲设备为NULL';

-- ============================================================
-- 完成
-- ============================================================
SELECT '✅ 迁移脚本执行完成' AS result;
