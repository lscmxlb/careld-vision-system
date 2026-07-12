# 第三阶段：数据库设计

## 3.1 数据库架构

### 3.1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        数据存储架构                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    云数据中心 (MySQL)                      │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       │   │
│  │  │ 主库    │ │ 从库    │ │ 备份库  │ │ 统计库  │       │   │
│  │  │ Master  │ │ Slave   │ │ Backup  │ │ Report  │       │   │
│  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘       │   │
│  │       └───────────┴───────────┴───────────┘            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              │ 同步/备份                         │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    缓存层 (Redis)                        │   │
│  │  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐   │   │
│  │  │ 会话缓存      │ │ 数据缓存      │ │ 分布式锁      │   │   │
│  │  │ Session       │ │ Cache         │ │ Lock          │   │   │
│  │  └───────────────┘ └───────────────┘ └───────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              │ 增量同步                         │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    TV端本地 (SQLite)                    │   │
│  │  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐   │   │
│  │  │ 儿童简档      │ │ 视力记录      │ │ 同步日志      │   │   │
│  │  │ Child Cache   │ │ Vision Record │ │ Sync Log      │   │   │
│  │  └───────────────┘ └───────────────┘ └───────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.1.2 数据库选型

| 数据类型 | 存储方案 | 选型理由 |
|---------|---------|---------|
| 业务数据 | MySQL 8.0 | 成熟稳定，支持JSON，主从复制 |
| 缓存数据 | Redis 7.0 | 高性能，支持分布式锁、过期策略 |
| 文件存储 | MinIO/OSS | 对象存储，高可用，低成本 |
| TV本地数据 | SQLite | Android内置，零配置，轻量级 |
| 日志数据 | Elasticsearch | 可选，用于日志检索分析 |

---

## 3.2 MySQL数据库设计

### 3.2.1 数据库命名规范

```
数据库命名：careld_vision
表命名：小写下划线，模块前缀
  ├─ sys_ 系统模块
  ├─ store_ 门店模块
  ├─ child_ 儿童档案模块
  ├─ schedule_ 预约排班模块
  ├─ vision_ 视力检测模块
  ├─ sync_ 同步模块
  └─ stat_ 统计模块

字段命名：小写下划线
  ├─ 主键：id
  ├─ 外键：xxx_id
  ├─ 状态：xxx_status
  ├─ 时间：xxx_time / xxx_at
  └─ 布尔：is_xxx

索引命名：idx_表名_字段名
```

### 3.2.2 核心数据表

#### 表1: sys_user - 用户表

```sql
CREATE TABLE sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(64) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(128) COMMENT '邮箱',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    user_type TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型:1总部运营 2门店医护 3家长',
    store_id BIGINT UNSIGNED COMMENT '所属门店ID(门店医护)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) COMMENT '最后登录IP',
    login_fail_count INT DEFAULT 0 COMMENT '登录失败次数',
    lock_time DATETIME COMMENT '锁定时间',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    updated_by BIGINT UNSIGNED COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME COMMENT '删除时间(软删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_store_id (store_id),
    KEY idx_user_type (user_type),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

#### 表2: sys_role - 角色表

```sql
CREATE TABLE sys_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_desc VARCHAR(256) COMMENT '角色描述',
    user_type TINYINT NOT NULL COMMENT '适用用户类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 初始化角色数据
