# 家长端 parent-h5 测试反馈第 2 批改造 Spec（第 67 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（家长端为主体，含医生端 H5、医院端 PC、后端 child-service） |
| 来源 | 测试反馈第 67 项（docs/测试记录.txt） |
| 涉及端 | 家长端 H5（parent-h5，主体）；医生端 H5（doctor-h5）；医院端 PC（store-web）；后端 careld-child-service |
| 涉及页面 | parent-h5：儿童档案列表/详情（pages/child/index、detail）、添加档案（pages/child/form）、预约（pages/appointment/index）、我的（pages/mine/index）、登录（pages/login/index）、pages.json；doctor-h5：儿童档案列表/详情（pages/child/index、detail）；store-web：儿童档案（views/child/index.vue） |
| 状态 | 开发中（2026-09-18；后端已完成并 API 验证通过，前端实现中） |
| 后端改动 | careld-child-service（无 DDL：复用 child_profile.status 新增取值 2=已隐藏；新增认领/恢复接口、列表 statuses 多值过滤；2026-09-18 已重新编译并重启 8284） |

---

## 1. 需求清单（用户原话拆解）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 67-1 | 登录认领 | 登录用户手机号如系统中已存在，应自动列出该手机号对应的儿童档案；「我的」页家长姓名自动同步显示 | 后端（认领接口）+ parent-h5（登录/列表/我的联动） | ✅ 后端完成 |
| 67-2 | 建档表单 | 建立儿童档案时家长姓名栏自动带出当前用户姓名，不需二次输入 | parent-h5（child/form.vue） | 待复核（现状已部分满足） |
| 67-3 | 文案 | 按钮「+建立儿童档案」→「+添加儿童档案」 | parent-h5（child/index、appointment、pages.json） | 待实现 |
| 67-4 | 文案 | 档案卡片「可用次数为 0，请联系医院前台办理套餐充值」→「可用次数不足，请联系医院授权预约次数」 | parent-h5（child/index.vue） | 待实现 |
| 67-5 | 档案卡片与详情 | ① 卡片点击无效（只有「档案详情」按钮进详情）② 档案信息家长不可修改（仅医生端可改）③ 「养护记录」按钮后新增「删除档案」（删除后不可恢复，系统设为隐藏状态：家长不可见、医生端/医院端可查看并重新启用）④ 卡片与详情页「所属医院」→「建档医院」⑤ 姓名颜色按性别（男蓝 #2563eb / 女粉 #ec4899） | parent-h5（child/index、detail）+ doctor-h5 + store-web（隐藏档案恢复入口） | 待实现 |
| 67-6 | 预约流程 | ①「选择孩子」→「选择儿童」② 确认页「孩子」→「儿童姓名」③ 备注框改为一行左右结构，避免底部按钮被遮挡 | parent-h5（appointment/index.vue） | 待实现 |
| 67-7 | 同 67-1 | 登录手机号已存在时自动调取家长姓名与对应儿童档案 | 同 67-1 | 待实现 |

---

## 2. 用户拍板口径（8 条，实现必须遵守）

| # | 问题 | 拍板口径 |
|---|---|---|
| 1 | 「删除档案」的隐藏状态实现 | **child_profile.status 新增取值 2=已隐藏**（家长删除即置 2，不可恢复操作层面由医生端恢复；无需 DDL） |
| 2 | 手机号匹配到未绑定档案 | **自动绑定 parent_user_id 并带出**；已绑定其他家长的档案不抢占 |
| 3 | 账号姓名与档案家长姓名同步 | **仅当账号姓名为空或为默认名「家长+手机尾4位」时**，才用档案家长姓名回填账号 realName；否则不改 |
| 4 | 恢复入口 | **doctor-h5 + store-web 两端都要有**（查看「已隐藏」档案并「恢复」） |
| 5 | 删除边界 | **有未完成预约（已预约/养护中）时禁止删除**，提示「该儿童有尚未完成的预约，暂不能删除档案」 |
| 6 | 隐藏后历史数据 | **预约记录/养护记录/趋势等历史数据一并隐藏**（家长端不可见） |
| 7 | 文案范围 | **全端统一改**：「建立儿童档案」→「添加儿童档案」（parent-h5 3 处 + pages.json）；家长端所有「孩子」→「儿童」（含我的页「绑定孩子」、列表计数、登录页提示、预约流程） |
| 8 | 医生端筛选「全部」 | **不含已隐藏**：全部 = 待审核+已审核+已驳回+已禁用 四项；「已隐藏」为独立 chip |

---

## 3. 详细设计

### 3.1 后端 careld-child-service

#### 3.1.1 状态语义

