# Careld 视力养护系统 - Claude Code Agent 开发规范

## 项目概述

Careld 可尔欧得视力养护服务信息系统，采用微服务架构，包含后端服务、前端应用和 TV 端 Android 应用。

## 技术栈

### 后端
- **语言**: Java 17+
- **框架**: Spring Boot 3.2.1
- **ORM**: MyBatis-Plus 3.5.5
- **构建工具**: Maven 3.6+
- **API 文档**: Knife4j 4.4.0 (OpenAPI 3)
- **工具库**: Hutool 5.8.23, Fastjson2 2.0.43, JJWT 0.12.3

### 前端
- **框架**: Vue 3.5.38
- **构建工具**: Vite 8.0.16
- **UI 组件库**: Element Plus 2.14.2
- **状态管理**: Pinia 3.0.4
- **路由**: Vue Router 5.1.0
- **图表**: ECharts 6.1.0
- **HTTP 客户端**: Axios 1.18.0
- **Node.js**: 22.18.0+ 或 24.12.0+

### 数据库与缓存
- **关系型数据库**: MySQL 8.0+
- **缓存**: Redis 7.2+
- **本地数据库** (TV端): SQLite

### 容器化
- **Docker**: 20.10+
- **Docker Compose**: 2.0+

## 端口规范

### 后端微服务端口（8281-8287）

所有 Java 微服务统一使用 8281-8287 端口段：

| 服务名 | 端口 | 功能描述 | 启动顺序 |
|--------|------|----------|----------|
| careld-auth-service | **8281** | 认证服务（JWT签发/验证） | 1 |
| careld-user-service | **8282** | 用户管理服务 | 2 |
| careld-store-service | **8283** | 门店管理服务 | 2 |
| careld-child-service | **8284** | 儿童档案服务 | 2 |
| careld-schedule-service | **8285** | 预约排班服务 | 2 |
| careld-vision-service | **8286** | 视力检测服务 | 2 |
| careld-sync-service | **8287** | TV同步服务（WebSocket） | 2 |

**注意**: 
- auth-service 必须首先启动，其他服务依赖它
- 端口配置在各服务的 `application.yml` 中

### 基础设施端口

| 服务 | 默认端口 | 说明 |
|------|----------|------|
| MySQL | 3306 | 主数据库 |
| Redis | 6379 | 缓存与会话管理 |
| Nginx | 80/443 | 反向代理（生产环境） |

### 前端开发端口（Vite 自动分配）

| 应用 | 默认端口 | 说明 |
|------|----------|------|
| parent-web | 5173 | 家长端 Web |
| admin-web | 5174 | 运营中心 Web |
| store-web | 5175 | 门店端 Web |

如果端口被占用，Vite 会自动递增。

## 环境配置规范

### 本地开发环境

#### 1. MySQL 数据库（推荐使用本地）

```yaml
# 本地 MySQL 配置（application.yml）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/careld_vision?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_local_password  # 使用本地密码
```

**本地数据库初始化**:
```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS careld_vision CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入初始化脚本
mysql -u root -p careld_vision < database/mysql/init.sql
```

#### 2. Redis（使用 Docker）

```bash
# 启动 Redis 容器
docker run -d \
  --name careld-redis \
  -p 6379:6379 \
  -v redis_data:/data \
  redis:7.2-alpine \
  redis-server --appendonly yes

# 如果使用密码
docker run -d \
  --name careld-redis \
  -p 6379:6379 \
  -e REDIS_PASSWORD=CareldRedis@2024 \
  -v redis_data:/data \
  redis:7.2-alpine \
  redis-server --appendonly yes --requirepass CareldRedis@2024
```

**Redis 本地配置（application.yml）**:
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:  # 本地开发可留空
    database: 0
```

#### 3. 完整 Docker Compose（可选）

如需完整容器化环境：
```bash
cd deploy
docker-compose up -d mysql redis
```

### 环境变量规范

后端服务通过环境变量配置（优先级高于 application.yml）：

```bash
# 通用配置
SPRING_PROFILES_ACTIVE=dev  # dev | test | prod
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT 配置
JWT_SECRET=careld-vision-jwt-secret-key-2024

