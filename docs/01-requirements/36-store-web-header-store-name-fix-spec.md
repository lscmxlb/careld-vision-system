# store-web 页头医院名称不显示修复 Spec（第 88 项）

| 项目 | 内容 |
|---|---|
| 文档类型 | 缺陷修复 Spec（前端，无后端改动） |
| 来源 | 用户需求第 88 项：`http://39.162.49.28:5175/child` 进入页面后，上方的医院 logo 可以正常显示，医院名称文字没有显示出来 |
| 涉及端 | store-web（医院端） |
| 涉及页面 | 全部登录后页面（页头在 `layouts/MainLayout.vue`），用户报于 `/child` |
| 状态 | 已开发（2026-09-19，真机复现 + 修复后复验通过，vue-tsc EXIT=0） |

---

## 1. 需求清单

| # | 子项 | 说明 |
|---|---|---|
| 88-1 | 名称显示 | 进入页面后页头应正常显示医院名称（与医院 logo 同排） |

---

## 2. 现状诊断

### 2.1 现象与复现

- 页头结构：`MainLayout.vue` 的 `.header-left` = `his.png` 医院 logo + `{{ userStore.userInfo?.storeName }}`。
- 真机复现（修复前）：清空 token → `/login` → 手机号 `13800138001`（store001_mgr 王建国）登录 → SPA 跳转至 `/dashboard`、再点侧边栏进入 `/child`，页头 `<span>` 为空：

  ```
  headerHTML: <img src="/src/assets/his.png" class="header-logo"><span data-v-22686b16=""></span>
  ```

  即 logo（静态资源）正常、医院名称空白 —— 与用户描述一致。
- 手动整页刷新后名称恢复显示，说明并非账号数据缺失。

### 2.2 根因

1. **登录响应不含门店名**：`AuthServiceImpl.generateTokenResponse` 组装的 `user` 只含 `id/username/realName/phone/userType/storeId/centerId/agentId/deptId/jobTitle/roles/permissions`，**没有 `storeName`**；门店名仅在 `/users/me`（`UserServiceImpl.getCurrentUser` → `selectStoreName` 查 `store_info`）中补齐。
2. **登录后不会自动刷新**：登录页 `router.push('/dashboard')` 为 SPA 跳转；`userInfo` 不持久化（localStorage 只存 token），刷新页面时由路由守卫调用 `fetchUserInfo()` 补偿，但登录路径不经过该分支。
3. 结果：登录后直接浏览，页头永远拿不到 `storeName`；只有手动整页刷新才显示。

---

## 3. 改造设计

`frontend/store-web/src/stores/user.ts` 的 `login()`：设置 token 后立即补拉一次完整用户信息，与路由守卫的刷新补偿同源：

```ts
const login = async (loginData: LoginRequest) => {
  const res = await authApi.login(loginData)
  setToken(res.accessToken, res.refreshToken)
  userInfo.value = res.user
  // 登录响应不含门店名等展示字段（仅在 /users/me 中补齐），登录后立即补拉一次
  try {
    await fetchUserInfo()
  } catch {
    // 补拉失败保留登录响应中的基础信息
  }
  return res
}
```

- 一处修复覆盖所有入口（页头、权限判断等所有 `userInfo` 消费方），无需后端改动与重启。
- 补拉失败（网络异常）时静默降级，不影响登录成功。

---

## 4. 验证计划与结果（2026-09-19 真机）

| 项 | 手段 | 结果 |
|---|---|---|
| 缺陷复现 | 清空 token → 登录 → 进入 `/dashboard`、`/child` 读取页头 DOM | ✅ 修复前 span 为空（`<span></span>`），logo 正常 |
| **修复后复验** | 同流程重新登录（手机号 13800138001）→ `/dashboard` → 侧边栏进 `/child` | ✅ 页头显示 `北京朝阳门店`，logo `his.png` 加载成功（`naturalWidth > 0`） |
| 网络证据 | 页面网络记录 | ✅ 登录后出现 `GET /api/v1/users/me [200]` |
| 整页刷新回归 | 直接打开 `/child`（走路由守卫） | ✅ 名称仍正常，守卫逻辑未变 |
| 类型检查 | store-web `vue-tsc --noEmit` | ✅ EXIT=0 |

---

## 5. 遗留与说明

1. 后端登录响应仍未包含 `storeName`（本次走前端补拉，未动 auth 服务、无需重启）。admin-web / parent-web 已核查：页头未使用 `storeName`，不受影响；若后续有端要在登录即刻展示门店名，可同样补拉或改由后端返回。
2. 登录流程多一次 `/users/me` 请求（毫秒级，可接受）。
3. 与 #75–#87 同属未提交改动，等待确认后一并入库。
