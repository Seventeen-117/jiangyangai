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

# 基础镜像配置（支持国内镜像源）
BASE_IMAGE="openjdk:17-jdk-slim"
FALLBACK_IMAGES=(
    "registry.cn-hangzhou.aliyuncs.com/library/openjdk:17-jdk-slim"
    "registry.cn-beijing.aliyuncs.com/library/openjdk:17-jdk-slim"
    "registry.cn-shanghai.aliyuncs.com/library/openjdk:17-jdk-slim"
    "registry.cn-guangzhou.aliyuncs.com/library/openjdk:17-jdk-slim"
    "ccr.ccs.tencentyun.com/library/openjdk:17-jdk-slim"
    "docker.mirrors.ustc.edu.cn/library/openjdk:17-jdk-slim"
    "hub-mirror.c.163.com/library/openjdk:17-jdk-slim"
)

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
    echo "  offline    - 离线构建模式（跳过镜像拉取）"
    echo "  offline-tag - 离线构建并标签到远程仓库"
    echo "  offline-push - 离线构建、标签并推送到远程仓库"
    echo "  clean      - 清理本地镜像"
    echo "  network    - 检查网络连接状态"
    echo "  help       - 显示此帮助信息"
    echo ""
    echo "优化特性:"
    echo "  - 使用JRE基础镜像（更小）"
    echo "  - 优化.dockerignore文件"
    echo "  - 清理构建缓存"
    echo "  - 支持多个镜像源（解决网络问题）"
    echo ""
    echo "示例:"
    echo "  $0 build   # 只构建本地镜像"
    echo "  $0 tag     # 构建并标签"
    echo "  $0 push    # 构建、标签并推送"
    echo "  $0 network # 检查网络连接"
}

# 构建单个服务的函数（离线模式）
build_service_offline() {
    local service=$1
    local should_tag=${2:-false}
    local should_push=${3:-false}
    
    echo -e "${YELLOW}构建 ${service}（离线模式）...${NC}"
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
    
    # 检查本地是否有基础镜像
    if ! docker images | grep -q "openjdk.*17-jdk-slim"; then
        echo -e "${RED}  本地没有基础镜像 openjdk:17-jdk-slim${NC}"
        echo -e "${YELLOW}  请先手动拉取基础镜像:${NC}"
        echo -e "${BLUE}    docker pull openjdk:17-jdk-slim${NC}"
        cd ..
        return 1
    fi
    
    # 构建Docker镜像（本地标签）
    echo -e "${BLUE}  构建Docker镜像 ${service}...${NC}"
    
    # 将服务名称转换为小写用于Docker镜像标签
    local service_lower=$(echo "$service" | tr '[:upper:]' '[:lower:]')
    
    # 使用Docker BuildKit来优化构建
    DOCKER_BUILDKIT=1 docker build --network=host --timeout=600 -t ${service_lower}:${VERSION} .
    
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
    
    # 拉取基础镜像（支持多个镜像源）
    echo -e "${BLUE}  拉取基础镜像...${NC}"
    local image_pulled=false
    
    # 首先检查本地是否已有镜像
    if docker images | grep -q "openjdk.*17-jdk-slim"; then
        echo -e "${GREEN}  本地已有基础镜像，跳过拉取${NC}"
        image_pulled=true
    else
        # 尝试官方镜像（增加超时时间）
        echo -e "${BLUE}  尝试拉取官方镜像...${NC}"
        if timeout 60 docker pull ${BASE_IMAGE} 2>/dev/null; then
            echo -e "${GREEN}  成功拉取官方镜像: ${BASE_IMAGE}${NC}"
            image_pulled=true
        else
            echo -e "${YELLOW}  官方镜像拉取失败，尝试国内镜像源...${NC}"
            
            # 尝试备用镜像源（增加超时时间）
            for fallback_image in "${FALLBACK_IMAGES[@]}"; do
                echo -e "${BLUE}  尝试镜像源: $fallback_image${NC}"
                if timeout 60 docker pull "$fallback_image" 2>/dev/null; then
                    echo -e "${GREEN}  成功拉取镜像: $fallback_image${NC}"
                    # 给镜像打标签，使其与Dockerfile中的名称一致
                    docker tag "$fallback_image" "${BASE_IMAGE}"
                    image_pulled=true
                    break
                else
                    echo -e "${YELLOW}  镜像源拉取失败: $fallback_image${NC}"
                fi
            done
        fi
    fi
    
    if [ "$image_pulled" = false ]; then
        echo -e "${RED}  无法拉取任何基础镜像，请检查网络连接或手动拉取${NC}"
        echo -e "${YELLOW}  可以尝试以下命令手动拉取:${NC}"
        echo -e "${BLUE}    docker pull openjdk:17-jdk-slim${NC}"
        echo -e "${BLUE}    或者使用国内镜像源:${NC}"
        for fallback_image in "${FALLBACK_IMAGES[@]}"; do
            echo -e "${BLUE}    docker pull $fallback_image${NC}"
        done
        echo -e "${YELLOW}  拉取成功后重新运行构建脚本${NC}"
        echo -e "${YELLOW}  或者尝试使用离线构建模式（需要先手动拉取镜像）${NC}"
        cd ..
        return 1
    fi
    
    echo -e "${BLUE}  构建Docker镜像...${NC}"
    DOCKER_BUILDKIT=1 docker build --network=host --timeout=600 -t ${service_lower}:${VERSION} .
    
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

# 网络检查函数
check_network() {
    echo -e "${YELLOW}检查网络连接状态...${NC}"
    
    # 检查Docker是否运行
    if ! docker info >/dev/null 2>&1; then
        echo -e "${RED}Docker未运行或无法连接${NC}"
        return 1
    fi
    
    echo -e "${GREEN}Docker运行正常${NC}"
    
    # 测试官方镜像源
    echo -e "${BLUE}测试官方镜像源...${NC}"
    if timeout 10 docker pull hello-world:latest >/dev/null 2>&1; then
        echo -e "${GREEN}官方镜像源连接正常${NC}"
    else
        echo -e "${YELLOW}官方镜像源连接较慢或失败${NC}"
    fi
    
    # 测试国内镜像源
    echo -e "${BLUE}测试国内镜像源...${NC}"
    for fallback_image in "${FALLBACK_IMAGES[@]}"; do
        echo -e "${BLUE}测试: $fallback_image${NC}"
        if timeout 10 docker pull hello-world:latest >/dev/null 2>&1; then
            echo -e "${GREEN}镜像源连接正常: $fallback_image${NC}"
        else
            echo -e "${YELLOW}镜像源连接较慢: $fallback_image${NC}"
        fi
    done
    
    # 清理测试镜像
    docker rmi hello-world:latest >/dev/null 2>&1 || true
    
    echo -e "${GREEN}网络检查完成${NC}"
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

# 构建所有服务（离线模式）
build_all_services_offline() {
    local should_tag=${1:-false}
    local should_push=${2:-false}
    
    echo -e "${GREEN}开始构建Docker镜像（离线模式）...${NC}"
    
    # 构建所有服务
    for service in "${SERVICES[@]}"; do
        if ! build_service_offline $service $should_tag $should_push; then
            echo -e "${RED}构建失败，停止构建流程${NC}"
            return 1
        fi
    done
    
    echo -e "${GREEN}所有Docker镜像处理完成！${NC}"
    
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
        "offline")
            build_all_services_offline false false
            ;;
        "offline-tag")
            build_all_services_offline true false
            ;;
        "offline-push")
            build_all_services_offline true true
            ;;
        "clean")
            clean_images
            ;;
        "network")
            check_network
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 执行主函数
main "$@"
