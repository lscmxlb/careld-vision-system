# Careld 视力养护系统 — RBAC 权限规格文档

> **版本**: 1.0  
> **日期**: 2026-08-08  
> **状态**: 设计阶段（待实现）  

---

## 一、背景与目标

### 1.1 现状分析

当前系统的权限控制存在以下问题：

| 问题 | 说明 |
|------|------|
| **菜单可见性硬编码** | `MainLayout.vue` 中通过 `v-if="userType === 1"` 等方式硬编码菜单可见性，无法动态调整 |
| **无操作级权限控制** | 后端所有接口仅校验 `authenticated()`，任何登录用户都能调用增删改查接口 |
| **permissions 为通配符** | 登录时返回 `permissions: ["*"]`，前端 `hasPermission` 永远为 true |
| **角色表未使用** | 数据库有 `sys_role` 和 `sys_user_role` 表但未实际使用，无角色-权限关联表 |
| **无法自定义权限** | 新增用户类型或调整菜单权限需要改代码，无法通过管理后台动态配置 |

### 1.2 设计目标

- **菜单权限**：不同用户/角色登录后只能看到其有权限的菜单
- **操作权限**：每个页面的增、删、改、查按钮根据权限显示或隐藏
- **接口权限**：后端 API 根据用户权限进行校验，无权限返回 403
- **可视化配置**：在系统设置页面可以创建角色、分配菜单和操作权限
- **向后兼容**：保留现有 `userType` 层级逻辑作为兜底，RBAC 作为精细控制层

---

## 二、核心概念

### 2.1 权限模型

采用 **RBAC（Role-Based Access Control）** 模型：

```
用户(User) ──N:N── 角色(Role) ──N:N── 权限(Permission = 菜单 + 操作)
```

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  sys_user │     │ sys_user_role│     │  sys_role    │     │sys_role_menu │
│           │     │              │     │              │     │              │
│ id        │◄───►│ user_id      │     │ id           │◄───►│ role_id      │
│ username  │     │ role_id      │◄───►│ role_code    │     │ menu_id      │
│ user_type │     └──────────────┘     │ role_name    │     │ actions(JSON)│
│ center_id │                          │ user_type    │     └──────┬───────┘
│ agent_id  │                          │ status      │            │
│ store_id  │                          │ data_scope  │            ▼
└──────────┘                          └──────────────┘     ┌──────────────┐
                                                           │  sys_menu    │
                                                           │              │
                                                           │ id           │
                                                           │ parent_id    │
                                                           │ menu_name    │
                                                           │ menu_path    │
                                                           │ menu_icon    │
                                                           │ sort_order   │
                                                           │ menu_type    │
                                                           │ permission_key│
                                                           └──────────────┘
```

### 2.2 权限标识规范

权限标识采用 `模块:资源:操作` 三段式命名：

| 操作 | 标识 | 说明 |
|------|------|------|
| 查看 | `view` | 列表、详情查看 |
| 新增 | `create` | 创建操作 |
| 编辑 | `update` | 修改操作 |
| 删除 | `delete` | 删除操作 |
| 导出 | `export` | 导出数据 |
| 审核 | `audit` | 审核操作 |
| 重置密码 | `resetPwd` | 管理员重置用户密码 |
| 释放 | `release` | 特殊操作（如释放设备） |

**示例**：
- `organization:center:view` — 查看运营中心列表
- `organization:center:create` — 新增运营中心
- `organization:center:update` — 编辑运营中心
- `organization:center:delete` — 删除运营中心
- `store:list:view` — 查看医院列表
- `device:type:delete` — 删除设备类型

---

## 三、数据库设计

### 3.1 新增表

#### 3.1.1 菜单表 `sys_menu`

```sql
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
```

#### 3.1.2 角色-菜单关联表 `sys_role_menu`

```sql
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
```

> `actions` 字段存储 JSON 数组，如 `["view","create","update","delete"]`，表示该角色在此菜单下可执行的操作。

#### 3.1.3 扩展 `sys_role` 表

在现有 `sys_role` 表基础上新增字段：

```sql
ALTER TABLE sys_role ADD COLUMN data_scope TINYINT NOT NULL DEFAULT 1 
    COMMENT '数据范围:1全部 2本中心及下级 3本代理商及下级 4本医院 5个人' 
    AFTER role_desc;
