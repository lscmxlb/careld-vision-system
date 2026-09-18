# 医院端 store-web 测试反馈第 10 批改造 Spec（第 48-52 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（儿童档案列表列调整与姓名明文 + 预约记录时段单行 + 基础信息页合并共用保存） |
| 来源 | 测试反馈第 48-52 项（docs/测试记录.txt 第 96-100 行） |
| 涉及端 | 门店医院Web端（store-web）、后端 careld-child-service（仅 #52） |
| 涉及页面 | 档案管理-儿童档案、预约管理-预约记录、系统设置-基础信息 |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + API（真实登录态）+ 浏览器（DOM/Vue 状态）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 48 | 档案管理-儿童档案 | 列表新增「建档日期」为第一列，原「档案编号」移至最后一列（操作列前） | store-web 前端列调整 | ✅ 已完成 |
| 49 | 预约管理-预约记录 | 列表「时段」列一行显示 | store-web 前端格式化（HH:mm） | ✅ 已完成 |
| 50 | 系统设置-基础信息 | 两个区域合并为一个区域两组信息，共用一个「保存配置」按钮；有未保存改动时离开页面前提示 | store-web 前端（页面重写 + 路由离开守卫） | ✅ 已完成 |
| 51 | 档案管理-儿童档案 | 「建档时间」改为「建档日期」，格式为日期 | store-web 前端（并入 #48 同一列） | ✅ 已完成 |
| 52 | 档案管理-儿童档案 | 列表儿童姓名完整显示，不用 * 代替 | child-service 列表接口读时解密姓名 | ✅ 已完成 |

用户原话：

- 「48、儿童档案列表页，增加一列"建档时间"，在第一列显示，原来的档案编号移至最后一列」
- 「49、预约记录列表页优化，将时段列调整为一行显示」
- 「50、系统设置-基础信息页面，将二个区域合并成一个区域二组信息的格式，共用一个保存配置按钮，在这个页面有任何改动后，如果未保存要进入其它页面前要提示数据未保存」
- 「51、儿童档案列表页：建档时间修改为建档日期，格式调整为日期」
- 「52、儿童档案列表，儿童姓名完整显示，不要使用*代替」

> #48 与 #51 为同一列的两次口径收敛：#48 先按「建档时间」实现，随后按 #51 收敛为仅日期的「建档日期」。

---

## 2. 关键设计决策

1. **#48/#51 列口径**：第一列为「建档日期」，仅日期 `YYYY-MM-DD`（`createdAt.slice(0, 10)`），宽 120、居中；「档案编号」移至最后一个数据列（操作列前，仍 `show-overflow-tooltip`、宽 150）。不动接口——`GET /children` 已返回 `createdAt`（实测格式 `2026-09-09T14:55:13`）。
2. **#49 根因与做法**：接口返回 `timeSlotStart/timeSlotEnd` 为 `HH:mm:ss`（含秒），原样拼接为 17 字符（如 `14:00:00-15:00:00`），在 120px 列内折行；新增 `formatHm` 截断为 `HH:mm`（`17:00-18:00`，11 字符）实现单行，列宽不变。
3. **#50 合并方案**：单张卡片（header「基础信息」）内两个 `.section`——「科室管理」+「预约规则设置」（保留原提示 alert），底部唯一「保存配置」按钮；点击后依次保存科室（`PUT /departments/{id}`，仅有科室记录时）与预约规则（`PUT /appointment-config`）。**无科室记录的门店跳过科室表单校验**（DB 抽查：9 个门店中 5 个无科室行；store-web 无新建科室入口，避免科室必填校验阻塞配置保存）。
4. **#50 脏保护**：快照式比较（`JSON.stringify`，勾选数组排序归一避免勾选顺序误判）；基线在 `onMounted` 两个接口**加载完成后**捕获；`onBeforeRouteLeave` 守卫——脏时弹「有未保存的修改，确认放弃吗？」（放弃修改→放行；继续编辑→留在原页）；保存成功后重置基线。
5. **#52 实现位置（读时解密）**：`ChildServiceImpl.listProfiles` 逐行解密 `name_encrypted` 回填 `name`（解密失败回退掩码），**手机号保持脱敏**；不改写入逻辑、无数据迁移。写入路径（`updateProfile`）本就把表单明文重新加密入库；keyword 检索本就兼容掩码与明文（解密比对）。TV 检索端点 `/children/search` 仍返回掩码（不动）。

