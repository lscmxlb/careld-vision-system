# 医院端 store-web 测试反馈改造 Spec（第三批：第 19–27 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（前端 + 微服务） |
| 来源 | 测试反馈第 19–27 项（用户口头/文字反馈） |
| 涉及端 | 门店医院Web端（store-web）、儿童微服务（careld-child-service） |
| 涉及页面 | /child（儿童档案）、/care-record（养护记录）、/appointment-record（预约记录） |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + API（真实登录态 curl）+ 浏览器（DOM/Vue 状态/布局测量）三层验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 19 | 儿童档案列表 | 「可用次数」列前增加「养护次数」列；去掉「主治医师」列；操作区去掉「养护记录」按钮；操作列贴合内容不留空闲区域 | 前端 + 后端 | ✅ 已完成 |
| 20 | 儿童档案-预约授权弹窗 | 标题姓名改为 完整姓名+[手机号]；「当前剩余」改为「可用次数」 | 前端 | ✅ 已完成 |
| 21 | 儿童档案-预约记录详情弹窗 | 标题姓名改为 完整姓名+[手机号]；缴费方式列过宽，各列调整为合适宽度 | 前端 | ✅ 已完成 |
| 22 | 儿童档案-编辑页 | 视力状况 8 项分两行（正常/轻度近视/中度近视/高度近视 + 斜视/弱视/散光/远视），两行各列对齐 | 前端 | ✅ 已完成 |
| 23 | 养护记录-详情页 | 儿童基本信息各列优化宽度、平均分配 | 前端 | ✅ 已完成 |
| 24 | 儿童档案-新建/编辑 | 裸眼视力 双眼/左眼/右眼 改为下拉选择，可选值 5.3–4.0 | 前端 | ✅ 已完成 |
| 25 | 儿童档案列表 | 儿童姓名/性别/年龄/家长姓名/家长手机/可用次数/养护次数 7 列数据居中 | 前端 | ✅ 已完成 |
| 26 | 儿童档案列表 | 增加筛选「可用次数大于 x 次」 | 前端 + 后端 | ✅ 已完成 |
| 27 | 儿童档案/养护记录/预约记录 | 重置与搜索按钮位置对调，「搜索」改名「查询」 | 前端 | ✅ 已完成 |

---

## 2. 关键设计决策（含用户拍板）

1. **#19 养护次数口径**（拍板）：**含养护中的全部记录**，即 `care_record` 中 `status IN (1, 2) AND deleted_at IS NULL` 计数（不是仅已完成）。与养护记录页「累计养护次数」口径的差异点是明确的：本列口径更宽。
2. **#19 操作列宽**：按实际渲染测量定宽 **384px**——最宽行（待审核行：审核48 + 预约授权72 + 预约记录72 + 档案详情72 + 禁用48 + 4×12 间距 = 360 内容宽）+ 单元格左右 padding 24 = 384，实测无换行。普通行（4 按钮）内容 300px。
3. **#20/#21 标题手机号格式**（拍板）：显示**完整手机号**（非掩码）。列表行数据是掩码，打开弹窗时异步拉详情接口（`GET /children/{id}`，服务端解密）刷新标题；失败时保留掩码兜底。
4. **#22 视力状况两行布局**：`el-checkbox-group` 外包 `display: grid; grid-template-columns: repeat(4, 1fr)`，8 项自然形成两行四列且列对齐；`:deep(.el-checkbox){ margin-right: 0 }` 消除 EP 默认右间距保证精确对齐。
5. **#23 各列平均分配**：`el-descriptions` 加 `label-width="96px"`（统一标签宽）+ CSS `table-layout: fixed`（EP 表格自带 `width:100%`），内容列均分剩余宽度；实测 1153px 表格宽下 = 4×(96 标签 + 192 内容)，跨列项（span=2）480px。
6. **#24 裸眼视力可选值**：`VISION_OPTIONS = 5.3 → 4.0`（0.1 步长，14 项）代码生成。历史脏数据（如 "49"、"50"、3.5）不在选项内时 EP 仍显示原值文本，不阻断编辑，不做数据清洗。
7. **#26 筛选语义**：「大于 x 次」为**严格大于**（`remaining_count > x`）；空值不限。筛选在后端 Java 流中实现（与 keyword 过滤一致），不新增 SQL。
8. **#27 按钮顺序**（拍板）：**[重置][查询]**（重置在前），查询按钮 `type="primary"`。

---

## 3. 各项实现要点

### 3.1 后端（careld-child-service）