# 服务间调用（Docker 环境）
AUTH_SERVICE_URL=http://localhost:8281
```

## 项目结构

```
careld-vision-system/
├── backend/                          # 后端微服务
│   ├── careld-common/               # 公共模块（工具类、常量、DTO）
│   ├── careld-auth-service/         # 认证服务 :8281
│   ├── careld-user-service/         # 用户服务 :8282
│   ├── careld-store-service/        # 门店服务 :8283
│   ├── careld-child-service/        # 儿童档案服务 :8284
│   ├── careld-schedule-service/     # 预约排班服务 :8285
│   ├── careld-vision-service/       # 视力检测服务 :8286
│   └── careld-sync-service/         # TV同步服务 :8287
├── frontend/                         # 前端应用
│   ├── admin-web/                   # 运营中心
│   ├── store-web/                   # 门店端
│   └── parent-web/                  # 家长端
├── tv-apk/                           # TV端 Android 应用
├── database/                         # 数据库脚本
│   ├── mysql/                       # MySQL 初始化脚本
│   └── sqlite/                      # TV端 SQLite 脚本
├── deploy/                           # 部署配置
│   └── docker-compose.yml           # Docker Compose 配置
└── docs/                             # 文档
```

## 开发规范

### 1. 后端开发

#### 端口配置
所有微服务端口必须在 `application.yml` 中配置：

```yaml
server:
  port: 8281  # 根据服务调整：8281-8287
```

#### 服务依赖配置
微服务间调用使用 Feign 或 RestTemplate，配置示例：

```yaml
# 在需要调用 auth-service 的服务中
auth:
  service:
    url: http://localhost:8281  # 开发环境
    # url: http://careld-auth-service:8281  # Docker环境
```

#### 代码规范
- 使用 Lombok 简化代码
- Controller 返回统一 Result 包装类
- 使用 MyBatis-Plus 的 LambdaQueryWrapper
- 逻辑删除字段：deletedAt（时间戳）
- 包结构：`com.careld.{module}`

#### 启动顺序
1. **必须**: 先启动 MySQL 和 Redis
2. **必须**: 先启动 careld-auth-service（其他服务依赖）
3. **可选**: 其他服务可并行启动

### 2. 前端开发

#### 安装依赖
```bash
cd frontend/{app-name}
npm install
```

#### 开发服务器
```bash
npm run dev
```

#### 构建
```bash
npm run build
```

#### API 调用规范
```typescript
// src/api/index.ts
import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8281',
  timeout: 10000,
})

// 请求拦截器添加 JWT Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

> **开发环境 baseURL 必须用 `/api/v1`（走 Vite 代理），不要写 `http://localhost:8281` 直连后端**——否则浏览器跨域拦截，且路径缺 `/api/v1` 会 404。生产再按需配置直连 + 后端 CORS。

#### 前后端接口契约规范（强制）
- **统一响应体**：后端所有接口返回 `Result<T>`（`{code,message,data,timestamp}`）；`code===200` 为成功。业务异常用 `BusinessException(code, message)`，由 `GlobalExceptionHandler` 返回 HTTP 200 + 错误码，**禁止**把业务错误当成 HTTP 4xx/5xx。
- **列表接口统一分页**：列表查询必须返回 `Result<PageResult<T>>`，带 `page/size` 参数，用 `PageResult.of(records, current, size, total)`。**禁止**返回裸 `Result<List<T>>`。参考 `UserController.listUsers`、`DeviceController.list`、`OperationLogController.list`。
- **前端响应拦截器**：`request.ts` 成功时 `return response.data.data`（返回内层 `data`，非完整 Result）。因此：
  - 单对象接口：直接 `const res = await xxxApi.get(); res.field`
  - 列表接口：`res.list` / `res.pagination.total`（**不得**写 `res.data.list`）
- **可选过滤参数**：列表接口的过滤参数（如 `storeId`、`childId`）必须 `required = false`；运营中心后台列表不传这些参数时应返回全量。强制必填会导致“Required request parameter”→ 被包成“系统繁忙”。

#### Vite 代理规范（强制）
- 每个 `/api/v1/<module>` 路径**必须有显式 proxy 规则**指向对应微服务（如 `/api/v1/operation-logs` → 8282、`/api/v1/statistics` → 8282）。
- 兜底规则 `'/api' → 8281(auth-service)` **仅作 fallback**，不可依赖它路由业务接口——否则业务请求被转给 auth-service，返回“No static resource”→ 被包成“系统繁忙”。
- 新增微服务/接口模块时，同步在 admin-web / parent-web / store-web 三端 `vite.config.ts` 的 proxy 中补规则。

### 3. 数据库规范

