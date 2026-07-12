# Careld视力养护服务信息系统 - 后端开发文档

## 项目概述

这是一个四端（运营Web、门店Web、家长Web、安卓TV APK）视力养护管理系统的后端服务。

## 技术栈

- **Spring Boot 3.2.x**
- **Java 17**
- **MyBatis Plus 3.5.x**
- **MySQL 8.0**
- **Redis 7.x**
- **JWT认证**
- **AES-256-GCM加密**

## 微服务架构

```
careld-vision-system/
├── careld-common/              # 公共模块
├── careld-auth-service/        # 认证授权服务 (端口: 8081)
├── careld-user-service/        # 用户服务 (端口: 8082)
├── careld-store-service/       # 门店服务 (端口: 8083)
├── careld-child-service/       # 儿童档案服务 (端口: 8084)
├── careld-schedule-service/    # 预约排班服务 (端口: 8085)
├── careld-vision-service/      # 视力检测服务 (端口: 8086)
└── careld-sync-service/        # TV同步服务 (端口: 8087)
```

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis 7.x

### 2. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE careld_vision CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行建表脚本 (见 scripts/mysql/init.sql)
```

### 3. 构建项目

```bash
cd /root/.openclaw/workspace/projects/careld-vision-system/backend
mvn clean install -DskipTests
```

### 4. 启动服务

```bash
# 逐个启动服务
cd careld-auth-service && mvn spring-boot:run
cd careld-user-service && mvn spring-boot:run
# ... 其他服务
```

## API接口文档

启动服务后访问: http://localhost:8081/doc.html (Knife4j文档)

## 开发进度

| 服务 | 状态 | 完成度 |
|------|------|--------|
| careld-common | 已完成 | 100% |
| careld-auth-service | 已完成 | 100% |
| careld-user-service | 已完成 | 100% |
| careld-store-service | 已完成 | 100% |
| careld-child-service | 已完成 | 100% |
| careld-schedule-service | 已完成 | 100% |
| careld-vision-service | 已完成 | 100% |
| careld-sync-service | 已完成 | 100% |

## 接口说明

### 认证接口 (careld-auth-service)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户登录 | POST | /api/v1/auth/login | 用户名密码登录 |
| Token刷新 | POST | /api/v1/auth/refresh | 刷新访问令牌 |
| 用户登出 | POST | /api/v1/auth/logout | 用户登出 |
| TV设备登录 | POST | /api/v1/auth/device-login | TV设备登录 |

### 用户接口 (careld-user-service)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 当前用户 | GET | /api/v1/users/me | 获取当前用户信息 |
| 用户列表 | GET | /api/v1/users | 分页查询用户 |
| 创建用户 | POST | /api/v1/users | 创建新用户 |
| 更新用户 | PUT | /api/v1/users/{id} | 更新用户信息 |
| 重置密码 | POST | /api/v1/users/{id}/reset-password | 重置用户密码 |

### 门店接口 (careld-store-service)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 门店列表 | GET | /api/v1/stores | 查询门店列表 |
| 门店详情 | GET | /api/v1/stores/{id} | 获取门店详情 |
| 创建门店 | POST | /api/v1/stores | 创建门店 |
| 更新门店 | PUT | /api/v1/stores/{id} | 更新门店信息 |

### 儿童档案接口 (careld-child-service)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 档案列表 | GET | /api/v1/children | 查询儿童档案 |
| 档案详情 | GET | /api/v1/children/{id} | 获取档案详情 |
| 创建档案 | POST | /api/v1/children | 创建新档案 |
| 审核档案 | POST | /api/v1/children/{id}/audit | 审核档案 |
| TV搜索 | GET | /api/v1/children/search | TV端搜索档案 |

### 预约排班接口 (careld-schedule-service)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 排班日历 | GET | /api/v1/schedules/calendar | 获取排班日历 |
| 创建排班 | POST | /api/v1/schedules | 创建排班 |
| 预约列表 | GET | /api/v1/schedules/reserves | 查询预约列表 |
| 创建预约 | POST | /api/v1/schedules/reserves | 创建预约 |
| 取消预约 | POST | /api/v1/schedules/reserves/{id}/cancel | 取消预约 |

### 视力检测接口 (careld-vision-service)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 检测记录 | GET | /api/v1/vision/records | 查询检测记录 |
| 创建记录 | POST | /api/v1/vision/records | 录入检测记录 |
| 视力对比 | GET | /api/v1/vision/compare | 养护前后对比 |

### TV同步接口 (careld-sync-service)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 数据上传 | POST | /api/v1/sync/upload | TV上传检测数据 |
| 数据下发 | POST | /api/v1/sync/download/children | 下发儿童档案 |
| 同步配置 | GET | /api/v1/sync/config | 获取同步配置 |
| 回调通知 | POST | /api/v1/sync/callback | 同步状态回调 |

## 遇到的问题与解决方案

### 1. 数据加密方案

**问题**: 儿童隐私数据需要加密存储

**解决方案**: 使用AES-256-GCM算法加密敏感字段（姓名、手机号），脱敏展示。

### 2. TV离线同步

**问题**: TV设备可能离线，需要支持离线数据同步

**解决方案**: 
- TV端使用SQLite本地存储
- 设计增量同步协议
- 支持断点续传和冲突处理

### 3. 多服务数据一致性

**问题**: 微服务架构下数据一致性

**解决方案**: 
- 核心业务使用本地事务
- 非核心业务使用消息队列异步处理

## 单元测试

每个服务都包含单元测试，运行命令：

```bash
mvn test
```

## 配置文件

各服务的配置文件位于 `src/main/resources/application.yml`，包含：
- 数据库连接配置
- Redis配置
- JWT密钥配置
- 加密密钥配置

## 安全说明

1. 所有敏感数据使用AES-256-GCM加密
2. 密码使用BCrypt加密
3. JWT Token包含用户ID、用户类型、门店ID
4. TV设备需要绑定门店后才能使用

## 后续优化

1. 添加API网关统一入口
2. 实现服务注册与发现
3. 添加分布式事务支持
4. 完善日志和监控
5. 添加限流和熔断机制
