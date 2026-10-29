-- 运营中心管理员（center_admin）项目体验卡补充「导出 Excel」操作权限
-- 背景：admin-web 体验卡页「导出 Excel」按钮由 v-permission="'trialcard:export'" 控制；
--       trialcard:export 由 sys_role_menu.actions 在菜单 trialcard:view（menu_type=2）上派生
--       （PermissionServiceImpl：baseKey 的 :view 后缀替换为 :action）。
--       center_admin 现有 actions 缺 export，导致运营中心登录后按钮不显示；
--       总部管理员（hq_admin，迁移32）与超级管理员不受影响。
-- 幂等：仅当该角色已拥有该项目体验卡菜单且 actions 尚未包含 export 时追加。
UPDATE sys_role_menu rm
JOIN sys_menu m ON m.id = rm.menu_id
JOIN sys_role r ON r.id = rm.role_id
SET rm.actions = JSON_ARRAY_APPEND(rm.actions, '$', 'export')
WHERE r.role_code = 'center_admin'
  AND m.permission_key = 'trialcard:view'
  AND rm.actions IS NOT NULL
  AND JSON_VALID(rm.actions)
  AND JSON_CONTAINS(rm.actions, '"export"') = 0;
