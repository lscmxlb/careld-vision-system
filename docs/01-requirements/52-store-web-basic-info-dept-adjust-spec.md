# store-web 基础信息页科室信息调整 Spec（第 98 项，用户编号）

| 项目 | 内容 |
|---|---|
| 文档类型 | 前后端 Spec（表单字段调整 + DB 迁移 + 随机科室编码 + 新增门店名称更新端点） |
| 来源 | 用户需求（用户给定编号 **#98**）：http://39.162.49.28:5175/basic-info 系统设置-基础信息页面优化调整，共 5 项 |
| 涉及端 | store-web（5175）`/basic-info`；careld-store-service（8283）；DB 迁移 23 |
| 状态 | 已开发（2026-09-20，真机 API+DB 对账与浏览器 DOM 断言通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 98-1 | 隐藏科室编码 | 科室编码行从基础信息页移除；新增科室时由后端自动生成**随机 6 位数字**编码，不对外展示 |
| 98-2 | 医院名称 | 原科室编码位置改为「医院名称」，取后台系统中的门店名称，允许在本页修改 |
| 98-3 | 科室类型选项 | 可选项调整为：儿童保健科、妇幼保健科、中医科、眼科、其它科室（对应值 1–5） |
| 98-4 | 服务电话 | 科室类型下一行新增「服务电话」，由用户手动录入 |
| 98-5 | 收费标准单位 | 收费标准后的「元」改为「元/次」 |

---

## 2. 现状诊断

- 基础信息页第一行为「科室编码」输入框，需人工录入且与唯一键 `uk_store_dept (store_id, dept_code)` 绑定；用户不希望该编码对外暴露。
- 科室类型下拉为旧口径（1=门诊 2=养护 3=检测 4=其他），与医院业务实际科室分类不符。
- `store_department` 无服务电话字段；`charge_standard` 列已存在（前序需求新增），但前端单位文案为「元」。
- 门店名称此前仅存在于 `store_info`，医院端无修改入口；`PUT /stores/{id}` 需要 `store:list:update` 权限（运营中心权限，`hospital_admin` 角色不具备），医院端不能复用它改本店名称。

---

## 3. 改造设计

### 3.1 数据库（迁移 23）

`database/mysql/migration/23-store-department-service-phone.sql`：

```sql
ALTER TABLE store_department
    ADD COLUMN service_phone VARCHAR(32) NULL COMMENT '服务电话' AFTER dept_type;
```

同步更新 `docs/03-database/04-hospital-management-schema.md`：`store_department` 字段表补 `service_phone`、`charge_standard`，`dept_type` 注释改为 1儿童保健科/2妇幼保健科/3中医科/4眼科/5其它科室。

### 3.2 后端（careld-store-service）

| 文件 | 改动 |
|---|---|
| `entity/Department.java` | 新增 `servicePhone` 字段；`deptType` 注释更新为 5 类 |
| `service/impl/DepartmentServiceImpl.java` | ① `createDepartment`：`deptCode` 为空时用 `SecureRandom` 生成随机 6 位数字（`%06d`），唯一键冲突（含软删除行占用编码）捕获 `DuplicateKeyException` 重试，最多 20 次；② `updateDepartment`：**空 deptCode 置 null 不覆盖已有编码**（避免二次保存把编码清空）；`chargeStandard`/`servicePhone` 显式为 null 时用 `LambdaUpdateWrapper` 落 NULL，支持清空 |
| `service/StoreService.java` + `impl/StoreServiceImpl.java` | 新增 `updateCurrentStoreName(storeId, storeName)`：仅改本店名称，校验非空、trim、≤128 字符 |
| `controller/StoreController.java` | 新增 `PUT /api/v1/stores/current/name`（`@OperationLog`，无 `@RequirePermission`，与 `/stores/current` 同模式，用 `UserContext.getCurrentStoreId()` 限本店） |

权限说明：`hospital_admin` 角色无 `store:list:update`，故不能走 `PUT /stores/{id}`；新端点沿用该服务既有「登录即可访问 + 业务层限本店」的模式（DepartmentController 全部端点亦无权限注解）。无 token 调用时 `UserContext` 为空 → `getCurrentStoreId()` 为 null → 业务码 400 拒绝，写操作无法触达数据（fail-closed）。

### 3.3 前端（store-web）

