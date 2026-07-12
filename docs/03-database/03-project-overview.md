# Careld 可尔欧得视力养护服务信息系统 - 项目整体描述

> 本文档基于代码库和数据库结构，描述整个项目的架构、模块划分及各部分之间的关系。

---

## 一、项目概述

**Careld 可尔欧得视力养护服务信息系统** 是一套面向线下视力养护门店（医院）的云端管理解决方案，覆盖 **四端多角色**：

| 终端 | 技术形态 | 使用角色 | 核心场景 |
|------|---------|---------|---------|
| 运营中心Web | Vue3 SPA (PC) | 总部运营管理员 | 全局管理、数据看板、系统配置 |
| 门店Web | Vue3 SPA (PC) | 门店店长、医生、护士、技师 | 儿童建档、预约排班、记录查询 |
| 家长Web | Vue3 SPA (H5响应式) | 家长用户 | 子女档案、检测报告、视力趋势 |
| 安卓TV APK | Android (Kotlin + SQLite) | TV设备终端 | 电子视力表、离线检测、数据同步 |

> **说明**：系统中"门店"等同于"医院"，每家门店对应一家医院，医院内设有科室，科室下管理医生、护士、技师等医护人员。护士通过手机号登录门店Web端进行操作。

**核心业务流程**：
```
建档 → 审核 → 排班 → 预约 → 养护前检测 → 养护服务 → 养护后复测 → 数据同步 → 多端查看
```

---

## 二、技术架构总览

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         终端接入层                                 │
├─────────────┬─────────────┬─────────────┬─────────────────────────┤
│ 运营中心Web  │  门店Web     │  家长Web    │      安卓TV APK         │
│  (Vue3)     │   (Vue3)    │   (Vue3)    │  (Kotlin+SQLite)       │
└──────┬──────┴──────┬──────┴──────┬──────┴───────────┬─────────────┘
       │             │             │                │
       └─────────────┴─────────────┴────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │    API网关层       │  ← 统一鉴权、限流、HTTPS
                    └─────────┬─────────┘
                              │
       ┌──────────────────────┼──────────────────────┐
       │                      │                      │
