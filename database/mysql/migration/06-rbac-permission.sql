-- ============================================================
-- Careld 视力养护系统 - RBAC 权限控制迁移脚本
-- 日期: 2026-08-08
-- 说明: 新建菜单表、角色菜单关联表，扩展角色表，初始化权限数据
-- ============================================================

SET NAMES utf8mb4;
USE careld_vision;

-- ============================================================
-- 1. 新建：菜单表 sys_menu
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_menu (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    parent_id       BIGINT UNSIGNED DEFAULT 0 COMMENT '父菜单ID(0=顶级)',
    menu_name       VARCHAR(64) NOT NULL COMMENT '菜单名称',
    menu_type       TINYINT NOT NULL DEFAULT 1 COMMENT '类型:1目录 2菜单 3按钮',
    menu_path       VARCHAR(128) DEFAULT NULL COMMENT '前端路由路径(目录/菜单)',
    menu_icon       VARCHAR(64) DEFAULT NULL COMMENT '菜单图标',
    permission_key  VARCHAR(128) DEFAULT NULL COMMENT '权限标识(如 organization:center:view)',
    sort_order      INT NOT NULL DEFAULT 0 COMMENT '排序(升序)',
    visible         TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见:0隐藏 1显示',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_permission_key (permission_key),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- ============================================================
-- 2. 新建：角色-菜单关联表 sys_role_menu
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    menu_id     BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
    actions     VARCHAR(256) DEFAULT '["view"]' COMMENT '允许的操作列表(JSON数组)',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ============================================================
-- 3. 扩展：sys_role 表新增字段
-- ============================================================
-- 使用存储过程安全添加列（避免重复执行报错）
DROP PROCEDURE IF EXISTS add_role_columns;
DELIMITER $$
CREATE PROCEDURE add_role_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='careld_vision' AND TABLE_NAME='sys_role' AND COLUMN_NAME='data_scope') THEN
        ALTER TABLE sys_role ADD COLUMN data_scope TINYINT NOT NULL DEFAULT 1 COMMENT '数据范围:1全部 2本中心及下级 3本代理商及下级 4本医院 5个人' AFTER role_desc;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='careld_vision' AND TABLE_NAME='sys_role' AND COLUMN_NAME='sort_order') THEN
        ALTER TABLE sys_role ADD COLUMN sort_order INT DEFAULT 0 COMMENT '排序' AFTER data_scope;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='careld_vision' AND TABLE_NAME='sys_role' AND INDEX_NAME='idx_sort_order') THEN
        ALTER TABLE sys_role ADD KEY idx_sort_order (sort_order);
    END IF;
END$$
DELIMITER ;
CALL add_role_columns();
DROP PROCEDURE IF EXISTS add_role_columns;

-- ============================================================
-- 4. 初始化菜单数据
-- ============================================================
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, menu_path, menu_icon, permission_key, sort_order) VALUES
-- 顶级目录/菜单
(1,  0, '数据看板',   2, '/dashboard',          'HomeFilled',      'dashboard:view',          1),
(2,  0, '组织架构',   1, '/organization',        'OfficeBuilding',  NULL,                       2),
(3,  0, '医院管理',   1, '/store',               'Shop',             NULL,                       3),
(4,  0, '设备管理',   1, '/device',              'Monitor',          NULL,                       4),
(5,  0, '用户管理',   2, '/user',                'UserFilled',       'user:view',                5),
(6,  0, '统计报表',   2, '/statistics',          'DataAnalysis',     'statistics:view',          6),
(7,  0, '操作日志',   2, '/operation-log',       'Document',         'operationlog:view',        7),
(8,  0, '系统设置',   1, '/settings',            'Setting',          NULL,                       8),

-- 组织架构子菜单
(10, 2, '运营中心',   2, '/organization/centers','OfficeBuilding',  'organization:center:view', 1),
(11, 2, '代理商管理', 2, '/organization/agents', 'Connection',       'organization:agent:view',  2),

-- 医院管理子菜单
(20, 3, '医院列表',   2, '/store/list',          'Shop',             'store:list:view',          1),

-- 设备管理子菜单
(30, 4, '设备列表',   2, '/device/list',         'Monitor',          'device:list:view',         1),
(31, 4, '设备类型',   2, '/device/types',        'Cpu',              'device:type:view',         2),