---

## 3. 各项实现要点

### 3.1 #48/#51 儿童档案列表列顺序（`views/child/index.vue`）

```html
<el-table-column label="建档日期" width="120" align="center">
  <template #default="{ row }">{{ (row.createdAt || '').slice(0, 10) || '-' }}</template>
</el-table-column>
<el-table-column prop="name" label="儿童姓名" width="100" align="center" />
...
<el-table-column prop="childCode" label="档案编号" width="150" show-overflow-tooltip />
<el-table-column label="操作" width="326" fixed="right" align="right">
```

最终表头顺序（浏览器实测）：建档日期 / 儿童姓名 / 性别 / 年龄 / 家长姓名 / 家长手机 / 养护次数 / 可用次数 / 审核状态 / 状态 / 档案编号 / 操作。操作列按钮未变（预约授权 / 预约记录 / 档案详情 / 禁用|启用，审核按钮条件显示）。

### 3.2 #49 时段单行显示（`views/appointment-record/index.vue`）

```html
<el-table-column label="时段" width="120">
  <template #default="{ row }">{{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}</template>
</el-table-column>
```

```ts
/** 时段显示统一为 HH:mm（后端返回 HH:mm:ss 时去掉秒） */
const formatHm = (t?: string) => (t ? t.slice(0, 5) : '')
```

### 3.3 #50 基础信息页重写（`views/basic-info/index.vue`）

模板结构：单卡片 → 两个 `.section`（组标题 `.section-title` 带主题色左竖线）→ `.footer-actions` 唯一「保存配置」按钮（`:loading="submitLoading"`）。科室表单 label-width 120 / max-width 560；配置表单 label-width 200 / max-width 640；两表单仍为独立 `el-form`（各自 ref 校验）。

脚本关键点：

```ts
const takeSnapshot = () =>
  JSON.stringify({
    deptCode: deptForm.deptCode, deptName: deptForm.deptName, deptType: deptForm.deptType,
    chargeStandard: deptForm.chargeStandard ?? null,
    parentCancelHours: configForm.parentCancelHours, /* ...其余配置项... */
    defaultShowStatuses: [...configForm.defaultShowStatuses].sort().join(',')
  })

const isDirty = computed(() => baseline.value !== '' && takeSnapshot() !== baseline.value)

const handleSubmit = async () => {
  // 无科室记录时不校验科室组（store-web 无新建科室入口），配置仍可保存
  const validations: Promise<unknown>[] = [configFormRef.value.validate()]
  if (deptForm.id) validations.push(deptFormRef.value.validate())
  try { await Promise.all(validations) } catch { return }
  submitLoading.value = true
  try {
    if (deptForm.id) await departmentApi.updateDepartment(deptForm.id, { storeId, ...科室字段, sortOrder: deptForm.sortOrder })
    await appointmentConfigApi.saveConfig({ ...configForm, defaultShowStatuses: configForm.defaultShowStatuses.join(',') })
    ElMessage.success('保存成功')
    baseline.value = takeSnapshot()
  } finally { submitLoading.value = false }
}

onBeforeRouteLeave(async () => {
  if (!isDirty.value) return true
  try {
    await ElMessageBox.confirm('有未保存的修改，确认放弃吗？', '提示', {
      confirmButtonText: '放弃修改', cancelButtonText: '继续编辑', type: 'warning'
    })
    return true
  } catch { return false }
})

onMounted(async () => {
  await Promise.all([loadDepartment(), loadConfig()])
  baseline.value = takeSnapshot()
})
```

### 3.4 #52 列表姓名明文（`careld-child-service` `ChildServiceImpl.listProfiles`）

