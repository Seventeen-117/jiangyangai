# Gateway Service

网关服务 - 统一负载均衡和路由

## 概述

Gateway Service 是一个基于 Spring Cloud Gateway 的微服务网关，提供统一的路由、负载均衡、过滤和监控功能。

## 主要功能

### 1. 统一路由
- **静态路由**: 预定义的基础路由规则
- **动态路由**: 支持运行时动态添加、修改、删除路由
- **负载均衡**: 自动负载均衡到后端服务

### 2. 动态路由管理
- **REST API**: 提供完整的动态路由管理接口
- **Nacos集成**: 支持从Nacos配置中心加载路由配置
- **实时更新**: 路由配置变更实时生效
- **持久化**: 路由配置自动保存到Nacos

### 3. 请求过滤
- **全局日志记录**: 记录所有请求和响应信息
- **请求ID追踪**: 为每个请求生成唯一ID
- **异常统一处理**: 统一处理网关异常
- **请求头添加**: 自动添加网关标识头

### 4. 监控端点
- 健康检查: `/actuator/health`
- 应用信息: `/actuator/info`
- 指标信息: `/actuator/metrics`
- 网关路由: `/actuator/gateway`

### 5. Resilience4j 熔断器集成
- **默认熔断器配置**: 滑动窗口大小10，失败率阈值50%
- **服务特定熔断器**: signature-service、bgai-service、chat-api、auth-api等
- **自动状态切换**: CLOSED、OPEN、HALF_OPEN状态自动切换
- **回退处理**: 服务不可用时返回友好的错误信息

### 6. 限流器配置
- **全局限流**: 100请求/秒
- **服务特定限流**: 签名服务200请求/秒，聊天API 50请求/秒
- **Redis限流**: 基于Redis的分布式限流
- **突发流量处理**: 支持突发流量配置

### 7. 重试机制
- **最大重试次数**: 3次
- **指数退避**: 启用指数退避策略
- **异常类型**: 只对特定异常进行重试
- **超时控制**: 可配置的重试超时时间

### 8. Nacos白名单配置
- **动态配置**: 白名单路径从Nacos配置中心读取
- **分类管理**: 跳过认证、Token验证、签名验证等分类
- **热更新**: 配置修改后无需重启服务
- **环境隔离**: 不同环境使用不同配置文件

### 1. 统一路由
- **静态路由**: 预定义的基础路由规则
- **动态路由**: 支持运行时动态添加、修改、删除路由
- **负载均衡**: 自动负载均衡到后端服务

### 2. 动态路由管理
- **REST API**: 提供完整的动态路由管理接口
- **Nacos集成**: 支持从Nacos配置中心加载路由配置
- **实时更新**: 路由配置变更实时生效
- **持久化**: 路由配置自动保存到Nacos

### 3. 请求过滤
- **全局日志记录**: 记录所有请求和响应信息
- **请求ID追踪**: 为每个请求生成唯一ID
- **异常统一处理**: 统一处理网关异常
- **请求头添加**: 自动添加网关标识头

### 4. 监控端点
- 健康检查: `/actuator/health`
- 应用信息: `/actuator/info`
- 指标信息: `/actuator/metrics`
- 网关路由: `/actuator/gateway`

### 5. Resilience4j 熔断器集成
- **默认熔断器配置**: 滑动窗口大小10，失败率阈值50%
- **服务特定熔断器**: signature-service、bgai-service、chat-api、auth-api等
- **自动状态切换**: CLOSED、OPEN、HALF_OPEN状态自动切换
- **回退处理**: 服务不可用时返回友好的错误信息

### 6. 限流器配置
- **全局限流**: 100请求/秒
- **服务特定限流**: 签名服务200请求/秒，聊天API 50请求/秒
- **Redis限流**: 基于Redis的分布式限流
- **突发流量处理**: 支持突发流量配置

### 7. 重试机制
- **最大重试次数**: 3次
- **指数退避**: 启用指数退避策略
- **异常类型**: 只对特定异常进行重试
- **超时控制**: 可配置的重试超时时间

### 8. Nacos白名单配置
- **动态配置**: 白名单路径从Nacos配置中心读取
- **分类管理**: 跳过认证、Token验证、签名验证等分类
- **热更新**: 配置修改后无需重启服务
- **环境隔离**: 不同环境使用不同配置文件

## 技术栈

