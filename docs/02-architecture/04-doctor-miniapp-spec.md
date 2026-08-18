# Careld 医生端小程序 开发规格说明

> 日期：2026-08-10  
> 范围：医生端微信小程序（doctor-miniapp）+ 后端对应微服务适配  
> 依赖：store-web 0810 改造（医护管理、产品套餐模块）完成后  
> 关联文档：`03-store-web-refactor-spec.md`（医护管理、产品套餐）

---

## 一、产品定位

### 1.1 概述
医生端小程序是门店医护人员（医师、护师、验光师等）的日常移动工作端。核心职责包括：儿童档案管理、排班预约管理、到院签到、养护前后视力检测。

### 1.2 用户角色
| 角色 | 来源 | 权限 |
|------|------|------|
| 医生/医护 | store_staff 表注册（门店管理系统录入） | 全部功能 |
| 店长 | store_staff 中 position='店长' | 全部功能 + 排班管理 |

### 1.3 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | uni-app (Vue 3) | 跨平台，后续可扩展至 H5/App |
| UI 组件 | uView Plus / uni-ui | 小程序原生风格 |
| 状态管理 | Pinia | 与现有前端一致 |
| 后端 | 复用现有微服务 | auth-service / child-service / schedule-service / vision-service / store-service |

---

## 二、登录认证

### 2.1 登录方式
复用 store-web 医护管理模块注册的账号体系。

| 字段 | 说明 |
|------|------|
| 账号 | 注册手机号 |
| 密码 | 手机号后六位（初始），可修改 |
| 认证表 | `store_staff`（store-service） |

### 2.2 登录流程
```
小程序启动 → 检查本地 token
  ↓ 无 token
登录页（手机号 + 密码）
  ↓
POST /api/v1/auth/staff-login { phone, password }
  ↓
auth-service 查询 store_staff 表 → BCrypt 验证
  ↓ 成功
签发 JWT（payload: staff_id, store_id, name, position）
  ↓
存储 token → 进入首页
```

### 2.3 微信快捷登录（可选，二期）
- 微信授权获取手机号 → 匹配 store_staff.phone → 自动登录
- 首次绑定需输入密码验证身份

### 2.4 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/auth/staff-login | POST | 医护人员登录（已在 03-store-web spec 定义） |
| /api/v1/auth/staff/change-password | PUT | 修改登录密码 |
| /api/v1/auth/staff/profile | GET | 获取当前医护信息 |
| /api/v1/auth/staff/profile | PUT | 更新个人信息（仅头像、昵称） |

---

## 三、功能模块总览

### 3.1 页面结构
```
医生端小程序
├── 登录页
├── 首页（工作台）
│   ├── 今日概览（待服务数、已预约数、已签到数）
│   ├── 快捷入口（新建档案、排班管理、视力检测）
│   └── 今日预约列表
├── 档案管理
│   ├── 档案列表（搜索 + 筛选）
│   ├── 新建档案
│   ├── 档案详情
│   │   ├── 基本信息
│   │   ├── 套餐购买记录
│   │   ├── 预约记录
│   │   ├── 视力检测记录
│   │   └── 养护记录
│   └── 编辑档案
├── 排班预约
│   ├── 排班日历视图
│   ├── 创建/编辑排班
│   ├── 预约列表（按日期/状态筛选）
│   ├── 预约详情 & 操作（确认/修改/取消）
│   └── 家长预约处理（审核/调整时间）
├── 签到 & 服务
│   ├── 签到列表（今日待签到）
│   ├── 签到操作
│   ├── 养护前视力检测
│   ├── 养护后视力检测
│   └── 服务完成确认（扣减套餐次数）
└── 我的
    ├── 个人信息
    ├── 修改密码
    └── 退出登录
```

---

## 四、档案管理模块

### 4.1 功能说明
儿童电子档案的增删改查。**所有档案首次创建均由医生端发起**，家长端后续可绑定查看。

