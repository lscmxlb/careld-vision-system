# 医院管理模块数据库设计补充

> 本文档医院组织架构（科室）和医护人员分层管理的数据库设计部分。

---

## 一、现有设计不足分析

当前数据库设计中：

1. **缺少科室管理**：`store_info`（门店表）只有门店基本信息，没有医院的**科室组织架构**
2. **医护人员未分层**：`sys_user` 表的 `user_type` 只有"门店医护"一个大类，没有区分**医生**和**护士**
3. **排班人员未关联用户**：`schedule_info` 表的 `technician_id` 没有明确关联 `sys_user`，护士排班关系不清晰
4. **缺少医护与科室的绑定关系**：护士应该属于某个科室，排班也应该按科室进行

---

## 二、需要新增的表

### 2.1 store_department - 门店科室表

```sql
CREATE TABLE store_department (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '科室ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属门店ID(医院)',
    dept_code VARCHAR(32) NOT NULL COMMENT '科室编码',
    dept_name VARCHAR(64) NOT NULL COMMENT '科室名称',
    dept_type TINYINT DEFAULT 1 COMMENT '科室类型:1门诊 2养护 3检测 4其他',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    updated_by BIGINT UNSIGNED COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (store_id, dept_code),
    KEY idx_store_id (store_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店科室表';
```

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 科室ID（主键） |
| store_id | BIGINT UNSIGNED | 所属门店ID（医院），外键关联 store_info.id |
| dept_code | VARCHAR(32) | 科室编码 |
| dept_name | VARCHAR(64) | 科室名称（如：养护科、检测科） |
| dept_type | TINYINT | 科室类型：1门诊 / 2养护 / 3检测 / 4其他 |
| sort_order | INT | 排序 |
| status | TINYINT | 状态：0禁用 / 1启用 |
| created_by | BIGINT UNSIGNED | 创建人 |
| updated_by | BIGINT UNSIGNED | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

### 2.2 需要修改的表

#### sys_user - 用户表（增加科室关联）

**新增字段**：

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| dept_id | BIGINT UNSIGNED | 所属科室ID（外键关联 store_department.id） |
| job_title | VARCHAR(32) | 职称：如 主任医师、护士长、技师等 |
| license_no | VARCHAR(64) | 执业证书编号 |

**修改 user_type 枚举定义**：

| 值 | 原定义 | 新定义 |
|----|--------|--------|
| 1 | 总部运营 | 总部运营 |
| 2 | 门店医护 | → 拆分为：2门店店长 / 3门店医生 / 4门店护士 / 5门店技师 |
| 3 | 家长 | 家长 |

#### sys_role - 角色表（增加护士角色）

在原有基础上新增角色：

| role_code | role_name | user_type | 说明 |
|-----------|-----------|-----------|------|
| store_nurse | 门店护士 | 4 | 门店护士角色 |
| store_doctor | 门店医生 | 3 | 门店医生角色 |
| store_technician | 门店技师 | 5 | 门店技师角色 |

#### schedule_info - 养护排班表（明确关联护士）

**修改 technician_id 关联说明**：

| 字段名 | 原定义 | 新定义 |
|--------|--------|--------|
| technician_id | BIGINT UNSIGNED COMMENT '技师ID' | BIGINT UNSIGNED COMMENT '技师/护士ID，外键关联 sys_user.id' |

---

## 三、表关系设计

### 3.1 医院组织架构关系

```
store_info (门店/医院)
    │
    ├── store_department (科室)
    │       │
    │       ├── sys_user (医生/护士，属于某个科室)
    │       │
    │       └── schedule_info (排班，指定了 technician_id)
    │
    └── store_tv_device (TV设备)
```

### 3.2 完整的表关系

