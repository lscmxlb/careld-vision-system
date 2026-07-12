# 数据库结构概览

> 本文档仅包含各表字段结构与表关系，不含SQL代码。

---

## 一、MySQL 业务数据库表

### 1.1 用户权限模块

#### sys_user - 用户表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 用户ID（主键） |
| username | VARCHAR(64) | 登录用户名 |
| password | VARCHAR(128) | 密码（BCrypt加密） |
| real_name | VARCHAR(64) | 真实姓名 |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(128) | 邮箱 |
| avatar_url | VARCHAR(512) | 头像URL |
| user_type | TINYINT | 用户类型：1总部运营 / 2门店医护 / 3家长 |
| store_id | BIGINT UNSIGNED | 所属门店ID（门店医护） |
| status | TINYINT | 状态：0禁用 / 1启用 |
| last_login_time | DATETIME | 最后登录时间 |
| last_login_ip | VARCHAR(64) | 最后登录IP |
| login_fail_count | INT | 登录失败次数 |
| lock_time | DATETIME | 锁定时间 |
| created_by | BIGINT UNSIGNED | 创建人 |
| updated_by | BIGINT UNSIGNED | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| deleted_at | DATETIME | 删除时间（软删除） |

#### sys_role - 角色表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 角色ID（主键） |
| role_code | VARCHAR(64) | 角色编码 |
| role_name | VARCHAR(64) | 角色名称 |
| role_desc | VARCHAR(256) | 角色描述 |
| user_type | TINYINT | 适用用户类型 |
| status | TINYINT | 状态：0禁用 / 1启用 |
| sort_order | INT | 排序 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### sys_user_role - 用户角色关联表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | ID（主键） |
| user_id | BIGINT UNSIGNED | 用户ID |
| role_id | BIGINT UNSIGNED | 角色ID |
| created_at | DATETIME | 创建时间 |

---

### 1.2 门店模块

#### store_info - 门店表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 门店ID（主键） |
| store_code | VARCHAR(32) | 门店编码 |
| store_name | VARCHAR(128) | 门店名称 |
| province_code | VARCHAR(16) | 省份编码 |
| province_name | VARCHAR(64) | 省份名称 |
| city_code | VARCHAR(16) | 城市编码 |
| city_name | VARCHAR(64) | 城市名称 |
| district_code | VARCHAR(16) | 区县编码 |
| district_name | VARCHAR(64) | 区县名称 |
| address | VARCHAR(256) | 详细地址 |
| longitude | DECIMAL(10,7) | 经度 |
| latitude | DECIMAL(10,7) | 纬度 |
| contact_name | VARCHAR(64) | 联系人 |
| contact_phone | VARCHAR(20) | 联系电话 |
| business_hours | VARCHAR(64) | 营业时间 |
| network_type | TINYINT | 网络类型：1宽带 / 2 4G / 3混合 |
| status | TINYINT | 状态：0禁用 / 1启用 / 2暂停 |
| open_time | DATE | 开业时间 |
| remark | TEXT | 备注 |
| created_by | BIGINT UNSIGNED | 创建人 |
| updated_by | BIGINT UNSIGNED | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### store_tv_device - TV设备表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 设备ID（主键） |
| device_code | VARCHAR(64) | 设备唯一码 |
| device_name | VARCHAR(128) | 设备名称 |
| store_id | BIGINT UNSIGNED | 所属门店ID |
| android_version | VARCHAR(32) | Android系统版本 |
| screen_resolution | VARCHAR(32) | 屏幕分辨率 |
| screen_size | DECIMAL(4,1) | 屏幕尺寸（英寸） |
| app_version | VARCHAR(32) | APK版本号 |
| calibration_status | TINYINT | 校准状态：0未校准 / 1已校准 |
| calibration_data | JSON | 校准数据 |
| last_online_time | DATETIME | 最后在线时间 |
| last_sync_time | DATETIME | 最后同步时间 |
| status | TINYINT | 状态：0禁用 / 1启用 |
| bind_time | DATETIME | 绑定时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

### 1.3 儿童档案模块