ALTER TABLE sys_role ADD COLUMN sort_order INT DEFAULT 0 
    COMMENT '排序' AFTER data_scope;
ALTER TABLE sys_role ADD KEY idx_sort_order (sort_order);
```

> `data_scope` 控制数据可见范围，与菜单/操作权限配合实现完整权限控制。

### 3.2 初始化菜单数据

```sql
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, menu_path, menu_icon, permission_key, sort_order) VALUES
-- 顶级目录
(1,  0, '数据看板',   2, '/dashboard',         'HomeFilled',      'dashboard:view',          1),
(2,  0, '组织架构',   1, '/organization',       'OfficeBuilding',  NULL,                       2),
(3,  0, '医院管理',   1, '/store',              'Shop',            NULL,                       3),
(4,  0, '设备管理',   1, '/device',             'Monitor',         NULL,                       4),
(5,  0, '用户管理',   2, '/user',               'UserFilled',       'user:view',                5),
(6,  0, '统计报表',   2, '/statistics',         'DataAnalysis',     'statistics:view',          6),
(7,  0, '操作日志',   2, '/operation-log',      'Document',         'operationlog:view',        7),
(8,  0, '系统设置',   1, '/settings',           'Setting',          NULL,                       8),

-- 组织架构子菜单
(10, 2, '运营中心',   2, '/organization/centers','OfficeBuilding', 'organization:center:view',  1),
(11, 2, '代理商管理', 2, '/organization/agents', 'Connection',      'organization:agent:view',  2),

-- 医院管理子菜单
(20, 3, '医院列表',   2, '/store/list',         'Shop',             'store:list:view',          1),

-- 设备管理子菜单
(30, 4, '设备列表',   2, '/device/list',        'Monitor',          'device:list:view',         1),
(31, 4, '设备类型',   2, '/device/types',       'Cpu',              'device:type:view',         2),

-- 系统设置子菜单
(80, 8, '系统参数',   2, '/settings',           'Setting',          'settings:view',            1),
(81, 8, '角色权限',   2, '/settings/role',      'UserFilled',        'settings:role:view',       2),
(82, 8, '菜单管理',   2, '/settings/menu',      'Menu',              'settings:menu:view',       3);
```

**按钮级权限（menu_type=3）示例**：

```sql
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
(31, '新增设备类型', 3, 'device:type:create',          1),
(31, '编辑设备类型', 3, 'device:type:update',          2),
(31, '删除设备类型', 3, 'device:type:delete',          3),
-- 用户按钮
(5,  '新增用户',     3, 'user:create',                1),
(5,  '编辑用户',     3, 'user:update',                2),
(5,  '删除用户',     3, 'user:delete',                 3),
(5,  '重置密码',     3, 'user:resetPwd',               4),
(5,  '启用/禁用',    3, 'user:toggleStatus',           5);
```

### 3.3 初始化角色与权限

```sql
-- 更新现有角色，增加 data_scope
UPDATE sys_role SET data_scope = 1, sort_order = 1 WHERE role_code = 'super_admin';
UPDATE sys_role SET data_scope = 2, sort_order = 2 WHERE role_code = 'ops_admin';
UPDATE sys_role SET data_scope = 3, sort_order = 3 WHERE role_code = 'store_manager';
UPDATE sys_role SET data_scope = 4, sort_order = 4 WHERE role_code = 'store_doctor';
UPDATE sys_role SET data_scope = 5, sort_order = 5 WHERE role_code = 'parent';

