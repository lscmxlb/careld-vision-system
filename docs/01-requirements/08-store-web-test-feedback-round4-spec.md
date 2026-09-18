# 医院端 store-web 测试反馈第 4 批改造 Spec（第 30–35 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（纯前端，无后端/数据库改动） |
| 来源 | 测试反馈第 30–35 项（用户文字反馈） |
| 涉及端 | 门店医院Web端（store-web），files 全部位于 `frontend/store-web/src` |
| 涉及页面 | 侧边菜单、/care-record（养护记录列表+详情）、/department（系统设置-科室管理）、/appointment-record（预约记录）、/child（儿童档案） |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + 浏览器（DOM 实测/布局测量/交互/图表 option）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 30 | 档案管理 | 「视力记录」菜单项隐藏 | MainLayout | ✅ 已完成 |
| 31 | 养护记录 + 养护记录详情 | 视力录入值后面为 "+0" 时不显示 "+0" | care-record | ✅ 已完成 |
| 32 | 系统设置-科室管理 | 弹出页去掉最后一行的「排序」 | department | ✅ 已完成 |
| 33 | 预约记录 | 开始/完成养护为同一按钮二态；四个按钮（开始养护/预约调整/标记爽约/取消预约）一直显示、不可用置灰；操作列宽恰好容纳四按钮 | appointment-record | ✅ 已完成 |
| 34 | 儿童档案 | 操作按钮整体右移靠最右；操作列宽恰好容纳四个按钮 | child | ✅ 已完成 |
| 35 | 预约记录 | 状态筛选框选项前加方框打勾样式 | appointment-record | ✅ 已完成 |

---

## 2. 关键设计决策

1. **#30 只隐藏菜单项，保留路由**：`MainLayout.vue` 移除 `/vision` 菜单项；`router` 中 `/vision` 路由保留（直接输入 URL 仍可访问），未牵连其他端（grep 确认无其他页面链接该路由）。
2. **#31 展示层剥离，不改存储**：新增展示函数 `displayVision(v) = (v \|\| '').replace(/\+0$/, '')`——仅去掉结尾 `+0`，`+2/-1` 等保留；**只作用于养护记录列表与详情**，不触碰养护登记弹窗（弹窗内次值下拉仍默认显示 `+0`，遵循既有规则）。列表「首次视力/当前视力」、详情「裸眼视力」、图表柱标签（`full`）与 tooltip 全部走该函数。
3. **#32 只删表单项、payload 保留 sortOrder**：弹窗最后一行的「排序」`el-form-item` 删除；`form.sortOrder` 在编辑时仍由 `handleEdit` 从行数据带入原值、新增时为 0，提交 payload 不变——**不改变已有排序数据**。列表「排序」列保留（用户只要求改弹窗）。
4. **#33 二态合成按钮**：`开始养护/完成养护` 合并为一个按钮——`status=1`（已预约）显示「开始养护」（primary），`status=2`（养护中）显示「完成养护」（success），其余状态显示「开始养护」置灰；启用条件 `status===2 || (status===1 && 当天)`。四个按钮常显，禁用条件沿用既有业务规则：
   - 预约调整：仅 `status=1` 可用；逾期时原有 tooltip「已超过预约时段，不可调整」保留；
   - 标记爽约：仅 `status=1 且（当天 或 已逾期）`可用；
   - 取消预约：仅 `status=1 且未逾期`可用（养护中置灰，沿用第 14 条规则）。
5. **#33/#34 列宽"恰好容纳"**：按浏览器实测按钮分数宽度反推列宽（内容宽 + 单元格左右各 12px padding），并各留约 1px 抗分数取整；两页均为 `fixed="right"` 固定列。
6. **#34 右对齐**：操作列加 `align="right"`，按钮组贴单元格右侧（距列右边缘 12px，即单元格内边距）。
7. **#35 方框勾选样式**：状态多选下拉的每个 `el-option` 内前置 `el-checkbox`（`:model-value` 受控、`pointer-events:none` 纯展示，点击仍由选项行切换选中）；隐藏 EP 默认的右侧对勾（`.el-select-dropdown__item.is-selected::after`）。popper 挂载在 body，样式写在**非 scoped** 样式块的 `.status-checkbox-popper` 下。

---

## 3. 各项实现要点

### 3.1 #30 隐藏视力记录菜单（`layouts/MainLayout.vue`）

- 档案管理子菜单删除 `<el-menu-item index="/vision">视力记录</el-menu-item>`，剩「儿童档案 / 养护记录」。

### 3.2 #31 视力值隐藏 +0（`views/care-record/index.vue`）

- 新增 `displayVision` 展示函数（与既有 `visionMain` 并列）。
- 列表：`首次视力` → `displayVision(row.nakedVisionBoth)`、`当前视力` → `displayVision(row.visionAfterBoth)`。
- 详情弹窗：`裸眼视力` 双眼/左眼/右眼三项改用 `displayVision`。
- 图表：柱子数据 `full: displayVision(raw)`（标签显示值）；tooltip 各系列值同样走 `displayVision`。

### 3.3 #32 科室管理弹窗去掉排序行（`views/department/index.vue`）

- 删除「排序」`el-form-item`（原最后一行）；其余（编码/名称/类型/收费标准）不变。

### 3.4 #33 预约记录操作列（`views/appointment-record/index.vue`）

