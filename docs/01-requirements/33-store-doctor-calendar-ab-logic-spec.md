# 医院端/医生端日历 A/B 口径统一 Spec（第 80 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 逻辑口径调整 Spec（前端为主 + 后端查询补充 status 过滤） |
| 来源 | 用户需求第 80 项：医院端 `/schedule` 日历与医生端首页日历的 A/B 数据逻辑重新调整 —— A 为当天已经预约的数值，B 为当天可接受预约的总数值，B 的数值不受 A 的数值影响 |
| 涉及端 | store-web（医院端）、doctor-h5（医生端） |
| 涉及页面 | 医院端 - 预约管理 `/schedule`；医生端 - 工作台首页 `#/pages/home/index` |
| 前置 | 第 62 项（日历 A/B 首次实现）、第 73 项（B 定为固定值，仅医生端落地） |
| 状态 | 已开发（2026-09-19，vue-tsc EXIT=0 + 真机 DOM 双端比对 + 新增/取消预约实测 B 不受 A 影响） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 80-1 | A 的定义 | A = 当天已经预约的数值（已占用的预约名额数） |
| 80-2 | B 的定义 | B = 当天可接受预约的总数值（当天开放时段容量合计） |
| 80-3 | B 的独立性 | B 为固定值，不随 A 的增减而变化（预约/取消均只影响 A） |
| 80-4 | 双端一致 | 医院端日历与医生端首页日历对同一天展示相同的 A/B |

---

## 2. 现状诊断

需求提出时，两端日历"原则上"已是 A=已约 / B=容量，但存在三处口径分裂，导致两端可能显示不同值：

| # | 问题 | 端 | 详情 |
|---|---|---|---|
| 1 | A 的数据源不同 | store-web | A 取自 `reserve_order` 统计（total − cancelled，`/schedules/reserves/statistics`）；doctor-h5 取自 `schedule_slot` 的 Σ已约（`/schedule-rules/slot-daily-summary`）。两个源在测试数据下已出现不一致：门店 7 / 2026-09-14，订单统计为 5，时段 Σ已约 为 4（该日存在容量 2 的时段被约入 5 单的异常数据） |
| 2 | A=0 的显示不同 | doctor-h5 | 通过 `abNum()` 将 0 渲染为 `-`，出现 `-/6`；store-web 显示 `0/6` |
| 3 | B 口径不严谨 | store-web | 日历 B 由"每月逐日拉取 `/schedule-rules/slots` 后前端求和"得到，且该接口未过滤 `status=1`，会把停用时段容量计入 B；同时每月要发 ~30 次逐日请求，`loadedMonths` 缓存遇到请求失败会永久留下 `-/-` |

另注：第 73 项仅将 B 定为固定值落地在医生端；医院端 B 虽由时段容量求和得到、结构上也不随 A 变化，但与 A 异源、口径不统一，未达到"双端同口径"。

---

## 3. 改造设计

### 3.1 统一数据源：`GET /api/v1/schedule-rules/slot-daily-summary`

两端日历统一改为请求该接口按月取数：

```sql
SELECT slot_date AS slotDate, SUM(booked_count) AS booked, SUM(max_capacity) AS total
FROM schedule_slot
WHERE store_id = ? AND slot_date BETWEEN ? AND ? AND status = 1
GROUP BY slot_date ORDER BY slot_date
```

- **A = `booked`**（Σ开放时段 `booked_count`），即当天已占用名额数；
- **B = `total`**（Σ开放时段 `max_capacity`），为排班表中的固定容量，仅排班规则变更时改变；
- B 与 A 的结构独立性：全链路只有 `incrementBooked` / `decrementBooked` 会改动 `booked_count`（预约/取消），`max_capacity` 从不随预约变化，故 B 天然不受 A 影响；
- 无排班（或全部时段停用）的日期不在结果集中。

### 3.2 store-web `src/views/schedule/index.vue`

- 移除日历对 `reserveApi.getStatistics` 的依赖（该接口保留给顶部"预约查询汇总"卡片使用）；移除逐日 `getSlots` 的 ~30 次请求及 `loadedMonths` / `dayRemainMap` / `statMap` / `loadDaySlotStats`。
- 新增 `dayBookedMap`（A）、`dayCapacityMap`（B）、`dayFullMap`（满员态），均由 `fetchCalendar()` 一次性取回填充；月切换时只需一次请求。
- 单元格显示：`capacity === 0 ? '-/-' : `${booked}/${capacity}``；A=0 时显示 `0` 而非 `-`，与需求"B 不受 A 影响"的语义一致（有排班即显示实数）。
- `isDayEmpty` 简化为仅判断 `-/-`（原"0/0 也置灰"分支不再需要，因为存在排班时容量必 > 0）。
- 「添加预约」按钮的满员判定 `isDayFull` 继续走 `total − booked <= 0`，语义不变。

