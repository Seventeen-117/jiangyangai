#!/bin/bash

# Docker构建脚本
# 用于构建所有服务的Docker镜像

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目版本
VERSION="1.0.0-Final"
REGISTRY="jiangyang"

# 服务列表
SERVICES=(
    "base-service"
    "bgai-service"
    "chat-agent"
    "deepSearch-service"
    "gateway-service"
    "messages-service"
    "signature-service"
    "dubbo-api"
)

echo -e "${GREEN}开始构建Docker镜像...${NC}"

# 首先构建基础依赖
echo -e "${YELLOW}构建基础依赖...${NC}"
cd base-service
mvn clean package -DskipTests
docker build -t ${REGISTRY}/base-service:${VERSION} .
cd ..

# 构建其他服务
for service in "${SERVICES[@]}"; do
    if [ "$service" != "base-service" ]; then
        echo -e "${YELLOW}构建 ${service}...${NC}"
        cd $service
        
        # 检查是否有Dockerfile
        if [ -f "Dockerfile" ]; then
            # 构建项目
            mvn clean package -DskipTests
            
            # 构建Docker镜像
            docker build -t ${REGISTRY}/${service}:${VERSION} .
            echo -e "${GREEN}${service} 构建完成${NC}"
        else
            echo -e "${RED}${service} 没有找到Dockerfile，跳过${NC}"
        fi
        
        cd ..
    fi
done

# 构建Seata Server
echo -e "${YELLOW}构建 Seata Server...${NC}"
cd seata-server
if [ -f "Dockerfile" ]; then
    docker build -t ${REGISTRY}/seata-server:${VERSION} .
    echo -e "${GREEN}Seata Server 构建完成${NC}"
else
    echo -e "${RED}Seata Server 没有找到Dockerfile，跳过${NC}"
fi
cd ..

echo -e "${GREEN}所有Docker镜像构建完成！${NC}"

# 显示构建的镜像
echo -e "${YELLOW}构建的镜像列表：${NC}"
docker images | grep ${REGISTRY}
