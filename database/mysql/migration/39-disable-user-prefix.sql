-- 禁用用户数据规则（存量处理）：
-- 1) 禁用用户用户名统一加 ZF- 前缀（已加前缀的不重复处理）
-- 2) 清空禁用用户手机号，释放原用户名/手机号供其它身份重新注册
UPDATE sys_user SET username = CONCAT('ZF-', username), updated_at = NOW()
WHERE status = 0 AND deleted_at IS NULL AND username NOT LIKE 'ZF-%';

UPDATE sys_user SET phone = NULL, updated_at = NOW()
WHERE status = 0 AND deleted_at IS NULL AND phone IS NOT NULL;
