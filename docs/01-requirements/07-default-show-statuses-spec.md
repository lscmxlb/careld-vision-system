# 医院端 store-web 预约记录默认显示选择 Spec（第 28–29 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（前端 + 微服务 + 数据库迁移） |
| 来源 | 测试反馈第 28–29 项（用户文字反馈） |
| 涉及端 | 门店医院Web端（store-web）、排班微服务（careld-schedule-service） |
| 涉及页面 | /appointment-config（系统设置-预约规则）、/appointment-record（预约记录） |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + API（真实登录态 curl + DB 对账）+ 浏览器（DOM/请求参数/交互）三层验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 28 | 系统设置-预约规则 | 增加「预约记录默认显示选择」选项，后面是 已预约/养护中/已完成/已取消/已爽约 五项，每项前有勾选框 | 数据库 + 后端 + 前端 | ✅ 已完成 |
| 29 | 预约记录 | 进入后列表只按预约规则中「预约记录默认显示选择」勾选的状态显示记录 | 后端 + 前端 | ✅ 已完成 |

---

## 2. 关键设计决策（含用户拍板）

1. **状态筛选形态**（拍板）：预约记录页「状态」由单选下拉改为**多选下拉**（`multiple + collapse-tags + collapse-tags-tooltip`，宽 180px），进入时回显配置勾选（如全选显示「已预约 + 4」）；**清空 = 不限**（不传 statuses，显示全部）。
2. **全不勾选处理**（拍板）：预约规则保存时校验**至少勾选一项**（前端表单校验 + 后端 400 双重拦截），避免出现"进入页面无任何记录"的配置。
3. **存量门店初始默认**（拍板）：**五个全部勾选**（即不改变现有进入页行为）；迁移脚本 `NOT NULL DEFAULT '1,2,3,4,5'` 自动回填存量行。
4. **重置行为**（拍板）：预约记录页点「重置」后状态恢复为**配置的默认勾选状态**（先拉配置缓存到 `defaultShowStatuses`），关键词与日期清空。
5. **存储形态**：单表加列 `appointment_config.default_show_statuses VARCHAR(32) NOT NULL DEFAULT '1,2,3,4,5'`（CSV，1已预约2养护中3已完成4已取消5已爽约），与既有"每医院单行"配置模式一致。
6. **多状态过滤语义**：后端新增 `statuses` 多值参数（非空时优先于原 status/noShowFlag）；**4（已取消）与 5（已爽约）后端同为 status=4，靠 no_show_flag 区分**——同时勾 4+5 等价于全部 status=4；只勾 4 取非爽约、只勾 5 取爽约；与真实状态（1/2/3）并存时按 OR 组合。
7. **日期范围规则推广**（沿用既有"默认今天及以后"）：未指定日期且**仅勾选 {4,5}** 时覆盖历史（不加 startDate）；其余情况（含全选、清空不限）保持 startDate=今天。
8. **兼容性**：原 `status` / `noShowFlag` 参数保留（其他调用方/历史行为不破坏）；`appointmentConfigApi` 仅 store-web 使用，加字段无跨端影响。

---

## 3. 各项实现要点

### 3.1 数据库

- `database/mysql/migration/19-appointment-config-default-show-statuses.sql`：`ALTER TABLE appointment_config ADD COLUMN default_show_statuses VARCHAR(32) NOT NULL DEFAULT '1,2,3,4,5' COMMENT '预约记录默认显示的状态，逗号分隔，1已预约2养护中3已完成4已取消5已爽约'`；存量 2 行（store 1/7）自动回填。

### 3.2 后端（careld-schedule-service）

- `AppointmentConfig`：加 `String defaultShowStatuses`。
- `ScheduleRuleServiceImpl`：`getConfig` 无行默认值加 `setDefaultShowStatuses("1,2,3,4,5")`；`saveConfig` 加 `validateDefaultShowStatuses`（至少一项、值域仅 1-5，违规抛 400 中文提示）。
- `ScheduleService/Impl.listReserves`：签名加 `List<Integer> statuses`；新增私有 `applyStatusesFilter`——real=statuses∩{1,2,3}；无 4/5 → `status IN real`；4+5 → `status IN (real...,4)` 或 `status=4`；仅 4/5 → `status=4` + no_show_flag 细分；real 与取消类并存 → `(status IN real OR (status=4 AND no_show_flag 条件))`。
- `ScheduleController.reserves`：加 `@RequestParam List<Integer> statuses`（Spring 原生支持 `statuses=1,2,3` CSV 绑定）。

### 3.3 前端（store-web）

