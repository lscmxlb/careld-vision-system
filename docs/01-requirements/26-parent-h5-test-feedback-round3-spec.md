# 家长端 H5 测试反馈（第 3 批）改造 Spec — 第 69 项

- 来源：`docs/测试记录.txt` 第 69 项（2026-09-18，共 7 小项）
- 范围：`frontend/parent-h5` 纯前端改造，**无后端改动**（复用 `GET /children/{id}/service-records` 与现有接口）
- 状态：待实现

## 1. 现状诊断（2026-09-18 代码核实）

| 项 | 现状 |
|---|---|
| (1) | 登录页两处跳转（已有 token 进入 / 验证码登录成功）均为 `switchTab('/pages/child/index')`；tabBar 页签顺序：儿童档案 / 预约养护 / **预约记录** / 养护记录 / 我的 |
| (2) | 我的页中部有一张入口卡片，含「我的预约 / 养护记录 / 视力趋势 / 检测报告」四行（`goMyReserve/goCareRecord/goTrend/goReport`） |
| (3) | 档案详情页入口区只有「历次养护视力趋势 / 养护检测报告」两行；PC 医院端儿童档案的「预约记录」= 次数变更流水 `GET /children/{id}/service-records`（字段：变更类型/变更次数/可用次数/缴费方式/金额/开单医生/备注/时间），家长端 `childApi.getServiceRecords` 与类型 `ChildServiceRecord` 已存在 |
| (4) | 儿童档案卡片右上角显示「N次」（`remainingCount`，40rpx 加粗青绿）；接口 list/detail 均已返回 `careCount`（养护次数，含养护中/已完成） |
| (5) | `appointment/index.vue:226`「可用次数不足，请联系医院前台」、`:397`「可用次数不足，请联系医院前台充值」需按统一文案改；`mine/index.vue:58`「如有疑问请联系医院前台。」为咨询渠道语义，不动 |
| (6) (7) | 同一真因：parent-h5 无 `reserve/create.vue`，「立即预约」是 `switchTab` 到预约养护页（tabBar 页）；该页底部 `.sticky-bar` 为 `position:fixed; bottom:0; z-index:20`，而 H5 tabBar 为 `fixed; bottom:0; z-index:998`（高度 54px）→ 底栏整条被 tabBar 压住；`.page` 的 `padding-bottom:180rpx` 只留内容留白，救不了 fixed 条 |

## 2. 改造设计

### 2.1 #69(1) 登录后默认进入「预约记录」

- `pages/login/index.vue` 两处 `switchTab` 目标由 `/pages/child/index` 改为 `/pages/appointment/list`（已登录进入 + 验证码登录成功）；其余跳转/守卫不变。

### 2.2 #69(2) 我的页删除四个入口

- 删除「我的预约 / 养护记录 / 视力趋势 / 检测报告」整张入口卡片（及 `goMyReserve/goCareRecord/goTrend/goReport` 四个函数）。
- 「视力趋势」「检测报告」页保留（仍可从档案详情进入）；「关于 / 退出登录」卡片与页脚版本号不变。

### 2.3 #69(3) 档案详情新增「预约记录」

- `pages/child/detail.vue` 入口卡片在「养护检测报告」之后新增一行「预约记录」（`entry-row` 结构，`›` 箭头），点击跳转新页 `/pages/child/records?id={childId}`。
- 新页 `pages/child/records.vue`（pages.json 注册，标题「预约记录」，普通页 `navigateTo`）：
  - 数据：`childApi.getServiceRecords(id)`（与 PC 同一接口，保证一致）；
  - 每条流水卡片展示（字段与 PC 一致）：变更类型（tag：1 预约授权 / 2 预约扣减 / 3 取消退还 / 4 爽约退还 / 5 爽约不退还）、变更次数（+N 绿 / -N 红）、可用次数、日期 + 时间、缴费方式、缴费金额、开单医生、备注（空不显示）；
  - 加载中 / 空态（「暂无预约记录」）处理。

### 2.4 #69(4) 儿童档案卡片次数改 A/B

- `pages/child/index.vue` 卡片右上角由「N次」改为 A/B：结构照医生端（`.remain-label` 养护 / `.remain-num` / `.remain-slash` / `.remain-label` 可用 / `.remain-num`），A=养护次数 `careCount`、B=可用次数 `remainingCount`；数字使用家长端主色 `#14b8a6`（0 值灰 `#cbd5e1`），字号/间距照医生端（标签 22rpx、数字 36rpx 加粗、斜杠 26rpx）。
- 详情页 hero「可用次数 N 次」保持现状（需求仅指列表卡片）。

### 2.5 #69(5) 文案修正

