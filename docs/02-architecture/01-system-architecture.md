# 第二阶段：技术架构方案

## 2.1 整体技术架构设计

### 2.1.1 架构分层详解

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              终端接入层                                        │
├─────────────────┬─────────────────┬─────────────────┬─────────────────────────┤
│   运营中心Web    │    门店Web      │    家长Web      │     安卓TV APK          │
│   (Vue3+Element)│   (Vue3+Element)│  (Vue3+Element) │  (Android+SQLite)       │
│   PC端后台管理   │   PC端业务操作  │  响应式H5       │  大屏TV专用UI           │
└────────┬────────┴────────┬────────┴────────┬────────┴───────────┬─────────────┘
         │                 │                 │                    │
         └─────────────────┴─────────────────┴────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │         网关接入层            │
                    │  ┌─────────────────────┐     │
                    │  │    API Gateway      │     │
                    │  │  • 统一鉴权认证      │     │
                    │  │  • 请求限流          │     │
                    │  │  • HTTPS加密         │     │
                    │  │  • 路由转发          │     │
                    │  │  • 日志记录          │     │
                    │  └─────────────────────┘     │
                    └───────────────┬───────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │         业务服务层            │
                    │  ┌─────────────────────────┐  │
                    │  │    SpringBoot微服务      │  │
                    │  │  ┌─────────────────┐    │  │
                    │  │  │ 用户权限服务      │    │  │
                    │  │  │ • 账号管理       │    │  │
                    │  │  │ • 权限控制       │    │  │
                    │  │  │ • 角色管理       │    │  │
                    │  │  └─────────────────┘    │  │
                    │  │  ┌─────────────────┐    │  │
                    │  │  │ 档案服务         │    │  │
                    │  │  │ • 儿童建档       │    │  │
                    │  │  │ • 档案审核       │    │  │
                    │  │  │ • 档案查询       │    │  │
                    │  │  └─────────────────┘    │  │
                    │  │  ┌─────────────────┐    │  │
                    │  │  │ 预约排班服务      │    │  │
                    │  │  │ • 排班管理       │    │  │
                    │  │  │ • 预约管理       │    │  │
                    │  │  └─────────────────┘    │  │
                    │  │  ┌─────────────────┐    │  │
                    │  │  │ 视力检测服务      │    │  │
                    │  │  │ • 检测记录       │    │  │
                    │  │  │ • 视力对比       │    │  │
                    │  │  └─────────────────┘    │  │
                    │  │  ┌─────────────────┐    │  │
                    │  │  │ 数据统计服务      │    │  │
                    │  │  │ • 报表生成       │    │  │
                    │  │  │ • 数据分析       │    │  │
                    │  │  └─────────────────┘    │  │
                    │  │  ┌─────────────────┐    │  │
                    │  │  │ TV设备同步服务    │    │  │
                    │  │  │ • 离线数据接收   │    │  │
                    │  │  │ • 增量下发       │    │  │
                    │  │  │ • 同步状态管理   │    │  │
                    │  │  └─────────────────┘    │  │
                    │  └─────────────────────────┘  │
                    └───────────────┬───────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │         数据存储层            │
                    │  ┌─────────┬─────────┬──────┐ │
                    │  │  MySQL  │  Redis  │  OSS │ │
                    │  │  主库   │  缓存   │ 对象存储│ │
                    │  └─────────┴─────────┴──────┘ │
                    └───────────────────────────────┘
