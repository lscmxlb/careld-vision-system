# Careld 管理后台（admin-web）功能调整规格说明

> 基于现有系统功能审查后的修改需求整理  
> 日期：2026-08-08  
> 范围：admin-web 门店管理、设备管理、用户管理模块 + 后端对应微服务

---

## 一、需求总览

| 模块 | 变更类型 | 说明 |
|------|----------|------|
| 组织架构 | 结构重构 | 四级架构：总部 → 运营中心 → 代理商 → 医院(门店) |
| 门店管理 | 字段扩展 | 新增代理商关联、加盟时间、床位数、设备明细；业务负责人读取代理商 |
| 菜单调整 | 隐藏菜单 | 隐藏档案审核、排版监控、预约管理、视力记录、科室管理 |
| 设备类型 | 新增模块 | 独立的设备类型管理 |
| 设备管理 | 字段扩展 | 新增设备类型、设备ID（手动输入）、使用寿命（含到期预警） |
| 数据迁移 | 迁移脚本 | 现有测试数据迁移至新组织架构 |

---

## 二、组织架构（四级层级）

### 2.1 层级关系

```
总部（Brand HQ）
  └── 运营中心（Operations Center）— 多个
        └── 代理商（Agent）— 多个
              └── 医院/门店（Store）— 多个，每个门店只归属一个代理商
```

### 2.2 已确认规则
- 运营中心有**多个**（非单一）
- 一个门店**只能归属一个代理商**
- 门店的"业务负责人"**直接读取其所属代理商的负责人信息**（不在门店表冗余存储）
- 设备ID 为**手动输入**
- 使用寿命到期需要**提醒和预警**功能
- 现有测试数据**需要迁移**

---

## 三、总部（Brand HQ）

### 3.1 数据表 `brand_hq`
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| brand_name | VARCHAR(100) | 品牌/总部名称 |
| contact_name | VARCHAR(50) | 业务负责人姓名 |
| contact_phone | VARCHAR(20) | 业务负责人电话 |
| contact_email | VARCHAR(100) | 联系邮箱 |
| address | VARCHAR(255) | 总部地址 |
| status | TINYINT | 状态：0禁用 1启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted_at | DATETIME | 逻辑删除 |

### 3.2 说明
- 当前项目为单品牌（Careld可尔欧得），但表结构设计支持未来多品牌扩展
- 总部下管理多个运营中心

---

## 四、运营中心（Operations Center）

### 4.1 数据表 `ops_center`
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| center_code | VARCHAR(50) | 运营中心编码（唯一） |
| center_name | VARCHAR(100) | 运营中心名称 |
| hq_id | BIGINT | 所属总部ID（外键） |
| contact_name | VARCHAR(50) | 业务负责人姓名 |
| contact_phone | VARCHAR(20) | 业务负责人电话 |
| contact_email | VARCHAR(100) | 联系邮箱 |
| region | VARCHAR(255) | 负责区域 |
| status | TINYINT | 状态：0禁用 1启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted_at | DATETIME | 逻辑删除 |

---

## 五、代理商（Agent）

### 5.1 数据表 `agent`
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| agent_code | VARCHAR(50) | 代理商编码（唯一） |
| agent_name | VARCHAR(100) | 代理商名称 |
| center_id | BIGINT | 所属运营中心ID（外键） |
| contact_name | VARCHAR(50) | 业务负责人姓名 |
| contact_phone | VARCHAR(20) | 业务负责人电话 |
| contact_email | VARCHAR(100) | 联系邮箱 |
| region | VARCHAR(255) | 负责区域 |
| status | TINYINT | 状态：0禁用 1启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted_at | DATETIME | 逻辑删除 |

### 5.2 说明
- 代理商的业务负责人信息（contact_name / contact_phone）即为其下属门店的"业务负责人"
- 门店通过 `agent_id` 关联，前端展示时直接读取代理商的负责人信息

---

## 六、用户管理

### 6.1 用户表 `sys_user` 调整
新增字段：
- `hq_id` BIGINT — 所属总部
- `center_id` BIGINT — 所属运营中心
- `agent_id` BIGINT — 所属代理商

用户类型调整：
| user_type | 说明 |
|-----------|------|
| 1 | 总部人员 |
| 2 | 运营中心人员 |
| 3 | 代理商人员 |
| 4 | 门店人员（原门店店长/医生/护士合并或保留子类型） |
| 6 | 家长（保持不变） |

