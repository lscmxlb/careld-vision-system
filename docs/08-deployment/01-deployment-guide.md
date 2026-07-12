# 第八阶段：部署上线方案

## 8.1 部署架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              生产环境架构                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                              ┌─────────────┐                               │
│                              │   CDN加速   │                               │
│                              │ 静态资源分发 │                               │
│                              └──────┬──────┘                               │
│                                     │                                      │
│                              ┌──────┴──────┐                              │
│                              │  负载均衡    │                               │
│                              │   Nginx     │                               │
│                              └──────┬──────┘                               │
│                                     │                                      │
│       ┌─────────────────────────────┼─────────────────────────────┐       │
│       │                             │                             │       │
│       ▼                             ▼                             ▼       │
│  ┌─────────────┐            ┌─────────────┐            ┌─────────────┐   │
│  │  Web前端    │            │  API网关    │            │  Web前端    │   │
│  │  (运营/门店) │            │   Gateway   │            │  (家长端)   │   │
│  └─────────────┘            └──────┬──────┘            └─────────────┘   │
│                                    │                                      │
│       ┌────────────────────────────┼────────────────────────────┐       │
│       │                            │                            │       │
│       ▼                            ▼                            ▼       │
│  ┌─────────────┐            ┌─────────────┐            ┌─────────────┐   │
│  │  业务服务    │            │  业务服务    │            │  业务服务    │   │
│  │  (Pod 1)    │            │  (Pod 2)    │            │  (Pod N)    │   │
│  └─────────────┘            └─────────────┘            └─────────────┘   │
│       │                            │                            │       │
│       └────────────────────────────┼────────────────────────────┘       │
│                                    │                                      │
│       ┌────────────────────────────┼────────────────────────────┐       │
│       │                            │                            │       │
│       ▼                            ▼                            ▼       │
│  ┌─────────────┐            ┌─────────────┐            ┌─────────────┐   │
│  │  MySQL主库  │            │   Redis     │            │   MinIO     │   │
│  │  (Master)   │            │   Cluster   │            │   (OSS)     │   │
│  └─────────────┘            └─────────────┘            └─────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────┐                                                            │
│  │ MySQL从库  │                                                            │
│  │  (Slave)   │                                                            │
│  └─────────────┘                                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8.2 服务器配置

### 8.2.1 服务器清单

| 服务器 | 配置 | 数量 | 用途 |
|--------|------|------|------|
| 应用服务器 | 8核16G | 3台 | 运行微服务 |
| 数据库服务器 | 8核32G | 2台 | MySQL主从 |
| 缓存服务器 | 4核8G | 2台 | Redis集群 |
| 文件服务器 | 4核8G | 1台 | MinIO |
| 负载均衡 | 4核8G | 1台 | Nginx |

### 8.2.2 软件版本

| 软件 | 版本 | 说明 |
|------|------|------|
| CentOS | 7.9 | 操作系统 |
| Docker | 24.x | 容器化 |
| Kubernetes | 1.28 | 容器编排 |
| MySQL | 8.0.35 | 数据库 |
| Redis | 7.2 | 缓存 |
| Nginx | 1.24 | 负载均衡 |
| MinIO | RELEASE.2024 | 对象存储 |

---

## 8.3 容器化部署

### 8.3.1 Dockerfile示例

#### 后端服务

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 前端应用

```dockerfile
# frontend/Dockerfile
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# 运行阶段
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 8.3.2 Kubernetes配置

```yaml
# k8s/careld-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: careld-server
  namespace: careld
spec:
  replicas: 3
  selector:
    matchLabels:
      app: careld-server
  template:
    metadata:
      labels:
        app: careld-server
    spec:
      containers:
        - name: careld-server
          image: registry.careld.com/careld-server:v1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: DB_HOST
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: host
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: careld-server
  namespace: careld
spec:
  selector:
    app: careld-server
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

---

## 8.4 CI/CD流水线

### 8.4.1 GitLab CI配置

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - package
  - deploy

variables:
  DOCKER_REGISTRY: registry.careld.com
  IMAGE_TAG: ${CI_COMMIT_SHA:0:8}

# 后端构建
backend-build:
  stage: build
  image: eclipse-temurin:17-jdk
  script:
    - cd backend
    - ./mvnw clean compile
  only:
    changes:
      - backend/**/*

backend-test:
  stage: test
  image: eclipse-temurin:17-jdk
  script:
    - cd backend
    - ./mvnw test
  coverage: '/Total.*?([0-9]{1,3})%/'  
  only:
    changes:
      - backend/**/*

backend-package:
  stage: package
  image: docker:latest
  services:
    - docker:dind
  script:
    - cd backend
    - docker build -t $DOCKER_REGISTRY/careld-server:$IMAGE_TAG .
    - docker push $DOCKER_REGISTRY/careld-server:$IMAGE_TAG
  only:
    - develop
    - main

# 前端构建
frontend-build:
  stage: build
  image: node:18
  script:
    - cd frontend/store-web
    - npm ci
    - npm run build
  only:
    changes:
      - frontend/**/*

frontend-package:
  stage: package
  image: docker:latest
  services:
    - docker:dind
  script:
    - cd frontend/store-web
    - docker build -t $DOCKER_REGISTRY/careld-store-web:$IMAGE_TAG .
    - docker push $DOCKER_REGISTRY/careld-store-web:$IMAGE_TAG
  only:
    - develop
    - main

# 部署到开发环境
deploy-dev:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/careld-server 
        careld-server=$DOCKER_REGISTRY/careld-server:$IMAGE_TAG
        -n careld-dev
  environment:
    name: development
    url: https://dev.careld.com
  only:
    - develop

