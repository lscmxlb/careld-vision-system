# Careld 门店管理系统（store-web）改造规格说明

> 日期：2026-08-10  
> 范围：store-web 门店管理系统 + 后端对应微服务（store-service / user-service / auth-service）  
> 基于 admin-web 组织架构重构（0808 spec）完成后的新一轮改造

---

## 一、需求总览

| 模块 | 变更类型 | 说明 |
|------|----------|------|
| 医护管理 | **新增模块** | 门店医护人员的增删改查，手机号 + 后六位密码登录机制 |
| 产品管理 | **新增模块** | 护理套餐定义（次数型），购买记录与剩余次数管理 |
| 设备管理 | **改为只读** | 门店只能查看运营中心分配的设备，不能新增/删除/编辑 |
| 科室管理 | **隐藏菜单** | 前端菜单不展示（保留代码） |
| 医生端登录 | **机制设计** | 医护人员用注册手机号登录，密码为手机号后六位 |

---

## 二、医护管理（新增模块）

### 2.1 功能说明
门店管理员可在门店管理系统中添加、编辑、删除本门店的医护人员信息。医护人员注册后，后续开发医生端时可通过手机号 + 手机号后六位作为密码进行登录。

### 2.2 数据表 `store_staff`

> 归属微服务：`careld-store-service`  
> 该表为门店医护人员的**主数据源**，所有 CRUD 操作通过此表进行。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键 |
| store_id | BIGINT UNSIGNED | NOT NULL, FK→store_info.id | 所属门店 |
| name | VARCHAR(64) | NOT NULL | 姓名 |
| gender | TINYINT | NOT NULL DEFAULT 0 | 性别：0未知 1男 2女 |
| position | VARCHAR(50) | | 职务（如：医师、护师、验光师、店长） |
| phone | VARCHAR(20) | NOT NULL | 手机号（唯一，作为登录账号） |
| login_password | VARCHAR(128) | NOT NULL | 登录密码（BCrypt加密，初始值为手机号后6位） |
| status | TINYINT | NOT NULL DEFAULT 1 | 状态：0禁用 1启用 |
| created_by | BIGINT UNSIGNED | | 创建人 |
| updated_by | BIGINT UNSIGNED | | 更新人 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted_at | DATETIME | | 逻辑删除 |

**索引**：
- `UNIQUE KEY uk_store_phone (store_id, phone)` — 同一门店内手机号唯一
- `KEY idx_store_id (store_id)`
- `KEY idx_status (status)`

### 2.3 登录机制

#### 2.3.1 密码规则
- **初始密码**：手机号后六位（如手机号 `13812345678`，密码为 `345678`）
- **加密方式**：BCrypt（与现有 sys_user 一致）
- **创建员工时**：后端自动取 phone 字段的后6位，BCrypt 加密后存入 `login_password`

#### 2.3.2 认证流程（医生端登录时）
```
医生端登录请求 → auth-service
  ↓
查询 store_staff 表（phone 匹配 + store 匹配 + status=1）
  ↓
BCrypt 验证 login_password
  ↓
签发 JWT Token（token 中携带 staff_id, store_id, name, position）
```

#### 2.3.3 与 sys_user 的关系
| 维度 | sys_user | store_staff |
|------|----------|-------------|
| 定位 | 系统全局用户表 | 门店医护专属表 |
| 管理方 | admin-web（总部/运营中心管理） | store-web（门店自行管理） |
| 登录方式 | 用户名 + 密码 | 手机号 + 手机号后六位 |
| 数据关系 | 创建 store_staff 时**同步创建** sys_user 记录（user_type=2） | 主数据源 |

**同步规则**：
- 新增 store_staff 时，自动在 sys_user 创建对应记录：
  - `username` = phone（手机号）
  - `password` = BCrypt(手机号后6位)
  - `real_name` = store_staff.name
  - `phone` = store_staff.phone
  - `user_type` = 2（门店医护）
  - `store_id` = 当前门店ID
