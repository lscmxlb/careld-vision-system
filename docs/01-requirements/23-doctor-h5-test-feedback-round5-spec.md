# 医生端 doctor-h5 测试反馈第 5 批改造 Spec（第 64/65/66 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（医生端 + 家长端 + 医院端文案，共 10 小项） |
| 来源 | 测试反馈第 64/65/66 项（docs/测试记录.txt 第 158-173 行） |
| 涉及端 | 医生端 H5（doctor-h5，主体）；家长端 H5（parent-h5，文案）；医院端 PC（store-web，文案） |
| 涉及页面 | 工作台（pages/home/index）、我的（pages/mine/index）、预约记录（pages/reserve/index）、开始养护（pages/reserve/care）、儿童档案列表/详情（pages/child/index、detail）、预约授权（pages/child/grant）、预约详情（pages/reserve/detail，本批删除）、pages.json（tabBar） |
| 状态 | 已开发（2026-09-18，vue-tsc + API + 浏览器 DOM 全量验证通过；含 #66(3) 当天已爽约放行） |
| 后端改动 | careld-schedule-service `ScheduleServiceImpl.startCare`（#66(3) 当天已爽约放行；2026-09-18 已重新编译并重启 8285） |

---

## 1. 需求清单（用户原话拆解）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 64-1 | 家长端建档 | 选择医院后无法选择该医院医师 | parent-h5（诊断确认，无改动） | ✅ 诊断完成（用户确认"此前 bug 已修复，可忽略"） |
| 65-1 | 我的页面 | 行序重排为：所在医院 / 用户身份 / 用户姓名 / 手机号码(登录账号) / 登录密码（删「账号」行） | doctor-h5（mine/index.vue） | ✅ 已完成 |
| 65-2 | 工作台 | 快捷按钮区与日历区上下对调、两区间距合适、两区下方无内容（删除今日预约卡片列表）；「儿童档案」→「添加档案」（同「+建档」），前移至第三位、养护记录第四 | doctor-h5（home/index.vue） | ✅ 已完成 |
| 65-3 | 预约授权 | 缴费方式与授权次数上下对调；开单医师默认选中当前登录医师（儿童档案内授权页同步） | doctor-h5（child/grant.vue） | ✅ 已完成 |
| 65-4 | 开始养护 | 录入养护前裸眼视力后点「开始养护」成功 → 返回预约列表页 | doctor-h5（reserve/care.vue） | ✅ 已完成 |
| 65-5 | 预约记录 | 点卡片不再进详情页（无响应）；详情页四按钮（取消预约/调整日期/标记爽约/开始养护）移到卡片下方、大小合适完整显示；卡片不显示「执行：xxx」；**用户拍板：直接删除详情页** | doctor-h5（reserve/index.vue、ReserveCard.vue、删除 reserve/detail.vue、child/detail.vue 链路） | ✅ 已完成 |
| 65-6 | 儿童档案 | 筛选区「已禁用」后新增「全部」（用户拍板：互斥全选）；所有位置「已通过」→「已审核」（用户拍板：全端含 store-web） | doctor-h5（child/index.vue、utils/dict.ts）；parent-h5（utils/dict.ts）；store-web（views/child/index.vue、types/index.ts） | ✅ 已完成 |
| 66-1 | 底部导航 | 从左至右固定：工作台、儿童档案、预约记录、养护记录、我的（锁定不允许修改） | doctor-h5（pages.json + 新增 tab 图标，养护记录升为 tab 页） | ✅ 已完成 |
| 66-2 | 完成养护 | 点「完成养护」后自动返回上一级列表页 | doctor-h5（reserve/care.vue） | ✅ 已完成 |
| 66-3 | 开始养护 | 「已预约当天时段，当天任意时间都应允许开始养护」的提示错误问题 | 诊断：已预约+当天任意时刻本就放行，缺口＝当天已爽约订单；**用户拍板：放开「当天且已爽约」** | ✅ 已完成（后端 ScheduleServiceImpl.startCare + doctor-h5 ReserveCard/reserve-rules） |

用户原话见 docs/测试记录.txt 第 158-173 行。

