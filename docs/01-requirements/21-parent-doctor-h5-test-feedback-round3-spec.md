# 家长端 parent-h5 + 医生端 doctor-h5 测试反馈第 3 批改造 Spec（第 61/62 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（家长端优化 + 医生端优化） |
| 来源 | 测试反馈第 61/62 项（docs/测试记录.txt 第 132-146 行） |
| 涉及端 | 家长端 H5（parent-h5）、医生端 H5（doctor-h5）；后端 auth-service / user-service / schedule-service |
| 涉及页面 | 家长端：儿童档案列表（pages/child/index）、建档（pages/child/form）、预约（pages/appointment/index）、我的（pages/mine/index，顺带）；医生端：工作台（pages/home/index）、儿童档案列表/详情（pages/child/index、detail）、预约授权（pages/child/grant）、我的（pages/mine/index） |
| 状态 | 已开发（2026-09-18，vue-tsc + API + 浏览器（DOM/组件交互/截图）验证通过） |

---

## 1. 需求清单（用户原话拆解）

### 1.1 #61 家长端优化（7 项）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 61-1 | 建档页 | 医院信息选择要包括全部医院（原来只能选上海市区域） | 后端 auth-service（登录限制，见下）+ 现有数据权限链路；前端无硬编码限制 | ✅ 已完成 |
| 61-2 | 儿童档案列表 | 建档后未审核通过时也要有卡片，让用户知道已提交、正在等待审核 | parent-h5 前端（卡片审核态展示） | ✅ 已完成 |
| 61-3 | 页面标题 | 「子女档案」→「儿童档案」；空态「还没有建立儿童档案」→「还没有建立任何档案」 | parent-h5 前端（pages.json + 空态文案） | ✅ 已完成 |
| 61-4 | 预约页 | 无孩子时预约页的提示和按钮样式与档案页一致 | parent-h5 前端（空态样式） | ✅ 已完成 |
| 61-5 | 建档页 | 录入了家长姓名则自动更新「我的」页面家长姓名 | parent-h5 前端（提交后同步；复用 PUT /users/me） | ✅ 已完成 |
| 61-6 | 建档流程 | 提交后医院端后台能看到且审核状态为「已通过」（无医生审核过程）、家长端却看不到该档案 | 同 61-1 根因；后端 auth-service 修复 + 端到端验证 | ✅ 已完成 |
| 61-7 | tabBar | 「档案」→「儿童档案」、「预约」→「预约养护」、「我的预约」→「预约记录」 | parent-h5 前端（pages.json） | ✅ 已完成 |

### 1.2 #62 医生端优化（5 项）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 62-1 | 预约授权页 | 缴费方式选择框改为与「开单医师」相同格式的下拉选择框 | doctor-h5 前端（grant.vue） | ✅ 已完成 |
| 62-2 | 儿童档案 | 性别为女时显示「女孩」而非「未知」，名字粉色 | doctor-h5 前端（列表 + 详情） | ✅ 已完成 |
| 62-3 | 我的页面 | 手机号可手动修改；账号下一行添加「修改密码」（需验证旧密码） | doctor-h5 前端 + 后端 user-service（2 个接口） | ✅ 已完成 |
| 62-4 | 工作台 | 去掉今日预约数据卡片，增加当月日历，每日显示 A/B（A=已预约、B=可预约，为 0 显示「-」） | doctor-h5 前端 + 后端 schedule-service（按天聚合接口） | ✅ 已完成 |
| 62-5 | tabBar | 「预约」→「预约记录」、「档案」→「儿童档案」 | doctor-h5 前端（pages.json） | ✅ 已完成 |

用户原话见 docs/测试记录.txt 第 132-146 行。

---

## 2. 关键设计决策

### 2.1 #61(1)(2)(6) 同一根因：测试账号身份错误

三项反馈实际是**同一个根因**：测试时用 13621240199（杨东，医院管理员 `user_type=2`）走家长端短信登录。由此产生三个现象：

1. **(1) 医院只显示上海市区域**：建档页地区/医院选项由 `GET /stores/all`（家长端 `storeApi.getAllStores()`）动态生成（`rebuildColumns()` 从门店列表提取省/市/区去重）。该接口按登录身份做数据权限过滤：`user_type=2` 时 scopeStoreId=本医院 → 只返回 1 家门店 → 地区只剩该门店所在省市区。**并非前端硬编码限制**。
2. **(2) 无「待审核」卡片**：`ChildController.create` 按 `UserContext.getCurrentUserType()` 判来源——`userType=3`（家长）才置 `sourceType=1`+`parentUserId`，`ChildServiceImpl` 对其设 `auditStatus=0`（待审核）；非家长身份置 `sourceType=2`+`sourceUserId` 并**自动** `auditStatus=1`（不需要审核）。用医院管理员账号建档 → 直接「已通过」，自然不会出现待审核卡片。
3. **(6) 医院端显示「已通过」且家长端看不到**：同上，`sourceType=2` 的医院端建档语义为「医院侧录入、免审核」，故后台直接显示已通过且无医生审核过程；家长端列表以 `parentUserId`（家长建档来源）为准，看不到这条医院侧档案。

