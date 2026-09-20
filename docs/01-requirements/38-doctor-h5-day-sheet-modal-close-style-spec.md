# doctor-h5 A/B 弹窗红色关闭按钮与严格模态 Spec（第 91 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 交互/样式修复 Spec（前端，无后端改动） |
| 来源 | 用户需求第 91 项：doctor-h5 首页 A/B 弹窗右上角关闭按钮换成显眼的红色；弹窗打开期间只能操作弹窗，不能操作其它页面 |
| 涉及端 | doctor-h5（医生端） |
| 涉及页面 | `#/pages/home/index` 日历 A/B 明细弹层（`DayReserveSheet`） |
| 状态 | 已开发（2026-09-19，真机 DOM/computed 断言 + 开关循环回归通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 91-1 | 关闭按钮红色 | 弹窗右上角 ✕ 改为显眼的红色 |
| 91-2 | 严格模态 | 弹窗打开期间只能操作弹窗本身；不点 ✕ 无法误触其它页面（含底部导航栏） |

---

## 2. 现状诊断

- `DayReserveSheet.vue` 根容器 `.day-root { position: relative; z-index: 90 }`，远低于 H5 底部 tabbar `.uni-tabbar` 的 `z-index: 998`：遮罩虽为全屏 `position: fixed`，但其所在层叠上下文整体被 tabbar 压在下面，**tabbar 始终可点**（第 89 项同源问题的另一半）。
- 遮罩绑定了 `@click="close"`：点击弹窗外任意处（含 tabbar 区域之外的空白）会直接关闭弹窗，属于"点遮罩即退出"的松模态行为，不符合"只能操作弹窗"。
- ✕ 按钮沿用全局 `.sheet-close` 样式（灰色 `color: #9ca3af`），辨识度低。

---

## 3. 改造设计

`frontend/doctor-h5/src/components/DayReserveSheet.vue`：

1. **红色关闭按钮**（scoped 样式，变量来自 `src/uni.scss`）：
   ```scss
   /* 弹层唯一出口：显眼的红色关闭按钮 */
   .day-root .sheet-close {
     color: $app-danger;   /* #ef4444 */
     font-weight: 700;
   }
   ```
2. **严格模态**：
   - 关闭遮罩点击关闭：`<view class="mask" @click="close" />` → `<view class="mask" />`，✕ 成为唯一出口（对应用户「在不点击右上角的关闭按钮的情况下」的表述）。
   - 抬升根容器层级：`.day-root { z-index: 90 → 1000 }`，高于 tabbar（998）。此时全屏遮罩覆盖整页含导航栏，点击落在遮罩上（无处理器）而不透传到下层任何控件。
   - 不动全局 `.mask`/`.sheet` 样式，仅本组件生效。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 项 | 手段 | 结果 |
|---|---|---|
| 按钮颜色 | 打开 09-15 A/B 弹层，读取 `.sheet-close` computed `color`/`font-weight` | ✅ `rgb(239, 68, 68)`、`700` |
| 层级覆盖 | 读取 `.day-root` 与 `.uni-tabbar` computed `z-index` | ✅ 1000 > 998；遮罩 `position: fixed; pointer-events: auto`，覆盖导航栏 |
| 模态-遮罩 | 弹层内对 `.mask` 派发 click，400ms 后断言 | ✅ 弹层仍在（标题 `2026-09-15 预约详情`），不再被遮罩关闭 |
| 模态-✕ | 点击 `.sheet-close` 后断言 DOM | ✅ `.day-root` / `.sheet` 移除 |
| 开关循环 | 再次点击 A/B 打开 → ✕ 关闭 | ✅ 可重复开关，无卡死 |
| 内容回归 | 弹层卡片列表 | ✅ 2 张卡片（已完成 14:00 在上、已爽约 08:30 在下），与第 86 项一致 |
| 日历回归 | 读取 2026-09 日历单元格 | ✅ 15 = `1/8`，其余与修复前一致 |
| 类型检查 | doctor-h5 `vue-tsc --noEmit` | ✅ EXIT=0 |

> 过程说明：`touch` 沙箱文件强制 Vite 重新 transform 后，以 `curl` 确认 style 模块已含新规则（`#ef4444`、`z-index: 1000`）再断言。

---

## 5. 遗留与说明

1. 遮罩现在无点击行为，属有意为之（严格模态）；如后续希望恢复"点空白关闭"，只需给遮罩加回 `@click="close"`，层级改动可保留。
2. 同页其它弹层（`CancelSheet`、`VisionPicker` 等）仍为松模态+灰色 ✕，本次按用户反馈范围只调整 A/B 弹层。
3. Android 物理返回键/手势退出属 WebView 行为，不在 H5 可控范围。
4. 与 #75–#89 同属未提交改动，等待确认后一并入库。
