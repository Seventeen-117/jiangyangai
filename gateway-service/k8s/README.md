# 网关服务 Kubernetes 部署指南

## 概述

本文档描述了网关服务在 Kubernetes 环境中的部署配置，实现了与云原生基础设施的联合设计。

## 架构设计

### 1. 多层架构
```
Internet
    ↓
Load Balancer (AWS ALB/NLB)
    ↓
Kubernetes Ingress Controller (Nginx)
    ↓
Gateway Service (Spring Cloud Gateway)
    ↓
Microservices (bgai, signature-service)
```

### 2. 核心组件

#### Ingress Controller
- **功能**: 外部流量入口，SSL终止，路由分发
- **配置**: Nginx Ingress Controller
- **特性**: 
  - SSL/TLS 终止
  - 限流控制
  - CORS 支持
  - 灰度发布
  - 链路追踪

#### Gateway Service
- **功能**: 内部网关，服务聚合，安全控制
- **特性**:
  - JWT 认证
  - 权限控制
  - 限流熔断
  - 请求转换
  - 日志监控

#### Service Mesh (可选)
- **功能**: 服务间通信，流量管理
- **推荐**: Istio 或 Linkerd

## 部署配置

### 1. 命名空间
```bash
kubectl create namespace jiangyangai
```

### 2. RBAC 权限
```bash
kubectl apply -f k8s/rbac.yaml
```

### 3. 配置管理
```bash
kubectl apply -f k8s/config.yaml
```

### 4. 服务部署
```bash
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/deployment.yaml
```

### 5. 网络策略
```bash
kubectl apply -f k8s/network-policy.yaml
```

### 6. 自动扩缩容
```bash
kubectl apply -f k8s/hpa.yaml
```

### 7. 入口配置
```bash
kubectl apply -f k8s/ingress.yaml
```

## 一键部署

使用部署脚本进行一键部署：

```bash
# 生产环境部署
./k8s/deploy.sh latest canary production

# 开发环境部署
./k8s/deploy.sh dev dev development
```

## 灰度发布

### 1. 启用灰度发布
```bash
kubectl patch ingress gateway-ingress -n jiangyangai \
  -p '{"metadata":{"annotations":{"nginx.ingress.kubernetes.io/canary":"true","nginx.ingress.kubernetes.io/canary-weight":"10"}}}'
```

### 2. 更新灰度权重
```bash
kubectl patch ingress gateway-ingress -n jiangyangai \
  -p '{"metadata":{"annotations":{"nginx.ingress.kubernetes.io/canary-weight":"20"}}}'
```

### 3. 禁用灰度发布
```bash
kubectl patch ingress gateway-ingress -n jiangyangai \
  -p '{"metadata":{"annotations":{"nginx.ingress.kubernetes.io/canary":"false"}}}'
```

## 监控和日志

### 1. 健康检查
```bash
# 查看 Pod 状态
kubectl get pods -n jiangyangai -l app=gateway-service

# 查看服务端点
kubectl get endpoints -n jiangyangai gateway-service

# 查看健康状态
kubectl logs -f deployment/gateway-service -n jiangyangai
```

### 2. 指标监控
```bash
# 端口转发到 Prometheus 指标
kubectl port-forward svc/gateway-service 9090:9090 -n jiangyangai

# 访问指标端点
curl http://localhost:9090/actuator/prometheus
```

### 3. 日志收集
```bash
# 查看应用日志
kubectl logs -f deployment/gateway-service -n jiangyangai

# 查看 Ingress 日志
kubectl logs -f deployment/nginx-ingress-controller -n ingress-nginx
```

## 安全配置

### 1. 网络安全
- **NetworkPolicy**: 控制 Pod 间通信
- **Ingress**: 外部访问控制
- **Service**: 内部服务发现

### 2. 认证授权
- **JWT 认证**: 统一身份验证
- **RBAC**: Kubernetes 权限控制
- **ServiceAccount**: 服务身份管理

### 3. 数据安全
- **TLS 终止**: Ingress 层 SSL 终止
- **敏感信息**: 使用 Secret 管理
- **配置管理**: 使用 ConfigMap 管理

## 高可用设计

### 1. 多副本部署
```yaml
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

### 2. 自动扩缩容
```yaml
spec:
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 3. 健康检查
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 5
```

## 性能优化

### 1. 资源限制
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

### 2. 亲和性配置
```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - gateway-service
        topologyKey: kubernetes.io/hostname
```

### 3. 缓存策略
- **Redis**: 分布式缓存
- **本地缓存**: Caffeine
- **CDN**: 静态资源缓存

## 故障排查

### 1. 常见问题

#### Pod 启动失败
```bash
# 查看 Pod 状态
kubectl describe pod <pod-name> -n jiangyangai

# 查看容器日志
kubectl logs <pod-name> -n jiangyangai
```

#### 服务无法访问
```bash
# 检查 Service 配置
kubectl get service gateway-service -n jiangyangai -o yaml

# 检查 Endpoints
kubectl get endpoints gateway-service -n jiangyangai

# 检查 Ingress 配置
kubectl describe ingress gateway-ingress -n jiangyangai
```

#### 网络连接问题
```bash
# 检查 NetworkPolicy
kubectl get networkpolicy -n jiangyangai

# 测试网络连接
kubectl exec -it <pod-name> -n jiangyangai -- curl <service-url>
```

### 2. 调试工具

#### 进入容器调试
```bash
kubectl exec -it <pod-name> -n jiangyangai -- /bin/bash
```

#### 端口转发
```bash
kubectl port-forward svc/gateway-service 8080:8080 -n jiangyangai
```

#### 查看事件
```bash
kubectl get events -n jiangyangai --sort-by='.lastTimestamp'
```

## 最佳实践

### 1. 部署策略
- **蓝绿部署**: 零停机时间
- **滚动更新**: 渐进式更新
- **灰度发布**: 风险可控

### 2. 监控告警
- **Prometheus**: 指标收集
- **Grafana**: 可视化面板
- **AlertManager**: 告警通知

### 3. 日志管理
- **ELK Stack**: 日志收集分析
- **Fluentd**: 日志转发
- **Kibana**: 日志可视化

### 4. 安全加固
- **Pod Security Standards**: 安全标准
- **Network Policies**: 网络隔离
- **RBAC**: 权限最小化

## 扩展功能

### 1. 服务网格
```bash
# 安装 Istio
istioctl install --set profile=demo

# 启用自动注入
kubectl label namespace jiangyangai istio-injection=enabled
```

### 2. 多集群部署
```bash
# 使用 Karmada 或 KubeFed
# 实现跨集群服务发现和流量管理
```

### 3. 云原生存储
```bash
# 使用 CSI 驱动
# 支持动态存储卷
```

## 总结

通过以上配置，网关服务实现了与 Kubernetes 的深度集成，具备：

1. **高可用性**: 多副本部署，自动扩缩容
2. **安全性**: 网络安全策略，认证授权
3. **可观测性**: 监控日志，链路追踪
4. **可扩展性**: 水平扩展，灰度发布
5. **云原生**: 容器化部署，声明式配置

这种架构设计确保了网关服务在云原生环境中的稳定运行和高效管理。 