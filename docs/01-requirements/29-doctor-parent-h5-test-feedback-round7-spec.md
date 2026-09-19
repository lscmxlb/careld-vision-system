# 医生端 doctor-h5（第 7 批）+ 家长端 parent-h5（第 4 批）测试反馈改造 Spec（第 73/74 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（医生端 3 项 + 家长端 3 项） |
| 来源 | 测试反馈第 73/74 项（`docs/测试记录.txt` 第 224-232 行） |
| 涉及端 | 医生端 H5（doctor-h5）、家长端 H5（parent-h5）；后端 schedule-service / child-service |
| 涉及页面 | 医生端：养护记录（pages/care-record/index）、工作台（pages/home/index）；家长端：预约记录（pages/child/records）、预约养护（pages/appointment/index） |
| 状态 | 已开发（2026-09-18，vue-tsc + API/DB 对账 + 浏览器 DOM 断言验证通过） |

---

## 1. 需求清单（用户原话拆解）

### 1.1 #73 医生端（3 项）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 73-1 | 养护记录页 | 去掉「筛选」按钮 | doctor-h5 前端（pages/care-record/index） | ✅ 已完成 |
| 73-2 | 工作台日历 | A/B 中 B 改为「当天可预约总数量」，固定值，不受 A 影响 | 后端 schedule-service（SQL 聚合口径）+ doctor-h5 类型/渲染 | ✅ 已完成 |
| 73-3 | 工作台抬头 | 医院名称文字自适应，字多时自动缩小字号保证单行、不遮挡右侧日期 | doctor-h5 前端（pages/home/index） | ✅ 已完成 |

### 1.2 #74 家长端（3 项）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 74-1 | 档案详情-预约记录 | 「-.-」那一行改为预约日期 + 时段信息 | 后端 child-service（流水回填预约信息）+ parent-h5 渲染 | ✅ 已完成 |
| 74-2 | 预约养护页 | 「上一步/下一步」按钮上移，与底部标题区（tabBar）留出空闲间距 | parent-h5 前端（全局 `.sticky-bar-on-tab`） | ✅ 已完成 |
| 74-3 | 预约养护页第 1 步 | 去掉底部「下一步」按钮，改为点击儿童档案卡片直接进入下一页；按钮同样上移留白 | parent-h5 前端（pages/appointment/index） | ✅ 已完成 |

---

## 2. 现状诊断（2026-09-18 代码核实）

| 项 | 现状 |
|---|---|
| 73-1 | 养护记录页顶栏为「儿童姓名/手机号搜索下拉 + 查询 + 筛选」三件套；筛选按钮展开本地筛选面板（与儿童档案页的 chip 筛选同构） |
| 73-2 | `ScheduleSlotMapper.sumDailyByRange` 按天聚合返回 `booked=Σ已约`、`available=Σ(max_capacity - booked_count)`（= 剩余名额）；工作台卡片渲染 `${booked}/${available}` → **B 会随 A 变大而变小**，与用户预期（B=当日可预约总量，固定）相反。PC 端 store-web 排班日历口径本就为「Σ时段容量（固定不变）」，两端不一致 |
| 73-3 | 抬头 `<text class="hero-store">` 固定 `font-size: 42rpx`、`nowrap` 且无溢出保护：医院名较长时会被右侧日期区挤压/遮挡（`hero-left` 为 flex:1、`min-width:0`，超长即溢出） |
| 74-1 | 家长端「档案详情 → 预约记录」页（pages/child/records）数据源为 `GET /children/{id}/service-records`；非授权类流水（预约扣减 2 / 取消退还 3 / 爽约退还 4 / 爽约不退还 5）的 `paymentMethod/paymentAmount/doctorName` 全为空 → 该行渲染为「— · — · 开单医生 —」占位。流水表本身只有 `appointment_id`，无预约日期/时段字段 |
| 74-2 | 预约养护页（tabBar 页）底栏 `.sticky-bar.sticky-bar-on-tab` 紧贴 tabBar（`bottom: var(--window-bottom, 0)`，间隙 0），观感局促 |
| 74-3 | 第 1 步「选择儿童」为悬停选中态 + 底部「下一步」按钮两步操作；用户要求点卡片即进入下一步 |

---

## 3. 改造设计

### 3.1 #73(1) 养护记录页去掉「筛选」按钮

