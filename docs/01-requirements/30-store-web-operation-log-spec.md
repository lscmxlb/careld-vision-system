# 门店端日志记录（操作审计）开发 Spec（第 75 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 新功能开发 Spec |
| 来源 | 用户需求第 75 项：医院端后台「系统设置」新增「日志记录」，记录所有人员在何时做了何种操作（新建档案、审核档案、预约、服务取消等全部事件），供查询 |
| 涉及端 | 门店端 store-web（新增页面）；后端全部 7 个微服务（新增采集）+ careld-common（新增公共组件） |
| 涉及页面 | store-web：系统设置 → 日志记录（新增） |
| 状态 | 已完成并通过真机验证（2026-09-19） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 75-1 | 菜单入口 | store-web 左侧「系统设置」下新增「日志记录」 |
| 75-2 | 全事件采集 | 记录所有人员的业务操作：新建档案、审核档案、预约、取消预约/服务、开始/完成养护、授权次数、视力录入、排班与系统设置变更等 |
| 75-3 | 关键要素 | 每条日志须含：**操作人**（姓名）、**操作时间（精确到秒）**、**操作类型**（模块 + 动作）、**操作对象描述** |
| 75-4 | 查询 | 支持按日期范围、操作人、模块、操作类型、关键字检索；支持查看单条详情 |
| 75-5 | 数据隔离 | 门店只能看到**本店**人员的操作记录；总部（admin-web 既有入口）可见全部 |

---

## 2. 现状诊断（2026-09-19 代码与数据库核实）

| 项 | 现状 |
|---|---|
| 数据库 | `sys_operation_log` 表**已存在**（`backend/scripts/mysql/init.sql`），字段完备：`log_type`(1操作/2登录/3异常)、`user_id/user_type/user_name/store_id`、`module/action/description`、`request_method/url/params`、`ip_address/user_agent`、`execute_time`、`status/error_msg`、`created_at` |
| 实体/Mapper | `careld-user-service` 内有 `SysOperationLog` 实体与 `OperationLogMapper`（仅 2 个只读 `@Select`） |
| Service | `OperationLogServiceImpl` 只有 `pageLogs` / `getLogById` 两个读方法 |
| **采集侧** | **完全空缺** —— 全库检索无任何 `insert` 调用、无 `@Aspect` 拦截 controller。表中现有 10 行全部是 `created_at = 2026-08-08` 的初始化种子数据，**真实业务从未写入过日志** |
| 查询接口 | `GET /api/v1/operation-logs`（user-service 8282），权限 `@RequirePermission("operationlog:view")` |
| 前端 | admin-web 已有 `views/operation-log/index.vue`（总部侧）；**store-web 无任何日志页面** |
| 权限 | `sys_menu` id=7 `operationlog:view` **仅挂给 `hq_admin`**；`hospital_admin`（医院管理员 type=2）**无此权限** → 门店端直接调接口会 403 |
| 门店账号角色 | `store001_mgr` 等 3 个店长账号在 `sys_user_role` **完全没有角色行**，登录后 `permissions=[]`，任何 `@RequirePermission` 接口都会 403 |
| JWT 载荷 | 签发时未写入 `realName`，日志只能记到登录账号（`store001_mgr`）而非姓名 |
| 代理 | store-web `vite.config.ts` **已有** `/api/v1/operation-logs` → 8282 的 proxy 规则，无需新增 |
| 公共模块 | 7 个服务**全部依赖** `careld-common`；`CareldCommonAutoConfiguration` 用 `@Import` 注册切面 → 采集组件放 common 即可全服务生效 |
| 横切先例 | 仅 `careld-common/.../security/PermissionAspect.java` 一个切面可参照；`UserContext`(ThreadLocal) 提供 userId/userType/storeId/username |

### 2.1 关键结论
**基础设施（表/查询接口/admin-web 页面）早已齐备，唯独缺「谁在什么时候把日志写进去」这一环。** 本次开发的核心是补全采集链路 + 门店端查询页面 + 打通门店侧权限。

---

## 3. 改造设计

### 3.1 采集侧（careld-common，全服务自动生效）

新增 5 个文件到 `careld-common`：

| 组件 | 职责 |
|---|---|
| `log/OperationLog.java` | 业务语义注解：`module` / `action` / `description`（**直接写中文文案**，不做 SpEL 模板——运行期解析增加复杂度且无用例）/ `logType` / `recordParams` |
| `log/OperationLogAspect.java` | `@Around` 切面，两级拦截（见下） |
| `log/OperationLogWriter.java` | 异步落库：单线程守护线程池 + 有界队列(5000) + 丢弃最旧策略，**异常全吞**，日志写入失败绝不影响业务；`@PreDestroy` 优雅关停 |
| `entity/SysOperationLog.java` | 写入用实体（由 user-service 上移到 common，user-service 改为引用，避免重复定义） |
| `mapper/OperationLogWriteMapper.java` | 写入用 `BaseMapper`；`CareldCommonAutoConfiguration` 补 `@MapperScan("com.careld.common.mapper")` |

