# 医院端 store-web 测试反馈第 5 批改造 Spec（第 36–37 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（store-web 前端为主 + store-service / user-service 删除保护） |
| 来源 | 测试反馈第 36–37 项（用户文字反馈） |
| 涉及端 | 门店医院Web端（store-web）、后端 careld-store-service、careld-user-service |
| 涉及页面 | /department（系统设置-科室管理）、/medical-staff（系统设置-医务人员） |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + API（真实登录态 curl）+ 浏览器（DOM 实测/布局测量/交互）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 36 | 系统设置-科室管理 | 列表去掉「排序」「状态」；默认保留一个科室长期存在不可删除、信息可编辑；去掉「新增科室」「删除」两个按钮 | store-web department + store-service 删除接口兜底 | ✅ 已完成 |
| 37 | 系统设置-医务人员 | 所有人员录入后，可修改或禁用，但不可删除 | store-web medical-staff + user-service 删除接口拦截 | ✅ 已完成 |

用户原话：「36、科室管理中，列表去掉排序、状态。默认保留一个科室长期存在不可删除，信息可以编辑。去掉该页面中的"新增科室"和"删除"这二个按钮」
　　　　　「37、医务人员，所有人员录入后，可修改或禁用，但不可删除」

---

## 2. 关键设计决策

1. **列表精简**：删除「排序」列（`sortOrder`）与「状态」列（店长可见的启用/禁用 `el-switch` + 其他角色只读 `el-tag`）。
2. **只保留编辑入口**：删除卡片头部的「新增科室」按钮与行内「删除」按钮，操作列仅剩「编辑」；编辑弹窗标题固定为「编辑科室」（不再区分新增/编辑）。
3. **「默认科室不可删除」双层保障**：
   - 前端：无删除入口（按钮已移除）；
   - 后端兜底：`DELETE /api/v1/departments/{id}` 删除"该门店最后一个未删除科室"时返回 400「默认科室长期保留，不可删除」，其余情况正常逻辑删除。
   - 兜底原因：预约授权自动计费取「本店第一条启用科室」的收费标准（见 07/08 批次无关的既有规则），门店若 0 科室会使自动计费失去依据；同时防止 admin-web 等其它入口误删。
4. **信息可编辑**：科室编码/名称/类型/收费标准均可修改；`sortOrder` 不在页面展示，编辑提交时随原值带回（不改变既有排序数据、也不新增 0 值覆盖）。
5. **操作列宽"恰好容纳"**：沿用既往实测惯例，列宽 74px = 实测「编辑」按钮 48.02px + 单元格左右 padding 24px + 约 2px 余量；未做右对齐（用户未要求）。
6. **死代码清理**：`handleAdd` / `handleDelete` / `handleStatusChange`、`ElMessageBox` 引入、`dialogTitle`、`form.status`、`createDepartment` / `updateDepartmentStatus` 调用一并移除（页面不再有对应入口）。
7. **#37 去删除按钮、保留修改/禁用**：医务人员操作列移除「删除」，保留「编辑」「禁用/启用」「重置密码」；新增入口保留（"录入"仍需要）。
8. **#37 后端硬拦截**：`DELETE /api/v1/medical-staff/{id}` 在校验权限/存在性后一律返回 400「医务人员不可删除，如需停用请使用「禁用」」——医务人员被预约单/养护记录等历史数据引用，删除会破坏历史可读性；admin-web 的删除按钮同样会被拦截（保留 404 语义以防跨院探测）。
9. **#37 操作列宽"恰好容纳"**：列宽 234px。实测三按钮 48.02+48.02+72.02 = 168.06，两处间距各 **20.02px**（EP 相邻按钮 12px 外边距 + 容器 flex `gap:8px` 叠加，此前的经验值 8px 不适用），内容 208.1 + padding 24 + 约 2px 余量。

---

## 3. 各项实现要点

### 3.1 前端（`frontend/store-web/src/views/department/index.vue`）