- 编辑 store_staff 时，同步更新 sys_user 的 real_name、phone
- 禁用 store_staff（status=0）时，同步禁用 sys_user（status=0）

### 2.4 前端页面

#### 2.4.1 医护管理页 `/staff`
- **搜索栏**：关键词搜索（姓名/手机号）、职务筛选、状态筛选
- **表格列**：

| 列名 | 字段 | 说明 |
|------|------|------|
| 姓名 | name | |
| 性别 | gender | 男/女/未知 |
| 职务 | position | |
| 手机号 | phone | |
| 状态 | status | 启用/禁用 开关 |
| 操作 | - | 编辑、删除 |

- **新增/编辑弹窗表单**：

| 字段 | 组件 | 校验 |
|------|------|------|
| 姓名 | input | 必填 |
| 性别 | radio（男/女/未知） | 必填 |
| 职务 | select（医师/护师/验光师/店长/其他） | 必填 |
| 手机号 | input | 必填，11位手机号格式校验，门店内唯一 |
| 状态 | switch | 默认启用 |

- **新增提示**：创建成功后提示"初始登录密码为手机号后六位：XXXXXX"
- **权限控制**：仅店长（userType=2）可新增/编辑/删除，普通店员只能查看

---

## 三、产品管理（新增模块）

### 3.1 功能说明
门店端管理护理套餐产品（次数型套餐），以及记录儿童的套餐购买信息。

**核心概念**：
- **套餐**：定义护理项目名称和包含的服务次数（如"儿童近视护理10次"）
- **购买记录**：记录某个儿童购买了某个套餐，跟踪总次数和剩余次数
- **次数 = 预约权**：剩余次数决定该儿童还能预约多少次服务
- **次数用完**：可再次购买，产生新的购买记录
- **门店端职责**：套餐定义 + 购买记录录入
- **医生端职责**（后续开发）：决定儿童需要购买哪个套餐、多少次

### 3.2 数据表

#### 3.2.1 服务套餐表 `service_package`

> 归属微服务：`careld-store-service`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键 |
| store_id | BIGINT UNSIGNED | NOT NULL, FK→store_info.id | 所属门店 |
| package_code | VARCHAR(50) | NOT NULL | 套餐编码（门店内唯一） |
| package_name | VARCHAR(100) | NOT NULL | 套餐名称（如"儿童近视护理10次"） |
| total_sessions | INT | NOT NULL | 包含服务次数 |
| unit_price | DECIMAL(10,2) | | 单次价格 |
| package_price | DECIMAL(10,2) | | 套餐总价 |
| description | VARCHAR(500) | | 套餐说明 |
| status | TINYINT | NOT NULL DEFAULT 1 | 状态：0停售 1在售 |
| created_by | BIGINT UNSIGNED | | 创建人 |
| updated_by | BIGINT UNSIGNED | | 更新人 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted_at | DATETIME | | 逻辑删除 |

**索引**：
- `UNIQUE KEY uk_store_code (store_id, package_code)` — 门店内套餐编码唯一
- `KEY idx_store_id (store_id)`
- `KEY idx_status (status)`

#### 3.2.2 套餐购买记录表 `package_purchase`

> 归属微服务：`careld-store-service`

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键 |
| store_id | BIGINT UNSIGNED | NOT NULL | 所属门店 |
| package_id | BIGINT UNSIGNED | NOT NULL, FK→service_package.id | 购买的套餐ID |
| child_id | BIGINT UNSIGNED | NOT NULL, FK→child_profile.id | 购买的儿童 |
| total_sessions | INT | NOT NULL | 购买时的总次数（快照） |
| used_sessions | INT | NOT NULL DEFAULT 0 | 已使用次数 |
| remaining_sessions | INT | NOT NULL | 剩余次数（= total - used） |
| purchase_price | DECIMAL(10,2) | | 实际购买价格 |
| purchase_date | DATE | NOT NULL | 购买日期 |
| expire_date | DATE | | 有效期截止日期（可选） |
| status | TINYINT | NOT NULL DEFAULT 1 | 状态：1使用中 2已用完 3已过期 4已退款 |
| remark | VARCHAR(500) | | 备注 |
| created_by | BIGINT UNSIGNED | | 创建人（录入人） |
| updated_by | BIGINT UNSIGNED | | 更新人 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| deleted_at | DATETIME | | 逻辑删除 |

