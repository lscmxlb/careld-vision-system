# 医生端 doctor-h5 测试反馈第 1 批改造 Spec（第 54 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（医生端儿童档案卡片改版、详情/编辑弹窗确认、授权计费修复） |
| 来源 | 测试反馈第 54 项（docs/测试记录.txt 第 106-111 行） |
| 涉及端 | 医生端 H5（doctor-h5） |
| 涉及页面 | 儿童档案列表（pages/child/index）、档案详情（pages/child/detail）、儿童档案表单（pages/child/form）、次数授权（pages/child/grant） |
| 状态 | 已开发（2026-09-18，vue-tsc 类型检查 + 浏览器（DOM/截图/computed）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 子项 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 54-1a | 儿童档案列表 | 卡片上不放按钮，点击卡片直接进入详情 | doctor-h5 前端（模板） | ✅ 已完成 |
| 54-1b | 儿童档案列表 | 主治医师名称换成家长姓名，后跟手机号码 | doctor-h5 前端（模板） | ✅ 已完成 |
| 54-1c | 儿童档案列表 | 建档日期上移至原手机号位置 | doctor-h5 前端（模板） | ✅ 已完成 |
| 54-1d | 儿童档案列表 | 右上可用次数改为 A/B 格式（A=养护次数，B=可用次数） | doctor-h5 前端（模板+样式） | ✅ 已完成 |
| 54-2a | 档案详情 | 「授权次数」按钮更名为「预约授权」 | doctor-h5 前端（模板） | ✅ 已完成 |
| 54-2b | 编辑档案 | 页面有任何修改时，点「取消」或退出需弹窗确认 | doctor-h5 前端（取消按钮补脏校验） | ✅ 已完成 |
| 54-3a | 次数授权 | 金额未自动计算 → 修复为按「次数 × 科室收费标准」自动计算 | doctor-h5 前端（departmentApi 解包 PageResult） | ✅ 已完成 |
| 54-3b | 次数授权 | 开单医师改用选择框样式 | doctor-h5 前端（模板+样式） | ✅ 已完成 |
| 54-3c | 次数授权 | 去掉底部「授权后可用次数」框体 | doctor-h5 前端（模板+样式） | ✅ 已完成 |

用户原话：

- 「54、医生端
  (1)、儿童档案进入后直接显示各个卡片，卡片上不放按钮，点击卡片进入详情。卡片上的内容优化，将主治医师名称换成家长姓名，后面跟随手机号码，将建档日期上移至原电话号码位置。原可用次数位置优化，调整为A/B格式，A为养护次数，B为可用次数
  (2)、查看详情页面，授权次数名称修改为预约授权。编辑档案时，如果页面有任何修改，取消或退出时要弹窗确认。
  (3)、授权次数时，未自动计算金额，需计算费用。选择医生要用选择框的样式。去掉下面的授权后可用次数框体。」

---

## 2. 关键设计决策

