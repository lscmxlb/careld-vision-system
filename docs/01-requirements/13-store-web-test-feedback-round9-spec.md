# 医院端 store-web 测试反馈第 9 批改造 Spec（第 46-47 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（养护时段口径 + 预约记录列顺序） |
| 来源 | 测试反馈第 46-47 项（docs/测试记录.txt 第 93-94 行） |
| 涉及端 | 门店医院Web端（store-web）、后端 careld-vision-service（家长端 parent-web 同步生效） |
| 涉及页面 | 档案管理-养护记录、预约管理-预约记录 |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + API（真实登录态）+ 浏览器（DOM）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 46 | 档案管理-养护记录 | 养护时段以实际开始/结束养护时间为准，未录入结束时间的按开始+60分钟 | vision-service 读时计算（list/page/get 三端点） | ✅ 已完成 |
| 47 | 预约管理-预约记录 | 养护人列前移至预约人后一列 | store-web 前端列顺序 | ✅ 已完成 |

用户原话：

- 「46、养护记录，养护时段，以医师或医助实际录入的开始养护和结束时间为准，只录入开始养护时间未录入结束时间的，按开始养护时间加60分钟计算结束时间」
- 「47、预约记录列表，将养护人列位置前移至预约人后一列显示」

---

## 2. 关键设计决策

1. **#46 数据来源判定（诊断结论）**：`care_record.time_slot` 存储列在开始养护时写入的是**预约时段**（`ScheduleServiceImpl:293` 用 `reserve_order.reserve_time_start/end` 格式化），与实际情况不符；医师/医助在「养护记录登记」弹窗录入的实际开始时间存于 `reserve_order.start_time`，结束养护点击时刻由系统记录于 `reserve_order.completed_at`。
2. **#46 展示口径**：养护时段 = 实际开始 ~ 实际结束：
   - 已完成（`completed_at` 存在且晚于开始时间）→ 实际结束时间；
   - 只录入开始养护时间（养护中，或 `completed_at` 缺失/早于开始）→ 开始时间 + 60 分钟；
   - 无实际开始时间（异常兜底）→ 保留原 `time_slot` 值。
   展示格式 `HH:mm-HH:mm`（与既有格式一致）。
3. **#46 实现位置（最小影响拍板）**：在 **vision-service 读时计算**（`CareRecordController#fillActualCarePeriod`），不改写入逻辑、不做数据迁移：
   - `/care-records/page`（医院端养护记录页）、`/care-records`（按儿童查询：家长端瀑布流 + 医院端导出用）、`/care-records/{id}`（详情）三端点统一生效；
   - 历史数据自动按新口径展示，无需 DML；
   - `care_record.time_slot` 存储列保持原值（仍为预约时段），展示层不再直接使用（见遗留 1）。
4. **跨日/异常兜底**：结束时间早于开始时间视为「未录入有效结束时间」，回落 +60 分钟；+60 跨零点按次日时间展示（如 23:30-00:30），不做跨日特殊标注（罕见，见遗留 2）。
5. **#47 列顺序**：预约记录表格列调整为「…来源、预约人、**养护人**、取消人、调整人、操作」；纯前端调整，列宽不变（各 100px）。

---

## 3. 各项实现要点

### 3.1 #46 后端（careld-vision-service）

- `mapper/CareRecordMapper.java`：新增内部类 `CarePeriodSource`（appointmentId/startTime/completedAt）+ `@Select` 批量查询 `reserve_order`（`WHERE id IN`）的 `selectCarePeriodSources`，供时段计算取数（避免逐行查询）。
- `controller/CareRecordController.java`：
  - 常量 `HM = DateTimeFormatter.ofPattern("HH:mm")`；
  - 新增 `fillActualCarePeriod(List<CareRecord>)`：批量取预约单时间 → 逐条计算并覆盖 `timeSlot`（仅内存，不落库）；
  - `list()`、`page()`（解密姓名之后）、`get()` 三处调用。
