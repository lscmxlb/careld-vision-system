# 家长端 parent-h5 测试反馈第 1 批改造 Spec（第 55、56 项，含第 57 项医生端）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（家长端建档改为选医院+主治医师、家长姓名/关系必填与账号同步、卡片/详情显示医院、取消脏确认；医生端备注占位文案） |
| 来源 | 测试反馈第 55/56/57 项（docs/测试记录.txt 第 117-121 行） |
| 涉及端 | 家长端 H5（parent-h5）、医生端 H5（doctor-h5）、后端 4 个微服务（child / user / schedule / vision） |
| 涉及页面 | 家长端：子女档案（pages/child/index）、档案详情（pages/child/detail）、儿童档案表单（pages/child/form）；医生端：次数授权（pages/child/grant） |
| 状态 | 已开发（2026-09-18，vue-tsc + 后端构建重启 + 浏览器全链路 + DB 对账验证通过；测试数据已物理清理） |

---

## 1. 需求清单（用户原话拆解）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 55-1a | 子女档案 | 空态提示「还没有孩子档案」→「还没有建立儿童档案」 | parent-h5 前端（模板） | ✅ 已完成 |
| 55-1b | 子女档案 | 空态按钮「添加孩子」→「建立儿童档案」（含悬浮按钮「＋ 建立儿童档案」） | parent-h5 前端（模板+导航标题） | ✅ 已完成 |
| 55-2a | 建档表单 | 「基本信息」前新增「医院信息」组（省/市/区 → 医院 → 主治医师），三项必填 | parent-h5 前端（模板+级联逻辑） | ✅ 已完成 |
| 55-2b | 建档表单 | 无医师的医院：照常可选，提示「该医院暂无可选医师」且不能提交（方案 A，用户拍板） | parent-h5 前端（提示+校验） | ✅ 已完成 |
| 55-3 | 建档归属 | 档案保存至家长所选医院名下，由该医院审核 | 后端数据权限（child/user/schedule/vision）+ parent-h5 提交携带 storeId/doctorId | ✅ 已完成 |
| 55-4a | 建档表单 | 「取消」按钮有修改时弹窗确认（用户追加拍板） | parent-h5 前端（confirmDiscard） | ✅ 已完成 |
| 55-4b | 卡片/详情 | 我的孩子卡片与详情页显示所属医院名称（用户追加拍板，区分异地孩子） | parent-h5 前端（模板+样式） | ✅ 已完成 |
| 56-1 | 建档表单 | 手机号码前新增「家长姓名」（必填），提交后自动保存为当前账号姓名 | parent-h5 前端 + 后端 `PUT /users/me` | ✅ 已完成 |
| 56-2 | 建档表单 | 新增「与儿童关系」（必填，用户确认） | parent-h5 前端 | ✅ 已完成 |
| 57 | 医生端授权 | 预约授权页面备注栏占位文案 →「请录入诊疗单号码、购买套餐、活动赠送等信息」 | doctor-h5 前端（模板） | ✅ 已完成 |

用户原话：

- 「55、家长端
  （1）、注册完毕后，提示信息还没有孩子档案修改为还没有建立儿童档案，按钮"添加孩子"修改为"建立儿童档案"
  （2）、添加儿童档案页面中，在"基本信息"前面添加一组信息，名称为"医院信息"，要选择省、市、区后，选择系统中对应的医院信息，然后选择主治医师，这些是必填项
  （3）、家长添加完儿童档案后，按家长选择的医院，将儿童档案保存至对应医院的儿童档案下，由医院审核后即建档完毕」
- 「56、添加儿童档案时，在基本信息的手机号码前面增加二行，第一行是家长姓名，添加这个姓名后自动保存为当前账号对应的家长姓名，第二行是与儿童关系」
- 「57、医生手机端，预约授权页面的备注栏中的提示信息修改为"请录入诊疗单号码、购买套餐、活动赠送等信息"」
- 追加拍板：「无医生的医院怎么处理，方案A。「＋ 建立儿童档案」。建档页「取消」按钮也加"有修改时弹窗确认。我的孩子卡片/详情显示医院名称（有多个异地孩子时可区分）。」；「与儿童关系」做成必填。

---

## 2. 关键设计决策

