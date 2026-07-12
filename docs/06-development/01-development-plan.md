# 第六阶段：开发实施计划

## 6.1 开发阶段划分

### 6.1.1 开发里程碑

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          开发时间线（总计20周）                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Week 1-2   │  Week 3-6   │  Week 7-10  │  Week 11-14 │  Week 15-18 │ Week 19-20 │
│  环境搭建   │  后端开发    │  前端开发    │  TV端开发   │  联调测试   │  优化上线  │
│             │  核心服务   │  Web三端    │  电子视力表 │             │            │
│             │             │             │  离线同步   │             │            │
│             │             │             │             │             │            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.1.2 详细排期

| 阶段 | 周期 | 主要任务 | 交付物 |
|------|------|---------|--------|
| **Sprint 0** | 第1-2周 | 环境搭建、基础框架 | 开发环境、CI/CD |
| **Sprint 1** | 第3-4周 | 用户/门店/设备服务 | 基础服务上线 |
| **Sprint 2** | 第5-6周 | 档案/排班/预约服务 | 核心业务API |
| **Sprint 3** | 第7-8周 | 视力检测/同步服务 | TV同步API完成 |
| **Sprint 4** | 第9-10周 | 门店Web开发 | 门店端可用 |
| **Sprint 5** | 第11-12周 | 运营Web开发 | 运营后台可用 |
| **Sprint 6** | 第13-14周 | 家长Web开发 | 家长端可用 |
| **Sprint 7** | 第15-16周 | TV APK开发 | TV端可用 |
| **Sprint 8** | 第17-18周 | 联调测试 | 全链路通 |
| **Sprint 9** | 第19-20周 | 优化上线 | 生产就绪 |

---

## 6.2 团队分工

### 6.2.1 团队配置

```
┌─────────────────────────────────────────────────────────────────┐
│                      开发团队配置                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    项目管理 (1人)                        │   │
│  │  产品经理/项目经理                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌───────────────────────────┼───────────────────────────┐   │
│  │                           │                           │   │
│  ▼                           ▼                           ▼   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   后端组 (3人)  │  │   前端组 (3人)  │  │  TV端组 (2人)   │ │
│  │                 │  │                 │  │                 │ │
│  │ • 技术负责人     │  │ • Vue开发 x2    │  │ • Android开发x2 │ │
│  │ • Java开发 x2   │  │ • UI设计师      │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                              │                                  │
│  ┌───────────────────────────┴───────────────────────────┐   │
│  │                    测试组 (2人)                        │   │
│  │  • 测试工程师 x2                                       │   │
│  │  • 自动化测试                                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌───────────────────────────┴───────────────────────────┐   │
│  │                    运维组 (1人)                        │   │
│  │  • DevOps工程师                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  总计：10人                                                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2.2 详细分工

#### 后端组（3人）

| 角色 | 人数 | 职责 |
|------|------|------|
| 技术负责人 | 1 | 架构设计、代码审查、技术决策 |
| Java开发工程师A | 1 | 用户/门店/档案服务 |
| Java开发工程师B | 1 | 预约/视力/同步/统计服务 |

#### 前端组（3人）

| 角色 | 人数 | 职责 |
|------|------|------|
| Vue开发工程师A | 1 | 门店Web + 家长Web |
| Vue开发工程师B | 1 | 运营Web + 公共组件 |
| UI设计师 | 1 | 四端UI设计、交互设计 |

#### TV端组（2人）

| 角色 | 人数 | 职责 |
|------|------|------|
| Android开发工程师A | 1 | 电子视力表组件、UI |
| Android开发工程师B | 1 | 离线同步、网络模块 |

---

## 6.3 开发规范

### 6.3.1 代码规范

#### Java代码规范

```java
/**
 * 代码规范示例
 * 
 * 1. 类注释必须包含作者、日期、功能描述
 * 2. 方法注释使用JavaDoc格式
 * 3. 常量使用大写下划线命名
 * 4. 变量使用驼峰命名
 * 5. 方法不超过50行
 * 6. 类不超过500行
 */

