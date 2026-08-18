# Careld 视力养护系统 - 本地环境访问信息

本文档记录本地开发环境各应用的访问地址、端口以及可用的测试账号。

> 最后更新：2026-07-16

---

## 一、基础设施

| 服务 | 地址 | 账号 / 备注 |
|------|------|-------------|
| MySQL | `localhost:3309` | 用户名：`root`，密码：`CareldDev@2026`，数据库：`careld_vision` |
| Redis | `localhost:6379` | 无密码（开发模式） |

> MySQL 容器名：`careld-mysql-dev`；Redis 容器名：`careld-redis-dev`。
> 使用 MySQL 5.7 镜像（本地已有），非生产指定的 8.0。

---

## 二、后端微服务

所有后端服务均运行在本机，端口段 8281–8287。

| 服务名 | 端口 | 说明 | 启动顺序 |
|--------|------|------|----------|
| careld-auth-service | `:8281` | 认证服务（JWT 签发 / 验证） | 1（必须先启动） |
| careld-user-service | `:8282` | 用户管理服务 | 2 |
| careld-store-service | `:8283` | 门店管理服务 | 2 |
| careld-child-service | `:8284` | 儿童档案服务 | 2 |
| careld-schedule-service | `:8285` | 预约排班服务 | 2 |
| careld-vision-service | `:8286` | 视力检测服务 | 2 |
| careld-sync-service | `:8287` | TV 同步服务（WebSocket） | 2 |

健康检查（部分服务 actuator 被安全策略拦截，返回 403 为正常现象）：

```
http://localhost:8281/actuator/health
http://localhost:8283/actuator/health
...
```

API 文档（Knife4j，启动后访问）：

```
http://localhost:8281/doc.html   （auth-service）
http://localhost:8282/doc.html   （user-service）
...
```

---

## 三、前端应用

前端通过 Vite 开发服务器运行，已配置反向代理转发 `/api/v1/*` 到对应后端服务。

| 应用 | 本地地址 | 说明 |
|------|----------|------|
| admin-web | http://localhost:5174/ | 运营中心（总部后台） |
| store-web | http://localhost:5175/ | 门店端（门店医护使用） |
| parent-web | http://localhost:5176/ | 家长端（家长用户使用） |

> 如果 5173–5175 端口被占用，Vite 会自动递增端口号，请关注终端输出。
>
> **IPv6 兼容**：Vite 已配置 `host: true`（双栈监听），`http://localhost:端口` 在 Chrome 等浏览器中可正常访问。如仍遇 `ERR_EMPTY_RESPONSE`，可改用 `http://127.0.0.1:端口/`。

---

## 四、测试账号

所有测试账号统一密码：**`Careld@2024`**

### 4.1 总部运营人员（user_type = 1，登录 admin-web）

| 用户名 | 姓名 | 角色 | 说明 |
|--------|------|------|------|
| `admin` | 超级管理员 | `super_admin` | 最高权限 |
| `ops_zhangsan` | 张三 | `ops_admin` | 运营管理员 |
| `ops_lisi` | 李四 | `ops_admin` | 运营管理员 |

### 4.2 门店医护（user_type = 2，登录 store-web）

| 用户名 | 姓名 | 门店 | 角色 |
|--------|------|------|------|
| `store001_mgr` | 王建国 | 北京朝阳门店 | `store_manager`（店长） |
| `store001_doc1` | 赵医生 | 北京朝阳门店 | `store_doctor` |
| `store001_doc2` | 孙护士 | 北京朝阳门店 | `store_doctor` |
| `store002_mgr` | 陈店长 | 上海浦东门店 | `store_manager` |
| `store002_doc1` | 刘医生 | 上海浦东门店 | `store_doctor` |
| `store003_mgr` | 林店长 | 广州天河门店 | `store_manager` |
| `store003_doc1` | 黄医生 | 广州天河门店 | `store_doctor` |

### 4.3 家长用户（user_type = 3，登录 parent-web）

| 用户名 | 姓名 | 手机号 |
|--------|------|--------|
| `parent_liu` | 刘妈妈 | 13600136001 |
| `parent_chen` | 陈爸爸 | 13600136002 |
| `parent_wang` | 王妈妈 | 13600136003 |
| `parent_zhao` | 赵爸爸 | 13600136004 |
| `parent_sun` | 孙妈妈 | 13600136005 |

---

## 五、登录接口

```
POST http://localhost:8281/api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Careld@2024"
}
```

返回字段中包含 `accessToken` 和 `refreshToken`，其他接口请求头带上：

```
Authorization: Bearer <accessToken>
```

---

## 六、常用运维命令

```bash
# 查看后端进程
ps -ef | grep 'careld-.*-service' | grep -v grep

# 查看前端进程
ps -ef | grep vite | grep -v grep

# 查看容器状态
docker ps --filter name=careld-

# 查看服务日志
tail -f logs/auth-service.log
tail -f logs/parent-web.log

# 停止所有后端服务
pkill -f 'careld-.*-service-.*.jar'

# 停止所有前端
pkill -f 'vite'

# 停止容器
docker rm -f careld-redis-dev careld-mysql-dev
```

---

## 七、注意事项

1. **启动顺序**：MySQL / Redis → auth-service → 其他后端服务 → 前端
2. **环境变量**：后端启动时需要设置 `MYSQL_PASSWORD`、`JWT_SECRET` 等环境变量，详见 `deploy/.env.example`
3. **日志位置**：所有服务日志统一输出到项目根目录的 `logs/` 文件夹
4. **端口冲突**：若端口被占用，请修改各服务 `application.yml` 或 Vite 配置
5. **测试数据**：初始化脚本位于 `deploy/init-scripts/`，包含完整的业务样例数据
