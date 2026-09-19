# 医院端 store-web 测试反馈第 12 批改造 Spec（第 71 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（全站头部/侧边栏文案与标题布局调整） |
| 来源 | 测试反馈第 71 项（docs/测试记录.txt 第 212-216 行） |
| 涉及端 | 门店医院Web端（store-web） |
| 涉及页面 | 全站布局（MainLayout）+ 9 个业务页面卡片标题 |
| 状态 | 待开发（设计已定稿，待确认后编码） |

---

## 1. 需求清单（用户原话拆解）

| # | 子项 | 主题 | 改动面 |
|---|---|---|---|
| 71-1 | 「数据看板」修改为「数据中心」 | 侧边栏菜单 + 路由标题 | store-web 前端（2 处文案） |
| 71-2 | 去掉所有页面最上方的标题；右侧医院名称字号放大 2 倍、加粗、靠左显示；右侧登录的医生身份信息大小位置不变 | 布局头部重构 + 9 个页面卡片标题删除 | store-web 前端（MainLayout + 9 个 view） |
| 71-3 | 页面中的「医院管理系统」修改为「Careld诊约助手服务」 | 侧边栏 logo + 登录页大标题 | store-web 前端（2 处文案） |
| 71-4 | 导航栏最下方加两行文字：`系统服务电话：` / `400 999 3608` | 侧边栏底部信息区 | store-web 前端（MainLayout） |

用户原话：

- 「71、医院管理pc端（http://39.162.49.28:5175/dashboard）
  （1）数据看板修改为数据中心
  （2）页面调整，所有页面最上一方的标题，比如儿童档案页面最上方的"儿童档案"、养护记录页面的"养护记录"等全部去掉，然后右侧医院名称信息比如"武汉武昌协和卫生服务中心"字号放大2倍加粗靠左显示，右侧登录的医生身份信息大小位置不变
  （3）页面中的"医院管理系统"修改为"Careld诊约助手服务"
  （4）在导航栏最下方二行的位置加入二行文字，第一行"系统服务电话："第二行"400 999 3608"」

---

## 2. 关键设计决策

1. **71-1 改 2 处**：侧边栏菜单文案 `layouts/MainLayout.vue:17` + 路由 `meta.title` `router/index.ts:22`。当前 `meta.title` 仅被 header-left 使用（本批移除），保留字段并同步改名，避免其它地方（如后续面包屑）出现旧文案。
2. **71-2 头部重构**：header 左侧的「当前页标题」（`{{ $route.meta?.title || '医院管理' }}`）整体删除；原位于 header 右侧的医院名（`.store-name`，14px、#666）移到 header 左侧，字号放大 2 倍 → **28px、font-weight 700、深色**；header 右侧仅保留登录医生身份下拉（`.user-info`，字号/位置不变）。
3. **71-2 页面标题删除范围 = 页面级标题**：每页 el-card 的 `#header` 中作为页面标题的 `<span>`。共 9 页 10 处（其中 `schedule` 页有 2 个卡片头，只删页面级「预约查询」）。**「预约日历 - {{月份}}」卡片标题保留**——它是日历功能区标题且随月份变化，不属"页面最上方标题"（见 §5 遗留 1）。
   - 删除标题后，卡片头若仅剩操作按钮/表单，保持原有右对齐位置：`.card-header` 的 `justify-content` 由 `space-between` 改为 `flex-end`；卡片头若已无任何内容（`care-record`、`basic-info`），整个 `<template #header>` 删除，避免残留空白头。
