# 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 复制pom文件和源代码
COPY pom.xml .
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests -q

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="devops@careld.com"
LABEL version="1.0.0"
LABEL description="Careld Vision Service - 视力检测管理服务"

# 创建应用目录
WORKDIR /app

# 安装必要的工具
RUN apk add --no-cache curl ca-certificates

# 从构建阶段复制jar文件
COPY --from=builder /build/target/careld-vision-service-*.jar app.jar

# 创建非root用户
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

# 设置文件权限
RUN chown -R appuser:appgroup /app

USER appuser

# 暴露端口
EXPOSE 8086

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8086/actuator/health || exit 1

# JVM参数
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
