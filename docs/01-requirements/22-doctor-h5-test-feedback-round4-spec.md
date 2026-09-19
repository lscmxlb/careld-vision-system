# 医生端 doctor-h5 测试反馈第 4 批改造 Spec（第 63 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（医生端优化，6 小项） |
| 来源 | 测试反馈第 63 项（docs/测试记录.txt 第 149-155 行） |
| 涉及端 | 医生端 H5（doctor-h5）；后端 careld-schedule-service |
| 涉及页面 | 预约记录列表（pages/reserve/index）、预约详情（pages/reserve/detail）、开始养护（pages/reserve/care）、添加预约（pages/reserve/create）、工作台（pages/home/index）、选择儿童（pages/child/select，新增）、预约授权（pages/child/grant）、儿童档案表单/详情（pages/child/form、detail） |
| 状态 | 已开发（2026-09-18，vue-tsc + API + 浏览器（DOM/计算样式/真实 UI 流程）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 63-1 | 开始养护页 | 底部三按钮：红「取消」、蓝「开始养护」、绿「完成养护」，大小与排列参照儿童档案详情页底部按钮；录入数据后返回/取消提示未保存；开始养护未录养护前视力提示；完成养护未录养护后视力提示 | doctor-h5（App.vue 全局按钮 + care.vue） | ✅ 已完成 |
| 63-2 | 开始养护页 | 时间前加「预约养护时间：」；执行人→养护人、养护前视力→养护前裸眼视力、养护后视力→养护后裸眼视力 | doctor-h5（care.vue） | ✅ 已完成 |
| 63-3 | 全端选择框 | 所有页面选择框统一为「预约授权页-开单医师」下拉框样式 | doctor-h5（App.vue 全局类 + 各页去重复样式） | ✅ 已完成 |
| 63-4 | 工作台 | 「新建预约」→「添加预约」；新增「添加授权」入口（先选儿童再进授权页） | doctor-h5（home/index.vue + 新增 child/select.vue） | ✅ 已完成 |
| 63-5 | 添加预约 | 不预列全部儿童，仅输入条件后以下拉框显示匹配记录，选中后回填查找框 | doctor-h5（新增 ChildSearchPicker 组件 + create.vue） | ✅ 已完成 |
| 63-6 | 预约记录 | 卡片「家长：」显示正确姓名；卡片按钮全部去掉；详情页按钮改名（调整日期/标记爽约/取消预约）与重排（取消预约最左、开始养护最右、取消与标记爽约对调、四按钮等距均分） | doctor-h5（ReserveCard/index/detail）+ 后端 schedule-service（家长姓名） | ✅ 已完成 |

用户原话见 docs/测试记录.txt 第 149-155 行（原文错字「攀约」=爽约、「詎瞳」=开始养护）。

---

## 2. 关键设计决策

### 2.1 三按钮与提示（63-1）

1. **按钮体系**：App.vue 全局新增两个实心按钮类——`.btn-danger`（$app-danger `#ef4444`）、`.btn-success`（$app-success `#22c55e`），与既有 `.btn-primary`（`#2563eb`）同规格；三按钮直接作为 `.sticky-bar` 子元素，沿用全局「sticky-bar 内按钮 `flex:1` 等宽 + gap 20rpx」布局（与儿童档案详情页底部按钮布局同源）。
2. **取消按钮语义**：点「取消」= 离开页面。未改动任何数据（与加载快照一致）时直接返回；有改动则弹「放弃填写 / 已录入的数据将不会保存，确认离开吗？」（继续填写｜确认离开）。`onBackPress` 同样拦截，避免物理返回绕过提示。
3. **视力未录提示**（用户拍板：弹确认框可继续）：点「开始养护」时养护前左/右眼未录 → `uni.showModal`「数据未填写完整 / 养护前裸眼视力未录入完整，是否确认开始养护？」（继续填写｜确认开始）；点「完成养护」时养护后左/右眼未录 → 同形态「…是否确认结束养护？」（继续填写｜确认结束）。确认后照常提交。
4. **脏检查机制**：`capture()` 快照在 onLoad 数据加载完成后写入（自动预填的执行人/开始时间不算脏），`isDirty() = !leaving && JSON.stringify(form) !== snapshot`。

### 2.2 家长姓名根因与修复（63-6）

卡片与详情页「家长：」显示 `-` 的根因有两层：

1. 后端 `fillChildInfo` 只用订单自身字段回填，未从儿童档案取家长姓名；
2. `reserve_order.parent_name` 在多数历史订单中为 NULL，权威数据在 `child_profile.parent_name`（明文列）。

