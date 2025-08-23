#!/bin/bash

# 优化的Docker构建脚本
# 使用JRE基础镜像来减少镜像大小

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
    echo -e "${BLUE}JiangyangAI 优化Docker构建脚本${NC}"
    echo ""
    echo "使用方法:"
    echo "  $0 [选项]"
    echo ""
    echo "选项:"
    echo "  build      - 只构建本地镜像（优化版本）"
    echo "  tag        - 构建并标签到远程仓库"
    echo "  push       - 构建、标签并推送到远程仓库"
    echo "  clean      - 清理本地镜像"
    echo "  help       - 显示此帮助信息"
    echo ""
    echo "优化特性:"
    echo "  - 使用JRE基础镜像（更小）"
    echo "  - 优化.dockerignore文件"
    echo "  - 清理构建缓存"
    echo ""
    echo "示例:"
    echo "  $0 build   # 只构建本地镜像"
    echo "  $0 tag     # 构建并标签"
    echo "  $0 push    # 构建、标签并推送"
}

# 构建单个服务的函数（优化版本）
build_service_optimized() {
    local service=$1
    local should_tag=${2:-false}
    local should_push=${3:-false}
    
    echo -e "${YELLOW}构建 ${service}（优化版本）...${NC}"
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
        
        # 显示JAR文件大小
        jar_size=$(du -h "$jar_file" | cut -f1)
        echo -e "${BLUE}  JAR文件大小: $jar_size${NC}"
    fi
    
    # 构建Docker镜像（本地标签）
    echo -e "${BLUE}  构建Docker镜像 ${service}...${NC}"
    
    # 将服务名称转换为小写用于Docker镜像标签
    local service_lower=$(echo "$service" | tr '[:upper:]' '[:lower:]')
    
    # 使用Docker BuildKit来优化构建
    DOCKER_BUILDKIT=1 docker build -t ${service_lower}:${VERSION} .
    
    if [ "$should_tag" = true ]; then
        # 标签到远程仓库
        echo -e "${BLUE}  标签 ${service} 到远程仓库...${NC}"
        docker tag ${service_lower}:${VERSION} ${REMOTE_REGISTRY}/${service_lower}:${VERSION}
        
        if [ "$should_push" = true ]; then
            # 推送到远程仓库
            echo -e "${BLUE}  推送 ${service} 到远程仓库...${NC}"
            docker push ${REMOTE_REGISTRY}/${service_lower}:${VERSION}
        fi
    fi
    
    echo -e "${GREEN}${service} 处理完成${NC}"
    cd ..
}

# 清理镜像函数
clean_images() {
    echo -e "${YELLOW}清理本地镜像...${NC}"
    
    for service in "${SERVICES[@]}"; do
        # 将服务名称转换为小写用于Docker镜像标签
        local service_lower=$(echo "$service" | tr '[:upper:]' '[:lower:]')
        
        # 清理本地镜像
        docker rmi ${service_lower}:${VERSION} 2>/dev/null || true
        docker rmi ${REMOTE_REGISTRY}/${service_lower}:${VERSION} 2>/dev/null || true
    done
    
    # 清理Seata Server镜像
    docker rmi seata-server:${VERSION} 2>/dev/null || true
    docker rmi ${REMOTE_REGISTRY}/seata-server:${VERSION} 2>/dev/null || true
    
    echo -e "${GREEN}镜像清理完成${NC}"
}

# 构建所有服务（优化版本）
build_all_services_optimized() {
    local should_tag=${1:-false}
    local should_push=${2:-false}
    
    echo -e "${GREEN}开始构建优化的Docker镜像...${NC}"
    
    # 构建所有服务
    for service in "${SERVICES[@]}"; do
        if ! build_service_optimized $service $should_tag $should_push; then
            echo -e "${RED}构建失败，停止构建流程${NC}"
            return 1
        fi
    done
    
    echo -e "${GREEN}所有优化的Docker镜像处理完成！${NC}"
    
    # 显示构建的镜像
    echo -e "${YELLOW}本地镜像列表：${NC}"
    # 创建小写的服务名称列表用于grep
    local services_lower=""
    for service in "${SERVICES[@]}"; do
        services_lower="${services_lower}|$(echo "$service" | tr '[:upper:]' '[:lower:]')"
    done
    services_lower="${services_lower:1}" # 移除开头的|
    docker images | grep -E "(${services_lower}|seata-server)" | head -20
    
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
            build_all_services_optimized false false
            ;;
        "tag")
            build_all_services_optimized true false
            ;;
        "push")
            build_all_services_optimized true true
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