1. **家长数据范围改为「本人孩子」维度（55-3 核心）**：家长可为孩子选择任意医院建档，若仍按注册门店强制过滤，异地医院的孩子会在列表「消失」。`DataScopeHelper` 新增 `isParent()`（userType=3）与 `resolveStoreIdWithParentChoice(param)`——家长直接取前端参数、不做本店强制；其他角色完全沿用原 `resolveStoreId`。仅改造**读接口**（children 列表/待审数、medical-staff 列表、schedule-rules slots/available-dates、appointment-config、reserves 列表、care-record 列表/分页），**不动 `resolveStoreId` 本身**，避免医疗人员写操作等被同步放宽。
2. **防止跨院串看（保护性 guard）**：家长调养护记录 / 预约记录分页接口**不带 childId** 时直接返回空页（此前门店强制曾起到隐式隔离作用，放开门店后必须显式收口）；带 childId 时仍按孩子过滤。
3. **省市区数据源 = 门店列表派生（不给 H5 引第三方行政区划包）**：PC 端 store-web 用 `element-china-area-data`，家长端为 uni-app 工程，直接拉 `GET /stores/all`（仅启用门店）按 `provinceName → cityName → districtName` 去重派生三级选项——**只出现有医院的地区，无死胡同**，也避免引入数 MB 区划数据。级联用 `picker mode="multiSelector"` + `@columnchange` 重建下级列，确认时回填名称并清空已选医院/医师。
4. **建档归属靠 POST 请求体 storeId**：`ChildController#create` 对家长（userType=3）已置 `source_type=1` + `parent_user_id`，storeId 缺失才回落当前门店；家长端显式提交所选医院 storeId 即完成归属，后端无需新增建档逻辑。建档后 `audit_status=0`，由所选医院在 store-web 审核（55-3「由医院审核后即建档完毕」）。
5. **无医师医院方案 A**：医院照常可选；医师选择框占位显示「该医院暂无可选医师」，表单内同时出现黄条提示「该医院暂无可选医师，请选择其他医院」；`validate()` 拦截提交并 toast 同一文案。
6. **家长姓名同步账号（56-1）**：提交成功后调 `PUT /users/me`（新增接口，仅改 `real_name`，医务人员 userType=6 拒绝）+ 前端 `userStore.fetchProfile()` 刷新缓存；**同步失败不阻塞建档**。预填规则：账号 `real_name` 非系统自动生成格式（`/^家长\d{4}$/`，注册时按手机尾号 4 位生成）才带入表单。
7. **卡片/详情显示医院名（55-4b）**：列表接口 `ChildController#list` 返回的 `ChildProfile` 已含 JOIN 的 `storeName`，前端类型与模板补字段即可；详情页「所属医院」「主治医师」并入档案信息卡（主治医师由视力情况卡移入，医院+医师相邻），避免两处重复。
8. **取消脏确认（55-4a）**：沿用 doctor-h5 #54 的 `confirmDiscard()` 模式（与 `onBackPress` 共用同一弹窗文案），无修改直接返回、有修改弹窗二选一。
9. **导航标题同步**：建档页导航栏标题「添加孩子」→「建立儿童档案」，与空态按钮/FAB 新命名一致（需求未点名，属同一命名变更的顺带统一）。

---

## 3. 实现要点

### 3.1 后端（4 个微服务 + common）

| 文件 | 改动 |
|---|---|
| `careld-common/.../DataScopeHelper.java` | 新增 `isParent()`、`resolveStoreIdWithParentChoice(Long)`（家长取参数原值，其余走 `resolveStoreId`） |
| `careld-child-service/.../ChildController.java` | `list` / `pending-count` 改用新 helper（parentUserId 仍经 `resolveUserId` 兜底） |
| `careld-user-service/.../MedicalStaffController.java` | 仅 `list` 改用新 helper（家长按所选医院查医师）；create/update/resetPassword 等写操作保持原样 |
| `careld-schedule-service/.../ScheduleRuleController.java` | `slots` / `available-dates` 改用新 helper |
| `careld-schedule-service/.../AppointmentConfigController.java` | `get` 改用新 helper |
| `careld-schedule-service/.../ScheduleController.java` | `listReserves` 改用新 helper；家长不带 childId 时返回空页 |
| `careld-vision-service/.../CareRecordController.java` | `list` / `page` 改用新 helper；`page` 家长不带 childId 时返回空页 |
| `careld-user-service/.../UserController.java` | 新增 `PUT /api/v1/users/me`（请求体 `{realName}`；userType=6 返回 400「当前身份不支持此操作」） |
| `careld-user-service/.../UserServiceImpl.java` | `updateMyRealName`：非空、≤20 字、用户存在校验后更新 `real_name` |

### 3.2 家长端 parent-h5