### 3.3 doctor-h5 `src/pages/home/index.vue`

- 单元格显示改为 `item && item.total > 0 ? `${item.booked}/${item.total}` : '-/-'`，删除 `abNum()`（原会把 A=0 渲染为 `-`）。
- 数据源本来就是 `slot-daily-summary`，无需改动请求逻辑。

### 3.4 后端 `careld-schedule-service`

- `ScheduleSlotMapper.selectByDate` 增加 `status = 1` 过滤（逐日时段列表只返回开放时段，与聚合口径一致；「添加预约」弹窗的时段下拉随之对齐）。
- `sumDailyByRange`（A/B 聚合）本身口径已正确（`status = 1`、`SUM(max_capacity)`），未改动。
- `ScheduleRuleController` 的 `/slot-daily-summary` `@Operation` 描述更新为「A=已约 B=当天可接受预约总数，B 不随已约变化」。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 项 | 手段 | 结果 |
|---|---|---|
| 类型检查 | store-web / doctor-h5 分别 `vue-tsc --noEmit` | ✅ 均 EXIT=0 |
| 后端构建 | `mvn -o -q -DskipTests package -pl careld-schedule-service -am`，重启 8285 | ✅ 构建成功，服务 11:28 重启完成 |
| 接口/数据库对账 | 对门店 1 取 `/slot-daily-summary` 与 DB 直查 `schedule_slot` 逐日比对 | ✅ 每日 booked/total 与 DB 一致；无排班日期不返回 |
| 双端 DOM 比对 | 浏览器分别读取 store-web `/schedule` 与 doctor-h5 `#/pages/home/index` 日历单元格文本（门店 1，2026-09） | ✅ 两端完全一致：01 `-/-`、02 `-/-`、03 `0/6`、10 `-/-`、15 `1/8`、21 `0/6`、22 `0/6`、23 `0/6`、30 `0/6` |
| A 随预约变化 | 真机新增预约（订单 86 / 儿童 58 / 2026-09-22 时段 2095046536043061250）后刷新双端日历 | ✅ 两端该日 A 由 `0` 变 `1`（`1/6`） |
| **B 不受 A 影响** | 上述预约前后与取消后，读取同日 B 值 | ✅ B 始终为 `6`：`0/6` → `1/6` → 取消后 `0/6` |
| 取消还原 | 取消订单 86（状态置 4，备注「日历A/B验证后取消」）后刷新 | ✅ 两端 A 回到 0 |
| 查询性能 | 统计 store-web `/schedule` 单月加载的网络请求数 | ✅ 由 ~32 次（1 次统计 + ~30 次逐日时段）降为 3 次（统计 + 汇总 + 当月时段总量） |
| 「添加预约」回归 | 打开 09-23 添加预约弹窗，检查时段下拉 | ✅ 正常列出 3 个开放时段（如「剩余2人」），`status=1` 过滤未误伤可用时段 |

---

## 5. 遗留与说明

1. 两端 A 现统一为 `schedule_slot.booked_count` 汇总。若历史测试数据造成 Σ已约 与 `reserve_order` 活跃订单数不一致（如门店 7 / 2026-09-14），日历以时段占位为准；正常预约/取消链路二者始终相等（`createReserveV2` 的容量保护与 `decrementBooked` 保证）。
2. 顶部「预约查询汇总」卡片（预约总数/完成数/取消数）仍走 `reserve_order` 统计，与日历 A 不同源属预期：卡片统计的是订单量（含已完成/已取消），日历 A 统计的是当天的名额占位。
3. `selectByDate` 增加 `status=1` 后，若某日存在"全部时段停用但仍有 booked_count"的残留数据，该日日历将显示 `-/-` 而实际有预约占位 —— 属排班管理的边界场景（停用时段不可再约），未做特殊处理。
4. 本次 A/B 口径统一涉及 store-web、doctor-h5 与 schedule 服务三处改动，与 #75–#79 同属未提交改动，等待确认后一并入库。
5. **2026-09-19 巡检补充**：门店 7 实测发现"保存排班规则后，已预约时段保留旧容量"会使 B 跨天不一致（09-14=12 ≠ 09-18=10）——属规则保存链路的存量数据问题（与本页 A/B 取数口径无关），已由第 81 项修复（spec 34），修复后门店 7 工作日 B 统一为 15、周六日 45。
