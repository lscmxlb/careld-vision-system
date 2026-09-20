# store-web 数据中心统计卡改为「本月/总量」双指标 Spec（第 98 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 后端统计接口扩展 + 前端统计卡版面改造 Spec |
| 来源 | 用户需求：http://39.162.49.28:5175/dashboard 四张卡片优化——①「本月预约 / 总预约数量」②「本月新增 / 档案总数」③「本月养护 / 总养护数量」④不变；每格数字在上、文字说明在下（编号顺延自拟为第 98 项） |
| 涉及端 | store-web（5175）`/dashboard` + careld-user-service（8282）统计接口 |
| 状态 | 已开发（2026-09-19，真机 DOM 断言 + API/DB 对账通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 98-1 | 卡片 1 双指标 | 左「本月预约」、右「总预约数量」，数字在上、文字在下 |
| 98-2 | 卡片 2 双指标 | 左「本月新增」、右「档案总数」 |
| 98-3 | 卡片 3 双指标 | 左「本月养护」、右「总养护数量」 |
| 98-4 | 卡片 4 不变 | 仍为单指标「今日检测」 |
| 98-5 | 口径确认 | 预约口径＝含已取消（记录数），即 `deleted_at IS NULL` 全状态计数；本月养护按月＝**按养护日期 `care_date`**（非 created_at） |

口径由 AskUserQuestion 与用户确认：预约「含已取消（记录数）」、本月养护「按养护日期 care_date」。

---

## 2. 现状诊断

- `frontend/store-web/src/views/dashboard/index.vue` 四张卡均为单指标：今日预约（`todayReserves`）/ 待审核档案（`pendingChildren`）/ 在线设备（`activeDevices`）/ 今日检测（`todayTests`），数据来自 `GET /api/v1/statistics/dashboard?storeId=`。
- 后端 `StatisticsDtos.DashboardStats` 仅 4 个当日字段；`StatisticsMapper.dashboard` 仅 4 段当日子查询（`careld-user-service` 直接跨表 SQL 查共享库）。
- 「档案总数」需沿用既有档案口径：`deleted_at IS NULL AND status <> 2`（排除已隐藏，见 #88/档案统计惯例）。
- 「养护」计数源为 `care_record`（`care_date` 为业务日期）。

---

## 3. 改造设计

### 3.1 后端（careld-user-service）

1. `StatisticsDtos.DashboardStats` 新增 6 个字段（`long`）：`monthReserveCount / totalReserveCount / monthChildCount / totalChildCount / monthCareCount / totalCareCount`，原 4 个当日字段保持不动。
2. `StatisticsMapper.dashboard` 增加 `@Param("monthStart") LocalDate / @Param("monthEnd") LocalDate`，在原 4 段子查询后新增 6 段：
   - 本月预约：`reserve_order WHERE deleted_at IS NULL AND reserve_date BETWEEN #{monthStart} AND #{monthEnd}`
   - 总预约数量：`reserve_order WHERE deleted_at IS NULL`（不筛状态，含已取消）
   - 本月新增：`child_profile WHERE deleted_at IS NULL AND status <> 2 AND created_at >= #{monthStart} AND created_at < DATE_ADD(#{monthEnd}, INTERVAL 1 DAY)`
   - 档案总数：`child_profile WHERE deleted_at IS NULL AND status <> 2`
   - 本月养护：`care_record WHERE deleted_at IS NULL AND care_date BETWEEN #{monthStart} AND #{monthEnd}`
   - 总养护数量：`care_record WHERE deleted_at IS NULL`
   - 六段均带 `(#{storeId} IS NULL OR store_id = #{storeId})` 门店隔离条件。
3. `StatisticsServiceImpl.dashboard` 计算当月起止（`today.withDayOfMonth(1)` / 当月最后一天）后传入 mapper。

### 3.2 前端（store-web）

1. `types/index.ts` 的 `DashboardStats` 增补上述 6 个字段。
2. `views/dashboard/index.vue` 卡片 1–3 改为「图标 + 双指标块 `.stat-metrics`」结构：每个 `.metric` 内 `.stat-value`（数字，28px/700，在上）+ `.stat-label`（说明，14px #999，在下），两指标以 1px `#f0f0f0` 竖线分隔；卡片 4 保持原 `.stat-info` 单指标结构。
3. 脚本 `stats` reactive 初始化补 6 个字段为 0；接口返回后 `Object.assign` 原样覆盖。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 手段 | 结果 |
|---|---|
| 后端构建 | ✅ `mvn package -DskipTests -q -pl careld-user-service -am` 成功，jar 时间戳更新；8282 重启为新 PID（旧 2332190 → 新 2407916），端口 LISTEN 确认 |
| API/DB 对账（门店 1） | ✅ 接口返回 `10/17/8/12/1/1`（本月预约/总预约/本月新增/档案总数/本月养护/总养护），与直连 `careld_vision` 库逐项 SQL 计数完全一致 |
| API/DB 对账（门店 7） | ✅ 接口返回 `75/76/17/17/17/17`，与 DB 计数完全一致 |
| 真机 DOM 断言（`http://39.162.49.28:5175/dashboard`，store001_mgr 登录） | ✅ 四卡渲染为 `10/本月预约 ｜ 17/总预约数量`、`8/本月新增 ｜ 12/档案总数`、`1/本月养护 ｜ 1/总养护数量`、卡片 4 `1/今日检测`；`metricCounts: [2,2,2,0]`（前 3 卡各 2 指标、卡片 4 保持单指标）；每指标「数字在上」结构成立，数字字号 28px、字重 700；三处分隔线均为 `1px rgb(240,240,240)`；4 个图标均存在 |
| `vue-tsc --noEmit`（store-web） | ✅ EXIT=0 |
| 控制台 | ✅ 无新增错误（`[ECharts] Can't get DOM width or height` 为应用内浏览器 0×0 隐藏视口环境假象；`el-radio label` 弃用告警为登录页既有告警） |

---

## 5. 遗留与说明

1. 应用内浏览器为 0×0 隐藏视口，无法截图走查实际版面；本次以 DOM 结构 + 计算样式断言证明「双指标、数字在上、分隔线」，最终视觉效果以用户截图确认为准。
2. 卡片 4「今日检测」按需求保持原样（单指标 `.stat-info`）。
3. 后端仅扩展 `dashboard` 接口返回字段，未改动其它接口；`DashboardStats` 新增字段对旧前端为向后兼容的增量。
4. 与 #63–#97 同属未提交改动，等待确认后一并入库。
