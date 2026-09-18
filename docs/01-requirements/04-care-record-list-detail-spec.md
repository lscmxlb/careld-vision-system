# 养护记录列表与详情改造 Spec

| 项目 | 内容 |
|---|---|
| 文档类型 | 功能设计 Spec（列表改造 + 详情弹窗新增） |
| 涉及端 | 医院端 Web（store-web）、养护查询服务（careld-vision-service）、儿童档案服务（careld-child-service，仅复用接口） |
| 涉及页面 | http://localhost:5175/care-record（养护记录） |
| 状态 | 已开发（2026-09-14，API + 浏览器验证通过；含开发中追加的"姓名筛选双模式"修复） |

---

## 1. 背景与需求

### 1.1 现状

- 养护记录页现为**按日期分组的横向卡片流**：按 `care_date` 分组展示卡片（儿童名/状态/时段/养护前后视力/家长/执行人），点击卡片弹出 520px 详情弹窗（仅基础字段）。
- 后端 `careld-vision-service` 提供 `GET /api/v1/care-records/page`（JOIN child_profile，支持儿童姓名/家长姓名/手机号/养护次数筛选），返回字段见 5.1；`GET /api/v1/care-records?childId=` 返回某儿童全部养护记录（日期倒序）。
- 儿童姓名当前展示 `name_mask`（脱敏，如"张*丰"），与预约记录页"显示全名"的口径不一致。

### 1.2 需求（用户原话拆解）

**（1）列表改造**：养护记录默认为列表（表格）形式，展示列＝养护日期、养护时段、儿童姓名、性别、手机号码、养护次数（该儿童累计养护次数）、可用次数、养护人、首次视力（建档时双眼视力）、当前视力（本次养护结束后双眼视力）、备注；右侧增加"详情"按钮。

**（2）详情弹窗**：横屏页面、占用较大区域；上部为儿童基本信息（与儿童档案相同，只显示不编辑）；下部为趋势图表，显示每一次养护的一组数据（双眼/左眼/右眼 × 养护前后），每组数据下方显示三行：养护日期、时段、养护人。

---

## 2. 目标

1. 卡片流改为标准表格列表，一次性展示 11 个信息列 + 详情入口。
2. 新增横屏大尺寸详情弹窗：上部档案信息只读展示、下部可视化儿童历次养护视力趋势。
3. 姓名按全名展示（解密，失败回退掩码），手机号保持脱敏，与儿童档案/预约记录口径一致。
4. 不新增页面、不新增独立接口，复用儿童详情与养护记录查询接口。

---

## 3. 列表设计

### 3.1 列定义（表格）

| # | 列名 | 来源字段 | 展示说明 |
|---|---|---|---|
| 1 | 养护日期 | `care_record.care_date` | YYYY-MM-DD |
| 2 | 养护时段 | `care_record.time_slot` | `HH:mm-HH:mm`，空显示"-" |
| 3 | 儿童姓名 | `child_profile.name_encrypted` 解密（失败回退 `name_mask`） | **全名**（与预约记录一致） |
| 4 | 性别 | `child_profile.gender` | 1→男、0→女、空→"-" |
| 5 | 手机号码 | `child_profile.phone_mask` | 脱敏格式（如 135****0003），与档案列表一致 |
| 6 | 养护次数 | 该儿童累计养护次数（口径见 5.3） | 数字 |
| 7 | 可用次数 | `child_profile.remaining_count` | el-tag：>0 绿色、=0 灰色（与档案列表一致） |
| 8 | 养护人 | `care_record.executor_name` | 空→"-" |
| 9 | 状态 | `care_record.status` | 2→"已完成"绿色标签、1→"养护中"橙色标签（与改造前卡片标签一致） |
| 10 | 首次视力 | `child_profile.naked_vision_both` | 建档时裸眼双眼视力，空→"-" |
| 11 | 当前视力 | `care_record.vision_after_both` | 本次养护结束后双眼视力；养护中（未录入）→"-" |
| 12 | 备注 | `reserve_order.remark`（按 `appointment_id` 关联） | 本次养护对应预约单备注，空→"-" |
| 13 | 操作 | — | "详情"按钮（plain 小按钮、蓝色），打开详情弹窗 |