| 文件 | 改动 |
|---|---|
| `types/index.ts` | `Department` 增加 `servicePhone?: string \| null`；`deptType` 注释改为 5 类 |
| `api/store.ts` | 新增 `updateCurrentStoreName(storeName)` → `PUT /stores/current/name` |
| `views/basic-info/index.vue` | 表单行序改为：医院名称(必填) → 科室名称(必填) → 科室类型(必填，5 项) → 服务电话 → 收费标准(元/次)；删除科室编码行；`loadStoreName()` 取 `/stores/current`；保存时若医院名称有改动先更新门店（并同步导航栏显示），科室保存 `deptCode` 原样带回（新建时为空由后端生成）；`servicePhone` 空串转 null |

`deptForm.deptCode` 保留但不再有输入控件：编辑时随记录带回，新建时为空串 → 后端生成随机编码。

---

## 4. 验证计划与结果（2026-09-20 真机）

### 4.1 迁移与构建

| 手段 | 结果 |
|---|---|
| 迁移 23 应用沙箱 DB | ✅ `service_phone` 列已建 |
| `mvn package -DskipTests -q -pl careld-store-service -am` | ✅ EXIT=0，8283 重启成功（`Started StoreServiceApplication in 2.841 seconds`） |
| `vue-tsc --noEmit`（store-web） | ✅ EXIT=0 |

### 4.2 API + DB 对账

| 手段 | 结果 |
|---|---|
| 新增科室不传 `deptCode` | ✅ 落库 `dept_code=298192`（随机 6 位数字） |
| 更新时传空 `deptCode`（二次保存场景） | ✅ 编码保持 `298192` 不变（修复前会被清空，见 §5.1） |
| 更新 `servicePhone=027-88886666`、`chargeStandard=66.5` | ✅ 落库一致 |
| 显式传 null 清空服务电话 | ✅ `service_phone` 置 NULL |
| 无 token 调用 `PUT /stores/current/name` | ✅ 业务码 400「当前用户未绑定门店」（HTTP=200 为 Result 统一包装，实际拒绝） |
| 名称更新与恢复 | ✅ 门店账号改为「北京朝阳门店-验证」→ DB 生效 → 恢复「北京朝阳门店」 |
| 临时验证数据 | ✅ 测试科室硬删除，store 1 科室数回到 3 |

### 4.3 浏览器 DOM 断言（/basic-info，store001_mgr，0×0 隐藏视口）

| 断言 | 结果 |
|---|---|
| 表单行序 | ✅ 医院名称 → 科室名称 → 科室类型 → 服务电话 → 收费标准（无「科室编码」行） |
| 医院名称回显 | ✅ 「北京朝阳门店」（取 `/stores/current`） |
| 科室类型选项 | ✅ 打开下拉：儿童保健科/妇幼保健科/中医科/眼科/其它科室，共 5 项；已选显示「儿童保健科」（与 `dept_type=1` 一致） |
| 服务电话 | ✅ 输入框存在，placeholder「请输入服务电话」 |
| 收费标准单位 | ✅ 「元/次」 |
| UI 保存全链路 | ✅ 改医院名称+服务电话 → 「保存成功」→ DB 落库（`store_info.store_name`、`store_department.service_phone`）→ 导航栏名称同步更新 → UI 改回并再次保存 → DB 完全复原 |
| 保存后离开页面 | ✅ 不弹未保存提醒（baseline 正确重置） |

---

## 5. 遗留与说明

1. **编号冲突**：用户给定的 #98 与本地自拟的「第 98 项」（spec 45，dashboard 统计卡双指标）重号。两份 spec 均保留，以**用户编号**为权威；后续自拟编号仍按历史序列顺延（当前至 #104）。
2. **admin-web 科室管理页未同步新类型文案**：`/department` 页仍为「门诊/养护/检测」，且仍展示科室编码列。用户本次未要求改动；其中「科室类型」文案已在 #99（spec 53）的医院编辑页按新口径落地。
3. 一个门店存在多个科室时，基础信息页仅维护第一条记录（原有逻辑，与 `loadDepartment` 取 `list[0]` 一致）；科室编码在 admin-web 科室列表仍可见（用户仅要求医院端隐藏）。
4. 科室编码为随机 6 位，理论存在 1/100,000 碰撞概率，已由唯一键 + 重试 20 次兜底；软删除记录仍占用编码，重试逻辑已覆盖。
5. 应用内浏览器为 0×0 隐藏视口，无法截图，最终视觉效果以用户确认为准。
6. 与 #63–#104 同属未提交改动，等待确认后一并入库。
