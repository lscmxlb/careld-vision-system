# 医生端 H5 测试反馈（第 6 批）改造 Spec — 第 68 项

- 来源：`docs/测试记录.txt` 第 68 项（2026-09-18，共 8 小项，第 (8) 条客户留空待补充）
- 范围：`frontend/doctor-h5` 纯前端改造，**无后端改动**（复用既有接口）
- 状态：待实现

## 1. 现状诊断（2026-09-18 代码核实）

| 项 | 现状 |
|---|---|
| (1) | 修改手机号/密码为底部抽屉弹层（`.mask/.sheet`，`bottom:0`、z-index 98/99），页面是 tabBar 页，H5 tabBar 的 z-index=998 → 弹层底部（含按钮区）被压住；`.sheet-footer` 缺 `display:flex`，取消/确认上下堆叠整行排列 |
| (2) (3) | `grant.vue` 提交成功仅 `uni.navigateBack()`，`onLoad` 只有 `id` 无来源参数。工作台入口导航栈为 `home → child/select → grant`，回退落在「选择儿童」页而非首页；档案详情入口回退落在详情页而非列表页 |
| (4) | 儿童档案页有搜索框（无按钮）；右下 FAB「＋建档」`bottom:60rpx` 且被 tabBar 压掉一半；筛选 chip 为 6 枚（待审核/已审核/已驳回/已禁用/已隐藏/全部，`padding:10rpx 26rpx`、26rpx 字、渐变选中态），与预约记录页 5 枚状态 chip（`height:56rpx`、`padding:0 24rpx`、25rpx 字、浅蓝选中态）不一致；预约记录页「新建」按钮在搜索框右侧（`.create-btn`） |
| (5) | 养护记录页搜索框为纯 input（无下拉候选），右侧是「筛选」文本按钮（展开内联高级筛选面板：儿童姓名/家长姓名/养护次数大于）。项目已有 `components/ChildSearchPicker.vue`（下拉候选，仅返回已审核且启用儿童，关键字匹配儿童姓名/手机号）；养护记录接口支持 `childId` 精确过滤 |
| (6) | 日历每格只有日期数字 + A/B 小字，无边框/背景/圆角，空白日无统一样式 |
| (7) | 抬头医院名 `26rpx` 半透明白，医生姓名 `42rpx/600` 白；「医师」来自 `utils/dict.ts` 的 `STAFF_ROLE_MAP[1]`，工作台抬头与我的页身份行共用 `userRoleText()` |

## 2. 改造设计

### 2.1 #68(1) 手机号/密码弹窗上下左右居中 + 按钮左右排列

- `App.vue` 全局新增居中弹窗样式（非 scoped）：
  - `.modal-mask`：`position:fixed; inset:0; background:rgba(15,23,42,.45); z-index:1000; display:flex; align-items:center; justify-content:center; padding:0 56rpx;`
  - `.modal-card`：`width:100%; background:#fff; border-radius:24rpx; display:flex; flex-direction:column; max-height:70vh;`
  - `.modal-header`（标题居中 32rpx/600）、`.modal-body`（`padding:16rpx 32rpx;`）、`.modal-footer`（`display:flex; gap:20rpx; padding:20rpx 32rpx 32rpx;`，`.btn{flex:1}`）
- `pages/mine/index.vue`：修改手机号、修改登录密码两个弹层由 `.mask/.sheet` 换为 `.modal-mask/.modal-card`，底部「取消 / 确认修改」左右等宽排列；提交成功后先关闭弹窗再 toast/跳转。
- 全局 `.mask`/`.sheet` 的 z-index 由 98/99 提升到 **1000/1001**：修复同类问题（tab 页底部弹层被 tabBar 遮挡），预约记录页 `CancelSheet`、儿童详情审核弹层等同步受益；`.sheet-footer` 增加 `display:flex; gap:20rpx;`（`.btn{flex:1}`），使既有双按钮弹层的取消/确认也左右排列。
- 说明：`.sheet` 仍保持贴底样式（用户仅要求手机号/密码两个弹窗居中），其余页面弹层只修遮挡与按钮排列。