#### child_profile - 儿童电子档案表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 档案ID（主键） |
| child_code | VARCHAR(32) | 档案编号 |
| store_id | BIGINT UNSIGNED | 所属门店ID |
| name_encrypted | VARCHAR(256) | 姓名（加密） |
| name_mask | VARCHAR(64) | 姓名脱敏显示 |
| phone_encrypted | VARCHAR(256) | 家长手机号（加密） |
| phone_mask | VARCHAR(20) | 手机号脱敏显示 |
| birth_date | DATE | 出生日期 |
| gender | TINYINT | 性别：0女 / 1男 |
| id_card_encrypted | VARCHAR(512) | 身份证号（加密） |
| eye_condition | TEXT | 眼部状况 |
| medical_history | TEXT | 病史 |
| allergy_info | TEXT | 过敏史 |
| family_history | TEXT | 家族眼病史 |
| audit_status | TINYINT | 审核状态：0待审核 / 1已通过 / 2已驳回 |
| audit_remark | TEXT | 审核备注 |
| audited_by | BIGINT UNSIGNED | 审核人 |
| audited_at | DATETIME | 审核时间 |
| parent_user_id | BIGINT UNSIGNED | 绑定家长用户ID |
| status | TINYINT | 状态：0禁用 / 1启用 |
| created_by | BIGINT UNSIGNED | 创建人 |
| updated_by | BIGINT UNSIGNED | 更新人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

### 1.4 预约排班模块

#### schedule_info - 养护排班表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 排班ID（主键） |
| store_id | BIGINT UNSIGNED | 门店ID |
| schedule_date | DATE | 排班日期 |
| technician_id | BIGINT UNSIGNED | 技师ID |
| technician_name | VARCHAR(64) | 技师姓名 |
| time_slot_start | TIME | 时段开始 |
| time_slot_end | TIME | 时段结束 |
| max_capacity | INT | 最大预约数 |
| reserved_count | INT | 已预约数 |
| status | TINYINT | 状态：0取消 / 1正常 / 2已满 |
| remark | TEXT | 备注 |
| created_by | BIGINT UNSIGNED | 创建人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### reserve_order - 预约订单表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 预约ID（主键） |
| order_no | VARCHAR(64) | 预约单号 |
| store_id | BIGINT UNSIGNED | 门店ID |
| schedule_id | BIGINT UNSIGNED | 排班ID |
| child_id | BIGINT UNSIGNED | 儿童档案ID |
| parent_name | VARCHAR(64) | 家长姓名 |
| parent_phone | VARCHAR(20) | 家长电话 |
| reserve_date | DATE | 预约日期 |
| reserve_time_start | TIME | 预约开始时间 |
| reserve_time_end | TIME | 预约结束时间 |
| reserve_type | TINYINT | 预约类型：1普通养护 / 2首次体验 |
| status | TINYINT | 状态：0待到店 / 1已到店 / 2服务中 / 3已完成 / 4已取消 |
| source | TINYINT | 来源：1门店录入 / 2家长预约 |
| remark | TEXT | 备注 |
| cancel_reason | TEXT | 取消原因 |
| cancelled_at | DATETIME | 取消时间 |
| completed_at | DATETIME | 完成时间 |
| created_by | BIGINT UNSIGNED | 创建人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

### 1.5 视力检测模块

#### vision_test_record - 视力检测记录表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 记录ID（主键） |
| record_code | VARCHAR(64) | 记录编号 |
| child_id | BIGINT UNSIGNED | 儿童档案ID |
| store_id | BIGINT UNSIGNED | 门店ID |
| device_id | BIGINT UNSIGNED | TV设备ID |
| reserve_id | BIGINT UNSIGNED | 关联预约ID |
| test_type | TINYINT | 检测类型：1养护前 / 2养护后 |
| eye_type | TINYINT | 眼睛：1左眼 / 2右眼 |
| vision_level | VARCHAR(10) | 视力值（如4.8） |
| vision_decimal | DECIMAL(3,2) | 视力小数表示 |
| screen_size | DECIMAL(4,1) | 屏幕尺寸（英寸） |
| test_distance | DECIMAL(3,1) | 检测距离（米） |
| lighting_condition | VARCHAR(64) | 光线条件 |
| tester_name | VARCHAR(64) | 检测医护姓名 |
| tester_id | BIGINT UNSIGNED | 检测医护ID |
| sync_source | TINYINT | 来源：1TV同步 / 2手动录入 |
| device_local_id | VARCHAR(64) | TV端本地记录ID |
| synced_at | DATETIME | 同步时间 |
| remark | TEXT | 备注 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

