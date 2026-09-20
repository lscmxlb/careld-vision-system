# 家长端取消弹窗居中/严格模态 + 取消原因前缀 + 医师端统计球描边 Spec（第 105 项，用户编号）

| 项目 | 内容 |
|---|---|
| 文档类型 | 两端前端 Spec（弹层交互改造 + 文案前缀 + 描边配色，无接口/表结构改动；家长端取消请求新增一个已有字段） |
| 来源 | 用户需求（用户给定编号 **#105**）：「（1）家长端我的预约页面，已经预约的卡片，点击取消预约弹出的窗体，要在当前页面的上下左右居中的位置显示，同时不允许点击这个窗体以外的内容。已经取消的卡片上，取消原因的具体内容前增加"[xxx]"，这个 xxx 显示是医院取消或是家长取消（2）医师手机端工作台页面中，统计数据三个球描边的颜色从原来的白色调整成为淡粉色」 |
| 涉及端 | parent-h5（5177）`pages/appointment/list`、`components/CancelSheet.vue`、`api/reserve.ts`、`types/index.ts`；doctor-h5（5176）`pages/home` |
| 状态 | 已开发（2026-09-20，真机浏览器 DOM/几何断言通过，两端 vue-tsc EXIT=0；同日按用户反馈将 105-2 描边由初版 `#f9a8d4` → 调淡 `#fbcfe8` → **最终改浅蓝 `#93c5fd`**） |

> **编号说明**：用户 #105 与本仓库自拟序列里的第 105 项（spec 54，admin-web basic-info「诊疗项目」改名换位）**重号**；按「用户编号优先」规则，本 spec 记为第 105 项（用户编号），自拟第 105 项仅作历史引用。

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 105-1a | 取消弹窗居中 + 严格模态 | parent-h5「我的预约」页点「取消预约」的弹窗改为**上下左右居中**；打开期间**不允许点击窗体以外的任何内容**（含底部 tabbar），点窗体/外部不关闭，只能通过关闭钮或提交结束 |
| 105-1b | 取消原因前缀 | 已取消卡片上「取消原因」内容前加 `[xxx]`：`[医院取消]` / `[家长取消]` |
| 105-2 | 医师端统计球描边 | doctor-h5 工作台三个统计球（儿童档案 / 本月预约 / 养护次数）描边由白色改为**淡粉色** |

---

## 2. 现状诊断

- **105-1a**：`CancelSheet.vue` 原为贴底 `sheet`（`.sheet` 默认 `bottom: 0`），遮罩 `.mask` 绑定了 `@click="close"`（点弹窗外部即关闭）；且 `.cancel-root` 的 `z-index: 90`。
  - 本页（`pages/appointment/list`）是 **tabBar 页**，uni-app H5 的 `uni-tabbar` 固定定位 `z-index: 998`。`.cancel-root`（`position: relative; z-index: 90`）自成层叠上下文，其内部 `.mask`(98)/`.sheet`(99) 都低于 998 —— 即**弹窗打开时底部 tabbar 仍浮在遮罩之上可被点击**，不满足「不允许点击窗体以外的内容」。
  - 真机浏览器视口为 0×0（IDE 内置浏览器未展开），`rpx→rem` 与百分比高度按 0 解析，无法在主页面上直接量几何，故几何验证改在**同源 375×667 iframe** 内完成（真实布局/真实 CSS/同一份代码）。
- **105-1b**：`list.vue` 原样输出 `取消原因：{{ item.cancelReason }}`，不区分取消来源。数据侧：`reserve_order.cancel_reason_type`（1家长原因 2医院原因）**历史数据大量为 NULL**（家长端取消接口原先未传该字段），且爽约记录（`no_show_flag=1`）的 `cancel_reason` 是「爽约（系统自动标记）」，与「取消」非同类。
- **105-2**：`.stats-ball` 原描边 `border: 2rpx solid rgba(255, 255, 255, 0.9)`（白），球体本身是浅蓝玻璃质感，白色描边在浅蓝底板上不显。

---

## 3. 改造设计

### 105-1a `frontend/parent-h5/src/components/CancelSheet.vue`

```html
<view v-if="visible" class="cancel-root">
  <!-- 严格模态：遮罩只负责挡住下层（含 tabbar），点击/拖动都不关闭弹窗 -->
  <view class="mask" @touchmove.stop.prevent="noop" />
  <view class="sheet sheet-center">   <!-- 复用 App.vue 全局居中样式 -->
```

```scss
// 本页是 tabbar 页，弹层层级需高于 uni-tabbar（z-index: 998）才能挡住底部导航
.cancel-root {
  position: relative;
  z-index: 1000;   /* 原 90 */
}
```

- 居中：复用全局 `.sheet-center`（`top: 50% + translateY(-50%)`、左右各 48rpx、圆角 24rpx、`max-height: 80vh`），与「我的」页各弹窗（#91/#102 已验收）同一套样式。
- 严格模态：遮罩去掉 `@click="close"`，改为 `@touchmove.stop.prevent="noop"`（禁拖动穿透）；层级提到 1000 > tabbar 998，遮罩完整覆盖底部导航并拦下点击。
- 关闭入口保留弹窗内右上角 ✕。

