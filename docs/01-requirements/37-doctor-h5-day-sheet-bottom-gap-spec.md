# doctor-h5 A/B 弹窗与导航栏间距修复 Spec（第 89 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 缺陷修复 Spec（前端，无后端改动） |
| 来源 | 用户需求第 89 项：doctor-h5 首页点击 A/B 数字弹出的弹窗下方要与导航栏保持合理间距，不要连在一起 |
| 涉及端 | doctor-h5（医生端） |
| 涉及页面 | `#/pages/home/index` 日历 A/B 明细弹层（第 86 项新增的 `DayReserveSheet`） |
| 状态 | 已开发（2026-09-19，真机 CSS/几何断言 + 功能回归通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 89-1 | 底部间距 | A/B 弹窗下沿与底部导航栏（tabbar）之间留出可见间距，不再连成一片 |

---

## 2. 现状诊断

- `DayReserveSheet.vue` 复用全局弹层样式（`App.vue`）：`.mask { z-index: 1000 }`、`.sheet { position: fixed; left/right: 0; bottom: 0; max-height: 86vh }`，组件根 `.day-root { position: relative; z-index: 90 }`。
- H5 底部 tabbar `.uni-tabbar`：`position: fixed; bottom: 0; height: 54px; z-index: 998`（高度取自 `pages.json` 的 `tabBar.height`，实测 54px）。
- 由于 tabbar 的 z-index（998）高于弹层容器（90），tabbar 叠在弹层下沿之上：白色弹层从下沿被 tabbar 接住，视觉上"弹窗与导航栏连在一起"（修复前实测 `.sheet` computed `bottom: 0px`）。

---

## 3. 改造设计

`frontend/doctor-h5/src/components/DayReserveSheet.vue` scoped 样式中以更高优先级选择器覆盖（仅作用于本组件，不动全局 `.sheet`）：

```scss
/* 浮在底部 tabbar 上方并留出间距，避免弹层与导航栏连成一片（tabbar 高 54px，见 pages.json） */
.day-root .sheet {
  bottom: calc(54px + env(safe-area-inset-bottom) + 16px);
  border-radius: 28rpx;
}
```

- 上移量 = tabbar 高（54px）+ 安全区 + 16px 间距；浮起后四角圆角（全局原为仅上圆角）。
- `.day-list` 底部内边距由 `calc(env(safe-area-inset-bottom) + 24rpx)` 简化为 `24rpx`（不再贴屏幕底边，安全区补偿由弹层位移承担）。
- 不改全局样式的原因：`.sheet` 在非 tabbar 页面（如儿童档案详情等）也会使用，全局加 `bottom` 偏移会在这些页面留下悬空缝隙。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 项 | 手段 | 结果 |
|---|---|---|
| 间距 | 打开 09-15 A/B 弹层，读取 `.sheet` computed `bottom` 与 `.sheet` / `.uni-tabbar` 的 `getBoundingClientRect` | ✅ `bottom: 70px`（54 + env 0 + 16）；sheet 下沿与 tabbar 上沿间距 = **16px** |
| 圆角 | 读取 computed `border-radius` | ✅ 规则 `0.875rem`（环境视口宽为 0 时 rem 折算为 0，规则已生效） |
| 功能回归 | 弹层内容与第 86 项一致 | ✅ 标题 `2026-09-15 预约详情`，2 张卡片（已完成 14:00 在上、已爽约 08:30 在下） |
| 关闭回归 | 点击 ✕ 后断言 DOM | ✅ `.sheet`、`.mask` 均移除 |
| 日历回归 | 读取 2026-09 全部日历单元格 | ✅ A/B 值与修复前一致（15 = `1/8`，1/2/10 = `-/-`，其余 `0/6`） |
| 类型检查 | doctor-h5 `vue-tsc --noEmit` | ✅ EXIT=0 |

> 过程说明：同步到沙箱后 Vite 一度仍返回旧 CSS（文件监听未触发），在沙箱 `touch` 该文件后重新 transform 生效；后续同类问题可先 touch 再验证。

---

## 5. 遗留与说明

1. 其它弹层（预约记录页「取消预约」`CancelSheet`、`VisionPicker` 等）仍使用全局 `.sheet` 的 `bottom: 0`，在 tabbar 页面同样会与导航栏相接；本次按用户反馈范围只调整 A/B 弹层，如需统一可对 tabbar 页的弹层套用同样的位移。
2. `env(safe-area-inset-bottom)` 在本沙箱桌面浏览器为 0；真机 iPhone 上弹层会随安全区再上移，间距只增不减。
3. 与 #75–#88 同属未提交改动，等待确认后一并入库。