### 4.2 核心流程
```
医生新建档案 → 填写儿童信息 → 系统生成档案编号 → 审核通过（默认通过）
  ↓
家长通过手机号绑定（家长端功能）→ 可查看自己孩子的档案
```

### 4.3 数据表 `child_profile`（已存在，复用）

| 字段 | 类型 | 说明 | 医生端操作 |
|------|------|------|-----------|
| id | BIGINT | 主键 | - |
| child_code | VARCHAR(32) | 档案编号（自动生成） | 只读 |
| store_id | BIGINT | 所属门店（自动填充当前登录门店） | 自动 |
| name_encrypted | VARCHAR(256) | 姓名（加密存储） | 必填 |
| name_mask | VARCHAR(64) | 姓名（脱敏显示） | 必填 |
| phone_encrypted | VARCHAR(256) | 家长手机号（加密） | 必填 |
| phone_mask | VARCHAR(20) | 手机号（脱敏） | 必填 |
| birth_date | DATE | 出生日期 | 必填 |
| gender | TINYINT | 性别：0女 1男 | 必填 |
| eye_condition | TEXT | 眼部状况 | 选填 |
| medical_history | TEXT | 病史 | 选填 |
| allergy_info | TEXT | 过敏史 | 选填 |
| family_history | TEXT | 家族眼病史 | 选填 |
| audit_status | TINYINT | 审核状态 | 医生创建默认=1（已通过） |
| created_by | BIGINT | 创建人（当前医护ID） | 自动 |

### 4.4 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/children | GET | 档案列表（分页，支持 keyword/auditStatus 筛选） |
| /api/v1/children | POST | 新建档案（医生端发起，自动审核通过） |
| /api/v1/children/{id} | GET | 档案详情（含套餐、预约、视力记录汇总） |
| /api/v1/children/{id} | PUT | 编辑档案 |
| /api/v1/children/{id} | DELETE | 删除档案（逻辑删除） |
| /api/v1/children/{id}/summary | GET | 档案摘要（套餐剩余次数、最近预约、最近视力） |

### 4.5 前端页面

#### 4.5.1 档案列表页
- **搜索栏**：姓名/手机号模糊搜索
- **筛选**：按性别、年龄段筛选
- **列表项**：姓名、性别、年龄、家长手机号（脱敏）、档案编号、创建时间
- **浮动按钮**：+ 新建档案
- **点击列表项**：进入档案详情

#### 4.5.2 新建/编辑档案页
| 字段 | 组件 | 校验 |
|------|------|------|
| 姓名 | input | 必填 |
| 性别 | radio（男/女） | 必填 |
| 出生日期 | date-picker | 必填 |
| 家长手机号 | input（数字键盘） | 必填，11位手机号 |
| 眼部状况 | textarea | 选填 |
| 病史 | textarea | 选填 |
| 过敏史 | textarea | 选填 |
| 家族眼病史 | textarea | 选填 |

- **新建成功后**：自动生成档案编号，默认审核通过，提示"档案创建成功"

#### 4.5.3 档案详情页
- **顶部**：姓名、性别、年龄、档案编号
- **Tab 切换**：
  - 基本信息（可编辑）
  - 套餐记录（关联 package_purchase，显示套餐名/总次数/已用/剩余）
  - 预约记录（关联 reserve_order，时间线展示）
  - 视力记录（关联 vision_test_record，养护前/后对比）

---

## 五、排班预约模块

### 5.1 功能说明
| 功能 | 说明 |
|------|------|
| 排班管理 | 医生/店长创建排班时段，设定可预约数量 |
| 预约管理 | 查看/处理所有预约（门店创建 + 家长发起） |
| 家长预约处理 | 家长在家长端发起预约请求，医生端可确认/调整时间/拒绝 |

### 5.2 核心业务流程

#### 5.2.1 排班流程
```
店长/医生创建排班 → 选择日期 + 时段 + 最大预约数
  ↓
排班数据发布 → 家长端可见可预约
```