- `pages/care-record/index.vue` 顶栏移除「筛选」按钮，仅保留「儿童姓名/手机号」搜索下拉 + 「查询」按钮；页面全量文案已无「筛选」字样。

### 3.2 #73(2) 工作台日历 B 改为当日可预约总量（固定值）

- `ScheduleSlotMapper.sumDailyByRange`：`SUM(max_capacity - booked_count) AS available` → `SUM(max_capacity) AS total`，注释同步为「booked=Σ已约、total=Σ时段容量（总量固定，不随已约变化）」。过滤条件（`status=1`、按门店/日期范围、无排班日期不返回）不变。
- `ScheduleRuleServiceImpl.getSlotDailySummary`：输出键 `available` → `total`。
- `doctor-h5/src/types/index.ts`：`SlotDailySummary { date; booked; total }`（字段改名 + 注释）。
- `pages/home/index.vue`：卡片渲染 `${booked}/${total}`，A 或 B 为 0 显示 `-` 的逻辑不变。
- 兼容性：该接口为工作台专用（`GET /schedule-rule/slot-daily-summary`），PC 端未调用；PC 排班日历另一接口本就以「Σ时段容量」为口径，改造后两端一致。

### 3.3 #73(3) 医院名称自动缩放字号

- `pages/home/index.vue` 方案：**离屏测量元素 + 宽度等比缩放 + 最小字号兜底**（无 DOM 依赖、兼容 H5/小程序）。
  - hero 内新增离屏测量节点 `.hero-measure`（`position: fixed; left: -9999px; visibility: hidden`），以基准字号（42rpx）渲染同一医院名，其 `clientWidth` 即文字自然宽度；
  - `fitStoreName()`：`uni.createSelectorQuery()` 测 `.hero-left` 可用宽（减 8px 安全边距）与测量宽，按「同字体下文字宽度与字号成正比」等比缩放：`size = min(base, base * avail / measure)`，取整并夹在最小字号（24rpx）之上；`watch(storeNameText, fitStoreName, { immediate: true })`，门店名变化（登录/切换）自动重算；
  - `.hero-store` 增加 `white-space: nowrap; overflow: hidden; text-overflow: ellipsis`：极端超长名称在最小字号下仍超宽时以省略号收尾，避免半个字被硬裁。

### 3.4 #74(1) 预约记录页占位行改为预约日期 + 时段

- 后端 child-service：
  - `ChildServiceRecord` 新增 3 个 `@TableField(exist = false)` 展示字段：`reserveDate / timeSlotStart / timeSlotEnd`（不落库）；
  - `ChildServiceRecordMapper.selectReserveInfoByIds(ids)`：按 `id IN (...)` 批量查 `reserve_order`，`DATE_FORMAT` 输出 `%Y-%m-%d` 与 `%H:%i`，过滤 `deleted_at IS NULL`；
  - `ChildServiceImpl.listServiceRecords` 在回溯可用次数前调用 `fillReserveInfo(records)`：按 `appointment_id` 去重批量查、回填三条展示字段（无 appointmentId 或预约已物理删除则不回填）。
- 家长端 parent-h5：
  - `types/index.ts` `ChildServiceRecord` 增加 `reserveDate?/timeSlotStart?/timeSlotEnd?`；
  - `pages/child/records.vue` 支付行改为条件渲染：`row.reserveDate` 存在时显示「预约日期 {date} · 时段 {HH:mm}-{HH:mm}」，否则保持原「缴费方式 · 金额 · 开单医生」行。
- 口径：预约授权（type=1）无关联预约，继续显示缴费信息；扣减/退还/爽约类显示该笔流水对应的预约日期与时段。PC 端流水弹窗（store-web）未改动，仍显示缴费列。

### 3.5 #74(2)(3) 预约养护页按钮上移 + 点卡片进入下一步