---

## 2. 关键设计决策

### 2.1 家长端选医师（64-1）

诊断链路：`parent-h5/src/pages/child/form.vue` 的 `loadDoctors()` 按所选医院 `storeId` 调 `/api/v1/medical-staff`；后端该接口受数据权限/鉴权约束，早期版本在家长端 token 下返回「没有权限访问」。经复查：当前代码（parent-h5 请求头带 token + 接口已放开家长读取）选择医院后医师下拉可正常加载，用户已确认"此前 bug 修复过，可忽略"。**本批不改动。**

### 2.2 我的页面行序（65-1）

固定 5 行（自上而下）：所在医院 / 用户身份 / 用户姓名 / 手机号码(登录账号) / 登录密码。原「账号」行删除（信息与手机号重复）；后两行为可点击入口（手机号 → 修改手机号弹窗，登录密码 → 去修改）。`.kv-key` 由固定 `width: 180rpx` 改为 `min-width: 180rpx; flex: none; white-space: nowrap`，「手机号码(登录账号)」长标签不换行、不挤压右侧值。

### 2.3 工作台结构重排（65-2）

1. 结构：hero（医院/姓名/角色/日期）→ **快捷按钮区** → **日历卡片** → 底部 tabBar，快捷区与日历区间距 `24rpx`，两区下方无任何内容（原「今日预约」卡片列表整体删除）。
2. 快捷区 4 个按钮（顺序即用户要求）：添加预约（＋）→ 添加授权（🎫）→ **添加档案**（👤，第三位）→ 养护记录（✎，第四位）。「添加档案」复用 `navigateTo('/pages/child/form')`，与儿童档案页「+ 建档」同页同功能。
3. 删除工作台「今日预约」后，`reserveApi`、`ReserveCard`、`CancelSheet` 及调整/取消/爽约/开始养护等处理函数一并从 home 页清理；「养护记录」入口因该页已升为 tab 页改用 `uni.switchTab`。

### 2.4 预约授权页（65-3）

1. 字段顺序：开单医师 → **缴费方式** → **授权次数** → 缴费金额 → 备注（缴费方式与授权次数对调）。
2. 开单医师默认选中当前登录医师：`loadDoctors()`（`staffRole=1、status=1`）加载后按 `userStore.displayName` 与医师姓名匹配，命中则写入 `form.doctorId`；**医生助理（staffRole=2）不在医师列表内，则保持「请选择开单医师」**（该账号需手动选择，实测 store7 当前账号为助理即为此分支）。
3. `onLoad` 调整为 `await loadDoctors()` 后再取档案/收费标准并 `capture()`，避免默认医师异步晚到导致脏数据误判。

### 2.5 开始/完成养护返回列表（65-4 / 66-2）

`care.vue` 成功后统一 `setTimeout(() => uni.switchTab({ url: '/pages/reserve/index' }), 600)`（保留 toast 展示时间）；「开始养护」与「完成养护」两条成功路径均不再停留在当前页，也不走 `navigateBack`（该页可由 tab 页/档案详情等多入口进入，`switchTab` 语义稳定）。

### 2.6 预约记录：删详情页、四按钮下移（65-5）

1. **删除 `pages/reserve/detail.vue`**（含 `pages.json` 路由）；`child/detail.vue` 内的预约卡片点击链路一并去掉，卡片组件改为纯展示。
2. **ReserveCard 重构**：props 收敛为 `row / showDate / readonly`，emits 为 `care / adjust / noshow / cancel`（去掉 `detail`）；卡片下方按钮区（`.rc-actions`）由 `row.status` 驱动——status=1 展示「取消预约｜调整日期｜标记爽约｜开始养护」四按钮（`flex:1; min-width:0` 等宽均分、间距 12rpx、`btn-sm` 高度完整显示），status=2 展示单个「完成养护」。禁用态沿用 `reserve-rules.ts`（未到期可取消/调整、当天或逾期可标记爽约、当天可开始养护），禁用时点击仍给出原因 toast。
3. **列表页自持弹窗**：取消原因 `CancelSheet` 与「标记爽约」确认框移到 `pages/reserve/index.vue`（此前在详情页），提交成功后 `reload()`。
4. 卡片正文不再渲染「执行：xxx」行；家长姓名/手机号行保留。

