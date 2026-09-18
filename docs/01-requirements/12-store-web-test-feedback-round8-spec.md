# 医院端 store-web 测试反馈第 8 批改造 Spec（第 42–45 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（纯前端 store-web：文案 + 弹窗脏数据保护 + 弹窗位置） |
| 来源 | 测试反馈第 42–45 项 |
| 涉及端 | 门店医院Web端（store-web），无后端改动 |
| 涉及页面 | 预约记录（/appointment-record）、预约管理（/schedule）、排班设置（/schedule-rule） |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + 浏览器（Vue 组件状态 / DOM 断言）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 42 | 预约记录-开始养护弹窗 | 「养护服务」下拉提示文字改为「请选择进行养护服务的医师或医生助理」 | 单行文案 | ✅ 已完成 |
| 43 | 排班设置-新增/编辑规则弹窗 | 录入或修改数据后点「取消」或点弹窗外区域，不能直接关闭，需弹确认框提示有数据未保存 | 脏数据保护（`before-close` + 取消按钮） | ✅ 已完成 |
| 44 | 预约记录-新建预约 / 预约管理-添加预约弹窗 | 选择或填写任何内容后未保存即关闭，需弹确认框提示有数据未保存 | 脏数据保护（`before-close` + 取消按钮） | ✅ 已完成 |
| 45 | 预约记录-开始养护弹窗 | 框体位置上移，保证整个弹窗完整显示 | 弹窗 `top` + 内容超高内部滚动 | ✅ 已完成 |

用户原话：
- 「42、预约管理-开始养护的弹出页面中，养护服务右侧选择框中的提示文字"请选择医生"修改为"请选择进行养护服务的医师或医生助理"」
- 「43、排班设置，编辑或新增排班规则时，如果录入或修改了数据，点击取消或点击弹窗区域个的地方时，页面会自动关闭，要弹窗告诉用户有数据未保存，不能直接退出」
- 「44、在预约记录页面中新建预约或预约管理页面中添加预约时，在弹窗页面上选择或填写了任何内容后，如果未保存关闭页面，要要弹窗告诉用户有数据未保存，不能直接退出」
- 「45、预约记录-开始养护弹出的页面框体位置上移，以完全显示当前页面」

---

## 2. 关键设计决策

1. **统一脏数据保护方案（三处弹窗各自显式实现，不抽公共 composable）**：
   - 打开弹窗时把表单 `JSON.stringify` 存为「未改动基线」快照；
   - 关闭前（`ElMessageBox.confirm`）比较当前表单与快照，一致则直接关闭、不一致则弹确认框；
   - `el-dialog` 的 `:before-close` 覆盖 **点 X / 点遮罩 / 按 ESC** 三条关闭路径；弹窗底部「取消」按钮改走同一处理函数。
   - 不抽 composable 的原因：三处字段结构与基线时机不同，且代码库既有风格为页面内显式实现（养护登记弹窗 `careFormDirty`/`handleCareCancel` 即此模式）；沿用既有确认框文案模式（标题「关闭确认」、确认「确认关闭」、取消「继续填写」），仅按页面语境区分「已填写的数据」（预约）/「已录入的数据」（排班规则）。

2. **用「快照比较」而不是「有值即脏」**：三处表单打开时都带系统预置值，非空判断会误弹。
   - 排班设置「新增」自带默认时段（工作日/周六日各 08:00-12:00、每小时 3 人）→ 用户未录入即取消，不应提示；
   - 排班设置「编辑」基线为现有规则回填值 → 未改动即取消，不应提示；
   - 预约管理「添加预约」由日期单元格进入时会预填该日期 → 未改动即取消，不应提示。

3. **预约管理「添加预约」的快照时机**：`handleAdd` 内先取一次快照（预填日期），`await loadSlots()` 结束后**再取一次**。因为时段加载若返回空会程序化清空 `date`（并提示"该日期无可约时段"），若不重取快照，这种系统行为会被误判为「用户有未保存数据」。