```

### 2.1.2 技术栈选型

#### 后端技术栈

| 组件 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 开发语言 | Java | 17 LTS | 长期支持版本 |
| 核心框架 | Spring Boot | 3.2.x | 微服务快速开发 |
| 安全框架 | Spring Security | 6.x | 认证授权 |
| ORM框架 | MyBatis Plus | 3.5.x | 简化数据库操作 |
| 数据库连接池 | HikariCP | 5.x | 高性能连接池 |
| 缓存 | Redis | 7.x | 分布式缓存 |
| 消息队列 | RabbitMQ | 3.12.x | 异步消息处理 |
| 搜索引擎 | Elasticsearch | 8.x | 档案搜索（可选） |
| 对象存储 | MinIO/阿里云OSS | - | 文件存储 |
| API文档 | Knife4j | 4.x | Swagger增强 |
| 监控 | Prometheus + Grafana | - | 系统监控 |

#### 前端技术栈

| 组件 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 开发框架 | Vue 3 | 3.4.x | 组合式API |
| 构建工具 | Vite | 5.x | 快速构建 |
| UI组件库 | Element Plus | 2.5.x | 后台管理组件 |
| 状态管理 | Pinia | 2.x | Vue官方推荐 |
| 路由 | Vue Router | 4.x | 路由管理 |
| HTTP客户端 | Axios | 1.6.x | API请求 |
| 图表库 | ECharts | 5.x | 数据可视化 |
| CSS预处理器 | SCSS | - | 样式管理 |

#### TV端技术栈

| 组件 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 开发语言 | Kotlin | 1.9.x | 现代Android开发 |
| 最低SDK | Android API 24 | Android 7.0 | 兼容主流电视 |
| 目标SDK | Android API 34 | Android 14 | 最新特性 |
| 本地数据库 | SQLite | - | Android内置 |
| 网络库 | Retrofit + OkHttp | 2.x | HTTP请求 |
| JSON解析 | Gson | - | JSON序列化 |
| 协程 | Kotlin Coroutines | - | 异步处理 |
| 依赖注入 | Hilt | 2.x | 依赖注入框架 |

---

## 2.2 服务拆分设计

### 2.2.1 微服务划分

```
careld-vision-system/
├── careld-gateway/              # API网关服务
│   ├── 统一认证入口
│   ├── 请求路由转发
│   ├── 限流熔断
│   └── 日志记录
│
├── careld-auth-service/         # 认证授权服务
│   ├── 用户登录/登出
│   ├── Token管理
│   ├── 权限校验
│   └── 角色管理
│
├── careld-user-service/         # 用户服务
│   ├── 总部运营账号管理
│   ├── 门店医护账号管理
│   ├── 家长账号管理
│   └── 账号绑定关系
│
├── careld-store-service/        # 门店服务
│   ├── 门店信息管理
│   ├── 门店账号分配
│   └── 门店配置管理
│
├── careld-child-service/        # 儿童档案服务
│   ├── 儿童档案CRUD
│   ├── 档案审核流程
│   ├── 档案查询检索
│   └── 档案数据加密
│
├── careld-schedule-service/     # 预约排班服务
│   ├── 技师排班管理
│   ├── 预约订单管理
│   ├── 排班日历查询
│   └── 预约提醒通知
│
├── careld-vision-service/       # 视力检测服务
│   ├── 检测记录管理
│   ├── 视力对比分析
│   ├── 历史记录查询
│   └── 检测报告生成
│
├── careld-sync-service/         # TV同步服务（核心）
│   ├── 离线数据接收
│   ├── 批量数据导入
│   ├── 增量数据下发
│   ├── 同步状态管理
│   └── 冲突检测处理
│
├── careld-statistics-service/   # 数据统计服务
│   ├── 门店客流统计
│   ├── 视力改善分析
│   ├── 养护频次报表
│   └── 数据导出服务
│
├── careld-device-service/       # TV设备服务
│   ├── 设备注册绑定
│   ├── 设备状态管理
│   ├── 设备远程控制
│   └── 设备日志收集
│
├── careld-file-service/         # 文件服务
│   ├── 文件上传下载
│   ├── 图片压缩处理
│   └── 文件存储管理
│
└── careld-system-service/        # 系统服务
    ├── 操作日志记录
    ├── 系统配置管理
    ├── 数据备份任务
    └── 定时任务调度
```

### 2.2.2 服务间通信

```
┌─────────────────────────────────────────────────────────────┐
│                      服务通信方式                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  同步通信（REST API）                                        │
│  ┌─────────┐    HTTP/REST    ┌─────────┐                   │
│  │ 网关    │ ──────────────> │ 各服务   │                   │
│  └─────────┘                 └─────────┘                   │
│                                                             │
│  异步通信（消息队列）                                         │
│  ┌─────────┐    发布消息     ┌──────────┐    消费消息      │
│  │ 服务A   │ ──────────────> │ RabbitMQ │ ───────────>     │
│  └─────────┘                 └──────────┘    ┌─────────┐   │
│                                            │ 服务B   │   │
│                                            └─────────┘   │
│                                                             │
│  使用场景：                                                  │
│  • 同步：实时查询、数据校验                                   │
│  • 异步：TV数据同步、通知推送、日志记录、统计计算              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2.3 跨端数据同步方案（核心）