- **Spring Cloud Gateway**: 网关框架
- **Spring Cloud LoadBalancer**: 负载均衡
- **Spring Cloud Alibaba Nacos**: 服务发现和配置中心
- **Spring Boot Actuator**: 监控端点
- **WebFlux**: 响应式编程
- **Resilience4j**: 熔断器、限流器、重试机制

## 网关过滤器执行顺序

### 过滤器链执行顺序

网关服务中的过滤器按照以下顺序执行：

#### 1. 认证过滤器 (AuthenticationFilter)
- **执行顺序**: -100
- **功能**: JWT认证，统一身份验证
- **作用**: 验证用户身份，阻止未授权请求

#### 2. 权限控制过滤器 (AuthorizationFilter)
- **执行顺序**: -90
- **功能**: 基于角色的权限控制
- **作用**: 根据用户角色限制访问特定API

#### 3. 限流过滤器 (RateLimitFilter)
- **执行顺序**: -80
- **功能**: 流量控制和配额管理
- **作用**: 防止突发流量打垮服务

#### 4. 熔断过滤器 (CircuitBreakerFilter)
- **执行顺序**: -70
- **功能**: 熔断降级功能
- **作用**: 在服务故障时快速失败，避免雪崩效应

#### 5. 日志过滤器 (LoggingFilter)
- **执行顺序**: -60
- **功能**: 记录访问日志
- **作用**: 记录所有请求的URL、状态码、延迟等信息

#### 6. 请求转换过滤器 (RequestTransformFilter)
- **执行顺序**: -50
- **功能**: Header管理和请求转换
- **作用**: 添加/删除请求头，转换请求格式

#### 7. 防御性策略过滤器 (DefensiveFilter)
- **执行顺序**: -40
- **功能**: 防爬虫、防重放攻击、请求体校验
- **作用**: 识别异常流量模式，防止恶意攻击

#### 8. Token验证过滤器 (TokenValidationFilter)
- **执行顺序**: -30
- **功能**: 验证token有效性，转发用户信息
- **作用**: 验证通过时添加用户信息到Header，验证不通过仍转发给服务

#### 9. 路由过滤器 (Spring Cloud Gateway内置)
- **执行顺序**: 0
- **功能**: 路由转发
- **作用**: 将请求转发到目标服务

## Resilience4j 功能增强

### 1. 熔断器配置

#### 默认熔断器配置
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 5000
        slidingWindowSize: 10
        slidingWindowType: COUNT_BASED
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

#### 服务特定熔断器
- **signature-service-breaker**: 签名服务熔断器
- **bgai-service-breaker**: bgai服务熔断器
- **default-service-breaker**: 默认服务熔断器
- **chat-api-breaker**: 聊天API熔断器
- **auth-api-breaker**: 认证API熔断器

### 2. 限流器配置

#### 全局限流器
- **默认限流**: 100请求/秒
- **签名服务限流**: 200请求/秒
- **bgai服务限流**: 100请求/秒
- **聊天API限流**: 50请求/秒
- **认证API限流**: 300请求/秒

### 3. 重试机制

#### 重试配置
- **最大重试次数**: 3次
- **重试间隔**: 1秒
- **指数退避**: 启用
- **退避倍数**: 2

#### 重试异常类型
- `java.io.IOException`
- `java.util.concurrent.TimeoutException`
- `org.springframework.web.client.ResourceAccessException`

### 4. 路由增强

#### 路由配置示例
```yaml
# 签名验证服务路由 - 最高优先级
- id: signature-service
  uri: http://localhost:8689
  order: -1000
  predicates:
    - Path=/api/signature/**,/api/keys/**,/api/auth/**
  filters:
    - AddRequestHeader=X-Gateway-Source, gateway-service
    - AddResponseHeader=X-Gateway-Response, true
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 100
        redis-rate-limiter.burstCapacity: 200
    - name: CircuitBreaker
      args:
        name: signature-service-breaker
        fallbackUri: forward:/fallback/signature-service
```

#### 熔断器回退处理
- **签名服务回退**: `/fallback/signature-service`
- **bgai服务回退**: `/fallback/bgai-service`
- **默认服务回退**: `/fallback/default`
- **聊天API回退**: `/fallback/chat-api`
- **认证API回退**: `/fallback/auth-api`

## Nacos白名单配置

### 配置结构

#### 1. 跳过认证的路径 (skip-auth-paths)
这些路径不需要进行任何认证检查，包括：
- JWT认证
- Token验证
- 权限检查
- 限流检查
- 熔断器检查
- 防御性检查

