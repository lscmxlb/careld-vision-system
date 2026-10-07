# 63 · admin-web 医院列表「新增/编辑医院」弹窗新增「医院类型」字段

- **编号**：#111（自拟，用户未给编号）
- **提交人**：用户（对话，附新增医院弹窗截图）
- **日期**：2026-10-05
- **状态**：已开发（2026-10-05，沙箱真机验证 + API/DB 对账，见 §5）

## 1. 需求清单（用户原话拆解）

> 「http://39.162.49.28:5172/store/list 这个医院的医院名称前面加一行，叫做"医院类型"，是必选的选择项，选项为社区卫生服务中心、卫生院、妇幼保健院、医院、其它这几个选项，默认选项为社区卫生服务中心」

1. admin-web 医院列表页（`/store/list`）新增/编辑医院弹窗中，在「医院名称」上方新增一行「医院类型」。
2. 该字段为**必选**（表单必填校验）。
3. 选项固定 5 个：社区卫生服务中心 / 卫生院 / 妇幼保健院 / 医院 / 其它。
4. **默认选项为「社区卫生服务中心」**（新增时的默认值）。

## 2. 现状诊断

- 弹窗表单位于 `frontend/admin-web/src/views/store/list.vue`，当前行序：机构性质（单选）→ 医院编码 → 所属运营中心 → 所属代理商 → **医院名称** → 所在地区 → …
- `store_info` 表无医院类型字段（现有 `institution_type` 为「机构性质」，语义不同，不能复用）。
- 新增/编辑提交直接发送 `formData`（`POST /stores` / `PUT /stores/{id}`，body 即 `Store` 实体，MyBatis-Plus 按 camelCase→snake_case 自动映射），后端只需实体加字段 + 建列即可完成落库与回显。

## 3. 改造设计

### 3.1 数据库（migration 37）

```sql
ALTER TABLE store_info
    ADD COLUMN store_type TINYINT NOT NULL DEFAULT 1 COMMENT '医院类型 1=社区卫生服务中心 2=卫生院 3=妇幼保健院 4=医院 5=其它' AFTER store_name;
```

- `NOT NULL DEFAULT 1`：存量医院自动回填「社区卫生服务中心」，与"默认选项"口径一致；其它写入路径（若有）未显式赋值时也走默认值。

### 3.2 后端（careld-store-service）

- `Store.java` 增加 `private Integer storeType;`（无需改 Controller/Service/Mapper，沿用全量实体映射）。

### 3.3 前端（admin-web）

- `src/types/index.ts`：`Store` 接口加 `storeType?: number // 1=社区卫生服务中心 2=卫生院 3=妇幼保健院 4=医院 5=其它`。
- `src/views/store/list.vue`：
  - 模板：在「所属代理商」与「医院名称」之间插入 `el-form-item label="医院类型" prop="storeType"`（el-select，5 个 el-option，值 1–5，不设 clearable）。
  - 校验：`formRules.storeType = [{ required: true, message: '请选择医院类型', trigger: 'change' }]`。
  - 默认值：`formData` 初始化与 `handleAdd`（新增重置）均置 `storeType: 1`（社区卫生服务中心）；编辑时由 `Object.assign(formData, row)` 回显行数据。
- 范围说明：不改列表列、不改「数据统计」弹窗（本次未要求）；仅 admin-web，不动医院端/医师端/家长端。

## 4. 验证计划

1. `vue-tsc` 类型检查通过（admin-web）。
2. 沙箱（39.162.49.28:5172）真机：
   - 新增医院弹窗：「医院名称」上方出现「医院类型」，默认显示「社区卫生服务中心」；
   - 编辑医院弹窗：回显库中值；
   - 把某医院改为「其它」保存 → 重新打开弹窗回显「其它」→ DB `store_info.store_type` 对账 → 改回原值恢复现场。
3. 需求边界：必填项因有默认值不可清空（无 clearable）；如需允许留空另议。

## 5. 验证结果

真机验证完成（2026-10-05，沙箱 39.162.49.28:5172，登录账号 careld）：

1. `vue-tsc`（admin-web）类型检查通过；`careld-store-service` 重新打包并重启（新 jar 生效）。
2. migration 37 执行成功：`store_info.store_type TINYINT NOT NULL DEFAULT 1` 位于 `store_name` 之后，存量 11 行全部回填为 1。
3. 新增医院弹窗（真机 DOM）：
   - 行序断言 `所属代理商 → 医院类型 → 医院名称`（取自 `.el-form-item` 标签序列）；
   - 「医院类型」带必填星号（`is-required`）；下拉选项恰为 5 个：社区卫生服务中心 / 卫生院 / 妇幼保健院 / 医院 / 其它；
   - 默认显示「社区卫生服务中心」；取消关闭弹窗后列表仍 6 条（未产生新数据）。
4. 编辑回显与保存回环（门店 STORE005 杭州西湖社区卫生服务中心，id=5）：
   - 打开编辑弹窗，医院类型回显「社区卫生服务中心」；
   - 改为「其它」→ 保存 toast「更新成功」→ DB 对账 `store_type=5` → 重开弹窗回显「其它」；
   - 改回「社区卫生服务中心」→ 保存 → DB 对账 `store_type=1`，全表恢复 11 行均为 1（现场已恢复）。
5. 边界：字段带默认值且无 clearable，实际无法置空；必填规则仅作兜底（如未来引入其它写入路径）。

遗留：未在沙箱新建完整医院做端到端新增断言（避免产生需连带清理的店长账号数据）；新增路径与编辑共用同一表单与提交逻辑，默认值已验证。如需，可在用户确认后补一次新增实测。
