# 医生端 doctor-h5 测试反馈第 2 批改造 Spec（第 58/59/60 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（医生端档案卡片、我的页面、档案筛选区） |
| 来源 | 测试反馈第 58/59/60 项（docs/测试记录.txt 第 121-130 行） |
| 涉及端 | 医生端 H5（doctor-h5）；后端 auth-service / user-service 各加 1 个字段 |
| 涉及页面 | 儿童档案列表（pages/child/index）、档案详情（pages/child/detail，顺带）、我的（pages/mine/index）、工作台（pages/home/index，顺带） |
| 状态 | 已开发（2026-09-18，vue-tsc + 浏览器（DOM/computed/组件交互）+ API 验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 58-a | 儿童档案列表 | 卡片上家长姓名后用括号显示与儿童关系，如「杨沫沫(妈妈)」 | doctor-h5 前端（模板） | ✅ 已完成 |
| 58-b | 儿童档案列表 | 「建档2026-09-16」改为「2026-09-16建档」 | doctor-h5 前端（模板） | ✅ 已完成 |
| 58-c | 儿童档案列表 | 儿童姓名按性别着色：男→蓝色、女→粉色 | doctor-h5 前端（样式） | ✅ 已完成 |
| 58-d | 儿童档案列表 | 性别文案「男」→「男孩」、「女」→「女孩」 | doctor-h5 前端（文案；详情页顺带同步） | ✅ 已完成 |
| 59-1 | 我的页面 | 姓名下「医务人员」→ 对应角色「医师」/「医生助理」 | doctor-h5 前端 + 后端（staffRole 透出；工作台抬头顺带同步） | ✅ 已完成 |
| 59-2 | 我的页面 | 「所属门店」→「所在医院」 | doctor-h5 前端（文案） | ✅ 已完成 |
| 59-3 | 我的页面 | 去掉「养护记录」「儿童档案」两个入口 | doctor-h5 前端（模板+脚本） | ✅ 已完成 |
| 59-4 | 我的页面 | 关于页「请联系门店管理员」→「请联系医院管理员」；最下行加「系统服务支持电话：400-999-3608」 | doctor-h5 前端（文案） | ✅ 已完成 |
| 59-5 | 我的页面 | 页面底部「Careld 医生端 V2.0.1」→「Careld 诊约助手医生端 V2.0.1」 | doctor-h5 前端（文案） | ✅ 已完成 |
| 60-a | 儿童档案页 | 筛选区去掉「全部」按钮 | doctor-h5 前端（模板） | ✅ 已完成 |
| 60-b | 儿童档案页 | 「含禁用」开关改为与「已通过」等相同样式的按钮，名称「已禁用」 | doctor-h5 前端（模板+样式） | ✅ 已完成 |
| 60-c | 儿童档案页 | 进入页面默认显示「已通过」和「待审核」的档案 | doctor-h5 前端（默认筛选态+本地过滤） | ✅ 已完成 |

用户原话见 docs/测试记录.txt 第 121-130 行。

---

## 2. 关键设计决策

1. **筛选 chip 改多选，默认「待审核 + 已通过」**：原为单选的 `auditStatus`（全部/待审核/已通过/已驳回）+ 独立「含禁用」开关。新设计为四个同款式 chip：待审核 / 已通过 / 已驳回 / 已禁用（`AUDIT_CHIPS`），多选切换；进入页面默认选中前两项（`DEFAULT_CHIPS = [0, 1]`），页面内导航往返保留选择。去掉「全部」后不设「不选=全部」的隐藏状态。
2. **「已禁用」chip 的语义**：选中时放行 `status=0` 的禁用档案（不限审核状态）；审核状态 chip 之间为并集（如「已通过 + 已禁用」= 已通过的启用档案 ∪ 全部禁用档案）。卡片上「已禁用」tag 展示逻辑不变。
3. **筛选为纯前端过滤**：列表接口本就返回门店全量数组（前端本地分页），改造后请求固定带 `includeDisabled=true` 拉全量，`filteredList` computed 按 chip 选择过滤，`共 N 条`/上拉加载均以过滤后集为准——零后端改动，避免为多值审核状态新增接口参数。
4. **至少保留一个筛选状态**：点击唯一处于选中态的 chip 时不再取消，弹出 toast「至少保留一个筛选状态」。防止出现「全部取消后列表空白」的迷惑状态（去掉「全部」按钮后无恢复入口）。
5. **角色文案改造范围**：我的页 hero 与「身份」行、工作台抬头「姓名 · 角色」三处同源于 `userRoleText(userType, staffRole)`——`userType=6` 时按 `staffRole` 输出「医师」(1) /「医生助理」(2)，缺失时回退「医务人员」；非医务人员按用户类型原样输出。工作台顺带修复（同一缺陷）。
6. **staffRole 数据来源（后端小改）**：医务人员登录响应与 `/users/me` 原本都不含 `staffRole`，两处各加一个字段（auth-service `LoginResponse.UserInfo`、user-service `UserResponse`），分别从 `medical_staff.staffRole` 取值；避免前端为拿一个角色再调 `medical-staff` 列表接口。
7. **姓名着色色值**：男孩 `#2563eb`（与主题蓝一致）、女孩 `#ec4899`；性别未知不着色（沿用默认 `#0f172a`）。
8. **日期文案**：`createdLabel()` 输出 `YYYY-MM-DD建档`；无建档时间时回退 `—`（不再出现「建档—」）。
9. **档案详情页性别文案顺带同步**（需求点名卡片，但详情 hero 同样展示性别，保持模块内一致：男/女 → 男孩/女孩/未知）；表单内性别选择项（录入控件）未动。
10. **关于弹窗底部电话行**加 `about-phone` 间距类（`margin-top: 16rpx`），与版本/联系文案视觉分层。