- `App.vue` 全局 `.sticky-bar-on-tab`：`bottom: var(--window-bottom, 0)` → `bottom: calc(var(--window-bottom, 0px) + 24rpx)`（`--window-bottom` 为 uni-h5 注入的 tabBar 高 + 安全区），按钮条整体上移 24rpx 与 tabBar 留出空缺；`padding-bottom: 16rpx` 不变。该全局类当前仅 `appointment/index` 使用（child/form、child/detail 为非 tab 页用基础 `.sticky-bar`，行为不变）。
- `pages/appointment/index.vue` 第 1 步：
  - `pickChild()` 改为 async：选中卡片后直接 `step.value = 2` 并 `await loadAvailability()`（原「选中高亮 + 底部下一步」两步合并为一步）；
  - 底部按钮条加 `v-if="step > 1"`：第 1 步无任何底部按钮，第 2/3 步（上一步/下一步、上一步/确认预约）保持；
  - `canNext` 仅剩日期+时段校验，`nextStep()` 移除 step1 分支（保留 每日上限校验 → step 3）。

---

## 4. 验证计划与结果（2026-09-18 已执行）

- 构建：schedule-service / child-service `mvn -DskipTests package` 后经 `./start-all.sh` 重启（8285/8284）；parent-h5、doctor-h5 `vue-tsc --noEmit` 均通过（EXIT=0）。
- 浏览器（MCP，医生端已登录 张小丽 / 家长端 13869108888）：

| 项 | 结果 | 证据 |
| --- | --- | --- |
| 73-1 | 通过 | 养护记录页（`#/pages/care-record/index`）全页无「筛选」文案；顶栏仅「儿童姓名/手机号」下拉 + 「查询」，与原「查询」按钮布局不变 |
| 73-2 | 通过（API/DB 对账 + 页面） | `GET /schedule-rule/slot-daily-summary`（store 7，09-01~09-30）返回 13 天全部 = DB `SUM(max_capacity)` 逐日一致（例：09-18 booked=8 total=10，旧口径剩余仅 2；09-14 booked=4 total=10，旧口径为 4/6）；工作台日历实际渲染 `-/11、-/11、3/34、-/36、4/10、-/11、-/9、8/10、2/35、2/16、2/16、1/16、-/16`，B 已不随 A 变化（同一时段容量合计） |
| 73-3 | 通过 | 现名「武汉武昌协和卫生服务中心」：字号 28px（=42rpx 基准），`scrollWidth == clientWidth == 420`（单行不溢出），与右侧日期区无重叠（标题右缘 442 = 日期左缘 442，flex 布局正常分界）；运行时把门店名改成 23 字长名后：字号自动降到 16px（=24rpx 最小字号下限），仍无重叠，`.hero-store` `text-overflow: ellipsis` 生效（超宽部分省略号，不再硬裁半个字），改回原值后字号回弹 28px |
| 74-1 | 通过 | `#/pages/child/records?id=46` 共 12 行 meta：4 条扣减行显示「预约日期 2026-09-27 · 时段 09:00-10:00」等（与 `GET /children/46/service-records` 返回逐条一致：appt 82/78/77/75）；2 条授权行仍显示「医保-个人余额 · ¥1680 · 开单医生 胡春花」「免费体验 · ¥0 · 开单医生 胡春花」；全页「—」出现 0 次（原「— · — · 开单医生 —」占位行已消失） |
| 74-2 | 通过 | 第 2 步底栏（`.sticky-bar.sticky-bar-on-tab`）底缘 547，真实 tabBar 顶缘 563 → 间隙 16px（=24rpx 新留白），无重叠；第 3 步底栏同样间隙 16px |
| 74-3 | 通过 | 第 1 步无底栏（`hasSticky=false`）；点击「周小平」卡片 → 直接进入第 2 步（步骤条「选择儿童 ✓ / 2 选择时间」）并加载日期/时段，底栏出现「上一步/下一步」；「上一步」两次逐级退回第 1 步后底栏再次隐藏。验证全程未点「确认预约」；DB 核对：`reserve_order` 中 child_id=46 近 40 分钟无新增行 |

---

## 5. 遗留与说明

1. #73(2) 口径为「当日全部开放时段容量之和」（如 09-18 = 10），与该日已约数无关；若排班时段本身增删，B 会变化（B 固定仅指不受 A 影响）。
2. #73(3) 最小字号下限 24rpx（约基准的 57%）；超过约 21 个汉字的医院名会落到下限并以省略号收尾，属预期兜底。
3. #74(1) 仅家长端预约记录页展示变更；PC 端（store-web）流水弹窗保持原「缴费方式/金额/开单医生」列未动，如需同步另议。
4. 本批未提交 git（工作区累积 #63–#74 改动，提交待用户确认后统一入库，分支 dev）。