4. **71-3 改 2 处**：`MainLayout.vue:5` logo 文案；`views/login/index.vue:4`「医院管理系统登录」→「Careld诊约助手服务登录」（保留"登录"二字，与原文案结构一致）。全库无其它「医院管理系统」出现（已 grep 确认）。
5. **71-4 侧边栏底部两行**：`.sidebar` 改为纵向 flex（logo / 菜单 / 电话区），`el-menu` 设 `flex:1; overflow-y:auto`，电话区沉底；两行为两段文本、居中、浅色（深色底可读）：
   - 第一行 `系统服务电话：`（`rgba(255,255,255,0.55)`，13px）
   - 第二行 `400 999 3608`（`rgba(255,255,255,0.85)`，13px、600）
   - 下方留 16px 底距，顶部 1px 半透明白描边与菜单分隔（与 logo 底部描边呼应）。
6. **不改动的部分**：菜单结构与路由、`isManager` 系统设置显隐、退出登录逻辑、各页面内部内容与样式（除卡片标题）。

---

## 3. 实现要点

### 3.1 `layouts/MainLayout.vue`

模板（改动后）：

```html
<el-aside width="200px" class="sidebar">
  <div class="logo">
    <span>Careld诊约助手服务</span>
  </div>
  <el-menu …>
    <el-menu-item index="/dashboard">
      <el-icon><DataBoard /></el-icon>
      <span>数据中心</span>
    </el-menu-item>
    …
  </el-menu>
  <!-- 71-4 新增：服务电话两行 -->
  <div class="sidebar-footer">
    <div class="footer-line">系统服务电话：</div>
    <div class="footer-phone">400 999 3608</div>
  </div>
</el-aside>

<el-header class="header">
  <!-- 71-2：左侧改为医院名（原 header-left 标题删除） -->
  <div class="header-left">{{ userStore.userInfo?.storeName }}</div>
  <div class="header-right">
    <!-- 原 .store-name 已上移，此处仅保留登录身份 -->
    <el-dropdown @command="handleCommand">
      <span class="user-info">
        {{ userStore.userInfo?.realName }}
        <el-icon><ArrowDown /></el-icon>
      </span>
      …
    </el-dropdown>
  </div>
</el-header>
```

样式：

```scss
.sidebar {
  background-color: #001529;
  display: flex;
  flex-direction: column;

  .el-menu { border-right: none; flex: 1; overflow-y: auto; }

  /* 71-4 */
  .sidebar-footer {
    flex: none;
    text-align: center;
    padding: 12px 0 16px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);

    .footer-line { font-size: 13px; color: rgba(255, 255, 255, 0.55); line-height: 20px; }
    .footer-phone { font-size: 13px; font-weight: 600; color: rgba(255, 255, 255, 0.85); line-height: 20px; }
  }
}

.header {
  .header-left { font-size: 28px; font-weight: 700; color: #001529; }  /* 14px→28px 加粗 靠左 */
  .header-right { /* 不变：gap 20px */
    .user-info { /* 不变 */ }
  }
  /* .store-name 样式删除 */
}
```

### 3.2 9 个页面卡片标题删除清单

| 页面 | 位置 | 改动 |
|---|---|---|
| 儿童档案 | `views/child/index.vue:5-10` | 删 `<span>儿童档案管理</span>`，`.card-header` 改 `flex-end`（保留「新建档案」按钮） |
| 养护记录 | `views/care-record/index.vue:4-8` | 整个 `<template #header>` 删除（卡片头只有标题） |
| 预约记录 | `views/appointment-record/index.vue:5-10` | 删 `<span>预约记录</span>`，改 `flex-end`（保留「新建预约」） |
| 预约查询 | `views/schedule/index.vue:6-7` | 删 `<span>预约查询</span>`，改 `flex-end`（保留日期范围表单）；第 50-51 行「预约日历 - …」保留 |
| 排班设置 | `views/schedule-rule/index.vue:5-10` | 删 `<span>排班设置</span>`，改 `flex-end`（保留「新增排班规则」） |
| 基础信息 | `views/basic-info/index.vue:5-7` | 整个 `<template #header>` 删除（卡片头只有标题） |
| 医务人员 | `views/medical-staff/index.vue:5-10` | 删 `<span>医务人员管理</span>`，改 `flex-end`（保留「新增医务人员」） |
| TV设备管理 | `views/device/index.vue:5-10` | 删 `<span>TV设备管理</span>`，改 `flex-end`（保留「绑定设备」） |
| 视力养护记录 | `views/vision/index.vue:5-10` | 删 `<span>视力养护记录</span>`，改 `flex-end`（保留「导出记录」） |