**修复方案（后端小改，auth-service）**：家长端短信登录只放行家长账号——`smsLogin` 中手机号匹配到账号后，若 `userType != 3` 抛 `1006「该手机号不是家长账号，请使用家长手机号登录」`。该判断位于验证码核销（`markUsed`）之后、任何 DB 写之前：不修改 `last_login_time`、不累计登录失败锁定。修复后家长用真实家长账号登录：建档走 `sourceType=1` → 待审核（卡片可见、医院端也显示待审核）；`GET /stores/all` 返回全部启用门店 → 地区选项覆盖全部医院。

### 2.2 其余设计决策

1. **待审核卡片（61-2）**：档案列表本就不过滤审核状态（后端 `listProfiles` 仅显式传参才过滤），卡片始终渲染；审核态以 tag + 提示行表达：`auditStatus!==1` 显示 tag（待审核/已驳回）、驳回显示原因、待审核显示「档案待医院审核，审核通过后即可预约养护」、已通过但可用次数为 0 显示充值提示。「可预约」判定 = 已通过且剩余次数 > 0（未通过不可预约）。
2. **空态一致性（61-4）**：预约页空态与档案页采用完全相同的结构/文案/样式（`.empty`/`.empty-icon`/`.empty-text`「还没有建立任何档案」/`.empty-btn`「建立儿童档案」；按钮 `linear-gradient(135deg,#2dd4bf,#14b8a6)` 主色渐变胶囊），仅跳转目标不同（建档页）。
3. **家长姓名同步（61-5）**：建档提交成功后调用 `PUT /users/me`（user-service 本人姓名接口，上一批次已提供）同步当前账号 `real_name`，成功后刷新用户 store；与现有姓名相同则跳过；同步失败不阻塞建档（catch 静默）。建档接口本身不变。
4. **性别文案与配色（62-2）**：全系统 gender 约定为 **1=男、0=女（无 2）**。上一批次误写为 `=== 2` 导致女童显示「未知」，本批修正为 `gender === 0 ? '女孩'`，名单色：男孩 `#2563eb`、女孩 `#ec4899`、未知不着色；列表与详情同步。
5. **缴费方式下拉（62-1）**：由独立按钮组改为与「开单医师」一致的 `picker mode="selector"` + `.select-box` 样式（统一下拉框视觉），选项来自 `PAYMENT_METHODS`，选中项回填 `form.paymentMethod`（计费联动逻辑不变）。
6. **修改密码/手机号（62-3）**：医生（`userType=6`，userId 为 medical_staff 主键，与 sys_user 无关联）走单独分支——改密 `POST /users/me/password`（校验旧密码 BCrypt、新密码 ≥6 位，旧密码错误返回 4003）；改号 `PUT /users/me/phone`（仅医务人员可用，格式 `^1\d{10}$` + 本院 medical_staff 内唯一 + 与 sys_user 撞号校验）。因手机号即登录账号，页面提示「修改成功后需使用新手机号重新登录」。
7. **工作台日历（62-4）**：删去 4 张今日预约统计卡片，改为当月「预约日历」卡；每日显示 `A/B`（A=Σbooked_count、B=Σ(max_capacity−booked_count)，仅统计启用时段 `status=1`），A 或 B 为 0 显示「-」；无排班日显示「-/-」；已过日期弱化显示。数据由新增 `GET /schedule-rules/slot-daily-summary?startDate&endDate[&storeId]` 一次取当月（门店缺省取当前登录医生所属门店）。
8. **tabBar 文案（61-7/62-5）**：两端 tabBar 与页面级 `navigationBarTitleText` 同步调整（家长端「儿童档案/预约养护/预约记录」；医生端「预约记录/儿童档案」），预约记录页标题一并统一。

---

## 3. 实现要点

### 3.1 后端（3 个服务）

