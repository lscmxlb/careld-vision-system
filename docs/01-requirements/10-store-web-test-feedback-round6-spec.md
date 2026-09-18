# 医院端 store-web 测试反馈第 6 批改造 Spec（第 38–40 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（store-web 前端为主 + user-service 默认密码） |
| 来源 | 测试反馈第 38–40 项（docs/测试记录.txt 第 82–84 行） |
| 涉及端 | 门店医院Web端（store-web）、后端 careld-user-service、admin-web（提示文字同步） |
| 涉及页面 | 系统设置-基础信息（新增）、系统设置-医务人员 |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + API（真实登录态 curl）+ 浏览器（DOM/交互）验证通过；含当日追加的第 40 项） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 38 | 系统设置-基础信息 | 新增「基础信息」页，把原科室管理页与预约规则页合并为同页两个区域 | store-web 新增页面 + 菜单/路由调整 + 删除旧页面 | ✅ 已完成 |
| 39 | 系统设置-医务人员 | 搜索改「查询」且与重置对调；重置密码默认值改 4009993608；禁用加二次确认；角色「医生」改「医师」；新增「账户启用时间」列 | store-web medical-staff + user-service 默认密码常量 | ✅ 已完成 |
| 40 | 系统设置-基础信息 | 科室管理区域不显示列表，直接展示「编辑」对应的表单项（内嵌表单） | store-web 基础信息页区域一 | ✅ 已完成 |

用户原话：「38、系统设置 中新增一个页面命名为基础信息，在这个新的页面中将原科室管理的弹出页面和原预约规则的页面都合并到这一个页面中的二个区域。」
　　　　　「39、医务人员管理页面，上面的按钮"搜索"修改为"查询"，然后和重置的位置对调。重置密码按钮弹窗的提示"默认密码：Careld@2024"修改为"默认登录密码：4009993608"，当任意用户被重置时，均重置密码为"4009993608"。点击"禁用"按钮后，要弹出提示窗"禁用后该医师/医助的账户将被禁用，无法登录本系统进行任何操作，是否确认禁用？"。列表和新增医务人员弹窗中，原有的角色医生修改为医师。医务人员列表增加一个列，账户启用时间，这个时间以首次录入用户的日期为准。」
　　　　　「40、基础信息页面，科室管理区域不显示列表，直接显示右侧按钮“编辑”对应的页面」

---

## 2. 关键设计决策

1. **「科室管理的弹出页面」释义**：用户在第 32 项反馈中称科室管理为「科室管理弹出页」，故 #38 所指即现有系统设置-科室管理页（`/department`）整页内容（其形式为页面内表格 + 编辑弹窗），而非仅弹窗本身。合并后原页面整体下线。
2. **合并页布局**：新页面「基础信息」为上下堆叠两张卡片——区域一「科室管理」、区域二「预约规则设置」（原有预约规则表单原样搬入）。两区域互不依赖，各自独立加载与保存。区域一形态经第 40 项追加调整为**内嵌编辑表单**（见决策 10）。
3. **旧入口下线**：「科室管理」「预约规则」两个菜单项与路由一并移除，对应视图文件 `views/department/index.vue`、`views/appointment-config/index.vue` 删除（不保留兼容跳转），避免同一功能双入口造成数据口径分叉。
4. **菜单保持精简**：系统设置组最终为「基础信息 / 医务人员 / 设备管理」三项。基础信息页沿用户长权限（`managerOnly: true`，与科室管理原权限一致），科室编辑按钮仍仅店长可见。
5. **「账户启用时间」列口径（用户拍板）**：「以首次录入用户的日期为准」即 `medical_staff.created_at`（雪花 ID + 插入时间，录入后不可变），与既有「创建时间」同源。按用户选择「替换创建时间列」处理——去掉原「创建时间」（含时分秒），仅保留新列「账户启用时间」并按日期 `YYYY-MM-DD` 展示。后端不加字段、不改数据。
6. **默认登录密码统一为 4009993608**：`MedicalStaffController` 提为常量 `DEFAULT_PASSWORD`，**新建**（不传密码）与**重置密码**（留空 body / 空串）两条路径共用同一常量，避免两处硬编码漂移。重置仍重生成 BCrypt 盐，历史密码不受影响。
7. **禁用二次确认**：仅「禁用」方向弹确认（启用不弹），文案逐字使用用户原文（含中文问号「？」）；确认框取消时不发起任何请求。
8. **角色文案「医生」→「医师」**：范围为用户明示的「列表和新增医务人员弹窗」，即表格角色列映射、搜索栏角色筛选项、新增/编辑弹窗角色单选（`el-radio`）。**「医生助理」标签保持原文未改**（用户仅点名「医生」，未要求改为「医助」）。
9. **admin-web 同步范围**：仅同步重置密码弹窗的提示文字（`默认登录密码：4009993608`）与 API 注释；其余 #39 UI（查询/重置顺序、禁用确认、医师文案、账户启用时间列）admin-web 未同步——本批反馈针对医院端，admin-web 另有独立交互形态，如需同步需另行确认。
10. **科室管理区域改为内嵌表单（#40）**：区域一不再展示科室列表与「编辑」弹窗，直接把编辑表单项渲染在页面内（4 项 + 「保存」按钮）。表单数据取该门店科室列表的**第一条**（后端按 `sortOrder ASC, id ASC` 排序，即门店默认科室，通常为唯一保留科室）；保存走 `PUT /api/v1/departments/{id}`，`sortOrder` 随原值提交。原「列表 + 弹窗」形态整体下线（弹窗、`handleEdit`、`isManager`、`getDeptTypeText`、表格列映射一并删除）。

