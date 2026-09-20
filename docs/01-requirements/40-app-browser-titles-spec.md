# 三端浏览器标签标题统一 Spec（第 93 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 品牌文案统一 Spec（前端，无后端改动） |
| 来源 | 用户需求第 93 项：三个前端各页面的浏览器标题统一修改 |
| 涉及端 | store-web（5175）、doctor-h5（5176）、parent-h5（5177） |
| 目标标题 | store-web → `Careld诊约助手医院客户端`；doctor-h5 → `Careld诊约助手医师手机端`；parent-h5 → `Careld诊约助手家长端` |
| 状态 | 已开发（2026-09-19，三端真机标题断言 + 页面内标题回归通过，vue-tsc ×3 EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 93-1 | 5175 标题 | 各页面浏览器标题由 `Vite App` 改为 `Careld诊约助手医院客户端` |
| 93-2 | 5176 标题 | 各页面浏览器标题由 `Careld 医生端` 改为 `Careld诊约助手医师手机端` |
| 93-3 | 5177 标题 | 各页面浏览器标题统一为 `Careld诊约助手家长端` |

---

## 2. 现状诊断

- **store-web**（纯 Vite SPA）：代码中无任何 `document.title` 逻辑，标题恒为 `index.html` 的 `Vite App`。
- **doctor-h5 / parent-h5**（uni-app H5）：实测 uni-app 会在页面切换时按该页 `navigationBarTitleText` 写 `document.title`（如切到预约记录页标题变 `预约管理`），无逐页标题的页面（首页）回退到 `globalStyle.navigationBarTitleText`。
- 关键约束：`navigationBarTitleText` **同时驱动页面内导航栏标题**（`.uni-page-head__title`）。若把所有页面标题都改成品牌名，会把「预约管理/儿童档案」等页面内标题一并改掉，超出「浏览器标题」的范围。

---

## 3. 改造设计

1. **静态标题**（首屏兜底）：
   - `frontend/store-web/index.html`：`Vite App` → `Careld诊约助手医院客户端`
   - `frontend/doctor-h5/index.html`：`Careld 医生端` → `Careld诊约助手医师手机端`
   - `frontend/parent-h5/index.html`：`Careld 家长端` → `Careld诊约助手家长端`
2. **uni-app 默认标题**：两个 H5 的 `pages.json` `globalStyle.navigationBarTitleText` 同步改为对应品牌名（仅作无逐页标题页面的兜底，页面内导航栏标题不受影响）。
3. **运行时锁定**（两个 H5 的 `src/main.ts` 模块顶层，早于应用挂载）：
   - 启动即把 `<title>` 元素写成品牌名（不依赖 index.html 内容）；
   - 用 `Object.defineProperty(document, 'title', …)` 拦截 uni-app 的逐页标题赋值：读返回品牌名，写时把 `<title>` 元素校正回品牌名。

```ts
const APP_TITLE = 'Careld诊约助手医师手机端'
const applyAppTitle = () => {
  const el = document.querySelector('title')
  if (el && el.textContent !== APP_TITLE) el.textContent = APP_TITLE
}
applyAppTitle()
Object.defineProperty(document, 'title', {
  get: () => APP_TITLE,
  set: applyAppTitle,
})
```

- 页面内导航栏标题（`navigationBarTitleText`）保持逐页配置不变。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 端 | 手段 | 结果 |
|---|---|---|
| store-web | 冷加载 `/schedule`、`/child` 读取 `document.title` | ✅ 两路由均为 `Careld诊约助手医院客户端`（dev server 已返回新 index.html） |
| doctor-h5 | 冷加载首页读取 `document.title` + `<title>` 元素 | ✅ 均为 `Careld诊约助手医师手机端` |
| doctor-h5 | 依次点击 5 个 tab（工作台/儿童档案/预约记录/养护记录/我的） | ✅ 5 页标签标题全部为新值；页面内标题保持 `儿童档案/预约管理/养护记录/我的`（工作台无导航栏） |
| doctor-h5 | 深链非 tab 子页 `#/pages/reserve/create` | ✅ 标签标题新值，页面内标题 `新建预约` |
| parent-h5 | 冷加载 `#/pages/child/index` | ✅ 标签标题 `Careld诊约助手家长端`，页面内标题 `儿童档案` |
| parent-h5 | 切换儿童档案/预约养护/我的三页 | ✅ 标签标题全部为新值，页面内标题保持原样 |
| 类型检查 | 三端 `vue-tsc --noEmit` | ✅ EXIT=0 ×3 |

---

## 5. 遗留与说明

1. **uni-app dev server 会缓存 index.html**：修改 `index.html` 后 `curl` 仍返回旧标题（进程需重启才刷新静态标题）。因 `main.ts` 启动即锁定标题，页面实际标签标题不受影响；生产构建（`build:h5`）直接读磁盘文件，无此问题。
2. 运行时锁定使浏览器标签标题不再随页面变化，属本次需求要求；若后续想恢复「页面名 + 品牌名」形式，需改为在 setter 中拼接而非忽略。
3. admin-web（5172）的 `Vite App` 标题不在本次需求范围，未改动。
4. 与 #75–#92 同属未提交改动，等待确认后一并入库。