| 服务 | 文件 | 改动 |
|---|---|---|
| auth-service | `service/impl/AuthServiceImpl.java` `smsLogin` | 手机号匹配账号后新增 `userType != 3` → `BusinessException(1006, "该手机号不是家长账号，请使用家长手机号登录")`（未注册手机号仍自动创建家长账号） |
| user-service | `controller/UserController.java` | 新增 `PUT /me/phone`（仅 userType=6，否则 400「当前身份不支持此操作」）；`POST /me/password` 增加 userType=6 → `changeMyStaffPassword` 分支 |
| user-service | `service/impl/UserServiceImpl.java` | 新增 `changeMyStaffPassword`（404 医务人员不存在 / 4003 原密码错误 / 400 新密码至少6位）与 `updateMyStaffPhone`（格式校验 / 本院唯一 `selectByStoreAndPhone` / sys_user 撞号 `selectByPhone`） |
| schedule-service | `mapper/ScheduleSlotMapper.java` | 新增 `sumDailyByRange`：`SELECT slot_date, SUM(booked_count) booked, SUM(max_capacity-booked_count) available FROM schedule_slot WHERE store_id=? AND slot_date BETWEEN ? AND ? AND status=1 GROUP BY slot_date ORDER BY slot_date` |
| schedule-service | `controller/ScheduleRuleController.java` | 新增 `GET /schedule-rules/slot-daily-summary`（storeId 可选，`DataScopeHelper.resolveStoreId` 兜底，缺省抛「医院不能为空」） |
| schedule-service | `service/impl/ScheduleRuleServiceImpl.java` | `getSlotDailySummary` 输出 `[{date, booked, available}]`（null 计 0） |

> 家长跨院读取（`resolveStoreIdWithParentChoice`）与 `PUT /users/me` 本人姓名接口为上一批次（#55/#56）已交付，本批直接复用。

### 3.2 parent-h5（#61）

- `src/pages.json`：tabBar「儿童档案 / 预约养护 / 预约记录」，档案页 `navigationBarTitleText="儿童档案"`。
- `src/pages/child/index.vue`：卡片审核态展示（tag + 驳回原因 + 待审核提示 + 次数为 0 提示）；空态文案「还没有建立任何档案」；`canReserve` = 已通过且次数 > 0。
- `src/pages/appointment/index.vue`：空态（图标/文案/按钮）与档案页逐字一致，按钮跳建档页。
- `src/pages/child/form.vue`：提交成功后 `syncAccountName(form.parentName)`（`userApi.updateMyProfile` → `PUT /users/me` + `userStore.fetchProfile()`，失败静默）；成功后 toast「已提交，待医院审核」并返回；地区/医院选项由 `loadStores()`（`GET /stores/all`）动态构建（`rebuildColumns` 省→市→区级联，随门店数据集自适应，无硬编码）。

### 3.3 doctor-h5（#62）

- `src/pages/child/grant.vue`：缴费方式改 `picker mode="selector"` + `.select-box`（与开单医师一致），`onMethodChange` 回填。
- `src/pages/child/index.vue`、`detail.vue`：`genderText` = `1→男孩 / 0→女孩 / 其它→未知`；`genderNameClass` 名单色 `name-boy #2563eb` / `name-girl #ec4899`。
- `src/pages/mine/index.vue`：账号区新增「修改密码」行（弹层：旧密码/新密码/确认新密码 → `userApi.changePassword`）与手机号行（弹层：新手机号，预填当前号 → `userApi.changePhone`），提示改号后需重新登录。
- `src/api/auth.ts`：新增 `changePassword`（`POST /users/me/password`）、`changePhone`（`PUT /users/me/phone`）。
- `src/pages/home/index.vue`：删除 4 张今日预约统计卡；新增「预约日历」卡（当月网格，每日 A/B，0 显示「-」，无排班 `-/-`）；`src/api/reserve.ts` 新增 `getSlotDailySummary`。
- `src/pages.json`：tabBar「预约记录 / 儿童档案」。

### 3.4 构建与重启

kill 旧进程 → `mvn package` → 启动：auth-service 8281、user-service 8282、schedule-service 8285（父 POM 2.0.1；common 包有新增方法时先 `mvn install -pl careld-common -am`）。前端 doctor-h5 5176、parent-h5 5177 走 Vite dev（免构建）。

---

## 4. 验证记录（2026-09-18）

### 4.1 类型检查与接口