**两级拦截策略**（保证「所有事件」不遗漏）：

1. **精确级**：方法上标注了 `@OperationLog` → 采用注解声明的中文语义（如 `module=children, action=audit, description="审核儿童档案"`）。
2. **兜底级**：所有 `@RestController` 的写方法（POST/PUT/DELETE/PATCH）自动捕获 → 由 URL 段推导模块、HTTP 方法推导动作，生成通用中文描述（如「儿童档案 - 新增」）。**即使漏加注解也不会丢事件。**

切面 `@Order(1)`，**优先级高于 `PermissionAspect`**，因此被权限拦截的越权尝试同样入账（`status=0`）。

**排除项**（噪音控制）：
- GET/HEAD/OPTIONS 一律不记
- `/api/v1/auth/refresh`（令牌刷新，技术动作）
- `/api/v1/sync/*`（TV 设备自动同步，非人工操作）
- `/api/v1/operation-logs` 自身（避免读操作污染）

**记录内容**：`UserContext` 取 `userId/userType/storeId/realName`；`RequestContextHolder` 取 IP/UA/URL/参数；切面计时写入 `execute_time`；异常写入 `status=0` + `error_msg`。

**两项安全加固**（实现中发现并修复）：
1. **凭据脱敏**：`request_params` 对 `password/token/code/secret/...` 字段值一律替换为 `***`，并二次正则擦除裸 JWT；手机号沿用 `MaskUtil.maskPhone`。（初版曾把登录明文密码写进日志表，已修复并清洗存量数据）
2. **登录操作人补齐**：登录请求发 token 之前没有用户上下文，`user_id/user_name/store_id` 会是空。改为解析登录响应 `Result.data.user` 回填操作人；登录**失败**则保持匿名，仅记录尝试（`status=0` + 错误原因）。

### 3.2 事件覆盖清单（埋点）

共 70 处 `@OperationLog` 注解，覆盖 16 个控制器：

| 服务 | 覆盖事件 |
|---|---|
| auth(7) | 账号登录、登出、短信验证码登录、TV 设备登录、短信配置保存/测试 |
| child(7) | **新建档案**、**审核档案**、编辑档案、删除(隐藏)档案、恢复档案、状态流转、**授权可约次数** |
| schedule(15) | **创建预约**、**取消预约/服务**、**爽约标记**、**预约改期**、**开始养护**、**完成养护**、排班增删改、批量排班、排班规则增删改、预约规则保存 |
| vision(1) | 视力检测记录录入 |
| store(18) | 门店增改/启停、设备增改删/绑定/解绑/释放/校准、设备类型增删改、科室增删改/启停 |
| user(23) | 用户增删改/重置密码/启停/分配角色、角色增删改/授权、菜单增删改、组织架构增删改、医务人员增改/启停/重置密码 |

模块 key 统一采用 **URL 第 3 段**（`children`/`schedules`/`devices`/...），与兜底推导共用同一 key 空间，避免同一业务出现两种模块值；预约相关动作单独归入 `reserve` 子模块便于筛选。

> **已知盲区**：`schedule-service` 的 `ReserveAutoTask` 定时任务（自动完成、自动爽约）无 HTTP 入口，切面拦截不到，不会产生日志；如需记录需单独埋点。

### 3.3 查询侧（careld-user-service）

- `OperationLogController.list` 新增 `action`、`userName` 参数；`OperationLogService.pageLogs` 同步扩展。
- `OperationLogMapper.selectPageWithStore` 增加 `storeId` 过滤，Service 内用 `DataScopeHelper.resolveStoreId(null)` 强制门店隔离（type=2 门店用户被强制回本店；总部/运营中心不受限）。
- `selectDetailById` 同样加 `storeId` 条件，**跨店直接读详情返回 404「日志不存在或无权查看」**，防越权。
- **keyword 检索扩展**：原 LIKE 仅覆盖 `user_name / action / request_url`，增加 `description`（否则按「取消服务」等业务词搜不到）。

### 3.4 RBAC 与数据修复（本次实际改动）

| 改动 | 原因 |
|---|---|
| `sys_menu` id=7 由顶级目录改为挂在 id=8「系统设置」下，`menu_name` 改为「日志记录」，`sort_order=4` | 与需求「在系统设置中新增日志记录」一致；path 仍为 `/operation-log`，admin-web 既有路由不受影响 |
| `sys_role_menu` 补 `(10, 7)` | 使 `hospital_admin`（医院管理员 type=2）获得 `operationlog:view` |
| `sys_user_role` 补 `(10,10) (20,10) (30,10)` | `store001_mgr/store002_mgr/store003_mgr` 三个店长账号原本**没有任何角色**，登录后权限为空，无法通过任何权限校验；补挂医院管理员角色 |