- `views/appointment-config/index.vue`：「预约记录默认显示选择」`el-checkbox-group`（5 项 `value="1"~"5"`，代码库新式 `value` API）；`defaultShowStatuses: string[]` 与 CSV 互转；自定义校验器"至少勾选一项"（trigger change）+ form-tip 说明。
- `views/appointment-record/index.vue`：
  - 状态下拉改多选（宽 180px，`collapse-tags` / `collapse-tags-tooltip` / `clearable`）；`queryForm.statuses: number[]`。
  - `loadDefaultShowStatuses()`：onMounted 先拉 `appointmentConfigApi.getConfig()` → 解析 CSV → 缓存 `defaultShowStatuses` 并填入 `queryForm.statuses`；失败兜底全选五状态。
  - `fetchReserves`：`statuses: statuses.length ? statuses.join(',') : undefined`（清空不传）；`startDate: queryForm.date || onlyCancelOrNoShow ? undefined : todayStr()`，`onlyCancelOrNoShow = statuses 非空 && every ∈ {4,5}`。
  - `handleReset`：`queryForm.statuses = [...defaultShowStatuses.value]` + 关键词/日期清空。
- `types/index.ts`：`AppointmentConfig` 加 `defaultShowStatuses?: string`。
- `api/schedule.ts`：`getReserveList` 参数加 `statuses?: string`。

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --noEmit` | 通过（exit 0，无输出） |
| 迁移 | 19 脚本执行 + 列结构核对 | 列存在；store 1/7 两行均回填 `1,2,3,4,5` |
| 后端构建 | schedule-service `mvn package`（先 kill 旧进程再打包）+ 重启 8285 | `Started ScheduleServiceApplication`；端口属主核对为新 PID |
| API | `GET /appointment-config`（careld3 真实 token） | 返回 `defaultShowStatuses: "1,2,3,4,5"` |
| API | PUT 空值 / `"1,6"` / 合法 `"1,2"` | 400「至少勾选一项」/ 400「选择不合法」/ 200 且 DB 落库；测后已恢复原值 |
| API | `GET /schedules/reserves` statuses 10 种组合 vs DB 真值（store 7 全量 30 条：1→7,2→1,3→6,4 非爽约→10,4 爽约→6） | **ALL PASS**（首轮发现 real-only 组合误含非爽约取消记录 +10，修复 `applyStatusesFilter` 补"无 4/5 只按真实状态"分支后全对） |
| 浏览器 | 预约规则页：五项勾选框渲染 + 按 DB 全勾回显；全不勾选保存被拦截（表单报「至少勾选一项」、DB 未写入）；重勾保存成功写库 | 通过（`is-success` 状态复核；失败/成功均以 DB `updated_at` 佐证） |
| 浏览器 | 预约记录页进入：回显「已预约 + 4」；实际请求 `statuses=1,2,3,4,5&startDate=2026-09-14`；共 21 条 = API = DB | 三方一致 |
| 浏览器 | 只选「已爽约」→ 请求 `statuses=5`（不带 startDate）、共 6 条 = DB 爽约数；行状态全部真实渲染为已爽约 | 通过 |
| 浏览器 | 「重置」→ 关键词清空、状态恢复「已预约 + 4」、请求恢复默认参数、共 21 条 | 通过 |
| 浏览器 | 清空下拉（不限）→ 请求无 statuses、共 21 条 | 通过 |
| 浏览器 | 端到端联动：配置改 `"3,5"` → 重新进入页面回显「已完成 + 1」、请求 `statuses=3,5&startDate=今天`、共 5 条 = DB 期望；测后配置已恢复 `1,2,3,4,5` | 通过 |

**环境备注**：应用内浏览器无可见表面（viewport 0×0），DOM `click()` 同 tick 连点会因 Vue 批处理出现"最后一次点击生效"（勾选框批量操作需逐个带间隔点击）；元素离场 transition 不触发导致残留节点（双 tag、stale 错误文案），属环境假象，已用"过滤 leave 节点 / `is-success` 表单状态 / DB 佐证"复核。

**并发写入说明**：验证期间（17:52）另有并发会话对同一门店配置做了 2 次保存（`1,2,3,5` → `1,2,3`，经 schedule-service DEBUG 日志 `updateById` 参数确认非本次验证动作，疑似他人在浏览器手动测试勾选框）。本批次验证所改配置值均已还原；最终库中值以当前页面展示为准——如需恢复全选，在预约规则页勾选五项后保存即可。

---

## 5. 遗留与注意事项

1. **视觉走查**：多选下拉收于 180px 宽，全选时显示「已预约 + 4」；像素级观感待用户走查，如需展示更多标签可加宽。
2. **多状态 SQL 语义**：`statuses` 为空数组与不传等价（后端均走原 status/noShowFlag 逻辑）；`statuses` 含非法值（如 9）时仅忽略该项，不报错——前端已保证只送 1-5。
3. **每个医院的配置独立**：预约规则为"每医院单行"，各门店可配置不同默认显示；家长端/其他端不受此配置影响（仅 store-web 预约记录页读取）。