### 2.2 #68(2)(3) 预约授权提交后返回

- `grant.vue`：`onLoad` 读取 `options.from`（`home`/`child`）；提交成功后：
  - `from=home` → `uni.switchTab({url:'/pages/home/index'})`（工作台首页）
  - `from=child` → `uni.switchTab({url:'/pages/child/index'})`（儿童档案列表）
  - 无 `from` → 保持 `uni.navigateBack()`（兼容）


- `pages/home/index.vue` `goGrant()` → `navigateTo('/pages/child/select?from=home')`；`pages/child/select.vue` 透传 `from` 到 `grant`。
- `pages/child/detail.vue` `goGrant()` → `navigateTo('/pages/child/grant?id=x&from=child')`。

### 2.3 #68(4) 儿童档案筛选 chip + 「添加档案」按钮 + 预约记录「添加预约」

- 筛选区改为与预约记录页一致：容器换成 flex 换行（`display:flex; flex-wrap:wrap; gap:14rpx; padding:12rpx 28rpx 8rpx; background:#fff;`），chip 采用预约记录样式：`height:56rpx; padding:0 24rpx; border-radius:28rpx; background:#f2f5f8; color:#5b6572; font-size:25rpx; display:flex; align-items:center;`；选中态 `.chip-on`：`background:#eff6ff; color:#1d4ed8; font-weight:500`（**完全照搬，取消原渐变选中态**，用户已拍板）。chip 数量与「全部」互斥逻辑不变（6 枚含已隐藏）。
- 去掉右下角 FAB「＋建档」；搜索框右侧新增按钮「添加档案」，样式照抄预约记录 `.create-btn`（`margin-left:20rpx; height:72rpx; padding:0 32rpx; border-radius:36rpx; 蓝色渐变底; color:#fff; font-size:27rpx;`），点击 `goCreate()`（进建档表单）。
- `pages/reserve/index.vue` 搜索框右侧「新建」按钮文案改为「**添加预约**」（位置/样式/行为不变）。

### 2.4 #68(5) 养护记录搜索下拉 + 「查询」

- `pages/care-record/index.vue` 顶部搜索框替换为 `components/ChildSearchPicker.vue`（placeholder「儿童姓名 / 手机号」，与组件实际匹配口径一致——后端 pick-options 仅匹配儿童姓名与手机号、且仅返回已审核且启用档案）。
- 选中候选儿童后记录 `pickedChildId`（清空则置空），点击新「**查询**」按钮执行查询：`childId=pickedChildId`，未选中时不带儿童条件。
- 原「筛选」按钮改为页面内小号文字开关（保留展开高级筛选面板：儿童姓名/家长姓名/养护次数大于 + 重置/查询），面板内「查询」按钮行为不变。
- 说明：高级筛选面板的「儿童姓名/家长姓名」为服务端模糊匹配，与顶部下拉（仅启用+已审核档案）口径不同，二者并存。

### 2.5 #68(6) 日历「每一天统一方格」

- 每个日期格改为统一样式的小方格（用户拍板方案）：
  - `.cal-cell`：`width:14.2857%; padding:5rpx 4rpx; box-sizing:border-box;`
  - 新增内层 `.cal-box`：`height:88rpx; border-radius:12rpx; background:#f8fafc; border:1rpx solid #eef2f7; display:flex; flex-direction:column; align-items:center; justify-content:center;`
  - 日期 `.cal-day` 26rpx（今天仍为蓝色圆形高亮），A/B `.cal-ab` 20rpx 蓝字，无排班日显示 `-/-`（灰 `#cbd5e1`）
- 月首空白格保持空白占位（不显示方格）。

### 2.6 #68(7) 抬头医院名字号 + 「主治医师」