---

## 3. 各项实现要点

### 3.1 #38 新增页（`frontend/store-web/src/views/basic-info/index.vue`，新建）

- `defineOptions({ name: 'StoreBasicInfo' })`；`onMounted` 同时触发 `loadDepartment()` 与 `loadConfig()`。
- 区域一（科室管理，第 40 项后的最终形态）：
  - `<el-card v-loading="deptLoading">` + `#header` 标题「科室管理」；卡片内为**内嵌编辑表单**（`el-form`，`label-width="120px"`，`max-width: 560px`），4 个表单项：科室编码（必填）/ 科室名称（必填）/ 科室类型（必填，`el-select`：门诊 1 / 养护 2 / 检测 3 / 其他 4）/ 收费标准（`el-input-number` 精度 2、宽 160px + 「元」后缀）。
  - 表单底部「保存」按钮（`:loading="deptSubmitLoading"`）→ `PUT /api/v1/departments/{id}`，成功提示「修改成功」并自动重新加载；无列表、无「编辑」弹窗、无新增/删除入口。
  - 数据加载 `loadDepartment()`：取 `departmentApi.getDepartmentList(storeId)` 返回的**第一条**回填表单（含 `id` 与 `sortOrder`，提交时随原值带回）。
- 区域二（预约规则设置）：
  - `<el-card class="config-card" v-loading="configLoading">` 标题「预约规则设置」+ `el-alert` 说明（「配置家长/医生取消预约的时间窗与爽约、自动完成规则，保存后立即生效。」）。
  - 表单 `label-width="200"`、`max-width: 640px`：家长可取消 0–72 小时 / 医生可取消 0–72 小时 / 爽约判定缓冲 0–240 分钟 / 爽约自动标记 1–72 小时 / 养护自动完成 0.5–8 小时(step 0.5) / 预约记录默认显示选择（5 个 `el-checkbox` value="1".."5"，validator 至少勾选一项）+「保存配置」按钮。
- 变量命名：`deptFormRef/deptLoading/deptSubmitLoading/deptForm/deptRules/loadDepartment/handleDeptSubmit` 与 `configFormRef/configLoading/configSubmitLoading/configForm/configRules/loadConfig/handleConfigSubmit` 两套前缀，脚本内以 `// ---------- 科室管理 ----------`、`// ---------- 预约规则 ----------` 分节（#40 后已移除 `dialogVisible/isManager/departmentList/getDeptTypeText/handleEdit`）。
- 样式：`.config-card{margin-top:20px}`、`.form-tip`（灰色 12px 提示）、`.charge-unit`（「元」后缀）。

### 3.2 #38 路由与菜单

- `frontend/store-web/src/router/index.ts`：删除 `department`、`appointment-config` 两条路由；新增 `{ path: 'basic-info', name: 'BasicInfo', component: () => import('@/views/basic-info/index.vue'), meta: { title: '基础信息', icon: 'InfoFilled', managerOnly: true } }`。
- `frontend/store-web/src/layouts/MainLayout.vue`：系统设置组三项：`基础信息(/basic-info)`、`医务人员(/medical-staff)`、`设备管理(/device)`。
- 删除文件：`views/department/index.vue`、`views/appointment-config/index.vue`（含空目录）。

### 3.3 #39 前端（`frontend/store-web/src/views/medical-staff/index.vue`，8 处）