```
┌─────────────────────────────────────────────────────────────────┐
│                        医院管理层                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   store_info (门店/医院)                                         │
│   │                                                              │
│   │ store_id                                                    │
│   │                                                             │
│   ├──► store_department (科室)                                   │
│   │       │                                                      │
│   │       │ dept_id                                             │
│   │       │                                                     │
│   │       └──► sys_user (医生/护士)                              │
│   │               │                                              │
│   │               │ user_id                                      │
│   │               │                                              │
│   │               └──► sys_user_role (用户角色关联)              │
│   │                       │                                      │
│   │                       │ role_id                              │
│   │                       │                                      │
│   │                       └──► sys_role (角色：医生/护士)        │
│   │                                                              │
│   │                                                              │
│   └──► child_profile (儿童档案，属于某家医院)                     │
│           │                                                      │
│   ┌───────┘                                                      │
│   │                                                              │
│   ▼                                                              │
│   schedule_info (排班，指定了 store_id 和 technician_id)        │
│   │                                                              │
│   ├──► reserve_order (预约订单，关联 child_id 和 schedule_id)    │
│   │                                                              │
│   └──► vision_test_record (视力检测记录)                         │
│                                                                │
│   store_tv_device (TV设备，绑定到门店)                           │
│                                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、业务流程与数据关系

### 4.1 医院账号开通流程

```
┌─────────────────────────────────────────────────────────┐
│                    总部运营中心                           │
│                                                         │
│  1. 创建医院账号（store_info）                           │
│     ├─ 分配门店编码、名称、地址                           │
│     └─ 设置门店状态                                     │
│                                                         │
│  2. 创建医院管理员（门店店长）                           │
│     ├─ 在 sys_user 中创建记录                            │
│     ├─ user_type = 2（门店店长）                         │
│     ├─ store_id = 刚创建的门店ID                         │
│     └─ 绑定角色：store_manager                           │
│                                                         │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      门店Web端                          │
│                                                         │
│  3. 店长登录后创建科室（store_department）               │
│     ├─ 养护科、检测科、门诊等                            │
│     └─ 每个科室关联当前门店                            │
│                                                         │
│  4. 录入医生和护士（sys_user）                           │
│     ├─ 医生：user_type = 3，绑定角色：store_doctor       │
│     ├─ 护士：user_type = 4，绑定角色：store_nurse        │
│     ├─ 技师：user_type = 5，绑定角色：store_technician  │
│     ├─ 每个医护指定 dept_id（所属科室）                 │
│     └─ 护士用手机号作为 username 登录                  │
│                                                         │
│  5. 排班（schedule_info）                               │
│     ├─ 指定 store_id（门店）                           │
│     ├─ 指定 technician_id（护士/医生ID）               │
│     └─ 护士登录后只能看到自己所属科室的排班              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 4.2 护士登录操作流程

