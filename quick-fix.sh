#!/bin/bash

# 快速修复脚本 - 解决Docker镜像拉取问题

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Docker镜像拉取问题快速修复脚本${NC}"
echo ""

# 检查Docker是否运行
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Docker未运行，请先启动Docker${NC}"
    exit 1
fi

echo -e "${GREEN}Docker运行正常${NC}"

# 尝试拉取基础镜像
echo -e "${YELLOW}尝试拉取基础镜像...${NC}"

# 镜像源列表
MIRRORS=(
    "openjdk:17-jre-slim"
    "registry.cn-hangzhou.aliyuncs.com/library/openjdk:17-jre-slim"
    "registry.cn-beijing.aliyuncs.com/library/openjdk:17-jre-slim"
    "registry.cn-shanghai.aliyuncs.com/library/openjdk:17-jre-slim"
    "registry.cn-guangzhou.aliyuncs.com/library/openjdk:17-jre-slim"
    "ccr.ccs.tencentyun.com/library/openjdk:17-jre-slim"
    "docker.mirrors.ustc.edu.cn/library/openjdk:17-jre-slim"
    "hub-mirror.c.163.com/library/openjdk:17-jre-slim"
)

success=false

for mirror in "${MIRRORS[@]}"; do
    echo -e "${BLUE}尝试镜像源: $mirror${NC}"
    
    if timeout 120 docker pull "$mirror" 2>/dev/null; then
        echo -e "${GREEN}成功拉取镜像: $mirror${NC}"
        
        # 如果是备用镜像源，需要打标签
        if [[ "$mirror" != "openjdk:17-jre-slim" ]]; then
            echo -e "${BLUE}给镜像打标签...${NC}"
            docker tag "$mirror" "openjdk:17-jre-slim"
            echo -e "${GREEN}标签完成${NC}"
        fi
        
        success=true
        break
    else
        echo -e "${YELLOW}镜像源拉取失败: $mirror${NC}"
    fi
done

if [ "$success" = true ]; then
    echo ""
    echo -e "${GREEN}基础镜像拉取成功！现在可以运行构建脚本了${NC}"
    echo -e "${BLUE}运行命令: ./build-docker-optimized.sh tag${NC}"
else
    echo ""
    echo -e "${RED}所有镜像源都拉取失败${NC}"
    echo -e "${YELLOW}建议解决方案:${NC}"
    echo "1. 检查网络连接"
    echo "2. 配置Docker镜像源"
    echo "3. 使用VPN或代理"
    echo "4. 手动下载镜像文件"
    
    echo ""
    echo -e "${BLUE}配置Docker镜像源的方法:${NC}"
    echo "在 /etc/docker/daemon.json 中添加:"
    echo '{'
    echo '  "registry-mirrors": ['
    echo '    "https://registry.cn-hangzhou.aliyuncs.com",'
    echo '    "https://docker.mirrors.ustc.edu.cn",'
    echo '    "https://hub-mirror.c.163.com"'
    echo '  ]'
    echo '}'
    echo ""
    echo "然后重启Docker: systemctl restart docker"
fi
