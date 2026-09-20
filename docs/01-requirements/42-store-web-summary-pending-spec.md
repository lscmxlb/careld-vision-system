# store-web 预约统计区新增「待养护数量」与改名 Spec（第 95 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 前端增项 + 统计字段扩展 Spec（store-web + careld-schedule-service） |
| 来源 | 用户需求第 95 项：统计区在「取消数」后新增「待养护数量」；「完成数」改名「已养护数」 |
| 涉及端 | store-web（5175）`/schedule` 页；careld-schedule-service（8285）`GET /api/v1/schedules/reserves/statistics` |
| 状态 | 已开发（2026-09-19，真机 UI 断言（含预约中/取消后两态）+ API/DB 对账 + 口径边界实测通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 95-1 | 新增「待养护数量」 | 位置在「取消数」之后（统计区第 4 项） |
| 95-2 | 文案调整 | 「完成数」→「已养护数」 |
| 95-3 | 口径确认 | 「待养护数量」= 仅状态为已预约(1)（用户明确选择，不含养护中(2)） |

---

## 2. 现状诊断

- 统计区数据来源 `GET /api/v1/schedules/reserves/statistics?storeId=&startDate=&endDate=`，
  SQL 在 `careld-schedule-service` 的 `ReserveOrderMapper.statisticsByDateRange`，按天分组返回
  `total / completed(3) / cancelled(4)` 三项。
- store-web `/schedule` 页汇总区为 3 列（`:span="8"`）：预约总数 / 完成数 / 取消数，
  前端对日期范围内每日返回值求和展示。

---

## 3. 改造设计

1. **后端**（`ReserveOrderMapper.statisticsByDateRange`）：
   追加 `SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS pending`（「待养护数」= 已预约(1)，与用户确认口径一致）；方法注释同步。
2. **前端类型**（`frontend/store-web/src/types/index.ts`）：
   `ReserveDailyStatistics` 增加 `pending: number`。
3. **前端页面**（`frontend/store-web/src/views/schedule/index.vue`）：
   - `summary` reactive 增加 `pending`，`fetchSummary` 对每日 `pending` 求和；
   - 模板改为 4 列（`:span="6"`），顺序：预约总数 / **已养护数**（success 绿）/ 取消数（danger 红）/ **待养护数量**（warning 橙）；
   - 样式新增 `.summary-item.warning { background: #fdf6ec; }`。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 手段 | 结果 |
|---|---|
| `vue-tsc --noEmit`（store-web） | ✅ EXIT=0 |
| 重建并重启 careld-schedule-service（2.1.0 jar） | ✅ 8285 端口新进程存活，接口返回含 `pending` |
| API/DB 对账（store 1，9 月） | ✅ 预约中临时单存在时 `total=10 / completed=1 / cancelled=8 / pending=1`；取消后 `10 / 1 / 9 / 0`，均与 DB 分组统计一致 |
| API/DB 对账（store 7，9 月） | ✅ 每日 `pending` 与 DB 一致（09-19:2、09-27:2、09-28:2、09-29:1，合计 7） |
| **口径边界实测**（可恢复变更） | ✅ 将 store 7 记录 id=80 的 `reserve_date` 由 2026-09-27 临时移至 2026-10-05：统计 `pending` 合计 **7→6**（09-27 当日 2→1）；恢复原值（含 `updated_at`）后回到 **7** |
| 真机 UI 断言（`http://39.162.49.28:5175/schedule`，store001_mgr） | ✅ 统计区依次显示「预约总数 10 / 已养护数 1 / 取消数 8 / 待养护数量 1」（预约中），取消后刷新为「10 / 1 / 9 / 0」，与 API/DB 一致 |

---

## 5. 遗留与说明

1. 「待养护数量」按用户确认的口径仅统计已预约(1)，不含养护中(2)；统计区四列仍为前端按天求和，未改动接口的日期入参语义。
2. 为验证非零展示创建的临时预约（id=89）已在验证后按正常业务流取消（详见 spec 41 遗留说明），store 1 九月的「取消数」因此 +1（8→9），属正常业务记录。
3. 与 #63–#94 同属未提交改动，等待确认后一并入库。
