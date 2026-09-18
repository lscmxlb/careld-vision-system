# 医院端 store-web 测试反馈改造 Spec（第二批：第 11–18 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（前端 + 微服务 + 数据库迁移） |
| 来源 | docs/测试记录.txt 第 11–18 项 |
| 涉及端 | 门店医院Web端（store-web）、排班微服务（careld-schedule-service）、门店微服务（careld-store-service）、数据库迁移 |
| 涉及页面 | /appointment-record（预约记录）、/child（儿童档案）、系统设置-科室管理、/care-record（养护记录） |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + API（真实登录态 curl）+ 浏览器（DOM/Vue 状态/图表 option）三层验证通过；测试数据已全部还原） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 11 | 预约记录 | 已爽约独立筛选；爽约与取消不同——取消**退还**预约次数，爽约**不退还**（手动/自动均不退还） | 前端 + 后端 | ✅ 已完成 |
| 12 | 儿童档案-预约详情弹窗 | 日期/时间分列；缴费金额右对齐；缴费方式移至缴费金额前一列；"授予"改为"预约授权" | 前端 | ✅ 已完成 |
| 13 | 儿童档案列表 | 年龄与家长手机之间增加"家长姓名"列；内容超宽支持左右滑动 | 前端 | ✅ 已完成 |
| 14 | 系统设置-科室管理 | 新增"收费标准"列，编辑科室时在科室类型下方录入，单位元，手动输入 | 前端 + 后端 + DB | ✅ 已完成 |
| 15 | 儿童档案-预约授权弹窗 | 授权次数上移至缴费方式上方且必填；自费支付/医保-个人余额/医保-统筹支付时自动计算缴费金额＝次数×科室收费标准；免费体验/其它为 0 元 | 前端 | ✅ 已完成 |
| 16 | 预约记录 | 开始养护确认后弹窗自动关闭 | 前端 | ✅ 已完成 |
| 17 | 养护记录列表 | 性别/手机号码/养护次数/可用次数/状态/首次视力/当前视力 7 列居中（状态后"..."按拍板暂不处理） | 前端 | ✅ 已完成 |
| 18 | 养护记录-详情页 | 7 项布局与图表调整（档案编号入标题、字段重排、删 5 字段、两次数对调、图表上限 5.3、图例禁点击） | 前端 | ✅ 已完成 |

---

## 2. 关键设计决策（含用户拍板）

1. **#11 爽约状态模型**：沿用既有 `reserve_order.status=4`，以 `no_show_flag=1` 与"取消"区分（不新增状态值）。前端筛选值为虚拟值 `5`（已爽约），查询映射：`5 → noShowFlag=true`；`4（已取消）→ noShowFlag=false`（排除爽约）。
2. **#11 次数退还口径**：爽约**不退还**预约次数——写入 `child_service_record`（type=5 爽约不退还、count=0）作为流水留痕；与取消（type=3 取消退还、count=+1）明确区分。手动标记与自动标记口径一致。
3. **#11 时间范围**：查询"已爽约/已取消"时不限制 `startDate`（今天起），以便检索历史记录。
4. **#15 收费标准来源**（拍板）：取本门店**第一条启用科室**（`status=1`）的 `charge_standard`，无需弹窗选科室；未配置时按 0 计算。
5. **#18(4) 视力排布**（拍板）：沿用 `el-descriptions` 结构，视力状况（label+值）与裸眼视力（label+值）各占 2 列（`span=2`），保留标签。
6. **#17 状态列"..."**（拍板）：先不管该内容，本轮仅做 7 列居中。

---

## 3. 各项实现要点

### 3.1 #11 已爽约独立筛选 + 不退还次数

**后端（schedule-service）**

- `ScheduleService`：
  - `listReserves(..., Boolean noShowFlag, ...)` 新增过滤参数；
  - `void markNoShow(Long id)`（手动爽约）、`void autoMarkNoShow(Long id)`（自动爽约）统一走 `applyNoShow(order, operatorId, operatorName, reason, noRefundRemark)`。
- `applyNoShow` 逻辑：`status=4`、`no_show_flag=1`、`refund_flag=0`、写取消原因与取消时间；释放时段占用（`slot.booked` 递减或 `schedule.reserved` 递减）；**不退还次数**（写 type=5 流水）。
- `listReserves` 过滤：`noShowFlag=true → eq(no_show_flag, 1)`；`noShowFlag=false → isNull(no_show_flag) OR ne(no_show_flag, 1)`。
- `ScheduleController`：`GET /api/v1/schedules/reserves` 增加 `noShowFlag` 请求参数；`POST /api/v1/schedules/reserves/{id}/no-show` 调 `markNoShow`。
- 自动爽约任务 `ReserveAutoTask`（每 5 分钟扫描，`autoNoShowHours` 默认 12h，迁移 17 已配置）改走同一 `applyNoShow`，保证"自动标记也不退还"。

**前端（store-web）**

- `api/schedule.ts`：`getReserveList` 参数加 `noShowFlag?: boolean`；新增 `markNoShow(id)`。
- `views/appointment-record/index.vue`：
  - 状态下拉新增"已爽约"（`:value="5"`）；
  - `fetchReserves` 映射：`status===5 → {status: undefined, noShowFlag: true}`；`status===4 → {noShowFlag: false}`；
  - 状态列标签：`noShowFlag===1` 显示红色"已爽约"（优先），否则按原有逻辑；
  - 标记爽约弹窗：去掉原因单选项，固定提示"标记爽约不退还预约次数（与取消不同）"。

