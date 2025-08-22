#!/bin/bash

# 定义颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 定义命名空间
NAMESPACE="jiangyang-ai"

echo -e "${BLUE}JiangyangAI 统一 Kubernetes 部署脚本${NC}"
echo "=================================="

# 检查kubectl
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}错误: kubectl 未安装或不在PATH中${NC}"
    exit 1
fi

# 检查配置文件是否存在
if [ ! -f "jiangyang-ai-unified-deployment.yaml" ]; then
    echo -e "${YELLOW}统一配置文件不存在，正在生成...${NC}"
    
    if [ -f "generate-unified-k8s.sh" ]; then
        chmod +x generate-unified-k8s.sh
        ./generate-unified-k8s.sh
    elif [ -f "generate-unified-k8s.bat" ]; then
        generate-unified-k8s.bat
    else
        echo -e "${RED}错误: 找不到配置文件生成脚本${NC}"
        exit 1
    fi
fi

echo -e "${YELLOW}正在部署JiangyangAI服务到Kubernetes...${NC}"

# 部署所有服务
echo -e "${BLUE}应用统一配置文件...${NC}"
kubectl apply -f jiangyang-ai-unified-deployment.yaml

echo -e "${GREEN}部署完成！${NC}"
echo ""
echo -e "${YELLOW}查看服务状态:${NC}"
kubectl get all -n $NAMESPACE

echo ""
echo -e "${BLUE}服务访问地址:${NC}"
echo "网关服务: http://jiangyang-ai.local/gateway"
echo "BGAI服务: http://jiangyang-ai.local/bgai"
echo "聊天服务: http://jiangyang-ai.local/chat"
echo "消息服务: http://jiangyang-ai.local/messages"
echo "签名服务: http://jiangyang-ai.local/signature"
echo "深度搜索: http://jiangyang-ai.local/deepsearch"

echo ""
echo -e "${BLUE}查看日志命令:${NC}"
echo "kubectl logs -f <pod-name> -n $NAMESPACE"
echo ""
echo -e "${BLUE}删除部署命令:${NC}"
echo "kubectl delete -f jiangyang-ai-unified-deployment.yaml"