-- 新增角色
INSERT INTO sys_role (role_code, role_name, role_desc, user_type, data_scope, sort_order) VALUES
('hq_admin',       '总部管理员',   '总部运营管理',     1, 1, 1),
('center_admin',   '运营中心管理员','运营中心管理',     4, 2, 2),
('agent_admin',    '代理商管理员', '代理商管理',       5, 3, 3),
('hospital_admin', '医院管理员',   '医院维护管理',     2, 4, 4)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);
```

**角色-菜单权限分配**（`sys_role_menu` 初始化）：

| 角色 | 可见菜单 | 操作权限 |
|------|----------|----------|
| 总部管理员(hq_admin) | 全部菜单 | 全部操作 |
| 运营中心管理员(center_admin) | 数据看板、医院管理、设备管理、用户管理、统计报表、系统设置 | 各菜单 view + 下级 create/update/delete |
| 代理商管理员(agent_admin) | 数据看板、医院管理、设备管理、用户管理 | 各菜单 view + 下级 create/update/delete |
| 医院管理员(hospital_admin) | 数据看板、设备管理、用户管理 | 各菜单 view；设备 view/create/update；用户 view（仅家长） |
| 家长(parent) | 数据看板 | 仅 view |

---

## 四、后端设计

### 4.1 新增实体

```
backend/careld-user-service/src/main/java/com/careld/user/
├── entity/
│   ├── Menu.java              # 菜单实体
│   ├── Role.java              # 角色实体（扩展现有）
│   ├── RoleMenu.java          # 角色-菜单关联实体
│   └── UserRole.java          # 用户-角色关联实体
├── mapper/
│   ├── MenuMapper.java
│   ├── RoleMapper.java        # 扩展现有
│   ├── RoleMenuMapper.java
│   └── UserRoleMapper.java    # 扩展现有
├── service/
│   ├── MenuService.java
│   ├── RoleService.java
│   └── impl/
│       ├── MenuServiceImpl.java
│       └── RoleServiceImpl.java
├── controller/
│   ├── MenuController.java
│   └── RoleController.java
└── dto/
    ├── MenuRequest.java
    ├── RoleRequest.java
    └── RolePermissionRequest.java
```

### 4.2 权限加载流程

```
用户登录
  │
  ▼
AuthServiceImpl.login()
  │
  ├── 验证用户名/密码
  ├── 查询用户角色（sys_user_role → sys_role）
  ├── 查询角色权限（sys_role_menu → sys_menu）
  │   返回: [{ menuId, menuPath, permissionKey, actions: ["view","create",...] }]
  │
  ├── 将权限列表写入 JWT claims（精简为 permission keys 数组）
  │   claims.put("permissions", ["dashboard:view","organization:center:view",...])
  │
  ▼
返回前端
  │
  ▼
前端 userStore.permissions = ["dashboard:view","organization:center:view",...]
```

### 4.3 JWT Claims 扩展

```java
// AuthServiceImpl.generateTokenResponse() 修改
private LoginResponse generateTokenResponse(User user) {
    // ... 现有逻辑 ...

    // 查询用户权限
    List<String> permissions = permissionService.getPermissionKeysByUserId(user.getId());
    
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    claims.put("username", user.getUsername());
    claims.put("userType", user.getUserType());
    claims.put("storeId", user.getStoreId());
    claims.put("deptId", user.getDeptId());
    claims.put("permissions", permissions);  // ← 新增

    // ... 生成 token ...

    userInfo.setPermissions(permissions);  // ← 替换原来的 List.of("*")
    
    return response;
}
```

> **注意**：JWT payload 有大小限制（建议 < 4KB）。如果权限项较多，改为在 `/api/v1/users/me` 接口返回，JWT 只存 userId。

### 4.4 后端权限校验

#### 方案A：自定义注解 + AOP 拦截（推荐）

```java
// 注解定义 (careld-common)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();          // 权限标识，如 "organization:center:create"
    boolean requireAll() default false;  // 多个权限时是否需要全部满足
}

// AOP 切面
@Aspect
@Component
public class PermissionAspect {
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String permissionKey = requirePermission.value();
        List<String> userPermissions = UserContext.getPermissions();
        
        if (userPermissions == null || !userPermissions.contains(permissionKey)) {
            throw new BusinessException(403, "无操作权限: " + permissionKey);
        }
        
