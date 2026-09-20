# store-web 数据中心新增「养护记录」卡片并统一列表倒序 Spec（第 99 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 后端列表排序参数扩展 + 前端版面改造 Spec |
| 来源 | 用户需求：http://39.162.49.28:5175/dashboard 底部左侧红框不变，右侧红框放「养护记录」，两个框中的内容都按倒序排列（编号顺延自拟为第 99 项） |
| 涉及端 | store-web（5175）`/dashboard` + careld-schedule-service（8285）预约列表 |
| 状态 | 已开发（2026-09-19，真机 DOM 断言 + API/DB 对账通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 99-1 | 左侧「最近预约」保持 | 卡片位置、列、跳转（查看全部 → `/schedule`）不变 |
| 99-2 | 右侧新增「养护记录」 | 与左卡并排（各占半行），列：日期 / 时段 / 儿童姓名 / 手机号码 / 养护人 / 状态；查看全部 → `/care-record`；取最近 5 条 |
| 99-3 | 两个列表倒序 | 均按时间倒序（最近的在前）：预约按预约日期+开始时间+ID 倒序；养护按养护日期+ID 倒序 |

---

## 2. 现状诊断

- 底部原为**单张全宽**「最近预约」卡片：`reserveApi.getReserveList({ storeId, page: 1, size: 5 })`。
- 后端 `ScheduleServiceImpl.listReserves` 固定升序（`reserve_date ASC, reserve_time_start ASC, id ASC`），因此仪表盘取到的是**最早**的 5 条（用户截图中显示 2026-09-12 的 5 条即此原因）；`/appointment-record` 预约记录页依赖该升序展示"最近及未来"，不能直接改为全局倒序。
- 该卡状态列映射为过期口径 `{0: 待服务, 1: 已完成, 2: 已取消}`，而实际状态为 1=已预约/2=养护中/3=已完成/4=已取消（爽约另以 `no_show_flag=1` 区分），导致真实数据渲染为「未知」（用户截图中 5 行全部「未知」）。
- 「养护记录」数据来自 `/care-records/page`（careld-vision-service），其 SQL 已为 `ORDER BY r.care_date DESC, r.id DESC`，天然倒序，无需后端改动。

---

## 3. 改造设计

### 3.1 后端（careld-schedule-service）

- `/api/v1/schedules/reserves` 新增可选参数 `orderDesc`（默认 false = 保持原升序，`/appointment-record` 等既有调用不受影响）；
- `ScheduleController.listReserves` → `ScheduleService.listReserves` → `ScheduleServiceImpl.listReserves` 三处透传；
- 实现：`orderDesc=true` 时 `orderByDesc(reserveDate, reserveTimeStart, id)`，否则维持原 `orderByAsc`。

### 3.2 前端（store-web）

1. `api/schedule.ts`：`getReserveList` 参数增加 `orderDesc?: boolean`。
2. `views/dashboard/index.vue`：
   - 底部改为 `el-row` + 两个 `el-col :span="12"`：左「最近预约」（原表结构不变）、右「养护记录」（`careRecordApi.getRecordPage({ storeId, page: 1, size: 5 })`，页面加载时并行拉取）；
   - 预约请求补 `orderDesc: true`；
   - 状态文案/颜色映射修正为应用统一口径 `1=已预约/2=养护中/3=已完成/4=已取消`，并增加 `noShowFlag === 1 → 已爽约`（对齐预约记录页）；
   - 养护记录状态列：`1=养护中（warning）/ 2=已完成（success）`，与养护记录页一致。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 手段 | 结果 |
|---|---|
| `vue-tsc --noEmit`（store-web） | ✅ EXIT=0 |
| 后端构建与重启 | ✅ `mvn package -DskipTests -q -pl careld-schedule-service -am` 成功；8285 重启（旧 PID 2332191 → 新 PID 2412144），日志 `Started ScheduleServiceApplication`、端口 LISTEN |
| API 对账·门店 7（预约倒序） | ✅ `orderDesc=true` 返回 id 110(10-11)/109(10-10)/108(10-09)/76(10-08)/107(10-07)，与 DB `ORDER BY reserve_date DESC, reserve_time_start DESC, id DESC` 前 5 完全一致 |
| API 对账·门店 7（默认升序未变） | ✅ 不带参数仍返回 19/24/37/39/40（2026-09-12 起最早 5 条），行为保持 |
| API 对账·养护记录 | ✅ `/care-records/page?storeId=7&size=5` 返回 id 53/52/51/50/48（09-19→09-18 倒序），与 DB 前 5 一致 |
| 真机 DOM 断言（5175 `/dashboard`，store001_mgr/门店 1） | ✅ 底部为两个 `el-col-12` 卡片：「最近预约」+「养护记录」（各带「查看全部」）；左表行序 2026-09-22 → 09-20 → 09-20 → 09-18 → 09-16（倒序，与 API 返回 id 86/85/89/72/71 一致）；状态列渲染为 已取消×4 + 已爽约（`no_show_flag=1`），不再出现「未知」；右表渲染 2026-09-15 11:59-12:00 张小明 赵医生 已完成（门店 1 仅 1 条养护记录），与 DB 一致 |
| 控制台 | ✅ 无新增错误（仅历史存在的 vue-router next() / el-radio label 弃用告警与 0×0 视口 ECharts 告警） |

---

## 5. 遗留与说明

1. 应用内浏览器为 0×0 隐藏视口，无法截图走查实际版面；本次以 DOM 结构/行序断言证明「双卡并排 + 倒序」，最终视觉效果以用户截图确认为准。
2. 顺带修正了左卡状态列的历史显示错误（旧映射 0/1/2 导致真实数据全部显示「未知」）。注：门店 1 存在一条种子脏数据 status=0（reserve_order id=4），任何映射下都会落到兜底文案，与本次改动无关。
3. 用户截图门店为门店 7「东湖中里社区卫生服务中心」：该账号（胡院长）密码非通用测试密码且不允许重置，故门店 7 采用 API/DB 对账，DOM 断言在门店 1（store001_mgr）完成。
4. 编号自拟：本项用户未给出编号，按序列记为 #99。
5. 与 #63–#98 同属未提交改动，等待确认后一并入库。