| # | 改动 |
|---|---|
| 1 | 搜索栏按钮改为先「重置」后「查询」（`@click="handleReset"` 在前、`type="primary" handleSearch` 在后） |
| 2 | 搜索栏角色筛选项「医生」→「医师」 |
| 3 | 表格列：原「创建时间」列（prop=createdAt，含时分秒）替换为「账户启用时间」列（width 120，`{{ formatDate(row.createdAt) }}`） |
| 4 | `getRoleText` 映射 1 →「医师」，2 →「医生助理」 |
| 5 | `formatDate`：`value.replace('T',' ').slice(0,10)`（兼容 ISO `2026-09-02T10:30:00`），空值渲染 `-` |
| 6 | 新增/编辑弹窗角色单选 label「医生」→「医师」 |
| 7 | `handleToggleStatus`：`row.status === 1` 时先 `ElMessageBox.confirm('禁用后该医师/医助的账户将被禁用，无法登录本系统进行任何操作，是否确认禁用？','禁用确认',{type:'warning'})`，取消则不发请求 |
| 8 | 重置密码弹窗 `inputPlaceholder` → `'默认登录密码：4009993608'`（message 仍为「请输入「{name}」的新密码，留空则重置为默认密码」） |

### 3.4 #39 后端（`backend/careld-user-service/.../controller/MedicalStaffController.java`）

```java
/** 默认登录密码（新建留空与重置留空均使用） */
private static final String DEFAULT_PASSWORD = "4009993608";
```

- 新建接口：请求未带密码时 `encoder.encode(DEFAULT_PASSWORD)`；重置接口：body 密码为空串/空白时重置为同一常量。
- 原值 `Careld@2024` 不再被任何代码路径使用。

### 3.5 #39 admin-web 同步（仅提示文字）