### 6.2 前端页面
- 总部管理页：总部信息 CRUD + 业务负责人
- 运营中心管理页：列表 + CRUD + 业务负责人（按总部筛选）
- 代理商管理页：列表 + CRUD + 业务负责人（按运营中心筛选）

---

## 七、门店管理模块（字段扩展）

### 7.1 现有 `store_info` 表新增字段
| 字段 | 类型 | 说明 |
|------|------|------|
| agent_id | BIGINT | 所属代理商ID（外键，一个门店只归属一个代理商） |
| join_date | DATE | 加盟时间 |
| bed_count | INT | 床位数 |

### 7.2 业务负责人（非冗余字段）
门店列表/详情中的"业务负责人"信息**不单独存储**在门店表，而是通过 `agent_id` 关联查询代理商的 `contact_name` / `contact_phone`。

### 7.3 设备明细
门店详情页展示该门店下的设备清单（关联设备管理模块），包含：
- 设备类型名称
- 设备ID（手动输入的序列号）
- 设备状态
- 使用寿命 / 到期日期

### 7.4 菜单隐藏项
以下菜单在 admin-web 中**全部隐藏**（不删除代码，仅前端菜单不展示）：
- ❌ 档案审核
- ❌ 排版监控
- ❌ 预约管理
- ❌ 视力记录
- ❌ 科室管理

---

## 八、设备类型管理（新增模块）

### 8.1 数据表 `device_type`
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| type_code | VARCHAR(50) | 类型编码（唯一） |
| type_name | VARCHAR(100) | 类型名称（如：视力检测仪、养护仪、TV终端） |
| description | VARCHAR(500) | 类型描述 |
| default_service_life | INT | 默认使用寿命（月） |
| status | TINYINT | 状态：0禁用 1启用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted_at | DATETIME | 逻辑删除 |

### 8.2 前端页面
- 设备类型列表页（CRUD）
- 字段：类型编码、类型名称、描述、默认使用寿命、状态
- 作为设备管理中"新增设备"时的下拉选择数据源

---

## 九、设备管理模块（字段扩展 + 到期预警）

### 9.1 现有 `store_tv_device` 表新增字段
| 字段 | 类型 | 说明 |
|------|------|------|
| device_type_id | BIGINT | 设备类型ID（外键） |
| device_sn | VARCHAR(100) | 设备ID/序列号（**手动输入**，唯一） |
| service_life_months | INT | 使用寿命（月） |
| install_date | DATE | 安装日期 |
| expire_date | DATE | 到期日期（= install_date + service_life_months） |
| warning_days | INT | 提前预警天数（默认30天） |

### 9.2 到期预警机制
| 状态 | 条件 | 说明 |
|------|------|------|
| 正常 | expire_date > 当前日期 + warning_days | 绿色标识 |
| 即将到期 | 当前日期 < expire_date <= 当前日期 + warning_days | 黄色预警 |
| 已到期 | expire_date <= 当前日期 | 红色告警 |

### 9.3 预警功能
- 设备列表页：到期状态列（颜色标识）
- 首页仪表盘：即将到期设备数量提醒
- 可选：到期前 N 天发送系统通知/消息提醒

### 9.4 设备列表展示
| 列名 | 说明 |
|------|------|
| 设备名称 | device_name |
| 设备类型 | 关联 device_type 表 |
| 设备ID | device_sn（手动输入） |
| 所属门店 | 关联 store_info |
| 使用寿命 | service_life_months |
| 安装日期 | install_date |
| 到期日期 | expire_date |
| 到期状态 | 正常/即将到期/已到期 |
| 设备状态 | 在线/离线/维修 |

---

## 十、后端接口变更清单

### 10.1 组织架构（careld-user-service :8282）
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/hq | GET/POST | 总部列表/新增 |
| /api/v1/hq/{id} | GET/PUT/DELETE | 总部详情/编辑/删除 |
| /api/v1/centers | GET/POST | 运营中心列表/新增（支持 hq_id 过滤） |
| /api/v1/centers/{id} | GET/PUT/DELETE | 运营中心详情/编辑/删除 |
| /api/v1/agents | GET/POST | 代理商列表/新增（支持 center_id 过滤） |
| /api/v1/agents/{id} | GET/PUT/DELETE | 代理商详情/编辑/删除 |