#### 5.2.2 预约流程（两种来源）
```
来源1：医生端直接创建预约
  → 选择儿童 → 选择排班时段 → 检查套餐剩余次数 → 创建预约

来源2：家长端发起预约
  → 家长选择排班时段 → 提交预约请求（status=0 待确认）
  → 医生端收到通知 → 确认/调整时间/拒绝
  → 确认后检查套餐剩余次数 → 创建预约
```

#### 5.2.3 套餐次数校验
```
创建/确认预约时：
  查询该儿童的 package_purchase（status=1, remaining_sessions > 0）
  ↓ 有可用套餐
允许预约，标记关联 purchase_id
  ↓ 无可用套餐
拒绝预约，提示"该儿童无可用套餐，请先购买"
```

### 5.3 数据表

#### `schedule_info`（已存在，复用）
| 字段 | 说明 | 医生端操作 |
|------|------|-----------|
| store_id | 门店ID | 自动 |
| schedule_date | 排班日期 | 必填 |
| technician_id | 技师/医生ID | 默认当前用户，可切换 |
| technician_name | 技师姓名 | 自动 |
| time_slot_start | 时段开始 | 必填 |
| time_slot_end | 时段结束 | 必填 |
| max_capacity | 最大预约数 | 必填 |
| reserved_count | 已预约数 | 自动计算 |

#### `reserve_order`（已存在，扩展）
| 字段 | 说明 | 医生端操作 |
|------|------|-----------|
| order_no | 预约单号（自动生成） | 自动 |
| store_id | 门店ID | 自动 |
| schedule_id | 排班ID | 选择排班 |
| child_id | 儿童ID | 选择儿童 |
| reserve_date | 预约日期 | 来自排班 |
| reserve_time_start/end | 预约时段 | 来自排班 |
| reserve_type | 预约类型 | 选择 |
| status | 状态 | 医生操作 |
| source | 来源：1门店 2家长 | 自动 |
| purchase_id | **新增** 关联套餐购买记录ID | 自动关联 |

**reserve_order.status 扩展**：
| 值 | 说明 | 触发方 |
|----|------|--------|
| 0 | 待到店 | 创建预约后 |
| 1 | 已到店（已签到） | 签到操作 |
| 2 | 服务中 | 开始养护 |
| 3 | 已完成 | 养护后检测完成 |
| 4 | 已取消 | 取消操作 |
| 5 | **待确认**（新增） | 家长发起预约时 |
| 6 | **已拒绝**（新增） | 医生拒绝家长预约 |

### 5.4 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/schedules | GET | 排班列表（按日期范围查询） |
| /api/v1/schedules | POST | 创建排班 |
| /api/v1/schedules/batch | POST | 批量创建排班（选日期范围 + 时段 + 重复规则） |
| /api/v1/schedules/{id} | PUT | 编辑排班 |
| /api/v1/schedules/{id} | DELETE | 取消排班（有预约时提示） |
| /api/v1/schedules/reserves | GET | 预约列表（按日期/状态/儿童筛选） |
| /api/v1/schedules/reserves | POST | 医生创建预约（自动扣减套餐次数） |
| /api/v1/schedules/reserves/{id}/confirm | POST | **新增** 确认家长预约 |
| /api/v1/schedules/reserves/{id}/reject | POST | **新增** 拒绝家长预约 |
| /api/v1/schedules/reserves/{id}/reschedule | POST | **新增** 调整预约时间 |
| /api/v1/schedules/reserves/{id}/cancel | POST | 取消预约（恢复套餐次数） |
| /api/v1/schedules/reserves/pending | GET | **新增** 待确认的家长预约列表 |

### 5.5 前端页面

#### 5.5.1 排班日历页
- **日历视图**：月历展示，每天显示排班数量标记
- **点击日期**：展示当天排班详情列表
- **浮动按钮**：+ 新建排班
- **新建排班弹窗**：

