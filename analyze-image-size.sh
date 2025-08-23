#!/bin/bash

# Docker镜像大小分析脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 分析镜像大小
analyze_image() {
    local image_name=$1
    local tag=${2:-latest}
    local full_image="${image_name}:${tag}"
    
    echo -e "${YELLOW}分析镜像: $full_image${NC}"
    
    # 检查镜像是否存在
    if ! docker image inspect "$full_image" >/dev/null 2>&1; then
        echo -e "${RED}镜像 $full_image 不存在${NC}"
        return
    fi
    
    # 获取镜像大小
    local size=$(docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | grep "$image_name" | grep "$tag" | awk '{print $3}')
    echo -e "${BLUE}镜像大小: $size${NC}"
    
    # 分析镜像层
    echo -e "${BLUE}镜像层分析:${NC}"
    docker history "$full_image" --format "table {{.CreatedBy}}\t{{.Size}}" | head -10
    
    # 创建临时容器来分析内容
    echo -e "${BLUE}镜像内容分析:${NC}"
    local container_id=$(docker create "$full_image")
    
    # 分析主要目录大小
    echo -e "${GREEN}主要目录大小:${NC}"
    docker exec "$container_id" du -sh /app /usr /opt 2>/dev/null || echo "无法访问容器内容"
    
    # 分析JAR文件大小
    echo -e "${GREEN}JAR文件大小:${NC}"
    docker exec "$container_id" find /app -name "*.jar" -exec du -h {} \; 2>/dev/null || echo "未找到JAR文件"
    
    # 清理临时容器
    docker rm "$container_id" >/dev/null 2>&1
    
    echo ""
}

# 显示帮助信息
show_help() {
    echo -e "${BLUE}Docker镜像大小分析脚本${NC}"
    echo ""
    echo "使用方法:"
    echo "  $0 [镜像名] [标签]"
    echo ""
    echo "示例:"
    echo "  $0 bgai-service latest"
    echo "  $0 chat-agent"
    echo ""
    echo "如果不指定参数，将分析所有本地镜像"
}

# 主函数
main() {
    if [ $# -eq 0 ]; then
        # 分析所有本地镜像
        echo -e "${GREEN}分析所有本地镜像...${NC}"
        docker images --format "{{.Repository}}:{{.Tag}}" | while read image; do
            if [[ $image != "<none>:<none>" ]]; then
                repo=$(echo "$image" | cut -d: -f1)
                tag=$(echo "$image" | cut -d: -f2)
                analyze_image "$repo" "$tag"
            fi
        done
    elif [ $# -eq 1 ] && [ "$1" = "help" ]; then
        show_help
    elif [ $# -ge 1 ]; then
        # 分析指定镜像
        image_name=$1
        tag=${2:-latest}
        analyze_image "$image_name" "$tag"
    else
        show_help
    fi
}

# 执行主函数
main "$@"