# 部署到生产环境
deploy-prod:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/careld-server 
        careld-server=$DOCKER_REGISTRY/careld-server:$IMAGE_TAG
        -n careld-prod
  environment:
    name: production
    url: https://careld.com
  when: manual
  only:
    - main
```

---

## 8.5 灰度发布

### 8.5.1 发布策略

```
灰度发布流程：

Phase 1: 内测发布（第1周）
├─ 范围：内部测试环境
├─ 用户：开发团队 + 产品经理
└─ 目标：核心功能验证

Phase 2: 试点门店（第2-3周）
├─ 范围：3家试点门店
├─ 用户：门店医护 + 总部运营
├─ 目标：真实环境验证
└─ 指标：无P0/P1缺陷

Phase 3: 扩大试点（第4-5周）
├─ 范围：20家门店
├─ 用户：更多门店
├─ 目标：并发性能验证
└─ 指标：系统稳定，无性能瓶颈

Phase 4: 全量发布（第6周）
├─ 范围：全国所有门店
├─ 用户：全部用户
└─ 目标：全面上线
```

### 8.5.2 蓝绿部署

```yaml
# 蓝绿部署配置
apiVersion: apps/v1
kind: Deployment
metadata:
  name: careld-server-blue
  namespace: careld
spec:
  replicas: 3
  selector:
    matchLabels:
      app: careld-server
      version: blue
  template:
    metadata:
      labels:
        app: careld-server
        version: blue
    spec:
      containers:
        - name: careld-server
          image: registry.careld.com/careld-server:v1.0.0
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: careld-server-green
  namespace: careld
spec:
  replicas: 0  # 初始为0
  selector:
    matchLabels:
      app: careld-server
      version: green
  template:
    metadata:
      labels:
        app: careld-server
        version: green
    spec:
      containers:
        - name: careld-server
          image: registry.careld.com/careld-server:v1.1.0
```

---

## 8.6 监控告警

### 8.6.1 监控指标

| 指标类型 | 指标名称 | 告警阈值 |
|---------|---------|---------|
| 系统 | CPU使用率 | > 80% |
| 系统 | 内存使用率 | > 85% |
| 系统 | 磁盘使用率 | > 90% |
| 应用 | 接口响应时间 | > 500ms |
| 应用 | 错误率 | > 1% |
| 应用 | QPS | 监控 |
| 数据库 | 连接数 | > 80% |
| 数据库 | 慢查询 | > 1s |
| 业务 | TV同步失败率 | > 5% |

### 8.6.2 告警配置

```yaml
# alertmanager-config.yaml
groups:
  - name: careld-alerts
    rules:
      - alert: HighCPUUsage
        expr: cpu_usage_percent > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "服务器 {{ $labels.instance }} CPU使用率超过80%"
      
      - alert: APIResponseSlow
        expr: http_request_duration_seconds > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API响应缓慢"
          description: "接口 {{ $labels.path }} 响应时间超过500ms"
      
      - alert: SyncFailureHigh
        expr: sync_failure_rate > 0.05
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "TV同步失败率过高"
          description: "TV同步失败率超过5%"
```

---

## 8.7 数据备份

### 8.7.1 备份策略

| 数据类型 | 备份方式 | 频率 | 保留期 |
|---------|---------|------|--------|
| MySQL全量 | 物理备份 | 每日 | 30天 |
| MySQL增量 | Binlog | 实时 | 7天 |
| Redis | RDB + AOF | 每小时 | 7天 |
| 文件存储 | 跨区域复制 | 实时 | 永久 |

### 8.7.2 备份脚本

```bash
#!/bin/bash
# mysql-backup.sh

BACKUP_DIR="/backup/mysql/$(date +%Y%m%d)"
DB_NAME="careld_prod"
RETENTION_DAYS=30

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
mysqldump -u backup_user -p'$PASSWORD' \
  --single-transaction \
  --routines \
  --triggers \
  $DB_NAME | gzip > $BACKUP_DIR/${DB_NAME}_$(date +%H%M%S).sql.gz

# 上传到OSS
ossutil cp -r $BACKUP_DIR oss://careld-backup/mysql/

# 清理本地旧备份
find /backup/mysql -type d -mtime +$RETENTION_DAYS -exec rm -rf {} \;

# 清理OSS旧备份
ossutil rm -rf oss://careld-backup/mysql/$(date -d "$RETENTION_DAYS days ago" +%Y%m%d)/

echo "Backup completed: $(date)"
```

---

## 8.8 回滚方案

### 8.8.1 回滚触发条件

- 生产环境出现P0级缺陷
- 系统可用性低于99%
- 数据完整性问题
- 安全漏洞

### 8.8.2 回滚流程

```bash
#!/bin/bash
# rollback.sh

VERSION=$1  # 回滚目标版本
SERVICE=$2  # 服务名

# 1. 暂停当前部署
kubectl rollout pause deployment/$SERVICE

# 2. 执行回滚
kubectl rollout undo deployment/$SERVICE --to-revision=$VERSION

# 3. 验证回滚
kubectl rollout status deployment/$SERVICE

# 4. 恢复流量
kubectl rollout resume deployment/$SERVICE

echo "Rollback completed to version $VERSION"
```

---

## 8.9 交付物清单

| 交付物 | 说明 |
|--------|------|
| 部署架构图 | 系统部署架构 |
| Docker镜像 | 各服务镜像 |
| K8s配置 | Deployment/Service/Ingress |
| CI/CD配置 | GitLab CI / Jenkins |
| 监控配置 | Prometheus/Grafana |
| 备份脚本 | 数据备份脚本 |
| 运维手册 | 日常运维指南 |
| 应急预案 | 故障处理手册 |