- `src/types/index.ts`：`Child` 加 `storeName?`；`Store` 省市区字段改为 `provinceCode/provinceName/cityCode/cityName/districtCode/districtName`。
- `src/api/auth.ts`：`userApi.updateMyProfile({realName}) => put('/users/me')`。
- `src/api/reserve.ts`：`medicalStaffApi.getStaffList` 参数加 `storeId?`。
- `src/pages/child/index.vue`：空态文案「还没有建立儿童档案」/按钮同名；FAB「＋ 建立儿童档案」；卡片新增「所属医院」行（`item.storeName || '—'`）。
- `src/pages/child/detail.vue`：档案信息卡插入「所属医院」「主治医师」两行（主治医师从视力情况卡移入）。
- `src/pages/child/form.vue`（主要改造）：
  - 新增「医院信息」卡（基本信息之前）：所在地区（multiSelector 省/市/区，选项由 `stores` 派生，`filteredStores` 按三级名称过滤）→ 医院（selector）→ 主治医师（selector，按所选 storeId 拉 `staffRole=1&status=1` 医师）。
  - 基本信息插入「家长姓名」（必填，按预填规则带入）与「与儿童关系」（必填，picker 用 `RELATION_OPTIONS`）。
  - `validate()` 顺序：省市区 → 医院 → 主治医师（无医师给专用文案）→ 儿童姓名 → 家长姓名 → 关系 → 手机号 → 出生日期 → 视力状况。
  - `submit()` 携带 `storeId/doctorId/doctorName/parentName/relation`；成功后 `syncAccountName()`（best-effort）。
  - `cancel()` 与 `onBackPress` 共用 `confirmDiscard()`。
- `src/pages.json`：建档页标题「建立儿童档案」。

### 3.3 医生端 doctor-h5（第 57 项）

- `src/pages/child/grant.vue` 备注 textarea `placeholder` →「请录入诊疗单号码、购买套餐、活动赠送等信息」。

---

## 4. 验证记录（2026-09-18）

1. **类型检查**：parent-h5 `vue-tsc --noEmit` 通过（修复 1 处 `data.parentName` 可能 undefined 的传参告警）。
2. **后端构建重启**：child/user/schedule/vision 四服务 kill → `mvn package` → 重启（BUILD SUCCESS）；API 冒烟：`/stores/all` 含省市区、`/medical-staff?storeId=10` 命中医师、`/children?parentUserId=` 跨院可见、`PUT /users/me` 改名往返成功（测试后已还原原值）。
3. **浏览器全链路（headless Chrome，5177）**：
   - 新手机号走 SMS 注册（验证码 123456）→ 子女档案空态显示「还没有建立儿童档案」「建立儿童档案」；
   - 建档页：医院信息三行就位、家长姓名未预填（账号名为 `家长XXXX`）、手机号自动带入；
   - 级联：省列表仅有 浙江省/河北省/上海市（现有医院所在省，无死胡同）→ 河北省/唐山市/古冶区 → 医院「武汉武昌协和卫生服务中心」→ 医师加载「胡春花」；切地区后医院/医师选择被清空；
   - 方案 A：选 浙江省/杭州市/西湖区 → 杭州西湖社区卫生服务中心（无医师）→ 医师框显「该医院暂无可选医师」、黄条提示出现、`validate()` 返回「该医院暂无可选医师，请选择其他医院」、提交被拦（toast 同文案）；
   - 取消脏确认：填写后点「取消」弹「有未保存的修改，确认放弃吗？/继续编辑·放弃修改」，「继续编辑」留在本页；
   - 提交：toast「已提交，待医院审核」→ 列表出现该孩子且「所属医院=武汉武昌协和卫生服务中心」；详情页档案信息卡显示所属医院/主治医师；预约页该孩子显示「档案待医院审核，暂不可预约」（审核门控正确）；
   - 二次进入建档页：家长姓名预填「李芳」（账号姓名已同步，非 `家长XXXX` 格式）。
4. **DB 对账**：新档案 `store_id=7`（异地医院）、`parent_user_id=新账号`、`doctor_id=1 胡春花`、`relation=妈妈`、`audit_status=0`、`source_type=1`；账号 `real_name` 由 `家长0999` → `李芳`。
5. **测试数据清理**：测试档案（physical delete）、测试账号、测试短信码全部物理删除并校验为 0；先前冒烟改动的 `sys_user.real_name`（100003）已还原为原值。

---

## 5. 遗留 / 备注

- 家长端「我的」页抬头仍为固定格式「家长 + 脱敏手机号」（页面既有设计），账号 `real_name` 已同步但该处不展示；如需展示真实姓名需另提需求。
- 家长端建档页「所在地区」的地区名与门店资料绑定（由门店派生），若新增医院未维护省市区，该医院不会出现在可选列表。
- 医生端 doctor-h5 / 家长端 parent-h5 代码整体尚未提交 git（含第 54、55、56、57 项），提交前需汇报征询（目标分支 dev）。