┌──────┴──────┐  ┌────────────┴────────────┐  ┌─────┴──────┐
│  业务服务层  │  │      数据存储层         │  │  网络支撑  │
│ (SpringBoot)│  │                         │  │            │
│             │  │  MySQL  Redis  OSS      │  │ 宽带/4G/  │
│ •用户服务    │  │  SQLite(TV本地)         │  │ LTE离线    │
│ •档案服务    │  │                         │  │            │
│ •预约服务    │  │                         │  │            │
│ •视力检测    │  │                         │  │            │
│ •同步服务    │  │                         │  │            │
└─────────────┘  └─────────────────────────┘  └────────────┘
```

### 2.2 技术栈选型

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **后端** | Java | 17 LTS | 长期支持版本 |
| | Spring Boot | 3.2.x | 微服务快速开发 |
| | MyBatis Plus | 3.5.x | ORM框架 |
| | JWT | 0.12.3 | Token认证 |
| | Knife4j | 4.4.0 | API文档 |
| **前端** | Vue | 3.5.x | 组合式API |
| | Vite | 8.x | 构建工具 |
| | Element Plus | 2.14.x | UI组件库 |
| | Pinia | 3.x | 状态管理 |
| | Axios | 1.18.x | HTTP客户端 |
| **TV端** | Kotlin | 1.9.x | Android开发 |
| | Room | - | SQLite ORM |
| | Hilt | 2.x | 依赖注入 |
| | Retrofit | 2.x | 网络请求 |

---

## 三、后端微服务架构

### 3.1 服务拆分

```
backend/
├── careld-common/           # 公共模块（实体基类、工具类、枚举、异常处理）
├── careld-auth-service/     # 认证授权服务 → sys_user, sys_role, sys_user_role
├── careld-user-service/     # 用户管理服务 → sys_user
├── careld-store-service/    # 门店管理服务 → store_info, store_tv_device
├── careld-child-service/    # 儿童档案服务 → child_profile
├── careld-schedule-service/ # 预约排班服务 → schedule_info, reserve_order
├── careld-vision-service/   # 视力检测服务 → vision_test_record
└── careld-sync-service/     # TV同步服务 → sync_log, local_child_profile, local_vision_record
```

### 3.2 各服务与数据库表的对应关系

| 微服务 | 负责的数据库表 | 核心职责 |
|--------|--------------|---------|
| **careld-auth-service** | sys_user, sys_role, sys_user_role | 用户登录、Token管理、权限校验 |
| **careld-user-service** | sys_user | 用户CRUD、账号管理 |
| **careld-store-service** | store_info, store_tv_device, store_department | 门店信息管理、科室管理、TV设备绑定 |
| **careld-child-service** | child_profile | 儿童档案CRUD、档案审核 |
| **careld-schedule-service** | schedule_info, reserve_order | 技师排班、预约管理 |
| **careld-vision-service** | vision_test_record | 检测记录管理、视力对比分析 |
| **careld-sync-service** | sync_log (云端) | TV数据同步、离线数据接收、增量下发 |

---

## 四、前端应用架构

### 4.1 运营中心Web (admin-web)

**路由结构**：
```
/                           → 主布局
├── /login                  → 登录页
├── /dashboard              → 数据看板（首页）
├── /store/list             → 门店列表
├── /store/audit            → 档案审核
├── /schedule               → 排班监控
├── /device                 → 设备管理
├── /user                   → 用户管理
└── /settings               → 系统设置
```

**API模块**：
- `auth.ts` - 登录/登出/Token刷新
- `user.ts` - 用户管理（含医生、护士账号）
- `store.ts` - 门店/医院管理
- `department.ts` - 科室管理
- `child.ts` - 儿童档案（审核）
- `schedule.ts` - 排班管理
- `device.ts` - TV设备管理
- `statistics.ts` - 数据统计

### 4.2 门店Web (store-web)

**路由结构**：
```
/                           → 主布局
├── /login                  → 登录页（护士手机号登录）
├── /child                  → 儿童档案
├── /schedule               → 排班预约
├── /vision                 → 视力记录
├── /device                 → 设备管理
└── /department             → 科室管理（店长可见）
```

**权限控制**：
- **店长**：可查看/管理所有菜单（含科室管理、医护账号管理）
- **医生/护士**：仅可查看/操作儿童档案、排班预约、视力记录
- **护士**：通过手机号+密码登录，只能操作自己科室的数据

### 4.3 家长Web (parent-web)

**路由结构**：
```
/                           → 主布局
├── /login                  → 登录页
├── /profile                → 子女档案
├── /report                 → 检测报告
└── /trend                  → 视力趋势
```

---

## 五、TV端APK架构

### 5.1 技术栈
- **语言**：Kotlin
- **最低SDK**：Android 7.0 (API 24)
- **目标SDK**：Android 14 (API 34)
- **本地数据库**：Room + SQLite
- **依赖注入**：Hilt
- **网络**：Retrofit + OkHttp
- **协程**：Kotlin Coroutines

### 5.2 本地数据库表（SQLite）

| 表名 | 说明 | 对应云端表 |
|------|------|-----------|
| `local_child_profile` | 本地儿童简档缓存 | child_profile |
| `local_vision_record` | 本地视力检测记录 | vision_test_record |
| `local_sync_log` | 本地同步日志 | sync_log |
| `local_config` | 本地配置表 | - |

### 5.3 核心功能模块
- **电子视力表**：符合GB 11533-2011标准，支持左右眼分别测量
- **离线缓存**：断网时本地存储，联网后自动同步
- **设备校准**：屏幕校准模块，视标物理尺寸计算
- **数据同步**：增量同步，支持断点续传和失败重试

---

## 六、数据库表关系与业务映射

### 6.1 MySQL表关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                          用户权限层                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   sys_user   │◄───│  sys_role    │    │sys_user_role │      │
│  │  (用户表)     │    │  (角色表)     │◄───│(用户角色关联) │      │
│  └──────┬───────┘    └──────────────┘    └──────────────┘      │
│         │                                                       │
│         │ store_id                                              │
│         │ dept_id                                               │
│         ▼                                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  store_info (门店/医院)                                   │   │
│  │  ──── 一家门店 = 一家医院 ────                            │   │
│  └────────┬────────────────────────────────────────────────┘   │
│            │                                                     │
│            │ store_id                                            │
│            ▼                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  store_department (科室)                                  │   │
│  │  ──── 养护科、检测科、门诊科等 ────                      │   │
│  └────────┬────────────────────────────────────────────────┘   │
│            │                                                     │
│            │ dept_id                                             │
│            ▼                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  sys_user (医生/护士/技师)                                │   │
│  │  user_type: 2=店长, 3=医生, 4=护士, 5=技师               │   │
│  └────────┬────────────────────────────────────────────────┘   │
│            │                                                     │
├────────────┼─────────────────────────────────────────────────────┤
│            │                  业务数据层                            │
│            │    ┌──────────────┐    ┌──────────────┐              │
│            │    │store_tv_device│   │ child_profile │              │
│            │    │  (TV设备表)  │   │ (儿童档案表)  │              │
│            │    └──────┬───────┘    └──────┬───────┘              │
│            │           │                   │                     │
│            │           │                   │                     │
│            │    ┌──────┴──────┐    ┌──────┴──────┐              │
│            │    │schedule_info│◄───│reserve_order│              │
│            │    │ (排班表)     │    │ (预约订单表) │              │
│            │    └─────────────┘    └──────┬───────┘              │
│            │                               │                     │
│            │    ┌──────────────────────────┘                     │
│            │    │                                                 │
│            │    ▼                                                 │
│            │    ┌──────────────────────┐                        │
│            └───►│  vision_test_record  │                        │
│                 │  (视力检测记录表)    │                        │
│                 └──────────┬───────────┘                        │
│                          │                                     │
│                          │ device_id (FK)                     │
│                          ▼                                     │
│               ┌──────────────────────┐                        │
│               │      sync_log        │                        │
│               │   (数据同步日志表)    │                        │
│               └──────────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 表关系详细说明

#### 6.2.1 用户权限体系

| 关系 | 说明 |
|------|------|
| **sys_user → store_info** | 用户通过 `store_id` 关联门店，门店医护属于特定门店 |
| **sys_user → store_department** | 用户通过 `dept_id` 关联所属科室 |
| **sys_user_role → sys_user** | 用户角色关联表通过 `user_id` 关联用户 |
| **sys_user_role → sys_role** | 用户角色关联表通过 `role_id` 关联角色 |

**角色定义**：

| 角色编码 | 角色名称 | user_type | 说明 |
|---------|---------|-----------|------|
| `super_admin` | 超级管理员 | 1 | 系统超级管理员 |
| `ops_admin` | 运营管理员 | 1 | 总部运营人员 |
| `store_manager` | 门店店长 | 2 | 门店管理人员，管理本医院 |
| `store_doctor` | 门店医生 | 3 | 门店医生，归属某个科室 |
| `store_nurse` | 门店护士 | 4 | 门店护士，归属某个科室，通过手机登录 |
| `store_technician` | 门店技师 | 5 | 门店技师 |
| `parent` | 家长 | 6 | 家长用户 |

#### 6.2.2 医院组织架构体系

| 关系 | 说明 |
|------|------|
| **store_department → store_info** | 科室通过 `store_id` 关联门店（医院），一院多科室 |
| **sys_user → store_department** | 医护通过 `dept_id` 关联科室，一科多医护 |
| **sys_user → store_info** | 医护通过 `store_id` 关联门店（医院） |
| **store_tv_device → store_info** | TV设备通过 `store_id` 关联门店，一店多设备 |

**科室类型**：
- `1` - 门诊科
- `2` - 养护科
- `3` - 检测科
- `4` - 其他

#### 6.2.3 儿童档案体系

| 关系 | 说明 |
|------|------|
| **child_profile → store_info** | 儿童档案通过 `store_id` 关联门店（医院） |
| **child_profile → sys_user** | 通过 `parent_user_id` 关联家长用户 |
| **child_profile → store_department** | 通过 `dept_id` 关联所属科室（可选） |

#### 6.2.4 预约排班体系

| 关系 | 说明 |
|------|------|
| **schedule_info → store_info** | 排班通过 `store_id` 关联门店（医院） |
| **schedule_info → store_department** | 排班通过 `dept_id` 关联科室 |
| **schedule_info → sys_user** | 排班通过 `technician_id` 关联护士/医生 |
| **reserve_order → store_info** | 预约通过 `store_id` 关联门店（医院） |
| **reserve_order → schedule_info** | 预约通过 `schedule_id` 关联排班 |
| **reserve_order → child_profile** | 预约通过 `child_id` 关联儿童档案 |

#### 6.2.5 视力检测体系

| 关系 | 说明 |
|------|------|
| **vision_test_record → child_profile** | 检测记录通过 `child_id` 关联儿童 |
| **vision_test_record → store_info** | 检测记录通过 `store_id` 关联门店（医院） |
| **vision_test_record → store_department** | 检测记录通过 `dept_id` 关联科室 |
| **vision_test_record → store_tv_device** | 检测记录通过 `device_id` 关联设备 |
| **vision_test_record → reserve_order** | 检测记录通过 `reserve_id` 关联预约 |

#### 6.2.6 数据同步体系

| 关系 | 说明 |
|------|------|
| **sync_log → store_tv_device** | 同步日志通过 `device_id` 关联设备 |
| **sync_log → store_info** | 同步日志通过 `store_id` 关联门店 |

### 6.3 医院管理核心业务流程

```
┌─────────────────────────────────────────────────────────┐
│                    总部运营中心                           │
│  1. 创建医院（store_info）                               │
│  2. 创建医院管理员（门店店长）                            │
│  └─ user_type = 2（门店店长）                            │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      门店Web端                          │
│  3. 店长登录后创建科室（store_department）               │
│     ├─ 养护科、检测科、门诊科等                          │
│     └─ 每个科室关联当前门店（医院）                      │
│  4. 录入医生和护士（sys_user）                           │
│     ├─ 医生：user_type = 3                               │
│     ├─ 护士：user_type = 4（手机号登录）                 │
│     └─ 每个医护指定 dept_id（所属科室）                 │
│  5. 排班（schedule_info）                               │
│     └─ 指定 technician_id（护士/医生ID）                 │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                      门店Web端                          │
│                  (护士登录)                              │
│  6. 护士用手机号+密码登录                                │
│  7. 查看自己所属科室的排班                               │
│  8. 执行儿童建档、预约、检测等业务                      │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
           ┌──────────────────────────────────────────┐
           │              业务数据流向                   │
           │                                            │
           │   child_profile → schedule_info            │
           │         │              │                     │
           │         │              ▼                     │
           │         │       reserve_order                │
           │         │              │                     │
           │         │              ▼                     │
           │         └──────► vision_test_record ◄──────│
           │                                            │
           └────────────────────────────────────────────┘
                            │
                            ▼
                      ┌──────────┐
                      │ 家长Web端 │
                      │(查看报告) │
                      └──────────┘
