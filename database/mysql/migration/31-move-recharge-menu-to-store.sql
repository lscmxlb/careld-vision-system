-- 后台「充值记录」菜单由「系统设置」移入「医院管理」（parent 8 → 3），
-- 路由同步由 /settings/recharge 改为 /store/recharge（前端路由已随之迁移）。
UPDATE sys_menu
SET parent_id  = 3,
    menu_path  = '/store/recharge',
    sort_order = 3
WHERE menu_path = '/settings/recharge';
