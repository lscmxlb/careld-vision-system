# 医院端 store-web 测试反馈第 7 批改造 Spec（第 41 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 测试反馈改造 Spec（store-web 路由放开 + user-service `/users/me` 身份修复） |
| 来源 | 测试反馈第 41 项（docs/测试记录.txt 第 84 行） |
| 涉及端 | 门店医院Web端（store-web）、后端 careld-user-service |
| 涉及页面 | 预约管理-排班设置 |
| 状态 | 已开发（2026-09-14，vue-tsc 类型检查 + API（真实登录态 curl）+ 浏览器（DOM）验证通过） |

---

## 1. 需求清单（用户原话拆解）

| # | 页面 | 主题 | 改动面 | 状态 |
|---|---|---|---|---|
| 41 | 预约管理-排班设置 | 医生账号登录后「排班设置」显示内容不正确（实为被路由守卫重定向到数据看板） | store-web 路由（放开 managerOnly）+ user-service `/users/me` 医务人员身份识别 | ✅ 已完成 |

用户原话：「41、医生账号"13399999998"登录系统后，排班设置项目显示的内容不正确」

---

## 2. 关键设计决策

1. **诊断结论（两处独立根因）**：
   - 根因一（#41 表象）：「排班设置」菜单项位于对**所有角色可见**的「预约管理」组，但路由 meta 为 `managerOnly: true`（仅店长 userType=2）。医生点击后被守卫重定向 `/dashboard`，看到的是数据看板内容，表现为"排班设置显示的内容不正确"。
   - 根因二（顺带发现）：「医务人员」token 的 `userId` 是 `medical_staff.id`，与 `sys_user.id` 无关联。原 `/users/me` 直接用该 id 查 `sys_user`，医务人员 id=1 恰好命中 admin（超级管理员），导致抬头显示「超级管理员」、门店名为空、86 项权限被注入。
2. **用户拍板一：排班设置 = 医生可完整操作**。放开 `/schedule-rule` 路由的 `managerOnly` 限制，医生可查看并新增/编辑/删除排班规则；**菜单位置不动**（保持在「预约管理」组，该组本就对所有角色可见）。其余 managerOnly 页面（基础信息、医务人员、设备管理）保持仅店长。
3. **用户拍板二：抬头身份错误本轮一起修**。改 user-service `/users/me`：token 中 `userType=6` 时按 `medical_staff.id` 查询，返回医生本人姓名/门店/门店名，不再误返回 admin，也不再注入 sys_user 权限。
4. **顺带修复（同端点既有缺口）**：非医务人员（如店长 careld3）刷新后 `/me` 也不返回门店名——`getUserById` 走 MP `selectById`（无 join，`storeName` 为非表字段恒为 null），且登录响应 `LoginResponse.UserInfo` 本就没有 storeName 字段，故抬头「门店名为空」对所有角色都存在。在 `getCurrentUser`（仅 `/me` 路径）对 `storeName` 为空且 `storeId` 非空时补齐；**不在** `convertToResponse` 统一补（列表查询已由 join 提供，统一补会造成 N+1）。
5. **权限边界未变**：医生 token 不注入权限列表（`permissions: []`），与登录响应 `generateStaffTokenResponse` 口径一致；店长既有 16 项权限不受影响。
6. **未动的相邻隐患（遗留）**：`POST /users/me/password`（修改本人密码）仍会用 token 的 userId 查 `sys_user`——医生 token 会命中 admin 行（同 id 冲突）。store-web 无该入口，本轮未修，见「遗留」。
7. **审计字段口径**：MetaObjectHandler 按 `UserContext.getCurrentUserId()` 写 `created_by/updated_by`，医生操作排班规则时写入的是 `medical_staff.id`（本例=1），与 `sys_user.id` 值域重叠、无法从审计列反推账户体系。属既有设计，本轮未改。

---

## 3. 各项实现要点

### 3.1 #41 前端路由（`frontend/store-web/src/router/index.ts`）

```ts
{
  path: 'schedule-rule',
  name: 'ScheduleRule',
  component: () => import('@/views/schedule-rule/index.vue'),
  meta: { title: '排班设置', icon: 'Clock' }   // 去掉 managerOnly: true
},
```

- 守卫逻辑（`meta.managerOnly && userType !== 2 → next('/dashboard')`）本身不变；排班设置页内也没有角色分支逻辑（只有路由 meta 一处拦截）。
- 后端 `ScheduleRuleController` 本就无角色校验：`POST` 在 body 无 storeId 时取 `UserContext.getCurrentStoreId()`（医生=本院 7）；`PUT/DELETE/{id}` 无归属校验（既有行为，保留）；`GET` 走 `DataScopeHelper.resolveStoreId`（医生取 token storeId）。

### 3.2 #41 后端 `/users/me` 医务人员身份（careld-user-service）

- `UserController#getCurrentUser` 增加 `@RequestAttribute(value = "userType", required = false)`，`userType == 6` 时走新分支：

```java
// 医务人员（userType=6）的 userId 是 medical_staff 主键，与 sys_user 无关联，需单独返回本人身份
if (userType != null && userType == 6) {
    return Result.success(userService.getMedicalStaffCurrentUser(userId));
}
```