修复：`QuotaMapper.selectChildMask` 的 SQL 追加 `parent_name AS parentName`，`fillChildInfo` 在档案家长姓名非空时覆盖订单值（为空时保留订单自身值）。手机号继续用掩码、儿童姓名解密显示全名（沿用既有策略）。

### 2.3 卡片按钮范围（63-6）

用户拍板：**仅预约记录页**卡片去掉操作按钮（统一进详情页操作），工作台「今日预约」卡片保留快捷按钮。实现为 `ReserveCard` 新增 `hideActions` prop，预约记录页传 `hide-actions` 并清理页内已不可达的取消/爽约/调整逻辑（含 CancelSheet 引入）。

### 2.4 详情页四按钮顺序（63-6，用户拍板 B 方案）

最终顺序：**取消预约｜调整日期｜标记爽约｜开始养护**（取消预约最左、开始养护最右，取消与标记爽约对调）。四按钮为 `.sticky-bar` 直接子元素 → 等宽均分、间距一致；文案：调整→「调整日期」、爽约→「标记爽约」、取消→「取消预约」；开始养护按钮按能否开工显示蓝底 `btn-primary` 或禁用态，status=2 时文案为「完成养护」。四按钮仅在 status=1 展示（与既有业务规则一致）。

### 2.5 选择框统一样式（63-3）

1. **基准样式**：预约授权页「请选择开单医师」的 `.field-control > picker > .select-box > .select-text + .select-arrow`。
2. **实现**：将 `.field-control / .select-box / .select-text / .select-arrow / .field-placeholder` 提升为 App.vue **全局（非 scoped）** 样式；各页删除本地重复定义（grant.vue 等），picker 一律包 `.field-control`。
3. **范围**（用户拍板：全部下拉类控件）：含视力值（VisionPicker）、时间、日期选择器。
4. **VisionPicker**：双 picker（主值+副值）改为 `.vr-cell` 样式（值与 `.select-box` 完全一致：`#f8fafc` 底、`#e2e8f0` 1rpx 边框、12rpx 圆角、72rpx 高、右箭头旋转 135deg），箭头由文字「▾」改为 CSS 边框绘制。
5. 覆盖位置：care.vue（养护人/开始时间）、child/form.vue（出生日期/关系/分娩方式/休息时间/起床时间/主治医师 6 处）、child/detail.vue 审核弹层（主治医生）、child/grant.vue（开单医师/缴费方式）、VisionPicker（care 页 6 组）。

### 2.6 搜索下拉组件（63-4/63-5）

新增 `components/ChildSearchPicker.vue`，被两个页面复用：

1. **添加预约页**（63-5）：替换原「预加载全部可预约儿童 + 前端关键字过滤」列表，改为输入关键字后 300ms 防抖调 `childApi.pickOptions({storeId, keyword})`，结果以下拉框展示（姓名 + 性别·年龄 + 手机号 + 剩余次数），选中回填「姓名 手机号」到查找框并收起下拉（带 ✕ 清空）；未输入时不发起请求、不展示任何记录。
2. **添加授权**（63-4）：工作台新增「添加授权」快捷入口 → 新增 `pages/child/select`（选择儿童页，同组件）→ 选中儿童后 `navigateTo` 进入既有 `pages/child/grant?id=xx`（授权表单零改动）。
3. 事件细节：`v-model` 先于 `@input` 更新（uni-app H5 源码已核实），`@input` 处理器无参读取 `keyword.value` 即可，避免 vue-tsc 将原生 `InputEvent` 判为类型不兼容。

### 2.7 文案调整（63-2）

开始养护页：时间行前置「预约养护时间：」；区块/字段「执行人」→「养护人」、「养护前视力」→「养护前裸眼视力」、「养护后视力」→「养护后裸眼视力」（含只读态「已登记，不可修改」/「结束养护时填写」提示不变）。

---

## 3. 实现要点

### 3.1 后端（careld-schedule-service）

- `mapper/QuotaMapper.java`：`selectChildMask` 增加 `parent_name AS parentName` 列。
- `service/impl/ScheduleServiceImpl.java`：`fillChildInfo` 档案家长姓名非空时覆盖订单 `parentName`。
- 构建重启：kill 旧进程 → `mvn package -DskipTests -pl careld-schedule-service` → nohup 启动（端口 8285）。

### 3.2 doctor-h5 前端

