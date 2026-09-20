# store-web 数据中心「最近预约」列调整 Spec（第 101 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 前端列表列调整 Spec（时段单行 + 去技师列 + 增性别/年龄列，含后端字段补充） |
| 来源 | 用户需求：http://39.162.49.28:5175/dashboard 「最近预约」列表调整优化：时段调整成一行、去掉技师列、儿童姓名后面增加儿童性别与儿童年龄列（编号顺延自拟为第 101 项） |
| 涉及端 | store-web（5175）`/dashboard` 数据中心；schedule-service（8285）预约列表接口 |
| 状态 | 已开发（2026-09-19，真机 DOM 断言 + API/DB 对账通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 101-1 | 时段一行 | 「时段」列内容不再换行（截图原为 `08:30:00` 两行显示） |
| 101-2 | 去技师列 | 删除「技师」列 |
| 101-3 | 新增性别列 | 「儿童姓名」之后增加「儿童性别」列 |
| 101-4 | 新增年龄列 | 「儿童姓名」之后增加「儿童年龄」列（与性别相邻） |

---

## 2. 现状诊断

- **时段换行**：后端 `reserve_time_start/end` 为 `LocalTime`，JSON 序列化为 `HH:mm:ss`（8 字符），与短横线拼接后样例如 `08:30:00-09:30:00`（17 字符），列宽 120px 放不下导致换行成两行。
- **技师列**：表格含 `technicianName` 列（数据来源 `fillTechnicianInfo`），用户不需要。
- **缺性别/年龄**：行数据（`ReserveOrder`）不含儿童性别与年龄；`fillChildInfo` 只回填了 `childName`/`parentPhone`/`parentName`，`QuotaMapper.selectChildMask` 的 SQL 也只查 `name_mask/phone_mask/parent_name`。

---

## 3. 改造设计

### 3.1 后端（schedule-service）

1. `QuotaMapper.selectChildMask`：SQL 扩展 `gender` 与 `TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) AS age`；返回类型 `Map<String, String>` → `Map<String, Object>`（仅 `fillChildInfo` 一处调用，无其他影响）。
2. `ReserveOrder` 新增两个 `@TableField(exist = false)` 展示字段：`childGender`（1=男 0=女）、`childAge`（岁）。
3. `ScheduleServiceImpl.fillChildInfo`：解析 mask 中的 `gender`/`age`（`instanceof Number` 判定）回填到行对象。
   - 年龄口径与全局一致：`TIMESTAMPDIFF(YEAR, birth_date, CURDATE())`（同 #94 care-record 口径）。

### 3.2 前端（store-web）

1. 列调整（`dashboard/index.vue` 最近预约表格）：
   - 「日期」120 → 「时段」120（一行）→ 「儿童姓名」100 → 新增「性别」70 居中 → 新增「年龄」70 居中 → 「家长电话」130 → 「状态」100。
   - 删除「技师」列。
2. 时段单行：新增 `formatHm = (t?: string) => (t ? t.slice(0, 5) : '')`，模板渲染 `{{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}`（HH:mm，5+1+5=11 字符），沿用 `/appointment-record` 页既有惯例。
3. 性别渲染：`row.childGender === 1 ? '男' : row.childGender === 0 ? '女' : '-'`（与 care-record 页三段式一致，脏数据如 gender=2 显示 `-`）。
4. 年龄渲染：`row.childAge ?? '-'`。
5. `types/index.ts`：`Reserve` 增加可选 `childGender?: number`、`childAge?: number`。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 手段 | 结果 |
|---|---|
| `vue-tsc --noEmit`（store-web） | ✅ EXIT=0 |
| `mvn package -DskipTests -q -pl careld-schedule-service -am` | ✅ EXIT=0，jar 更新时间 15:07 |
| 重启 8285 | ✅ OLD_PID=2412144 → NEW_PID=2418687，`Started ScheduleServiceApplication in 3.594 seconds`，端口就绪 |
| API/DB 对账（门店 7，管理 token） | ✅ 预约 110→(gender=1, age=8)、109→(1,8)、108→(0,3)、76→(1,9)、107→(0,12)，与 `child_profile` DB 查询逐条一致；门店 1 同样一致（86(2,7)/85(2,7)/89(0,7)/72(1,8)/71(1,8)） |
| 真机 DOM 断言（5175，store001_mgr 登录） | ✅ 表头顺序 `日期\|时段\|儿童姓名\|性别\|年龄\|家长电话\|状态`，`techHeader: false`（无技师列）；行示例 `2026-09-22 \| 08:30-09:30 \| 日志验证儿童 \| - \| 7 \| 139****1111 \| 已取消`、`张小明 \| 男 \| 8`、`09-16 行 \| 已爽约` |
| 时段单行 | ✅ 时段单元格文本 `08:30-09:30`，`timeCellHeight: 23px === lineHeight: 23px`（单行证明） |
| 控制台 | ✅ 无新增错误（仅历史既有告警：vite connecting/connected、pinia installed、vue-router `next()` 弃用、el-radio `label` 弃用 ×2、ECharts 0×0 视口） |

---

## 5. 遗留与说明

1. 排序、状态文案、倒序逻辑同 #99，本次未改动（默认升序接口 `/schedules/reserves` 的老调用方行为不受影响）。
2. 儿童性别存在历史脏数据（如 gender=2），统一兜底显示 `-`；未做数据清洗。
3. 若需在 `parentPhone` 之后再补其他儿童字段，可继续扩展 `selectChildMask`（一次查询即可）。
4. 应用内浏览器为 0×0 隐藏视口，单行以 DOM 高度/行高相等证明，最终视觉效果以用户截图确认为准。
5. 编号自拟：本项用户未给出编号，按序列记为 #101。
6. 与 #63–#100 同属未提交改动，等待确认后一并入库。