1. **卡片整卡可点，操作入口收敛到详情**：删除卡片底部 `.child-actions` 按钮行（原「授权次数 / 查看详情」两个按钮）；整卡 `@click="goDetail(item)"` 保留，`goGrant` 从列表页脚本移除（授权入口由详情页「预约授权」承担）。符合「进入后直接显示各个卡片」的卡片流。
2. **卡片信息两行布局**：行 1 = 性别 · 年龄 · 建档日期（建档日期上移至原手机号位置，保留「建档」前缀）；行 2 = 家长姓名 · 手机号（替换原「建档日期 · 主治医师」行）。审核/禁用 tag 与驳回原因提示保持不变。
3. **A/B 次数带小标签**：右上角由「N 次」改为「养护 A / 可用 B」——A=`careCount`、B=`remainingCount`；数字沿用蓝色加粗（36rpx），「养护/可用」为 22rpx 灰色小标签，斜杠浅灰；B 为 0 时数字灰显（沿用 `remain-zero` 语义）。小标签是对「A/B」的补充说明，避免裸数字歧义。
4. **手机号沿用列表接口脱敏值**：列表返回 `138****8888`（后端脱敏口径，原卡片同样如此），本次未改后端；详情页 hero 为明文。
5. **详情页按钮更名**：`pages/child/detail.vue` 底部操作栏「授权次数」→「预约授权」（页面内无其它「授权次数」文案，无需全局替换）。
6. **编辑档案取消补脏校验**：`cancel()` 原直接 `leaving=true; navigateBack()`（程序化返回不触发 `onBackPress`，有修改也会静默丢弃）。抽出 `confirmDiscard()`（与 `onBackPress` 同一弹窗文案：「有未保存的修改，确认放弃吗？」/ 放弃修改 · 继续编辑），`cancel()` 与 `onBackPress` 共用：无修改直接退出；有修改弹窗，「继续编辑」留在本页、「放弃修改」返回。
7. **授权页取消同类修复（超出字面需求，属同一保护机制）**：`grant.vue` 的 `cancel()` 存在同样问题，按同一模式顺带修复；如不需要可单独回退。
8. **计费不生效根因（54-3a）**：医生端 `api/reserve.ts` 的 `departmentApi.getDepartmentList` 直接 `get<Department[]>('/departments')`，但 store-service `GET /departments` 返回 `PageResult`（`{list, pagination}`）——`grant.vue` 里 `list.find(...)` 抛 TypeError 被 `catch` 吞掉，`departmentStandard` 恒为 0，金额恒 0。修复为解包 `res?.list ?? []`（对齐 store-web `api/department.ts` 写法），并显式带 `size: 200` 拉全量。计费规则不变：缴费方式 ∈ {自费支付, 医保-个人余额, 医保-统筹支付} 时 金额 = 授权次数 × 门店第一条启用科室 charge_standard；免费体验/其它 = 0（可手填）。
9. **医生选择框样式**：picker 外层加 `.field-control`（`flex:1; min-width:0`，撑满标签右侧剩余宽度），内部 `.select-box` 走输入框外观——浅灰底 `#f8fafc` + 边框 `#e2e8f0` + 圆角 12rpx + 高 72rpx，右侧下箭头用纯 CSS（上/右边框 3rpx 旋转 135°）。缴费方式字段本次未动（需求仅点名「选择医生」）。
10. **删除「授权后可用次数」预览卡**：`preview-card` 整块及 `.preview-*` 样式移除（hero 区「当前可用次数」保留）。

---

## 3. 实现要点

### 3.1 列表卡片（`pages/child/index.vue`）

```html
<view v-for="item in visibleList" :key="item.id" class="card child-card" @click="goDetail(item)">
  <view class="child-head">
    <view class="child-name-row"> …姓名 + 审核/禁用 tag… </view>
    <view class="remain">
      <text class="remain-label">养护</text>
      <text class="remain-num">{{ item.careCount ?? 0 }}</text>
      <text class="remain-slash">/</text>
      <text class="remain-label">可用</text>
      <text class="remain-num" :class="{ 'remain-zero': !item.remainingCount }">{{ item.remainingCount ?? 0 }}</text>
    </view>
  </view>
  <view class="child-meta">
    <text>{{ genderText(item.gender) }}</text><text class="dot">·</text>
    <text>{{ ageLabel(item) }}</text><text class="dot">·</text>
    <text>建档 {{ (item.createdAt || '').slice(0, 10) || '—' }}</text>
  </view>
  <view class="child-meta">
    <text>{{ item.parentName || '—' }}</text><text class="dot">·</text>
    <text>{{ item.phone || '—' }}</text>
  </view>
  <view v-if="item.auditStatus === 2 && item.auditRemark" class="reject-tip">驳回原因：{{ item.auditRemark }}</view>
</view>
```

- 删除 `.child-actions` 模板块与样式、`.meta-key` 样式、`goGrant()` 函数；`.remain` 改为 flex 容器（`align-items: baseline`），新增 `.remain-label / .remain-num / .remain-slash`。

### 3.2 授权页（`pages/child/grant.vue`）

医生选择框：

```html
<view class="field">
  <text class="field-label required">开单医师</text>
  <view class="field-control">
    <picker mode="selector" :range="doctorNames" :value="doctorIndex" @change="onDoctorChange">
      <view class="select-box">
        <text class="select-text" :class="{ 'field-placeholder': !form.doctorId }">{{ doctorName || '请选择开单医师' }}</text>
        <view class="select-arrow" />
      </view>
    </picker>
  </view>
</view>
```

```scss
.field-control { flex: 1; min-width: 0; }
.select-box {
  display: flex; align-items: center; height: 72rpx; padding: 0 20rpx;
  background: #f8fafc; border: 1rpx solid #e2e8f0; border-radius: 12rpx; box-sizing: border-box;
}
.select-text { flex: 1; min-width: 0; font-size: 28rpx; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.select-arrow { width: 14rpx; height: 14rpx; flex: none; margin-left: 12rpx; border-top: 3rpx solid #94a3b8; border-right: 3rpx solid #94a3b8; transform: rotate(135deg); }
```

