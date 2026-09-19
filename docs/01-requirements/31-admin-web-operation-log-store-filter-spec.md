# 运营中心日志查询「按医院筛选」开发 Spec（第 76 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 功能增强 Spec |
| 来源 | 用户需求第 76 项：`http://39.162.49.28:5172/operation-log` 后台中，日志查询增加一项，按医院查询 |
| 涉及端 | admin-web（运营中心）；后端 careld-user-service（查询接口）+ careld-store-service（下拉数据） |
| 涉及页面 | admin-web：系统设置 → 日志记录（`/operation-log`） |
| 状态 | 已开发（2026-09-19，mvn 全量通过 + admin-web vue-tsc EXIT=0 + 真机 11 项核对通过） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 76-1 | 筛选条件 | admin-web 日志记录页新增「医院」下拉，位于筛选区首位 |
| 76-2 | 数据范围 | 下拉列出全部医院（含已禁用医院，否则其历史日志无法查询）；总部/运营中心/代理商按既有组织链数据权限过滤 |
| 76-3 | 服务端过滤 | 查询走服务端 `storeId` 精确过滤，非前端分页内筛选 |
| 76-4 | 隔离不破 | 门店用户即便传入他人 storeId，仍只能看到本店日志 |

---

## 2. 现状诊断（2026-09-19 核实）

| 项 | 现状 |
|---|---|
| 前端筛选区 | 原有：日志类型、模块、日期范围、关键词；表格已有「所属医院」列但**无医院筛选** |
| 表格列 | 「模块」「操作」两列直接渲染后端英文 key（如 `children`/`create`），可读性差 |
| 后端接口 | `GET /api/v1/operation-logs`（8282）**无 `storeId` 入参**；服务层写死 `DataScopeHelper.resolveStoreId(null)`，即只能按登录人身份解析范围 |
| Mapper | `selectPageWithStore` **已支持** `storeId` 过滤条件 —— 仅缺入参链路 |
| 代理 | admin-web `vite.config.ts` 已有 `/api/v1/operation-logs` → 8282、`/api/v1/stores` → 8283 规则 |

### 2.1 顺带发现的两个缺陷（本次一并修复）

**缺陷 A —— 模块下拉筛不出任何数据**
admin-web 模块下拉用的是中文选项（`用户管理`/`儿童档案`/…），而第 75 项改造后 `sys_operation_log.module` 落库的是英文 key（`users`/`children`/…），两者永不匹配。因该页此前无真实数据，问题一直未暴露。

**缺陷 B —— `/stores/all` 只返回启用医院**
实测 10 家医院中仅 3 家 `status=1`，而日志最多的「北京朝阳门店」（`status=0`，24 条）不在下拉中 → 无法按该院筛选。

**缺陷 C —— 历史行 module 取值不统一**
库中存在归一化脚本执行前写入的单数 key（`child`/`device`/`department`/`schedule`/`store`/`user` 共 7 行），与前端下拉的复数 key 不一致。

---

## 3. 改造设计

### 3.1 后端

| 文件 | 改动 |
|---|---|
| `careld-user-service` `OperationLogController.list` | 新增 `@RequestParam storeId`（`required=false`），透传 Service |
| `OperationLogService.pageLogs` | 签名前置新增 `Long storeId` |
| `OperationLogServiceImpl.pageLogs` | `DataScopeHelper.resolveStoreId(storeId)` 替换原 `resolveStoreId(null)` —— 超管/总部放行入参，门店用户被强制回本店 |
| `careld-store-service` `StoreController.all` | 新增 `@RequestParam includeDisabled`（默认 false），透传 Service |
| `StoreService/StoreServiceImpl.listAllStores` | 新增 `boolean includeDisabled`；为 true 时不再附加 `status=1` 条件 |

> `includeDisabled` 默认 false，**既有下拉调用方行为完全不变**；仅日志页显式传 true。

### 3.2 前端（admin-web）