`child_profile.status`：0=禁用、1=启用、**2=已隐藏（家长删除）**。列表接口默认 `status=1` 不变；家长视角（userType=3）强制按默认可见过滤，不可通过参数绕过查看隐藏/禁用档案。

#### 3.1.2 接口契约

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/children` | 新增 `statuses` 参数（逗号分隔，如 `0,1`）；有 statuses 时按 `status IN (...)` 过滤，优先于单值 `status`；医院端「全部」用 `statuses=0,1`（不含已隐藏） |
| DELETE | `/api/v1/children/{id}` | **语义变更为隐藏**（原逻辑删除）：校验归属与未完成预约后置 `status=2` |
| PUT | `/api/v1/children/{id}/restore` | 恢复隐藏档案（status 2→1）；家长调用 403；门店用户限本店 |
| POST | `/api/v1/children/claim-by-phone` | 家长按登录手机号认领未绑定档案，返回 `{claimedCount, parentName}` |

#### 3.1.3 校验规则（ChildServiceImpl）

- `hideProfile(id)`：
  - 家长（userType=3）仅能删除 `parentUserId == 当前用户` 的档案，否则 403「无权删除该档案」；门店用户限本店，总部/运营/代理放行；
  - `reserve_order` 中该儿童存在 `status IN (1=已预约, 2=养护中)` 且未删除的记录 → 400「该儿童有尚未完成的预约，暂不能删除档案」；
  - 已隐藏档案重复删除 → 400「档案已删除」。
- `restoreProfile(id)`：家长 403「无权操作该档案」；非隐藏状态 → 400「该档案未处于已隐藏状态」；门店归属复用 `checkDetailScope`。
- `claimByPhone()`：仅 userType=3；读 `sys_user.phone`（child-service 与 user 库同库直查）；`MaskUtil.maskPhone` 预筛 `parent_user_id IS NULL AND status <> 2 AND phone_mask = 掩码` 的候选；逐条解密精确比对（密文缺失/`ENC:` 占位退化为掩码一致即命中）；命中写 `parent_user_id`；返回 `claimedCount` 与首个命中的档案 `parentName`。幂等：重复调用 `claimedCount=0`。
- `getProfile(id)`：家长访问 `status=2` 档案 → 404「档案不存在」（等同不存在，防越权深链）。
- `listProfiles`：家长强制 `includeDisabled=false / status=null / statuses=null`（默认 status=1）。

#### 3.1.4 历史数据一并隐藏（口径 6）实现路径

家长端「预约记录 / 养护记录 / 趋势 / 报告」页均为**先拉 `getChildList({parentUserId})` 得儿童集合、再按 childId 查明细**的结构；隐藏档案被默认 `status=1` 过滤出儿童集合后，其历史数据自然不外显（趋势/报告页若经深链进入，详情接口对家长返回 404，页面按空态处理）。无需逐页改筛选逻辑，本批仅做验证。

### 3.2 parent-h5（7 小项落地）

#### 3.2.1 登录认领与姓名同步（67-1、67-7）

新增 `childApi.claimByPhone()`，调用点三处：登录成功跳转前（login/index.vue）、儿童档案页 `onShow`、我的页 `onShow`（覆盖冷启动/切页多路径）。认领返回 `parentName` 时按口径 3 处理：`userStore.userInfo.realName` 为空或匹配 `/^家长\d{4}$/` → 调 `userApi.updateMyProfile({ realName: parentName })` 后 `fetchProfile()`，我的页 `displayName` 随之显示真实家长姓名；否则不动账号姓名。

#### 3.2.2 建档页家长姓名预填（67-2）

现状：`onLoad` 中 `realName` 非「家长+尾4位」默认名时预填 `form.parentName`（与口径 3 一致的语义）。本批复核该逻辑即可，默认名场景保持不预填（账号名无信息量，避免覆盖档案已有家长姓名）。

#### 3.2.3 文案统一（67-3、口径 7）

- 「建立儿童档案」→「添加儿童档案」：`child/index.vue`（空态按钮、FAB）、`appointment/index.vue`（空态按钮）、`pages.json` 该页标题；共 3 处 + 标题。
- 「孩子」→「儿童」（家长端全量）：child/index 计数「共 N 个孩子」、mine「绑定孩子」、login 提示语、appointment 的步进标题「选择孩子」/确认页「孩子」/toast/confirm-tip/placeholder、相关代码注释。

#### 3.2.4 次数不足提示（67-4）

`child/index.vue` 卡片提示文案改为「可用次数不足，请联系医院授权预约次数」（保留 `remainingCount=0` 且已审核通过时展示的条件）。

#### 3.2.5 档案卡片与详情（67-5）

- 卡片去掉整卡点击（`child-head`/`child-meta`/`child-store` 三处 `@click="goDetail"` 移除），仅底部「档案详情」按钮进入详情。
- 「所属医院」→「建档医院」（卡片 + 详情页 kv）。
- 姓名颜色按性别：男孩 `#2563eb`、女孩 `#ec4899`（卡片姓名；详情页档案信息「儿童姓名」值同步着色，hero 区保持白字，请以截图验收为准）。
- 「删除档案」入口在详情页 sticky bar「养护记录」按钮之后（养护记录 / 删除档案 / 立即预约）：点击弹 `uni.showModal` 确认，文案含「删除后不可恢复，系统将设为隐藏状态，医生端可查看并重新启用」；确认后调 `childApi.deleteChild(id)`，成功后返回列表并刷新；后端 400/403 时 toast 后端 message（如「该儿童有尚未完成的预约，暂不能删除档案」）。
- 家长端档案信息本就全只读（无编辑入口），满足「家长不可修改，仅医生端可改」。