**索引**：
- `KEY idx_store_id (store_id)`
- `KEY idx_child_id (child_id)`
- `KEY idx_package_id (package_id)`
- `KEY idx_status (status)`

### 3.3 业务规则

#### 3.3.1 次数扣减逻辑
```
儿童预约服务 → 检查该儿童是否有 status=1 的购买记录
  ↓
检查 remaining_sessions > 0
  ↓ 是
允许预约，预约完成/服务完成后：
  used_sessions += 1
  remaining_sessions -= 1
  ↓
若 remaining_sessions = 0 → 自动将 status 改为 2（已用完）
```

#### 3.3.2 多次购买
- 同一儿童可以有多条购买记录（次数用完后可再次购买）
- 预约时优先扣减**最早创建**的、仍有剩余次数的记录（先进先出）
- 若所有记录均已用完，则不允许预约（提示需重新购买）

#### 3.3.3 门店端 vs 医生端职责划分
| 操作 | 门店端（store-web） | 医生端（后续开发） |
|------|---------------------|-------------------|
| 定义套餐 | ✅ 创建/编辑/上下架 | ❌ 只读查看 |
| 录入购买记录 | ✅ 选择儿童 + 套餐 + 购买日期 | ❌ |
| 决定儿童需要哪个套餐 | ❌ | ✅ 医生评估后决定 |
| 查看剩余次数 | ✅ | ✅ |
| 预约时扣减次数 | 系统自动 | 系统自动 |

### 3.4 前端页面

#### 3.4.1 套餐管理页 `/product/packages`

- **搜索栏**：关键词（套餐名称/编码）、状态筛选
- **表格列**：

| 列名 | 字段 | 说明 |
|------|------|------|
| 套餐编码 | package_code | |
| 套餐名称 | package_name | |
| 服务次数 | total_sessions | |
| 单次价格 | unit_price | ¥格式 |
| 套餐价格 | package_price | ¥格式 |
| 状态 | status | 在售/停售 开关 |
| 操作 | - | 编辑、删除 |

- **新增/编辑弹窗表单**：

| 字段 | 组件 | 校验 |
|------|------|------|
| 套餐编码 | input | 必填，门店内唯一 |
| 套餐名称 | input | 必填 |
| 服务次数 | input-number | 必填，>0 |
| 单次价格 | input-number（精度2位） | 选填 |
| 套餐价格 | input-number（精度2位） | 选填 |
| 套餐说明 | textarea | 选填 |
| 状态 | switch | 默认在售 |

#### 3.4.2 购买记录页 `/product/purchases`

- **搜索栏**：儿童姓名搜索、套餐筛选、状态筛选
- **表格列**：

| 列名 | 字段 | 说明 |
|------|------|------|
| 儿童姓名 | child_name | 关联 child_profile |
| 套餐名称 | package_name | 关联 service_package |
| 总次数 | total_sessions | |
| 已使用 | used_sessions | |
| 剩余次数 | remaining_sessions | 颜色标识：>3绿色，1-3黄色，0红色 |
| 购买日期 | purchase_date | |
| 有效期至 | expire_date | 已过期标红 |
| 状态 | status | 使用中/已用完/已过期/已退款 |
| 操作 | - | 退款（改状态）、查看详情 |

- **新增购买记录弹窗表单**：