INSERT INTO sys_role (role_code, role_name, role_desc, user_type) VALUES
('super_admin', '超级管理员', '系统超级管理员', 1),
('ops_admin', '运营管理员', '总部运营人员', 1),
('store_manager', '门店店长', '门店管理人员', 2),
('store_doctor', '门店医生', '门店医生', 3),
('store_nurse', '门店护士', '门店护士', 4),
('store_technician', '门店技师', '门店技师', 5),
('parent', '家长', '家长用户', 6);
```

#### 表3: sys_user_role - 用户角色关联表

```sql
CREATE TABLE sys_user_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';
```

#### 表4: store_info - 门店表

```sql
CREATE TABLE store_info (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '门店ID',
    store_code VARCHAR(32) NOT NULL COMMENT '门店编码',
    store_name VARCHAR(128) NOT NULL COMMENT '门店名称',
    province_code VARCHAR(16) COMMENT '省份编码',
    province_name VARCHAR(64) COMMENT '省份名称',
    city_code VARCHAR(16) COMMENT '城市编码',
    city_name VARCHAR(64) COMMENT '城市名称',
    district_code VARCHAR(16) COMMENT '区县编码',
    district_name VARCHAR(64) COMMENT '区县名称',
    address VARCHAR(256) COMMENT '详细地址',
    longitude DECIMAL(10,7) COMMENT '经度',
    latitude DECIMAL(10,7) COMMENT '纬度',
    contact_name VARCHAR(64) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    business_hours VARCHAR(64) COMMENT '营业时间',
    network_type TINYINT DEFAULT 1 COMMENT '网络类型:1宽带 2 4G 3混合',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用 2暂停',
    open_time DATE COMMENT '开业时间',
    remark TEXT COMMENT '备注',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    updated_by BIGINT UNSIGNED COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_code (store_code),
    KEY idx_province (province_code),
    KEY idx_city (city_code),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店表';
```

#### 表5: store_tv_device - TV设备表

```sql
CREATE TABLE store_tv_device (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备唯一码',
    device_name VARCHAR(128) COMMENT '设备名称',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属门店ID',
    android_version VARCHAR(32) COMMENT 'Android系统版本',
    screen_resolution VARCHAR(32) COMMENT '屏幕分辨率',
    screen_size DECIMAL(4,1) COMMENT '屏幕尺寸(英寸)',
    app_version VARCHAR(32) COMMENT 'APK版本号',
    calibration_status TINYINT DEFAULT 0 COMMENT '校准状态:0未校准 1已校准',
    calibration_data JSON COMMENT '校准数据',
    last_online_time DATETIME COMMENT '最后在线时间',
    last_sync_time DATETIME COMMENT '最后同步时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    bind_time DATETIME COMMENT '绑定时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (device_code),
    KEY idx_store_id (store_id),
    KEY idx_status (status),
    KEY idx_last_online (last_online_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TV设备表';
```

#### 表6: child_profile - 儿童电子档案表

```sql
CREATE TABLE child_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '档案ID',
    child_code VARCHAR(32) COMMENT '档案编号',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '所属门店ID',
    -- 基本信息（加密存储）
    name_encrypted VARCHAR(256) NOT NULL COMMENT '姓名(加密)',
    name_mask VARCHAR(64) COMMENT '姓名脱敏显示',
    phone_encrypted VARCHAR(256) COMMENT '家长手机号(加密)',
    phone_mask VARCHAR(20) COMMENT '手机号脱敏显示',
    birth_date DATE COMMENT '出生日期',
    gender TINYINT COMMENT '性别:0女 1男',
    id_card_encrypted VARCHAR(512) COMMENT '身份证号(加密)',
    -- 眼部信息
    eye_condition TEXT COMMENT '眼部状况',
    medical_history TEXT COMMENT '病史',
    allergy_info TEXT COMMENT '过敏史',
    family_history TEXT COMMENT '家族眼病史',
    -- 审核状态
    audit_status TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态:0待审核 1已通过 2已驳回',
    audit_remark TEXT COMMENT '审核备注',
    audited_by BIGINT UNSIGNED COMMENT '审核人',
    audited_at DATETIME COMMENT '审核时间',
    -- 家长绑定
    parent_user_id BIGINT UNSIGNED COMMENT '绑定家长用户ID',
    -- 状态
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    updated_by BIGINT UNSIGNED COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_child_code (child_code),
    KEY idx_store_id (store_id),
    KEY idx_audit_status (audit_status),
    KEY idx_parent_user (parent_user_id),
    KEY idx_created_at (created_at),
    FULLTEXT KEY ft_name (name_mask)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='儿童电子档案表';
```

#### 表7: schedule_info - 养护排班表

```sql
CREATE TABLE schedule_info (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排班ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    schedule_date DATE NOT NULL COMMENT '排班日期',
    technician_id BIGINT UNSIGNED NOT NULL COMMENT '技师ID',
    technician_name VARCHAR(64) COMMENT '技师姓名',
    time_slot_start TIME NOT NULL COMMENT '时段开始',
    time_slot_end TIME NOT NULL COMMENT '时段结束',
    max_capacity INT NOT NULL DEFAULT 1 COMMENT '最大预约数',
    reserved_count INT NOT NULL DEFAULT 0 COMMENT '已预约数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0取消 1正常 2已满',
    remark TEXT COMMENT '备注',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_store_date (store_id, schedule_date),
    KEY idx_technician (technician_id),
    KEY idx_status (status),
    KEY idx_time_slot (time_slot_start, time_slot_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养护排班表';
```

#### 表8: reserve_order - 预约订单表

```sql
CREATE TABLE reserve_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    order_no VARCHAR(64) NOT NULL COMMENT '预约单号',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    schedule_id BIGINT UNSIGNED NOT NULL COMMENT '排班ID',
    child_id BIGINT UNSIGNED NOT NULL COMMENT '儿童档案ID',
    parent_name VARCHAR(64) COMMENT '家长姓名',
    parent_phone VARCHAR(20) COMMENT '家长电话',
    reserve_date DATE NOT NULL COMMENT '预约日期',
    reserve_time_start TIME COMMENT '预约开始时间',
    reserve_time_end TIME COMMENT '预约结束时间',
    reserve_type TINYINT DEFAULT 1 COMMENT '预约类型:1普通养护 2首次体验',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0待到店 1已到店 2服务中 3已完成 4已取消',
    source TINYINT DEFAULT 1 COMMENT '来源:1门店录入 2家长预约',
    remark TEXT COMMENT '备注',
    cancel_reason TEXT COMMENT '取消原因',
    cancelled_at DATETIME COMMENT '取消时间',
    completed_at DATETIME COMMENT '完成时间',
    created_by BIGINT UNSIGNED COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_store_id (store_id),
    KEY idx_child_id (child_id),
    KEY idx_schedule_id (schedule_id),
    KEY idx_status (status),
    KEY idx_reserve_date (reserve_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约订单表';
```

#### 表9: vision_test_record - 视力检测记录表

```sql
CREATE TABLE vision_test_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    record_code VARCHAR(64) COMMENT '记录编号',
    child_id BIGINT UNSIGNED NOT NULL COMMENT '儿童档案ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    device_id BIGINT UNSIGNED COMMENT 'TV设备ID',
    reserve_id BIGINT UNSIGNED COMMENT '关联预约ID',
    -- 检测信息
    test_type TINYINT NOT NULL COMMENT '检测类型:1养护前 2养护后',
    eye_type TINYINT NOT NULL COMMENT '眼睛:1左眼 2右眼',
    vision_level VARCHAR(10) NOT NULL COMMENT '视力值(如4.8)',
    vision_decimal DECIMAL(3,2) COMMENT '视力小数表示',
    -- 检测环境
    screen_size DECIMAL(4,1) COMMENT '屏幕尺寸(英寸)',
    test_distance DECIMAL(3,1) COMMENT '检测距离(米)',
    lighting_condition VARCHAR(64) COMMENT '光线条件',
    -- 检测人员
    tester_name VARCHAR(64) COMMENT '检测医护姓名',
    tester_id BIGINT UNSIGNED COMMENT '检测医护ID',
    -- 同步信息
    sync_source TINYINT DEFAULT 1 COMMENT '来源:1TV同步 2手动录入',
    device_local_id VARCHAR(64) COMMENT 'TV端本地记录ID',
    synced_at DATETIME COMMENT '同步时间',
    -- 备注
    remark TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_code (record_code),
    KEY idx_child_id (child_id),
    KEY idx_store_id (store_id),
    KEY idx_device_id (device_id),
    KEY idx_test_type (test_type),
    KEY idx_created_at (created_at),
    KEY idx_child_type_time (child_id, test_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视力检测记录表';
```

#### 表10: sync_log - 数据同步日志表

```sql
CREATE TABLE sync_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    device_id BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
    store_id BIGINT UNSIGNED NOT NULL COMMENT '门店ID',
    sync_type TINYINT NOT NULL COMMENT '同步类型:1上传 2下发',
    sync_batch_id VARCHAR(64) COMMENT '同步批次号',
    -- 记录统计
    record_count INT DEFAULT 0 COMMENT '记录总数',
    success_count INT DEFAULT 0 COMMENT '成功数',
    fail_count INT DEFAULT 0 COMMENT '失败数',
    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态:0进行中 1成功 2失败',
    -- 时间
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms INT COMMENT '耗时(毫秒)',
    -- 错误信息
    error_code VARCHAR(32) COMMENT '错误码',
    error_msg TEXT COMMENT '错误信息',
    -- 请求信息
    request_data JSON COMMENT '请求数据摘要',
    response_data JSON COMMENT '响应数据摘要',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_store_id (store_id),
    KEY idx_sync_type (sync_type),
    KEY idx_status (status),
    KEY idx_start_time (start_time),
    KEY idx_batch_id (sync_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据同步日志表';
```

#### 表11: sys_operation_log - 系统操作日志表

```sql
CREATE TABLE sys_operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    log_type TINYINT DEFAULT 1 COMMENT '日志类型:1操作 2登录 3异常',
    user_id BIGINT UNSIGNED COMMENT '操作用户ID',
    user_type TINYINT COMMENT '用户类型',
    user_name VARCHAR(64) COMMENT '用户名',
    store_id BIGINT UNSIGNED COMMENT '门店ID',
    -- 操作信息
    module VARCHAR(64) COMMENT '功能模块',
    action VARCHAR(64) COMMENT '操作类型',
    description VARCHAR(256) COMMENT '操作描述',
    request_method VARCHAR(16) COMMENT '请求方法',
    request_url VARCHAR(512) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_data TEXT COMMENT '响应数据',
    -- 客户端信息
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT 'User-Agent',
    device_type VARCHAR(32) COMMENT '设备类型',
    -- 执行信息
    execute_time INT COMMENT '执行时长(ms)',
    status TINYINT DEFAULT 1 COMMENT '状态:0失败 1成功',
    error_msg TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_store_id (store_id),
    KEY idx_module (module),
    KEY idx_created_at (created_at),
    KEY idx_log_type (log_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';
```

---

## 3.3 TV端SQLite数据库设计

### 3.3.1 数据库文件

```
/data/data/com.careld.vision/databases/
├── careld_vision.db          # 主数据库
└── careld_vision.db-journal  # 事务日志
```

### 3.3.2 表结构

#### 表1: local_child_profile - 本地儿童简档

```sql
CREATE TABLE local_child_profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    child_id TEXT NOT NULL,           -- 云端档案ID
    name TEXT NOT NULL,               -- 儿童姓名
    phone TEXT,                       -- 家长手机号（脱敏）
    birth_date TEXT,                  -- 出生日期 (YYYY-MM-DD)
    gender INTEGER,                   -- 性别：0女 1男
    medical_history TEXT,           -- 病史摘要
    sync_time INTEGER,                -- 最后同步时间戳(毫秒)
    cloud_updated_at INTEGER,         -- 云端更新时间戳
    created_at INTEGER DEFAULT (strftime('%s','now') * 1000)
);

-- 索引
CREATE INDEX idx_local_child_sync ON local_child_profile(sync_time);
CREATE INDEX idx_local_child_name ON local_child_profile(name);
```

#### 表2: local_vision_record - 本地视力检测记录

```sql
CREATE TABLE local_vision_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    local_id TEXT NOT NULL UNIQUE,    -- 本地唯一ID (UUID)
    child_id TEXT NOT NULL,           -- 儿童ID
    eye_type TEXT NOT NULL,           -- left/right
    vision_level TEXT NOT NULL,       -- 视力值（如4.8）
    test_time INTEGER NOT NULL,       -- 检测时间戳(毫秒)
    before_after TEXT NOT NULL,       -- before/after
    tester_name TEXT,                 -- 检测医护姓名
    remark TEXT,                      -- 备注
    -- 同步相关
    sync_status INTEGER DEFAULT 0,    -- 0未同步 1同步中 2已同步 3失败
    retry_count INTEGER DEFAULT 0,    -- 重试次数
    last_sync_time INTEGER,           -- 最后同步时间
    error_msg TEXT,                   -- 同步失败原因
    -- 云端回写
    cloud_record_id TEXT,             -- 云端记录ID（同步成功后）
    created_at INTEGER DEFAULT (strftime('%s','now') * 1000)
);

-- 索引
CREATE INDEX idx_vision_sync ON local_vision_record(sync_status);
CREATE INDEX idx_vision_child ON local_vision_record(child_id);
CREATE INDEX idx_vision_time ON local_vision_record(test_time);
```

#### 表3: local_sync_log - 本地同步日志

```sql
CREATE TABLE local_sync_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sync_type TEXT NOT NULL,          -- upload/download
    start_time INTEGER NOT NULL,      -- 开始时间戳
    end_time INTEGER,                 -- 结束时间戳
    record_count INTEGER DEFAULT 0,   -- 记录数
    success_count INTEGER DEFAULT 0,  -- 成功数
    fail_count INTEGER DEFAULT 0,     -- 失败数
    status INTEGER DEFAULT 0,         -- 0进行中 1成功 2失败
    error_msg TEXT,                   -- 错误信息
    created_at INTEGER DEFAULT (strftime('%s','now') * 1000)
);

-- 索引
CREATE INDEX idx_sync_time ON local_sync_log(start_time);
```

#### 表4: local_config - 本地配置表

```sql
CREATE TABLE local_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key TEXT NOT NULL UNIQUE,  -- 配置键
    config_value TEXT,                -- 配置值
    updated_at INTEGER DEFAULT (strftime('%s','now') * 1000)
);

-- 初始化配置
INSERT INTO local_config (config_key, config_value) VALUES
('store_id', ''),                   -- 门店ID
('device_code', ''),                -- 设备码
('last_sync_time', '0'),           -- 最后同步时间
('calibration_data', ''),          -- 校准数据
('app_version', '1.0.0'),          -- 应用版本
('vision_test_distance', '5.0'),   -- 检测距离(米)
('auto_sync_interval', '30');      -- 自动同步间隔(分钟)
```

---

## 3.4 数据库ER图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              数据库ER关系图                                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   sys_user   │         │  sys_role    │         │sys_user_role │
│──────────────│         │──────────────│         │──────────────│
│ id (PK)      │◄───────│ id (PK)      │         │ id (PK)      │
│ username     │         │ role_code    │         │ user_id (FK) │
│ password     │         │ role_name    │         │ role_id (FK) │
│ user_type    │         └──────────────┘         └──────────────┘
│ store_id(FK) │                │
└──────────────┘                │
       │                        │
       │    ┌───────────────────┘
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
│ ...          │         │ child_id(FK) │         │ device_id(FK)│
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

---

## 3.5 数据字典

### 3.5.1 通用状态枚举

| 状态名 | 值 | 说明 |
|--------|-----|------|
| **用户类型** | 1 | 总部运营 |
| | 2 | 门店医护 |
| | 3 | 家长 |
| **用户状态** | 0 | 禁用 |
| | 1 | 启用 |
| **门店状态** | 0 | 禁用 |
| | 1 | 启用 |
| | 2 | 暂停 |
| **档案审核状态** | 0 | 待审核 |
| | 1 | 已通过 |
| | 2 | 已驳回 |
| **预约状态** | 0 | 待到店 |
| | 1 | 已到店 |
| | 2 | 服务中 |
| | 3 | 已完成 |
| | 4 | 已取消 |
| **检测类型** | 1 | 养护前 |
| | 2 | 养护后 |
| **眼睛类型** | 1 | 左眼 |
| | 2 | 右眼 |
| **同步状态** | 0 | 未同步 |
| | 1 | 同步中 |
| | 2 | 已同步 |
| | 3 | 失败 |

### 3.5.2 视力等级对照表

| 对数视力 | 小数视力 | 描述 |
|---------|---------|------|
| 4.0 | 0.1 | 重度视力不良 |
| 4.1 | 0.12 | - |
| 4.2 | 0.15 | - |
| 4.3 | 0.2 | - |
| 4.4 | 0.25 | - |
| 4.5 | 0.3 | - |
| 4.6 | 0.4 | 中度视力不良 |
| 4.7 | 0.5 | - |
| 4.8 | 0.6 | - |
| 4.9 | 0.8 | 轻度视力不良 |
| 5.0 | 1.0 | 正常视力 |
| 5.1 | 1.2 | - |
| 5.2 | 1.5 | - |
| 5.3 | 2.0 | 超常视力 |

---

## 3.6 阶段交付物

1. **数据库ER图** - dbdiagram.io / PowerDesigner
2. **完整数据字典** - Excel文档
3. **MySQL建表SQL脚本** - `scripts/mysql/init.sql`
4. **TV端SQLite建表脚本** - `scripts/sqlite/init.sql`
5. **数据库迁移方案** - Flyway配置

---

## 3.7 数据库优化策略

### 索引优化
- 所有外键字段建立索引
- 常用查询条件字段建立索引
- 时间范围查询字段建立索引
- 避免过多索引影响写入性能

### 分表分库策略（未来）
- 视力检测记录按年分表
- 操作日志按月分表
- 用户数据按地区分库

### 读写分离
- 主库：写操作 + 实时读
- 从库：报表查询 + 数据分析