- `UserService` 新增 `getMedicalStaffCurrentUser(Long staffId)`；`UserServiceImpl` 实现：按 `medical_staff.id` 查询，返回 `id/username=phone/realName=name/phone/userType=6/storeId/storeName/status/createdAt`，`roles=['medical_staff']`、`permissions=[]`（与登录响应完全同构）。
- 门店名统一由 `selectStoreName(storeId)` 从 `store_info`（`deleted_at IS NULL`）查询；`getCurrentUser` 对非 6 类型在 `storeName` 为空时同样补齐（见决策 4）。

---

## 4. 验证记录（2026-09-14）

| 层 | 内容 | 结果 |
|---|---|---|
| 类型检查 | store-web `vue-tsc --noEmit` | 通过（exit 0） |
| 构建重启 | user-service `mvn package -DskipTests -pl careld-user-service -am`；先 kill 旧进程→等 8282 释放→启动 | BUILD SUCCESS；两次重启日志 `Started UserServiceApplication`（18:57:39 / 19:00:03，第二次为补齐 storeName 后重发版） |
| API | 医生（13399999998）登录后 `GET /users/me` | `{id:1, username:'13399999998', realName:'胡春花', userType:6, storeId:7, storeName:'武汉武昌协和卫生服务中心', roles:['medical_staff']}`，permissions=0 项（不再误返回 admin 的 86 项） |
| API | 店长（careld3）`GET /users/me` 回归 | `{id:114, realName:'胡院长', userType:2, storeId:7, storeName:'武汉武昌协和卫生服务中心'}`，permissions=16 项与改动前一致 |
| API | 医生 token `POST /schedule-rules`（2027-12-31，08:00-12:00 容量 3） | 200，返回 id=33；slot 查询该日生成 4 条整点时段 |
| API | 医生 token `PUT /schedule-rules/33`（容量改 5） | 200；列表回读容量=5 |
| API | 医生 token `DELETE /schedule-rules/33` | 200；列表回读仅剩既有规则（21） |
| DB | 测试规则 33 清理 | `schedule_rule_period/schedule_rule_exception` 0 残留、`schedule_slot`（2027-12-31）0 条；主行物理删除（`DELETE ... WHERE id=33`，影响 1 行） |
| 浏览器 | 医生账号 /schedule-rule | URL 停留（无重定向），卡片「排班设置」，表格 2 行（当前有效规则），行内 [编辑][删除]、页头 [新增排班规则] 均在 |
| 浏览器 | 医生账号抬头 | 「胡春花 / 武汉武昌协和卫生服务中心」（修复前为「超级管理员」+ 空） |
| 浏览器 | 医生账号菜单 | 档案管理 / 预约管理（无系统设置），与改动前一致 |
| 浏览器 | 医生账号 /device（managerOnly 回归） | 仍被重定向至 /dashboard（数据看板） |
| 浏览器 | 店长 careld3 /schedule-rule | 正常访问，抬头「胡院长 / 武汉武昌协和卫生服务中心」（门店名由本轮补齐），系统设置组可见 |
| 浏览器 | 控制台 | 无错误消息 |
| 浏览器 | 登录态恢复 | 验证后浏览器恢复为 careld3 登录 @ /dashboard（与验证前一致） |

**环境备注**：应用内浏览器为 0×0 视口，页面内 fetch 真实登录（未修改任何密码）；登录态经 `localStorage.setItem('token')` 切换，切换后整页导航触发守卫 `fetchUserInfo` 走真实 `/users/me`。

---

## 5. 遗留与注意事项

1. **`/users/me/password` 隐患未修**：医务人员 token（userId=medical_staff.id）调用该接口会命中 `sys_user` 同 id 行（当前=admin），理论上存在改错账号密码的风险。store-web 无此入口（仅 admin-web 有且不用医务人员 token），本轮未动；若后续为医务人员开放改密，必须先按 userType 分支处理。
2. **审计列口径（既有）**：`created_by/updated_by` 对医务人员写入 `medical_staff.id`，与 `sys_user.id` 值域重叠（本例排班规则 33 的 `created_by=1`）。如需审计可区分账户体系，需加 `user_type` 维度或前缀，属跨服务改造。
3. **间接验证并发数据变化**：验证期间 store 7 规则 id=32（2026-09-24~09-29）由账号 careld3（sys_user id=114）于 18:58:16 删除，与本轮改动无关（外部并发操作）；医生"完整操作"闭环用的是自建自删的规则 33，已物理清理。
4. **菜单位置与《医院端菜单顺序.txt》不一致（既有）**：文档中「排班设置」定义在系统设置组，实际在预约管理组；本次按用户拍板"医生可完整操作"未移动菜单，如后续要求归入系统设置组需一并处理医生可见性。
5. **非医务人员 `/me` 的 `roles` 仍为 null**：登录响应为 `['user']`，/me 未填充；store-web 未消费该字段，未改动。
6. **未提交 git**：本轮改动与既有未提交改动一同保留在工作区。