| 字段 | 组件 | 校验 |
|------|------|------|
| 选择儿童 | select（搜索 child_profile） | 必填 |
| 选择套餐 | select（筛选在售套餐） | 必填 |
| 服务次数 | 自动填充（读取套餐的 total_sessions） | 只读 |
| 购买价格 | input-number | 自动填充套餐价格，可修改 |
| 购买日期 | date-picker | 默认今天 |
| 有效期至 | date-picker | 选填 |
| 备注 | textarea | 选填 |

- **选择套餐后**：自动填充服务次数和价格，门店人员可修改价格但次数由套餐决定

---

## 四、设备管理（改为只读）

### 4.1 变更说明
门店端的设备管理从"可增删改"改为"只读查看"。设备由运营中心（admin-web）统一录入和分配，门店只能查看本门店下的设备信息。

### 4.2 前端调整
- **移除**：新增设备按钮、编辑按钮、删除按钮
- **保留**：设备列表展示（设备名称、设备类型、设备SN、状态、到期日期、到期状态）
- **新增**：到期状态的颜色标识（绿/黄/红，与 admin-web 一致）
- **新增**：设备详情弹窗（只读，展示完整设备信息）

### 4.3 后端调整
- 保留现有 GET /api/v1/devices 接口
- 移除或限制 POST/PUT/DELETE 端点在门店端的访问（后端不做硬限制，前端不展示操作按钮即可）

---

## 五、科室管理（隐藏）

### 5.1 变更说明
- 前端菜单中**移除**科室管理入口
- 前端路由中**移除**科室管理页面路由
- 后端 API 保留不动
- 代码文件保留（不删除 views/department 和 api/department）

---

## 六、菜单结构调整

### 6.1 调整前
```
store-web 菜单
├── 数据看板
├── 儿童档案
├── 排班预约
├── 视力记录
├── 设备管理        ← 店长可见，可增删改
└── 科室管理        ← 店长可见
```

### 6.2 调整后
```
store-web 菜单
├── 数据看板
├── 儿童档案
├── 医护管理        ← 【新增】
├── 产品管理        ← 【新增】
│   ├── 套餐管理    ← 定义护理套餐
│   └── 购买记录    ← 录入儿童购买信息
├── 排班预约
├── 视力记录
├── 设备管理        ← 【改为只读】所有角色可见，仅查看
└── 科室管理        ← 【隐藏】
```

### 6.3 权限调整
| 菜单 | 原权限 | 新权限 |
|------|--------|--------|
| 医护管理 | - | 所有角色可见列表；新增/编辑/删除仅店长 |
| 产品管理 | - | 所有角色可见；新增/编辑仅店长 |
| 设备管理 | 仅店长 | 所有角色可见（只读） |
| 科室管理 | 仅店长 | 隐藏 |

---

## 七、数据库迁移脚本

### 7.1 新建表
```sql
-- 文件：database/mysql/migration/04-store-web-refactor.sql

-- 1. 门店员工表
CREATE TABLE IF NOT EXISTS store_staff (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属门店ID',
    name VARCHAR(64) NOT NULL COMMENT '姓名',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别:0未知 1男 2女',
    position VARCHAR(50) DEFAULT NULL COMMENT '职务',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    login_password VARCHAR(128) NOT NULL COMMENT '登录密码(BCrypt)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_phone (store_id, phone),
    KEY idx_store_id (store_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店员工表';

-- 2. 服务套餐表
CREATE TABLE IF NOT EXISTS service_package (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属门店ID',
    package_code VARCHAR(50) NOT NULL COMMENT '套餐编码',
    package_name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    total_sessions INT NOT NULL COMMENT '包含服务次数',
    unit_price DECIMAL(10,2) DEFAULT NULL COMMENT '单次价格',
    package_price DECIMAL(10,2) DEFAULT NULL COMMENT '套餐总价',
    description VARCHAR(500) DEFAULT NULL COMMENT '套餐说明',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0停售 1在售',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_code (store_id, package_code),
    KEY idx_store_id (store_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务套餐表';

-- 3. 套餐购买记录表
CREATE TABLE IF NOT EXISTS package_purchase (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购买ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属门店ID',
    package_id BIGINT UNSIGNED NOT NULL COMMENT '套餐ID',
    child_id BIGINT UNSIGNED NOT NULL COMMENT '儿童ID',
    total_sessions INT NOT NULL COMMENT '总次数(快照)',
    used_sessions INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    remaining_sessions INT NOT NULL COMMENT '剩余次数',
    purchase_price DECIMAL(10,2) DEFAULT NULL COMMENT '实际购买价格',
    purchase_date DATE NOT NULL COMMENT '购买日期',
    expire_date DATE DEFAULT NULL COMMENT '有效期截止',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1使用中 2已用完 3已过期 4已退款',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_store_id (store_id),
    KEY idx_child_id (child_id),
    KEY idx_package_id (package_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐购买记录表';
```