### 3.2 筛选、排序与分页

- 搜索栏**保持不变**：儿童姓名 / 家长姓名 / 手机号码（支持完整或片段）/ 养护次数（大于 N 次）。
- 姓名匹配为**掩码 + 解密明文双模式**（与儿童档案搜索口径一致）：输入全名（"刘畅"）、部分字（"畅"）或掩码（"*畅"）均可命中。实现为「先按门店解析匹配儿童 id 集合，再随分页 SQL 过滤」，分页性能不受影响（开发中追加，原因见 8.7）。
- 排序**保持** `care_date DESC, id DESC`（最近的养护在最上）。
- 分页保持 20 条/页。

### 3.3 交互

- 仅"详情"按钮打开详情弹窗（卡片点击打开详情的行为随卡片流一并移除）。
- 列表包含"养护中"记录（尚未完成养护），其"当前视力"列显示"-"。

---

## 4. 详情弹窗设计

### 4.1 布局

- `el-dialog` 横屏大尺寸：`width: 92%`（`max-width: 1400px`）、`top: 4vh`；标题"养护记录详情"；内容区超高时内部滚动。
- 上部：**儿童基本信息**（只读，见 4.2）。
- 下部：**视力趋势图表**（见 4.3），高度约 420~480px。

### 4.2 上部：儿童基本信息（与儿童档案一致，只读）

数据来源：`GET /api/v1/children/{childId}`（child-service，已返回明文姓名/手机，不下发密文）。

字段顺序与儿童档案表单保持一致，`el-descriptions`（`border`、3~4 列）：

| 分组 | 字段 |
|---|---|
| 基本 | 档案编号（child_code）、儿童姓名（明文）、性别、出生日期（含年龄）、所在学校 |
| 家长 | 家长姓名、关系、手机号码（明文，与档案一致）、家庭地址 |
| 视力 | 视力状况（`eye_condition`，多选按顿号"、"分割展示）、裸眼视力（双眼/左眼/右眼） |
| 其他 | 分娩方式、日常作息（入睡/起床）、既往病史、过敏信息、家族病史、主治医师 |
| 运营 | 当前可用次数（remaining_count）、累计养护次数 |

> 只显示不编辑：所有字段以文本/标签形式呈现，不提供任何输入控件；空值统一显示"-"。

### 4.3 下部：视力趋势图表（ECharts）

数据来源：`GET /api/v1/care-records?childId=X`（vision-service，返回该儿童全部养护记录），前端按`care_date + id` 升序排列。

- **X 轴**：每一次养护为一组（第 1 次最左）；每组下方显示**三行**标注——养护日期 / 养护时段 / 养护人（axisLabel 多行文本）。
- **每组数据（6 个）**：双眼前、双眼后、左眼前、左眼后、右眼前、右眼后。
- **取值**：视力值格式为"主值+次值"（如 `5.0+0`、`4.8-1`）。
  - 柱高（Y 轴）= 主值（4.0~5.3，0.1 步进）；
  - 柱标签/tooltip 显示**完整记录值**（如 `5.0+1`）；
  - 主值为空（未录入）时不绘制该柱。
- **配色建议**：按眼别分色、前浅后深（双眼=蓝系、左眼=橙系、右眼=绿系；前=浅色、后=深色），图例区分 6 个系列。
- **交互**：tooltip 显示该次养护全量数据；图表绘制**全部**养护记录，超出弹窗宽度时通过 `dataZoom` 横向滚动查看（不截断历史数据）。
- 图表形态：**分组柱状图**（已确认）。