/**
 * 儿童档案服务
 * 
 * @author Zhang San
 * @since 2024-01-01
 */
@Service
@Slf4j
public class ChildService {
    
    private static final int MAX_NAME_LENGTH = 64;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    
    private final ChildRepository childRepository;
    private final EncryptionService encryptionService;
    
    public ChildService(ChildRepository childRepository, 
                       EncryptionService encryptionService) {
        this.childRepository = childRepository;
        this.encryptionService = encryptionService;
    }
    
    /**
     * 创建儿童档案
     * 
     * @param request 创建请求
     * @param operatorId 操作人ID
     * @return 创建的档案
     * @throws BusinessException 业务异常
     */
    @Transactional
    public ChildDTO createChild(CreateChildRequest request, Long operatorId) {
        // 参数校验
        validateCreateRequest(request);
        
        // 加密敏感数据
        String encryptedName = encryptionService.encrypt(request.getName());
        String encryptedPhone = encryptionService.encrypt(request.getPhone());
        
        // 构建实体
        ChildEntity entity = ChildEntity.builder()
            .nameEncrypted(encryptedName)
            .nameMask(MaskUtil.maskName(request.getName()))
            .phoneEncrypted(encryptedPhone)
            .phoneMask(MaskUtil.maskPhone(request.getPhone()))
            .auditStatus(0)
            .createdBy(operatorId)
            .build();
        
        // 保存
        ChildEntity saved = childRepository.save(entity);
        
        log.info("Child created: id={}, operator={}", saved.getId(), operatorId);
        
        return convertToDTO(saved);
    }
    
    private void validateCreateRequest(CreateChildRequest request) {
        Assert.notNull(request, "Request must not be null");
        Assert.hasText(request.getName(), "Name must not be empty");
        Assert.isTrue(request.getName().length() <= MAX_NAME_LENGTH, 
            "Name too long");
    }
}
```

#### Vue代码规范

```typescript
/**
 * Vue组件规范
 * 
 * 1. 使用Composition API
 * 2. 组件名使用PascalCase
 * 3. Props必须定义类型和默认值
 * 4. 使用TypeScript
 * 5. 样式使用scoped
 */

<template>
  <div class="child-list">
    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="age" label="年龄" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button @click="handleView(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getChildList } from '@/api/child'
import type { Child } from '@/types/child'

// Props定义
interface Props {
  storeId: number
}

const props = defineProps<Props>()

// 状态
const loading = ref(false)
const tableData = ref<Child[]>([])

// 方法
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getChildList({ storeId: props.storeId })
    tableData.value = res.data.list
  } finally {
    loading.value = false
  }
}

const handleView = (row: Child) => {
  // 查看详情逻辑
}

// 生命周期
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.child-list {
  padding: 20px;
}
</style>
```

### 6.3.2 Git工作流

```
Git Flow:

main        ●────────────────────────────────────────●
            │                                         │
release/    ●────────●                                │
v1.0.0                 │                                │
                       │                                │
develop   ●───────────●───────────────────────────────●
            │           │                               │
feature/    ●───────────●                               │
child-api                 │                               │
                          │                               │
feature/                  ●───────────●                   │
vision-api                              │                   │
                                        │                   │
hotfix/                                                 ●───●
v1.0.1
```

#### 分支规范

| 分支 | 用途 | 命名规范 |
|------|------|---------|
| main | 生产分支 | main |
| develop | 开发分支 | develop |
| feature | 功能开发 | feature/功能名 |
| release | 发布准备 | release/版本号 |
| hotfix | 紧急修复 | hotfix/版本号 |

#### Commit规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型**

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复bug |
| docs | 文档更新 |
| style | 代码格式 |
| refactor | 重构 |
| test | 测试代码 |
| chore | 构建/工具 |

**示例**

```
feat(child): 新增儿童档案审核功能

- 添加档案审核API
- 添加审核状态变更逻辑
- 添加审核日志记录

