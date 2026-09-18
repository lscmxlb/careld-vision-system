# Careld 医生端 H5 开发方案（uni-app）

> 日期：2026-09-15
> 版本：v0.2（讨论稿，按用户决策改为 uni-app 方案，待逐点拍板后定稿）
> 决策：**移动端（医生端、家长端）统一采用 uni-app（Vue 3）开发**，一套代码可同时输出 H5 / 微信小程序 / App，为后续上小程序做准备；PC 端（admin-web / store-web）保持 Vue 3 + Vite + element-plus 不变
> 前提：PC 医院端（store-web）功能已基本完成，本方案以其功能与现行后端接口为基准
> 关联文档：`02-architecture/05-child-care-service-spec.md`（现行业务体系）、`02-architecture/04-doctor-miniapp-spec.md`（2026-08-10 旧版，基于已废弃体系，本文档取代其设计）
> 状态：**已按本文档完成 P0–P3 开发并通过本地验证（2026-09-15）**；验证方式：`vue-tsc --noEmit` 0 错误、`npm run build:h5` 成功、本地浏览器全链路走查（登录 → 工作台 → 今日预约 → 开始/完成养护 → 预约管理/档案/养护记录/我的 → 退出登录）。部署动作（Q7）未执行。

---

## 一、产品定位

医生端是医院医护人员（医生、医生助理）的**移动工作端**，一期以 H5 形态交付（手机浏览器/微信内打开），后续可编译为微信小程序、App 发布。PC 医院端继续承担管理类低频操作。

| 维度 | 说明 |
|------|------|
| 使用人群 | 医务人员（`medical_staff`，staffRole 1=医生 2=医生助理），店长账号可登录使用（待确认 Q6） |
| 使用场景 | 到院接诊、养护服务现场：看今日预约、开始/完成养护、录视力、新建预约、查档案、授权次数 |
| 与 PC 关系 | 同一套后端接口、同一门店数据权限，两端操作结果实时一致 |
| 不做的事 | 数据看板图表、排班规则编辑、批量排班日历、科室管理、预约规则配置、医务人员管理、设备管理（均留在 PC） |

---

## 二、范围决策（做 / 不做）

### 2.1 本期做

| 模块 | 功能 |
|------|------|
| 登录 | 手机号 + 密码（复用现有医务人员账号体系，账号=手机号，密码由管理员初始设置） |
| 工作台 | 门店名 + 医生名 + 今日日期、今日预约统计、今日预约列表（含快捷操作） |
| 预约管理 | 全量预约列表（日期/状态筛选）、新建预约（选儿童→选日期→选时段）、开始养护、完成养护、取消、爽约、调整时段 |
| 儿童档案 | 档案列表（姓名/手机号搜索）、档案详情（基本信息/可用次数/服务记录/预约记录/养护记录）、新建/编辑档案、授权次数（服务记录，自动计费） |
| 养护记录 | 列表（姓名搜索）、详情（养护前/后视力、执行人、实际时段） |
| 我的 | 姓名/角色/门店展示、修改密码（待确认 Q5）、退出登录 |

### 2.2 本期不做（明确排除）

- **排班规则编辑**：区间规则表单在手机上体验差且低频，仅在预约新建时通过 `slots`/`available-dates` 只读消费排班结果。
- **批量排班日历**、**基础信息**、**医务人员管理**、**设备管理**：低频管理功能，留 PC。
- **数据看板图表**：留 PC。
- **微信快捷登录 / 订阅消息推送**：二期（小程序形态发布时再做，见 4.8 跨端策略）。

---

## 三、技术选型（uni-app）

| 层级 | 方案 | 说明 |
|------|------|------|
| 框架 | **uni-app（Vue 3 组合式 API）+ Vite + TypeScript** | CLI 工程模式（`dcloudio/uni-preset-vue#vite-ts` 模板），与现有前端同用 Vue 3 语法，跨端输出 H5 / 微信小程序 / App |
| UI 组件库 | **待拍板（Q1）**：uView Plus 3（功能全、移动风格，推荐）或 uni-ui（官方组件，轻量） | element-plus / Vant 4 不支持小程序端，不采用 |
| 状态管理 | Pinia | uni-app 官方支持，与现有项目一致 |
| 路由 | `pages.json`（页面注册 + tabBar 配置），API 用 `uni.navigateTo/redirectTo/switchTab` | 不用 vue-router；注意小程序页面栈 10 层限制，深链用 redirectTo |
| 请求层 | 自研 `uni.request` Promise 封装（拦截器注入 `Authorization: Bearer <accessToken>`、统一错误提示、401 跳登录） | 替代 axios（小程序端无 XHR）；封装逻辑对齐现有 `api/request.ts` 语义 |
| 存储 | `uni.setStorageSync/getStorageSync`（token、用户信息） | H5 下映射 localStorage，小程序下自动适配 |
| 图表 | 工作台/详情均不需要图表，暂不引入 | 若后续要趋势图再引入 qiun-data-charts（ucharts） |
| 工程目录 | `frontend/doctor-h5/`，H5 开发端口 5176（避开 store-web 5175 / admin-web 5172） | H5 代理在 `manifest.json` → h5 → devServer 配置，指向本地后端 |
| 工具 | VSCode 开发；小程序打包用 HBuilderX 或 CLI（`build:mp-weixin`）+ 微信开发者工具 | 一期只发 H5 |