- `ChildMapper.ENRICHED_COLUMNS`：加子查询 `(SELECT COUNT(*) FROM care_record cr WHERE cr.child_id = c.id AND cr.status IN (1,2) AND cr.deleted_at IS NULL) AS care_count`（child-service 与 care_record 同库 careld_vision，沿用既有 JOIN 模式）。
- `ChildProfile`：新增派生字段 `@TableField(exist = false) Integer careCount`。
- `ChildService.listProfiles(...)`：签名新增 `Integer remainingCountMin`；`ChildServiceImpl` 在 enrich 后、keyword 过滤前按 `(remainingCount ?? 0) > remainingCountMin` 过滤。
- `ChildController`：`GET /api/v1/children` 新增可选参数 `remainingCountMin`。

### 3.2 前端（store-web）

- `views/child/index.vue`：
  - 列表列序：档案编号 → 儿童姓名 → 性别 → 年龄 → 家长姓名 → 家长手机 → **养护次数（90，居中）** → **可用次数（100，居中）** → 审核状态 → 状态 → 操作（384，fixed right）；删除主治医师列与养护记录按钮。
  - 搜索栏：新增「可用次数 大于 [input] 次」（`el-input-number`，宽 80，不填不限）；按钮 [重置][查询]；`handleReset` 清空该筛选项。
  - 授权弹窗/预约记录弹窗：`:title` 绑定动态标题（完整姓名[手机号]）；「当前剩余」→「可用次数」；预约记录列宽收紧（日期95/时间60/变更类型90/变更次数80/可用次数80/缴费方式118/缴费金额90右对齐/开单医生85/备注 min-width120）。
  - 视力状况 `class="eye-grid"` 两行四列；裸眼视力三项改 `el-select`（`VISION_OPTIONS`）。
  - 清理：删除 `handleCareRecords` 与 `useRouter/router` 引用（仅保留 `useRoute` 用于建档引导）。
- `views/care-record/index.vue`：按钮改 [重置][查询]；详情 `el-descriptions` 加 `label-width="96px"` + `table-layout: fixed`。
- `views/appointment-record/index.vue`：按钮改 [重置][查询]。
- `types/index.ts`：`Child` 加 `careCount?: number`；`ChildQuery` 加 `remainingCountMin?: number`。

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --noEmit` | 通过（exit 0） |
| 后端构建 | child-service `mvn package` + 重启 8284 | 启动 `Started ChildServiceApplication` |
| API | `GET /children?storeId=7` 的 careCount 与 DB 逐一比对 | 11 条全部一致 |
| API | `GET /children?storeId=7&remainingCountMin=2` | 仅返回 remaining>2 的 4 条（9/7/3/17） |
| 浏览器 | 档案列表列序/居中/养护次数列；操作区无养护记录按钮 | 一致；操作列 384，模拟待审核行（5 按钮）无换行 |
| 浏览器 | 可用次数>2 筛选 UI 端到端 | 共 4 条，与 API 一致；重置恢复 |
| 浏览器 | 授权弹窗标题 `预约授权 - 张三丰[13888888888]`、label 可用次数 | 通过 |
| 浏览器 | 预约记录弹窗标题 `预约记录详情 - 张三丰[13888888888]`、各列宽度 | 通过 |
| 浏览器 | 视力状况两行对齐（x=116/273/430/587 两行完全一致）；裸眼视力下拉 14 项 5.3→4.0 | 通过 |
| 浏览器 | 养护详情 descriptions（强制对话框 1200px 实测）label 96 / 内容列 192 均分 | 通过 |
| 浏览器 | 三页按钮 [重置][查询] | 通过 |

**环境备注**：应用内浏览器无可见表面（viewport 0×0、截图不可用），布局类验证采用「结构 + 强制像素宽 + 测量」方案；关闭过渡冻结导致的弹窗残留 DOM 为环境假象，已用 Vue `setupState` 复核弹窗真实关闭。

---

## 5. 遗留与注意事项

1. **视觉走查**：截图不可用，像素级视觉验收（尤其列宽观感）待用户打开应用内浏览器走查；如需微调，操作列宽为 384、预约记录各列宽见 3.2。
2. **裸眼视力历史脏数据**：id=27 的 "49"/"50" 等不在下拉选项内，显示原值、不阻断保存；未做清洗（如需清洗另行确认）。
3. **养护次数口径**：本列（status 1/2）与养护记录页既有「累计养护次数」（口径以该页定义为准）可能存在数值差异，属预期。