- 未改：schedule-service 的开始/完成养护写入逻辑；无迁移脚本、无 DML。

```java
LocalDateTime end = source.getCompletedAt();
if (end == null || !end.isAfter(source.getStartTime())) {
    end = source.getStartTime().plusMinutes(60);
}
record.setTimeSlot(source.getStartTime().format(HM) + "-" + end.format(HM));
```

### 3.2 #47 前端（store-web `views/appointment-record/index.vue`）

养护人列块从「调整人之后」移至「预约人之后」，其余不变：

```html
<el-table-column prop="operatorName" label="预约人" width="100">
  <template #default="{ row }">{{ row.operatorName || '-' }}</template>
</el-table-column>
<el-table-column prop="executorName" label="养护人" width="100">
  <template #default="{ row }">{{ row.executorName || '-' }}</template>
</el-table-column>
```

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --noEmit` | 通过（exit 0） |
| 构建重启 | 先 kill 旧进程（2386025，端口 8286 约 2 秒释放）→ `mvn package -DskipTests -pl careld-vision-service -am` → 启动 | BUILD SUCCESS；jar 内含 `CareRecordMapper$CarePeriodSource.class`；日志 `Started VisionServiceApplication`（19:18:27，PID 2598581） |
| API | `GET /care-records/page?storeId=7`（careld3 登录态）8 行时段 | 逐行与 DB 实际时间一致：40→18:30-19:30（养护中，开始+60）；39→17:03-17:40；38→16:52-18:24；36→15:53-15:57；35→14:48-15:55；32→18:20-19:50；30→16:26-17:54；29→16:11-16:14 |
| API | `GET /care-records?childId=`（家长端同源端点） | childId=21→40 号 18:30-19:30；childId=38→29 号 16:11-16:14，与分页端点一致 |
| 浏览器 | 养护记录页 8 行「养护时段」列 | 显示值与上表一致（修复前为预约时段，如 19:00-20:00） |
| 浏览器 | 预约记录页表头顺序 | 日期/时段/儿童姓名/家长电话/状态/来源/预约人/**养护人**/取消人/调整人/操作；首行「预约人=胡院长、养护人=张明丽」 |
| 浏览器 | 控制台 | 无错误（仅既有 vue-router `next()` 弃用警告与 pinia 日志） |
| DB | 数据变更 | 无（本轮无 INSERT/UPDATE/DELETE） |
| 浏览器 | 登录态 | 验证后为 careld3 登录态（/care-record → /appointment-record 页面切换，未改登录） |

**环境备注**：应用内浏览器 0×0 视口下 `evaluate_script` 需传 `waitForStableDom:false` 才不超时（否则等待稳定 DOM 一直阻塞）；API 验证通过页面内 `fetch` + `localStorage` token 走 Vite 代理直连。

---

## 5. 遗留与注意事项

1. **`care_record.time_slot` 存储列仍为预约时段**：本轮只改展示口径（读时计算），写入逻辑（`ScheduleServiceImpl:293`）与存量数据未动，直接查库看到的仍是预约时段。如后续要求存储层对齐实际时间，需改 schedule-service 写入 + 存量回填（本轮有意保留，避免 DML）。
2. **跨零点时段未特殊标注**：开始 23:30 未结束 → 展示 23:30-00:30；已结束跨日同理，无「次日」标注。
3. **家长端（parent-web）同步生效**：家长端养护记录列表/详情与医院端同源（`/care-records`、`/care-records/{id}`），也会显示实际养护时段；如需家长端保留预约时段口径需另行区分。
4. **#47 列宽未调整**：养护人在 100px 列宽下显示正常（2-3 字姓名），如后续出现长姓名截断再评估。
5. **未提交 git**：本轮改动与既有未提交改动一同保留在工作区（vision-service 三个文件当前为未跟踪状态）。
