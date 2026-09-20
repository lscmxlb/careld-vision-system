# store-web 预约日历标题居中放大加粗 Spec（第 92 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 样式调整 Spec（前端，无后端改动） |
| 来源 | 用户需求第 92 项：`/schedule` 页面「预约日历 - 2026年9月」文字移到该行正中区域、字号放大、字体加粗 |
| 涉及端 | store-web（门店 PC 端） |
| 涉及页面 | `/schedule` 预约日历卡片的 header 行 |
| 状态 | 已开发（2026-09-19，真机 DOM/computed 断言通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 92-1 | 标题居中 | 「预约日历 - 2026年9月」移到该行的正中区域 |
| 92-2 | 字号放大 | 标题字号比原来更大 |
| 92-3 | 字体加粗 | 标题加粗显示 |

---

## 2. 现状诊断

- 日历卡片 header 与上方统计卡片共用 `.schedule-page .card-header`：`display: flex; justify-content: flex-end`。
- 因此「预约日历 - 2026年9月」与右侧三个按钮（上个月/今天/下个月）一起靠右排列，标题并非居中。

---

## 3. 改造设计

`frontend/store-web/src/views/schedule/index.vue`：

1. 模板：header 行加 `calendar-header` 类，标题 span 加 `calendar-title` 类（不影响统计卡片的 header）。
2. 样式（绝对定位居中，不受右侧按钮组宽度影响）：

```scss
.calendar-card {
  .calendar-header {
    position: relative;
    /* 标题绝对定位居中，不受右侧操作按钮宽度影响 */
    .calendar-title {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      font-size: 20px;
      font-weight: 700;
      white-space: nowrap;
    }
  }
}
```

- 行本身仍为 `flex + justify-content: flex-end`，三个操作按钮位置不变。
- 标题脱离文档流后不影响行高，长标题不折行（`white-space: nowrap`）。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 项 | 手段 | 结果 |
|---|---|---|
| 文案 | 读取 `.calendar-title` textContent | ✅ `预约日历 - 2026年9月` |
| 放大/加粗 | 读取 computed `font-size` / `font-weight` | ✅ `20px` / `700` |
| 居中 | 标题与行 `getBoundingClientRect` 中心对比 | ✅ 标题中心 241 == 行中心 241；`transform: matrix(1,0,0,1,-100.773,-14)`（即 -50% 位移） |
| 按钮位置 | 读取 `.header-actions` 三个按钮 | ✅ 上个月/今天/下个月仍在行右侧，行 `justify-content: flex-end` 不变 |
| 邻卡回归 | 统计卡片 header 检查 | ✅ 页面 2 张卡片，统计卡片 header 未含 `calendar-title`，样式未受影响 |
| 类型检查 | store-web `vue-tsc --noEmit` | ✅ EXIT=0 |

---

## 5. 遗留与说明

1. 本沙箱浏览器视口为 0×0（无头环境），几何断言采用中心点对比 + computed 位移验证；规则为确定性 CSS 居中，实际视口下行为一致。
2. 与 #75–#91 同属未提交改动，等待确认后一并入库。