### 2.3.1 同步架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         TV端（Android）                          │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    本地数据层（SQLite）                       │ │
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐    │ │
│  │  │ 儿童简档缓存   │  │ 视力检测记录   │  │ 同步日志      │    │ │
│  │  │ local_child   │  │ local_vision  │  │ local_sync    │    │ │
│  │  └───────────────┘  └───────────────┘  └───────────────┘    │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│                              ▼                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    同步管理层                                │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │ │
│  │  │ 网络监听  │  │ 数据打包  │  │ 冲突处理  │  │ 重试机制  │   │ │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                              │                                   │
└──────────────────────────────┼───────────────────────────────────┘
                               │ HTTPS
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                         云端（SpringBoot）                       │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    同步服务层                                │ │
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐    │ │
│  │  │ 数据接收API    │  │ 数据校验      │  │ 数据入库      │    │ │
│  │  │ /sync/upload   │  │ 去重/校验     │  │ MySQL写入     │    │ │
│  │  └───────────────┘  └───────────────┘  └───────────────┘    │ │
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐    │ │
│  │  │ 数据下发API    │  │ 增量计算      │  │ 同步确认      │    │ │
│  │  │ /sync/download │  │ 时间戳比对    │  │ 状态回调      │    │ │
│  │  └───────────────┘  └───────────────┘  └───────────────┘    │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3.2 同步协议设计

#### 上传协议（TV → 云端）

```json
{
  "protocol": "CARELD_SYNC_V1",
  "device_id": "TV_20240101_001",
  "store_id": "STORE_001",
  "timestamp": 1704067200000,
  "batch_id": "batch_001_20240101",
  "records": [
    {
      "record_type": "vision_test",
      "local_id": "local_001",
      "child_id": "CHILD_001",
      "eye_type": "left",
      "vision_level": "4.8",
      "test_time": "2024-01-01T10:00:00",
      "before_after": "before",
      "tester": "医护A",
      "remark": "室内光线良好"
    }
  ],
  "checksum": "md5_hash_here"
}
```

#### 下载协议（云端 → TV）

```json
{
  "protocol": "CARELD_SYNC_V1",
  "device_id": "TV_20240101_001",
  "store_id": "STORE_001",
  "timestamp": 1704067200000,
  "data_type": "child_profile",
  "records": [
    {
      "child_id": "CHILD_001",
      "name": "张三",
      "phone": "138****1234",
      "birth_date": "2015-01-01",
      "last_sync": 1703980800000
    }
  ],
  "has_more": false,
  "next_cursor": null
}
```

### 2.3.3 同步策略

| 策略项 | 说明 |
|--------|------|
| **触发时机** | 开机时、检测完成后、网络恢复时、手动触发、定时触发（每30分钟） |
| **同步方向** | 双向同步：TV上传检测数据，云端下发最新档案/排班 |
| **同步方式** | 增量同步，基于时间戳比对 |
| **数据打包** | 每批最多100条记录，超过分批传输 |
| **压缩传输** | 数据使用Gzip压缩，减少流量消耗 |
| **断点续传** | 支持批量传输中断后从断点继续 |
| **失败重试** | 指数退避：1min → 5min → 15min → 1h → 4h |
| **冲突处理** | 以TV端时间戳为准，云端去重（设备ID+本地ID） |
| **数据保留** | TV本地保留90天数据，云端永久保留 |

### 2.3.4 离线数据模型

#### TV端SQLite表结构