```

---

## 七、前后端交互关系

### 7.1 API接口设计

| 服务 | 基础路径 | 核心接口 |
|------|---------|---------|
| 认证服务 | `/api/v1/auth` | POST /login, POST /device-login, POST /refresh |
| 用户服务 | `/api/v1/users` | GET /, GET /{id}, POST /, PUT /{id} |
| 门店服务 | `/api/v1/stores` | GET /, GET /{id}, POST /, PUT /{id} |
| 科室服务 | `/api/v1/departments` | GET /, GET /{id}, POST /, PUT /{id} |
| 儿童服务 | `/api/v1/children` | GET /, GET /{id}, POST /, PUT /{id}, POST /{id}/audit |
| 排班服务 | `/api/v1/schedules` | GET /calendar, POST /, GET /reserves, POST /reserves |
| 视力服务 | `/api/v1/vision` | GET /records, POST /records, GET /compare |
| 同步服务 | `/api/v1/sync` | POST /upload, POST /download/children, POST /callback |

### 7.2 前端与微服务的调用关系

| 前端应用 | 调用的后端服务 | 说明 |
|---------|-------------|------|
| **admin-web** | auth, user, store, child, schedule, vision, sync | 全量数据权限 |
| **store-web** | auth, user, store, child, schedule, vision, sync | 本店数据权限（含科室管理） |
| **parent-web** | auth, child, vision | 绑定子女数据权限 |
| **TV APK** | auth, child, sync | 设备认证 + 本店档案 + 同步 |

---

## 八、数据安全与隐私

### 8.1 敏感数据处理

| 数据类型 | 存储方式 | 说明 |
|---------|---------|------|
| 儿童姓名 | AES-256-GCM加密 | `name_encrypted` 字段存储密文 |
| 家长手机号 | AES-256-GCM + 脱敏 | `phone_encrypted` 存储密文，`phone_mask` 展示脱敏 |
| 身份证号 | AES-256-GCM加密 | `id_card_encrypted` 字段存储密文 |
| 用户密码 | BCrypt加密 | 不可逆哈希存储 |

### 8.2 数据隔离规则

| 角色 | 数据可见范围 |
|------|------------|
| 总部运营 | 全国所有医院、所有科室、所有数据 |
| 医院店长 | 本医院所有科室的数据 |
| 科室医生 | 本科室的排班、预约、检测数据 |
| 科室护士 | 本科室的排班、预约、检测数据（操作权限） |
| 家长用户 | 仅绑定子女数据 |
| TV设备 | 仅绑定医院的儿童档案 |

---

## 九、项目模块关系总结

```
┌─────────────────────────────────────────────────────────────────┐
│                        终端层 (Frontend + TV)                     │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ admin-web   │  │ store-web   │  │ parent-web  │            │
│  │ (Vue3)      │  │ (Vue3)      │  │ (Vue3)      │            │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘            │
│         │               │               │                    │
│         └───────────────┼───────────────┘                    │
│                         │                                    │
│                    ┌────┴────┐                              │
│                    │ API网关  │                              │
│                    └────┬────┘                              │
│                         │                                    │
├─────────────────────────┼────────────────────────────────────┤
│                         │      业务服务层 (Backend)            │
│  ┌──────────────────────┼──────────────────────┐              │
│  │                      │                      │              │
│  ▼                      ▼                      ▼              │
│ ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
│ │ auth    │  │ user    │  │ store   │  │ child   │        │
│ │ (认证)  │  │ (用户)  │  │ (门店)  │  │ (档案)  │        │
│ └────┬────┘  └─────────┘  └─────────┘  └────┬────┘        │
│      │                                      │               │
│      │    ┌─────────┐  ┌─────────┐  ┌──────┴────────┐     │
│      └───►│ schedule│  │ vision  │  │ sync        │     │
│           │ (排班)  │  │ (视力)  │  │ (TV同步)    │     │
│           └─────────┘  └─────────┘  └───────────────┘     │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                      数据层 (Database)                   │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐     │
│  │  MySQL  │  │  Redis  │  │   OSS   │  │ SQLite  │     │
│  │ (主库)  │  │ (缓存)  │  │(文件存储)│  │(TV本地) │     │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘     │
└─────────────────────────────────────────────────────────┘
```

---

## 十、附录：模块清单

### 10.1 后端模块

| 模块名 | 端口建议 | 说明 |
|--------|---------|------|
| careld-auth-service | 8081 | 认证授权 |
| careld-user-service | 8082 | 用户管理 |
| careld-store-service | 8083 | 门店/医院管理、科室管理 |
| careld-child-service | 8084 | 儿童档案 |
| careld-schedule-service | 8085 | 预约排班 |
| careld-vision-service | 8086 | 视力检测 |
| careld-sync-service | 8087 | TV同步 |
| careld-common | - | 公共模块（无独立端口） |

### 10.2 前端模块

| 模块名 | 端口建议 | 说明 |
|--------|---------|------|
| admin-web | 3001 | 运营中心（总部管理） |
| store-web | 3002 | 门店端（医院管理） |
| parent-web | 3003 | 家长端 |

### 10.3 数据库模块

| 数据库 | 说明 |
|--------|------|
| careld_vision (MySQL) | 主业务数据库 |
| careld_vision.db (SQLite) | TV端本地数据库 |

### 10.4 数据库表清单（医院管理）

| 表名 | 说明 | 归属模块 |
|------|------|---------|
| **sys_user** | 用户表（含医生、护士、技师） | careld-auth-service |
| **sys_role** | 角色表（含医生/护士/技师角色） | careld-auth-service |
| **sys_user_role** | 用户角色关联表 | careld-auth-service |
| **store_info** | 门店/医院表 | careld-store-service |
| **store_department** | 门店科室表 | careld-store-service |
| **store_tv_device** | TV设备表 | careld-store-service |
| **child_profile** | 儿童电子档案表 | careld-child-service |
| **schedule_info** | 养护排班表（关联科室和护士） | careld-schedule-service |
| **reserve_order** | 预约订单表 | careld-schedule-service |
| **vision_test_record** | 视力检测记录表 | careld-vision-service |
| **sync_log** | 数据同步日志表 | careld-sync-service |
| **sys_operation_log** | 系统操作日志表 | careld-auth-service |