-- 系统设置子菜单
(80, 8, '系统参数',   2, '/settings',            'Setting',          'settings:view',            1),
(81, 8, '角色权限',   2, '/settings/role',       'UserFilled',        'settings:role:view',       2),
(82, 8, '菜单管理',   2, '/settings/menu',       'Menu',              'settings:menu:view',       3)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 按钮级权限 (menu_type=3)
INSERT INTO sys_menu (parent_id, menu_name, menu_type, permission_key, sort_order) VALUES
-- 运营中心按钮
(10, '新增运营中心', 3, 'organization:center:create', 1),
(10, '编辑运营中心', 3, 'organization:center:update', 2),
(10, '删除运营中心', 3, 'organization:center:delete', 3),
-- 代理商按钮
(11, '新增代理商',   3, 'organization:agent:create',   1),
(11, '编辑代理商',   3, 'organization:agent:update',   2),
(11, '删除代理商',   3, 'organization:agent:delete',   3),
-- 医院按钮
(20, '新增医院',     3, 'store:list:create',           1),
(20, '编辑医院',     3, 'store:list:update',           2),
(20, '删除医院',     3, 'store:list:delete',           3),
-- 设备按钮
(30, '新增设备',     3, 'device:list:create',          1),
(30, '编辑设备',     3, 'device:list:update',          2),
(30, '删除设备',     3, 'device:list:delete',          3),
(30, '释放设备',     3, 'device:list:release',         4),
-- 设备类型按钮
(31, '新增设备类型', 3, 'device:type:create',          1),
(31, '编辑设备类型', 3, 'device:type:update',          2),
(31, '删除设备类型', 3, 'device:type:delete',          3),
-- 用户按钮
(5,  '新增用户',     3, 'user:create',                1),
(5,  '编辑用户',     3, 'user:update',                2),
(5,  '删除用户',     3, 'user:delete',                 3),
(5,  '重置密码',     3, 'user:resetPwd',               4),
(5,  '启用/禁用',    3, 'user:toggleStatus',           5),
-- 角色管理按钮
(81, '新增角色',     3, 'settings:role:create',        1),
(81, '编辑角色',     3, 'settings:role:update',        2),
(81, '删除角色',     3, 'settings:role:delete',        3),
-- 菜单管理按钮
(82, '新增菜单',     3, 'settings:menu:create',        1),
(82, '编辑菜单',     3, 'settings:menu:update',        2),
(82, '删除菜单',     3, 'settings:menu:delete',        3);

-- ============================================================
-- 5. 更新角色数据
-- ============================================================
UPDATE sys_role SET data_scope = 1, sort_order = 1 WHERE role_code = 'super_admin';

-- 新增角色（如果不存在）
INSERT INTO sys_role (role_code, role_name, role_desc, user_type, data_scope, sort_order) VALUES
('hq_admin',       '总部管理员',    '总部运营管理',      1, 1, 2),
('center_admin',   '运营中心管理员', '运营中心管理',      4, 2, 3),
('agent_admin',    '代理商管理员',   '代理商管理',        5, 3, 4),
('hospital_admin', '医院管理员',     '医院维护管理',      2, 4, 5),
('parent',         '家长',           '家长用户',          3, 5, 6)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), data_scope = VALUES(data_scope), sort_order = VALUES(sort_order);

-- ============================================================
-- 6. 初始化角色-菜单权限分配
-- ============================================================

-- 获取角色ID
SET @hq_admin_id = (SELECT id FROM sys_role WHERE role_code = 'hq_admin');
SET @center_admin_id = (SELECT id FROM sys_role WHERE role_code = 'center_admin');
SET @agent_admin_id = (SELECT id FROM sys_role WHERE role_code = 'agent_admin');
SET @hospital_admin_id = (SELECT id FROM sys_role WHERE role_code = 'hospital_admin');
SET @parent_id = (SELECT id FROM sys_role WHERE role_code = 'parent');

-- 6.1 总部管理员：全部菜单 + 全部操作
INSERT INTO sys_role_menu (role_id, menu_id, actions)
SELECT @hq_admin_id, id, '["view","create","update","delete","release","resetPwd","toggleStatus"]'
FROM sys_menu WHERE status = 1
ON DUPLICATE KEY UPDATE actions = VALUES(actions);