4. **保存成功路径不受影响**：`el-dialog` 的程序化 `v-model=false`（提交成功后）**不会**触发 `before-close`，因此三个弹窗保存成功均直接关闭、无多余确认；`before-close` 仅在用户主动关闭时触发。

5. **#45 弹窗上移方案**：`top="3vh"`（默认 15vh）+ 弹窗体 `max-height: calc(97vh - 130px)` + `overflow-y: auto`，即整框（含标题/页脚）始终落在视口内，超出部分改为**弹窗内滚动**。样式写在**非 scoped** 样式块（弹窗 teleport 到 body，scoped `:deep` 对 teleport 内容不可靠；同文件已有 `status-checkbox-popper` 非 scoped 先例）。

6. **#42 为纯文案**：只改 `placeholder`，下拉数据源与选项文案（医师/医生助理姓名）不变。

---

## 3. 各项实现要点

### 3.1 `frontend/store-web/src/views/appointment-record/index.vue`

| 项 | 位置 | 要点 |
|---|---|---|
| #42 | 模板「养护服务」`el-select`（约 228 行） | `placeholder="请选择医生"` → `placeholder="请选择进行养护服务的医师或医生助理"` |
| #44 | 新建预约弹窗（约 154–218 行） | 加 `:before-close="handleReserveDialogBeforeClose"`；页脚「取消」改 `@click="handleReserveDialogCancel"`；脚本区新增 `reserveFormSnapshot`（`handleCreateReserve` 重置表单后立即取快照）、`confirmReserveClose()` / `handleReserveDialogCancel()` / `handleReserveDialogBeforeClose(done)` |
| #45 | 养护记录登记弹窗（约 220–232 行）+ 末尾非 scoped 样式 | 加 `top="3vh"`、`class="care-dialog"`；`.care-dialog .el-dialog__body { max-height: calc(97vh - 130px); overflow-y: auto; }` |

### 3.2 `frontend/store-web/src/views/schedule/index.vue`

- 添加预约弹窗加 `:before-close="handleReserveDialogBeforeClose"`，页脚「取消」改 `@click="handleReserveDialogCancel"`（约 121–130、176 行）。
- 脚本（约 355–490 行）：新增 `reserveFormSnapshot`；`handleAdd` 改 `async`，`Object.assign` 后取快照 → 开弹窗 → `await loadSlots()` → 重取快照；新增 `confirmReserveClose()` / 两个处理函数；`element-plus` 导入补 `ElMessageBox`。

### 3.3 `frontend/store-web/src/views/schedule-rule/index.vue`

