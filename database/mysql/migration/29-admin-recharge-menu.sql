-- 后台系统设置新增「充值记录」菜单（parent=8 系统设置，/settings/recharge）
-- 记录医院微信扫码充值流水，仅总部（hq_admin，role_id=7）可见
INSERT INTO sys_menu (parent_id, menu_name, menu_type, menu_path, menu_icon, permission_key, sort_order, visible, status)
SELECT 8, '充值记录', 2, '/settings/recharge', 'Money', 'settings:view', 4, 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_path = '/settings/recharge');

INSERT INTO sys_role_menu (role_id, menu_id, actions)
SELECT 7, m.id, '["view","create","update","delete","release","resetPwd","toggleStatus"]'
FROM sys_menu m
WHERE m.menu_path = '/settings/recharge'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 7 AND rm.menu_id = m.id);