---

### 1.6 日志模块

#### sync_log - 数据同步日志表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 日志ID（主键） |
| device_id | BIGINT UNSIGNED | 设备ID |
| store_id | BIGINT UNSIGNED | 门店ID |
| sync_type | TINYINT | 同步类型：1上传 / 2下发 |
| sync_batch_id | VARCHAR(64) | 同步批次号 |
| record_count | INT | 记录总数 |
| success_count | INT | 成功数 |
| fail_count | INT | 失败数 |
| status | TINYINT | 状态：0进行中 / 1成功 / 2失败 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| duration_ms | INT | 耗时（毫秒） |
| error_code | VARCHAR(32) | 错误码 |
| error_msg | TEXT | 错误信息 |
| request_data | JSON | 请求数据摘要 |
| response_data | JSON | 响应数据摘要 |
| created_at | DATETIME | 创建时间 |

#### sys_operation_log - 系统操作日志表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | BIGINT UNSIGNED | 日志ID（主键） |
| log_type | TINYINT | 日志类型：1操作 / 2登录 / 3异常 |
| user_id | BIGINT UNSIGNED | 操作用户ID |
| user_type | TINYINT | 用户类型 |
| user_name | VARCHAR(64) | 用户名 |
| store_id | BIGINT UNSIGNED | 门店ID |
| module | VARCHAR(64) | 功能模块 |
| action | VARCHAR(64) | 操作类型 |
| description | VARCHAR(256) | 操作描述 |
| request_method | VARCHAR(16) | 请求方法 |
| request_url | VARCHAR(512) | 请求URL |
| request_params | TEXT | 请求参数 |
| response_data | TEXT | 响应数据 |
| ip_address | VARCHAR(64) | IP地址 |
| user_agent | VARCHAR(512) | User-Agent |
| device_type | VARCHAR(32) | 设备类型 |
| execute_time | INT | 执行时长（ms） |
| status | TINYINT | 状态：0失败 / 1成功 |
| error_msg | TEXT | 错误信息 |
| created_at | DATETIME | 创建时间 |

---

## 二、SQLite 本地数据库表（TV端）

#### local_child_profile - 本地儿童简档

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | INTEGER | ID（主键，自增） |
| child_id | TEXT | 云端档案ID |
| name | TEXT | 儿童姓名 |
| phone | TEXT | 家长手机号（脱敏） |
| birth_date | TEXT | 出生日期（YYYY-MM-DD） |
| gender | INTEGER | 性别：0女 / 1男 |
| medical_history | TEXT | 病史摘要 |
| sync_time | INTEGER | 最后同步时间戳（毫秒） |
| cloud_updated_at | INTEGER | 云端更新时间戳 |
| created_at | INTEGER | 创建时间戳 |

#### local_vision_record - 本地视力检测记录

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | INTEGER | ID（主键，自增） |
| local_id | TEXT | 本地唯一ID（UUID） |
| child_id | TEXT | 儿童ID |
| eye_type | TEXT | left / right |
| vision_level | TEXT | 视力值（如4.8） |
| test_time | INTEGER | 检测时间戳（毫秒） |
| before_after | TEXT | before / after |
| tester_name | TEXT | 检测医护姓名 |
| remark | TEXT | 备注 |
| sync_status | INTEGER | 0未同步 / 1同步中 / 2已同步 / 3失败 |
| retry_count | INTEGER | 重试次数 |
| last_sync_time | INTEGER | 最后同步时间 |
| error_msg | TEXT | 同步失败原因 |
| cloud_record_id | TEXT | 云端记录ID（同步成功后） |
| created_at | INTEGER | 创建时间戳 |

#### local_sync_log - 本地同步日志

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | INTEGER | ID（主键，自增） |
| sync_type | TEXT | upload / download |
| start_time | INTEGER | 开始时间戳 |
| end_time | INTEGER | 结束时间戳 |
| record_count | INTEGER | 记录数 |
| success_count | INTEGER | 成功数 |
| fail_count | INTEGER | 失败数 |
| status | INTEGER | 0进行中 / 1成功 / 2失败 |
| error_msg | TEXT | 错误信息 |
| created_at | INTEGER | 创建时间戳 |

#### local_config - 本地配置表