        return joinPoint.proceed();
    }
}
```

**使用示例**：

```java
@RestController
@RequestMapping("/api/v1/ops-centers")
public class OpsCenterController {

    @GetMapping
    @RequirePermission("organization:center:view")
    public Result<PageResult<OpsCenter>> list(...) { ... }

    @PostMapping
    @RequirePermission("organization:center:create")
    public Result<Long> create(...) { ... }

    @PutMapping("/{id}")
    @RequirePermission("organization:center:update")
    public Result<Void> update(...) { ... }

    @DeleteMapping("/{id}")
    @RequirePermission("organization:center:delete")
    public Result<Void> delete(...) { ... }
}
```

#### 方案B：Spring Security `@PreAuthorize`

```java
@PreAuthorize("hasAuthority('organization:center:create')")
@PostMapping
public Result<Long> create(...) { ... }
```

> 需要在 `JwtAuthFilter` 中将权限列表设为 `GrantedAuthority`。

### 4.5 权限缓存策略

为避免每次请求都查询数据库，使用 Redis 缓存用户权限：

```
缓存 Key:  careld:perm:user:{userId}
缓存值:    ["dashboard:view","organization:center:view",...]
TTL:       30 分钟（与 access token 过期时间对齐）
失效条件:  用户角色变更、角色权限变更时主动清除
```

```java
@Service
public class PermissionServiceImpl implements PermissionService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public List<String> getPermissionKeysByUserId(Long userId) {
        // 1. 先查缓存
        String cacheKey = "careld:perm:user:" + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JSON.parseArray(cached, String.class);
        }

        // 2. 查数据库
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return List.of();
        
        List<RoleMenu> roleMenus = roleMenuMapper.selectByRoleIds(roleIds);
        Set<String> permissions = new HashSet<>();
        for (RoleMenu rm : roleMenus) {
            // 菜单本身的 permission_key
            if (rm.getMenu().getPermissionKey() != null) {
                permissions.add(rm.getMenu().getPermissionKey());
            }
            // actions 中的操作权限
            for (String action : rm.getActions()) {
                String baseKey = rm.getMenu().getPermissionKey();
                if (baseKey != null) {
                    permissions.add(baseKey.replaceAll(":view$", ":" + action));
                }
            }
        }

        // 3. 写缓存
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(permissions), 
            Duration.ofMinutes(30));

        return new ArrayList<>(permissions);
    }

    /** 角色权限变更时清除缓存 */
    public void clearUserPermissionCache(Long userId) {
        redisTemplate.delete("careld:perm:user:" + userId);
    }
}
```

### 4.6 数据范围控制

结合 `data_scope` 字段，在查询时自动添加数据过滤条件：

```java
@DataScope(field = "centerId")  // 注解声明过滤字段
@GetMapping("/api/v1/stores")
public Result<PageResult<Store>> list(...) { ... }
```

| data_scope | 值 | 说明 | 过滤逻辑 |
|------------|---|------|----------|
| ALL | 1 | 全部数据 | 不加过滤 |
| CENTER_AND_BELOW | 2 | 本中心及下级 | center_id = 用户centerId（含下级代理商的医院） |
| AGENT_AND_BELOW | 3 | 本代理商及下级 | agent_id = 用户agentId |
| STORE_ONLY | 4 | 仅本医院 | store_id = 用户storeId |
| PERSONAL | 5 | 仅个人 | created_by = 用户userId |

---

## 五、前端设计

### 5.1 路由动态生成

```typescript
// stores/permission.ts
export const usePermissionStore = defineStore('permission', () => {
  const menus = ref<MenuItem[]>([])        // 动态菜单树
  const permissions = ref<string[]>([])     // 权限标识列表

  /** 从后端加载菜单和权限 */
  const loadPermissions = async () => {
    const res = await menuApi.getUserMenus()  // GET /api/v1/menus/my
    menus.value = buildMenuTree(res.menus)
    permissions.value = res.permissions
  }

  /** 检查是否有权限 */
  const hasPermission = (key: string) => {
    return permissions.value.includes(key) || permissions.value.includes('*')
  }

  /** 检查是否有某个操作权限 */
  const hasAction = (module: string, resource: string, action: string) => {
    return hasPermission(`${module}:${resource}:${action}`)
  }

  return { menus, permissions, loadPermissions, hasPermission, hasAction }
})
```

### 5.2 路由守卫改造

```typescript
// router/index.ts
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const permStore = usePermissionStore()

  if (to.meta?.public) { next(); return }
  if (!userStore.isLoggedIn) { next('/login'); return }

  // 首次加载时获取权限
  if (permStore.permissions.length === 0) {
    await permStore.loadPermissions()
    
    // 动态添加路由
    const dynamicRoutes = generateRoutes(permStore.menus)
    dynamicRoutes.forEach(route => router.addRoute('Layout', route))
    
    // 重新导航以确保路由已注册
    next({ ...to, replace: true })
    return
  }

  // 检查路由权限
  const permissionKey = to.meta?.permission as string
  if (permissionKey && !permStore.hasPermission(permissionKey)) {
    next('/403')
    return
  }

  next()
})
```

### 5.3 侧边栏菜单改造

```vue
<!-- MainLayout.vue -->
<template>
  <el-menu :default-active="$route.path" router>
    <template v-for="menu in permissionStore.menus" :key="menu.id">
      <!-- 目录（有子菜单） -->
      <el-sub-menu v-if="menu.children?.length" :index="menu.path">
        <template #title>
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.name }}</span>
        </template>
        <el-menu-item 
          v-for="child in menu.children" 
          :key="child.id"
          :index="child.path"
        >
          {{ child.name }}
        </el-menu-item>
      </el-sub-menu>
      
      <!-- 单个菜单 -->
      <el-menu-item v-else :index="menu.path">
        <el-icon><component :is="menu.icon" /></el-icon>
        <template #title>{{ menu.name }}</template>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup>