### 2.7 儿童档案筛选「全部」与「已审核」（65-6）

1. 筛选 chip 组：待审核 / 已审核 / 已驳回 / 已禁用 / **全部**（末尾新增）；默认仍为「待审核 + 已审核」两态。
2. 「全部」语义（用户拍板：互斥全选）：
   - 点「全部」→ 四个筛选值全部选中，展示上**仅高亮「全部」**（避免五个 chip 同时高亮的歧义）；
   - 全选态下点任一单项 → **仅保留该项**（与「全部」互斥，点击结果与视觉一致）；
   - 非全选态下点单项 → 维持原多选 toggle 行为（至少保留一个筛选状态）。
3. 文案：`AUDIT_STATUS_MAP[1].label` / `AUDIT_STATUS_OPTIONS`（医生端、家长端 dict）与 store-web 筛选下拉、列文案、审核成功提示统一由「已通过」改为「**已审核**」；admin-web 门店审核域文案不在本批范围。

### 2.8 tabBar 五页签（66-1）

- `pages.json` tabBar list 固定为：工作台（home）/ 儿童档案（child）/ 预约记录（reserve）/ **养护记录（care-record）** / 我的（mine）；`care-record/index` 由普通页升为 tab 页，`pages/reserve/detail` 同时移除。
- 新图标 `static/tabbar/care.png`、`care-active.png`（81×81 RGBA，灰 `#94a3b8` / 蓝 `#2563eb`，与其他图标同线宽与视觉风格，脚本超采样绘制）。
- 「锁定不允许修改」落实为：顺序与页签集合写入 pages.json 固定配置，后续迭代不得增删改序（本 spec 作为约定记录）。

### 2.9 #66(3) 诊断结论与拍板（当天已爽约放行）

**诊断**：
- 前端规则 `canOperateCare = status===2 || (status===1 && isReserveToday)`（`reserve-rules.ts`）与后端 `startCare` 原本即为「**已预约 + 预约日期=今天 → 当天任意时刻**可开始养护」（后端注释明确「允许提前开始养护」，无时段窗口限制）。
- 实测：对当天 11:00 的已预约订单在 16:15 调用开始养护成功 → 「当天任意时间」对 status=1 已满足。
- 唯一返回 4012「预约状态不允许开始养护」的路径：订单状态 ≠ 1。**当天被标记爽约的订单**（如 11:00 场次在时段结束+12h 后由 `ReserveAutoTask` 自动转爽约，或人工标记）再点「开始养护」即命中，这就是测试者遇到的场景。

**用户拍板（2026-09-18）：放开「当天且已爽约」（方案 A）**，即：

1. 后端 `ScheduleServiceImpl.startCare` 新增 `sameDayNoShow` 判断：`status==4 && noShowFlag==1 && reserveDate==今天` → 放行（不再抛 4012）；`status=2` 时一并 `noShowFlag=0`（清除爽约标记，按正常「养护中」流转）。
2. 次数口径不变：爽约本就不退还次数，开工不补扣、不返还（实测 `remaining_count` 与流水均无变化）。
3. 时段占用不补回：爽约时已 `decrementBooked` 释放名额，开工不再 `incrementBooked`（避免与期间已被他人预约的名额冲突/超卖）。
4. 前端 `canOperateCare` 与 `ReserveCard` 同步：当天已爽约卡片显示单个可点「开始养护」按钮（`sameDayNoShow` 计算属性）；非当天的爽约卡片不渲染操作区。
5. 边界保持拒绝：**非当天已爽约、当天已取消、已完成、养护中误调开始接口**仍 4012；「仅预约当天」4017 不变。
6. 未同步范围：store-web PC 端预约记录的开始养护按钮规则（`views/appointment-record/index.vue`）仍为旧口径（当天爽约置灰），如需同步另行处理。

---

## 3. 实现要点

### 3.1 doctor-h5