Closes #123
```

---

## 6.4 开发任务拆解

### 6.4.1 后端任务清单

#### Sprint 1: 基础服务（第3-4周）

| 任务ID | 任务名称 | 负责人 | 工时 | 状态 |
|--------|---------|--------|------|------|
| BE-001 | 项目脚手架搭建 | 技术负责人 | 2d | ⬜ |
| BE-002 | 数据库连接配置 | 技术负责人 | 1d | ⬜ |
| BE-003 | Redis连接配置 | 技术负责人 | 1d | ⬜ |
| BE-004 | 统一响应封装 | 技术负责人 | 1d | ⬜ |
| BE-005 | 全局异常处理 | 技术负责人 | 1d | ⬜ |
| BE-006 | JWT认证实现 | Java开发A | 2d | ⬜ |
| BE-007 | RBAC权限控制 | Java开发A | 2d | ⬜ |
| BE-008 | 用户CRUD接口 | Java开发A | 2d | ⬜ |
| BE-009 | 门店CRUD接口 | Java开发A | 2d | ⬜ |
| BE-010 | TV设备管理接口 | Java开发B | 2d | ⬜ |
| BE-011 | 数据加密工具 | Java开发B | 2d | ⬜ |
| BE-012 | 操作日志记录 | Java开发B | 2d | ⬜ |

#### Sprint 2: 核心业务（第5-6周）

| 任务ID | 任务名称 | 负责人 | 工时 | 状态 |
|--------|---------|--------|------|------|
| BE-013 | 儿童档案CRUD | Java开发A | 3d | ⬜ |
| BE-014 | 档案审核流程 | Java开发A | 2d | ⬜ |
| BE-015 | 档案加密存储 | Java开发A | 2d | ⬜ |
| BE-016 | 排班管理接口 | Java开发B | 3d | ⬜ |
| BE-017 | 预约管理接口 | Java开发B | 3d | ⬜ |
| BE-018 | 预约冲突检测 | Java开发B | 2d | ⬜ |

#### Sprint 3: TV同步（第7-8周）

| 任务ID | 任务名称 | 负责人 | 工时 | 状态 |
|--------|---------|--------|------|------|
| BE-019 | 视力检测记录接口 | Java开发B | 2d | ⬜ |
| BE-020 | 视力对比查询 | Java开发B | 2d | ⬜ |
| BE-021 | TV批量上传接口 | Java开发B | 3d | ⬜ |
| BE-022 | 增量下发接口 | Java开发B | 2d | ⬜ |
| BE-023 | 同步冲突处理 | Java开发B | 2d | ⬜ |
| BE-024 | 数据统计接口 | Java开发A | 3d | ⬜ |
| BE-025 | 数据导出接口 | Java开发A | 2d | ⬜ |

### 6.4.2 前端任务清单

#### Sprint 4: 门店Web（第9-10周）

| 任务ID | 任务名称 | 负责人 | 工时 | 状态 |
|--------|---------|--------|------|------|
| FE-001 | 项目脚手架搭建 | Vue开发A | 1d | ⬜ |
| FE-002 | 登录页面 | Vue开发A | 2d | ⬜ |
| FE-003 | 布局组件 | Vue开发A | 2d | ⬜ |
| FE-004 | 档案管理页面 | Vue开发A | 3d | ⬜ |
| FE-005 | 档案表单组件 | Vue开发A | 2d | ⬜ |
| FE-006 | 预约管理页面 | Vue开发A | 3d | ⬜ |
| FE-007 | 排班日历组件 | Vue开发A | 3d | ⬜ |
| FE-008 | 视力记录查询 | Vue开发A | 2d | ⬜ |
| FE-009 | 设备管理页面 | Vue开发A | 2d | ⬜ |

#### Sprint 5: 运营Web（第11-12周）

| 任务ID | 任务名称 | 负责人 | 工时 | 状态 |
|--------|---------|--------|------|------|
| FE-010 | 运营后台布局 | Vue开发B | 2d | ⬜ |
| FE-011 | 门店管理页面 | Vue开发B | 2d | ⬜ |
| FE-012 | 档案审核页面 | Vue开发B | 2d | ⬜ |
| FE-013 | 排班总览页面 | Vue开发B | 2d | ⬜ |
| FE-014 | 数据看板页面 | Vue开发B | 3d | ⬜ |
| FE-015 | 图表组件封装 | Vue开发B | 2d | ⬜ |
| FE-016 | 系统配置页面 | Vue开发B | 2d | ⬜ |

#### Sprint 6: 家长Web（第13-14周）

| 任务ID | 任务名称 | 负责人 | 工时 | 状态 |
|--------|---------|--------|------|------|
| FE-017 | 家长端布局 | Vue开发A | 2d | ⬜ |
| FE-018 | 档案绑定页面 | Vue开发A | 2d | ⬜ |
| FE-019 | 档案查看页面 | Vue开发A | 2d | ⬜ |
| FE-020 | 报告列表页面 | Vue开发A | 2d | ⬜ |
| FE-021 | 视力趋势图表 | Vue开发A | 2d | ⬜ |

### 6.4.3 TV端任务清单

#### Sprint 7: TV APK（第15-16周）

| 任务ID | 任务名称 | 负责人 | 工时 | 状态 |
|--------|---------|--------|------|------|
| TV-001 | 项目脚手架搭建 | Android开发A | 1d | ⬜ |
| TV-002 | 设备登录页面 | Android开发A | 2d | ⬜ |
| TV-003 | 门店绑定功能 | Android开发A | 2d | ⬜ |
| TV-004 | 屏幕校准模块 | Android开发A | 3d | ⬜ |
| TV-005 | SQLite数据库 | Android开发B | 2d | ⬜ |
| TV-006 | 离线数据存储 | Android开发B | 2d | ⬜ |
| TV-007 | 网络状态监听 | Android开发B | 2d | ⬜ |
| TV-008 | 自动同步机制 | Android开发B | 3d | ⬜ |
| TV-009 | 电子视力表组件 | Android开发A | 5d | ⬜ |
| TV-010 | 儿童检索功能 | Android开发A | 2d | ⬜ |
| TV-011 | 视力录入页面 | Android开发A | 2d | ⬜ |
| TV-012 | 同步日志页面 | Android开发B | 2d | ⬜ |

---

## 6.5 开发环境

### 6.5.1 环境规划

| 环境 | 用途 | 数据库 | 配置 |
|------|------|--------|------|
| Local | 本地开发 | H2/SQLite | application-local.yml |
| Dev | 开发联调 | MySQL Dev | application-dev.yml |
| Test | 测试验证 | MySQL Test | application-test.yml |
| Staging | 预发布 | MySQL Staging | application-staging.yml |
| Prod | 生产环境 | MySQL Prod | application-prod.yml |

### 6.5.2 本地开发启动

```bash
# 1. 启动基础设施
docker-compose -f docker-compose.dev.yml up -d