- `appointment/index.vue`：
  - `:226` `pickTip()` → 「可用次数不足，请联系医院授权预约次数」
  - `:397` 提交前拦截 toast → 「可用次数不足，请联系医院授权预约次数」

### 2.6 #69(6)(7) 底部按钮遮挡修复（同一真因）

- `App.vue` 新增全局修饰类 `.sticky-bar-on-tab { bottom: var(--window-bottom, 0); padding-bottom: 16rpx; }`：uni-h5 运行时注入 `--window-bottom = tabBar 高 + 安全区`（tab 页）或 `= 安全区`（非 tab 页），故 tab 页底栏抬到 tabBar 之上、安全区已由下方占位，去掉自身底部安全区内边距。`pages/appointment/index.vue` 的底栏加该类。
  - 未直接改 `.sticky-bar` 全局样式：非 tab 页（`child/form`、`child/detail`）的 `--window-bottom` 为安全区高度（非 0），全局套用会在刘海屏多出一条空隙，故改为 tab 页按需加类。
- `pages/appointment/index.vue` `.page` 的 `padding-bottom` 重算为「底栏高 + tabBar 高 + 安全区」（`calc(240rpx + env(safe-area-inset-bottom))`），保证内容不被底栏遮挡。
- 该页含两处反馈（预约养护页按钮被遮挡、儿童档案「立即预约」进入后的页面按钮被遮挡），一次修复同时覆盖。

## 3. 验证计划与结果（2026-09-18 已执行）

- parent-h5 `vue-tsc` 通过（EXIT=0）。
- 浏览器逐项（登录账号 13869108888 家长，孩子 周小平 id=53 / 杨小虎 id=46）：

| 项 | 结果 | 证据 |
| --- | --- | --- |
| #69(1) 登录落地预约记录 | 通过（真实登录链路） | 我的页「退出登录」（确认框 `.uni-modal__btn` 点「退出」）→ token 清空回登录页 → 手机号 13869108888 + 验证码 123456 → 登录成功 300ms 后落地 `#/pages/appointment/list`（我的预约，5 条记录）；登录页 `onLoad` 已登录分支同样指向该页 |
| #69(2) 我的页无四入口 | 通过 | 我的页仅剩「手机号码/绑定儿童(2个)/剩余可用次数(8次)」卡片 + 关于/退出登录 + 版本号；无「我的预约/养护记录/视力趋势/检测报告」文案 |
| #69(3) 档案详情「预约记录」新页 | 通过 | 详情入口卡片第 3 行「预约记录 ›」→ `#/pages/child/records?id=46`（导航标题「预约记录」）；渲染 6 条与 `GET /children/46/service-records` 返回逐条一致（类型/±次数/可用次数/缴费方式/金额/开单医生/备注），与 PC 端流水弹窗同源同数据 |
| #69(4) 卡片 A/B 次数 | 通过 | 卡片右上「养护 N / 可用 M」：数字 24.58px(=36rpx)/700/#14b8a6，标签 15.02px(=22rpx)/#94a3b8，斜杠 17.75px(=26rpx)/#cbd5e1；数据与档案（careCount/remainingCount）一致（周小平 1/1、杨小虎 1/7）。`remainingCount=0` 的置灰类 `.remain-zero` 为模板绑定，本期无 0 次儿童未实测 |
| #69(5) 文案 | 通过 | `pickTip()` 运行时：`remainingCount:0` → 「可用次数不足，请联系医院授权预约次数」、待审核 → 待审核文案、正常 → 空；儿童档案「立即预约」对 0 次儿童同样走该 toast（`canPick(0次)=false`） |
| #69(6)(7) 底栏遮挡 | 通过 | 预约养护页 `.sticky-bar.sticky-bar-on-tab`：bar 492~563，真实 tabBar（`uni-tabbar .uni-tabbar`，54px）563~617，**间隙 0、无重叠、完整可见**；`--window-bottom = calc(54px + 0px)`。儿童档案「立即预约」→ switchTab 进同页，底栏同样完整可见 |
| 附加 | 通过 | 我的页底部/预约养护页底栏均为 `.sticky-bar-on-tab`；非 tab 页（child/form、child/detail）未加类、`bottom:0` 行为不变 |

## 4. 遗留与说明

1. 卡片 A/B 配色采用家长端主色（青绿），未照搬医生端蓝色，保持端内视觉一致；如要求与医生端完全同色可再调。
2. 「预约记录」新页为只读流水，不含分页（按儿童维度记录量小，接口返回全量）。
3. `.remain-zero`（可用 0 置灰）当前测试数据中无 0 次儿童，未做运行时不观感验证。