```java
for (ChildProfile profile : list) {
    enrich(profile);
    // 列表姓名展示明文（解密失败回退掩码）；手机号仍脱敏
    String plainName = decryptForDetail(profile.getNameEncrypted(), aesKey);
    if (StringUtils.hasText(plainName)) {
        profile.setName(plainName);
    }
}
```

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --build`（多次，含 `--force`） | 通过（exit 0） |
| 构建重启 | child-service：kill 旧进程（2478484，端口 2s 释放）→ `mvn package -DskipTests -pl careld-child-service -am` → 启动 | BUILD SUCCESS（4.2s）；`Started ChildServiceApplication`（19:33:13，PID 2616055），端口 8284 |
| API | `GET /children?status=1&includeDisabled=true`（careld3 登录态，11 行） | 姓名明文：张三丰/张吱吱/刘畅/胡大/张无远/王小明/浏览器测试童/小明；手机号仍 `138****8888` 式脱敏 |
| 浏览器 | 儿童档案列表表头顺序与首列 | 建档日期 / 儿童姓名 / … / 档案编号 / 操作；首行「建档日期 2026-09-09、张三丰」；姓名列无 `*` |
| 浏览器 | 预约记录列表「时段」列 | 单元格高度 23px（单行），显示 `HH:mm-HH:mm`（如 17:00-18:00），列宽 120 不变 |
| 浏览器 | 基础信息页（#50） | 单卡片两组信息 + 唯一「保存配置」；一次点击同秒更新 `store_department(9)` 与 `appointment_config(2)`（19:24:58） |
| 浏览器 | 基础信息页脏保护（#50） | 改值 → `isDirty=true` → 点左侧菜单被拦截（「有未保存的修改，确认放弃吗？」）→ 继续编辑停留且值保留 → 放弃修改成功跳转 → 重进页面 `isDirty=false` |
| 浏览器 | 无科室门店守卫（#50） | 内存置 `deptForm.id=null`+清空科室编码 → 点保存无校验错误、「保存成功」、仅发出 `PUT /appointment-config` |
| 浏览器 | 表单校验失败路径（#50） | 清空科室编码 → 校验错误提示、无写请求 |
| 浏览器 | 控制台 | 无错误（仅既有 vue-router `next()` 弃用警告与 pinia 日志） |
| DB | 数据变更 | 无（#52 为读时计算；#50 保存验证为同值写入） |

**环境备注**：
1. 应用内浏览器 0×0 视口下 `evaluate_script` 需传 `waitForStableDom:false`；ElMessageBox 关闭动画在隐藏页冻结（DOM 残留、`display` 仍 inline-block、overlay 带 `fade-in-linear-leave-active`）——属环境假象，判定以 URL / Vue 状态（`isDirty`、`setupState`）为准。
2. 验证期间共享开发环境有其它客户端并发写入：19:25:32 `store_department(9)`（charge_standard=168）、19:29:55 `appointment_config(2)`（parent_cancel_hours=23、default_show_statuses='1,2,3'，updated_by=114）；非本轮操作产生，未做还原。

---

## 5. 遗留与注意事项

1. **#52 影响面**：`GET /children` 为 store-web / parent-web / admin-web 共用端点，三端列表姓名均变为明文（家长查看自己孩子、总部审核识别档案均属合理场景）；手机号仍脱敏。如需仅医院端明文，需在接口层区分客户端（本轮未做，按「完整显示」需求整体放开）。
2. **列表响应仍含密文列**（`nameEncrypted/phoneEncrypted`，改动前即如此，前端未使用）；如需收紧可在下发前置空（同类接口 `pick-options`/`by-phone` 已置空）。
3. **无科室门店**（9 个门店中 5 个）：store-web 无新建科室入口，合并保存时跳过科室组校验；后续若新增科室维护入口需同步调整该校验条件。
4. **#50 脏保护范围**：仅覆盖路由离开（含左侧菜单/顶部跳转）；浏览器刷新/关闭标签页未加 `beforeunload`（需求未要求）。
5. **#49 列宽未变**（120px）：如后续时段展示口径变化（跨天等）再评估。
6. **未提交 git**：本轮改动（含前几轮）仍保留在工作区。