-- 6.2 运营中心管理员
INSERT INTO sys_role_menu (role_id, menu_id, actions) VALUES
(@center_admin_id, 1, '["view"]'),
(@center_admin_id, 11, '["view","create","update","delete"]'),
(@center_admin_id, 3, '["view"]'),
(@center_admin_id, 20, '["view","create","update","delete"]'),
(@center_admin_id, 4, '["view"]'),
(@center_admin_id, 30, '["view","create","update","delete","release"]'),
(@center_admin_id, 31, '["view","create","update","delete"]'),
(@center_admin_id, 5, '["view"]'),
(@center_admin_id, 6, '["view"]'),
(@center_admin_id, 8, '["view"]'),
(@center_admin_id, 80, '["view"]')
ON DUPLICATE KEY UPDATE actions = VALUES(actions);

-- 运营中心管理员的用户管理按钮
INSERT INTO sys_role_menu (role_id, menu_id, actions)
SELECT @center_admin_id, id, '["view","create","update","delete","resetPwd","toggleStatus"]'
FROM sys_menu WHERE parent_id = 5 AND menu_type = 3
ON DUPLICATE KEY UPDATE actions = VALUES(actions);

-- 6.3 代理商管理员
INSERT INTO sys_role_menu (role_id, menu_id, actions) VALUES
(@agent_admin_id, 1, '["view"]'),
(@agent_admin_id, 3, '["view"]'),
(@agent_admin_id, 20, '["view","create","update","delete"]'),
(@agent_admin_id, 4, '["view"]'),
(@agent_admin_id, 30, '["view","create","update","delete","release"]'),
(@agent_admin_id, 31, '["view","create","update","delete"]'),
(@agent_admin_id, 5, '["view"]')
ON DUPLICATE KEY UPDATE actions = VALUES(actions);

-- 代理商管理员的用户管理按钮
INSERT INTO sys_role_menu (role_id, menu_id, actions)
SELECT @agent_admin_id, id, '["view","create","update","delete","resetPwd","toggleStatus"]'
FROM sys_menu WHERE parent_id = 5 AND menu_type = 3
ON DUPLICATE KEY UPDATE actions = VALUES(actions);

-- 6.4 医院管理员
INSERT INTO sys_role_menu (role_id, menu_id, actions) VALUES
(@hospital_admin_id, 1, '["view"]'),
(@hospital_admin_id, 4, '["view"]'),
(@hospital_admin_id, 30, '["view","create","update","delete","release"]'),
(@hospital_admin_id, 5, '["view"]')
ON DUPLICATE KEY UPDATE actions = VALUES(actions);

-- 医院管理员的用户管理按钮（仅查看+新增+编辑，不可删除/重置密码）
INSERT INTO sys_role_menu (role_id, menu_id, actions) VALUES
(@hospital_admin_id, (SELECT id FROM sys_menu WHERE permission_key = 'user:create'), '["view"]'),
(@hospital_admin_id, (SELECT id FROM sys_menu WHERE permission_key = 'user:update'), '["view"]'),
(@hospital_admin_id, (SELECT id FROM sys_menu WHERE permission_key = 'user:toggleStatus'), '["view"]')
ON DUPLICATE KEY UPDATE actions = VALUES(actions);

-- 6.5 家长
INSERT INTO sys_role_menu (role_id, menu_id, actions) VALUES
(@parent_id, 1, '["view"]')
ON DUPLICATE KEY UPDATE actions = VALUES(actions);

-- ============================================================
-- 7. 为现有用户分配默认角色
-- ============================================================

-- 根据 user_type 匹配默认角色
-- user_type=1 → hq_admin
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.user_type = 1 AND r.role_code = 'hq_admin'
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- user_type=4 → center_admin
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.user_type = 4 AND r.role_code = 'center_admin'
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- user_type=5 → agent_admin
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.user_type = 5 AND r.role_code = 'agent_admin'
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- user_type=2 → hospital_admin
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.user_type = 2 AND r.role_code = 'hospital_admin'
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- user_type=3 → parent
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.user_type = 3 AND r.role_code = 'parent'
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- ============================================================
-- 完成
-- ============================================================
SELECT '✅ RBAC权限迁移脚本执行完成' AS result;