#### 2. 跳过Token验证的路径 (skip-token-validation-paths)
这些路径不需要进行Token验证，但仍可能需要进行其他检查。

#### 3. 跳过签名验证的路径 (skip-signature-validation-paths)
这些路径不需要进行签名验证，但仍可能需要进行其他检查。

### Nacos配置示例

#### 配置ID
- `gateway-service-local.yml` (本地环境)
- `gateway-service-dev.yml` (开发环境)
- `gateway-service-prod.yml` (生产环境)

#### 命名空间
- `public` (默认)

#### 分组
- `DEFAULT_GROUP` (默认)

### 白名单路径说明

#### 系统路径
- `/actuator/**` - Spring Boot管理端点
- `/health` - 健康检查
- `/metrics` - 监控指标
- `/info` - 应用信息
- `/v3/api-docs/**` - OpenAPI文档
- `/swagger-ui/**` - Swagger UI界面

#### 业务路径
- `/api/keys/**` - API密钥管理接口
- `/api/signature/**` - 签名服务接口
- `/api/auth/**` - 认证服务接口
- `/api/public/**` - 公开接口
- `/public/**` - 静态资源

## 监控端点

### 管理端点
- **健康检查**: `/actuator/health`
- **网关信息**: `/actuator/gateway`
- **熔断器状态**: `/actuator/circuitbreakers`
- **重试状态**: `/actuator/retries`
- **限流器状态**: `/actuator/ratelimiters`

### 自定义监控端点
- **网关状态**: `/api/monitoring/status`
- **熔断器列表**: `/api/monitoring/circuit-breakers`
- **限流器列表**: `/api/monitoring/rate-limiters`
- **重试器列表**: `/api/monitoring/retries`
- **特定熔断器**: `/api/monitoring/circuit-breakers/{name}`

## Resilience4j 功能增强

### 1. 熔断器配置

#### 默认熔断器配置
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 5000
        slidingWindowSize: 10
        slidingWindowType: COUNT_BASED
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

#### 服务特定熔断器
- **signature-service-breaker**: 签名服务熔断器
- **bgai-service-breaker**: bgai服务熔断器
- **default-service-breaker**: 默认服务熔断器
- **chat-api-breaker**: 聊天API熔断器
- **auth-api-breaker**: 认证API熔断器

### 2. 限流器配置

#### 全局限流器
- **默认限流**: 100请求/秒
- **签名服务限流**: 200请求/秒
- **bgai服务限流**: 100请求/秒
- **聊天API限流**: 50请求/秒
- **认证API限流**: 300请求/秒

### 3. 重试机制

#### 重试配置
- **最大重试次数**: 3次
- **重试间隔**: 1秒
- **指数退避**: 启用
- **退避倍数**: 2

#### 重试异常类型
- `java.io.IOException`
- `java.util.concurrent.TimeoutException`
- `org.springframework.web.client.ResourceAccessException`

### 4. 路由增强

#### 路由配置示例
```yaml
# 签名验证服务路由 - 最高优先级
- id: signature-service
  uri: http://localhost:8689
  order: -1000
  predicates:
    - Path=/api/signature/**,/api/keys/**,/api/auth/**
  filters:
    - AddRequestHeader=X-Gateway-Source, gateway-service
    - AddResponseHeader=X-Gateway-Response, true
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 100
        redis-rate-limiter.burstCapacity: 200
    - name: CircuitBreaker
      args:
        name: signature-service-breaker
        fallbackUri: forward:/fallback/signature-service
```

#### 熔断器回退处理
- **签名服务回退**: `/fallback/signature-service`
- **bgai服务回退**: `/fallback/bgai-service`
- **默认服务回退**: `/fallback/default`
- **聊天API回退**: `/fallback/chat-api`
- **认证API回退**: `/fallback/auth-api`

## Nacos白名单配置

### 配置结构

#### 1. 跳过认证的路径 (skip-auth-paths)
这些路径不需要进行任何认证检查，包括：
- JWT认证
- Token验证
- 权限检查
- 限流检查
- 熔断器检查
- 防御性检查

#### 2. 跳过Token验证的路径 (skip-token-validation-paths)
这些路径不需要进行Token验证，但仍可能需要进行其他检查。

#### 3. 跳过签名验证的路径 (skip-signature-validation-paths)
这些路径不需要进行签名验证，但仍可能需要进行其他检查。

### Nacos配置示例