```sql
-- 本地儿童简档（仅本店）
CREATE TABLE local_child_profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    child_id VARCHAR(64) NOT NULL,        -- 云端档案ID
    name VARCHAR(64) NOT NULL,             -- 儿童姓名
    phone VARCHAR(20),                     -- 家长手机号（脱敏）
    birth_date DATE,                       -- 出生日期
    gender TINYINT,                        -- 性别：0女 1男
    medical_history TEXT,                -- 病史摘要
    sync_time BIGINT,                      -- 最后同步时间戳
    cloud_updated_at BIGINT,               -- 云端更新时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 本地未同步视力记录
CREATE TABLE local_vision_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    local_id VARCHAR(64) NOT NULL UNIQUE,  -- 本地唯一ID
    child_id VARCHAR(64) NOT NULL,         -- 儿童ID
    eye_type VARCHAR(10) NOT NULL,         -- 左眼left/右眼right
    vision_level VARCHAR(10) NOT NULL,     -- 视力值（如4.8）
    test_time TIMESTAMP NOT NULL,          -- 检测时间
    before_after VARCHAR(10) NOT NULL,     -- 养护前before/养护后after
    tester_name VARCHAR(64),               -- 检测医护姓名
    remark TEXT,                           -- 备注
    sync_status TINYINT DEFAULT 0,         -- 0未同步 1同步中 2已同步 3失败
    retry_count INT DEFAULT 0,             -- 重试次数
    last_sync_time BIGINT,                 -- 最后同步时间
    error_msg TEXT,                        -- 同步失败原因
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 本地同步日志
CREATE TABLE local_sync_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sync_type VARCHAR(20) NOT NULL,        -- upload/download
    start_time TIMESTAMP NOT NULL,         -- 开始时间
    end_time TIMESTAMP,                    -- 结束时间
    record_count INT DEFAULT 0,            -- 记录数
    success_count INT DEFAULT 0,           -- 成功数
    fail_count INT DEFAULT 0,              -- 失败数
    status TINYINT DEFAULT 0,              -- 0进行中 1成功 2失败
    error_msg TEXT,                        -- 错误信息
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引优化
CREATE INDEX idx_child_sync_time ON local_child_profile(sync_time);
CREATE INDEX idx_vision_sync_status ON local_vision_record(sync_status);
CREATE INDEX idx_vision_child_id ON local_vision_record(child_id);
```

---

## 2.4 安全架构

### 2.4.1 安全分层设计

```
┌─────────────────────────────────────────────────────────────────┐
│                      安全架构分层                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 1: 传输层安全                                             │
│  ├─ 全站HTTPS (TLS 1.3)                                          │
│  ├─ HSTS头部                                                     │
│  └─ 证书固定 (Certificate Pinning)                               │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 2: 网关层安全                                             │
│  ├─ API鉴权 (JWT Token)                                          │
│  ├─ 请求限流 (Rate Limiting)                                     │
│  ├─ IP白名单（TV设备）                                           │
│  ├─ CORS配置                                                     │
│  └─ DDoS防护                                                     │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 3: 应用层安全                                             │
│  ├─ RBAC权限控制                                                 │
│  ├─ 输入参数校验                                                 │
│  ├─ SQL注入防护 (MyBatis参数化)                                  │
│  ├─ XSS防护 (前端转义)                                           │
│  ├─ CSRF防护                                                     │
│  └─ 敏感操作二次确认                                             │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 4: 数据层安全                                             │
│  ├─ 儿童隐私数据AES-256加密                                      │
│  ├─ 密码BCrypt加密                                               │
│  ├─ 数据库连接加密                                               │
│  ├─ 敏感字段脱敏（日志/前端）                                    │
│  └─ 数据备份加密                                                 │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 5: 审计层安全                                             │
│  ├─ 全操作日志记录                                               │
│  ├─ 异常行为检测                                                 │
│  ├─ 登录失败锁定                                                 │
│  └─ 敏感操作审计                                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.4.2 数据加密方案

#### 敏感数据定义

| 数据类型 | 敏感级别 | 加密方式 |
|---------|---------|---------|
| 儿童姓名 | 高 | AES-256-GCM |
| 家长手机号 | 高 | AES-256-GCM + 脱敏展示 |
| 出生日期 | 中 | AES-256-GCM |
| 视力检测值 | 低 | 明文存储 |
| 检测记录 | 低 | 明文存储 |

#### 加密实现

```java
// 加密服务接口
public interface EncryptionService {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}

