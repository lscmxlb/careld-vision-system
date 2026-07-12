# Careld视力养护系统测试项目

## 项目概述

本项目为Careld可尔欧得视力养护服务信息系统的完整测试套件，覆盖四端（运营Web、门店Web、家长Web、安卓TV APK）的全面测试。

## 目录结构

```
tests/
├── README.md                          # 项目说明
├── test-cases/                        # 测试用例文档
│   ├── unit-test-cases.md            # 单元测试用例
│   ├── integration-test-cases.md     # 集成测试用例
│   ├── e2e-test-cases.md             # E2E测试用例
│   └── specialized-test-cases.md     # 专项测试用例
├── unit-tests/                        # 单元测试代码
│   ├── backend/                       # 后端单元测试
│   ├── frontend/                      # 前端单元测试
│   └── tv/                            # TV端单元测试
├── integration-tests/               # 集成测试代码
│   ├── api/                           # API接口测试
│   └── business/                      # 业务流程测试
├── e2e-tests/                         # E2E测试代码
│   ├── workflow/                      # 完整业务流程
│   └── cross-platform/                # 跨端联调测试
├── specialized-tests/                 # 专项测试
│   ├── offline/                       # TV离线缓存测试
│   ├── performance/                   # 性能测试
│   ├── compatibility/                 # 兼容性测试
│   └── security/                      # 安全测试
├── scripts/                           # 自动化测试脚本
│   ├── python/                        # Python API测试脚本
│   ├── playwright/                    # Playwright UI测试脚本
│   └── jmeter/                        # JMeter性能测试脚本
└── templates/                         # 测试模板
    ├── test-report-template.md       # 测试报告模板
    └── bug-report-template.md        # Bug报告模板
```

## 测试覆盖范围

### 1. 单元测试 (70%)
- 后端服务单元测试
- 前端组件单元测试
- TV端单元测试

### 2. 集成测试 (20%)
- API接口集成测试
- 数据库集成测试
- 服务间集成测试

### 3. E2E测试 (10%)
- 完整业务流程测试
- 跨端联调测试

### 4. 专项测试
- TV离线缓存测试
- 并发压力测试
- 兼容性测试
- 安全测试

## 执行命令

### 后端单元测试
```bash
cd unit-tests/backend
./gradlew test
```

### 前端单元测试
```bash
cd unit-tests/frontend
npm run test
```

### API集成测试
```bash
cd scripts/python
pytest test_api_integration.py
```

### E2E测试
```bash
cd scripts/playwright
npx playwright test
```

### 性能测试
```bash
cd scripts/jmeter
jmeter -n -t careld-performance-test.jmx
```

## 测试环境

- **测试环境**: https://test.careld.com
- **预发布环境**: https://staging.careld.com
- **生产环境**: https://api.careld.com

## 联系方式

- 测试负责人: 测试工程师
- 项目地址: /root/.openclaw/workspace/projects/careld-vision-system/
