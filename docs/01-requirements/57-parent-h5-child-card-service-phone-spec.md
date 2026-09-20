# parent-h5 儿童档案卡片新增「服务电话」行 Spec（第 108 项，编号自拟）

| 项目 | 内容 |
|---|---|
| 文档类型 | 前端 Spec（列表卡片增加展示行，接口无改动） |
| 来源 | 用户需求（承接 #106/#107，未给编号，按序列自拟为第 108 项）：「在儿童档案的卡片上，在建档医院的下面添加一行，服务电话」 |
| 涉及端 | parent-h5（5177）`pages/child/index`（儿童档案列表卡片） |
| 状态 | 已开发（2026-09-20，浏览器 DOM 断言 + 数据联动验证通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 108-1 | 卡片增行 | 儿童档案列表的每张卡片中，在「建档医院」行**下方**增加「服务电话」行 |
| 108-2 | 取数口径 | 同 #106：该儿童建档医院在医院端基础信息维护的科室服务电话（`store_department.service_phone`），空值显示「—」 |

---

## 2. 现状诊断

- 儿童档案列表卡片当前展示：姓名/审核标签 → 性别·年龄·手机号 → **建档医院** → 审核提示 → 操作按钮。
- 列表数据来自 `GET /children?parentUserId=...`，与详情接口**共用** `ChildMapper.ENRICHED_COLUMNS`，#106 已把 `store_service_phone` 加进该列集，故列表响应中本就带 `storeServicePhone`，**本次无需改后端**。

---

## 3. 改造设计

`frontend/parent-h5/src/pages/child/index.vue`：建档医院行之后追加同结构一行（复用 `.child-store / .child-store-key / .child-store-value` 样式，无新增样式）：

```html
<view class="child-store">
  <text class="child-store-key">服务电话</text>
  <text class="child-store-value">{{ item.storeServicePhone || '—' }}</text>
</view>
```

两行 key 均为 4 字，左对齐位置一致；值超长按既有规则省略号截断。

---

## 4. 验证计划与结果（2026-09-20 真机）

| 手段 | 结果 |
|---|---|
| `vue-tsc --noEmit`（parent-h5） | ✅ EXIT=0 |
| dev server 产物核对 | ✅ `curl /src/pages/child/index.vue` 转译产物中 建档医院(13856) < 服务电话(15338) 且含 `storeServicePhone`（rsync 后已 touch 强制重编译） |
| 浏览器 DOM 断言（5177，家长 13600136001，4 张卡片） | ✅ 每张卡片均为 `建档医院=北京朝阳门店` → `服务电话=—`（当时科室未维护，空值兜底正确） |
| 数据联动 | ✅ 临时经医院端基础信息同款接口 `PUT /api/v1/departments/1` 写服务电话 `010-77778888` → 整页 reload 后 4 张卡片均显示 `服务电话=010-77778888`（证列表值与基础信息同源） |
| 数据复原 | ✅ 同款接口置空 → DB 三行科室恢复 NULL → 卡片复测显示 `服务电话=—` |

---

## 5. 遗留与说明

1. 仅 parent-h5 儿童档案列表卡片；该页其它区块与其它端未变动。
2. 编号自拟：本项用户未给编号，按序列记为 #108（自拟序列当前至 #108）。
3. 与 #63–#107 同属未提交改动，等待确认后一并入库。
