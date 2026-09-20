# 手机端（医师 + 家长）我的页优化 Spec（第 102 项，用户编号）

| 项目 | 内容 |
|---|---|
| 文档类型 | 前端 Spec（实时校验提示 + 关于页文案，无数据与接口改动） |
| 来源 | 用户需求（用户给定编号 **#102**）：「手机端优化」共 3 项 |
| 涉及端 | doctor-h5（5176）`pages/mine`；parent-h5（5177）`pages/mine` |
| 状态 | 已开发（2026-09-20，浏览器 DOM 断言通过，两端 vue-tsc EXIT=0） |

> **编号说明**：用户 #102 与本仓库自拟序列里的第 102 项（spec 49，dashboard 统计卡 1/2 对调）**重号**；按「用户编号优先」规则，本 spec 记为第 102 项（用户编号），自拟第 102 项仅作历史引用。

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 102-1 | 医师端修改密码实时提示 | doctor-h5「我的」→ 修改密码弹窗中，二次输入的新密码**不一致时实时提示**（输入过程即时反馈，不待提交） |
| 102-2 | 医师端关于文案 | doctor-h5「我的」→ 关于中，「系统服务支持电话：400-999-3608」一行改为两行：`系统服务商：可尔欧得医疗科技` / `服务支持电话：400-999-3608` |
| 102-3 | 家长端关于文案 | parent-h5「我的」→ 关于中，在原有内容后面追加两行：`系统服务商：可尔欧得医疗科技` / `服务支持电话：400-999-3608` |

---

## 2. 现状诊断

- **102-1**：doctor-h5 修改密码弹窗（`pwdVisible`）的三个输入框只有提交时 `submitPassword()` 里做 `newPassword !== confirmPassword` 校验（toast 提示），输入过程中无任何反馈；且按钮在提交前始终可点。parent-h5 同页**已有**成熟的实时比对实现（`pwdMismatch` / `pwdMatchText` / `.pwd-compare.is-bad/.is-ok`），本次以同思路回填到 doctor-h5，保持两端体验一致。
- **102-2**：doctor-h5 关于 sheet 末行为「系统服务支持电话：400-999-3608」（单行，语义混在一起：服务商未标注、电话未单列）。
- **102-3**：parent-h5 关于 sheet 原为三段说明文字（产品名 / 版本 / 账号说明），无服务商与服务电话信息。

---

## 3. 改造设计

### 102-1 `frontend/doctor-h5/src/pages/mine/index.vue`

脚本新增两个计算属性（`pwdForm` 已有）：

```ts
/** 二次新密码实时比对：非空即提示一致/不一致 */
const pwdMismatch = computed(() => !!pwdForm.value.confirmPassword && pwdForm.value.confirmPassword !== pwdForm.value.newPassword)
const pwdHintText = computed(() => {
  if (!pwdForm.value.confirmPassword) return ''
  return pwdMismatch.value ? '两次输入的新密码不一致' : '两次输入的新密码一致'
})
```

模板：确认密码输入框之后新增提示行；确认按钮在 `pwdSubmitting || pwdMismatch` 时禁用：

```html
<view class="pwd-hint-row">
  <text class="pwd-hint" :class="pwdMismatch ? 'is-bad' : 'is-ok'">{{ pwdHintText }}</text>
</view>
…
<view class="btn btn-primary" :class="{ 'is-disabled': pwdSubmitting || pwdMismatch }" @click="submitPassword">
```

样式：`.pwd-hint.is-bad` = `#ef4444`（红）、`.pwd-hint.is-ok` = `#16a34a`（绿）；`.pwd-hint-row` 留 `min-height: 40rpx` 占位避免弹窗高度跳动。原 `submitPassword()` 的提交校验原样保留（实时提示为辅助，不替代提交拦截）。

### 102-2 / 102-3 关于文案

| 端 | 改动 |
|---|---|
| doctor-h5 | 末行「系统服务支持电话：400-999-3608」替换为两行：`系统服务商：可尔欧得医疗科技`、`服务支持电话：400-999-3608`（首行加 `.about-contact` 上间距 16rpx，与上文分隔） |
| parent-h5 | 原有三行之后追加同格式两行（同样首行加 `.about-contact` 上间距） |

---

## 4. 验证计划与结果（2026-09-20 真机）

| 手段 | 结果 |
|---|---|
| `vue-tsc --noEmit`（doctor-h5 / parent-h5） | ✅ 两端均 EXIT=0 |
| 102-1 不一致态（5176 赵菲账号） | ✅ 旧 `oldpass1` / 新 `abcdef12` / 确认 `abcdef1` → 提示「两次输入的新密码不一致」，class `pwd-hint is-bad`，按钮 class 含 `is-disabled` |
| 102-1 一致态 | ✅ 确认改为 `abcdef12` → 提示「两次输入的新密码一致」，class `pwd-hint is-ok`，computed 色 `rgb(22,163,74)`（=#16a34a），按钮恢复 `btn btn-primary`（可点） |
| 102-2 doctor-h5 关于 | ✅ 5 行：产品名 / 版本 / 密码说明 / **系统服务商：可尔欧得医疗科技** / **服务支持电话：400-999-3608**；旧「系统服务支持电话：…」行已不存在 |
| 102-3 parent-h5 关于（5177 刘妈妈） | ✅ 5 行：原三行 + **系统服务商：可尔欧得医疗科技** + **服务支持电话：400-999-3608** |
| 数据影响 | ✅ 纯前端文案与本地校验，无接口调用与数据变更 |

说明：验证过程未实际提交密码修改（仅输入触发比对逻辑），未改动任何账号密码；弹窗最终以「取消」关闭。

---

## 5. 遗留与说明

1. 实时提示为**辅助**：仅当确认密码非空时显示，提交按钮在不一致时禁用；原提交时的 toast 校验保留（双保险）。
2. doctor-h5 关于新增两行的**行距**沿用 `.about-sub`（25rpx 灰字），与既有说明行风格一致；parent-h5 同步。
3. 编号：用户 #102 与自拟 #102（spec 49）重号，本项以用户编号为准。
4. **后续更新**：关于弹窗中的密码相关说明行已按用户要求删除（见 spec 60 / 自拟 #110），本文第 4 节关于弹窗的「5 行」断言为当时状态。
5. 与 #63–#101、#103–#109 同属未提交改动，等待确认后一并入库。
