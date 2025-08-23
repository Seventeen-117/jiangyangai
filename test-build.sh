#!/bin/bash

# 测试构建脚本
# 用于诊断Docker构建问题

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SERVICE="bgai-service"

echo -e "${YELLOW}开始测试 ${SERVICE} 构建...${NC}"

# 进入服务目录
cd $SERVICE

echo -e "${BLUE}当前目录: $(pwd)${NC}"

# 检查文件是否存在
echo -e "${BLUE}检查必要文件...${NC}"
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}pom.xml 不存在${NC}"
    exit 1
fi

if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}Dockerfile 不存在${NC}"
    exit 1
fi

echo -e "${GREEN}必要文件存在${NC}"

# 清理之前的构建
echo -e "${BLUE}清理之前的构建...${NC}"
if [ -d "target" ]; then
    rm -rf target
    echo -e "${GREEN}清理完成${NC}"
fi

# 编译项目
echo -e "${BLUE}编译项目...${NC}"
if ! mvn clean package -DskipTests; then
    echo -e "${RED}编译失败${NC}"
    exit 1
fi

echo -e "${GREEN}编译成功${NC}"

# 检查target目录
echo -e "${BLUE}检查target目录...${NC}"
if [ ! -d "target" ]; then
    echo -e "${RED}target目录不存在${NC}"
    exit 1
fi

echo -e "${GREEN}target目录存在${NC}"

# 列出target目录内容
echo -e "${BLUE}target目录内容:${NC}"
ls -la target/

# 查找JAR文件
echo -e "${BLUE}查找JAR文件...${NC}"
jar_files=$(find target -name "*.jar" -not -name "*sources.jar" -not -name "*javadoc.jar")
if [ -z "$jar_files" ]; then
    echo -e "${RED}没有找到JAR文件${NC}"
    exit 1
fi

echo -e "${GREEN}找到JAR文件:${NC}"
echo "$jar_files"

# 检查Dockerfile中的COPY命令
echo -e "${BLUE}检查Dockerfile中的COPY命令...${NC}"
grep -n "COPY.*target" Dockerfile || echo -e "${YELLOW}Dockerfile中没有找到COPY target命令${NC}"

# 构建Docker镜像
echo -e "${BLUE}构建Docker镜像...${NC}"
if ! docker build -t test-$SERVICE .; then
    echo -e "${RED}Docker构建失败${NC}"
    exit 1
fi

echo -e "${GREEN}Docker构建成功${NC}"

# 清理测试镜像
echo -e "${BLUE}清理测试镜像...${NC}"
docker rmi test-$SERVICE

echo -e "${GREEN}测试完成${NC}"