- `vue-tsc --noEmit`：parent-h5、doctor-h5 均通过。
- 接口：
  - `POST /auth/sms/...` + `POST /auth/sms-login`（13621240199 杨东）：返回 1006「该手机号不是家长账号，请使用家长手机号登录」；真实家长账号登录正常。
  - `GET /stores/all`（家长 token）：返回全部 3 家**启用**医院（杭州西湖社区卫生服务中心 / 武汉武昌协和卫生服务中心 / 上海东方中医院），地区覆盖浙江/河北/上海（门店库共 10 家，另 7 家为禁用状态，不参与展示）。
  - `POST /children`（家长 token）：落库 `source_type=1`、`audit_status=0`（待审核），医院端后台可见且为待审核。
  - `GET /schedule-rules/slot-daily-summary?startDate&endDate`（医生 token）：逐日 booked/available 与 DB `schedule_slot` 聚合值一致。
- 修改密码 / 修改手机号接口：按「严禁修改任何账号密码」约束未做提交实测，弹层交互与入参逻辑以代码审阅 + 浏览器弹层验证为准（校验分支：4003 旧密码错误 / 400 新密码不足 6 位 / 400 手机号格式或占用）。

### 4.2 浏览器（DOM/组件交互/截图）

- **doctor-h5（5176）**：
  - 工作台：统计卡已移除，日历显示 2026 年 9 月，今日 8/2，无排班日 `-/-`，与 DB 逐日核对一致。
  - 儿童档案：女童显示「女孩」且姓名粉色（男童蓝色）实测。
  - 预约授权：缴费方式为与开单医师一致的「选择框」下拉样式。
  - 我的页：账号 13399999998；「修改密码 去修改 ›」打开弹层（旧/新/确认三输入框）；手机号行可点，弹层标题「修改手机号」、提示「手机号即登录账号，修改成功后需使用新手机号重新登录。」、预填当前号；**未提交任何修改**。
- **parent-h5（5177）**：
  - tabBar：儿童档案 / 预约养护 / 预约记录，页面级标题同步。
  - 建档页：地区 picker 含浙江/河北/上海（即 3 家启用医院所在地区，#61-1）；选择医院后主治医师联动正常。
  - 家长账号（13621240158）登录：列表显示「待审核」tag 与「档案待医院审核，审核通过后即可预约养护」卡片（#61-2/#61-6），与医院端后台该档案待审核状态一致；未通过档案点预约提示「档案待医院审核，审核通过后即可预约」。
  - 新账号空态：档案页与预约页提示+按钮完全一致（#61-3/#61-4）。
  - 端到端建档 3 次（测试小娃/测试小娃二/测试小娃三）：均 `audit_status=0` 待审核、家长端提交后立即可见（#61-2/#61-6）；第 3 次录入新家长姓名「王测试新」→ 「我的」页姓名自动更新（#61-5；此前两次分别验证了「与现名相同则跳过」的防重复行为与 Vite dev proxy 偶发 30s 挂起非代码问题）。
- **测试数据清理**：验证产生的 `child_profile` id=50/51/52 与临时账号 `sys_user` id=100005 已物理删除（删前核对 child_service_record / care_record / reserve_order / vision_test_record / sys_user_role / sys_operation_log 关联均为 0），清理后无残留；浏览器 localStorage token 清理回登录页。

---

## 5. 遗留 / 待确认

1. **#61-6 历史数据**：测试账号杨东（13621240199）此前建档产生的那条医院侧档案（child_profile id=49）保留未动（历史测试数据，非本次清理范围）。
2. **启用医院范围**：门店库共 10 家，`GET /stores/all` 只返回「启用」（status=1）的 3 家（杭州西湖社区卫生服务中心 / 武汉武昌协和卫生服务中心 / 上海东方中医院）。如需更多医院出现在家长端建档选项，属管理后台门店管理→「启用」状态配置（后台配置类操作按步骤给出、不代改数据）。
3. **门店 7 省市区与名称不一致（测试数据）**：「武汉武昌协和卫生服务中心」库中省市区字段为河北省/唐山市/古冶区，故家长端地区选项出现「河北」；属测试数据问题，可在门店管理修正省市区。
4. **家长端登录限制**：非家长账号将无法登录家长端（1006）。医院人员如需用家长端，需另以家长身份注册（手机号即账号，一人一号）。
5. **改号后的会话**：医生修改手机号后，旧 token 在过期前仍可用，新登录须用新号（已用页面文案提示）。
6. **日历口径**：A/B 为门店级全量时段汇总；无排班日显示「-/-」（需求仅规定 A 或 B 为 0 显示「-」，此处取一致处理），如希望无排班日显示其它样式可单独调整。
7. 代码未提交 git（本批次含两端 H5 与后端三个服务改动）。
8. 本地 Vite dev proxy 偶发请求挂起（约 30s 超时、transfer=0）为环境问题，已用 curl/直连复测排除代码原因。