---

## 四、页面设计

### 4.1 页面结构（pages.json）

```
frontend/doctor-h5/
├── src/
│   ├── pages/
│   │   ├── login/index.vue          # 登录（无 tabBar）
│   │   ├── home/index.vue           # 工作台（tabBar 首页）
│   │   ├── reserve/
│   │   │   ├── index.vue            # 预约列表（tabBar）
│   │   │   ├── create.vue           # 新建预约（三步弹层或独立页）
│   │   │   └── detail.vue           # 预约详情（动作面板）
│   │   ├── child/
│   │   │   ├── index.vue            # 档案列表（tabBar）
│   │   │   ├── detail.vue           # 档案详情（Tab：基本信息/服务记录/预约记录/养护记录）
│   │   │   ├── form.vue             # 新建/编辑档案
│   │   │   └── grant.vue            # 授权次数（弹层）
│   │   ├── care-record/
│   │   │   ├── index.vue            # 养护记录列表（档案模块内二级页）
│   │   │   └── detail.vue           # 养护记录详情
│   │   └── mine/index.vue           # 我的（tabBar）
│   ├── api/                         # uni.request 封装 + 各模块 api（auth/child/reserve/schedule-rule/care-record/user/store）
│   ├── stores/                      # user.ts 等 Pinia
│   ├── utils/                       # 工具函数（日期/状态字典/格式化）
│   ├── components/                  # 状态标签、预约卡片、弹层等公共组件
│   ├── styles/                      # 全局样式
│   ├── App.vue / main.ts
│   ├── pages.json / manifest.json
│   └── uni.scss
└── package.json
```

tabBar 四入口：**工作台 / 预约 / 档案 / 我的**（养护记录放「档案」Tab 下作为二级入口，与 PC 端"档案管理组"结构对齐）。

### 4.2 工作台

- 统计口径与 PC 数据看板一致：取 `GET /schedules/reserves?reserveDate=今日` 前端按状态聚合，不新增后端接口。
- 今日预约卡片按时间升序，状态标签复用 PC 颜色语义（已预约=黄/养护中=蓝/已完成=绿/已取消=灰/已爽约=红）。
- 卡片快捷操作：
  - 已预约 → [开始养护]（主）、[取消] [爽约] [调整]
  - 养护中 → [完成养护]（主）
  - 已完成/已取消/已爽约 → 查看详情

### 4.3 预约操作闭环（核心，与 PC 端规则完全一致）

| 操作 | 触发状态 | 弹层内容 | 后端接口 | 规则要点 |
|------|---------|---------|---------|---------|
| 新建预约 | — | 选儿童（搜索选择）→ 日期 → 时段（`slots`，满额禁用）→ 确认 | `POST /schedules/reserves/v2` | 后端强校验：已审核 + 次数>0 + 时段合法 + 满额 + 每日一约；成功当场扣次数 |
| 开始养护 | 已预约(1) | 执行人（医师/医生助理下拉）+ 养护前视力（左/右/双眼）+ 开始 | `POST /schedules/reserves/{id}/start` | 校验已到预约时段；状态→养护中；同事务创建养护记录 |
| 完成养护 | 养护中(2) | 养护后视力（左/右/双眼）+ 完成 | `POST /schedules/reserves/{id}/complete` | 状态→已完成；养护记录置已完成 |
| 取消 | 已预约(1) | 取消原因（必填） | `POST /schedules/reserves/{id}/cancel` | 医生时间窗校验；时间窗内取消自动退还次数 |
| 爽约 | 已预约(1) | 是否退还次数（选择） | `POST /schedules/reserves/{id}/no-show` | 逾期未开始由医生标记；退还与否按选择 |
| 调整 | 已预约(1) | 新日期 + 新时段 | `POST /schedules/reserves/{id}/adjust` | 逾期预约禁止调整（后端校验，前端同样禁用） |

- 弹层组件用 `uni-popup`/uView `u-popup`（底部动作面板式），列表项操作不跳新页。
- 未保存数据脏保护：新建预约弹层、开始/完成养护弹层在填写中途关闭时弹确认（对齐 PC 端第 42–45 项反馈的脏保护习惯）。
- 逾期预约（已过预约时段未开始）在卡片上打「逾期」角标，取消/调整按钮禁用，仅可爽约。