const permissionStore = usePermissionStore()
// 不再使用 v-if="userType === 1" 硬编码
</script>
```

### 5.4 操作按钮权限控制

#### 方式A：自定义指令 `v-permission`

```typescript
// directives/permission.ts
export const permissionDirective = {
  mounted(el: HTMLElement, binding: { value: string | string[] }) {
    const permStore = usePermissionStore()
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    const hasAny = required.some(p => permStore.hasPermission(p))
    
    if (!hasAny) {
      el.parentNode?.removeChild(el)
    }
  }
}

// main.ts
app.directive('permission', permissionDirective)
```

**使用**：

```vue
<el-button v-permission="'organization:center:create'" type="primary" @click="handleAdd">
  新增运营中心
</el-button>

<el-button v-permission="'organization:center:update'" type="warning" @click="handleEdit(row)">
  编辑
</el-button>

<el-button v-permission="'organization:center:delete'" type="danger" @click="handleDelete(row)">
  删除
</el-button>
```

#### 方式B：组合式函数 `usePermission`

```typescript
// composables/usePermission.ts
export function usePermission() {
  const permStore = usePermissionStore()
  
  return {
    has: (key: string) => permStore.hasPermission(key),
    hasAction: (module: string, resource: string, action: string) => 
      permStore.hasAction(module, resource, action)
  }
}