- `frontend/admin-web/src/views/store/medical-staff.vue`：重置密码弹窗 `inputPlaceholder` 同步为 `'默认登录密码：4009993608'`。
- `frontend/admin-web/src/api/medical-staff.ts`：注释改为「新增医务人员（不传密码则默认 4009993608）」。

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --noEmit` | 通过（exit 0） |
| 构建重启 | user-service `mvn package -DskipTests -pl careld-user-service -am`；先 kill 旧进程→等 8282 释放→启动 | BUILD SUCCESS；日志 `Started UserServiceApplication`（18:37:04），8282 监听，PID 2542876 |
| API | 临时医务人员（id=11，手机 13500001234，「临时验证账户」）新建不传密码 | 201/200 success；python3 bcrypt 校验：匹配 `4009993608`=True、匹配 `Careld@2024`=False |
| API | `PUT /api/v1/medical-staff/11/password`（空体，模拟弹窗留空重置） | 200 success；重新查询哈希后校验 `4009993608`=True、`Careld@2024`=False，盐已重生成 |
| DB | 临时账户清理 | `DELETE FROM medical_staff WHERE id=11`（物理删除）；复核影响 0 行遗留 |
| DB | store 7 医务人员复核 | 有效 4 人全部 `status=1`、`deleted_at=NULL`；无本次改动引入的数据变化 |
| DB | `appointment_config` store 7 复核 | 各字段值未变（24/2/15/12/1.5/'1,2,3'），仅 `updated_at` 因页内同值保存刷新 |
| 浏览器 | /basic-info 页面 | 两张卡片渲染：区域一「科室管理」（5 列 + 编辑按钮）、区域二「预约规则设置」（表单回显完整） |
| 浏览器 | 菜单 | 系统设置组为「基础信息 / 医务人员 / 设备管理」，无「科室管理」「预约规则」 |
| 浏览器 | 科室编辑弹窗 | 标题「编辑科室」、4 表单项回显正确、同值保存提示「修改成功」 |
| 浏览器 | 预约规则保存 | 页内同值保存 200，提示成功；数据值不变 |
| 浏览器 | 医务人员搜索栏 | 按钮顺序 [重置][查询]；角色下拉含「医师」「医生助理」 |
| 浏览器 | 医务人员列表表头 | 含「账户启用时间」、无「创建时间」；日期值 `2026-09-02`/`2026-09-14`（无时分秒） |
| 浏览器 | 新增/编辑弹窗 | 角色单选为 [男, 女, 医师, 医生助理] |
| 浏览器 | 禁用二次确认 | 文案逐字一致「禁用后该医师/医助的账户将被禁用，无法登录本系统进行任何操作，是否确认禁用？」；点「取消」后无状态变化、无请求 |
| 浏览器 | 重置密码弹窗 | placeholder 为「默认登录密码：4009993608」 |
| 浏览器 | 控制台 | 仅 1 条历史 vue-router 弃用告警，无错误 |
| 类型检查 #40 | store-web `vue-tsc --noEmit`（改造后复跑） | 通过（exit 0）；旧标识符（`dialogVisible/departmentList/handleEdit/getDeptTypeText/isManager/loadDepartments`）无残留引用 |
| 浏览器 #40 | /basic-info 区域一 | 卡片内**无列表**（页面 `.el-table` 计数 0）、`el-dialog` 计数 0；4 表单项回显 Careld2026095 / 视力校正门诊 / 养护 / 158.00，底部仅「保存」按钮 |
| 浏览器 #40 | 点「保存」（同值） | 提示「修改成功」，按钮 loading 复位；`PUT /api/v1/departments/9` → 200，随后自动重查 `GET /departments?storeId=7` → 200 |
| DB #40 | `store_department` id=9 复核 | 业务字段全部未变（编码/名称/类型 2/收费标准 158.00/排序 0/状态 1/`deleted_at` NULL），仅 `updated_at` 刷新为 18:42:52 |
| 浏览器 #40 | 区域二与菜单回归 | 「预约规则设置」6 表单项 + 5 勾选项 + 「保存配置」完好（值 24/2/15/12/1.5；勾选 1,2,3,4,5）；菜单系统设置仍为 基础信息 / 医务人员 / 设备管理 |
| 浏览器 #40 | 布局实测（0×0 视口） | 两卡片间距 20px、表单项 label 宽 120px、「保存」按钮 60×32 |
| 控制台 #40 | 页面消息 | 无本次改动引入的错误（仅历史 vue-router 弃用告警与登录前 1 条 `/users/me` 403 记录） |

**环境备注**：应用内浏览器无可见表面（viewport 0×0），验证以 `evaluate_script` 读 DOM / Vue 状态为准；页面内 fetch 真实登录（`careld3`，未修改任何密码）后走 vite 代理访问后端。

---

## 5. 遗留与注意事项

1. **admin-web 未同步 #39 其余项**：admin-web 医务人员页仍是「搜索」按钮、无禁用确认、角色文案未改「医师」、无「账户启用时间」列（admin-web 侧默认可参考本轮改动，但其表单形态不同，需另行确认）；其重置密码提示文字已同步。
2. **「医生助理」文案未改**：用户原话仅要求「角色医生修改为医师」，「医生助理」保持原样；若后续要统一为「医助」（含禁用确认文案中已出现「医师/医助」措辞），需与用户确认口径。
3. **`DEFAULT_PASSWORD` 硬编码**：默认密码以常量硬编码在 Controller（与旧值同形态），未走配置中心；如需按环境配置化可另行提出。**重置后旧密码立即失效，存量账户密码不受影响**。
4. **账户启用时间 = created_at**：口径为「首次录入日期」，即数据行创建时间（不可变）；若未来引入「禁用后启用」的启用时间语义（即最近一次启用时间），与本列含义不同，需新列或新口径区分。
5. **旧 URL 失效**：`/department`、`/appointment-config` 已下线（无重定向），书签/外链访问会跳 404 兜底页；如需保留 302 到 `/basic-info` 可另行提出。
6. **并发数据变化说明**：验证过程中发现 store 7 医务人员角色数据与早前快照不一致（胡春花 role 2→1、张小丽 1→2 等，`updated_at` 未变），判定为外部并发操作（疑似直接 SQL），非本次改动所致；UI 映射已按现值核对正确（role 1=医师、role 2=医生助理）。
7. **数据说明**：临时验证账户 id=11 已物理清理；store 7 医务人员维持 4 人（含 id=3 张明丽为软删除状态，早于本批改造）。
8. **视觉走查**：0×0 视口环境无法截图，页面结构/交互为数值级验证，像素级观感（两卡片间距 20px、表单宽度 640px 上限）待用户打开真实浏览器走查确认。
9. **多科室门店只呈现第一条（#40 决策）**：区域一取科室列表第一条（`sort_order` 升序，同 `sort_order` 时按 id）编辑；若某门店存在多条有效科室，其余科室在 store-web 已无编辑入口（admin-web 仍保留完整科室管理）。如需在基础信息页提供科室切换/选择，需另行提出。
10. **内嵌表单无「取消」入口（#40）**：弹窗时代的「取消」按钮随弹窗一并移除，改动后如需放弃只能手工改回；用户未要求「重置」按钮，如需可另行提出。