| 文件 | 改动 |
|---|---|
| `src/App.vue` | 新增 `.btn-danger` / `.btn-success`；新增全局选择框样式块（299-334 行） |
| `src/pages/reserve/care.vue` | 三按钮 sticky-bar；文案调整；select-box 化 picker；快照脏检查 + 返回拦截；两处视力未录确认框 |
| `src/pages/reserve/detail.vue` | 底部四按钮改名/重排/等宽；status=2 时「完成养护」 |
| `src/components/ReserveCard.vue` | 新增 `hideActions` prop |
| `src/pages/reserve/index.vue` | 卡片传 `hide-actions`，移除 CancelSheet 与页内取消/爽约/调整逻辑 |
| `src/pages/reserve/create.vue` | 儿童选择改 `ChildSearchPicker`，删除全量列表与前端过滤逻辑 |
| `src/pages/home/index.vue` | 「新建预约」→「添加预约」；新增「添加授权」（`goGrant` → `/pages/child/select`）；`.qi-emerald` 图标色 |
| `src/pages/child/select.vue` | 新增：选择儿童页（ChildSearchPicker → grant 页） |
| `src/components/ChildSearchPicker.vue` | 新增：搜索下拉组件（防抖查询/下拉展示/回填/清空） |
| `src/pages/child/form.vue` | 6 处 picker 统一 select-box 样式；删除本地 `.field-value` 残留 |
| `src/pages/child/detail.vue` | 审核弹层主治医生 picker 统一 select-box 样式 |
| `src/pages/child/grant.vue` | 删除本地重复样式定义（改用全局） |
| `src/components/VisionPicker.vue` | 双 picker 样式统一为 `.vr-cell`（等值 select-box 外观），CSS 箭头 |
| `src/pages.json` | 注册 `pages/child/select`（标题「选择儿童」） |

---

## 4. 验证记录（2026-09-18）

### 4.1 类型检查与接口

- `vue-tsc --noEmit` 通过（EXIT=0，含新增组件与页面）。
- 后端重启后 API 校验：预约列表返回家长姓名 张老六/关晓丽/王大明/胡大帅/周丽/大吱（原为 `-`）。

### 4.2 浏览器（DOM / 计算样式 / 真实 UI 流程）

| 项 | 断言结果 |
|---|---|
| 工作台快捷按钮 | 4 个：添加预约 / 添加授权 / 养护记录 / 儿童档案（含 qi-emerald 图标） |
| 预约记录卡片 | 无操作按钮；家长姓名正确显示 |
| 详情页四按钮 | 顺序 取消预约｜调整日期｜标记爽约｜开始养护；宽度 114/114/114/112px（x=17/145/274/402），等宽均分 |
| 开始养护页文案 | 「预约养护时间：2026-09-18 17:00-18:00」；养护人 / 养护前裸眼视力 / 养护后裸眼视力 |
| 三按钮样式 | 取消 `#ef4444`、开始养护 `#2563eb`、完成养护 `#22c55e`；实宽 156px ×3 等宽 |
| 真实流程（id=29） | 录入养护前视力(5.3×3) → 开始养护：无弹窗、status 1→2、写入 care_record（vision_before_*）；完成养护未录养护后视力 → 弹「数据未填写完整 / 养护后裸眼视力未录入完整，是否确认结束养护？」→ 继续填写（不完成）；改数据后点取消 → 弹「放弃填写 / 已录入的数据将不会保存，确认离开吗？」 |
| 添加授权链路 | 工作台 → 选择儿童页 → 搜索「刘」→ 下拉「刘畅 女·6岁 13900000000 剩余 10 次」→ 授权页 `grant?id=29`（刘畅[13900000000] / 当前可用次数 10 次 / 表单齐全） |
| 添加预约 | 初始不列任何儿童；输入「刘」后下拉显示匹配记录；选中后查找框回填「刘畅 13900000000」 |
| 选择框统一样式 | grant 2 处 / form 6 处 / detail 审核弹层 1 处 / care 2 处 + VisionPicker：计算样式一致（底 `rgb(248,250,252)`、边框 1px `rgb(226,232,240)`、圆角 8.496px、高 50.97px、含箭头，placeholder 灰字类生效） |

### 4.3 测试数据恢复

- `reserve_order` id=29：验证后恢复 status=1 / start_time=NULL / executor 清空（原值记录：status 1、start_time NULL、no_show_flag 0、adjust_flag 0）。
- `care_record` appointment_id=29：真实流程产生的记录已物理删除。
- `child_profile` id=47：为验证审核弹层样式临时由 store_id 1 调到 7，验证后已恢复 1。

---

## 5. 遗留 / 待确认

1. 视力未录提示仅校验左/右眼（双眼值为可选），与既有组件 `showSub` 设计一致，未调整。
2. `child/detail.vue` 审核弹层主治医生下拉存在同名医生（医师/助理同显示名）选项，属既有数据现象，本批未调整范围。
3. 「添加授权」链路只验证到授权页加载（未实际提交授权，避免产生业务数据）。
