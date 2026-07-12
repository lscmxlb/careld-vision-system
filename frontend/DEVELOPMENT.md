# Careld视力养护系统 - 前端开发文档

## 项目概述

本项目包含三个Web前端应用：
- **admin-web**: 运营中心Web（PC端后台管理）
- **store-web**: 门店Web（PC端业务操作）
- **parent-web**: 家长Web（移动端H5）

## 技术栈

- Vue 3.4.x (Composition API)
- TypeScript
- Element Plus 2.5.x
- Pinia (状态管理)
- Vue Router 4.x
- Axios (HTTP客户端)
- ECharts 5.x (数据可视化)
- Vite (构建工具)
- SCSS (样式预处理器)

## 项目结构

```
frontend/
├── admin-web/          # 运营中心Web
│   ├── src/
│   │   ├── api/        # API接口
│   │   ├── components/ # 公共组件
│   │   ├── layouts/    # 布局组件
│   │   ├── router/     # 路由配置
│   │   ├── stores/     # Pinia状态管理
│   │   ├── types/      # TypeScript类型定义
│   │   ├── views/      # 页面组件
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   └── vite.config.ts
├── store-web/          # 门店Web
│   └── ...
└── parent-web/         # 家长Web
    └── ...
```

## 开发进度

### 已完成模块

#### admin-web (运营中心Web)
1. ✅ 项目脚手架搭建
2. ✅ 登录页面
3. ✅ 主布局组件
4. ✅ 数据看板（Dashboard）
   - 统计卡片
   - 客流趋势图表
   - 门店业绩排行
   - 待审核档案列表
   - 设备状态监控
5. ✅ 门店管理
   - 门店列表
   - 门店新增/编辑
   - 门店状态管理
6. ✅ 档案审核工作台
   - 待审核/已通过/已驳回筛选
   - 档案详情查看
   - 审核通过/驳回操作
7. ✅ 排班监控
   - 排班日历视图
   - 预约列表
   - 统计概览
8. ✅ 设备管理
   - 设备列表
   - 设备绑定/解绑
   - 设备校准
9. ✅ 用户管理
   - 用户列表
   - 新增/编辑用户
   - 重置密码
   - 启用/禁用用户
10. ✅ 系统设置
    - 系统参数配置
    - TV同步配置
    - 通知配置
    - 安全设置

#### store-web (门店Web)
1. ✅ 项目脚手架搭建
2. ✅ 登录页面
3. ✅ 主布局组件
4. ✅ 儿童档案管理
   - 档案列表
   - 新建档案
   - 编辑档案
   - 提交审核
5. ✅ 排班预约管理
   - 排班日历
   - 批量排班
   - 预约列表
6. ✅ 视力养护记录
   - 记录列表
   - 视力对比
   - 数据导出
7. ✅ TV设备管理
   - 设备列表
   - 设备绑定
   - 数据同步

#### parent-web (家长Web)
1. ✅ 项目脚手架搭建
2. ✅ 登录页面（手机验证码）
3. ✅ 移动端布局
4. ✅ 子女档案查看
   - 档案卡片
   - 视力概览
   - 档案绑定
5. ✅ 检测报告
   - 报告列表
   - 视力数据展示
6. ✅ 视力趋势
   - 趋势图表
   - 养护统计
   - 历史记录

## 组件说明

### 公共组件

#### Breadcrumb (面包屑)
- 路径: `admin-web/src/components/Breadcrumb.vue`
- 功能: 根据路由自动生成面包屑导航

### 布局组件

#### MainLayout (主布局)
- admin-web: 侧边栏 + 顶部导航 + 内容区
- store-web: 简化版侧边栏布局
- parent-web: 移动端底部导航 + 顶部标题栏

## API封装

### 请求封装
所有项目使用统一的Axios封装，位于 `src/api/request.ts`：
- 统一错误处理
- Token自动注入
- 401自动跳转登录

### API模块
- `auth.ts` - 认证相关
- `user.ts` - 用户管理
- `store.ts` - 门店管理
- `child.ts` - 儿童档案
- `schedule.ts` - 排班预约
- `device.ts` - 设备管理
- `statistics.ts` - 数据统计

## 状态管理

### User Store
管理用户登录状态和用户信息：
```typescript
const userStore = useUserStore()
userStore.login(credentials)  // 登录
userStore.logout()            // 登出
userStore.isLoggedIn          // 登录状态
userStore.userInfo            // 用户信息
```

### App Store
管理应用级别状态：
- sidebarCollapsed: 侧边栏折叠状态
- theme: 主题设置
- breadcrumbs: 面包屑数据

## 遇到的问题

### 1. Element Plus 图标导入
问题：图标组件需要单独导入
解决：使用 `@element-plus/icons-vue` 并全局注册

### 2. 移动端适配
问题：parent-web需要适配移动端
解决：使用响应式布局，底部导航栏设计

### 3. ECharts 在Vue3中的使用
问题：需要正确处理图表实例的创建和销毁
解决：使用ref获取DOM，在onUnmounted中销毁实例

### 4. 路由守卫
问题：需要统一处理登录状态校验
解决：在router.beforeEach中统一处理

## 单元测试

每个项目包含基础的单元测试配置：
- Vitest 作为测试框架
- 组件测试示例

运行测试：
```bash
npm run test
```

## 构建部署

### 开发环境
```bash
npm run dev
```

### 生产构建
```bash
npm run build
```

### 环境变量
- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置

## 代码规范

- 使用 ESLint + Prettier 进行代码格式化
- 组件名使用 PascalCase
- Props 必须定义类型和默认值
- 使用 Composition API

## 后续计划

1. 完善单元测试覆盖率
2. 添加更多图表组件封装
3. 优化移动端交互体验
4. 添加PWA支持
5. 性能优化（懒加载、代码分割）