// 在组件中使用
const { has } = usePermission()
const canEdit = computed(() => has('organization:center:update'))
```

### 5.5 系统设置页新增 Tab

在 `settings/index.vue` 中新增「角色权限」和「菜单管理」两个 Tab：

```
系统设置
├── 系统参数（现有）
├── TV同步配置（现有）
├── 通知配置（现有）
├── 安全设置（现有）
├── 角色管理（新增）    ← 创建/编辑角色、分配菜单和操作权限
└── 菜单管理（新增）    ← 查看/编辑菜单树（一般初始化后不需要频繁修改）
```

#### 角色管理页面

```
┌─────────────────────────────────────────────────────────┐
│ 角色管理                                          [新增角色] │
├──────────────┬──────────────────────────────────────────┤
│ 角色列表      │ 角色权限配置                                │
│              │                                          │
│ ◉ 总部管理员  │ 角色：总部管理员                            │
│ ○ 运营中心    │                                          │
│ ○ 代理商      │ ┌─ 菜单权限树 ──────────────────────────┐ │
│ ○ 医院维护    │ │ ☑ 数据看板       [view]              │ │
│ ○ 家长       │ │ ☑ 组织架构                            │ │
│              │ │   ☑ 运营中心     [view][create][update][delete] │
│              │ │   ☑ 代理商管理   [view][create][update][delete] │
│              │ │ ☑ 医院管理                            │ │
│              │ │   ☑ 医院列表     [view][create][update][delete] │
│              │ │ ☑ 设备管理                            │ │
│              │ │   ...                                │ │
│              │ └──────────────────────────────────────┘ │
│              │                           [保存权限分配]    │
└──────────────┴──────────────────────────────────────────┘
```

---

## 六、API 接口设计

### 6.1 菜单接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/v1/menus` | 菜单树列表 | `settings:menu:view` |
| GET | `/api/v1/menus/my` | 当前用户菜单和权限 | 登录即可 |
| POST | `/api/v1/menus` | 新增菜单 | `settings:menu:create` |
| PUT | `/api/v1/menus/{id}` | 修改菜单 | `settings:menu:update` |
| DELETE | `/api/v1/menus/{id}` | 删除菜单 | `settings:menu:delete` |

**GET `/api/v1/menus/my` 响应**：

```json
{
  "code": 200,
  "data": {
    "menus": [
      {
        "id": 1,
        "parentId": 0,
        "menuName": "数据看板",
        "menuType": 2,
        "menuPath": "/dashboard",
        "menuIcon": "HomeFilled",
        "permissionKey": "dashboard:view",
        "sortOrder": 1,
        "children": []
      },
      {
        "id": 2,
        "parentId": 0,
        "menuName": "组织架构",
        "menuType": 1,
        "menuPath": "/organization",
        "menuIcon": "OfficeBuilding",
        "sortOrder": 2,
        "children": [
          {
            "id": 10,
            "parentId": 2,
            "menuName": "运营中心",
            "menuType": 2,
            "menuPath": "/organization/centers",
            "permissionKey": "organization:center:view",
            "children": []
          }
        ]
      }
    ],
    "permissions": [
      "dashboard:view",
      "organization:center:view",
      "organization:center:create",
      "organization:center:update",
      "organization:center:delete"
    ]
  }
}
```

### 6.2 角色接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/v1/roles` | 角色列表 | `settings:role:view` |
| GET | `/api/v1/roles/{id}` | 角色详情（含权限） | `settings:role:view` |
| POST | `/api/v1/roles` | 新增角色 | `settings:role:create` |
| PUT | `/api/v1/roles/{id}` | 修改角色 | `settings:role:update` |
| DELETE | `/api/v1/roles/{id}` | 删除角色 | `settings:role:delete` |
| PUT | `/api/v1/roles/{id}/permissions` | 分配角色权限 | `settings:role:update` |
| GET | `/api/v1/roles/{id}/users` | 角色下用户列表 | `settings:role:view` |

**PUT `/api/v1/roles/{id}/permissions` 请求体**：

```json
{
  "roleMenus": [
    {
      "menuId": 1,
      "actions": ["view"]
    },
    {
      "menuId": 10,
      "actions": ["view", "create", "update", "delete"]
    },
    {
      "menuId": 11,
      "actions": ["view", "create", "update", "delete"]
    }
  ]
}
```

### 6.3 用户角色接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| PUT | `/api/v1/users/{id}/roles` | 分配用户角色 | `user:update` |
| GET | `/api/v1/users/{id}/roles` | 查看用户角色 | `user:view` |

---

## 七、默认权限矩阵

### 7.1 菜单可见性