- 新增/编辑弹窗加 `:before-close="handleDialogBeforeClose"`，页脚「取消」改 `@click="handleDialogCancel"`（第 64、125 行）。
- 脚本：新增 `formSnapshot`；`handleAdd`/`handleEdit` 在 `Object.assign` 后取快照；新增 `confirmDialogClose()`（文案为「确认关闭吗？已录入的数据将不会保存」）、`handleDialogCancel()`、`handleDialogBeforeClose(done)`。

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --noEmit` | 通过（exit 0） |
| 浏览器 | #42 养护登记弹窗「养护服务」占位文字 | `.el-select__placeholder` 文本 = 「请选择进行养护服务的医师或医生助理」（共 13 个下拉，仅该处变化） |
| 浏览器 | #45 弹窗上移 | `.care-dialog` 内联样式 `--el-dialog-margin-top: 3vh`（原默认 15vh）；样式表命中 `.care-dialog .el-dialog__body { max-height: calc(-130px + 97vh); overflow-y: auto; }`；弹窗体 computed `overflow-y: auto` |
| 浏览器 | #44 新建预约：未改动点「取消」 | 无确认框，弹窗关闭（组件 `visible=false`，`modelValue` 同步 false） |
| 浏览器 | #44 新建预约：填备注后点「取消」 | 弹出「关闭确认 / 确认关闭吗？已填写的数据将不会保存 / [继续填写][确认关闭]」，弹窗保持打开；「继续填写」→ 保持打开；再点「取消」→「确认关闭」→ 关闭 |
| 浏览器 | #44 新建预约：填备注后点弹窗外区域 | 弹出同一确认框，弹窗保持打开；「确认关闭」→ 关闭 |
| 浏览器 | #44 新建预约：未改动点弹窗外区域 | 无确认框，直接关闭 |
| 浏览器 | #44 添加预约：预填日期未改动点「取消」 | 无确认框，直接关闭（验证决策 2/3 的基线口径） |
| 浏览器 | #44 添加预约：填备注后「取消」/点遮罩 | 均弹确认框且保持打开；「继续填写」保持打开，「确认关闭」关闭 |
| 浏览器 | #43 新增排班规则：未改动（含默认时段）点「取消」 | 无确认框，直接关闭 |
| 浏览器 | #43 新增排班规则：点「添加一批例外日期」后点「取消」 | 弹「关闭确认 / 确认关闭吗？已录入的数据将不会保存」且保持打开；「继续填写」保持打开；「确认关闭」关闭 |
| 浏览器 | #43 编辑排班规则：未改动点遮罩 | 无确认框，直接关闭（基线=回填值） |
| 浏览器 | #43 编辑排班规则：点「添加工作日时段」后点遮罩 | 弹确认框且保持打开；「确认关闭」关闭 |
| 浏览器 | 控制台 | 三个页面均无错误消息（仅既有 vue-router `next()` 弃用警告） |
| 数据 | 测试动作 | 全程只开弹窗/改表单/取消，未提交任何表单；无新增/修改/删除业务数据 |
| 登录态恢复 | 验证后浏览器 | 恢复 careld3 @ /dashboard（与验证前一致） |

**验证方法备注**：应用内浏览器为 0×0 视口，无指针/截图能力，全部通过 `evaluate_script` 以 DOM + Vue 组件状态断言：
- 弹窗开闭以 `__vue_app__` 遍历组件实例读取 `el-dialog` 内部 `exposed.visible`（权威）与本页 `setupState`（`reserveDialogVisible` / `dialogVisible`、dirty 快照比较）判定；
- 「点遮罩」需按 Element Plus `useSameTarget` 实现派发 `mousedown`+`mouseup`+`click` 三连事件（仅 `.click()` 不会触发关闭）；
- 0×0 视口下 rAF 停摆导致弹窗过渡不结束、`afterLeave` 不触发（`modelValue` 不回写），属该环境渲染限制，不影响真实浏览器；因此每个需要"重新打开"的场景先整页刷新重置状态。

---

## 5. 遗留与注意事项

1. **养护记录登记弹窗的 X / ESC 仍可直接关闭**（无确认框）：本轮按需求仅覆盖「新建预约」「添加预约」两个弹窗（#44），养护登记弹窗此前只有「取消」按钮做了脏检查（`careFormDirty`），点 X/ESC 会直接丢弃已录入的养护数据。如需一并保护，加 `:before-close` 复用 `handleCareCancel` 即可。
2. **#45 视觉走查待确认**：受 0×0 视口限制，只能验证 `top` 变量与 `max-height` 规则生效，实际"整框完整显示"观感请以真实浏览器截图为准；如仍偏高/偏低，只需调 `top="3vh"` 与 `calc(97vh - 130px)` 两个数值。
3. **未改动也不弹窗的边界**：日期/时间选择器面板中未点「确定」的临时输入不计入表单值，不会触发脏检查（属预期）；语义上仍是"未保存即关闭需提示"。
4. **确认框按钮顺序**：沿用 Element Plus 默认（[继续填写] 在左、[确认关闭] 在右），与既有养护登记弹窗取消确认一致。
5. **未提交 git**：本轮改动与既有未提交改动一同保留在工作区。