// AES-256-GCM实现
@Service
public class AesGcmEncryptionService implements EncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    @Value("${encryption.key}")
    private String secretKey;
    
    @Override
    public String encrypt(String plaintext) {
        // 生成随机IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);
        
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // IV + ciphertext + auth tag
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
        byteBuffer.put(iv);
        byteBuffer.put(encrypted);
        
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }
    
    // ... decrypt实现
}
```

### 2.4.3 TV设备安全

```
TV设备安全机制：

1. 设备注册
   └─ 首次使用输入门店编码 → 云端生成设备唯一码 → 绑定门店

2. 设备认证
   └─ 每次请求携带设备码 + Token → 云端校验设备-门店绑定关系

3. 数据隔离
   └─ TV仅能访问绑定门店的儿童档案
   └─ 跨门店访问被拒绝

4. 防篡改
   └─ APK签名校验
   └─ 关键配置云端下发

5. 日志上报
   └─ TV操作日志实时/批量上报云端
   └─ 异常操作告警
```

---

## 2.5 UI/UX设计规范

### 2.5.1 分端设计标准

| 终端 | 设计标准 | 适配要点 |
|------|---------|---------|
| 运营Web/门店Web | 1920×1080 PC端后台布局 | 侧边导航、内容区、固定头部 |
| 家长Web | 响应式 320px-1920px | 移动端优先、触摸友好 |
| TV APK | 1080P/4K 大屏适配 | 大字体、大按钮、遥控器操作 |

### 2.5.2 色彩规范

```css
/* 主色调 */
--primary-color: #1890FF;      /* 品牌蓝 */
--success-color: #52C41A;      /* 成功绿 */
--warning-color: #FAAD14;      /* 警告黄 */
--error-color: #F5222D;          /* 错误红 */

/* 中性色 */
--text-primary: #262626;       /* 主文本 */
--text-secondary: #595959;     /* 次要文本 */
--text-tertiary: #8C8C8C;      /* 辅助文本 */
--border-color: #D9D9D9;       /* 边框 */
--bg-color: #F5F5F5;           /* 背景 */

/* TV端专用（高对比度） */
--tv-text: #FFFFFF;            /* TV文字 */
--tv-bg: #1A1A1A;              /* TV背景 */
--tv-focus: #00D4FF;           /* TV焦点色 */
--tv-selected: #1890FF;        /* TV选中色 */
```

### 2.5.3 TV端交互规范

```
TV端设计原则：

1. 遥控器操作
   ├─ 方向键：上下左右移动焦点
   ├─ 确认键：选中/确认操作
   ├─ 返回键：返回上一级
   └─ 菜单键：呼出快捷菜单

2. 焦点管理
   ├─ 焦点元素高亮显示
   ├─ 焦点移动有视觉反馈
   └─ 焦点不丢失（循环聚焦）

3. 字体规范
   ├─ 标题：32sp-48sp
   ├─ 正文：24sp-28sp
   ├─ 辅助文字：20sp-24sp
   └─ 最小字号不小于18sp

4. 按钮规范
   ├─ 最小尺寸：120dp × 60dp
   ├─ 按钮间距：≥ 20dp
   └─ 焦点状态有明显区分

5. 视力表专用
   ├─ 全屏显示，无干扰元素
   ├─ 视标按物理尺寸计算
   ├─ 支持2.5m/5m检测距离切换
   └─ 左右眼切换有明确提示
```

---

## 2.6 阶段交付物

1. **《整体技术架构设计文档》** - 本文档
2. **TV安卓APK技术适配方案** - TV端详细技术方案
3. **离线同步技术方案** - 同步协议、数据模型
4. **四端高保真原型** - Figma/Axure原型
5. **UI视觉规范** - 色彩、字体、组件规范
6. **交互说明文档** - TV遥控器操作规范

---

## 2.7 技术风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| TV端屏幕适配复杂 | 高 | 建立多品牌电视测试矩阵，制定适配规范 |
| 离线同步数据冲突 | 高 | 设计完善的冲突检测和解决机制 |
| 视力表精度问题 | 高 | 屏幕校准模块，视标物理尺寸计算 |
| 4G网络不稳定 | 中 | 弱网优化，超时重试，本地缓存 |
| 数据安全合规 | 高 | 加密存储，权限隔离，操作审计 |
| 并发性能瓶颈 | 中 | 微服务拆分，Redis缓存，数据库优化 |