- `pages/home/index.vue`：`.hero-store` 字号/字重/颜色调整为与 `.hero-doctor` 一致（`font-size:42rpx; font-weight:600; color:#fff;`），两行格式统一。
- `utils/dict.ts` `STAFF_ROLE_MAP`：`1: '医师'` → `'主治医师'`（**全局统一**，用户已拍板；我的页「用户身份」行、工作台抬头等共用 `userRoleText()` 全部生效；`2: '医生助理'` 不变）。

### 2.7 #68(8)

客户留空，待补充后单独处理。

## 3. 验证计划与结果（2026-09-18 已执行）

- doctor-h5 `vue-tsc` 通过（EXIT=0）。
- 浏览器逐项（登录账号 张小丽 13788888888，store 7，staff_role=2）：

| 项 | 结果 | 证据 |
| --- | --- | --- |
| #68(1) 手机号/密码弹窗 | 通过 | `.modal-mask` flex+center（z-index 1000 > tabBar 998）；card 左右边距相等（38.22/38.22）、垂直居中（top 165.8 / 126.5）；页脚 `display:flex; gap:13.6px`，取消/确认修改按钮 `flex:1` 宽 190/188；点「取消」关闭，未提交任何修改 |
| #68(2) 工作台添加授权→回首页 | 通过（真实链路） | 工作台「添加授权」→ `/pages/child/select` → 选「刘畅」→ `grant?id=29&from=home` → 填开单医师后提交成功 → 落地 `#/pages/home/index`（POST /children/29/service-records 200） |
| #68(3) 档案详情预约授权→回列表 | 通过 | 详情「预约授权」→ `grant?id=29&from=child`；`backAfterDone()` 分支执行落地 `#/pages/child/index`（成功回调 →500ms→ backAfterDone 与 (2) 共用同一代码路径，已在 (2) 实测） |
| #68(4) chip 与「添加档案」 | 通过 | 儿童档案 `.chip`/`.chip-on` 与预约记录页 computed 完全一致（选中 #eff6ff/#1d4ed8/500、未选 #f2f5f8/#5b6572、h 38.2、radius 19.1）；「添加档案」渐变按钮（135deg #60a5fa→#2563eb、白字、h 49.1=72rpx）位于搜索框右侧，FAB 已移除；预约记录按钮文案「添加预约」 |
| #68(5) 养护记录下拉+查询 | 通过 | `ChildSearchPicker` 下拉返回「刘畅 女·6岁 13900000000 剩余 9 次」；选中后 `pickedChildId=29`，右侧「查询」按钮点击后列表过滤为该儿童 3 条记录；「筛选」为独立高级筛选开关 |
| #68(6) 日历方格 | 通过 | `.cal-cell` 31 格 / `.cal-box` 30 个（月首 1 空位），box 57.6×60.1（88rpx 高）、`flex-direction:column`，两行 `.cal-day`(17.7px)+`.cal-ab`(13.7px)，空值显示 `-/-` |
| #68(7) 抬头字号/主治医师 | 通过 | `.hero-store` 与 `.hero-doctor` computed 相同 28.67px(=42rpx)/600/白；`STAFF_ROLE_MAP` 1→主治医师 已由 vite 提供（工作台/我的页共用 `userRoleText()`；当前账号 staff_role=2 显示「医生助理」，role 1 即显「主治医师」） |

- 实测数据已还原：验证 (2) 产生的授权流水 `child_service_record.id=147`（刘畅 +1 次 / 168.00 元 / 自费支付）已删除，`child_profile.remaining_count` 由 10 回退为 9。

## 4. 遗留与说明

1. `.sheet` 贴底弹层仅修遮挡与按钮排列，未统一改居中（仅手机号/密码两个弹窗居中，按需求原文）。
2. #68(5) 下拉候选来源为 `/children/pick-options`（已审核且启用），已隐藏儿童的历史养护记录无法通过下拉筛选；如需覆盖，后续可给 pick-options 增加范围参数（后端改动）。
3. #68(8) 待客户补充。