### 105-1b 取消原因前缀

`frontend/parent-h5/src/pages/appointment/list.vue`：

```html
<text class="rc-meta-item rc-reason">取消原因：{{ cancelPrefix(item) }}{{ item.cancelReason }}</text>
```

```ts
/**
 * 取消原因前缀：2=医院原因 → [医院取消]；1 或历史无类型数据 → [家长取消]。
 * 爽约由系统自动标记，不属于取消，不加前缀。
 */
function cancelPrefix(item: Reserve) {
  if (item.noShowFlag === 1) return ''
  return item.cancelReasonType === 2 ? '[医院取消]' : '[家长取消]'
}
```

`frontend/parent-h5/src/types/index.ts`：`Reserve` 补 `cancelReasonType?: number`（1家长原因 2医院原因）。

`frontend/parent-h5/src/api/reserve.ts`：家长端取消恒为家长原因，请求体补传该字段（后端 `CancelReserveRequest` 已支持，无需改动）：

```ts
cancelReserve: (id: number, cancelReason: string): Promise<void> =>
  post<void>(`/schedules/reserves/${id}/cancel`, { cancelReason, cancelReasonType: 1 }),
```

### 105-2 `frontend/doctor-h5/src/pages/home/index.vue`

```scss
.stats-ball {
  ...
  border: 2rpx solid #93c5fd;   /* 原 rgba(255, 255, 255, 0.9) 白色 → 浅蓝（同日先试粉 #f9a8d4/#fbcfe8，用户最终定蓝） */
}
```

球体渐变、内外阴影、反光斑均不变。

---

## 4. 验证计划与结果（2026-09-20 真机）

| 手段 | 结果 |
|---|---|
| `vue-tsc --noEmit`（parent-h5 / doctor-h5） | ✅ 两端均 EXIT=0 |
| 105-1a 居中（同源 375×667 iframe，真实布局） | ✅ 弹窗 rect `[24, 162, 327, 343]`，中心 (187.5, 333.5) 与视口中心 (187.5, 333.5) **偏移 dx=0 / dy=0**（375 宽下左右各留 24px 边距） |
| 105-1a 严格模态（同上） | ✅ 遮罩 rect `[0, 0, 375, 667]` 全覆盖；层级 `cancel-root z=1000 > uni-tabbar z=998`；在 tabbar 位置（375/2, 647）与左侧 tab 位置 (60, 647) 命中元素均为 `mask`；对遮罩/tabbar 位置派发 mousedown+mouseup+click 后弹窗仍打开、路由 hash 保持 `#/pages/appointment/list` 未跳转 |
| 105-1a 关闭入口 | ✅ 点弹窗内 ✕ → 弹窗移除；再次点「取消预约」可重新打开 |
| 105-1b 前缀（5177 刘妈妈，真实卡片 DOM） | ✅ 已取消卡片分别渲染「取消原因：[家长取消]临时有事，改期再约」「[家长取消]家长临时有事，改期再约」；两条爽约卡片为「取消原因：爽约（系统自动标记）」（无前缀） |
| 105-1b 端到端（真实预约→取消） | ✅ 经真实接口建一条临时预约（订单 127，2026-09-22 08:30），页面内提交取消：toast「预约已取消，次数已退还」，卡片变为「已取消 + 取消原因：[家长取消]验证 #105 弹窗（验证后清理）」；DB 订单 127 `status=4, cancel_reason_type=1, refund_flag=1, cancel_operator_name=刘妈妈` —— 家长端补传的类型已正确落库 |
| 105-2（5176 赵医生，工作台） | ✅ 3 个 `.stats-ball` computed `border: 1px solid rgb(147, 197, 253)`（=#93c5fd 浅蓝，最终版；中间试过 #f9a8d4/#fbcfe8 粉），底板仍为 #eff6ff |
| 数据清理 | ✅ 临时订单 127 与其 2 条 `child_service_record` 流水已删除；儿童 1 `remaining_count` 回到 1、时段 `booked_count` 回到 0（与验证前一致） |

---

## 5. 遗留与说明

1. **编号**：用户 #105 与自拟 #105（spec 54）重号，本项以用户编号为准。
2. **历史数据默认值**：`cancel_reason_type` 为 NULL 的历史记录（含门店/管理端早前取消的单）统一显示 `[家长取消]`；如需更精确（例如按取消操作人区分门店侧），可再提需求调整。爽约记录不加前缀（系统标记，非取消）。
3. **描边配色**：最终为浅蓝 `#93c5fd`（与球体浅蓝玻璃质感、底板 #eff6ff 同色系；该色值在 doctor-h5 首页其它元素中也已使用）。同日先试粉色 `#f9a8d4`→`#fbcfe8`，用户最终改为蓝色；如需更淡可降到 `#bfdbfe`、更深可到 `#60a5fa`。
4. 与 #63–#104、#106–#110 同属未提交改动，等待确认后一并入库。