| 菜单 | 总部管理员 | 运营中心管理员 | 代理商管理员 | 医院管理员 | 家长 |
|------|:---:|:---:|:---:|:---:|:---:|
| 数据看板 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 组织架构-运营中心 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 组织架构-代理商 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 医院管理-医院列表 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 设备管理-设备列表 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 设备管理-设备类型 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 用户管理 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 统计报表 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 操作日志 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 系统设置-系统参数 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 系统设置-角色权限 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 系统设置-菜单管理 | ✅ | ❌ | ❌ | ❌ | ❌ |

### 7.2 操作权限

| 操作 | 总部管理员 | 运营中心管理员 | 代理商管理员 | 医院管理员 |
|------|:---:|:---:|:---:|:---:|
| 运营中心-增删改 | ✅ | ❌ | ❌ | ❌ |
| 代理商-增删改 | ✅ | ✅ | ❌ | ❌ |
| 医院-增删改 | ✅ | ✅ | ✅ | ❌ |
| 设备-增删改/释放 | ✅ | ✅ | ✅ | ✅(仅本院) |
| 设备类型-增删改 | ✅ | ✅ | ✅ | ❌ |
| 用户-增删改/重置密码 | ✅ | ✅(下级) | ✅(下级) | ✅(仅家长) |
| 角色-增删改 | ✅ | ❌ | ❌ | ❌ |
| 菜单-增删改 | ✅ | ❌ | ❌ | ❌ |

---

## 八、实现计划

### 阶段一：基础设施（数据库 + 实体 + Mapper）

| 序号 | 任务 | 涉及文件 |
|------|------|----------|
| 1 | 创建迁移脚本 `06-rbac-permission.sql` | `database/mysql/migration/` |
| 2 | 创建 `Menu`、`RoleMenu` 实体 | `careld-user-service/entity/` |
| 3 | 扩展 `Role` 实体（增加 dataScope） | `careld-user-service/entity/` |
| 4 | 创建 `MenuMapper`、`RoleMenuMapper` | `careld-user-service/mapper/` |
| 5 | 在 `careld-common` 中新增 `@RequirePermission` 注解 + AOP 切面 | `careld-common/security/` |
| 6 | 在 `UserContext` 中增加 `permissions` 字段 | `careld-common/security/` |
| 7 | 在 `JwtAuthFilter` 中解析 `permissions` claim | `careld-common/security/` |

### 阶段二：后端业务层

| 序号 | 任务 | 涉及文件 |
|------|------|----------|
| 8 | 创建 `PermissionService` + 实现类 | `careld-user-service/service/` |
| 9 | 创建 `MenuService` + 实现类 | `careld-user-service/service/` |
| 10 | 扩展 `RoleService` + 实现类 | `careld-user-service/service/` |
| 11 | 创建 `MenuController`、`RoleController` | `careld-user-service/controller/` |
| 12 | 修改 `AuthServiceImpl` 登录时加载权限 | `careld-auth-service/` |
| 13 | 在各 Controller 方法上添加 `@RequirePermission` 注解 | 全部微服务 Controller |
| 14 | 权限缓存（Redis） | `careld-user-service/` |

### 阶段三：前端改造

| 序号 | 任务 | 涉及文件 |
|------|------|----------|
| 15 | 创建 `permission` store | `admin-web/stores/permission.ts` |
| 16 | 创建 `v-permission` 指令 | `admin-web/directives/permission.ts` |
| 17 | 改造路由守卫，支持动态路由 | `admin-web/router/index.ts` |
| 18 | 改造 `MainLayout` 侧边栏为动态菜单 | `admin-web/layouts/MainLayout.vue` |
| 19 | 各页面操作按钮添加 `v-permission` | 各 `views/` 页面 |
| 20 | 新增角色管理页面 | `admin-web/views/settings/role.vue` |
| 21 | 新增菜单管理页面 | `admin-web/views/settings/menu.vue` |
| 22 | 新增对应前端 API | `admin-web/api/menu.ts`、`role.ts` |
| 23 | 用户编辑表单增加角色选择 | `admin-web/views/user/index.vue` |

### 阶段四：测试与验证