### 10.2 门店管理（careld-store-service :8283）
| 接口 | 变更 | 说明 |
|------|------|------|
| /api/v1/stores | 扩展字段 | 新增 agent_id、join_date、bed_count |
| /api/v1/stores/{id} | 扩展响应 | 详情中返回代理商业务负责人信息 |
| /api/v1/stores/{id}/devices | 新增 | 获取门店设备明细列表 |

### 10.3 设备管理（careld-store-service :8283）
| 接口 | 变更 | 说明 |
|------|------|------|
| /api/v1/devices | 扩展字段 | 新增 device_type_id、device_sn、service_life_months、expire_date |
| /api/v1/devices/expiring | 新增 | 获取即将到期/已到期设备列表 |
| /api/v1/device-types | GET/POST | 设备类型列表/新增 |
| /api/v1/device-types/{id} | GET/PUT/DELETE | 设备类型详情/编辑/删除 |

---

## 十一、前端菜单结构（调整后）

```
admin-web 菜单
├── 首页/仪表盘              ← 含设备到期预警数量
├── 用户管理
│   ├── 总部管理             ← 品牌总部
│   ├── 运营中心             ← 第二级
│   └── 代理商管理           ← 第三级
├── 门店管理
│   └── 门店列表             ← 含代理商关联、加盟时间、床位数、设备明细
├── 设备管理
│   ├── 设备类型             ← 新增
│   └── 设备列表             ← 含设备类型、设备ID、使用寿命、到期状态
└── （隐藏，不删除代码）
    ├── 档案审核
    ├── 排版监控
    ├── 预约管理
    ├── 视力记录
    └── 科室管理
```

---

## 十二、数据迁移方案

### 12.1 迁移步骤
1. 创建新表：`brand_hq`、`ops_center`、`agent`、`device_type`
2. 扩展现有表：`store_info`（新增 agent_id、join_date、bed_count）、`store_tv_device`（新增 device_type_id、device_sn、service_life_months 等）、`sys_user`（新增 hq_id、center_id、agent_id）
3. 插入默认总部数据（Careld 可尔欧得总部）
4. 创建运营中心并关联到总部
5. 创建代理商并关联到运营中心
6. 将现有 8 家门店分配到代理商
7. 将现有 15 个用户分配到对应层级
8. 插入默认设备类型数据
9. 为现有设备补充类型和序列号

### 12.2 现有测试数据映射

**总部**：
- Careld 可尔欧得总部（默认唯一）

**运营中心**（按区域划分，示例）：
| 运营中心 | 负责区域 |
|----------|----------|
| 华北运营中心 | 北京、武汉 |
| 华东运营中心 | 上海、杭州、南京 |
| 华南运营中心 | 广州、深圳 |
| 西南运营中心 | 成都 |

**代理商**（示例）：
| 代理商 | 所属运营中心 | 业务负责人 | 下属门店 |
|--------|-------------|-----------|----------|
| 北京朝阳代理 | 华北运营中心 | 张三 13800000002 | 北京朝阳门店 |
| 上海浦东代理 | 华东运营中心 | 李四 13800000003 | 上海浦东门店 |
| ... | ... | ... | ... |

**设备类型**（默认）：
| 类型编码 | 类型名称 | 默认寿命(月) |
|----------|----------|-------------|
| TV_TERMINAL | TV终端 | 60 |
| VISION_TEST | 视力检测仪 | 36 |
| CARE_DEVICE | 养护仪 | 48 |

---

## 十三、实施优先级

| 优先级 | 模块 | 依赖 | 说明 |
|--------|------|------|------|
| P0 | 数据库表结构变更 + 迁移脚本 | 无 | 所有功能的基础 |
| P0 | 后端：总部/运营中心/代理商 CRUD | P0-DB | 核心架构 |
| P1 | 后端：门店管理字段扩展 | P0 | 依赖代理商 |
| P1 | 后端：设备类型 CRUD | P0-DB | 独立模块 |
| P1 | 后端：设备管理字段扩展 + 到期预警 | P1-设备类型 | 依赖设备类型 |
| P2 | 前端：用户管理页面重构 | P0-后端 | 依赖后端接口 |
| P2 | 前端：门店管理页面扩展 | P1-门店后端 | 依赖后端接口 |
| P2 | 前端：设备管理 + 设备类型页面 | P1-设备后端 | 依赖后端接口 |
| P3 | 前端：菜单隐藏调整 | 无 | 独立可做 |