# 2. 启动后端
cd backend/careld-server
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 3. 启动门店Web
cd frontend/store-web
npm install
npm run dev

# 4. 启动TV APK（Android Studio）
# Open tv-apk/ in Android Studio
# Click Run
```

---

## 6.6 代码审查

### 6.6.1 审查清单

| 检查项 | 说明 |
|--------|------|
| 功能实现 | 是否满足需求 |
| 代码规范 | 是否符合编码规范 |
| 单元测试 | 是否有单元测试覆盖 |
| 性能优化 | 是否存在性能隐患 |
| 安全漏洞 | 是否存在安全问题 |
| 日志记录 | 关键操作是否有日志 |
| 异常处理 | 异常处理是否完善 |

### 6.6.2 审查流程

```
1. 开发者提交PR
2. CI自动运行（编译、测试、检查）
3. 代码审查人Review
4. 发现问题 → 开发者修改 → 重新Review
5. 审查通过 → 合并到Develop分支
```

---

## 6.7 阶段交付物

1. **源代码仓库** - Git完整提交历史
2. **单元测试代码** - 覆盖率≥70%
3. **API文档** - Swagger自动生成
4. **构建产物** - Jar包/Dist/APK
5. **开发文档** - 本地开发指南
6. **部署脚本** - Docker/K8s配置