#### 配置ID
- `gateway-service-local.yml` (本地环境)
- `gateway-service-dev.yml` (开发环境)
- `gateway-service-prod.yml` (生产环境)

#### 命名空间
- `public` (默认)

#### 分组
- `DEFAULT_GROUP` (默认)

### 白名单路径说明

#### 系统路径
- `/actuator/**` - Spring Boot管理端点
- `/health` - 健康检查
- `/metrics` - 监控指标
- `/info` - 应用信息
- `/v3/api-docs/**` - OpenAPI文档
- `/swagger-ui/**` - Swagger UI界面

#### 业务路径
- `/api/keys/**` - API密钥管理接口
- `/api/signature/**` - 签名服务接口
- `/api/auth/**` - 认证服务接口
- `/api/public/**` - 公开接口
- `/public/**` - 静态资源

## 监控端点

### 管理端点
- **健康检查**: `/actuator/health`
- **网关信息**: `/actuator/gateway`
- **熔断器状态**: `/actuator/circuitbreakers`
- **重试状态**: `/actuator/retries`
- **限流器状态**: `/actuator/ratelimiters`

### 自定义监控端点
- **网关状态**: `/api/monitoring/status`
- **熔断器列表**: `/api/monitoring/circuit-breakers`
- **限流器列表**: `/api/monitoring/rate-limiters`
- **重试器列表**: `/api/monitoring/retries`
- **特定熔断器**: `/api/monitoring/circuit-breakers/{name}`

## 使用示例

### 1. 查看熔断器状态
```bash
curl http://localhost:8080/api/monitoring/circuit-breakers
```

### 2. 查看限流器状态
```bash
curl http://localhost:8080/api/monitoring/rate-limiters
```

### 3. 查看网关整体状态
```bash
curl http://localhost:8080/api/monitoring/status
```

### 4. 查看特定熔断器
```bash
curl http://localhost:8080/api/monitoring/circuit-breakers/bgai-service-breaker
```

## 快速开始

### 1. 启动服务

#### 本地模式（推荐测试）
```bash
# Windows
test-start.bat

# Linux/Mac
mvn spring-boot:run -Dspring.profiles.active=local
```

#### Nacos模式
```bash
# Windows
start.bat

# Linux/Mac
chmod +x start.sh
./start.sh
```

### 2. 验证服务
```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 获取所有路由
curl http://localhost:8080/gateway/routes

# 查看熔断器状态
curl http://localhost:8080/api/monitoring/circuit-breakers
```

## 动态路由API

### 1. 获取所有路由
```bash
GET /gateway/routes
```

### 2. 添加路由
```bash
POST /gateway/routes
Content-Type: application/json

{
  "id": "api-test-route",
  "predicates": [
    {
      "name": "Path",
      "args": {
        "pattern": "/api/test/**"
      }
    }
  ],
  "filters": [
    {
      "name": "StripPrefix",
      "args": {
        "parts": "1"
      }
    }
  ],
  "uri": "http://localhost:8688/test",
  "order": 0
}
```

### 3. 更新路由
```bash
PUT /gateway/routes
Content-Type: application/json

{
  "id": "api-test-route",
  "predicates": [...],
  "filters": [...],
  "uri": "http://localhost:8688/updated",
  "order": 1
}
```

### 4. 删除路由
```bash
DELETE /gateway/routes/{routeId}
```

### 5. 获取特定路由
```bash
GET /gateway/routes/{routeId}
```

### 6. 刷新路由
```bash
POST /gateway/routes/refresh
```

## 配置说明

### 1. 端口配置
- 默认端口：8080
- 可通过 `server.port` 修改

### 2. Nacos配置
```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: 8.133.246.113:8848
        namespace: d750d92e-152f-4055-a641-3bc9dda85a29
        group: DEFAULT_GROUP
```

### 3. Resilience4j配置
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 5000
        slidingWindowSize: 10
        slidingWindowType: COUNT_BASED
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
  timelimiter:
    configs:
      default:
        timeoutDuration: 30s
  retry:
    configs:
      default:
        maxRetryAttempts: 3
        waitDuration: 1000
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
  ratelimiter:
    configs:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 0
