#!/bin/bash
# Careld Vision System - 一键编译和测试脚本

set -e

echo "=========================================="
echo "Careld Vision System - 编译和测试脚本"
echo "=========================================="
echo ""

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "错误: Java 未安装，请先安装 JDK 17+"
    echo "  sudo apt-get install -y openjdk-17-jdk"
    exit 1
fi

# 检查 Maven 环境
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven 未安装，请先安装 Maven 3.9.x+"
    echo "  sudo apt-get install -y maven"
    exit 1
fi

echo "Java 版本: $(java -version 2>&1 | head -1)"
echo "Maven 版本: $(mvn -version 2>&1 | head -1)"
echo ""

# 编译后端
echo "=========================================="
echo "[1/4] 编译后端 (Backend)"
echo "=========================================="
cd backend
mvn clean compile -DskipTests
echo "后端编译成功!"
echo ""

# 运行后端测试
echo "=========================================="
echo "[2/4] 运行后端测试"
echo "=========================================="
mvn test
echo "后端测试完成!"
echo ""

cd ..

# 编译前端 - store-web
echo "=========================================="
echo "[3/4] 编译前端 - store-web"
echo "=========================================="
cd frontend/store-web
if [ ! -d "node_modules" ]; then
    npm install
fi
npm run build-only
echo "store-web 编译成功!"
echo ""

# 编译前端 - admin-web
echo "=========================================="
echo "[4/4] 编译前端 - admin-web"
echo "=========================================="
cd ../admin-web
if [ ! -d "node_modules" ]; then
    npm install
fi
npm run build-only
echo "admin-web 编译成功!"
echo ""

cd ../..

echo "=========================================="
echo "所有编译和测试任务完成!"
echo "=========================================="
