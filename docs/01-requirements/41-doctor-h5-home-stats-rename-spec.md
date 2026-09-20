# doctor-h5 首页统计改名与「本月预约」口径调整 Spec（第 94 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 前端文案 + 统计口径调整 Spec（doctor-h5 + careld-user-service） |
| 来源 | 用户需求第 94 项：首页统计改名；「当前已预约数量」口径调整为本月状态为「已预约」的记录数 |
| 涉及端 | doctor-h5（5176）首页工作台；careld-user-service（8282）`GET /api/v1/statistics/workbench` |
| 状态 | 已开发（2026-09-19，真机 UI 断言 + API/DB 对账 + 口径边界实测通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 94-1 | 三项标签改名 | 儿童档案数量→**儿童档案**；当前已预约数量→**本月预约**；已完成养护次数→**养护次数** |
| 94-2 | 口径调整 | 「本月预约」= 预约日期在本月（当月首日～末日）且状态为已预约(1) 的记录数 |

---

## 2. 现状诊断

- doctor-h5 首页三个统计球文案为旧值（`frontend/doctor-h5/src/pages/home/index.vue` 的 `statItems`）。
- 数据来源 `GET /api/v1/statistics/workbench?storeId=`，SQL 在 `StatisticsMapper.workbenchStats`：
  - `childCount`：该院未删除且非已隐藏(2) 档案；
  - `reservedCount`（旧口径）：`ro.status = 1 AND ro.deleted_at IS NULL`，**不限月份**（全时段累计）；
  - `completedCareCount`：`care_record.status = 2`。
- 该接口仅 doctor-h5 首页使用，改口径无其它页面连带影响。

---

## 3. 改造设计

1. **前端文案**（`frontend/doctor-h5/src/pages/home/index.vue`）：
   - 三项 label 改为 儿童档案 / 本月预约 / 养护次数（区块注释同步）；
   - `src/types/index.ts` 中 `WorkbenchStats.reservedCount` 注释改为「本月预约数量（预约日期在本月且状态为已预约）」。
2. **后端口径**（`careld-user-service`）：
   - `StatisticsMapper.workbenchStats` 增加 `monthStart / monthEnd` 参数，`reservedCount` 子查询追加
     `AND ro.reserve_date BETWEEN #{monthStart} AND #{monthEnd}`；
   - `StatisticsServiceImpl.workbenchStats` 由 `LocalDate.now()` 计算当月首末日传入
     （`today.withDayOfMonth(1)` ～ `today.withDayOfMonth(today.lengthOfMonth())`）；
   - `StatisticsDtos.WorkbenchStats` 类/字段注释同步新口径。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 手段 | 结果 |
|---|---|
| `vue-tsc --noEmit`（doctor-h5） | ✅ EXIT=0 |
| 重建并重启 careld-user-service（2.1.0 jar） | ✅ 8282 端口新进程存活，接口正常 |
| API/DB 对账（store 1） | ✅ 接口 `{childCount:12, reservedCount:0, completedCareCount:1}` = DB（12 正常档案 / 0 条 9 月状态 1 / 1 条已完成养护） |
| API/DB 对账（store 7） | ✅ `reservedCount=7` = DB 中 store 7 九月状态 1 记录数 7 |
| **口径边界实测**（可恢复变更） | ✅ 将 store 7 记录 id=80 的 `reserve_date` 由 2026-09-27 临时移至 2026-10-05：`reservedCount` **7→6**（证明月份过滤生效，旧全时段口径不会下降）；恢复原值（含 `updated_at`）后回到 **7** |
| 真机 UI 断言（`http://39.162.49.28:5176/#/pages/home/index`，store001_doc1） | ✅ 三球依次为 `12 儿童档案 | 1 本月预约 | 1 养护次数`，与当时 API/DB 一致（该 1 来自验证用临时预约，见遗留说明） |

---

## 5. 遗留与说明

1. 为让「本月预约」在 UI 上出现非零值以便断言，创建了 1 条临时预约（id=89，store 1 / 2026-09-20 / 状态 1）；验证后已按正常业务流取消（状态 4，预约时段名额与儿童剩余次数均已回退），该记录作为正常取消记录留存，取消数相应 +1。
2. 月份边界取服务器本地时区的自然月（`LocalDate.now()`），与业务其它按月口径一致。
3. 与 #63–#93 同属未提交改动，等待确认后一并入库。