```

### 4. 动态路由配置
- **数据ID**: `gateway-routes`
- **分组**: `DEFAULT_GROUP`
- **格式**: JSON数组

## 使用示例

### 1. 查看熔断器状态
```bash
curl http://localhost:8080/api/monitoring/circuit-breakers
```

### 2. 查看限流器状态
```bash
curl http://localhost:8080/api/monitoring/rate-limiters
```

### 3. 查看网关整体状态
```bash
curl http://localhost:8080/api/monitoring/status
```

### 4. 查看特定熔断器
```bash
curl http://localhost:8080/api/monitoring/circuit-breakers/bgai-service-breaker
```

## 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| NACOS_HOST | 8.133.246.113 | Nacos服务器地址 |
| NACOS_PORT | 8848 | Nacos服务器端口 |
| NACOS_NAMESPACE | d750d92e-152f-4055-a641-3bc9dda85a29 | Nacos命名空间 |
| NACOS_DISCOVERY_ENABLED | true | 是否启用服务发现 |
| NACOS_CONFIG_ENABLED | true | 是否启用配置中心 |
| SPRING_PROFILES_ACTIVE | dev | Spring配置文件 |

## 注意事项

1. **网关服务不需要数据库**: 已排除所有数据库相关配置
2. **响应式编程**: 使用WebFlux，不支持传统Spring MVC
3. **服务发现**: 需要目标服务在Nacos中注册
4. **负载均衡**: 需要目标服务有多个实例才能看到负载均衡效果
5. **动态路由**: 路由变更会实时生效，无需重启服务
6. **熔断器状态**: 熔断器有三种状态（CLOSED、OPEN、HALF_OPEN），根据失败率自动切换
7. **限流器**: 当请求超过限制时，会返回 429 Too Many Requests 状态码
8. **重试机制**: 只对特定的异常类型进行重试，避免对业务异常进行不必要的重试
9. **监控端点**: 所有监控端点都配置了适当的权限控制，确保安全性
10. **回退处理**: 当服务不可用时，会返回结构化的错误信息，便于客户端处理

## 故障排查

### 常见问题

1. **路由不生效**
   - 检查路由定义格式是否正确
   - 确认目标服务是否可访问
   - 查看网关日志

2. **Nacos连接失败**
   - 检查Nacos服务是否正常运行
   - 确认网络连接和防火墙设置
   - 验证命名空间和分组配置

3. **动态路由API无响应**
   - 确认网关服务已启动
   - 检查端口是否被占用
   - 验证API路径是否正确

4. **配置不生效**
   - 检查Nacos连接是否正常
   - 确认配置文件ID、命名空间、分组是否正确
   - 查看网关启动日志，确认配置是否加载成功

5. **白名单路径仍然被拦截**
   - 检查路径格式是否正确
   - 确认通配符使用是否正确
   - 查看过滤器日志，确认跳过逻辑是否执行
   - 检查Nacos连接是否正常
   - 确认配置文件ID、命名空间、分组是否正确
   - 查看网关启动日志，确认配置是否加载成功

5. **白名单路径仍然被拦截**
   - 检查路径格式是否正确
   - 确认通配符使用是否正确
   - 查看过滤器日志，确认跳过逻辑是否执行

### 调试方法

1. **启用调试日志**
   ```yaml
   logging:
     level:
       com.jiangyang.gateway: DEBUG
       org.springframework.cloud.gateway: DEBUG
   ```

2. **查看路由信息**
   ```bash
   curl http://localhost:8080/actuator/gateway/routes
   ```

3. **检查健康状态**
   ```bash
   curl http://localhost:8080/actuator/health
   ```



1. **最小权限原则**: 只将必要的路径加入白名单
2. **分类管理**: 按功能模块分类管理白名单路径
3. **定期审查**: 定期审查白名单配置，移除不再需要的路径
4. **环境隔离**: 不同环境使用不同的配置文件
5. **版本控制**: 对配置文件进行版本控制，便于回滚
6. **性能优化**: 合理配置超时时间和重试次数，避免资源浪费
7. **监控告警**: 可以通过 Actuator 端点集成监控系统，实现告警功能

## 后续扩展

1. **安全配置**: 可添加JWT认证、OAuth2等安全机制
2. **限流配置**: 可添加Redis限流功能
3. **熔断配置**: 可添加Hystrix或Sentinel熔断功能
4. **监控配置**: 可添加Prometheus、Grafana等监控
5. **缓存配置**: 可添加Redis缓存路由配置

## 版本信息

- **版本**: 1.0.0-Final
- **Java版本**: 17
- **Spring Boot版本**: 3.2.5
- **Spring Cloud版本**: 2023.0.1
- **Spring Cloud Alibaba版本**: 2022.0.0.0 