#### MySQL
- 字符集：utf8mb4
- 排序规则：utf8mb4_unicode_ci
- 时区：Asia/Shanghai
- 表名：小写+下划线（如 user_info）
- 字段名：小写+下划线（如 created_at）
- 主键：id (BIGINT)
- 必须字段：created_at, updated_at, deleted_at
- **审计列（强制）**：凡实体继承 `BaseEntity`（`backend/careld-common/.../entity/BaseEntity.java`）的表，必须建出全部五列 `created_by / updated_by / created_at / updated_at / deleted_at`。MyBatis-Plus 会自动映射 `createdBy/updatedBy`，且 `@TableLogic` 依赖 `deleted_at` 在 WHERE 拼接 `deleted_at IS NULL`。缺列会导致 `Unknown column` → 被全局异常包成“系统繁忙”。
- **逻辑删除配置（强制）**：每个微服务的 `application.yml` 必须配置 `mybatis-plus.global-config.db-config`：`logic-delete-field: deletedAt`、`logic-delete-value: "NOW()"`、`logic-not-delete-value: "NULL"`。缺失时 MP 用默认 `deleted_at=0` 去匹配，而 DATETIME 列存的是 NULL，导致查询返回空（看似接口正常但无数据）。新建微服务时务必从已有服务复制这段配置。
- **编码（防乱码）**：
  - 所有 init 脚本首行必须 `SET NAMES utf8mb4;`
  - 命令行导入必须带 `--default-character-set=utf8mb4`，否则 `docker exec mysql` 默认 `latin1` 客户端会把中文双重编码成乱码
  - JDBC URL 必须含 `characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true`
  - MySQL 容器 `character-set-server=utf8mb4`

#### Redis
- Key 命名规范：`careld:{module}:{business}:{id}`
- 示例：`careld:auth:token:{userId}`
- 默认数据库：0

### 4. Git 规范

#### 分支命名
- `main` - 生产分支
- `develop` - 开发分支
- `feature/{name}` - 功能分支
- `bugfix/{name}` - 修复分支
- `hotfix/{name}` - 紧急修复

#### Commit 规范
```
<type>(<scope>): <subject>

类型（type）：
- feat: 新功能
- fix: 修复 bug
- docs: 文档更新
- style: 代码格式
- refactor: 重构
- test: 测试
- chore: 构建/工具

示例：
feat(auth): 添加JWT刷新token功能
fix(user): 修复用户列表分页问题
```

## 快速启动指南

### 本地开发环境启动

```bash
# 1. 启动 MySQL（使用本地或 Docker）
# 方式一：使用本地 MySQL
mysql -u root -p
CREATE DATABASE careld_vision CHARACTER SET utf8mb4;

# 方式二：使用 Docker MySQL
cd deploy
docker-compose up -d mysql

# 2. 启动 Redis（Docker）
docker run -d --name careld-redis -p 6379:6379 redis:7.2-alpine

# 3. 启动认证服务
cd backend/careld-auth-service
mvn spring-boot:run

# 4. 启动其他服务（新终端）
cd backend/careld-user-service
mvn spring-boot:run

cd backend/careld-store-service
mvn spring-boot:run

# ... 其他服务

# 5. 启动前端（新终端）
cd frontend/parent-web
npm install
npm run dev
```

### 验证服务
```bash
# 检查后端服务
curl http://localhost:8281/actuator/health  # auth-service
curl http://localhost:8282/actuator/health  # user-service

# 检查 Redis
docker exec -it careld-redis redis-cli ping

# 前端访问
# 浏览器打开 http://localhost:5173
```

## 常见问题

### 端口冲突
如果遇到端口冲突：
1. 检查是否有服务已占用端口：`lsof -i :8281`
2. 修改 application.yml 中的端口配置
3. 或终止占用端口的进程

### 数据库连接失败
1. 确认 MySQL 已启动：`systemctl status mysql` 或 `docker ps`
2. 检查数据库配置（用户名/密码/端口）
3. 确认数据库已创建：`SHOW DATABASES;`

### Redis 连接失败
1. 确认 Redis 容器运行：`docker ps | grep redis`
2. 检查 Redis 配置（host/port/password）
3. 测试连接：`docker exec -it careld-redis redis-cli ping`

## 测试规范

### 后端测试
```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify -Pintegration-test
```

### 前端测试
```bash
# 运行测试
npm run test

# 运行 E2E 测试
npm run test:e2e
```

## 部署规范

### 开发环境
使用本地 MySQL + Docker Redis

### 生产环境
使用 Docker Compose 一键部署：
```bash
cd deploy
docker-compose up -d
```

## 安全规范

1. **敏感信息**: 不要在代码中硬编码密码，使用环境变量或配置文件
2. **JWT**: 生产环境必须更换 JWT_SECRET
3. **数据库**: 生产环境使用强密码，限制访问来源
4. **HTTPS**: 生产环境必须启用 HTTPS

## 参考文档

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [Vue 3 官方文档](https://vuejs.org/)
- [Element Plus 文档](https://element-plus.org/)
- [Docker 文档](https://docs.docker.com/)

---

**最后更新**: 2026-07-11
**维护者**: Careld 开发团队