---

## 5. 接口与字段口径

### 5.1 扩展现有接口：`GET /api/v1/care-records/page`（vision-service）

在 `CareRecordMapper.selectPageWithChild` 的 SQL 上扩展，**接口路径与入参不变**：

```sql
SELECT r.*,
       /* 新增字段 */
       c.gender            AS childGender,
       c.remaining_count   AS remainingCount,
       c.naked_vision_both AS nakedVisionBoth,
       c.name_encrypted    AS childNameEncrypted,   -- service 层解密为全名
       (SELECT COUNT(*) FROM care_record cr2
         WHERE cr2.child_id = r.child_id AND cr2.deleted_at IS NULL
           /* 口径见 5.3 */ ) AS careCount,
       o.remark            AS remark                -- 预约备注
FROM care_record r
LEFT JOIN child_profile c ON c.id = r.child_id AND c.deleted_at IS NULL
LEFT JOIN reserve_order o ON o.id = r.appointment_id AND o.deleted_at IS NULL
...（原有筛选条件与排序不变）
```

实体 `CareRecord`（vision-service）新增 `@TableField(exist = false)` 字段：`childGender`、`remainingCount`、`nakedVisionBoth`、`careCount`、`remark`；`childName` 改为解密后的全名（解密失败回退 `name_mask`）。

**解密能力**：vision-service 尚无加解密配置，需在 `application.yml` 增加：

```yaml
encryption:
  key: ${ENCRYPTION_KEY}          # 与 child-service 同源（.env 注入）
```

Service/Controller 层使用 `com.careld.common.security.AesUtil.decrypt`，兼容种子数据的 `ENC:明文` 占位格式（与 child-service `decryptForDetail` 同逻辑）。

### 5.2 复用接口（不新增）

| 用途 | 接口 | 服务 |
|---|---|---|
| 详情弹窗上部档案信息 | `GET /api/v1/children/{id}` | child-service（8284） |
| 详情弹窗图表数据 | `GET /api/v1/care-records?childId=X` | vision-service（8286） |
| 列表数据 | `GET /api/v1/care-records/page` | vision-service（8286） |

### 5.3 字段口径

| 字段 | 口径 | 备注 |
|---|---|---|
| 养护次数 | 该儿童**累计已完成养护次数**（`status=2`） | 已确认：与搜索栏"养护次数大于"筛选口径一致 |
| 可用次数 | `child_profile.remaining_count` | 与档案列表、预约选人同源 |
| 首次视力 | `child_profile.naked_vision_both` | 建档时录入的裸眼双眼视力 |
| 当前视力 | `care_record.vision_after_both` | 本次养护结束后双眼视力 |
| 备注 | `reserve_order.remark`（按 appointment_id） | 预约时的备注信息 |

---

## 6. 前端实现要点

| 文件 | 改动 |
|---|---|
| `frontend/store-web/src/views/care-record/index.vue` | 卡片流 → el-table 列表 + 新详情弹窗（上部 descriptions + 下部 ECharts） |
| `frontend/store-web/src/types/index.ts`（CareRecord 类型） | 新增 `childGender/childPhone/careCount/remainingCount/nakedVisionBoth/remark` 字段 |
| `frontend/store-web/src/api/` | 复用现有 `careRecordApi` 与儿童详情 API，无新增 |

- 图表按 `dashboard/index.vue` 的现有用法（`echarts.init` + resize 监听），不引入新依赖（echarts 6 / vue-echarts 已装）。
- 操作列按钮样式沿用 plain 小按钮规范（蓝）。
- 视力值解析复用"主值+次值"正则（`^(5\.[0-3]|4\.[0-9])([+-]\d+)?$`）。

---

## 7. 验收标准

