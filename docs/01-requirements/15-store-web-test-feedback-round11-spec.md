# 医院端 store-web 测试反馈第 11 批改造 Spec（第 53 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（儿童档案新建/编辑弹窗布局优化：同行合并、隐藏家族病史、整体上移压缩间距） |
| 来源 | 测试反馈第 53 项（docs/测试记录.txt 第 101-104 行） |
| 涉及端 | 门店医院Web端（store-web） |
| 涉及页面 | 档案管理-儿童档案（新建/编辑档案弹窗与「档案详情」编辑入口共用同一弹窗） |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + 浏览器（DOM/Vue 状态）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 53-1 | 其它信息区 | 「分娩方式」与「日常作息」同一行左右显示 | store-web 前端（模板 el-row） | ✅ 已完成 |
| 53-2 | 其它信息区 | 「既往病史」与「过敏信息」同一行左右显示 | store-web 前端（模板 el-row） | ✅ 已完成 |
| 53-3 | 其它信息区 | 隐藏「家族病史」项目 | store-web 前端（仅隐藏 UI，字段与数据流保留） | ✅ 已完成 |
| 53-4 | 弹窗整体 | 框体整体上移 + 优化间距，尽量一屏完整显示、避免上下滑动 | store-web 前端（`top` + 非 scoped 样式压缩） | ✅ 已完成 |

用户原话：

- 「53、儿童档案-新建/编辑档案页面优化：
  （1）其它信息区，分娩方式与日常作息这二项放在同一行左右显示
  （2）既往病史和过敏信息这二项放在同一行左右显示
  （3）隐藏家族病史项目
  （4）页面框体整体上移，并优化间距，让页面在一个页面显示完整，避免上下滑动，方便用户操作」

---

## 2. 关键设计决策

1. **同行合并沿用「基本信息」区既有写法**：`<el-row :gutter="16">` + 两个 `<el-col :span="12">`，与基本信息区 4 行两列保持一致的栅格节奏；「日常作息」与「既往病史/过敏信息」均保留原 `label-width="100px"`（左对齐一致）。
2. **日常作息改自适应宽度**：原两个 el-time-picker 固定 `width:140px`（当时占整行宽度，够用）；改为半列（约 256px 内容宽）后固定宽度会溢出，故去掉内联宽度，改 `.schedule-row :deep(.el-date-editor) { flex: 1; min-width: 0 }` 均分剩余宽度（实测各 111px，`HH:mm` 值与「休息时间/起床时间」placeholder 均可完整显示），分隔符「至」`margin: 0 6px` 不参与拉伸。
3. **家族病史仅隐藏 UI，数据链路完整保留**：删除表单项标记，但保留 `formData.familyHistory` 字段、`handleAdd` 重置、`handleEdit` 详情回填、`handleSubmit` 提交（`familyHistory: formData.familyHistory || undefined`）——保证编辑旧档案再保存时不会把已存值清空（实测编辑 `child_profile(27)`：DB `family_history='无'`，组件 `formData.familyHistory='无'` 而页面无该表单项）。接口/DB 字段不动，后续如需恢复展示直接加回表单项即可。
4. **整体上移**：el-dialog 加 `top="3vh"`（EP 2.14 渲染为 CSS 变量 `--el-dialog-margin-top: 3vh`，`.el-dialog` 的 `margin: var(--el-dialog-margin-top, 15vh) auto 50px` 生效）——默认 15vh 顶距压缩到 3vh，弹窗整体上移约 12vh。
5. **间距压缩 + 内部滚动兜底**：给弹窗挂 `class="child-dialog"`，配**非 scoped** 样式块（teleport 到 body 的弹窗内容需非 scoped 才能命中，参照预约记录页 `care-dialog` 既有做法）：body 上下内边距 16px→6px/10px、分隔线外边距 24px→12px/14px（首个 divider 上边距归零）、表单项下边距 18px→12px；同时 `max-height: calc(97vh - 130px); overflow-y: auto` 兜底——超矮窗口时弹窗内滚动，不超出视口。
6. **未改动的部分**：视力状况 8 项 4 列网格、裸眼视力三下拉、分区标题（基本信息/视力情况/其它信息）、校验规则（含姓名/家长姓名/手机号/主治医师必填）与防误关配置（`close-on-click-modal/press-escape=false` + `before-close` 脏数据二次确认）均保持原样。

---

## 3. 实现要点（`views/child/index.vue`）

### 3.1 弹窗容器（53-4）

```html
<el-dialog
  v-model="dialogVisible"
  :title="dialogTitle"
  width="760px"
  top="3vh"
  class="child-dialog"
  :close-on-click-modal="false"
  :close-on-press-escape="false"
  :before-close="handleDialogClose"
>
```

```scss
/* 非 scoped：弹窗 teleport 到 body；整体上移并压缩间距，尽量一屏显示；内容超高时弹窗内滚动 */
.child-dialog {
  .el-dialog__body {
    padding-top: 6px;
    padding-bottom: 10px;
    max-height: calc(97vh - 130px);
    overflow-y: auto;
  }

  .el-divider--horizontal {
    margin: 12px 0 14px;

    &:first-child {
      margin-top: 0;
    }
  }

  .el-form-item {
    margin-bottom: 12px;
  }
}
```