| 字段 | 组件 | 说明 |
|------|------|------|
| 排班日期 | date-picker | 必填 |
| 开始时间 | time-picker | 必填 |
| 结束时间 | time-picker | 必填 |
| 执行医生 | select（本门店医护列表） | 默认当前用户 |
| 最大预约数 | stepper | 默认1 |
| 重复 | select（不重复/每周重复/自定义） | 可选 |

#### 5.5.2 预约列表页
- **Tab 切换**：全部 / 待确认 / 待到店 / 服务中 / 已完成 / 已取消
- **列表项**：儿童姓名、预约时间、预约类型、来源标识（门店/家长）、状态标签
- **待确认 Tab**：突出显示家长发起的预约请求，带确认/拒绝按钮
- **点击列表项**：进入预约详情

#### 5.5.3 预约详情页
- **信息展示**：儿童姓名、预约时间、排班医生、预约类型、来源、状态
- **操作按钮**（根据状态显示）：
  - 待确认 → 确认 / 改期 / 拒绝
  - 待到店 → 改期 / 取消
  - 服务中 → 完成服务
- **改期弹窗**：选择新的排班时段

---

## 六、签到 & 服务流程

### 6.1 完整服务流程
```
儿童到院 → 签到 → 养护前视力检测 → 养护服务 → 养护后视力检测 → 完成（扣减次数）
```

### 6.2 签到

#### 6.2.1 签到流程
```
选择预约/选择儿童 → 点击签到 → 更新 reserve_order.status = 1（已到店）
  ↓
记录签到时间 → 进入服务流程
```

#### 6.2.2 签到方式
| 方式 | 说明 |
|------|------|
| 预约签到 | 从今日预约列表中选择，一键签到 |
| 扫码签到 | 扫描儿童/家长二维码签到（二期） |
|  walk-in | 无预约直接签到，自动创建预约记录 |

### 6.3 视力检测

#### 6.3.1 检测时机
| 类型 | test_type | 时机 | 说明 |
|------|-----------|------|------|
| 养护前 | 1 | 签到后、养护开始前 | 记录当前视力基线 |
| 养护后 | 2 | 养护服务结束后 | 记录养护后视力，与养护前对比 |

#### 6.3.2 检测数据
复用 `vision_test_record` 表：

| 字段 | 说明 | 录入方式 |
|------|------|----------|
| child_id | 儿童ID | 自动（当前服务儿童） |
| reserve_id | 关联预约ID | 自动 |
| test_type | 1=养护前 2=养护后 | 选择 |
| eye_type | 1=左眼 2=右眼 | 分别录入 |
| vision_level | 视力值（如 4.8、5.0） | 手动输入 |
| vision_decimal | 小数表示（如 0.6、1.0） | 自动换算 |
| tester_id | 检测人 | 自动（当前医护） |
| tester_name | 检测人姓名 | 自动 |
| sync_source | 来源：1=TV同步 2=手动录入 | 默认2（手动） |
| remark | 备注 | 选填 |

#### 6.3.3 TV 设备联动（可选）
- 若门店有 TV 视力检测设备，可选择"TV 检测"模式
- TV 端完成检测后自动同步结果到小程序（通过 sync-service）
- 也可手动录入（无 TV 设备时）

### 6.4 服务完成 & 次数扣减

#### 6.4.1 完成流程
```
养护后视力检测完成 → 点击"完成服务"
  ↓
系统自动执行：
  1. reserve_order.status → 3（已完成）
  2. reserve_order.completed_at → 当前时间
  3. package_purchase.used_sessions += 1
  4. package_purchase.remaining_sessions -= 1
  5. 若 remaining_sessions = 0 → purchase.status → 2（已用完）
  ↓
提示"服务完成，已扣减1次套餐次数"
```

### 6.5 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/service/checkin | POST | 签到（更新预约状态为已到店） |
| /api/v1/service/checkin/today | GET | 今日待签到/已签到列表 |
| /api/v1/service/{reserveId}/start | POST | 开始服务（状态→服务中） |
| /api/v1/service/{reserveId}/complete | POST | 完成服务（状态→已完成 + 扣减次数） |
| /api/v1/vision/records | POST | 录入视力检测结果 |
| /api/v1/vision/records/by-reserve | GET | 按预约ID查询视力记录（养护前/后） |
| /api/v1/vision/compare | GET | 视力对比（养护前 vs 养护后） |