| 序号 | 任务 |
|------|------|
| 24 | 数据库迁移脚本执行验证 |
| 25 | API 接口测试（菜单/角色/权限 CRUD） |
| 26 | 前端菜单动态渲染测试 |
| 27 | 按钮权限控制测试 |
| 28 | 后端接口权限拦截测试（403 场景） |
| 29 | 不同角色登录端到端测试 |

---

## 九、兼容性与降级策略

### 9.1 向后兼容

- **userType 保留**：`sys_user.user_type` 字段保留，用于层级控制（谁能创建谁、数据范围兜底）
- **无角色用户降级**：如果用户没有分配角色，按 `userType` 自动匹配默认角色的权限
- **缓存失效降级**：Redis 不可用时，直接查数据库

### 9.2 迁移策略

```
迁移步骤：
1. 执行 06-rbac-permission.sql（建表 + 初始化数据）
2. 为所有现有用户分配默认角色（根据 userType 匹配）
3. 部署新版后端（权限加载 + API）
4. 部署新版前端（动态菜单 + 按钮控制）
5. 验证各角色登录后功能正常
6. 逐步在各 Controller 上添加 @RequirePermission 注解
```

### 9.3 权限缺失处理

| 场景 | 处理方式 |
|------|----------|
| 用户无任何角色 | 按 userType 分配默认角色权限 |
| 角色无任何权限分配 | 只能看到数据看板 |
| JWT 中无 permissions claim | 调用 `/api/v1/menus/my` 实时获取 |
| 后端权限缓存失效 | 回源数据库查询 |
| 菜单不存在对应路由 | 显示 404 页面 |

---

## 十、安全注意事项

1. **权限校验必须在后端执行**：前端隐藏按钮仅为 UX 优化，后端 `@RequirePermission` 是安全防线
2. **超级管理员不可删除**：`super_admin` / `hq_admin` 角色不可被删除，其权限不可被收回
3. **角色分配权限限制**：只有拥有 `settings:role:update` 权限的用户才能分配角色权限
4. **操作日志**：角色权限变更必须记录操作日志
5. **JWT 安全**：permissions claim 仅存权限 key 列表，不存菜单结构（菜单通过 API 获取）

---

## 附录：现有文件影响范围

### 后端需修改的文件

| 文件 | 修改内容 |
|------|----------|
| `careld-common/security/UserContext.java` | 新增 `permissions` 字段和 `getPermissions()` 方法 |
| `careld-common/security/JwtAuthFilter.java` | 解析 JWT 中的 `permissions` claim |
| `careld-auth-service/AuthServiceImpl.java` | 登录时查询用户权限写入 JWT |
| `careld-user-service/config/SecurityConfig.java` | （如用方案B）配置方法级权限 |
| 各微服务 Controller | 添加 `@RequirePermission` 注解 |

### 前端需修改的文件

| 文件 | 修改内容 |
|------|----------|
| `stores/user.ts` | permissions 从后端动态获取（已有字段，改为实际值） |
| `stores/permission.ts` | 新建：菜单树 + 权限列表 store |
| `router/index.ts` | 改为动态路由注册 + 权限校验 |
| `layouts/MainLayout.vue` | 侧边栏改为动态菜单渲染 |
| `directives/permission.ts` | 新建：v-permission 指令 |
| `main.ts` | 注册 v-permission 指令 |
| `api/menu.ts` | 新建：菜单 API |
| `api/role.ts` | 新建：角色 API |
| `views/settings/role.vue` | 新建：角色管理页面 |
| `views/settings/menu.vue` | 新建：菜单管理页面 |
| `views/settings/index.vue` | 增加 Tab 切换到角色/菜单管理 |
| `views/user/index.vue` | 编辑表单增加角色选择器 |
| 各 `views/*/index.vue` | 操作按钮添加 `v-permission` |

### 新建数据库表

| 表名 | 说明 |
|------|------|
| `sys_menu` | 菜单表（目录/菜单/按钮三级） |
| `sys_role_menu` | 角色-菜单关联表（含操作权限） |

### 修改数据库表

| 表名 | 修改内容 |
|------|----------|
| `sys_role` | 新增 `data_scope`、`sort_order` 字段 |