### 3.2 其它信息区同行合并（53-1/53-2/53-3）

```html
<el-divider content-position="left">其它信息</el-divider>
<el-row :gutter="16">
  <el-col :span="12">
    <el-form-item label="分娩方式" prop="deliveryType"> …el-select… </el-form-item>
  </el-col>
  <el-col :span="12">
    <el-form-item label="日常作息" prop="bedtime">
      <div class="schedule-row">
        <el-time-picker v-model="formData.bedtime" format="HH:mm" value-format="HH:mm" placeholder="休息时间" />
        <span class="schedule-sep">至</span>
        <el-time-picker v-model="formData.wakeTime" format="HH:mm" value-format="HH:mm" placeholder="起床时间" />
      </div>
    </el-form-item>
  </el-col>
  <el-col :span="12">
    <el-form-item label="既往病史">
      <el-input v-model="formData.medicalHistory" type="textarea" :rows="2" />
    </el-form-item>
  </el-col>
  <el-col :span="12">
    <el-form-item label="过敏信息">
      <el-input v-model="formData.allergyInfo" type="textarea" :rows="2" />
    </el-form-item>
  </el-col>
</el-row>
<el-form-item label="主治医师" prop="doctorId"> …el-select… </el-form-item>
```

- 「家族病史」表单项已删除；`formData.familyHistory` 及 handleAdd/handleEdit/handleSubmit 数据流保留（见设计决策 3）。
- 日常作息时间选择器去掉内联固定宽度，改为：

```scss
.schedule-row {
  display: flex;
  align-items: center;
  width: 100%;

  /* 半列宽下两个时间选择器均分剩余宽度 */
  :deep(.el-date-editor) {
    flex: 1;
    min-width: 0;
  }

  .schedule-sep {
    margin: 0 6px;
    flex: none;
  }
}
```

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --build --force` | 通过（exit 0） |
| 浏览器·结构（新建） | 点「新建档案」打开弹窗后读 DOM | dialog class `el-dialog child-dialog`；表单项标签 15 个，含 分娩方式/日常作息/既往病史/过敏信息，**无「家族病史」**；el-row 栅格 `[8, 4]`（基本信息 8 个 el-col、其它信息 4 个 el-col） |
| 浏览器·样式（新建） | 读 computed style | body `padding-top:6px` / `padding-bottom:10px`、`max-height: calc(97vh - 130px)`；divider 外边距首个 `0/14px`、其余 `12px/14px`；`.el-form-item` `margin-bottom:12px`；时间选择器 `flex-grow:1; min-width:0`、无内联宽度，实测各 111px；占位文本「休息时间」实测 56px < 输入框可用宽 67px，未截断 |
| 浏览器·上移 | el-dialog inline style | `--el-dialog-margin-top: 3vh; --el-dialog-width: 760px;`；EP 主题 `.el-dialog` 规则为 `margin: var(--el-dialog-margin-top,15vh) auto 50px`，确认 3vh 生效（默认 15vh） |
| 浏览器·一屏估算 | 弹窗内容实测高度 | body 内容 `scrollHeight=525px`；全弹窗估算 header 40 + body(16+525) + footer 48 ≈ **629px**（760px 固定宽下与视口宽无关）→ 1080p 浏览器（≈950px 可视高）与 1366×768 窗口（≈660px 可视高）均可一屏完整显示 |
| 浏览器·编辑路径 | 点「档案详情」打开编辑弹窗（记录 27 张无远） | 标题「编辑档案」；结构同上（无家族病史；栅格 `[8,4]`）；`setupState.formData.familyHistory="无"`（与 DB `child_profile.family_history='无'` 一致）——UI 隐藏但值保留，保存不会丢 |
| 浏览器·控制台 | error/warn 汇总 | 无 error；仅既有 vue-router `next()` 弃用与 el-radio label 弃用警告（全局既有，非本次引入） |

**环境备注**：应用内浏览器为 0×0 隐藏视口，`evaluate_script` 需 `waitForStableDom:false`；该环境下 `vh` 解析为 0（`top:3vh`、`max-height: calc(97vh-130px)` 的 computed 均为 0px，body 高度塌陷为 padding 16px），属环境假象——判定以 inline CSS 变量、px 级 margin/padding computed 值与 `scrollHeight` 为准；真实视口的视觉走查（截图）待用户确认。

---

## 5. 遗留与注意事项

1. **视觉走查待确认**：本次弹窗布局改动（上移幅度、两个 textarea 半列观感、时间选择器 111px 宽度下的 placeholder 展示）建议由用户在真实浏览器确认，如有偏差按截图微调间距/宽度。
2. **家族病史恢复入口**：字段与提交链路完整保留（DB/接口不动），后续若需恢复展示，只需把表单项标记加回「其它信息」区（与既往病史同排即可）。
3. **半列宽度敏感性**：`.schedule-row` 依赖父容器半列宽度；若后续调整弹窗宽度或 label-width，时间选择器会自适应但因 `flex:1` 随容器变化。
4. **未提交 git**：本轮改动（含前几轮）仍保留在工作区。