#### 3.2.6 预约流程（67-6）

- `STEP_LABELS`：`['选择孩子','选择时间','确认预约']` → `['选择儿童','选择时间','确认预约']`；确认页 kv「孩子」→「儿童姓名」；步骤注释与 toast（「请选择预约的孩子」→「请选择预约的儿童」）同步。
- 备注行改左右结构：`field-label` 与 textarea 同行（flex，label 定宽、textarea `flex:1` 且 `auto-height`），替换原 `.field-block` 上下结构，避免底部 sticky 按钮遮挡。

#### 3.2.7 API 层

`api/child.ts`：`getChildList` 参数加可选 `statuses`；新增 `claimByPhone()`（POST）与 `restoreChild(id)`（供医生端，见 3.3）；`deleteChild` 语义保持（后端已改为隐藏）。

### 3.3 doctor-h5（口径 4、8）

- `child/index.vue` 筛选 chips：新增「已隐藏」chip；`ALL_FILTER_VALUES`（「全部」互斥集合）**排除 hidden**；`filteredList` 优先处理 `status===2`（仅 hidden chip 命中，不计入审核状态集合）。
- 卡片：`status===2` 显示「已隐藏」tag（灰色系）。
- `child/detail.vue`：`status===2` 时禁用/启用按钮替换为「恢复档案」（调 `PUT /children/{id}/restore`，成功刷新详情）；列表页「已隐藏」chip 下可进入详情恢复。

### 3.4 store-web（口径 4、8）

- 状态筛选下拉：新增「已隐藏」(2)；「全部」不再用 `status:undefined + includeDisabled:true`（会含 2），改为 `statuses='0,1'`（statuses 优先级高于 status/includeDisabled，无需再传 includeDisabled）。
- 状态列：`status=2` 显示「已隐藏」tag（灰色）。
- 操作列：`status===2` 时显示「恢复」按钮（调 restore，成功刷新列表）；禁用/启用按钮仅 status 0/1 时显示。

---

## 4. 验证计划与结果

### 4.1 后端 API（2026-09-18 已执行，8284）

| 场景 | 请求 | 结果 |
|---|---|---|
| 认领 | 家长 13869108888（id=100007）POST /claim-by-phone | 200，`claimedCount=1`（child 46 手机号密文解密后精确匹配；child 38 同掩码但真实号码不同，未抢占），`parentName=杨沫沫` |
| 认领幂等 | 同账号重复调用 | 200，`claimedCount=0` |
| 删除-有未完成预约 | 家长删 child 46（4 条未完成预约） | 400「该儿童有尚未完成的预约，暂不能删除档案」 |
| 删除-非本人 | 家长删 child 38（未绑定该家长） | 403「无权删除该档案」 |
| 删除-本人 | 家长删 child 53（已绑定、无未完成预约） | 200，DB `status=2` |
| 家长可见性 | 家长 GET /children/53、家长列表 | 详情 404「档案不存在」；列表不含 53 |
| 医生列表 | store7 `includeDisabled=true`（无 statuses） | 含 53（status=2） |
| 医生列表 | store7 `statuses=0,1` | 不含 53 |
| 恢复-家长 | 家长 PUT /children/53/restore | 403「无权操作该档案」 |
| 恢复-医生 | 医生 PUT /children/53/restore | 200，DB `status=1`；重复恢复 400「该档案未处于已隐藏状态」 |

### 4.2 前端验证（2026-09-18 已执行）

类型检查：parent-h5 / doctor-h5 / store-web 三端 `npx vue-tsc --noEmit` 均 0 错误。

浏览器实测（parent-h5 5177 家长 13869108888 / doctor-h5 5176 store7 / store-web 5175 store7）：