> 工作台 `views/dashboard/index.vue` 无页面级标题（头部标题原由布局 header-left 提供），本批无需改动。

### 3.3 `router/index.ts`

```ts
meta: { title: '数据中心', icon: 'DataBoard' }   // 原 '数据看板'
```

---

## 4. 验证计划

- store-web `vue-tsc --build --force` 通过；
- 浏览器（store-web 5175）逐项：
  1. 侧边栏首项显示「数据中心」，进入后路由 meta.title 一致；侧边栏 logo 显示「Careld诊约助手服务」；登录页大标题为「Careld诊约助手服务登录」；
  2. 侧边栏最底部两行电话文字渲染且可见（不被菜单挤压/溢出，菜单项变多时可滚动）；
  3. header 左端显示医院名，computed 字号 28px / 字重 700 / 颜色为设计值；header 右端仅登录身份（realName + 下拉），computed 字号与改动前一致；
  4. 逐页确认 9 个页面卡片顶部无页面标题字样，页内操作按钮仍在卡片头右侧、功能可点（新建档案/新建预约/新增排班规则/新增医务人员/绑定设备/导出记录/预约查询表单）。
- 视觉走查：按用户截图验收标准，交付后由用户截图确认。

## 5. 遗留与注意事项

1. **「预约日历 - 2026年9月」卡片标题保留**（预约查询页第二张卡片）：它是日历功能区标题且承载月份导航语义，不属"页面最上方标题"。若客户要求一并去掉，再单独处理（去掉后上个月/今天/下个月按钮将独占卡片头）。
2. **医院名字号取值**：「放大 2 倍」按原 `.store-name` 14px × 2 = 28px 取；如客户期望以 header 标题 16px 为基数（32px），或觉得 28px 过大，按截图微调。
3. 医院端登录页 `<title>`（`index.html` 为 `Vite App`）未在本批要求内，未改动；如需统一品牌文案可后续补。
4. 本批为纯前端改动，无后端/数据库变更。

---

## 6. 验证结果（2026-09-18 本地实测）

- 类型检查：store-web `vue-tsc --noEmit` 通过（无错误输出）。
- 浏览器（localhost:5175，登录账号 careld3/胡院长 store 7，token 注入 localStorage 后整页加载）：
  1. header 左端 `header-left` 文本 = `武汉武昌协和卫生服务中心`，computed = **28px / 700 / text-align:start**；header 右端仅显示登录身份 `胡院长`（与改动前一致）；
  2. 侧边栏 logo = `Careld诊约助手服务`；菜单首项 = `数据中心`；`sidebar-footer` 两行 = `系统服务电话：` / `400 999 3608`；
  3. 逐页卡片头 innerText 核对（`.el-card__header`）：
     - `/child` → 仅 `新建档案`；`/care-record` → **无卡片头**；`/vision` → 仅 `导出记录`；
     - `/appointment-record` → 仅 `新建预约`；`/device` → 仅 `绑定设备`；`/medical-staff` → 仅 `新增医务人员`；
     - `/schedule-rule` → 仅 `新增排班规则`；`/basic-info` → **无卡片头**；
     - `/schedule` → `日期范围 - 查询` + `预约日历 - 2026年9月 …`（日历标题按设计保留）；
     - `/dashboard` → 卡片自带业务标题（预约/检测趋势、视力检测统计、最近预约），无页面级标题。
- 说明：隐藏视口下截图不可用（`NATIVE_BROWSER_VIEWPORT_UNAVAILABLE`），以上为 DOM/computed 结构化验证；视觉微调仍以用户截图为准（遗留 2 的字号取值待截图确认）。