- 卡片头部仅保留「科室管理」标题（去掉包装 div 与「新增科室」按钮，删除无用 `.card-header` 样式）。
- 表格列变为 5 列：科室编码(120) / 科室名称 / 科室类型 / 收费标准(120) / 操作(74, `v-if="isManager"`，仅「编辑」)。
- 弹窗：`title="编辑科室"` 固定；表单项 4 行（编码/名称/类型/收费标准，无排序）。
- 脚本：`handleSubmit` 仅走 `updateDepartment`（`form.id` 必填校验），成功提示「修改成功」后刷新列表。

### 3.2 后端（`backend/careld-store-service/.../service/impl/DepartmentServiceImpl.java`）

```java
public void deleteDepartment(Long id) {
    Department department = departmentMapper.selectById(id);
    if (department == null) { return; }
    Long remain = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
            .eq(Department::getStoreId, department.getStoreId()));
    if (remain != null && remain <= 1) {
        throw new BusinessException(400, "默认科室长期保留，不可删除");
    }
    departmentMapper.deleteById(id);
}
```

- `selectCount` 自动带逻辑删除过滤（`deleted_at IS NULL`），即按"有效科室数"判断。
- 沿用同模块先例（`DeviceTypeServiceImpl` 删除前校验设备数）与全局异常处理器（返回真实 message，非"系统繁忙"）。

### 3.3 #37 前端（`frontend/store-web/src/views/medical-staff/index.vue`）

- 表格操作列去掉「删除」按钮（4 按钮 → 3 按钮：编辑 / 禁用-启用 / 重置密码），`width="320"` → `234`。
- 删除已无入口的 `handleDelete` 函数（`ElMessageBox` 仍被「重置密码」使用，保留导入）。
- 其余不变：搜索栏（关键词/角色/状态）、状态 tag 列、新增/编辑弹窗、禁用启用、重置密码、分页。

### 3.4 #37 后端（`backend/careld-user-service/.../controller/MedicalStaffController.java`）

```java
@Operation(summary = "删除医务人员（录入后不可删除，仅可禁用）")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id) {
    Long storeId = DataScopeHelper.resolveStoreId(null);
    MedicalStaff exist = medicalStaffMapper.selectById(id);
    // 总部用户（storeId=null）可管理任意医院的医务人员
    if (exist == null || (storeId != null && !storeId.equals(exist.getStoreId()))) {
        throw new BusinessException(404, "医务人员不存在");
    }
    throw new BusinessException(400, "医务人员不可删除，如需停用请使用「禁用」");
}
```