### 7.2 预置套餐数据（示例）
```sql
INSERT INTO service_package (store_id, package_code, package_name, total_sessions, unit_price, package_price, description)
VALUES
    (1, 'PKG_MYOPIC_10', '儿童近视护理10次', 10, 150.00, 1200.00, '针对儿童近视的养护套餐，含10次护理'),
    (1, 'PKG_MYOPIC_20', '儿童近视护理20次', 20, 130.00, 2200.00, '针对儿童近视的养护套餐，含20次护理，更优惠'),
    (1, 'PKG_WEAK_15', '弱视训练15次', 15, 200.00, 2500.00, '针对弱视儿童的训练套餐，含15次训练');
```

---

## 八、后端接口清单

### 8.1 医护管理（careld-store-service :8283）

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/store-staff | GET | 员工列表（支持 keyword、position、status 筛选，分页） |
| /api/v1/store-staff | POST | 新增员工（自动 BCrypt 手机号后6位，同步创建 sys_user） |
| /api/v1/store-staff/{id} | GET | 员工详情 |
| /api/v1/store-staff/{id} | PUT | 编辑员工（同步更新 sys_user） |
| /api/v1/store-staff/{id} | DELETE | 删除员工（逻辑删除，同步禁用 sys_user） |
| /api/v1/store-staff/{id}/toggle-status | PATCH | 启用/禁用（同步 sys_user） |
| /api/v1/store-staff/all | GET | 全部员工（下拉选择用，不分页） |

### 8.2 产品管理（careld-store-service :8283）

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/service-packages | GET | 套餐列表（支持 keyword、status 筛选，分页） |
| /api/v1/service-packages | POST | 新增套餐 |
| /api/v1/service-packages/{id} | GET | 套餐详情 |
| /api/v1/service-packages/{id} | PUT | 编辑套餐 |
| /api/v1/service-packages/{id} | DELETE | 删除套餐（逻辑删除） |
| /api/v1/service-packages/{id}/toggle-status | PATCH | 上架/下架 |
| /api/v1/service-packages/active | GET | 在售套餐列表（购买时选择用） |
| /api/v1/package-purchases | GET | 购买记录列表（支持 childId、packageId、status 筛选，分页） |
| /api/v1/package-purchases | POST | 新增购买记录（自动计算 remaining_sessions） |
| /api/v1/package-purchases/{id} | GET | 购买记录详情 |
| /api/v1/package-purchases/{id}/refund | PATCH | 退款（改状态为4） |
| /api/v1/package-purchases/{id}/consume | POST | 消费一次（扣减 remaining_sessions，内部调用） |
| /api/v1/package-purchases/child/{childId}/available | GET | 查询某儿童可用的购买记录（剩余次数>0） |

### 8.3 认证扩展（careld-auth-service :8281）

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/auth/staff-login | POST | 医护人员登录（phone + password） |

**请求体**：
```json
{
  "phone": "13812345678",
  "password": "345678"
}
```

**响应**：与现有登录接口一致，返回 accessToken、refreshToken、user 信息

---

## 九、前端文件变更清单

### 9.1 新增文件

