#!/bin/bash

# 定义应用列表 - jiangyangAI项目子服务
APPS=(
  "gateway-service"
  "base-service"
  "bgai-service"
  "chat-agent"
  "deepSearch-service"
  "messages-service"
  "signature-service"
  "dubbo-api"
  "seata-server"
)

# 定义端口列表 - 对应每个服务的端口
PORTS=(
  8080  # gateway-service
  8087  # base-service
  8081  # bgai-service
  8082  # chat-agent
  8085  # deepSearch-service
  8083  # messages-service
  8084  # signature-service
  8086  # dubbo-api
  8091  # seata-server
)

# 定义镜像仓库前缀
REGISTRY="registry.cn-shanghai.aliyuncs.com/bg-boot"

# 定义命名空间
NAMESPACE="jiangyang-ai"

# 定义服务器地址
SERVER_HOST="8.133.246.113"

# 创建统一的Kubernetes部署文件
cat << EOF > "jiangyang-ai-unified-deployment.yaml"
# JiangyangAI 统一 Kubernetes 部署配置
# 包含所有子服务的 Deployment 和 Service 配置
# 使用已部署的服务器: ${SERVER_HOST}

---
# 命名空间配置
apiVersion: v1
kind: Namespace
metadata:
  name: ${NAMESPACE}
  labels:
    name: ${NAMESPACE}
    project: jiangyang-ai

---
# 配置映射
apiVersion: v1
kind: ConfigMap
metadata:
  name: jiangyang-ai-config
  namespace: ${NAMESPACE}
  labels:
    project: jiangyang-ai
data:
  # 通用配置
  SPRING_PROFILES_ACTIVE: "prod"
  NACOS_HOST: "${SERVER_HOST}"
  NACOS_PORT: "8848"
  TZ: "Asia/Shanghai"
  
  # 数据库配置
  MYSQL_HOST: "${SERVER_HOST}"
  MYSQL_PORT: "3306"
  MYSQL_DATABASE: "jiangyang_ai"
  MYSQL_USER: "jiangyang"
  MYSQL_PASSWORD: "jiangyang123"
  
  # Redis配置
  REDIS_HOST: "${SERVER_HOST}"
  REDIS_PORT: "6379"
  
  # Seata配置
  SEATA_HOST: "${SERVER_HOST}"
  SEATA_PORT: "8091"

---
# 密钥配置
apiVersion: v1
kind: Secret
metadata:
  name: jiangyang-ai-secret
  namespace: ${NAMESPACE}
  labels:
    project: jiangyang-ai
type: Opaque
data:
  # Base64编码的敏感信息
  mysql-root-password: cm9vdA==  # root
  mysql-password: amlhbmd5YW5nMTIz  # jiangyang123
  redis-password: ""  # 空密码

EOF

# 生成所有服务的 Deployment 和 Service 配置
for i in "${!APPS[@]}"; do
  APP_NAME="${APPS[$i]}"
  PORT="${PORTS[$i]}"

  cat << EOF >> "jiangyang-ai-unified-deployment.yaml"

---
# ${APP_NAME} Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${APP_NAME}-deployment
  namespace: ${NAMESPACE}
  labels:
    app: ${APP_NAME}
    project: jiangyang-ai
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${APP_NAME}
  template:
    metadata:
      labels:
        app: ${APP_NAME}
        project: jiangyang-ai
    spec:
      containers:
      - name: ${APP_NAME}-container
        image: ${REGISTRY}/${APP_NAME}:latest
        ports:
        - containerPort: ${PORT}
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: NACOS_HOST
          value: "${SERVER_HOST}"
        - name: NACOS_PORT
          value: "8848"
        - name: TZ
          value: "Asia/Shanghai"
EOF

  # 为需要Seata的服务添加Seata环境变量
  if [[ "$APP_NAME" == "bgai-service" || "$APP_NAME" == "messages-service" || "$APP_NAME" == "signature-service" ]]; then
    cat << EOF >> "jiangyang-ai-unified-deployment.yaml"
        - name: SEATA_HOST
          value: "${SERVER_HOST}"
        - name: SEATA_PORT
          value: "8091"
EOF
  fi

  cat << EOF >> "jiangyang-ai-unified-deployment.yaml"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: ${PORT}
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: ${PORT}
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
      restartPolicy: Always

---
# ${APP_NAME} Service
apiVersion: v1
kind: Service
metadata:
  name: ${APP_NAME}-service
  namespace: ${NAMESPACE}
  labels:
    app: ${APP_NAME}
    project: jiangyang-ai
spec:
  selector:
    app: ${APP_NAME}
  ports:
    - protocol: TCP
      port: ${PORT}
      targetPort: ${PORT}
      name: http
  type: ClusterIP
EOF
done

# 添加Ingress配置
cat << EOF >> "jiangyang-ai-unified-deployment.yaml"

---
# Ingress 配置
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: jiangyang-ai-ingress
  namespace: ${NAMESPACE}
  labels:
    project: jiangyang-ai
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  rules:
  - host: jiangyang-ai.local
    http:
      paths:
      - path: /gateway
        pathType: Prefix
        backend:
          service:
            name: gateway-service-service
            port:
              number: 8080
      - path: /bgai
        pathType: Prefix
        backend:
          service:
            name: bgai-service-service
            port:
              number: 8081
      - path: /chat
        pathType: Prefix
        backend:
          service:
            name: chat-agent-service
            port:
              number: 8082
      - path: /messages
        pathType: Prefix
        backend:
          service:
            name: messages-service-service
            port:
              number: 8083
      - path: /signature
        pathType: Prefix
        backend:
          service:
            name: signature-service-service
            port:
              number: 8084
      - path: /deepsearch
        pathType: Prefix
        backend:
          service:
            name: deepSearch-service-service
            port:
              number: 8085
EOF

echo "统一Kubernetes配置文件生成完成！"
echo "生成的文件: jiangyang-ai-unified-deployment.yaml"
echo "使用服务器地址: ${SERVER_HOST}"
echo ""
echo "使用方法："
echo "1. 部署所有服务："
echo "   kubectl apply -f jiangyang-ai-unified-deployment.yaml"
echo ""
echo "2. 删除所有服务："
echo "   kubectl delete -f jiangyang-ai-unified-deployment.yaml"
echo ""
echo "3. 查看部署状态："
echo "   kubectl get all -n ${NAMESPACE}"
echo ""
echo "包含的服务："
for i in "${!APPS[@]}"; do
  echo "  - ${APPS[$i]} (端口: ${PORTS[$i]})"
done
echo ""
echo "服务访问地址："
echo "  - 网关服务: http://jiangyang-ai.local/gateway"
echo "  - BGAI服务: http://jiangyang-ai.local/bgai"
echo "  - 聊天服务: http://jiangyang-ai.local/chat"
echo "  - 消息服务: http://jiangyang-ai.local/messages"
echo "  - 签名服务: http://jiangyang-ai.local/signature"
echo "  - 深度搜索: http://jiangyang-ai.local/deepsearch"
echo ""
echo "基础设施服务地址："
echo "  - Nacos: http://${SERVER_HOST}:8848/nacos"
echo "  - MySQL: ${SERVER_HOST}:3306"
echo "  - Redis: ${SERVER_HOST}:6379"
echo "  - Seata: ${SERVER_HOST}:8091"