| 文件 | 变更 |
|---|---|
| `pages.json` | 删除 `pages/reserve/detail` 路由；tabBar 增补养护记录（care.png / care-active.png）并调整顺序为 5 项 |
| `pages/home/index.vue` | 结构重排（快捷区上/日历下、间距 24rpx、删今日预约区）；快捷按钮改名与顺序调整；养护记录入口改 switchTab |
| `pages/mine/index.vue` | 5 行行序与文案；`.kv-key` 不换行 |
| `pages/reserve/index.vue` | 卡片事件接入（care/adjust/noshow/cancel）；新增 CancelSheet 与爽约确认；删除详情页跳转 |
| `pages/reserve/care.vue` | 开始/完成养护成功后 switchTab 回预约列表 |
| `pages/reserve/detail.vue` | **删除**（349 行） |
| `components/ReserveCard.vue` | 重写：按钮区下移至卡片内、等宽、去 detail 事件、去「执行：」行；当天已爽约（`sameDayNoShow`）渲染单个可点「开始养护」按钮；status=1 拆为三按钮 + 主按钮 |
| `pages/child/index.vue` | 筛选新增「全部」chip 与互斥全选逻辑 |
| `pages/child/detail.vue` | 预约卡片改纯展示（去详情跳转）；审核提示文案改「已审核」 |
| `pages/child/grant.vue` | 字段顺序调整；开单医师默认本人；onLoad 先加载医师再 capture |
| `utils/dict.ts`（doctor-h5、parent-h5） | 「已通过」→「已审核」 |
| `utils/reserve-rules.ts` | `canOperateCare` 放开当天已爽约：`status===2 \|\| (isReserveToday && (status===1 \|\| (status===4 && noShowFlag===1)))` |
| `static/tabbar/care.png`、`care-active.png` | 新增图标 |

### 3.2 后端（careld-schedule-service）

| 文件 | 变更 |
|---|---|
| `service/impl/ScheduleServiceImpl.java` | `startCare` 新增 `sameDayNoShow` 判断（status==4 && noShowFlag==1 && reserveDate==今天 → 放行，不抛 4012）；状态置 2 时同步清 `noShowFlag=0`；次数与时段占用均不动（见 2.9） |

### 3.3 医院端（store-web，仅文案）

`store-web/src/views/child/index.vue`（下拉选项/列文案/审核提示改「已审核」）、`store-web/src/types/index.ts`（注释）。

---

## 4. 验证记录（2026-09-18）

### 4.1 类型检查

| 端 | 命令 | 结果 |
|---|---|---|
| doctor-h5 | `npx vue-tsc --noEmit` | 通过（无输出） |
| parent-h5 | `npx vue-tsc --noEmit` | 通过（exit 0） |
| store-web | `npx vue-tsc --noEmit` | 通过（无输出） |

### 4.2 浏览器（DOM / 真实 UI 流程，视口隐藏时以结构断言替代截图）

| 项 | 断言与结果 |
|---|---|
| 工作台 | 快捷区在日历上方（`compareDocumentPosition`），区间距实测 ≈17px（= 24rpx @531px 视口）；无「今日预约」文本；按钮顺序「添加预约/添加授权/添加档案/养护记录」 |
| 工作台入口 | 点「添加档案」→ `#/pages/child/form`（标题「儿童档案」） |
| tabBar | 5 页签文本与顺序正确；进入养护记录页时该页签图标切换为 `care-active.png`，其余为未激活图标 |
| 预约记录 | 12 张卡片（9 张含操作区）；status=1 卡片四按钮等宽（107.8/107.8/107.8/105.8px，高度 41px 完整显示）、status=2 卡片单「完成养护」；无「执行：」文本；点卡片后 hash 与页面栈不变（无响应） |
| 开始养护 | 预约 #70（小明，当天 11:00）在 16:15 点「开始养护」→ 录入养护前裸眼视力 → 提交成功 → hash 回 `#/pages/reserve/index`，卡片变「养护中」（status=2，按钮变「完成养护」） |
| 完成养护 | 同一预约点「完成养护」→ 录入养护后裸眼视力 → 提交成功 → hash 回 `#/pages/reserve/index`，status=3（已完成） |
| 养护记录 | 新记录出现在养护记录页首条（小明 2026-09-18 16:15-16:16，当前视力 5.1） |
| 儿童档案筛选 | chip 组「待审核/已审核/已驳回/已禁用/全部」，默认两态高亮；点「全部」→ 仅「全部」高亮；全选态点「已驳回」→ 仅「已驳回」高亮（列表 0 条、空态正常）；全页无「已通过」文本 |
| 我的 | 5 行顺序与文案正确（所在医院/用户身份/用户姓名/手机号码(登录账号)/登录密码），无「账号」行 |
| 预约授权 | 字段顺序「开单医师→缴费方式→授权次数→缴费金额→备注」；当前账号（医生助理）不下拉预选、保持占位；模拟登录人为医师（内存内替换 `userInfo.realName`）后重载医师列表 → `form.doctorId` 自动置为医师 id=1「胡春花」并回显 |
| 档案详情 | 儿童详情「预约记录」8 张只读卡片（无按钮、无「执行：」文本、点击无跳转）；审核状态显示「已审核」 |