| 文件路径 | 说明 |
|----------|------|
| src/api/staff.ts | 医护管理 API |
| src/api/product.ts | 产品管理 API（套餐 + 购买记录） |
| src/views/staff/index.vue | 医护管理页面 |
| src/views/product/packages.vue | 套餐管理页面 |
| src/views/product/purchases.vue | 购买记录页面 |

### 9.2 修改文件

| 文件路径 | 变更内容 |
|----------|----------|
| src/router/index.ts | 新增 staff、product/packages、product/purchases 路由 |
| src/layouts/MainLayout.vue | 新增医护管理、产品管理菜单；设备管理移除 v-if；科室管理移除 |
| src/views/device/index.vue | 移除新增/编辑/删除按钮，改为只读列表 |
| src/types/index.ts | 新增 StoreStaff、ServicePackage、PackagePurchase 类型 |
| vite.config.ts | 新增 /api/v1/store-staff、/api/v1/service-packages、/api/v1/package-purchases 代理到 8283 |
| src/api/index.ts | 新增导出 staff、product |

### 9.3 保留不动的文件

| 文件路径 | 说明 |
|----------|------|
| src/views/department/index.vue | 代码保留，路由和菜单移除 |
| src/api/department.ts | 代码保留 |

---

## 十、类型定义（新增）

```typescript
// ==================== 医护管理 ====================
export interface StoreStaff {
  id: number
  storeId: number
  name: string
  gender: number       // 0未知 1男 2女
  position: string
  phone: string
  status: number       // 0禁用 1启用
  createdAt: string
}

export interface StoreStaffQuery {
  page?: number
  size?: number
  keyword?: string
  position?: string
  status?: number
}

// ==================== 产品管理 ====================
export interface ServicePackage {
  id: number
  storeId: number
  packageCode: string
  packageName: string
  totalSessions: number
  unitPrice: number | null
  packagePrice: number | null
  description: string | null
  status: number       // 0停售 1在售
  createdAt: string
}

export interface ServicePackageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

export interface PackagePurchase {
  id: number
  storeId: number
  packageId: number
  packageName: string   // 关联查询
  childId: number
  childName: string     // 关联查询
  totalSessions: number
  usedSessions: number
  remainingSessions: number
  purchasePrice: number | null
  purchaseDate: string
  expireDate: string | null
  status: number        // 1使用中 2已用完 3已过期 4已退款
  remark: string | null
  createdAt: string
}

export interface PackagePurchaseQuery {
  page?: number
  size?: number
  childId?: number
  packageId?: number
  status?: number
  keyword?: string
}
```

---

## 十一、实施优先级

| 优先级 | 任务 | 依赖 | 说明 |
|--------|------|------|------|
| P0 | 数据库迁移脚本（3张新表） | 无 | 基础 |
| P0 | 后端：StoreStaff 实体 + CRUD | P0-DB | 医护管理核心 |
| P0 | 后端：StoreStaff 创建时同步 sys_user | P0-后端 | 登录前提 |
| P1 | 后端：ServicePackage 实体 + CRUD | P0-DB | 产品管理核心 |
| P1 | 后端：PackagePurchase 实体 + CRUD | P1-套餐 | 购买记录 |
| P1 | 后端：次数扣减逻辑（consume 接口） | P1-购买 | 预约联动 |
| P2 | 前端：类型定义 + API 层 | P0/P1-后端 | 前端基础 |
| P2 | 前端：医护管理页面 | P2-类型 | |
| P2 | 前端：套餐管理页面 | P2-类型 | |
| P2 | 前端：购买记录页面 | P2-类型 | |
| P2 | 前端：路由 + 菜单调整 | P2-页面 | |
| P3 | 前端：设备管理改为只读 | 无 | 独立可做 |
| P3 | 前端：科室管理隐藏 | 无 | 独立可做 |
| P4 | 后端：auth-service 医护人员登录接口 | P0-同步sys_user | 医生端开发时使用 |