**兼容性**：历史爽约数据（早期逻辑 refund_flag=1）不改写，`no_show_flag=1` 的行均计入"已爽约"筛选。

### 3.2 #12 / #13 儿童档案

- `views/child/index.vue` 预约记录弹窗（流水）：
  - 日期列 `createdAt.slice(0,10)`、时间列 `createdAt.slice(11,16)` 分两列；
  - 列顺序：… → 缴费方式 → 缴费金额（`align="right"`）→ …；
  - `getChangeTypeText`：`1 → '预约授权'`（原"授予"）。
- 列表：`age` 与 `phone` 之间插入"家长姓名"列（`prop="parentName"`，宽 100）；表格内容超宽时 Element Plus 自动出现横向滚动条（`el-table--scrollable-x`）。

### 3.3 #14 科室收费标准

- 迁移 `database/mysql/migration/18-store-department-charge-standard.sql`：

```sql
ALTER TABLE store_department
    ADD COLUMN charge_standard DECIMAL(10,2) NULL COMMENT '收费标准(元)' AFTER dept_type;
```

  > 注意：MySQL 不支持 `ADD COLUMN IF NOT EXISTS`，手工执行一次（已执行）。
- `careld-store-service`：`Department.java` 新增 `BigDecimal chargeStandard`；Mapper 为 `SELECT *`，新列自动带出，无 SQL 改动。
- 前端 `views/department/index.vue`：表格"科室类型"列后新增"收费标准"列（`x 元` / `-`）；编辑/新增表单在"科室类型"下方加 `el-input-number`（`precision=2`、`step=10`、`min=0`）+ "元" 后缀。
- 类型：`types/index.ts` `Department` 加 `chargeStandard?: number`。

### 3.4 #15 预约授权弹窗自动计算

- 弹窗字段顺序：当前剩余 → 开单医师 → **授权次数（必填，上移至缴费方式上方）** → 缴费方式 → 缴费金额 → 备注。
- `loadDepartmentStandard()`：拉取门店科室列表，取第一条 `status===1` 的 `chargeStandard`（默认 0）。
- `calcPaymentAmount()`：缴费方式 ∈ {自费支付, 医保-个人余额, 医保-统筹支付} → `round(授权次数 × 收费标准, 2)`；免费体验/其它 → 0。
- 触发时机：弹窗打开（`loadDepartmentStandard().then(calcPaymentAmount)`）、授权次数 change、缴费方式 change。
- 校验：`changeCount` 必填；提交时提示"已预约授权 X 次"。

### 3.5 #16 开始养护关弹窗

- `handleCareStart` 成功后：`ElMessage.success('已开始养护')` → `careVisible = false` → 刷新列表（status 变 2）。

### 3.6 #17 / #18 养护记录

- 列表：性别、手机号码、养护次数、可用次数、状态、首次视力、当前视力共 7 列 `align="center"`。
- 详情弹窗（`views/care-record/index.vue`）：
  1. 标题：`儿童基本信息[档案编号：{childCode}]`；
  2. `el-descriptions` 字段重排：第一行 儿童姓名/性别/出生日期/所在学校；第二行 家长姓名/关系/手机号码/家庭地址；第三行 视力状况(span2)/裸眼视力(span2)；第四行 主治医师/累计养护次数/可用次数（后两者对调）；
  3. 删除：分娩方式、日常作息、既往病史、家族病史、过敏信息；
  4. 图表：`yAxis.max = 5.3`（去掉 5.4 行）；`legend.selectedMode = false`（图例不支持点击）。

---

## 4. 验收记录（2026-09-14）

| 层面 | 结论 |
|---|---|
| 类型检查 | `vue-tsc --noEmit` 无错误 |
| API（真实登录态） | #11 三种查询模式实测：`noShowFlag=true`→6 条全爽约；`status=4&noShowFlag=false`→10 条全非爽约；`status=4` 无 flag→16 条合并。写路径实测（order 53）：`status=4/no_show_flag=1/refund_flag=0`、儿童剩余次数**不退还**、slot 释放、type=5 流水已写 |
| 浏览器 | 预约记录"已爽约"筛选 5 条（含历史 09-12/09-13）且"已取消"10 条排除爽约；标记爽约弹窗无单选项；#14 科室列"200 元"与编辑录入；#15 自动计算（1×200=200、3→600、免费体验→0、医保-统筹支付→600）且必填标记正确；#16 确认后弹窗关闭且行状态刷新；#12/#13 列序/右对齐/家长姓名列/横向滚动条；#17 7 列居中精确匹配；#18 标题格式、13 字段顺序、5 字段已删、图表 `yAxis.max=5.3` 与 `legend.selectedMode=false` |
| 测试数据 | 已全部还原：order 52/53、child 38 剩余次数、slot 2099411239993241604、child_service_record 77/78、care_record 37 |

---

## 5. 遗留事项

1. 科室"收费标准"为业务配置项：测试库门店 7 的科室（id=8"视力校正门诊"）已填 **200 元**，需门店按实际标准修改。
2. 历史爽约数据（早期 `refund_flag=1`）保留原样，不做回溯改写。
3. （非本次范围）vision-service 仍无鉴权链，建议后续按 schedule-service GuardFilter 方案统一处理。