### 4.4 儿童档案

- **列表**：`GET /children?keyword=` 分页；卡片显示姓名（后端已回填明文）、性别/年龄、手机号（脱敏）、可用次数（`remainingCount`）、建档日期。
- **详情**：`GET /children/{id}` 基本信息；Tab 内复用接口：
  - 服务记录：`GET /children/{id}/service-records`（流水：授予/扣减/退还）
  - 预约记录：`GET /schedules/reserves?childId=`
  - 养护记录：`GET /care-records/page?childId=`
- **新建/编辑**：移动表单字段集**待拍板（Q3）**。建议精简集：姓名、性别、出生日期、家长手机号、视力状况（多选）、裸眼视力（下拉 5.3–4.0）；其余字段（眼位/分娩方式/日常作息/既往病史/过敏信息）按 PC 规则可折叠或省略，保持与 PC 存储格式一致（多选逗号分隔、加密字段走后端）。
- **授权次数**：弹层输入授予次数 + 备注；金额按 PC 同规则自动计费（取门店第一条启用科室的收费标准），调 `POST /children/{id}/service-records`。展示当前可用次数并实时刷新。

### 4.5 养护记录

- 列表 `GET /care-records/page`（姓名搜索）；卡片显示日期 + 实际养护时段（后端已按开始/完成时间计算）、儿童姓名、执行人、状态。
- 详情 `GET /care-records/{id}`：养护前/后视力（左/右/双眼）对比展示、执行人、备注。

### 4.6 我的

- 身份信息取 `GET /users/me`（医务人员按 medical_staff.id 返回本人，含 storeName）。
- 修改密码：现行后端仅有管理员重置接口（`PUT /medical-staff/{id}/password`），**无自助改密端点**，见 Q5。
- 退出登录：`POST /auth/logout` + 清理本地 token。

### 4.7 组件选型映射（uView Plus / uni-ui 口径）

| 页面元素 | PC 端（element-plus） | 移动端（uni-app） |
|---------|----------------------|-------------------|
| 弹窗 | el-dialog | u-popup / uni-popup（底部动作面板） |
| 表单 | el-form | u-form / uni-forms |
| 日期选择 | el-date-picker | u-datetime-picker / picker（date） |
| 时间选择 | el-time-picker | picker（time） |
| 下拉选择 | el-select | u-picker / picker |
| 多选标签 | el-checkbox-group | u-checkbox-group |
| 卡片列表 | el-table | 自定义卡片 + u-loadmore（分页加载） |
| 提示 | ElMessage | uni.showToast / u-toast |
| 确认框 | ElMessageBox | uni.showModal |

### 4.8 跨端策略（H5 先行，小程序适配预留）

- 所有业务代码**不写平台专属 API**，统一走 uni.* 与条件编译约定：
  - 登录：一期 H5 用账密表单；小程序形态发布时用 `#ifdef MP-WEIXIN` 增加 wx.login + 手机号授权快捷登录（对应后端二期新增接口）。
  - 请求域名：小程序要求 HTTPS + 微信公众平台域名白名单，部署时在 `manifest.json` 配置；后端需 HTTPS 网关（部署注意事项，开发期不受影响）。
  - 页面跳转统一封装（内部处理 navigateTo/redirectTo 选择），避免小程序页面栈 10 层溢出。
- 打包脚本：`dev:h5` / `build:h5` / `build:mp-weixin`（后者需微信开发者工具）。

---

## 五、接口复用清单

全部复用现有微服务，**预期零后端开发**（自助改密除外，见 Q5）。

| 页面/功能 | 接口 | 所属服务 |
|------|------|---------|
| 登录 / 刷新 / 登出 | `POST /api/v1/auth/login`、`/refresh`、`/logout` | auth-service |
| 当前用户 | `GET /api/v1/users/me` | user-service |
| 工作台/预约列表 | `GET /api/v1/schedules/reserves`（date/statuses/childId/keyword 筛选 + 分页） | schedule-service |
| 新建预约 | `POST /api/v1/schedules/reserves/v2`（childId + slotId） | schedule-service |
| 可约日期/时段 | `GET /api/v1/schedule-rules/available-dates`、`/slots` | schedule-service |
| 开始/完成/取消/爽约/调整 | `POST /api/v1/schedules/reserves/{id}/start|complete|cancel|no-show|adjust` | schedule-service |
| 预约关联养护记录 | `GET /api/v1/schedules/reserves/{id}/care-record` | schedule-service |
| 档案列表/详情/新建/编辑 | `GET|POST /api/v1/children`、`GET|PUT /api/v1/children/{id}` | child-service |
| 档案选择（预约用） | `GET /api/v1/children/search`（或 pick-options） | child-service |
| 可用次数 | `GET /api/v1/children/{id}/remaining-count` | child-service |
| 授权/服务记录 | `POST|GET /api/v1/children/{id}/service-records` | child-service |
| 养护记录 | `GET /api/v1/care-records/page`、`GET /api/v1/care-records/{id}` | vision-service |
| 医务人员选项（开始养护执行人） | `GET /api/v1/medical-staff`（现有列表接口，取本店启用人员） | user-service |
| 科室收费标准（授权自动计费） | `GET /api/v1/departments?storeId=`（取第一条启用科室） | store-service |

