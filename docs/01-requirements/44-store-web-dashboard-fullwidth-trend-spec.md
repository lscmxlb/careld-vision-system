# store-web 数据中心去掉「视力检测统计」并让趋势图占满整行 Spec（第 97 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 前端版面调整 Spec（删除卡片 + 图表整行铺开） |
| 来源 | 用户需求：http://39.162.49.28:5175/dashboard 去掉红框「视力检测统计」卡片，左侧「预约/检测趋势」向右延长至页面最右侧（编号顺延自拟为第 97 项） |
| 涉及端 | store-web（5175）`/dashboard` 数据中心 |
| 状态 | 已开发（2026-09-19，真机 DOM 断言通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 97-1 | 删除卡片 | 去掉「视力检测统计」柱状图卡片 |
| 97-2 | 趋势图铺开 | 「预约/检测趋势」卡片宽度由 14/24 栏延长至整行（24/24），右边缘与页面内容区一致 |

---

## 2. 现状诊断

- `frontend/store-web/src/views/dashboard/index.vue` 图表区为一行两列：`el-col :span="14"`（预约/检测趋势，折线图）+ `el-col :span="10"`（视力检测统计，柱状图）。
- 视力图数据来自 `statisticsApi.getVisionStatistics`（养护前/养护后柱状），仅本页使用；页面脚本含 `visionChartRef / fetchVisionStatistics / renderVisionChart` 与 resize、dispose 引用。

---

## 3. 改造设计

1. 模板：删除 `el-col :span="10"` 的「视力检测统计」卡片；「预约/检测趋势」改为 `el-col :span="24"`，其余（标题、近7天/近30天切换、图容器）不变。
2. 脚本：同步删除随之失效的视力图代码——`visionChartRef`、`fetchVisionStatistics`、`renderVisionChart`，以及 `handleResize` / `onBeforeUnmount` / `onMounted` 中的对应引用；`statisticsApi.getVisionStatistics` 等接口层封装与后端接口保持不动。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 手段 | 结果 |
|---|---|
| `vue-tsc --noEmit`（store-web） | ✅ EXIT=0，无未使用变量报错 |
| 真机 DOM 断言（`http://39.162.49.28:5175/dashboard`，store001_mgr 登录） | ✅ 图表行仅剩 1 个 `el-col`，类名为 `el-col-24`；`.chart-container` ×1、`canvas` ×1（仅趋势图）；卡片标题仅 `预约/检测趋势`、`最近预约`；页面文本不含「视力检测统计」「养护前/养护后」 |
| 页面整体回归 | ✅ 顶部 4 张统计卡（今日预约/待审核档案/在线设备/今日检测）与「最近预约」表格正常渲染，趋势图 canvas 正常初始化 |
| 控制台 | ✅ 无新增错误（`[ECharts] Can't get DOM width or height` 为应用内浏览器 0×0 隐藏视口的环境假象，历史 spec 14/27 已记录；`el-radio label` 弃用告警为登录页既有告警） |

---

## 5. 遗留与说明

1. 应用内浏览器为 0×0 隐藏视口，无法截图走查实际视觉宽度；本次以 DOM 结构断言（单列 `el-col-24`）证明铺满整行，最终视觉效果以用户截图确认为准。
2. 后端 `/api/v1/statistics/vision` 接口与 `statisticsApi.getVisionStatistics` 封装未删除（仅页面不再调用），后续如需恢复该图表可直接复用。
3. 与 #63–#96 同属未提交改动，等待确认后一并入库。