- 操作列 `width="282"`（原 360），四按钮常显：
  - 二态养护按钮：`:type="row.status === 2 ? 'success' : 'primary'"`、`:disabled="!canOperateCare(row)"`、文案/事件按 `status===2` 分支；
  - 预约调整：逾期分支保留 tooltip+disabled，否则普通按钮 `:disabled="row.status !== 1"`；
  - 标记爽约：`:disabled="row.status !== 1 || !(isReserveToday(row) || isOverdue(row))"`；
  - 取消预约：`:disabled="row.status !== 1 || isOverdue(row)"`。
- 新增 `canOperateCare(row)`：`status===2 || (status===1 && isReserveToday(row))`。

### 3.5 #34 儿童档案操作列（`views/child/index.vue`）

- 操作列 `width="326"`（原 384）、加 `align="right"`；按钮组与判断逻辑不变（含待审核行的「审核」按钮）。

### 3.6 #35 状态筛选方框勾选（`views/appointment-record/index.vue`）

- 下拉选项改为 `v-for STATUS_OPTIONS`（新增常量数组：1 已预约 / 2 养护中 / 3 已完成 / 4 已取消 / 5 已爽约），每个选项内前置 `el-checkbox.status-option-box`；`popper-class="status-checkbox-popper"`。
- 非 scoped 样式：`.status-checkbox-popper` 下实现 `.status-option` 弹性布局（checkbox 与文字 8px 间距）、checkbox `pointer-events:none`/`margin-right:0`/`height:auto`、`.el-select-dropdown__item.is-selected::after { display:none }`。

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --noEmit` | 通过（exit 0） |
| 浏览器 #30 | 侧边菜单项核对（档案管理组） | 仅「儿童档案 / 养护记录」，无「视力记录」 |
| 浏览器 #31 | 养护记录列表「首次视力/当前视力」列值 | `5.0+0 → 5.0`、`5.2+0 → 5.2`、`5.3+0 → 5.3`，无 +0 |
| 浏览器 #31 | 详情弹窗图表 `getOption()` 全系列 `full` 值 | 无 `+0`；`4.9+2` 保留；`displayVision('5.0+0')='5.0'`、`('4.9+2')='4.9+2'`、`(undefined)=''` |
| 浏览器 #31 | 详情弹窗 tooltip formatter 实调 | `养护后·左眼：4.9+2` 保留、其余无 +0 |
| 浏览器 #32 | 「编辑科室」弹窗表单项 | 4 行：科室编码/科室名称/科室类型/收费标准，无「排序」 |
| 浏览器 #33 | 10 行操作列按钮数与换行 | 全部 4 按钮、单行无换行；`完成养护` 出现于 status=2 行 |
| 浏览器 #33 | 各状态禁用组合（status=3/4 全灰；status=2 仅完成养护可点；status=1 当天全可点；status=1 未来仅调整/取消可点） | 与业务规则一致 |
| 浏览器 #33 | 禁用按钮点击不弹窗；可点「开始养护」打开养护登记弹窗（mode=start、footer 开始/结束养护按未填数据置灰） | 通过（取消后 `careVisible=false`，残留 DOM 为 0×0 视口过渡假象） |
| 浏览器 #33 | 操作列宽实测：内容 256.08px（4×58.02 + 3×8）；列宽 280 时超 0.08px 换行 → 调 282 后全部单行 | 通过（280→282） |
| 浏览器 #34 | 10 行操作列按钮 | 全部 4 按钮、右对齐（`text-align:right`、末按钮距列右 12px）、单行无换行 |
| 浏览器 #34 | 列宽实测：内容 300.08px（3×72.02 + 48.02 + 3×12）；列宽 324 时超 0.08px 换行 → 调 326 后全部单行 | 通过（324→326） |
| 浏览器 #35 | 下拉选项 DOM | 5 项均含 checkbox；选中项 `is-checked`；默认右侧对勾 `::after display:none` |
| 浏览器 #35 | 点击「已取消」选项 | 勾选框即时变勾选（单击切换、无双重触发）；再点取消恢复；选中回显「已预约 +2」 |

**环境备注**：应用内浏览器无可见表面（viewport 0×0），列宽测量采用「`getBoundingClientRect` 分数宽度 + `offsetTop` 判换行 + 单元格 padding 换算」；弹窗关闭后残留 DOM 属 transition 不触发的环境假象（以 `setupState.careVisible` 复核为准）。

---

## 5. 遗留与注意事项

1. **待审核行与列宽**：儿童档案操作列按"四个按钮"收紧为 326px；家长自建待审核行会多一个「审核」按钮（共 5 个，需约 384px），该行按钮会折行。当前库中无待审核儿童（`audit_status=0` 共 0 条），如需兼顾可再调宽或改为按行隐藏。
2. **视觉走查**：列宽为实测紧贴值（1px 级余量），像素级观感（尤其右对齐贴边效果、勾选框间距）待用户打开应用内浏览器走查确认。
3. **#35 交互细节**：勾选框为纯展示（`pointer-events:none`），点击选项文字或勾选框区域都由选项行切换选中；如需"点勾选框本身也切换"，现有实现已覆盖（事件落到选项行）。
4. **视力记录路由保留**：菜单隐藏但 `/vision` 路由仍可直达；如需彻底下线（连路由/接口一并移除）请另行提出。