### 6.6 前端页面

#### 6.6.1 今日工作台（首页核心区域）
```
┌─────────────────────────────┐
│  今日工作台                   │
│  ┌─────┐ ┌─────┐ ┌─────┐   │
│  │待签到│ │服务中│ │已完成│   │
│  │  5  │ │  2  │ │  8  │   │
│  └─────┘ └─────┘ └─────┘   │
│                             │
│  待处理                      │
│  ┌─────────────────────────┐│
│  │ 张三  10:00  待签到      ││
│  │ 李四  10:30  服务中      ││
│  │ 王五  11:00  待签到      ││
│  └─────────────────────────┘│
│                             │
│  待确认预约（家长发起）        │
│  ┌─────────────────────────┐│
│  │ 赵六  明天 14:00 [确认]  ││
│  └─────────────────────────┘│
└─────────────────────────────┘
```

#### 6.6.2 服务流程页（核心操作页）
```
步骤条展示当前进度：签到 → 养护前检测 → 养护中 → 养护后检测 → 完成

[签到] 按钮 → 签到完成
  ↓
[养护前视力检测] 
  左眼: [___] 右眼: [___] 备注: [___]
  [保存]
  ↓
[开始养护]
  ↓
[养护后视力检测]
  左眼: [___] 右眼: [___] 备注: [___]
  [保存]
  ↓
[完成服务] → 自动扣减套餐次数 → 提示剩余次数
```

---

## 七、首页（工作台）

### 7.1 页面布局
```
┌─────────────────────────────┐
│  早上好，张医生              │
│  成都朝阳门店                │
├─────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐   │
│  │今日  │ │待确认│ │本月  │   │
│  │预约  │ │预约  │ │服务  │   │
│  │ 15  │ │  3  │ │ 128 │   │
│  └─────┘ └─────┘ └─────┘   │
├─────────────────────────────┤
│  快捷入口                    │
│  ┌──────┐ ┌──────┐         │
│  │新建  │ │排班  │         │
│  │档案  │ │管理  │         │
│  └──────┘ └──────┘         │
│  ┌──────┐ ┌──────┐         │
│  │视力  │ │套餐  │         │
│  │检测  │ │查询  │         │
│  └──────┘ └──────┘         │
├─────────────────────────────┤
│  今日预约                   │
│  09:00 张三  待签到 [签到]   │
│  09:30 李四  服务中 [检测]   │
│  10:00 王五  已完成          │
│  ...                        │
└─────────────────────────────┘
```

### 7.2 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/doctor/dashboard | GET | 工作台数据（今日预约数、待确认数、本月服务数） |
| /api/v1/doctor/today-reserves | GET | 今日预约列表 |
| /api/v1/statistics/doctor-monthly | GET | 本月服务统计 |

---

## 八、数据库变更

### 8.1 扩展 `reserve_order` 表
```sql
-- 新增字段
ALTER TABLE reserve_order ADD COLUMN purchase_id BIGINT UNSIGNED DEFAULT NULL 
  COMMENT '关联套餐购买记录ID' AFTER source;
ALTER TABLE reserve_order ADD KEY idx_purchase_id (purchase_id);

-- status 扩展说明（无需改表结构，仅扩展枚举值）：
-- 5=待确认（家长发起预约）
-- 6=已拒绝（医生拒绝家长预约）
```

