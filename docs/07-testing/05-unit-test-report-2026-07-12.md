# Careld 视力养护系统 - 单元测试修复与运行报告

**报告日期**: 2026-07-12
**执行环境**: Linux / Node.js v22.22.0 / OpenJDK 25.0.3 / Maven 3.9.9
**执行人员**: Claude Code Agent

---

## 一、测试框架搭建

### 1.1 前端测试基础设施

**问题**: 三个前端应用存在测试文件但缺少完整的测试基础设施

**修复内容**:

| 修复项 | parent-web | admin-web | store-web |
|--------|------------|-----------|-----------|
| `package.json` 添加 vitest | ✅ | ✅ | ✅ |
| `package.json` 添加 @vue/test-utils | ✅ | ✅ | ✅ |
| `package.json` 添加 jsdom | ✅ | ✅ | ✅ |
| `package.json` 添加 test 脚本 | ✅ | ✅ | ✅ |
| 创建 `vitest.config.ts` | ✅ | ✅ | ✅ |
| 创建 `src/test-setup.ts` | ✅ | ✅ | ✅ |
| 删除无意义 example.spec.ts | ✅ | - | ✅ |
| 编写有意义的测试用例 | ✅ | ✅ | ✅ |

**新增 devDependencies**:
```json
{
  "@vue/test-utils": "^2.4.6",
  "jsdom": "^25.0.1",
  "vitest": "^3.2.4"
}
```

**新增 test 脚本**:
```json
{
  "test": "vitest run",
  "test:watch": "vitest",
  "test:coverage": "vitest run --coverage"
}
```

**test-setup.ts 提供的全局 mock**:
- `localStorage` — 完整的 mock 实现
- Element Plus 组件 stubs（ElBreadcrumb, ElMenu, ElTable 等 20+ 组件）
- 每个测试前自动清理 mock 状态

### 1.2 后端测试修复

| 修复项 | 文件 | 修复内容 |
|--------|------|----------|
| JWT 测试密钥 | `JwtUtilTest.java` | 替换为明显测试用途的长密钥 |
| JWT 类型断言 | `JwtUtilTest.java` | 修复 Long/Integer 反序列化差异 |
| AES 密钥长度 | `AesUtilTest.java` | 修正为恰好 32 字节 |
| 密码打印 | `AuthServiceTest.java` | 移除 `System.out.println` |
| 空测试方法 | `UserServiceTest.java` | 替换为 DTO 字段验证测试 |

---

## 二、测试运行结果

### 2.1 后端测试 (careld-common)

```
[INFO] Tests run: 3, Failures: 0, Errors: 0 -- MaskUtilTest
[INFO] Tests run: 4, Failures: 0, Errors: 0 -- AesUtilTest
[INFO] Tests run: 3, Failures: 0, Errors: 0 -- JwtUtilTest
[INFO] BUILD SUCCESS
```

| 测试类 | 用例数 | 通过 | 失败 | 说明 |
|--------|--------|------|------|------|
| `MaskUtilTest` | 3 | ✅ | 0 | 手机号/姓名/身份证脱敏 |
| `AesUtilTest` | 4 | ✅ | 0 | AES 加解密、null 处理、手机号加密 |
| `JwtUtilTest` | 3 | ✅ | 0 | Token 生成/解析、userId 提取、无效 Token |
| **合计** | **10** | **10** | **0** | **通过率 100%** |

### 2.2 前端测试

#### parent-web（6 个测试）

```
✓ src/stores/__tests__/user.spec.ts (6 tests)

Test Files  1 passed (1)
     Tests  6 passed (6)
  Duration  613ms
```

| 测试用例 | 验证内容 |
|----------|----------|
| `initializes with empty state` | 初始状态：token 空、未登录、无权限 |
| `reads existing token from localStorage` | 从 localStorage 恢复登录态 |
| `sets token and persists` | setToken 写入内存 + localStorage |
| `clears token and removes` | clearToken 清空所有状态 |
| `hasPermission matching` | 精确权限匹配 |
| `hasPermission wildcard` | 通配符 `*` 权限 |

#### admin-web（9 个测试）

```
✓ src/stores/__tests__/app.spec.ts (6 tests)
✓ src/components/__tests__/Breadcrumb.spec.ts (3 tests)

Test Files  2 passed (2)
     Tests  9 passed (9)
  Duration  656ms
```

**AppStore 测试 (6)**:
| 测试用例 | 验证内容 |
|----------|----------|
| `initializes with default state` | 侧边栏展开、浅色主题、无加载 |
| `toggles sidebar` | toggleSidebar 双向切换 |
| `sets sidebar explicitly` | setSidebarCollapsed 直接设置 |
| `switches theme` | light ↔ dark 主题切换 |
| `sets loading state` | 全局 loading 状态管理 |
| `sets breadcrumbs` | 面包屑数据设置 |

**Breadcrumb 组件测试 (3)**:
| 测试用例 | 验证内容 |
|----------|----------|
| `renders home link` | 始终显示"首页" |
| `renders items from route` | 根据路由 matched 渲染面包屑 |
| `no undefined titles` | 过滤无 meta.title 的路由 |

#### store-web（3 个测试）

```
✓ src/stores/__tests__/user.spec.ts (3 tests)

Test Files  1 passed (1)
     Tests  3 passed (3)
  Duration  866ms
```

| 测试用例 | 验证内容 |
|----------|----------|
| `initializes with default state` | 初始空状态 |
| `setToken updates state` | Token 设置 + 登录状态 |
| `clearToken resets all` | Token 清除 + 状态重置 |

---

## 三、测试总览

| 类别 | 测试文件 | 用例数 | 通过 | 失败 | 耗时 |
|------|----------|--------|------|------|------|
| 后端 Java | 3 | 10 | 10 | 0 | 4.9s |
| 前端 parent-web | 1 | 6 | 6 | 0 | 0.6s |
| 前端 admin-web | 2 | 9 | 9 | 0 | 0.7s |
| 前端 store-web | 1 | 3 | 3 | 0 | 0.9s |
| **总计** | **7** | **28** | **28** | **0** | **7.1s** |

**整体通过率**: **100%** ✅

---

## 四、运行命令

### 后端测试
```bash
# 需要 Java 17+ 和 Maven 3.6+
export JAVA_HOME="/path/to/jdk"
cd backend
mvn test -pl careld-common
```

### 前端测试
```bash
# 首次需安装依赖
cd frontend/parent-web  # 或 admin-web / store-web
npm install

# 运行测试
npm run test            # 单次运行
npm run test:watch      # 监听模式
npm run test:coverage   # 覆盖率报告
```

---

## 五、后续改进建议

1. **提升覆盖率**: 当前仅测试了 store 和组件挂载，建议补充：
   - API 请求 mock 测试
   - 路由守卫测试
   - 表单验证测试
2. **E2E 测试**: 建议引入 Playwright 进行端到端测试
3. **CI 集成**: 在 GitHub Actions / GitLab CI 中集成测试流程
4. **后端集成测试**: 使用 Testcontainers 启动真实 MySQL/Redis 进行集成测试

---

**报告完成时间**: 2026-07-12
**验证状态**: ✅ 全部 28 个测试通过