### 3.5 前端（store-web）

| 文件 | 改动 |
|---|---|
| `src/api/log-record.ts` | 新增：列表 / 详情接口封装 |
| `src/views/log-record/index.vue` | 新增：筛选区（日期范围、操作人、日志类型、所属模块、操作类型、关键字）+ 表格（时间/操作人/类型/操作内容/接口/内容/IP/耗时/结果）+ 详情抽屉（含格式化 JSON 参数） |
| `src/types/index.ts` | 新增 `OperationLog` / `OperationLogQuery` |
| `src/router/index.ts` | 新增 `/log-record` 路由，`meta: { title: '日志记录', managerOnly: true }` |
| `src/layouts/MainLayout.vue` | 「系统设置」子菜单新增「日志记录」，`v-if="canViewLogs"` |
| `src/api/index.ts` | 导出新 API |

**契约对齐**：列表返回 `Result<PageResult<T>>`，响应拦截器已剥到内层 `data`，页面取 `res.list` / `res.pagination.total`（不写 `res.data.list`）；过滤参数全部 `required = false`。

**菜单可见性**：不沿用 `isManager`（userType===2）——门店的**医生/护士同样是 userType=2**，用它等于对所有门店人员放行。改为按后端真实权限 `operationlog:view` 控制，与接口 403 行为一致。

---

## 4. 验证结果（2026-09-19 真机实测）

| 项 | 结果 |
|---|---|
| 编译 | `mvn -o package -DskipTests` 全量 BUILD SUCCESS；store-web `vue-tsc --noEmit` 退出码 0 |
| 部署 | 7 个后端服务全部重启就绪（8281-8287） |
| **新建档案** | `[09:15:55] 王建国 \| children/create \| 新建儿童档案 \| status=1` |
| **审核档案** | `[09:16:05] 王建国 \| children/audit \| 审核儿童档案 \| status=1` |
| **预约** | `[09:16:39] 王建国 \| reserve/create \| 创建预约 \| status=1` |
| **服务取消** | `[09:16:48] 王建国 \| reserve/cancel \| 取消预约（取消服务） \| status=1` |
| 授权次数 | `[09:16:39] 王建国 \| children/grant \| 授权可约次数 \| status=1` |
| 视力录入 | `[09:16:39] 王建国 \| vision/create \| 录入视力检测记录 \| status=1` |
| 登录留痕 | `[09:21:04] 王建国 \| auth/login \| 用户登录 \| status=1`，`user_id/store_id` 已回填 |
| 失败留痕 | 失败请求记 `status=0` 并附错误原因（如「用户名或密码错误」「剩余可约次数不足」） |
| **越权留痕** | 医生账号调 `POST /api/v1/users` 被 403 拦截，仍写入 `[09:17:20] 赵医生 \| users/create \| 新建用户 \| status=0` |
| 凭据脱敏 | `request_params` 中 `"password":"***"`；存量明文密码已 SQL 清洗 |
| **门店隔离** | `store001_mgr`(store 1) 查询仅返回 `store_id=1` 的 14 条；`store002_mgr`(store 9) 返回 0 条 |
| **跨店越权** | `store002_mgr` 读 store 1 的日志详情 → 404「日志不存在或无权查看」 |
| **权限拒绝** | `store001_doc1`（无权限）调列表 → `无操作权限: operationlog:view` |
| 条件过滤 | 接口层与浏览器实测均通过：module=children→5 条、action=create→8 条、userName=王建国→12 条、2026-09-19→12 条、2026-09-18→0 条、keyword=取消服务→2 条 |
| **页面** | 浏览器打开 `http://39.162.49.28:5175/log-record`：菜单正常、列表 21 条、按操作人筛选 18→1 条、按模块筛选→5 条、重置恢复、详情抽屉字段与格式化 JSON 完整 |
| 菜单可见性 | 店长（有权限）可见「日志记录」；医生（无权限）菜单项隐藏 |

---

## 5. 遗留与说明

1. `ReserveAutoTask` 定时任务（自动完成/自动爽约）无 HTTP 入口，切面无法覆盖，本期不记录。
2. 采集为异步写入，极端情况（进程被强杀）可能丢失最后少量日志；不影响业务。
3. 日志表无清理策略，随业务增长会持续膨胀，后续如需可分页归档或定期清理（本期不做）。
4. `sys_operation_log` 表无 `deleted_at`/`created_by` 等审计列，故实体不继承 `BaseEntity`（与既有实现一致）。
5. `request_params` 内容在列表页不展示，仅在详情抽屉按需查看。
6. `DELETE /api/v1/medical-staff/{id}` 业务上恒定抛 400（医务人员只允许禁用不允许删除），前端无入口；若被直接调用会产生一条 `status=0` 的兜底日志，属预期留痕，未做排除。
7. 已登录用户需**重新登录**才会拿到新的 `operationlog:view` 权限（权限随 JWT 签发，有效期 2 小时）。