### 8.2 新增签到记录表（可选，增强签到追踪）
```sql
CREATE TABLE IF NOT EXISTS service_checkin (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '签到ID',
    reserve_id BIGINT UNSIGNED NOT NULL COMMENT '预约ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    child_id BIGINT UNSIGNED NOT NULL COMMENT '儿童ID',
    staff_id BIGINT UNSIGNED NOT NULL COMMENT '操作医护ID',
    checkin_time DATETIME NOT NULL COMMENT '签到时间',
    checkout_time DATETIME DEFAULT NULL COMMENT '签退时间',
    service_start_time DATETIME DEFAULT NULL COMMENT '服务开始时间',
    service_end_time DATETIME DEFAULT NULL COMMENT '服务结束时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1已签到 2服务中 3已完成',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_reserve_id (reserve_id),
    KEY idx_store_date (store_id, checkin_time),
    KEY idx_child_id (child_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务签到记录表';
```

---

## 九、后端接口汇总

### 9.1 认证（auth-service :8281）
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/auth/staff-login | POST | 医护登录 |
| /api/v1/auth/staff/change-password | PUT | 修改密码 |
| /api/v1/auth/staff/profile | GET | 当前医护信息 |

### 9.2 档案管理（child-service :8284）
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/children | GET | 档案列表 |
| /api/v1/children | POST | 新建档案 |
| /api/v1/children/{id} | GET/PUT/DELETE | 档案详情/编辑/删除 |
| /api/v1/children/{id}/summary | GET | 档案摘要（套餐+预约+视力） |

### 9.3 排班预约（schedule-service :8285）
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/schedules | GET/POST | 排班列表/创建 |
| /api/v1/schedules/batch | POST | 批量排班 |
| /api/v1/schedules/{id} | PUT/DELETE | 编辑/取消排班 |
| /api/v1/schedules/reserves | GET/POST | 预约列表/创建 |
| /api/v1/schedules/reserves/{id}/confirm | POST | 确认家长预约 |
| /api/v1/schedules/reserves/{id}/reject | POST | 拒绝家长预约 |
| /api/v1/schedules/reserves/{id}/reschedule | POST | 调整预约时间 |
| /api/v1/schedules/reserves/{id}/cancel | POST | 取消预约 |
| /api/v1/schedules/reserves/pending | GET | 待确认预约 |

### 9.4 签到 & 服务（schedule-service :8285 或新建 service 模块）
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/service/checkin | POST | 签到 |
| /api/v1/service/checkin/today | GET | 今日签到列表 |
| /api/v1/service/{reserveId}/start | POST | 开始服务 |
| /api/v1/service/{reserveId}/complete | POST | 完成服务 + 扣减次数 |

### 9.5 视力检测（vision-service :8286）
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/vision/records | GET/POST | 视力记录列表/录入 |
| /api/v1/vision/records/by-reserve | GET | 按预约查询视力记录 |
| /api/v1/vision/compare | GET | 养护前后视力对比 |

### 9.6 套餐查询（store-service :8283，只读）
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/package-purchases/child/{childId}/available | GET | 查询儿童可用套餐 |
| /api/v1/service-packages/active | GET | 在售套餐列表 |

### 9.7 工作台（store-service :8283 或 schedule-service :8285）
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/doctor/dashboard | GET | 工作台统计数据 |
| /api/v1/doctor/today-reserves | GET | 今日预约列表 |

---

## 十、前端项目结构

```
frontend/doctor-miniapp/
├── src/
│   ├── api/
│   │   ├── request.ts          # 请求封装（同其他前端）
│   │   ├── auth.ts             # 认证 API
│   │   ├── child.ts            # 档案 API
│   │   ├── schedule.ts         # 排班 API
│   │   ├── reserve.ts          # 预约 API
│   │   ├── service.ts          # 签到/服务 API
│   │   └── vision.ts           # 视力检测 API
│   ├── pages/
│   │   ├── login/index.vue     # 登录页
│   │   ├── home/index.vue      # 首页工作台
│   │   ├── child/
│   │   │   ├── list.vue        # 档案列表
│   │   │   ├── create.vue      # 新建档案
│   │   │   └── detail.vue      # 档案详情
│   │   ├── schedule/
│   │   │   ├── calendar.vue    # 排班日历
│   │   │   └── create.vue      # 创建排班
│   │   ├── reserve/
│   │   │   ├── list.vue        # 预约列表
│   │   │   ├── detail.vue      # 预约详情
│   │   │   └── pending.vue     # 待确认预约
│   │   ├── service/
│   │   │   ├── workflow.vue    # 服务流程页（签到→检测→养护→检测→完成）
│   │   │   └── checkin.vue     # 签到页
│   │   └── profile/
│   │       └── index.vue       # 我的
│   ├── stores/
│   │   ├── user.ts             # 用户状态
│   │   └── app.ts              # 应用状态
│   ├── types/
│   │   └── index.ts            # 类型定义
│   ├── utils/
│   │   └── index.ts            # 工具函数
│   ├── App.vue
│   ├── main.ts
│   ├── manifest.json           # uni-app 配置
│   ├── pages.json              # 页面路由配置
│   └── uni.scss                # 全局样式变量
├── package.json
├── vite.config.ts
└── tsconfig.json
```

