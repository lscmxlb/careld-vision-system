# parent-h5 儿童档案详情新增「服务电话」行 Spec（第 106 项，编号自拟）

| 项目 | 内容 |
|---|---|
| 文档类型 | 前后端 Spec（child-service 详情聚合字段 + parent-h5 详情页展示行） |
| 来源 | 用户需求（未给编号，按序列自拟为第 106 项）：http://39.162.49.28:5177/#/pages/appointment/list 「儿童档案的档案详情中，在建档医院的下方，添加一行"服务电话"，这个内容取自己医院基础信息中的服务电话」 |
| 涉及端 | parent-h5（5177）`pages/child/detail`；后端 careld-child-service（8284） |
| 状态 | 已开发（2026-09-20，API 对账 + 浏览器 DOM 断言 + 基础信息保存路径联动验证通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 106-1 | 新增行 | 儿童档案「档案信息」卡片中，在「建档医院」**下方**新增一行「服务电话」 |
| 106-2 | 取数口径 | 值 = **该儿童建档医院在医院端基础信息中维护的服务电话**（`store_department.service_phone`，与 #98 新增的字段同源） |
| 106-3 | 空值展示 | 医院未维护时显示「—」（与同卡片其它行一致） |

**页面定位说明**：用户给出的链接是预约记录页（`#/pages/appointment/list`），但 parent-h5 中「建档医院」这一行只存在于 **儿童档案 → 档案详情**（`pages/child/detail.vue`），预约记录页仅展示预约卡片、无档案详情区块，故实现落在 `pages/child/detail.vue`。

---

## 2. 现状诊断

- parent-h5 档案详情页 `pages/child/detail.vue` 的「档案信息」卡片当前行序：档案编号 → 儿童姓名 → **建档医院** → 主治医师 → 性别 → …
- 「建档医院」取 `child.storeName`，由 child-service 详情接口 `GET /api/v1/children/{id}` 下发（`ChildMapper.ENRICHED_COLUMNS` 中 `LEFT JOIN store_info` 得到）。
- 医院端基础信息页维护的服务电话存在**科室表** `store_department.service_phone`（#98 新增列），该页取科室列表第一条（`ORDER BY sort_order, id`）展示与编辑；档案详情此前未下发该字段。

---

## 3. 改造设计

### 3.1 后端（careld-child-service）

| 文件 | 改动 |
|---|---|
| `mapper/ChildMapper.java` | `ENRICHED_COLUMNS` 增加子查询：`(SELECT d.service_phone FROM store_department d WHERE d.store_id = c.store_id AND d.deleted_at IS NULL ORDER BY d.sort_order ASC, d.id ASC LIMIT 1) AS store_service_phone` |
| `entity/ChildProfile.java` | 新增派生字段 `@TableField(exist = false) private String storeServicePhone;`（不映射本表列） |

取数口径与医院端基础信息页**完全对齐**：同样按 `sort_order, id` 取第一条科室、同样不筛 `status`（该页列表也未按状态过滤），若首条科室的服务电话为空则返回空，不做「取有值的那条」的兜底，避免两处显示不一致。

该列随 `ENRICHED_COLUMNS` 一并被列表 / 详情 / 认领查询复用，列表接口多返回一个字段（前端不展示），无破坏性影响；子查询按 `store_id` 走 `uk_store_dept` 前缀索引。

### 3.2 前端（parent-h5）

| 文件 | 改动 |
|---|---|
| `src/types/index.ts` | `Child` 接口新增 `storeServicePhone?: string` |
| `src/pages/child/detail.vue` | 模板「档案信息」卡片中 `建档医院` 行的**下一行**插入：`<view class="kv"><text class="kv-key">服务电话</text><text class="kv-value">{{ child.storeServicePhone || '—' }}</text></view>`，沿用既有 `kv / kv-key / kv-value` 样式，无新增样式 |

---

## 4. 验证计划与结果（2026-09-20 真机）

| 手段 | 结果 |
|---|---|
| child-service 构建重启 | ✅ `mvn package -pl careld-child-service -am`，8284 重启后 `Started ChildServiceApplication` |
| API 对账（3 家门店） | ✅ 临时把门店 1 科室 1 服务电话写入后，`GET /children/1`（家长 13600136001）返回 `storeName=北京朝阳门店`、`storeServicePhone=010-88886666`；`GET /children/62`（门店 11，真实数据）返回 `4008887888`；`GET /children/4`（门店 2，科室未维护）返回 `null` —— 逐店取值与 `store_department` 一致 |
| 基础信息保存路径联动 | ✅ 用 store-web 基础信息同款接口 `PUT /api/v1/departments/1`（store001_mgr，服务电话改为 `010-55556666`）→ DB 落库 → parent-h5 档案详情再次进入后 DOM 显示 `010-55556666`（未改任何前端代码），证明取的是基础信息维护值而非副本 |
| 浏览器 DOM 断言（5177） | ✅ 家长 13600136001 登录后，儿童档案 → 档案详情（child id=45 王小刚）：行序 `儿童姓名 → 建档医院(北京朝阳门店) → 服务电话 → 主治医师`，服务电话行紧跟建档医院下方 |
| 数据复原 | ✅ 再用同款接口把服务电话置空（`servicePhone: null`，走 #98 的显式置 NULL 分支）→ DB 三行科室恢复原状；此时详情页该行显示「—」，位置不变 |
| `vue-tsc --noEmit`（parent-h5） | ✅ EXIT=0 |

---

## 5. 遗留与说明

1. 只在 **parent-h5 档案详情**展示；医生端（doctor-h5）儿童档案详情、parent-web 端未同步该行（本次需求未涉及），如需可另提。
2. 「服务电话」取的是**该儿童建档医院**（`child_profile.store_id`）的科室服务电话；若同一医院后续新增/调整科室排序，展示值会随之切换到新的首条科室（与医院端基础信息页行为一致）。
3. 编号自拟：本项用户未给编号，按序列记为 #106（自拟序列当前至 #106）。
4. 与 #63–#105 同属未提交改动，等待确认后一并入库。