> 注：医务人员列表接口目前是 PC 医务人员管理页专用，医生端需要的仅是"本店启用人员"下拉选项，若无合适端点可复用 `pick-options` 风格新增一个只读接口（唯一可能的增量点，开发时核对）。

---

## 六、权限与角色

- 登录账号沿用医务人员体系：账号=手机号，密码由管理员在 PC 医务人员页设置（默认 `4009993608`）。
- 医生（staffRole=1）与医生助理（staffRole=2）在移动端的可见功能差异**待确认（Q4）**；后端接口已带身份校验，前端仅按 `staffRole` 控制入口显隐。
- 数据权限与 PC 一致：仅见本店（storeId）档案/预约/养护数据，后端自动过滤。

---

## 七、开发分期

| 阶段 | 内容 | 说明 |
|------|------|------|
| P0（核心闭环） | uni-app 工程初始化（CLI 模板 + UI 库 + 请求封装 + pages.json 骨架）+ 登录 + 工作台 + 今日预约 + 开始/完成养护（含前后视力录入） | 跑通接诊主链路即可上线试用的最小集 |
| P1（预约管理） | 预约全量列表（日期/状态筛选）+ 新建预约 + 取消/爽约/调整 | 对齐 PC 预约记录页操作面 |
| P2（档案与养护记录） | 档案列表/详情/新建/编辑/授权计费 + 养护记录列表/详情 | 对齐 PC 档案管理组 |
| P3（收尾） | 我的（改密待 Q5）+ 下拉刷新/角标等体验打磨 + H5 部署 | |
| 二期（小程序） | `build:mp-weixin` 编译调试、微信快捷登录、订阅消息、域名白名单/HTTPS 部署 | 视上线计划启动 |

---

## 八、验收标准

1. H5 形态在手机真机全流程可用（登录、工作台、预约闭环、档案、养护记录）。
2. 与 PC 端同一门店数据实时一致（两端交叉操作：H5 开始养护 → PC 列表状态同步）。
3. 关键操作（新建/开始/完成/取消/爽约/授权）在移动端与 PC 结果一致，含次数扣减/退还。
4. 代码全程走 uni.* API 与条件编译规范，无平台专属写法，`build:mp-weixin` 可编译通过（预留验证，不要求本期发小程序）。
5. vue-tsc 类型检查 + API 对账 + 浏览器自动化验证（沿用现有三层验证方式）。

---

## 九、待确认项（请逐点拍板）

> 执行说明（2026-09-15）：各项按"建议"列默认值实施——Q1 采用 uView Plus 3；Q2 排班规则编辑不进移动端（仅只读消费 `slots`/`available-dates`）；Q3 档案表单采用精简字段集；Q4 与 PC 现状一致（医生/医生助理均可登录操作，无额外限制）；Q5 未做自助改密（"我的"页提示联系门店管理员重置）；Q6 店长账号可登录。Q7 部署未执行。

| # | 事项 | 建议 |
|---|------|------|
| Q1 | UI 组件库：uView Plus 还是 uni-ui | 建议 uView Plus 3（组件全、表单/弹层/加载体验好）；uni-ui 更轻但组件少 |
| Q2 | 排班规则编辑是否进移动端 | 建议不进，只读消费排班结果 |
| Q3 | 档案新建/编辑移动端字段集 | 建议精简集（姓名/性别/生日/家长手机号/视力状况/裸眼视力），其余字段留 PC |
| Q4 | 医生助理权限边界（授权次数、开始养护是否放开） | 需确认；PC 端现状为两者均可登录操作，移动端建议保持一致 |
| Q5 | 自助修改密码 | 后端无自助改密接口；选项：本期新增接口 / 本期不做（管理员重置）/ 复用 auth 既有能力（开发时核对） |
| Q6 | 店长账号（userType=2）是否登录移动端 | 建议允许（店长可能兼一线医生），仅身份展示差异 |
| Q7 | 部署入口与域名（公众号菜单/二维码/独立域名） | 需确认；小程序形态还需微信小程序资质与 HTTPS 域名白名单 |