| 字段名 | 数据类型 | 说明 |
|--------|---------|------|
| id | INTEGER | ID（主键，自增） |
| config_key | TEXT | 配置键 |
| config_value | TEXT | 配置值 |
| updated_at | INTEGER | 更新时间戳 |

---

## 三、表关系图

### 3.1 MySQL 表关系

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   sys_user   │◄───────│  sys_role    │         │sys_user_role │
│──────────────│         │──────────────│         │──────────────│
│ id (PK)      │         │ id (PK)      │         │ id (PK)      │
│ username     │         │ role_code    │         │ user_id (FK) │
│ password     │         │ role_name    │         │ role_id (FK) │
│ user_type    │         └──────────────┘         └──────────────┘
│ store_id(FK) │                │
└──────────────┘                │
       │    ┌──────────────────┘
       │    │
       ▼    ▼
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│  store_info  │◄────────│store_tv_device│        │child_profile │
│──────────────│         │──────────────│        │──────────────│
│ id (PK)      │         │ id (PK)      │        │ id (PK)      │
│ store_code   │         │ device_code  │        │ store_id(FK) │
│ store_name   │         │ store_id(FK) │        │ name_encrypted│
│ ...          │         │ ...          │        │ audit_status │
└──────────────┘         └──────────────┘        │ parent_user_id│
       │                                         └──────────────┘
       │                                                │
       │    ┌────────────────────────────────────────────┘
       │    │
       ▼    ▼
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│schedule_info │◄────────│reserve_order │◄────────│vision_test_  │
│──────────────│         │──────────────│         │record        │
│ id (PK)      │         │ id (PK)      │         │──────────────│
│ store_id(FK) │         │ store_id(FK) │         │ id (PK)      │
│ schedule_date│         │ schedule_id  │         │ child_id(FK) │
│ ...          │         │ child_id(FK)│         │ device_id(FK)│
└──────────────┘         │ ...          │         │ ...          │
                         └──────────────┘         └──────────────┘
                                │
                                ▼
                         ┌──────────────┐
                         │   sync_log   │
                         │──────────────│
                         │ id (PK)      │
                         │ device_id(FK)│
                         │ ...          │
                         └──────────────┘
```

### 3.2 关系说明

| 主表 | 从表 | 关系说明 |
|------|------|---------|
| sys_user | sys_user_role | 一个用户可拥有多个角色 |
| sys_role | sys_user_role | 一个角色可分配给多个用户 |
| store_info | sys_user | 一个门店可拥有多个用户（门店医护） |
| store_info | store_tv_device | 一个门店可拥有多台TV设备 |
| store_info | child_profile | 一个门店可管理多个儿童档案 |
| store_info | schedule_info | 一个门店可设置多个排班 |
| store_info | reserve_order | 一个门店可产生多个预约 |
| store_info | vision_test_record | 一个门店可产生多条检测记录 |
| store_info | sync_log | 一个门店可产生多条同步日志 |
| store_info | sys_operation_log | 一个门店可产生多条操作日志 |
| store_tv_device | vision_test_record | 一台设备可产生多条检测记录 |
| store_tv_device | sync_log | 一台设备可产生多条同步日志 |
| child_profile | reserve_order | 一个儿童可产生多个预约 |
| child_profile | vision_test_record | 一个儿童可产生多条检测记录 |
| schedule_info | reserve_order | 一个排班可被多次预约 |
| reserve_order | vision_test_record | 一个预约可包含多条检测记录 |

---

## 四、状态枚举汇总

| 枚举名称 | 值 | 说明 |
|---------|-----|------|
| **用户类型** | 1 / 2 / 3 | 总部运营 / 门店医护 / 家长 |
| **用户状态** | 0 / 1 | 禁用 / 启用 |
| **门店状态** | 0 / 1 / 2 | 禁用 / 启用 / 暂停 |
| **档案审核状态** | 0 / 1 / 2 | 待审核 / 已通过 / 已驳回 |
| **预约状态** | 0 / 1 / 2 / 3 / 4 | 待到店 / 已到店 / 服务中 / 已完成 / 已取消 |
| **检测类型** | 1 / 2 | 养护前 / 养护后 |
| **眼睛类型** | 1 / 2 | 左眼 / 右眼 |
| **同步状态** | 0 / 1 / 2 / 3 | 未同步 / 同步中 / 已同步 / 失败 |
