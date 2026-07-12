# Careld可尔欧得视力养护系统 - 前端项目

## 项目结构

本目录包含三个前端Web应用：

```
frontend/
├── admin-web/          # 运营中心Web (PC端后台管理)
├── store-web/          # 门店Web (PC端业务操作)
├── parent-web/         # 家长Web (移动端H5)
├── DEVELOPMENT.md      # 开发文档
└── README.md           # 本文件
```

## 技术栈

- **Vue 3.4.x** - 前端框架
- **TypeScript** - 类型安全
- **Element Plus 2.5.x** - UI组件库
- **Pinia** - 状态管理
- **Vue Router 4.x** - 路由管理
- **Axios** - HTTP客户端
- **ECharts 5.x** - 数据可视化
- **Vite** - 构建工具
- **SCSS** - 样式预处理器

## 项目功能

### admin-web (运营中心Web)
- ✅ 登录页面
- ✅ 数据看板（统计卡片、图表、待审核档案、设备监控）
- ✅ 门店管理（列表、新增、编辑、状态管理）
- ✅ 档案审核工作台（待审核/已通过/已驳回）
- ✅ 排班监控（日历视图、预约列表）
- ✅ 设备管理（绑定/解绑、校准）
- ✅ 用户管理（CRUD、重置密码）
- ✅ 系统设置（系统参数、TV同步、通知、安全）

### store-web (门店Web)
- ✅ 登录页面
- ✅ 儿童档案管理（建档、编辑、提交审核）
- ✅ 排班预约管理（日历、批量排班）
- ✅ 视力养护记录（查询、对比、导出）
- ✅ TV设备管理（绑定、同步）

### parent-web (家长Web)
- ✅ 登录页面（手机验证码）
- ✅ 子女档案查看（卡片、视力概览、绑定）
- ✅ 检测报告（报告列表、视力数据）
- ✅ 视力趋势（趋势图表、统计、历史记录）

## 快速开始

### 安装依赖
```bash
cd admin-web && npm install
cd ../store-web && npm install
cd ../parent-web && npm install
```

### 启动开发服务器
```bash
# admin-web
cd admin-web && npm run dev

# store-web
cd store-web && npm run dev

# parent-web
cd parent-web && npm run dev
```

### 构建生产版本
```bash
npm run build
```

## 项目统计

- **总文件数**: 97个 (Vue/TS/MD)
- **admin-web**: 35个组件/页面
- **store-web**: 20个组件/页面
- **parent-web**: 20个组件/页面
- **技术文档**: 2个 (README.md, DEVELOPMENT.md)

## 目录说明

每个项目包含以下目录结构：
```
src/
├── api/              # API接口封装
├── components/       # 公共组件
├── layouts/          # 布局组件
├── router/           # 路由配置
├── stores/           # Pinia状态管理
├── types/            # TypeScript类型定义
├── views/            # 页面组件
├── App.vue           # 根组件
└── main.ts           # 入口文件
```

## 开发文档

详细开发文档请查看 [DEVELOPMENT.md](./DEVELOPMENT.md)

## 注意事项

1. 所有项目使用Vue 3 Composition API
2. 状态管理使用Pinia
3. API请求统一封装在api目录
4. 类型定义统一在types目录
5. 样式使用SCSS预处理器
