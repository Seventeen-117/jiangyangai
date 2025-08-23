#!/bin/bash

# Docker构建脚本
# 用于构建所有服务的Docker镜像并标签到阿里云镜像仓库

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目版本
VERSION="latest"
LOCAL_REGISTRY="jiangyang"
REMOTE_REGISTRY="registry.cn-shanghai.aliyuncs.com/bg-boot"

# 服务列表（对应jiangyangAI项目的服务名）
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

# 显示帮助信息
show_help() {
    echo -e "${BLUE}JiangyangAI Docker构建脚本${NC}"
    echo ""
    echo "使用方法:"
    echo "  $0 [选项]"
    echo ""
    echo "选项:"
    echo "  build      - 只构建本地镜像"
    echo "  tag        - 构建并标签到远程仓库"
    echo "  push       - 构建、标签并推送到远程仓库"
    echo "  clean      - 清理本地镜像"
    echo "  help       - 显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 build   # 只构建本地镜像"
    echo "  $0 tag     # 构建并标签"
    echo "  $0 push    # 构建、标签并推送"
}

# 构建单个服务的函数
build_service() {
    local service=$1
    local should_tag=${2:-false}
    local should_push=${3:-false}
    
    echo -e "${YELLOW}构建 ${service}...${NC}"
    cd $service
    
    # 检查是否有Dockerfile
    if [ ! -f "Dockerfile" ]; then
        echo -e "${RED}${service} 没有找到Dockerfile，跳过${NC}"
        cd ..
        return
    fi
    
    # 构建项目（如果有pom.xml）
    if [ -f "pom.xml" ]; then
        echo -e "${BLUE}  编译 ${service}...${NC}"
        if ! mvn clean package -DskipTests -q; then
            echo -e "${RED}  ${service} 编译失败${NC}"
            cd ..
            return 1
        fi
        
        # 检查JAR文件是否生成
        if [ ! -d "target" ]; then
            echo -e "${RED}  ${service} target目录不存在${NC}"
            cd ..
            return 1
        fi
        
        # 查找生成的JAR文件
        jar_file=$(find target -name "*.jar" -not -name "*sources.jar" -not -name "*javadoc.jar" | head -1)
        if [ -z "$jar_file" ]; then
            echo -e "${RED}  ${service} 没有找到生成的JAR文件${NC}"
            cd ..
            return 1
        fi
        
        echo -e "${GREEN}  ${service} 编译成功，生成JAR文件: $jar_file${NC}"
    fi
    
    # 构建Docker镜像（本地标签）
    echo -e "${BLUE}  构建Docker镜像 ${service}...${NC}"
    
    # 调试：显示当前目录和文件
    echo -e "${BLUE}  当前目录: $(pwd)${NC}"
    echo -e "${BLUE}  target目录内容:${NC}"
    ls -la target/ 2>/dev/null || echo "target目录不存在"
    
    # 检查.dockerignore文件
    if [ -f ".dockerignore" ]; then
        echo -e "${BLUE}  .dockerignore文件内容:${NC}"
        cat .dockerignore
    fi
    
    docker build -t ${service}:${VERSION} .
    
    if [ "$should_tag" = true ]; then
        # 标签到远程仓库
        echo -e "${BLUE}  标签 ${service} 到远程仓库...${NC}"
        docker tag ${service}:${VERSION} ${REMOTE_REGISTRY}/${service}:${VERSION}
        
        if [ "$should_push" = true ]; then
            # 推送到远程仓库
            echo -e "${BLUE}  推送 ${service} 到远程仓库...${NC}"
            docker push ${REMOTE_REGISTRY}/${service}:${VERSION}
        fi
    fi
    
    echo -e "${GREEN}${service} 处理完成${NC}"
    cd ..
}

# 清理镜像函数
clean_images() {
    echo -e "${YELLOW}清理本地镜像...${NC}"
    
    for service in "${SERVICES[@]}"; do
        # 清理本地镜像
        docker rmi ${service}:${VERSION} 2>/dev/null || true
        docker rmi ${REMOTE_REGISTRY}/${service}:${VERSION} 2>/dev/null || true
    done
    
    # 清理Seata Server镜像
    docker rmi seata-server:${VERSION} 2>/dev/null || true
    docker rmi ${REMOTE_REGISTRY}/seata-server:${VERSION} 2>/dev/null || true
    
    echo -e "${GREEN}镜像清理完成${NC}"
}

# 构建所有服务
build_all_services() {
    local should_tag=${1:-false}
    local should_push=${2:-false}
    
    echo -e "${GREEN}开始构建Docker镜像...${NC}"
    
    # 构建所有服务
    for service in "${SERVICES[@]}"; do
        if ! build_service $service $should_tag $should_push; then
            echo -e "${RED}构建失败，停止构建流程${NC}"
            return 1
        fi
    done
    
    # 构建Seata Server
    echo -e "${YELLOW}构建 Seata Server...${NC}"
    cd seata-server
    if [ -f "Dockerfile" ]; then
        echo -e "${BLUE}  构建Docker镜像 seata-server...${NC}"
        docker build -t seata-server:${VERSION} .
        
        if [ "$should_tag" = true ]; then
            echo -e "${BLUE}  标签 seata-server 到远程仓库...${NC}"
            docker tag seata-server:${VERSION} ${REMOTE_REGISTRY}/seata-server:${VERSION}
            
            if [ "$should_push" = true ]; then
                echo -e "${BLUE}  推送 seata-server 到远程仓库...${NC}"
                docker push ${REMOTE_REGISTRY}/seata-server:${VERSION}
            fi
        fi
        echo -e "${GREEN}Seata Server 处理完成${NC}"
    else
        echo -e "${RED}Seata Server 没有找到Dockerfile，跳过${NC}"
    fi
    cd ..
    
    echo -e "${GREEN}所有Docker镜像处理完成！${NC}"
    
    # 显示构建的镜像
    echo -e "${YELLOW}本地镜像列表：${NC}"
    docker images | grep -E "($(IFS=\|; echo "${SERVICES[*]}")|seata-server)" | head -20
    
    if [ "$should_tag" = true ]; then
        echo ""
        echo -e "${YELLOW}远程标签镜像列表：${NC}"
        docker images | grep ${REMOTE_REGISTRY} | head -20
    fi
}

# 主函数
main() {
    case "${1:-help}" in
        "build")
            build_all_services false false
            ;;
        "tag")
            build_all_services true false
            ;;
        "push")
            build_all_services true true
            ;;
        "clean")
            clean_images
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 执行主函数
main "$@"