| 端 | 场景 | 结果 |
|---|---|---|
| parent-h5 | 登录后「我的」页 | 姓名「杨小虎」（认领/同步生效）、手机号 138****8888、绑定儿童 2 个、剩余 7 次 |
| parent-h5 | 儿童档案列表 | 2 张卡片；「建档医院」文案；0 次卡片提示「可用次数不足，请联系医院授权预约次数」；底部「＋ 添加儿童档案」；「共 2 个儿童」 |
| parent-h5 | 卡片点击 | 点击卡片主体无跳转（仅「档案详情/立即预约」按钮可点） |
| parent-h5 | 姓名性别配色 | 女 周小平 `rgb(236,72,153)`、男 杨小虎 `rgb(37,99,235)`（含详情页「儿童姓名」值） |
| parent-h5 | 详情页底部 | 「养护记录 / 删除档案（红字 plain）/ 立即预约（0 次禁用）」；确认弹层文案「删除后不可恢复，系统将把档案设为隐藏状态：您将不再看到该儿童及其历史记录，医生端可查看并重新启用。」，删除按钮 #ef4444 |
| parent-h5 | 删除成功 | 确认后 toast「档案已删除」并返回上级页；列表仅剩 1 个儿童 |
| parent-h5 | 删除被拒 | 删 child 46（4 条未完成预约）→ toast「该儿童有尚未完成的预约，暂不能删除档案」，停留详情页，DB status 仍为 1 |
| parent-h5 | 历史数据一并隐藏 | 隐藏后：儿童档案 1 个、预约记录 5→4 条（周小平已完成消失）、养护记录 2→1 条（周小平消失）；tab 切换触发 onShow 重载即生效 |
| doctor-h5 | chips | 待审核/已审核/已驳回/已禁用/**已隐藏**/全部；默认待审核+已审核不含隐藏 |
| doctor-h5 | 「全部」不含已隐藏 | 14→13 张卡片，周小平不出现 |
| doctor-h5 | 「已隐藏」chip | 仅 1 张卡片，tag「已隐藏」（tag-grey） |
| doctor-h5 | 隐藏详情 + 恢复 | hero 显示「已隐藏」；底部按钮「编辑档案 / 恢复档案 / 预约授权」；确认弹层「确认恢复档案「周小平」吗？恢复后家长端将重新可见。」；恢复后 tag 消失、按钮变回「禁用」，DB status=1 |
| doctor-h5 | 恢复后列表 | 默认 chips 与「全部」（14 张）重新包含周小平；「已隐藏」chip 0 张 |
| store-web | 状态下拉 | 正常/已禁用/**已隐藏**/全部 |
| store-web | 默认「正常」 | 10 行不含隐藏档案 |
| store-web | 「全部」 | 共 13 条（14 - 1 隐藏），不含周小平 |
| store-web | 「已隐藏」 | 1 行；tag「已隐藏」（el-tag--warning）；操作列末位为「恢复」 |
| store-web | 恢复 | 确认框「确认恢复档案「周小平」吗？恢复后家长端将重新可见。」→ toast「已恢复」→ 该筛选下 0 行；DB status=1 |
| parent-h5 | 恢复后回显 | 儿童档案回复 2 个儿童（周小平重新可见） |

说明：浏览器自动化期间 in-app 浏览器视口不可见（`visibilityState=hidden`），页面驱动改用页面内 `evaluate_script` + DOM 事件完成（与既有 H5 验证手法一致）。

### 4.3 测试数据还原

- child 53：测试中两次置 `status=2` 并恢复，最终 `status=1`、`parent_user_id=100007`（均与原始一致）；
- child 46：认领测试将其 `parent_user_id` 写为 100007（原 NULL），2026-09-18 验证完成后已还原为 NULL（status=1 未变）；
- child 38 `parent_user_id=100008` 非本次测试写入（并发使用所致），未改动；
- 原值快照：`/tmp/child67_originals.txt`；认领为幂等操作，还原后家长再次登录会自动重新绑定。

---

## 5. 遗留与说明

1. child-service 无 Spring Security，仅靠轻量鉴权 Filter（与 schedule-service GuardFilter 同批待补的服务之一），本批新增接口的鉴权依赖 UserContext 注入（缺失时按未登录处理由全局异常拦截）——已在服务层对家长/门店角色做业务校验。
2. parent-h5 详情页 hero 区姓名保持白字（青绿底上蓝/粉对比度差），性别配色落在卡片姓名与详情「儿童姓名」值两处，如视觉上仍要求 hero 着色需再微调。
3. 「历史数据一并隐藏」依赖各记录页从儿童列表派生的既有结构；若后续新增按 childId 直查的历史页面，需同步补 status=2 过滤。