（uni-picker 在 H5 默认 `display:block`，外层 `flex:1` 容器即可撑满；`pages/child/grant.vue` 内 `.preview-card` 整块与 `.preview-*` 样式删除。）

### 3.3 取消脏确认（`form.vue`、`grant.vue` 同构）

```ts
function confirmDiscard() {
  uni.showModal({
    title: '提示',
    content: '有未保存的修改，确认放弃吗？',
    confirmText: '放弃修改',
    cancelText: '继续编辑',
    success: (res) => {
      if (res.confirm) { leaving.value = true; uni.navigateBack() }
    },
  })
}

function cancel() {
  if (leaving.value || !isDirty()) { leaving.value = true; uni.navigateBack(); return }
  confirmDiscard()
}

onBackPress(() => {
  if (leaving.value || !isDirty()) return false
  confirmDiscard()
  return true
})
```

### 3.4 API 解包（`api/reserve.ts`）

```ts
export const departmentApi = {
  /** 科室列表（授权自动计费取第一条启用科室的收费标准；接口返回 PageResult，此处解包为数组） */
  getDepartmentList: async (storeId?: number): Promise<Department[]> => {
    const res = await get<PageResult<Department>>('/departments', { storeId, size: 200 })
    return res?.list ?? []
  },
}
```

（`parent-h5` 同名方法为死代码、无调用方，本次未改。）

---

## 4. 验证记录（2026-09-18）

环境：doctor-h5 dev server 5176；登录账号 张小丽 13788888888（store 7，科室「视力校正门诊」charge_standard=168.00，id=9）。

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | doctor-h5 `npx vue-tsc --noEmit` | 通过（exit 0） |
| 浏览器·卡片结构 | 列表页 12 张卡片 innerText | 无「授权次数/查看详情」按钮；行 1「男 · 8岁 · 建档 2026-09-16」；行 2「杨沫沫 · 138\*\*\*\*8888」；右上「养护 0 / 可用 9」 |
| 浏览器·卡片交互 | 点击卡片 | 跳转 `#/pages/child/detail?id=46`（整卡进详情） |
| 浏览器·详情按钮 | 底部操作栏 innerText | 「编辑档案 \| 禁用 \| 预约授权」 |
| 浏览器·编辑取消 | 编辑页改「性别」后点取消 | 弹窗「提示 / 有未保存的修改，确认放弃吗？ / 继续编辑 / 放弃修改」；点「继续编辑」弹窗关闭停留本页；再点取消→「放弃修改」返回 `detail?id=46` |
| 浏览器·授权计费 | 授权页加载后 | 缴费金额输入框 disabled 值 `168`；提示「按「1 次 × 168 元/次」自动计算」（修复前恒 0） |
| 浏览器·次数联动 | 点 ＋ 至 2 次 | 金额 `336`；提示「按「2 次 × 168 元/次」自动计算」 |
| 浏览器·选择框 | `.select-box` / `.select-arrow` computed | 边框 `1px solid rgb(226,232,240)`、底色 `rgb(248,250,252)`、圆角 8.5px、高 51px、flex；箭头 rotate 135°（matrix -0.707）；picker 弹层可打开，选中「胡春花」后框内显示「胡春花」，金额仍 168 |
| 浏览器·预览卡 | `.preview-card` 查询 | 不存在（已删除） |
| 浏览器·授权页取消 | 改次数后点取消 | 弹同款脏确认，「放弃修改」返回详情页 |
| 浏览器·网络/控制台 | 全量请求 | 本批页面全部 200（含 `/departments?storeId=7&size=200`）；控制台无本次引入的错误（仅登录前遗留的 2 条 401/403） |
| 截图 | 列表页 / 详情页 / 授权页 | 已截图核对：卡片、预约授权按钮、选择框+168 元展示正常 |

---

## 5. 遗留与注意事项

1. **卡片手机号仍为脱敏显示**（`138****8888`，列表接口口径，改造前如此）：如需在卡片显示完整手机号，要改 child-service 列表接口返回明文，请确认后再做。
2. **缴费方式字段仍为纯文本 picker**（仅「开单医师」按要求改选择框）；如需统一外观，下一轮可一并调整。
3. **A/B 小标签为补充设计**：裸 `A/B` 语义不明，故加「养护/可用」小字；如用户希望纯数字格式（如 `0/9`），一行样式即可去掉标签。
4. **授权页取消脏确认为顺带修复**（需求字面只指编辑档案），如不需要可单独回退该处。
5. **视觉走查待用户确认**：卡片信息密度、A/B 标签字号、选择框高度等按测试记录截图反馈微调。
6. **未提交 git**：本轮改动保留在工作区。