---

## 3. 实现要点

### 3.1 后端（2 个字段）

| 服务 | 文件 | 改动 |
|---|---|---|
| auth-service | `dto/LoginResponse.java`（UserInfo） | 新增 `staffRole` |
| auth-service | `service/impl/AuthServiceImpl.java` `generateStaffTokenResponse` | `userInfo.setStaffRole(staff.getStaffRole())` |
| user-service | `dto/UserResponse.java` | 新增 `staffRole` |
| user-service | `service/impl/UserServiceImpl.java` `getMedicalStaffCurrentUser` | `response.setStaffRole(staff.getStaffRole())` |

按规范 kill → `mvn package` → 启动（8281/8282，11:56:31 就绪）；打包前需先 `mvn install -pl careld-common -am`（本地仓 common 包缺 #75 新增的 `resolveStoreIdWithParentChoice`）。

### 3.2 儿童档案列表（`pages/child/index.vue`）

- 卡片：`child-name` 增加 `:class="genderNameClass(item.gender)"`（`name-boy` / `name-girl`）；性别文案 `男孩/女孩/未知`；建档行改 `createdLabel(item.createdAt)`（`2026-09-16建档`）；家长行改 `parentLabel(item)`（`家长姓名(关系)`，无关系时仅姓名、无姓名显示 `—`）。
- 筛选区：删除「全部」chip 与 `switch`（含 `switch-wrap`/`switch-label` 样式）；`AUDIT_CHIPS` = 审核三态 + `{ value: 'disabled', label: '已禁用' }`；`activeFilters` 默认 `[0, 1]`；`toggleFilter()` 含「至少保留一个」守卫（toast）；`filteredList` 本地过滤（禁用记录仅由「已禁用」chip 放行，启用记录按审核状态并集）。
- `load()` 固定 `includeDisabled: true`，去掉 `auditStatus` 参数。

### 3.3 我的页面（`pages/mine/index.vue`）

- `roleText = userRoleText(user.value?.userType, user.value?.staffRole)`（hero + 身份行共用）；删除「养护记录 / 儿童档案」入口卡片及 `goCareRecord`/`goChild`。
- 「所属门店」→「所在医院」；关于页文案「如需修改密码，请联系医院管理员重置。」+ 新增「系统服务支持电话：400-999-3608」；底部版本「Careld 诊约助手医生端 V2.0.1」。

### 3.4 其它前端文件

- `utils/dict.ts`：`STAFF_ROLE_MAP` 1 由「医生」改「医师」；新增 `userRoleText()` 统一角色文案。
- `pages/home/index.vue`：`roleText` 改用 `userRoleText`（原来 userType=6 硬编码「医务人员」）。
- `pages/child/detail.vue`：hero 性别文案 `男孩/女孩/未知`。
- `types/index.ts`：`User` 增加 `staffRole?: number`。

---

## 4. 验证记录（2026-09-18）

- `vue-tsc --noEmit` 通过。
- API：`POST /auth/login`（13788888888）与 `GET /users/me` 均返回 `staffRole: 2`；13399999998 返回 `staffRole: 1`（登录响应 + /users/me 两路径）。
- 浏览器（医生端 5176，张小丽=医生助理 / 胡春花=医师 实测）：
  - 卡片：id 46 显示「杨小虎 … 男孩 · 8岁 · 2026-09-16建档 / 杨沫沫(妈妈) · 138****8888」（与需求示例一致）；姓名 computed 色 `rgb(37,99,235)`（男）/`rgb(236,72,153)`（女，临时将测试档案 gender 改 2 验证后还原）/性别未知不着色；详情页 hero 显示「男孩」。
  - 筛选：chips = 待审核/已通过/已驳回/已禁用（无「全部」、无开关）；进入即默认前两项选中、共 11 条；取消「已通过」→ 仅待审核；加「已驳回」→ 并集；仅「已驳回」→ 空态；「已禁用」→ 仅禁用档案（含「已禁用」tag）；唯一选中项再点击不取消并提示「至少保留一个筛选状态」。
  - 我的页：hero「张小丽 / 医生助理」、身份行一致；「所在医院：武汉武昌协和卫生服务中心」；入口仅剩 关于/退出登录；关于弹窗四行文案含电话行；底部「Careld 诊约助手医生端 V2.0.1」。胡春花账号显示「医师」。
  - 工作台抬头：「胡春花 · 医师」（改造前显示「医务人员」）。
- 测试数据：临时改动 3 条（id27 status=0、id30 gender=2、id38 audit_status=0）验证后已全部还原并核对。

---

## 5. 遗留 / 待确认

1. 筛选去掉「全部」后，采用「至少保留一个筛选状态」守卫 + 多选并集语义（本 Spec 决策 1-4），如希望改为「不选=全部」或单选切换，可单独调整。
2. 姓名颜色色值（蓝 `#2563eb` / 粉 `#ec4899`）为按主题色选定，视觉走查后可再微调。
3. 详情页性别文案、工作台角色抬头为顺带同步（需求未点名），如不需要可单独回退。
4. 关于弹窗内「版本 v2.0.1（H5 / uni-app）」与页面顶部标题「Careld 医生端」未随 59-5 调整（需求点名「页面底部文字」）。
5. 代码未提交 git。