```
┌─────────────────────────────────────────────────────────┐
│                    门店护士登录                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. 护士输入手机号和密码                                 │
│     └─ 后端验证 sys_user 表中的 phone 和 password      │
│                                                         │
│  2. 验证通过后，返回 Token                               │
│     └─ Token 中包含 user_type = 4（护士）              │
│                                                         │
│  3. 护士登录门店Web后                                    │
│     ├─ 只能看到自己所属科室（dept_id）的数据           │
│     ├─ 可以看到自己被排班的时段                        │
│     ├─ 可以进行儿童建档、预约管理                      │
│     └─ 可以查看和操作视力检测记录                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 五、数据权限设计

### 5.1 角色数据权限

| 角色 | 数据可见范围 |
|------|------------|
| 总部运营 | 全国所有医院、所有科室、所有数据 |
| 医院店长 | 本医院所有科室的数据 |
| 科室医生 | 本科室的排班、预约、检测数据 |
| 科室护士 | 本科室的排班、预约、检测数据（操作权限） |
| 家长用户 | 仅绑定子女的档案和检测记录 |
| TV设备 | 仅绑定医院的儿童档案（用于检测） |

### 5.2 登录权限控制

| 用户类型 | user_type | 登录端 | 可登录方式 |
|----------|-----------|--------|-----------|
| 总部运营 | 1 | admin-web | 用户名+密码 |
| 医院店长 | 2 | store-web | 用户名+密码 |
| 医生 | 3 | store-web | 用户名+密码 |
| 护士 | 4 | store-web | **手机号+密码**（或验证码） |
| 家长 | 5 | parent-web | 手机号+验证码 |

---

## 六、需要修改的现有表清单

### 6.1 新增字段

| 表名 | 新增字段 | 说明 |
|------|---------|------|
| **sys_user** | `dept_id` BIGINT UNSIGNED | 所属科室ID，外键关联 store_department.id |
| **sys_user** | `job_title` VARCHAR(32) | 职称 |
| **sys_user** | `license_no` VARCHAR(64) | 执业证书编号 |

### 6.2 修改字段定义

| 表名 | 字段 | 原定义 | 修改后 |
|------|------|--------|--------|
| **sys_user** | `user_type` | 1总部运营 / 2门店医护 / 3家长 | 1总部运营 / **2门店店长** / **3门店医生** / **4门店护士** / **5门店技师** / 6家长 |
| **schedule_info** | `technician_id` | BIGINT UNSIGNED '技师ID' | BIGINT UNSIGNED '技师/护士ID，外键关联 sys_user.id' |

### 6.3 新增角色数据

```sql
-- 在 sys_role 表中插入新角色
INSERT INTO sys_role (role_code, role_name, role_desc, user_type) VALUES
('store_manager', '门店店长', '门店管理人员', 2),
('store_doctor', '门店医生', '门店医生', 3),
('store_nurse', '门店护士', '门店护士', 4),
('store_technician', '门店技师', '门店技师', 5);
```

---

## 七、完整数据库关系图（含医院管理）

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户权限层                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    │
│  │   sys_user   │◄───│  sys_role    │    │sys_user_role │    │
│  │  (用户表)     │    │  (角色表)     │◄───│(用户角色关联) │    │
│  │  +dept_id    │    │  +新角色      │    │               │    │
│  └──────┬───────┘    └──────────────┘    └──────────────┘    │
│         │                                                       │
│         │ store_id                                              │
│         ▼                                                       │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  store_info (门店/医院)                                    │ │
│  │  ──── 一家医院 = 一个门店 ────                            │ │
│  └────────┬────────────────────────────────────────────────┘ │
│            │                                                     │
│            │ store_id                                            │
│            ▼                                                     │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  store_department (科室)                                  │  │
│  │  ──── 养护科、检测科、门诊科等 ────                      │  │
│  └────────┬────────────────────────────────────────────────┘  │
│            │                                                     │
│            │ dept_id                                             │
│            ▼                                                     │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  sys_user (医生/护士)                                     │  │
│  │  user_type: 3=医生, 4=护士, 5=技师                        │  │
│  │  ──── 护士通过手机号登录 ────                             │  │
│  └────────┬────────────────────────────────────────────────┘  │
│            │                                                     │
│            │ technician_id                                       │
│            ▼                                                     │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  schedule_info (排班)                                     │  │
│  │  ──── 指定了哪位护士在哪个时段值班 ────                   │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                        业务数据层                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    │
│  │ child_profile│    │reserve_order │    │vision_test_  │    │
│  │ (儿童档案)   │    │ (预约订单)   │    │  record      │    │
│  └──────────────┘    └──────────────┘    └──────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 八、关键查询场景

### 8.1 查询某医院的所有科室及医护人员

```sql
-- 查询门店ID=1的所有科室及所属护士
SELECT 
    d.dept_name,
    u.real_name,
    u.phone,
    u.job_title,
    r.role_name
FROM store_department d
LEFT JOIN sys_user u ON u.dept_id = d.id AND u.status = 1
LEFT JOIN sys_user_role ur ON ur.user_id = u.id
LEFT JOIN sys_role r ON r.id = ur.role_id
WHERE d.store_id = 1 AND d.status = 1
ORDER BY d.sort_order, u.real_name;
```

### 8.2 查询某护士的排班信息

```sql
-- 查询护士ID=10的排班
SELECT 
    s.schedule_date,
    s.time_slot_start,
    s.time_slot_end,
    s.max_capacity,
    s.reserved_count,
    d.dept_name
FROM schedule_info s
LEFT JOIN store_department d ON d.id = (
    SELECT dept_id FROM sys_user WHERE id = s.technician_id
)
WHERE s.technician_id = 10
  AND s.schedule_date >= CURDATE()
ORDER BY s.schedule_date, s.time_slot_start;
```

### 8.3 护士登录验证

```sql
-- 护士用手机号登录
SELECT * FROM sys_user 
WHERE phone = '13800138000' 
  AND user_type = 4  -- 护士
  AND status = 1;
```

---

## 九、总结

本次补充设计完善了医院管理模块的核心数据表：

| 新增/修改 | 表名 | 说明 |
|-----------|------|------|
| **新增** | `store_department` | 门店科室表，管理医院下属科室 |
| **修改** | `sys_user` | 增加 `dept_id`、`job_title`、`license_no` 字段；扩展 `user_type` 枚举 |
| **修改** | `sys_role` | 新增 `store_nurse`、`store_doctor`、`store_technician` 角色 |
| **修改** | `schedule_info` | 明确 `technician_id` 外键关联 `sys_user` |

**核心业务流程**：
1. 总部创建医院（store_info）→ 分配店长
2. 店长创建科室（store_department）
3. 店长录入医生/护士（sys_user）→ 指定科室
4. 护士用手机号登录门店Web → 查看排班 → 执行业务操作
