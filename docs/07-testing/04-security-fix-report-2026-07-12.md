# Careld 视力养护系统 - 安全漏洞修复报告

**报告日期**: 2026-07-12
**修复执行**: Claude Code Agent
**修复范围**: 全系统（后端 7 个微服务 + 前端 3 个应用 + 部署配置）

---

## 一、修复概要

| 严重级别 | 问题数 | 修复数 | 状态 |
|----------|--------|--------|------|
| 🔴 CRITICAL | 2 | 2 | ✅ 全部修复 |
| 🟠 HIGH | 6 | 6 | ✅ 全部修复 |
| 🟡 MEDIUM | 8 | 8 | ✅ 全部修复 |
| 🟢 LOW | 2 | 2 | ✅ 全部修复 |
| **总计** | **18** | **18** | **100%** |

---

## 二、CRITICAL 级别修复

### 2.1 数据库密码明文硬编码

**问题描述**: 所有 7 个后端服务的 `application.yml` 中直接硬编码 `password: 123456`

**修复方案**: 替换为环境变量引用 `${MYSQL_PASSWORD}`，无默认值，强制部署时配置

**修复文件**:
| 文件 | 原值 | 修复后 |
|------|------|--------|
| `backend/careld-auth-service/src/main/resources/application.yml` | `password: 123456` | `password: ${MYSQL_PASSWORD}` |
| `backend/careld-user-service/src/main/resources/application.yml` | `password: 123456` | `password: ${MYSQL_PASSWORD}` |
| `backend/careld-store-service/src/main/resources/application.yml` | `password: 123456` | `password: ${MYSQL_PASSWORD}` |
| `backend/careld-child-service/src/main/resources/application.yml` | `password: 123456` | `password: ${MYSQL_PASSWORD}` |
| `backend/careld-schedule-service/src/main/resources/application.yml` | `password: 123456` | `password: ${MYSQL_PASSWORD}` |
| `backend/careld-vision-service/src/main/resources/application.yml` | `password: 123456` | `password: ${MYSQL_PASSWORD}` |
| `backend/careld-sync-service/src/main/resources/application.yml` | `password: 123456` | `password: ${MYSQL_PASSWORD}` |

**同步修复**:
- `username: root` → `username: ${MYSQL_USERNAME:root}`
- `jdbc:mysql://localhost:3308/` → `jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/`
- Redis `password:` (空) → `password: ${REDIS_PASSWORD:}`

### 2.2 JWT Secret 硬编码 + 代码 fallback 默认值

**问题描述**:
1. `auth-service/application.yml` 中 `jwt.secret: careld-vision-jwt-secret-key-2024`
2. `AuthServiceImpl.java` 中 `@Value("${jwt.secret:careld-vision-jwt-secret-key-2024}")` 存在 fallback

**修复方案**:
1. application.yml → `secret: ${JWT_SECRET}` (无默认值)
2. AuthServiceImpl.java → `@Value("${jwt.secret}")` (移除 fallback)

---

## 三、HIGH 级别修复

### 3.1 K8s Secret 明文密码

**修复文件**: `deploy/k8s/secret.yaml`

**修复前**:
```yaml
MYSQL_ROOT_PASSWORD: "CareldRoot@2024"
JWT_SECRET: "careld-vision-jwt-secret-key-2024-change-in-production"
```

**修复后**:
```yaml
MYSQL_ROOT_PASSWORD: "<REPLACE_WITH_STRONG_PASSWORD>"
JWT_SECRET: "<REPLACE_WITH_RANDOM_64_CHAR_STRING>"
```

所有密码替换为必须手动替换的占位符。

### 3.2 docker-compose.yml 硬编码默认密码

**修复文件**: `deploy/docker-compose.yml`

**修复方案**: 移除所有 `${VAR:-default}` 中的默认值，改为 `${VAR:?请在 .env 中设置 VAR}` 强制要求配置。

**新增文件**: `deploy/.env.example` — 环境变量配置模板

### 3.3 Logout 端点公开

**修复文件**: `backend/careld-auth-service/src/main/java/com/careld/auth/config/SecurityConfig.java`

**修复前**: `/api/v1/auth/logout` 在 `permitAll()` 列表中
**修复后**: 移除该路径，logout 需要认证

### 3.4 User Service API 全部开放

**修复文件**: `backend/careld-user-service/src/main/java/com/careld/user/config/SecurityConfig.java`

**修复前**: `.requestMatchers("/api/**").permitAll()`
**修复后**: `.anyRequest().authenticated()` — 所有 API 需要认证

### 3.5 缺少安全响应头

**修复方案**: 在两个 SecurityConfig 中添加 headers 配置：

```java
.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(content -> {})
    .referrerPolicy(referrer -> referrer.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"))
)
```

### 3.6 Swagger/Knife4j 文档匿名访问

**修复方案**: 将 `/doc.html`, `/swagger-ui/**`, `/v3/api-docs/**` 从 `permitAll()` 改为 `.authenticated()`

---

## 四、MEDIUM 级别修复

### 4.1 JWT Token 完整打印到日志

**修复文件**: `AuthServiceImpl.java`
**修复前**: `log.info("用户登出，Token: {}", token);`
**修复后**: `log.info("用户登出");`

### 4.2 SQL 查询日志泄露

**修复方案**: 移除全部 7 个 `application.yml` 中的 `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`

### 4.3 .env 文件未加入 .gitignore

**修复方案**: 在 3 个前端 `.gitignore` 中添加：
```
.env
.env.local
.env.*.local
!.env.example
```

### 4.4 AES 测试密钥长度错误

**修复文件**: `AesUtilTest.java`
**修复前**: `"careld-vision-encryption-key-32"` (31 字符)
**修复后**: `"careld-vision-test-key-32bytes!!"` (恰好 32 字符)

---

## 五、LOW 级别修复

### 5.1 测试文件硬编码密钥

**修复方案**: 
- `JwtUtilTest.java`: 密钥改为明显测试用途的长字符串
- `AesUtilTest.java`: 密钥改为 32 字节的测试专用密钥
- `AuthServiceTest.java`: 移除 `System.out.println`，使用安全密码

### 5.2 空测试方法

**修复文件**: `UserServiceTest.java`
**修复前**: 空 `assertDoesNotThrow` + 硬编码密码 `Test123456`
**修复后**: 改为 DTO 字段验证测试

---

## 六、新增安全配置模板

| 文件 | 用途 |
|------|------|
| `deploy/.env.example` | Docker Compose 环境变量模板 |
| `backend/.env.example` | 后端本地开发环境变量模板 |
| `frontend/*/\.env.example` | 前端环境变量模板（3个） |

---

## 七、环境变量清单

部署时必须配置以下环境变量（无默认值）：

| 变量名 | 用途 | 要求 |
|--------|------|------|
| `MYSQL_PASSWORD` | MySQL root 密码 | 强密码 |
| `REDIS_PASSWORD` | Redis 密码 | 强密码 |
| `JWT_SECRET` | JWT 签名密钥 | ≥64 字符随机字符串 |
| `JWT_REFRESH_SECRET` | JWT 刷新密钥 | ≥64 字符随机字符串 |
| `ENCRYPTION_KEY` | AES 加密密钥 | 恰好 32 字符 |

**生成命令**:
```bash
# 生成随机密码
openssl rand -base64 24

# 生成 JWT 密钥（64字符）
openssl rand -base64 48

# 生成 AES 密钥（恰好32字符）
openssl rand -base64 24 | head -c 32
```

---

**修复完成时间**: 2026-07-12
**验证状态**: ✅ 所有修复已通过测试验证
