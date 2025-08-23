#!/bin/bash

# 简化的Docker构建测试脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Docker构建测试脚本${NC}"
echo ""

# 检查Docker是否运行
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Docker未运行，请先启动Docker${NC}"
    exit 1
fi

echo -e "${GREEN}Docker运行正常${NC}"

# 测试单个服务构建
test_service="bgai-service"

echo -e "${YELLOW}测试构建服务: $test_service${NC}"

# 检查服务目录
if [ ! -d "$test_service" ]; then
    echo -e "${RED}服务目录不存在: $test_service${NC}"
    exit 1
fi

cd "$test_service"

# 检查Dockerfile
if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}Dockerfile不存在${NC}"
    exit 1
fi

echo -e "${GREEN}Dockerfile存在${NC}"

# 检查JAR文件
if [ ! -d "target" ]; then
    echo -e "${YELLOW}target目录不存在，需要先编译${NC}"
    if [ -f "pom.xml" ]; then
        echo -e "${BLUE}开始编译...${NC}"
        mvn clean package -DskipTests -q
    else
        echo -e "${RED}没有找到pom.xml文件${NC}"
        exit 1
    fi
fi

# 查找JAR文件
jar_file=$(find target -name "*.jar" -not -name "*sources.jar" -not -name "*javadoc.jar" | head -1)
if [ -z "$jar_file" ]; then
    echo -e "${RED}没有找到JAR文件${NC}"
    exit 1
fi

echo -e "${GREEN}找到JAR文件: $jar_file${NC}"

# 检查基础镜像
if ! docker images | grep -q "openjdk.*17-jdk-slim"; then
    echo -e "${YELLOW}本地没有基础镜像，尝试拉取...${NC}"
    if ! docker pull openjdk:17-jdk-slim; then
        echo -e "${RED}无法拉取基础镜像${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}基础镜像检查通过${NC}"

# 显示构建上下文
echo -e "${BLUE}构建上下文:${NC}"
ls -la

# 尝试构建镜像
echo -e "${BLUE}开始构建镜像...${NC}"
if docker build -t test-build:latest .; then
    echo -e "${GREEN}构建成功！${NC}"
    
    # 显示构建的镜像
    echo -e "${BLUE}构建的镜像:${NC}"
    docker images | grep test-build
    
    # 清理测试镜像
    docker rmi test-build:latest
    echo -e "${GREEN}测试镜像已清理${NC}"
else
    echo -e "${RED}构建失败${NC}"
    exit 1
fi

cd ..

echo -e "${GREEN}测试完成！${NC}"
