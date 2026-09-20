# 预约详情列表默认排序优化 Spec（第 86、87 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 页面优化 Spec（无后端改动） |
| 来源 | 用户需求第 86、87 项（对话直接给出，未写入 docs/测试记录.txt）：①doctor-h5 首页日历点击 A/B 数字弹窗显示当天预约详情 ②两端「当天预约记录」默认排序为有效预约在上、已取消在下 |
| 涉及端 | doctor-h5（医生端）、store-web（医院端） |
| 涉及页面 | doctor-h5 `#/pages/home/index` 预约日历；store-web `/schedule` → 日历单元格「预约详情」弹窗 |
| 状态 | 已开发（2026-09-19，vue-tsc EXIT=0 ×2 + 真机 DOM 断言，门店 1 / 2026-09-15 混合状态数据验证通过） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 86-1 | 日历点击弹窗 | doctor-h5 首页点击日历 A/B 数字，弹窗显示当天预约详情 |
| 86-2 | 默认排序 | 弹窗内有效预约在上、已取消在下 |
| 87-1 | 默认排序 | store-web 日历「预约详情」弹窗列表同样有效在上、已取消在下 |

---

## 2. 现状诊断

1. **接口默认顺序不含状态分组**：`GET /api/v1/schedules/reserves` 由 `ScheduleServiceImpl.listReserves` 处理，排序固定为 `reserve_date ASC, reserve_time_start ASC, id ASC`，与状态无关。已取消/已爽约记录（DB 中同为 `status=4`，靠 `no_show_flag` 区分）会按时间穿插在有效记录之间。
   - 实例（门店 1 / 2026-09-15）：接口原序为 `08:30 已爽约(订单14)` → `14:00 已完成(订单73)`，先展示的是无效记录。
2. **doctor-h5 日历无点击交互**：`pages/home/index.vue` 的日历单元格只渲染 A/B 文本，无任何点击处理，医生无法从日历直接查看当天预约名单。
3. **store-web 弹窗直接用接口原序**：`views/schedule/index.vue` 的 `handleDetail` 将 `res.list` 直接赋给表格，未做任何排序。

---

## 3. 改造设计

### 3.1 排序规则（两端一致）

- 分桶：`status === 4`（已取消/已爽约）排在最后，其余（已预约/养护中/已完成）在前。
- 桶内：按 `timeSlotStart` 升序，同一开始时间再按 `id` 升序（与接口原序一致，保证稳定）。
- 仅调整展示层顺序，**不改服务端接口默认排序**，避免影响预约记录页、家长端、admin-web 等其它消费方。

### 3.2 store-web `src/views/schedule/index.vue`

- 新增 `sortDetailList(list)`，`handleDetail` 中改为 `detailList.value = sortDetailList(res.list)`。
- 其余（接口参数、表格列、空态文案）不变。

### 3.3 doctor-h5

- 新增组件 `src/components/DayReserveSheet.vue`：底部弹层（复用全局 `.mask` / `.sheet` 样式），标题 `{日期} 预约详情`，内容为只读 `ReserveCard`（`show-date readonly`，不显示操作按钮），含加载中与「该日期暂无预约记录」空态；组件内 `sortedList` 计算属性执行 3.1 排序。
- `src/pages/home/index.vue`：
  - 日历 A/B 文本绑定 `@click="openDayDetail(cell.date)"`，并加大点击热区（`padding: 2rpx 10rpx`）。
  - `openDayDetail` 拉取 `reserveApi.getReserveList({ storeId, date, page: 1, size: 100 })`，传给弹层；失败静默（错误提示由请求层统一处理）。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 项 | 手段 | 结果 |
|---|---|---|
| 类型检查 | store-web / doctor-h5 分别 `vue-tsc --noEmit` | ✅ 均 EXIT=0 |
| 同步生效 | 3 个文件 sync 至沙箱，两端 Vite 热更新 | ✅ store-web 5175、doctor-h5 5176 均加载新代码 |
| 接口原序对照 | 页面内以医生 token 调 `GET /schedules/reserves?storeId=1&date=2026-09-15&size=100` | ✅ 原序 `[14 08:30 status=4 noshow=1 刘思涵, 73 14:00 status=3 张小明]`（无效在前） |
| **#87 真机** | store-web（门店 1，王建国）→ `/schedule` → 09-15「预约详情」读表格行 | ✅ 2 行，顺序为 `14:00-15:00 张小明 已完成` → `08:30-09:30 刘思涵 已取消`（已按规则翻转） |
| **#86 真机** | doctor-h5（门店 1，赵医生）→ 首页点击 09-15 的「1/8」读弹层卡片 | ✅ 标题 `2026-09-15 预约详情`；卡片顺序 `张小明 已完成 14:00-15:00` → `刘思涵 已爽约 08:30-09:30` |
| 弹层只读 | 断言卡片内 `.rc-actions` 数量 | ✅ 0（不显示取消/调整/爽约/养护按钮） |
| 空态 | 点击 09-03（`0/6`、无预约）的 A/B | ✅ 弹层标题 `2026-09-03 预约详情`，0 张卡片，显示「该日期暂无预约记录」 |
| 关闭 | 点击弹层 ✕ 后断言 `.sheet` / `.mask` | ✅ 均移除 |

---

## 5. 遗留与说明

1. 排序为**展示层规则**，服务端 `/schedules/reserves` 默认顺序未变；若后续要求全局统一排序，需改 `listReserves` 并回归预约记录页、家长端、admin-web 等消费方。
2. 「已爽约」（`no_show_flag=1`）与「已取消」同属 `status=4`，一并归入下方：doctor-h5 显示为「已爽约」标签，store-web 显示为「已取消」标签（store-web 侧未区分爽约，为既有口径）。
3. doctor-h5 弹层为只读展示（需求仅要求"显示当天预约详情"）；如需在弹层内直接操作（开始养护/取消等），可后续按 `ReserveCard` 的非只读模式开放。
4. 点击 `-/-`（当天无排班）的日期同样可打开弹层，正常显示空态；若存在残留占位数据也会如实展示。
5. 与 #75–#81 同属未提交改动，等待确认后一并入库。