1. 养护记录页为表格列表，列顺序与 3.1 一致，右侧有"详情"按钮。
2. 儿童姓名显示**全名**、手机号**脱敏**；解密失败/占位数据（`ENC:`）可正常回退不报错。
3. "养护次数"为该儿童累计次数、"可用次数"与档案一致；空值列显示"-"。
4. 筛选（姓名/家长/手机/次数）与分页工作正常，排序为日期倒序。
5. 点击"详情"弹出横屏大弹窗：上部档案字段与儿童档案一致且不可编辑，空值显示"-"。
6. 下部图表：每次养护一组、每组 6 个数据（双眼/左眼/右眼 × 前后），组下三行显示日期/时段/养护人。
7. 养护中记录（无养护后视力）在列表中正常展示（当前视力"-"），图表中不绘制缺失柱。
8. 数据权限：门店用户仅能看到本门店养护记录（沿用现有 DataScope 过滤）。

---

## 8. 评审结论（2026-09-14）

1. **趋势图形态**：分组柱状图（✅ 已确认）。
2. **养护次数口径**：仅"已完成"（`status=2`，与搜索筛选一致）（✅ 已确认）。
3. **手机号展示**：列表脱敏、详情弹窗上部明文（与儿童档案一致）（✅ 已确认）。
4. **列表"状态"标识**：**新增"状态"列**（"养护人"列后，显示"已完成/养护中"彩色小标签）（✅ 已确认，方案 A）。
5. **图表数据展示**：绘制全部养护数据、横向滚动查看（✅ 已确认，取消"默认最近 8 次"方案）。
6. **关联风险（非本次范围）**：vision-service 目前无鉴权链（匿名请求可查数据），属已知问题，建议后续按 schedule-service 的 GuardFilter 方案统一处理。
7. **开发中追加修复（姓名筛选双模式）**：列表姓名改为显示全名后，原"按 `name_mask` LIKE"的筛选导致搜全名（如"刘畅"）命中不到（掩码为 `*畅`）。已对齐儿童档案搜索口径：掩码与解密明文双模式匹配，实现为「先按门店解析匹配儿童 id 集合，再随分页 SQL 过滤」（2026-09-14 已开发验证）。

### 3.1-a 列表"状态"标识（对应结论 4：**采用方案 A，已确认**）

**问题背景**：列表中的养护记录有两种状态——

| 状态 | 含义 | 列表表现 |
|---|---|---|
| 已完成 | 养护流程走完，养护后视力已录入 | 当前视力有值 |
| 养护中 | 客户已到店、点了"开始养护"，但还没点"结束养护" | 当前视力为空（显示"-"），养护后数据均无 |

"养护中"记录也会出现在列表中。若列表**不加**状态标识，这类行的"当前视力"显示"-"，店长无法区分"客户正在做养护"与"数据缺失"。改造前的卡片上有"已完成/养护中"彩色标签，改为表格后是否保留该信息由用户决定。

**备选方案**：
- 方案 A：新增"状态"列（"养护人"列后），显示"已完成/养护中"小标签（与改造前卡片标签一致）。
- 方案 B：不单独占列，"养护中"记录在"当前视力"列显示"-（养护中）"小字标注。
- 方案 C：严格按 11 列展示，不加任何状态标识。

> 已按方案 A 实现（见 3.1 表第 9 列"状态"）。

---

## 9. 实现要点备忘

- 后端改动集中在 `careld-vision-service`：`CareRecord.java`（实体字段）、`CareRecordMapper.java`（SQL）、新增解密处理（可在 Controller 或新 Service 内，参照 child-service `decryptForDetail`）、`application.yml`（encryption.key）。
- `care_record` 表本身**无需加列**（备注取自预约单；首次视力/次数取自 child_profile）。
- 兼容性：`name_encrypted` 为 `ENC:明文` 占位（种子数据）或真实 AES 密文，解密失败回退 `name_mask`。
- 前端 `GET /care-records?childId=` 返回的是无 JOIN 的基础实体，图表数据够用；如需养护人/时段已有字段即可。
