# AGENTS.md — 给所有开发者与 AI 会话的关键约束

> 本文件列出**任何修改都不允许破坏**的关键不变量。改动相关文件前必须先读这里。
> 违反历史案例：2026-10-11 并行会话基于旧代码重写 AuthServiceImpl，静默删掉登录隔离代码，导致家长账号能登医生端（已修复，commit 1ab1038/f0ef50c）。

## 一、关键不变量（删了就是事故）

### 1. 登录端级角色隔离 — `backend/careld-auth-service/.../service/impl/AuthServiceImpl.java`
`/auth/login` 中「端级角色隔离」代码块**必须保留**：
- `client=doctor` 时拒绝 `userType != 2` 的 sys_user 账号（家长不能登医生端）
- `client=admin` 时拒绝 `userType == 3`（家长不能登管理端）
- 拒绝时抛 `BusinessException(1007, ...)`
- 同时医务 staff 匹配分支的逻辑（user/staff 同号撞号消解）不得改动语义

### 2. 前端 LoginRequest 的 client 字段
`frontend/store-web/src/types/index.ts`（及 admin-web 同名类型）的 `LoginRequest.client?: string` **必须保留**，三端登录请求都要带 `client`（admin/doctor）。此字段曾因并行改动被删导致类型报错 + 隔离失效。

### 3. GuardFilter 轻量鉴权 — schedule/child/vision/sync 四服务
`config/GuardFilter.java` + `*WebConfig.java`（JwtAuthFilter order=100 → GuardFilter order=200）不得删除。注意：
- GuardFilter 注册必须是 **@Bean 注入** FilterRegistrationBean，不能 `new GuardFilter()`（会导致 @Value jwt.secret 不注入、设备 token 401）
- sync 版额外放行 `isDevice=true` 的 TV 设备 token
- 前端/脚本对接这些服务时，无 token 请求会返回 401

### 4. 手机号脱敏/加密体系
儿童姓名/手机号走 ENC(...) 占位加密存储（AES，密钥 ENCRYPTION_KEY，必须 32 位）。任何新代码读写这两列必须走既有加解密工具，禁止明文直存直查。

## 二、严禁入库的内容

- `careld-certs/`（微信商户私钥等）已在 .gitignore——**严禁以任何形式提交或打包进仓库/镜像**
- `.env`、数据库密码、JWT 密钥、支付密钥不入仓库
- 共享盘仓库提交时排除 `.*/`、`.qoder/` 等垃圾文件

## 三、账号与密码纪律

- **严禁修改任何账号密码**（登录验证/测试遇到密码不匹配时排查原因，不要重置）
- 生产 root 密码、数据库密码由用户持有，不写入任何文档/代码

## 四、构建与运行

- 后端：Maven 多模块，先装父 POM 再打包子模块；版本号在根 pom + 8 个模块 pom 同步
- 前端：admin-web/store-web 为 element-plus（`npm run build`）；doctor-h5/parent-h5 为 uni-app（**`npm run build:h5`**，不是 `npm run build`）
- 本地启动：根目录 `start-all.sh`/`stop-all.sh`，JWT_SECRET 必须取自 backend/.env
- 端口：auth 8281 / user 8282 / store 8283 / child 8284 / schedule 8285 / vision 8286 / sync 8287 / notify 8288；前端 admin 5172 / store 5175 / doctor 5176 / parent 5177
- 生产服务器 1.7G 内存：**8 个 JVM 严禁同时启动**，分批（每批 2 个）等端口监听再拉下一批；swap 必须先于服务存在

## 五、数据库迁移

- 新迁移放 `database/mysql/migration/NN-xxx.sql`，编号递增（下一个从 40 开始）
- 生产执行任何迁移/DML 前必须先备份数据库（备份放共享盘 db-dumps/，不入仓库），先 dry-run 再执行
- 实体继承 BaseEntity 的表，迁移脚本必须含 created_by/updated_by 等审计列
- 新增字段后检查手写 @Select 列清单是否需要同步补列

## 六、协作流程

- 日常提交到 **dev** 分支；发布时 `git push origin dev:main` + 打 tag
- 改代码前先 pull；收工前小步提交并更新 `PROGRESS.md`
- 修复类任务先诊断汇报、确认后再修；生产库操作先给 SQL 等确认
- 并行工作（多人或多 AI 会话）时：重读文件再编辑，不要基于记忆中的旧版本改写整个文件