| 文件 | 改动 |
|---|---|
| `src/types/index.ts` | `OperationLogQuery` 增 `storeId`；`OperationLog` 增 `description` |
| `src/api/store.ts` | `getAllStores(params?)` 支持透传 `includeDisabled` |
| `src/views/operation-log/index.vue` | ① 筛选区首位新增「医院」下拉（`filterable`+`clearable`，挂载时以 `includeDisabled:true` 加载）② **模块下拉 value 改为后端真实 key**，label 保持中文 ③ 新增「操作」下拉（action key 中文映射）④ 表格「模块」「操作」列改渲染中文（操作列优先展示 `description`）⑤ 详情弹窗同步中文化 ⑥ 重置清空 `storeId` |

### 3.3 数据修正

```sql
UPDATE sys_operation_log SET module='departments' WHERE module='department';
UPDATE sys_operation_log SET module='children'    WHERE module='child';
UPDATE sys_operation_log SET module='devices'     WHERE module='device';
UPDATE sys_operation_log SET module='schedules'   WHERE module='schedule';
UPDATE sys_operation_log SET module='stores'      WHERE module='store';
UPDATE sys_operation_log SET module='users'       WHERE module='user';
```
共 7 行（均为第 75 项验证期写入的测试数据）。修正后库内 11 个 module 取值与前端下拉完全一致。

---

## 4. 验证计划与结果（2026-09-19 真机）

**接口层**（curl 直连 8281/8282，hq 账号 `careld` 与门店账号 `store001_mgr` 实测）：

| # | 场景 | 期望 | 结果 |
|---|---|---|---|
| 1 | 总部 不传 storeId | 全部医院 | ✅ total=84，含 store 1/2/5/7 及无门店记录 |
| 2 | 总部 storeId=1 | 仅北京朝阳门店 | ✅ total=24，storeIds=[(1,北京朝阳门店)] |
| 3 | 总部 storeId=2 | 仅上海浦东门店 | ✅ total=1 |
| 4 | 总部 storeId=99999 | 空 | ✅ total=0 |
| 5 | 门店用户 不传 storeId | 仅本店 | ✅ total=24，store 1 |
| 6 | 门店用户 强行传 storeId=2 | **仍仅本店** | ✅ total=24，store 1（隔离未被绕过） |
| 7 | storeId=1 + module=children | 交集过滤 | ✅ total=5 |
| 8 | storeId=1 + keyword=预约 | 交集过滤 | ✅ total=6 |
| 9 | `/stores/all` 默认 | 3 家启用医院 | ✅ [(5,7,10)] |
| 10 | `/stores/all?includeDisabled=true` | 全部 10 家 | ✅ 含北京朝阳门店等禁用医院 |
| 11 | **页面实操**（浏览器 DOM 断言） | — | ✅ 见下 |

**页面层**（admin-web 5172 实操）：
- 筛选区渲染出「医院/日志类型/模块/操作/日期范围/关键词」6 项
- 医院下拉 10 家全部可选（含禁用医院）
- 选「北京朝阳门店」→ 共 24 条，仅该院；选「上海浦东门店」→ 共 1 条
- 「重置」→ 医院回「全部医院」，总数回 90 条
- 医院=北京朝阳门店 + 模块=儿童档案 → 共 5 条（与接口 [7] 一致）
- 模块=设备管理 → 命中归一化后的 `devices` 行（证明缺陷 A/C 已修）
- 表格中文列：`模块=儿童档案`、`操作内容=授权可约次数`
- 详情弹窗 12 个字段齐全，请求参数 JSON 正常（含脱敏）

**回归**：`vue-tsc --noEmit` EXIT=0；store-web 日志页（同一接口、不传 storeId）仍返回本店 24 条。

---

## 5. 遗留与说明

1. 医院下拉展示全部医院（含禁用），不做「仅显示有日志的医院」二次过滤 —— 若医院数量继续增长，可改为按 `sys_operation_log` 的 store_id 去重动态取值。
2. 总部/运营中心自身的操作日志无 `store_id`（如登录日志），仅在「全部医院」下可见，按具体医院筛选时不会出现。这是数据本身属性，非缺陷。
3. 模块下拉为前端静态枚举；若后续新增业务模块，需同步维护 `moduleOptions` 与 `OperationLogAspect.MODULE_NAMES`。
4. 与第 75 项同属未提交改动，一并等待确认后入库。