- 保留原权限/存在性校验（跨院访问仍返回 404，不泄露记录存在性），校验后一律拒绝删除；`update`（修改）与 `updateStatus`（禁用/启用）接口不变。

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --noEmit` | 通过（exit 0） |
| 构建重启 | store-service `mvn package -DskipTests -pl careld-store-service -am`；先 kill 旧进程→等 8283 释放→启动 | BUILD SUCCESS；日志 `Started StoreServiceApplication`，8283 监听 |
| API | `GET /api/v1/departments?storeId=7` | 仅 1 条有效科室（id=9 Careld2026095 视力校正门诊/养护/158 元） |
| API | 造临时科室（id=10）→ `DELETE /departments/10`（此时门店 2 个科室） | 200 success（放行正确，非最后科室可删） |
| API | `DELETE /departments/9`（门店最后一个有效科室） | 400「默认科室长期保留，不可删除」，数据未被删 |
| DB | `store_department` store 7 复核 | id=9 `deleted_at=NULL` 完好；临时行 id=10 已物理清理（测试数据还原） |
| 浏览器 | 列表表头 5 列 | 科室编码/科室名称/科室类型/收费标准/操作；无「排序」「状态」 |
| 浏览器 | 头部与行内按钮 | 卡片头部无「新增科室」；行内仅「编辑」，无「删除」 |
| 浏览器 | 点击「编辑」弹窗 | 标题「编辑科室」；4 表单项（无排序）；回显 Careld2026095/视力校正门诊/养护/158.00 |
| 浏览器 | 同值保存「确定」 | 提示「修改成功」、弹窗关闭、列表刷新（数据同值，updatedAt 更新，业务数据未变） |
| 浏览器 | 操作列宽实测 | 74px 下「编辑」按钮 48.02px、单行（高 24）、左留白 12 / 右留白 13.98 |
| 构建重启 | user-service `mvn package -DskipTests -pl careld-user-service -am`；先 kill 旧进程→等 8282 释放→启动 | BUILD SUCCESS；日志 `Started UserServiceApplication`，8282 监听 |
| API #37 | `DELETE /api/v1/medical-staff/2`（真实医务人员） | 400「医务人员不可删除，如需停用请使用「禁用」」 |
| API #37 | `DELETE /api/v1/medical-staff/99999`（不存在的 id） | 404「医务人员不存在」（权限/存在性校验保留） |
| API #37 | `PUT /1/status`（同值 1）与 `PUT /1`（同值编辑） | 均 200 success（修改/禁用路径正常） |
| DB #37 | `medical_staff` store 7 复核 | id=1/2 `deleted_at=NULL` 未被删除，数据无变化 |
| 浏览器 #37 | 医务人员列表行内按钮 | 每行 3 个：编辑 / 禁用 / 重置密码；无「删除」 |
| 浏览器 #37 | 操作列宽实测（234px，初版 210 换行） | 三按钮单行（top 相同），右留白 13.95；间距实测 20.02px/处 |
| 浏览器 #37 | 点击「编辑」 | 弹窗「编辑医务人员」回显张小丽/13788888888，点「取消」关闭（未改动数据） |

**环境备注**：应用内浏览器无可见表面（viewport 0×0），按钮/列宽为 `getBoundingClientRect` 分数宽度实测；弹窗关闭后残留 DOM 属 transition 不触发的环境假象（以 `setupState.dialogVisible` 复核为准）。

---

## 5. 遗留与注意事项

1. **admin-web 科室管理未同步**：admin-web 端页面仍保留新增/删除/状态切换（本轮测试反馈均针对 store-web，未改动 admin-web）；但其调用的同一后端删除接口已受兜底保护——门店最后一个有效科室无法删除。
2. **状态维度收窄**：store-web 不再提供启用/禁用与新增入口后，门店科室信息由店长通过「编辑」维护；如需停用某个科室，目前只能走 admin-web。
3. **计费口径不变**：预约授权自动计费仍取「本店第一条启用科室」的收费标准（取不到按 0 元）；若希望"科室全删/全禁用"也有保护，需另行提出。
4. **视觉走查**：0×0 视口环境无法截图，列宽/按钮排布为数值级验证（1px 级余量），像素级观感待用户打开真实浏览器走查确认。
5. **数据说明**：store 7 现有唯一有效科室为 id=9「视力校正门诊」（养护，158 元）；同店 id=8 于 2026-09-14 18:07 被软删除（非本次改造操作）；门店 1/2/3 的科室（各 3 条）未做数据调整。
6. **admin-web 医务人员页未同步**：admin-web 仍保留红色「删除」按钮（权限 `store:staff:delete`），后端已拦截，点击会提示「医务人员不可删除，如需停用请使用「禁用」」；如需同步移除该按钮（及可选回收权限）请另行提出。
7. **医务人员数据说明**：store 7 有效医务人员 2 人（id=1 胡春花-医生助理、id=2 张小丽-医生）；id=3 张明丽于 2026-09-14 18:13 被软删除（早于本次改造、非本次操作），如需恢复可执行 `UPDATE medical_staff SET deleted_at=NULL WHERE id=3`。
8. **禁用与删除的行为差异**：禁用后该人员仍保留在列表与历史记录中，但不会出现在新预约/授权的选人列表（选人请求带 `status=1` 过滤）；这正是"不可删除、可禁用"要达到的效果——历史可追溯、新人不可选。