---

## 十一、关键业务规则汇总

### 11.1 档案创建规则
| 规则 | 说明 |
|------|------|
| 创建方 | 仅医生端可创建档案 |
| 审核 | 医生创建的档案默认审核通过（audit_status=1） |
| 编号 | 系统自动生成：`CH` + 年月日 + 4位序号 |
| 绑定 | 家长通过手机号自动绑定（手机号匹配 phone_encrypted） |

### 11.2 预约次数规则
| 规则 | 说明 |
|------|------|
| 前置条件 | 儿童必须有 status=1 且 remaining_sessions>0 的购买记录 |
| 扣减时机 | 服务完成时扣减（非预约创建时） |
| 取消恢复 | 取消预约时不恢复次数（只有未完成才不扣减） |
| 多次购买 | 优先扣减最早创建的购买记录（FIFO） |
| 无套餐 | 拒绝预约，提示需先购买套餐 |

### 11.3 家长预约处理规则
| 规则 | 说明 |
|------|------|
| 家长提交 | 状态=5（待确认），不立即占用排班名额 |
| 医生确认 | 检查套餐 → 状态改为0（待到店）→ 占用名额 |
| 医生拒绝 | 状态改为6（已拒绝）→ 释放名额 → 通知家长 |
| 医生改期 | 调整到新的排班时段 → 通知家长 |
| 超时未处理 | 可选：24小时未自动确认（二期） |

### 11.4 视力检测规则
| 规则 | 说明 |
|------|------|
| 配对录入 | 同一次预约必须录入养护前 + 养护后两条记录 |
| 分别录入 | 左眼和右眼分别记录 |
| 自动对比 | 养护后录入后自动计算改善值 |
| TV 同步 | 支持 TV 设备自动同步或手动录入两种方式 |

---

## 十二、实施优先级

| 优先级 | 任务 | 依赖 | 说明 |
|--------|------|------|------|
| P0 | uni-app 项目初始化 | 无 | 创建 doctor-miniapp 项目 |
| P0 | 登录认证（staff-login） | store_staff 表 | 核心入口 |
| P0 | 后端：reserve_order 扩展字段 | 无 | purchase_id + 新状态 |
| P1 | 后端：签到/服务流程接口 | P0 | checkin/start/complete |
| P1 | 后端：家长预约确认/拒绝接口 | P0 | confirm/reject/reschedule |
| P1 | 前端：登录页 + 首页工作台 | P0 | 核心页面 |
| P1 | 前端：档案管理（CRUD） | P0 | 核心页面 |
| P2 | 前端：排班管理（日历+创建） | P1 | |
| P2 | 前端：预约管理（列表+详情+处理） | P1 | 含家长预约处理 |
| P2 | 前端：签到 & 服务流程页 | P1-后端 | 核心操作页 |
| P2 | 前端：视力检测录入 | P2-服务流程 | |
| P3 | 前端：我的页面（个人信息+改密） | P0 | |
| P3 | 后端：工作台统计接口 | P1 | |
| P4 | 微信快捷登录 | P0 | 二期功能 |
| P4 | TV 设备视力同步 | sync-service | 二期功能 |