### 4.3 #66(3) 当天已爽约放行（后端 + 前端）

**后端**：`ScheduleServiceImpl.startCare` 改后重启 8285（jar `careld-schedule-service-2.0.1.jar`，启动日志 `Started ...` 于 16:25）。

| 用例 | 请求 | 预期 | 实测 |
|---|---|---|---|
| 负向：非当天爽约 | POST `/schedules/reserves/69/start`（09-17 爽约） | 4012 | 4012 ✅ |
| 负向：当天已取消 | POST `/schedules/reserves/22/start`（当天已取消） | 4012 | 4012 ✅ |
| 正向：当天已爽约 | POST `/schedules/reserves/67/start`（当天 11:00 已爽约） | 200 | 200 ✅ |

正向用例数据变化（order 67）：`status` 4→2、`no_show_flag` 1→0、`start_time`=16:40、`executor`=胡春花(id=1)，新增 `care_record`(id=49，养护前 4.8/4.9/4.8)；**次数无变化**：`child_profile.remaining_count` 7→7、`child_service_record` 该儿童流水计数 2→2（不补扣、不返还）；时段占用不补回。

**前端（doctor-h5，浏览器 DOM 断言）**：

| 项 | 断言与结果 |
|---|---|
| status=1 当天 | 四按钮全 enabled ✅ |
| status=1 非当天 | 标记爽约/开始养护 disabled ✅ |
| status=2 | 「完成养护」enabled ✅ |
| status=3/4 非当天（历史） | 无操作区、无按钮 ✅ |
| 当天已爽约 | 显示单个可点「开始养护」✅ |
| 控制台 | 无 error（仅 vite/uView 常规日志）✅ |

**测试数据还原**：物理删除 `care_record` id=49；order 67 还原为 `status=4`、`no_show_flag=1`、清空 `start_time`/`executor_id`/`executor_name`/`completed_at`。

### 4.4 测试数据变化（本地开发库，仅供核对）

- 预约 #70（小明，2026-09-18 11:00-12:00）：验证开始/完成养护流程，状态经 已预约→养护中→已完成，并新增对应养护记录（养护前 5.0/4.9/5.1，养护后 5.1/5.0/5.2，执行人张小丽）。
- 原值记录：预约 #70 状态 1、noShowFlag 0、此前无养护记录。

---

## 5. 遗留 / 待确认

1. 「全部」chip 的交互已按「互斥全选」实现（点全部=全选仅亮全部；全选态点单项=只选该项），如与预期不符可在验收时微调。
2. 家长端/store-web 本批仅文案改动，未重新出包（随下批统一构建）。
3. **store-web PC 端未同步 #66(3)**：`views/appointment-record/index.vue` 的开始养护按钮仍为旧口径（当天爽约置灰），如需与医生端一致需另行改造。
4. 本批改动**暂未提交**（用户 2026-09-18 拍板：随下一批 #67 一起提交推送 dev）。
