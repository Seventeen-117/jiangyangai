# Signature Service - 签名验证微服务

## 概述

Signature Service 是一个独立的微服务，专门负责处理接口签名验证功能。它从原始的 bgai 项目中分离出来，提供完整的 HMAC-SHA256 签名验证、时间戳验证和 nonce 防重放攻击功能。

## 功能特性

### 🔐 核心功能
- **HMAC-SHA256 签名验证**：使用预共享密钥进行数字签名验证
- **时间戳验证**：防止过期请求，默认5分钟有效期
- **Nonce 防重放攻击**：使用 Redis 缓存确保请求唯一性
- **应用密钥管理**：支持多应用密钥的创建、查询、更新和删除

### ⚡ 异步验证模式
- **完全异步模式**：使用 CompletableFuture 实现完全异步处理
- **混合验证模式**：快速基础验证 + 异步详细验证
- **快速验证策略**：仅进行基础参数检查，提供毫秒级响应
- **批量验证优化**：支持批量请求的并行处理

### 📊 监控与告警
- **事件驱动架构**：基于 Spring 事件机制的解耦设计
- **实时监控**：成功/失败率、重放攻击检测、性能指标
- **智能告警**：失败率超限、重放攻击、性能异常告警
- **审计日志**：完整的操作审计和日志记录

### 🔗 微服务集成
- **服务注册发现**：支持 Nacos 服务注册
- **Feign 客户端**：与原始 bgai 服务通信
- **健康检查**：提供完整的健康检查端点
- **配置管理**：支持外部化配置

### 🔑 SSO 单点登录功能
- **多种授权类型支持**：授权码、刷新令牌、密码模式
- **JWT令牌生成**：支持访问令牌和刷新令牌
- **会话管理**：Cookie-based会话管理
- **令牌黑名单**：支持令牌撤销和黑名单
- **安全配置**：可配置的JWT密钥和过期时间

### 🛡️ API 安全功能
- **API密钥验证**：支持密钥验证和撤销
- **权限管理**：细粒度的权限控制
- **限流控制**：可配置的请求限流
- **使用统计**：API使用情况监控
- **客户端管理**：客户端信息管理

### 🔍 认证过滤器优化
- **Bearer Token验证**：标准Bearer Token格式验证
- **内部服务调用识别**：智能识别内部服务调用并跳过认证
- **智能路径排除**：灵活的路径排除机制
- **Servlet/WebFlux双环境支持**：同时支持传统Servlet和响应式WebFlux环境

## 技术栈

- **Spring Boot 3.2.0**：应用框架
- **Spring Cloud 2023.0.0**：微服务框架
- **Spring WebFlux**：响应式 Web 支持
- **MyBatis Plus**：ORM 框架
- **Redis**：缓存和防重放攻击
- **MySQL**：数据持久化
- **Hutool**：工具库
- **Lombok**：代码简化

## 微服务架构

### 架构概述

本项目采用微服务架构，将原始的 bgai 单体应用拆分为多个独立的微服务：

```
┌─────────────────────────────────────────────────────────────────┐
│                       微服务架构                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │
│  │   bgai-service  │    │ signature-service│    │  gateway    │ │
│  │   (原服务)      │    │   (签名服务)     │    │  (网关)     │ │
│  │   Port: 8688    │    │   Port: 8689    │    │  Port: 8080 │ │
│  └─────────────────┘    └─────────────────┘    └─────────────┘ │
│           │                       │                   │         │
│           └───────────────────────┼───────────────────┘         │
│                                   │                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐ │
│  │     MySQL       │    │     Redis       │    │   Nacos     │ │
│  │   (数据库)      │    │   (缓存)        │    │ (注册中心)   │ │
│  │   Port: 3306    │    │   Port: 6379    │    │ Port: 8848  │ │
│  └─────────────────┘    └─────────────────┘    └─────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 服务拆分策略

#### 1. bgai-service (原服务)
- **职责**：核心业务逻辑处理
- **功能**：
  - API 网关功能
  - 用户认证和授权
  - 业务数据处理
  - 文件上传下载
  - 聊天功能
  - 计费和统计

#### 2. signature-service (签名服务)
- **职责**：专门处理接口签名验证
- **功能**：
  - HMAC-SHA256 签名验证
  - 时间戳验证
  - Nonce 防重放攻击
  - 应用密钥管理
  - 异步验证处理
  - 批量验证优化
  - 监控和告警
  - SSO单点登录
  - API安全功能

#### 3. gateway-service (网关服务)
- **职责**：统一入口和路由
- **功能**：
  - 请求路由
  - 负载均衡
  - 限流熔断
  - 统一认证
  - 日志记录

### 服务间通信

#### 1. 同步通信
- **HTTP/REST**：服务间的直接调用
- **Feign Client**：声明式 HTTP 客户端
- **负载均衡**：通过 Nacos 实现

#### 2. 异步通信
- **事件驱动**：Spring 事件机制
- **消息队列**：Redis Pub/Sub (可选)
- **异步处理**：CompletableFuture

#### 3. 服务发现
- **Nacos**：服务注册与发现
- **健康检查**：自动健康检测
- **配置管理**：动态配置更新

### 数据管理

#### 1. 数据库设计
```
bgai-service 数据库：
├── api_key (API密钥表)
├── api_client (客户端表)
├── usage_record (使用记录表)
├── chat_completions (聊天记录表)
└── 其他业务表...

signature-service 数据库：
└── app_secret (应用密钥表)
```

#### 2. 缓存策略
- **Redis**：分布式缓存
- **本地缓存**：Caffeine (可选)
- **缓存策略**：Write-Through + Read-Through

#### 3. 数据一致性
- **最终一致性**：异步事件处理
- **分布式事务**：Saga 模式 (可选)
- **数据同步**：定时任务 + 事件驱动

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.0+ (可选)

### 2. 数据库初始化

```sql
-- 执行 src/main/resources/sql/app_secret.sql
-- 创建应用密钥表和测试数据
```

### 3. 配置修改

修改 `src/main/resources/application.yml` 中的数据库和 Redis 配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bgai
    username: your_username
    password: your_password
  
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 启动服务

```bash
# 进入项目目录
cd signature-service

# 编译项目
mvn clean compile

# 启动服务
mvn spring-boot:run
```

服务将在 `http://localhost:8689` 启动。

## 异步签名验证优化

### 验证模式

#### 1. ASYNC 模式 - 完全异步验证
```java
// 完全异步验证
CompletableFuture<Boolean> result = signatureVerificationService.verifySignatureAsync(params, sign, appId);
```

#### 2. HYBRID 模式 - 混合验证
```java
// 快速验证 + 异步详细验证
CompletableFuture<Boolean> result = signatureVerificationService.verifySignatureFast(params, sign, appId);
```

#### 3. QUICK 模式 - 快速验证
```java
// 仅基础验证，后台异步详细验证
boolean quickResult = signatureVerificationService.verifySignatureQuick(params, appId);
```

### 性能优化

#### 1. 线程池管理
- **异步处理线程池**: 10个线程，用于异步验证
- **批量处理线程池**: 5个线程，用于批量验证
- 线程池在服务启动时初始化，避免重复创建

#### 2. 缓存优化
- **Redis缓存**: 应用密钥和nonce缓存
- **批量预加载**: 按客户端ID分组，减少数据库查询

#### 3. 并行处理
- **Stream并行流**: 充分利用多核CPU
- **分组处理**: 按客户端ID分组，提高批量处理效率

### 批量验证优化

```java
// 按客户端ID分组，提高批量处理效率
CompletableFuture<List<Boolean>> verifySignatureBatch(List<SignatureVerificationRequest> verificationRequests)
```

### 异步辅助方法

```java
// 异步保存nonce
CompletableFuture<Void> saveNonceAsync(String nonce, long expireSeconds)

// 异步验证nonce
CompletableFuture<Boolean> validateNonceAsync(String nonce, long cacheExpireSeconds)

// 异步验证时间戳
CompletableFuture<Boolean> validateTimestampAsync(String timestamp, long expireSeconds)
```

### 事件驱动架构

#### 验证事件发布
```java
private void publishVerificationEvent(String appId, String path, String clientIp, 
                                    Map<String, String> params, boolean success, 
                                    String errorMessage, long verificationTime)
```

#### 事件类型
- `VERIFICATION_SUCCESS`: 验证成功
- `VERIFICATION_FAILED`: 验证失败

### 监控和统计

#### 统计信息
```java
SignatureVerificationStats getAppStats(String appId)
```

**统计指标**:
- 成功次数
- 失败次数
- 重放攻击次数
- 总请求数
- 失败率
- 平均验证时间

## API 接口

### 签名验证接口

#### 验证签名
```http
POST /api/signature/verify
Content-Type: application/json

{
    "appId": "test-app-001",
    "timestamp": "1703123456789",
    "nonce": "abc123def456ghi789",
    "sign": "calculated_signature",
    "params": {
        "appId": "test-app-001",
        "timestamp": "1703123456789",
        "nonce": "abc123def456ghi789"
    }
}
```

#### 生成测试参数
```http
GET /api/signature/generate-test-params?appId=test-app-001
```

#### 健康检查
```http
GET /api/signature/health
```

### 应用密钥管理接口

#### 创建应用密钥
```http
POST /api/app-secret
Content-Type: application/json

{
    "appId": "new-app-001",
    "appSecret": "secret_new_app_001",
    "appName": "新应用001",
    "description": "新应用的描述"
}
```

#### 获取所有应用密钥
```http
GET /api/app-secret
```

#### 根据应用ID获取密钥
```http
GET /api/app-secret/{appId}
```

#### 更新应用密钥
```http
PUT /api/app-secret/{id}
Content-Type: application/json

{
    "appName": "更新后的应用名称",
    "description": "更新后的描述"
}
```

#### 删除应用密钥
```http
DELETE /api/app-secret/{id}
```

### SSO 单点登录接口

#### 获取授权URL
```http
GET /api/auth/authorize?redirect_uri={redirect_uri}
```

#### 处理回调
```http
GET /api/auth/callback?code={code}&state={state}
```

#### 获取访问令牌
```http
POST /api/auth/token
Content-Type: application/json

{
    "code": "auth_code",
    "client_id": "bgai-client-id",
    "grant_type": "authorization_code"
}
```

#### 刷新令牌
```http
POST /api/auth/token/refresh
Content-Type: application/json

{
    "refresh_token": "your_refresh_token",
    "client_id": "bgai-client-id",
    "grant_type": "refresh_token"
}
```

#### 获取用户信息
```http
GET /api/auth/userinfo
Authorization: Bearer your_access_token
```

#### 验证令牌
```http
POST /api/auth/token/verify
Content-Type: application/json

{
    "token": "your_access_token"
}
```

#### 注销登录
```http
POST /api/auth/logout
Authorization: Bearer your_access_token
```

#### 获取配置信息
```http
GET /api/auth/config
```

#### 检查会话状态
```http
GET /api/auth/session/status
```

### API 安全接口

#### 验证API密钥
```http
POST /api/security/verify
X-API-Key: your_api_key
```

#### 检查权限
```http
POST /api/security/check-permission
X-API-Key: your_api_key
Content-Type: application/json

{
    "permission": "read",
    "resource": "user"
}
```

#### 获取客户端信息
```http
GET /api/security/client/{clientId}
```

#### 获取客户端列表
```http
GET /api/security/clients
```

#### 创建API密钥
```http
POST /api/security/api-key
Content-Type: application/json

{
    "clientId": "new-client",
    "permissions": ["read:user", "write:user"]
}
```

#### 撤销API密钥
```http
DELETE /api/security/api-key/{apiKey}
```

#### 获取使用统计
```http
GET /api/security/stats
```

#### 获取限流信息
```http
GET /api/security/rate-limit/{clientId}
```

#### 更新权限
```http
PUT /api/security/client/{clientId}/permissions
Content-Type: application/json

{
    "permissions": ["read:user", "write:user", "read:chat"]
}

## 配置说明

### 签名验证配置

```yaml
signature:
  enabled: true                           # 是否启用签名验证
  timestamp-expire-seconds: 300          # 时间戳有效期（秒）
  nonce-cache-expire-seconds: 1800      # nonce缓存过期时间（秒）
  app-secret-cache-expire-seconds: 3600 # 应用密钥缓存过期时间（秒）
  
  # 异步验证配置
  async:
    enabled: false                       # 是否启用异步验证
    mode: HYBRID                        # 异步验证模式：ASYNC/HYBRID/QUICK
    timeout: 5000                       # 异步验证超时时间（毫秒）
  
  # 批量验证配置
  batch:
    enabled: false                      # 是否启用批量验证
    max-batch-size: 100                # 最大批量大小
    timeout: 10000                     # 批量验证超时时间（毫秒）
```

### 服务配置

```yaml
server:
  port: 8689                           # 服务端口

spring:
  application:
    name: signature-service             # 服务名称
  
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848    # Nacos 服务地址
        namespace: public              # 命名空间
        group: DEFAULT_GROUP           # 分组
```

### SSO 配置

```yaml
sso:
  client-id: bgai-client-id
  client-secret: bgai-client-secret
  redirect-uri: http://localhost:8080/api/auth/callback
  authorize-url: http://localhost:8080/auth/authorize
  token-url: http://localhost:8080/auth/token
  user-info-url: http://localhost:8080/auth/userinfo
  logout-url: http://localhost:8080/auth/logout
  session:
    timeout: 3600
    max-sessions: 1000
  security:
    jwt-secret: jiangyang-sso-secret-key-2024-for-development-only-change-in-production
    jwt-expiration: 3600000
    refresh-token-expiration: 86400000
  callback:
    success-url: http://localhost:3000/dashboard
    failure-url: http://localhost:3000/login?error=authentication_failed
    logout-success-url: http://localhost:3000/login
```

### API 安全配置

```yaml
api:
  security:
    header-name: X-API-Key
    api-keys:
      "804822262af64439aeab611143864948": "client-1"
      "fdac4b850ba74f8f86338d3c445a88f5": "client-2"
    rate-limit:
      enabled: true
      default-limit: 100
      burst-capacity: 200
    permissions:
      client-1:
        - "read:user"
        - "write:user"
        - "read:chat"
        - "write:chat"
      client-2:
        - "read:user"
        - "read:chat"
    audit:
      enabled: true
      log-requests: true
      log-responses: false
      sensitive-headers:
        - "Authorization"
        - "X-API-Key"
```

### 认证过滤器配置

```yaml
signature:
  authentication:
    enabled: true
    authorization-header-name: Authorization
    bearer-prefix: "Bearer "
    api-key-header-name: X-API-Key
    request-from-header-name: X-Request-From
    test-api-key: test-api-key-123
    internal-service-name: bgtech-ai
    strict-validation: true
    enable-logging: true
    allow-internal-service-skip: true
    excluded-paths:
      - "/actuator/**"
      - "/health/**"
      - "/api/auth/**"
    strict-validation-paths:
      - "/api/signature/**"
      - "/api/security/**"
    internal-services:
      - "bgai-service"
      - "gateway-service"
      - "signature-service"
    jwt:
      secret: your-jwt-secret-key
      expiration-seconds: 3600
      refresh-expiration-seconds: 86400
      issuer: signature-service
      audience: signature-api
```

### 异步验证配置

```yaml
signature:
  verification:
    # 异步验证配置
    async-enabled: false
    async-mode: HYBRID  # ASYNC, HYBRID, QUICK
    async-timeout: 5000  # 毫秒
    async-thread-pool-size: 10
    batch-thread-pool-size: 5
    enable-async-event-publishing: true
    async-retry-count: 3
    async-retry-interval: 1000  # 毫秒
```

## 测试数据

服务启动后，数据库中已包含以下测试应用：

| 应用ID | 应用密钥 | 应用名称 | 状态 |
|--------|----------|----------|------|
| test-app-001 | secret_test_app_001 | 测试应用001 | 启用 |
| test-app-002 | secret_test_app_002 | 测试应用002 | 启用 |
| demo-app-001 | secret_demo_app_001 | 演示应用001 | 启用 |

## 监控指标

### 应用统计信息

通过事件监听器收集的指标包括：

- **总请求数**：应用的总验证请求数
- **成功请求数**：验证成功的请求数
- **失败请求数**：验证失败的请求数
- **重放攻击数**：检测到的重放攻击次数
- **成功率**：验证成功率
- **失败率**：验证失败率
- **平均验证时间**：平均签名验证耗时

### 告警条件

- **高失败率**：失败率超过 10%
- **重放攻击**：重放攻击次数超过 5 次
- **性能异常**：平均验证时间超过 1 秒

## 使用示例

### 1. SSO 认证流程
```bash
# 1. 获取授权URL
curl "http://localhost:8689/api/auth/authorize?redirect_uri=http://localhost:3000/callback"

# 2. 处理回调（浏览器自动跳转）
# 3. 获取访问令牌
curl -X POST "http://localhost:8689/api/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"code":"auth_code","client_id":"bgai-client-id","grant_type":"authorization_code"}'

# 4. 获取用户信息
curl "http://localhost:8689/api/auth/userinfo" \
  -H "Authorization: Bearer your_access_token"
```

### 2. API 安全验证
```bash
# 验证API密钥
curl -X POST "http://localhost:8689/api/security/verify" \
  -H "X-API-Key: 804822262af64439aeab611143864948"

# 检查权限
curl -X POST "http://localhost:8689/api/security/check-permission" \
  -H "X-API-Key: 804822262af64439aeab611143864948" \
  -d "permission=read&resource=user"

# 获取客户端信息
curl "http://localhost:8689/api/security/client/client-1"
```

### 3. 签名验证
```bash
# 使用签名验证的API调用
curl -X POST "http://localhost:8689/api/signature/verify" \
  -H "X-Signature: your_signature" \
  -H "X-Timestamp: 1234567890" \
  -d '{"data":"your_data"}'
```

### 4. 异步验证
```bash
# 异步验证
curl "http://localhost:8689/api/signature/async-verify?appId=test&timestamp=1234567890&nonce=abc123&sign=calculated-signature"

# 批量验证
curl -X POST "http://localhost:8689/api/signature/batch-verify" \
  -H "Content-Type: application/json" \
  -d '[
    {"appId":"test1","timestamp":"1234567890","nonce":"abc123","sign":"sign1"},
    {"appId":"test2","timestamp":"1234567891","nonce":"def456","sign":"sign2"}
  ]'
```

### 5. 认证过滤器
```bash
# 正确的Bearer Token
curl -H "Authorization: Bearer your-jwt-token" \
     http://localhost:8689/api/signature/verify

# 内部服务调用（跳过认证）
curl -H "X-API-Key: test-api-key-123" \
     -H "X-Request-From: bgtech-ai" \
     http://localhost:8689/api/signature/verify

# 排除路径（跳过认证）
curl http://localhost:8689/actuator/health
```

## 微服务集成

### 与原始 bgai 服务通信

Signature Service 通过 Feign 客户端与原始 bgai 服务通信：

```java
@FeignClient(name = "bgai-service", fallback = BgaiServiceClientFallback.class)
public interface BgaiServiceClient {
    
    @PostMapping("/api/auth/validate-api-key")
    Map<String, Object> validateApiKey(@RequestParam("apiKey") String apiKey);
    
    @GetMapping("/api/auth/user-info")
    Map<String, Object> getUserInfo(@RequestHeader("Authorization") String authorization);
    
    @GetMapping("/actuator/health")
    Map<String, Object> health();
}
```

### 服务发现

服务支持 Nacos 服务注册发现，可以自动注册到 Nacos 注册中心。

## 部署说明

### Docker 部署

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/signature-service-1.0.0.jar app.jar
EXPOSE 8689
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 构建和运行

```bash
# 构建项目
mvn clean package

# 运行 JAR 文件
java -jar target/signature-service-1.0.0.jar

# 使用 Docker 运行
docker build -t signature-service .
docker run -p 8689:8689 signature-service
```

## 开发指南

### 项目结构

```
signature-service/
├── src/main/java/com/bgpay/signature/
│   ├── SignatureServiceApplication.java    # 启动类
│   ├── controller/                        # 控制器层
│   │   ├── SignatureVerificationController.java
│   │   └── AppSecretController.java
│   ├── service/                          # 服务层
│   │   ├── SignatureVerificationService.java
│   │   └── impl/
│   │       └── SignatureVerificationServiceImpl.java
│   ├── entity/                           # 实体类
│   │   └── AppSecret.java
│   ├── mapper/                           # 数据访问层
│   │   └── AppSecretMapper.java
│   ├── model/                            # 模型类
│   │   └── SignatureVerificationRequest.java
│   ├── event/                            # 事件类
│   │   └── SignatureVerificationEvent.java
│   ├── listener/                         # 事件监听器
│   │   └── SignatureVerificationEventListener.java
│   └── feign/                            # Feign 客户端
│       ├── BgaiServiceClient.java
│       └── BgaiServiceClientFallback.java
├── src/main/resources/
│   ├── application.yml                   # 配置文件
│   └── sql/
│       └── app_secret.sql               # 数据库初始化脚本
└── pom.xml                              # Maven 配置
```

### 添加新功能

1. **新增实体类**：在 `entity` 包下创建实体类
2. **新增 Mapper**：在 `mapper` 包下创建 Mapper 接口
3. **新增服务**：在 `service` 包下创建服务接口和实现
4. **新增控制器**：在 `controller` 包下创建 REST 控制器
5. **新增事件**：在 `event` 包下创建事件类
6. **新增监听器**：在 `listener` 包下创建事件监听器

### 测试

```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify
```

## 迁移总结

### 迁移的组件

#### 1. 核心服务组件
- **ApiConfigService**: API配置管理服务
- **ApiKeyService**: API密钥管理服务
- **SignatureVerificationService**: 签名验证服务（增强版）

#### 2. 过滤器组件
- **ApiKeyAuthenticationFilter**: API密钥认证过滤器（Servlet）
- **ApiKeyWebFilter**: API密钥认证过滤器（WebFlux）
- **SignatureVerificationWebFilter**: 签名验证过滤器（WebFlux）
- **AsyncSignatureVerificationFilter**: 异步签名验证过滤器（新增）
- **AuthenticationFilter**: 认证过滤器（Servlet）
- **AuthenticationWebFilter**: 认证过滤器（WebFlux）

#### 3. 配置组件
- **ApiKeyConfig**: API密钥配置管理
- **SignatureConfig**: 签名验证配置管理
- **WebFluxConfig**: WebFlux环境配置
- **AuthenticationConfig**: 认证配置管理

#### 4. 工具类
- **PathMatcherUtil**: 路径匹配工具
- **Sha256Util**: SHA-256哈希工具

#### 5. 模型类
- **ApiKeyValidationResult**: API密钥验证结果
- **SignatureVerificationRequest**: 签名验证请求

#### 6. 事件和监听器
- **SignatureVerificationEvent**: 签名验证事件
- **SignatureVerificationEventListener**: 签名验证事件监听器

### 优化内容

#### 1. 架构优化
- **配置集中化**: 将所有配置从 `@Value` 注解迁移到配置类
- **工具类抽象**: 创建统一的工具类，提高代码复用性
- **类型安全**: 使用 `@ConfigurationProperties` 进行类型安全的配置管理

#### 2. 性能优化
- **异步处理**: 支持多种异步验证模式
- **缓存优化**: Redis缓存应用密钥和nonce
- **批量处理**: 支持批量请求的并行处理
- **线程池管理**: 可配置的线程池管理

#### 3. 可观测性优化
- **事件驱动**: 签名验证事件发布
- **统计信息**: 详细的统计信息收集
- **监控指标**: 完善的监控指标暴露

#### 4. 安全性优化
- **API密钥管理**: SHA-256哈希存储
- **签名验证**: HMAC-SHA256签名
- **防重放攻击**: nonce机制
- **权限控制**: 细粒度的权限管理

### 迁移对比

#### 原 bgai-service 实现
- 分散的配置管理
- 硬编码的路径匹配
- 基础的错误处理
- 简单的异步验证

#### 优化后的 signature-service 实现
- 集中的配置管理
- 灵活的路径匹配
- 完善的错误处理
- 多种异步验证模式
- 事件驱动架构
- 详细的监控和统计

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查数据库配置和网络连接
   - 确认数据库服务正常运行

2. **Redis 连接失败**
   - 检查 Redis 配置和网络连接
   - 确认 Redis 服务正常运行

3. **签名验证失败**
   - 检查应用密钥是否正确
   - 确认参数按字典序排列
   - 验证时间戳是否在有效期内

4. **服务注册失败**
   - 检查 Nacos 服务是否正常运行
   - 确认网络连接和配置正确

### 日志查看

```bash
# 查看应用日志
tail -f logs/signature-service.log

# 查看错误日志
grep ERROR logs/signature-service.log
```

## 版本历史

- **v1.0.0** (2024-01-01)
  - 初始版本
  - 实现基础签名验证功能
  - 支持异步验证和批量处理
  - 集成监控和告警功能

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## Token 生成服务详细指南

### 概述

Signature Service 提供了完整的 JWT Token 生成、验证、刷新和撤销功能。该服务基于现有的 JWT 工具类和 SSO 服务构建，支持多种 Token 类型和批量操作。

### 功能特性

- ✅ JWT Token 生成（访问令牌和刷新令牌）
- ✅ Token 验证和解析
- ✅ Token 刷新机制
- ✅ Token 撤销和黑名单管理
- ✅ 批量 Token 生成
- ✅ Token 信息查询
- ✅ 支持自定义过期时间
- ✅ 支持多种 Token 类型（ACCESS、REFRESH、BOTH）
- ✅ **无需 API Key 认证** - `/api/token/**` 端点被排除在 API Key 验证之外

### Token API 端点

#### 1. 生成 Token

**POST** `/api/token/generate`

**请求体：**
```json
{
  "userId": "user123",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "role": "USER",
  "nickname": "John Doe",
  "avatar": "https://example.com/avatar.jpg",
  "department": "Engineering",
  "position": "Software Engineer",
  "phone": "+1234567890",
  "gender": "male",
  "enabled": true,
  "locked": false,
  "tokenType": "BOTH",
  "expirationSeconds": 3600,
  "clientId": "web-app",
  "clientSecret": "secret123",
  "extraInfo": {
    "permissions": ["read", "write"],
    "groups": ["admin", "user"]
  }
}
```

**响应：**
```json
{
  "success": true,
  "userId": "user123",
  "username": "john.doe",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "BOTH",
  "expiresIn": 3600,
  "expiresAt": 1640995200000,
  "tokenTypeHeader": "Bearer"
}
```

#### 2. 刷新 Token

**POST** `/api/token/refresh?refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

**响应：**
```json
{
  "success": true,
  "userId": "user123",
  "username": "john.doe",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "BOTH",
  "expiresIn": 3600,
  "expiresAt": 1640995200000,
  "tokenTypeHeader": "Bearer"
}
```

#### 3. 验证 Token

**POST** `/api/token/validate?accessToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

**响应：**
```json
{
  "valid": true,
  "userId": "user123",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "role": "USER"
}
```

#### 4. 撤销 Token

**POST** `/api/token/revoke?accessToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

**响应：**
```json
{
  "success": true,
  "message": "Token撤销成功"
}
```

#### 5. 获取 Token 信息

**GET** `/api/token/info?accessToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

**响应：**
```json
{
  "userId": "user123",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "role": "USER",
  "expirationDate": "2024-01-01T12:00:00.000Z",
  "remainingTime": 1800000,
  "isBlacklisted": false,
  "isExpiringSoon": false
}
```

#### 6. 批量生成 Token

**POST** `/api/token/batch-generate`

**请求体：**
```json
[
  {
    "userId": "user1",
    "username": "user1",
    "email": "user1@example.com",
    "role": "USER",
    "tokenType": "BOTH"
  },
  {
    "userId": "user2",
    "username": "user2",
    "email": "user2@example.com",
    "role": "ADMIN",
    "tokenType": "ACCESS"
  }
]
```

**响应：**
```json
{
  "success": true,
  "count": 2,
  "tokens": [
    {
      "success": true,
      "userId": "user1",
      "username": "user1",
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "BOTH",
      "expiresIn": 3600,
      "expiresAt": 1640995200000,
      "tokenTypeHeader": "Bearer"
    },
    {
      "success": true,
      "userId": "user2",
      "username": "user2",
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "ACCESS",
      "expiresIn": 3600,
      "expiresAt": 1640995200000,
      "tokenTypeHeader": "Bearer"
    }
  ]
}
```

### Token 配置参数

在 `application.yml` 中配置以下参数：

```yaml
sso:
  security:
    jwt-secret: your-jwt-secret-key-here
    jwt-expiration: 3600  # 访问令牌过期时间（秒）
    refresh-token-expiration: 86400  # 刷新令牌过期时间（秒）
```

### Token 认证说明

#### API Key 认证豁免

`/api/token/**` 和 `/api/keys/**` 端点已被配置为**无需 API Key 认证**，这意味着：

- ✅ 可以直接调用 Token 生成端点，无需提供 `X-API-Key` 头
- ✅ 可以直接调用 API Key 生成端点，无需提供 `X-API-Key` 头
- ✅ 适用于内部服务调用和直接的用户认证场景
- ✅ 仍然受到其他安全机制保护（如 JWT 签名验证）

#### 排除的端点

以下端点不需要 API Key 认证：

**Token 相关端点：**
- `/api/token/generate` - 生成 Token
- `/api/token/refresh` - 刷新 Token  
- `/api/token/validate` - 验证 Token
- `/api/token/revoke` - 撤销 Token
- `/api/token/info` - 获取 Token 信息
- `/api/token/batch-generate` - 批量生成 Token

**API Key 相关端点：**
- `/api/keys/generate` - 生成 API Key
- `/api/keys/validate` - 验证 API Key
- `/api/keys/revoke` - 撤销 API Key
- `/api/keys/info` - 获取 API Key 信息

### Token 使用示例

#### Java 客户端示例

```java
@RestController
public class TokenClientController {
    
    @Autowired
    private WebClient webClient;
    
    public TokenResponse generateToken(String userId, String username) {
        TokenRequest request = TokenRequest.builder()
                .userId(userId)
                .username(username)
                .email("user@example.com")
                .role("USER")
                .tokenType("BOTH")
                .expirationSeconds(3600L)
                .build();
        
        return webClient.post()
                .uri("http://localhost:8689/api/token/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();
    }
    
    public boolean validateToken(String accessToken) {
        return webClient.post()
                .uri("http://localhost:8689/api/token/validate")
                .queryParam("accessToken", accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Boolean) response.get("valid"))
                .block();
    }
}
```

#### cURL 示例

```bash
# 生成 Token
curl -X POST "http://localhost:8689/api/token/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "role": "USER",
    "tokenType": "BOTH"
  }'

# 验证 Token
curl -X POST "http://localhost:8689/api/token/validate?accessToken=YOUR_TOKEN_HERE"

# 刷新 Token
curl -X POST "http://localhost:8689/api/token/refresh?refreshToken=YOUR_REFRESH_TOKEN_HERE"

# 撤销 Token
curl -X POST "http://localhost:8689/api/token/revoke?accessToken=YOUR_TOKEN_HERE"
```

### Token 错误处理

服务会返回详细的错误信息：

```json
{
  "success": false,
  "errorMessage": "Token生成失败: 用户ID不能为空",
  "errorCode": "TOKEN_GENERATION_FAILED"
}
```

常见错误代码：
- `TOKEN_GENERATION_FAILED`: Token 生成失败
- `TOKEN_REFRESH_FAILED`: Token 刷新失败
- `INVALID_TOKEN`: 无效的 Token
- `EXPIRED_TOKEN`: 过期的 Token
- `BLACKLISTED_TOKEN`: 已撤销的 Token

### Token 安全特性

1. **JWT 签名验证**: 使用 HMAC-SHA256 算法签名
2. **Token 黑名单**: 支持撤销的 Token 管理
3. **过期时间控制**: 可配置的 Token 过期时间
4. **刷新机制**: 安全的 Token 刷新流程
5. **批量操作**: 支持高效的批量 Token 生成
6. **无需 API Key 认证**: `/api/token/**` 和 `/api/keys/**` 端点被排除在 API Key 验证之外，可以直接访问

### Token 性能优化

1. **并行处理**: 批量操作使用 CompletableFuture 并行处理
2. **缓存机制**: 利用现有的缓存配置
3. **异步日志**: 使用异步日志记录减少性能影响
4. **连接池**: 使用 WebClient 连接池优化网络请求

### Token 监控和日志

服务提供详细的日志记录：

```
2024-01-01 12:00:00 INFO  c.s.service.impl.TokenServiceImpl - 开始生成Token: userId=user123, username=john.doe
2024-01-01 12:00:00 INFO  c.s.service.impl.TokenServiceImpl - Token生成成功: userId=user123, tokenType=BOTH
2024-01-01 12:00:00 INFO  c.s.controller.TokenController - Token生成成功: userId=user123, tokenType=BOTH
```

### Token 扩展功能

1. **自定义 Token 类型**: 支持 ACCESS、REFRESH、BOTH 类型
2. **自定义过期时间**: 支持每个请求自定义过期时间
3. **额外信息**: 支持在 Token 中包含额外的用户信息
4. **客户端验证**: 支持客户端 ID 和密钥验证
5. **批量操作**: 支持高效的批量 Token 生成

## 签名生成接口使用指南

### 签名生成概述

`/api/signature/generate` 接口用于生成 HMAC-SHA256 签名，它是签名验证服务的核心功能之一。该接口帮助客户端正确生成签名，确保请求能通过签名验证。

### 签名生成接口信息

- **URL**: `http://localhost:8689/api/signature/generate`
- **方法**: `POST`
- **内容类型**: `application/json`
- **认证**: 无需 API Key（在排除路径列表中）

### 签名生成请求参数

#### 必需参数

| 参数名 | 类型 | 说明 | 示例 |
|-------|------|------|------|
| `appId` | String | 应用ID，用于标识应用 | `"test-app-001"` |
| `secret` | String | 应用密钥，用于签名计算 | `"secret_test_app_001"` |
| `params` | Object | 业务参数对象（可选） | `{"userId": "123", "action": "login"}` |

#### 签名生成请求示例

##### 基础请求（无业务参数）
```json
{
    "appId": "test-app-001",
    "secret": "secret_test_app_001"
}
```

##### 包含业务参数的请求
```json
{
    "appId": "test-app-001",
    "secret": "secret_test_app_001",
    "params": {
        "userId": "12345",
        "action": "getUserInfo",
        "apiVersion": "v1"
    }
}
```

### 签名生成响应格式

#### 成功响应

```json
{
    "success": true,
    "message": "Signature generated successfully",
    "data": {
        "appId": "test-app-001",
        "timestamp": "1703123456789",
        "nonce": "abc123def456ghi789jkl",
        "userId": "12345",
        "action": "getUserInfo", 
        "apiVersion": "v1",
        "sign": "calculated_hmac_sha256_signature"
    }
}
```

#### 响应字段说明

| 字段名 | 类型 | 说明 |
|-------|------|------|
| `success` | Boolean | 请求是否成功 |
| `message` | String | 响应消息 |
| `data` | Object | 签名数据对象 |
| `data.appId` | String | 应用ID（原样返回） |
| `data.timestamp` | String | 时间戳（毫秒，系统自动生成） |
| `data.nonce` | String | 随机字符串（防重放，系统自动生成） |
| `data.sign` | String | 计算出的 HMAC-SHA256 签名 |
| `data.*` | String | 其他业务参数（原样返回） |

#### 错误响应

##### 参数缺失
```json
{
    "success": false,
    "message": "Missing required parameters: appId and secret"
}
```

##### 服务器错误
```json
{
    "success": false,
    "message": "Error generating signature: [具体错误信息]"
}
```

### 签名算法详解

#### 1. 参数准备

系统会自动添加以下参数：
- `timestamp`: 当前时间戳（毫秒）
- `nonce`: UUID 随机字符串（去掉横线）
- 原有的业务参数保持不变

#### 2. 签名计算步骤

1. **参数排序**: 按 key 的字典序排序所有参数（除了 `sign` 参数）
2. **字符串构造**: 按 `key=value&` 格式拼接，例如：`appId=test-app-001&nonce=abc123&timestamp=1703123456789&userId=12345`
3. **HMAC-SHA256 计算**: 使用应用密钥对构造的字符串进行 HMAC-SHA256 计算
4. **十六进制转换**: 将计算结果转为小写十六进制字符串

#### 3. 算法实现（伪代码）

```javascript
function generateSignature(params, secret) {
    // 1. 移除 sign 参数并按 key 排序
    const sortedParams = Object.keys(params)
        .filter(key => key !== 'sign' && params[key] !== '')
        .sort()
        .map(key => `${key}=${params[key]}`)
        .join('&');
    
    // 2. 计算 HMAC-SHA256
    const signature = hmacSha256(sortedParams, secret);
    
    // 3. 转为小写十六进制
    return signature.toLowerCase();
}
```

### 签名生成使用场景

#### 场景1: 客户端登录签名
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "mobile-app-001",
    "secret": "mobile_secret_key",
    "params": {
      "action": "login",
      "username": "user123",
      "deviceId": "device_abc123"
    }
  }'
```

#### 场景2: API调用签名
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "api-client-001", 
    "secret": "api_client_secret",
    "params": {
      "endpoint": "/api/user/profile",
      "method": "GET",
      "version": "v2"
    }
  }'
```

#### 场景3: 支付接口签名
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "payment-service",
    "secret": "payment_secret_2024",
    "params": {
      "orderId": "ORDER_20241209_001",
      "amount": "99.99",
      "currency": "CNY",
      "merchantId": "MERCHANT_123"
    }
  }'
```

### 签名生成前端集成示例

#### JavaScript/Fetch
```javascript
async function generateSignature(appId, secret, businessParams = {}) {
    const response = await fetch('http://localhost:8689/api/signature/generate', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            appId: appId,
            secret: secret,
            params: businessParams
        })
    });
    
    const result = await response.json();
    
    if (result.success) {
        return result.data; // 包含签名的完整参数
    } else {
        throw new Error(result.message);
    }
}

// 使用示例
generateSignature('test-app-001', 'secret_key', {
    userId: '12345',
    action: 'getUserProfile'
}).then(signedParams => {
    console.log('签名参数:', signedParams);
    // 使用 signedParams 发起实际的业务请求
}).catch(error => {
    console.error('签名生成失败:', error);
});
```

#### Java/Spring Boot
```java
@Service
public class SignatureService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Map<String, String> generateSignature(String appId, String secret, Map<String, String> params) {
        String url = "http://localhost:8689/api/signature/generate";
        
        Map<String, Object> request = new HashMap<>();
        request.put("appId", appId);
        request.put("secret", secret);
        request.put("params", params);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map<String, Object> result = response.getBody();
        
        if (Boolean.TRUE.equals(result.get("success"))) {
            return (Map<String, String>) result.get("data");
        } else {
            throw new RuntimeException((String) result.get("message"));
        }
    }
}
```

#### Python/Requests
```python
import requests
import json

def generate_signature(app_id, secret, params=None):
    url = "http://localhost:8689/api/signature/generate"
    
    payload = {
        "appId": app_id,
        "secret": secret,
        "params": params or {}
    }
    
    response = requests.post(url, json=payload)
    result = response.json()
    
    if result.get("success"):
        return result.get("data")
    else:
        raise Exception(result.get("message"))

# 使用示例
try:
    signed_params = generate_signature(
        app_id="test-app-001",
        secret="secret_key", 
        params={
            "userId": "12345",
            "action": "getUserProfile"
        }
    )
    print("签名参数:", signed_params)
except Exception as e:
    print("签名生成失败:", e)
```

### 签名生成最佳实践

#### 1. 密钥管理
- ✅ **安全存储**: 将应用密钥存储在环境变量或配置文件中
- ✅ **定期轮换**: 定期更换应用密钥
- ❌ **避免硬编码**: 不要在代码中硬编码密钥

#### 2. 参数处理
- ✅ **参数验证**: 在发送前验证业务参数的完整性
- ✅ **编码处理**: 确保参数值正确编码，避免特殊字符问题
- ✅ **空值过滤**: 系统会自动过滤空值参数

#### 3. 错误处理
- ✅ **重试机制**: 实现适当的重试逻辑
- ✅ **异常捕获**: 妥善处理网络异常和服务异常
- ✅ **日志记录**: 记录签名生成的关键信息

#### 4. 性能优化
- ✅ **连接复用**: 使用连接池复用 HTTP 连接
- ✅ **缓存策略**: 对于相同参数，可以短时间内缓存签名结果
- ✅ **批量处理**: 对于大量请求，考虑批量生成签名

### 签名生成常见问题

#### Q1: 签名生成后多长时间有效？
A: 生成的签名包含时间戳，默认有效期为 5 分钟（300秒）。在验证时会检查时间戳的有效性。

#### Q2: nonce 是否会重复？
A: nonce 使用 UUID 生成，重复概率极低。系统在验证时会检查 nonce 的唯一性（防重放）。

#### Q3: 如何处理时区问题？
A: 系统使用毫秒级时间戳，不涉及时区转换。客户端和服务端的时间差不应超过 5 分钟。

#### Q4: 能否自定义时间戳和 nonce？
A: 目前接口会自动生成时间戳和 nonce，确保其正确性和安全性。

#### Q5: 签名验证失败怎么办？
A: 检查以下几点：
- appId 和 secret 是否正确
- 参数是否与生成签名时一致
- 请求时间是否在有效期内
- nonce 是否被重复使用

### 签名生成相关接口

#### 签名验证接口
生成签名后，可以使用 `/api/signature/verify` 接口验证签名：

```bash
curl -X POST "http://localhost:8689/api/signature/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "test-app-001",
    "timestamp": "1703123456789",
    "nonce": "abc123def456ghi789jkl",
    "sign": "calculated_signature",
    "params": {
      "appId": "test-app-001",
      "timestamp": "1703123456789", 
      "nonce": "abc123def456ghi789jkl",
      "userId": "12345"
    }
  }'
```

#### 快速验证接口
使用 `/api/signature/verify-quick` 进行快速验证（不验证时间戳和 nonce）：

```bash
curl -X POST "http://localhost:8689/api/signature/verify-quick" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "test-app-001",
    "sign": "calculated_signature",
    "params": {
      "appId": "test-app-001",
      "userId": "12345"
    }
  }'
```

#### 示例参数生成接口
使用 `/api/signature/example` 获取示例签名参数：

```bash
curl "http://localhost:8689/api/signature/example?appId=test-app-001&secret=secret_key"
```

### 签名生成安全建议

1. **HTTPS**: 生产环境必须使用 HTTPS 传输
2. **密钥保护**: 绝不在客户端暴露应用密钥
3. **参数校验**: 验证所有输入参数的合法性
4. **监控告警**: 监控签名生成的频率和失败率
5. **访问控制**: 限制签名生成接口的访问频率

## 签名生成接口请求示例集合

### 快速开始

#### 基础 cURL 请求

```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "test-app-001",
    "secret": "secret_test_app_001"
  }'
```

**预期响应：**
```json
{
  "success": true,
  "message": "Signature generated successfully",
  "data": {
    "appId": "test-app-001",
    "timestamp": "1703123456789",
    "nonce": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "sign": "c8d4e3f7a1b2c5d6e9f8a7b4c1d8e5f2a3b6c9d2e5f8a1b4c7d0e3f6a9b2c5d8"
  }
}
```

### 完整示例集合

#### 1. 用户登录签名

```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "mobile-app-login",
    "secret": "mobile_login_secret_2024",
    "params": {
      "action": "login",
      "username": "user123",
      "deviceId": "mobile_device_abc123",
      "osType": "iOS",
      "appVersion": "1.2.3"
    }
  }'
```

#### 2. API 调用签名

```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "api-client-service",
    "secret": "api_client_secret_key",
    "params": {
      "endpoint": "/api/user/profile", 
      "method": "GET",
      "version": "v2",
      "userId": "12345",
      "requestId": "req_20241209_001"
    }
  }'
```

#### 3. 支付订单签名

```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "payment-gateway",
    "secret": "payment_secret_2024_prod",
    "params": {
      "orderId": "ORDER_20241209_123456",
      "amount": "299.99",
      "currency": "CNY",
      "merchantId": "MERCHANT_DEMO_001",
      "productName": "高级会员套餐",
      "customerEmail": "customer@example.com",
      "paymentMethod": "alipay"
    }
  }'
```

#### 4. 文件上传签名

```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "file-upload-service",
    "secret": "file_upload_secret",
    "params": {
      "action": "uploadFile",
      "fileName": "document.pdf",
      "fileSize": "2048576",
      "fileType": "application/pdf",
      "userId": "user_789",
      "bucketName": "user-documents"
    }
  }'
```

#### 5. 数据查询签名

```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "data-analytics",
    "secret": "analytics_secret_key",
    "params": {
      "queryType": "userStatistics",
      "startDate": "2024-12-01",
      "endDate": "2024-12-09", 
      "metrics": "pageViews,uniqueUsers,sessionDuration",
      "filters": "country=CN,platform=mobile"
    }
  }'
```

#### 6. 消息推送签名

```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "push-notification",
    "secret": "push_service_secret",
    "params": {
      "action": "sendPush",
      "targetType": "user",
      "targetId": "user_456",
      "messageType": "promotion",
      "title": "限时优惠活动",
      "priority": "high"
    }
  }'
```

#### 7. 第三方集成签名

```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "third-party-integration",
    "secret": "integration_secret_2024",
    "params": {
      "provider": "wechat",
      "action": "getUserInfo",
      "openId": "wx_openid_123456789",
      "scope": "userinfo",
      "state": "random_state_string"
    }
  }'
```

### 测试不同场景

#### 场景1: 空业务参数
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "simple-test",
    "secret": "simple_secret",
    "params": {}
  }'
```

#### 场景2: 单个参数
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "single-param-test",
    "secret": "single_param_secret",
    "params": {
      "userId": "test_user_123"
    }
  }'
```

#### 场景3: 中文参数
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "chinese-test",
    "secret": "chinese_secret",
    "params": {
      "用户名": "张三",
      "产品名称": "测试产品",
      "描述": "这是一个测试描述"
    }
  }'
```

#### 场景4: 特殊字符参数
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "special-chars-test",
    "secret": "special_chars_secret",
    "params": {
      "email": "test@example.com",
      "url": "https://api.example.com/v1/users?id=123&type=premium",
      "data": "key1=value1&key2=value2",
      "json": "{\"id\":123,\"name\":\"test\"}"
    }
  }'
```

### 错误测试示例

#### 缺少 appId
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "secret": "some_secret",
    "params": {
      "test": "value"
    }
  }'
```
**预期响应：**
```json
{
  "success": false,
  "message": "Missing required parameters: appId and secret"
}
```

#### 缺少 secret
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "test-app",
    "params": {
      "test": "value"
    }
  }'
```

#### 空请求体
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{}'
```

#### 无效 JSON
```bash
curl -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{"appId": "test", "secret": "secret", invalid json'
```

### 批量测试脚本

#### Bash 批量测试
```bash
#!/bin/bash

# 测试用例数组
declare -a test_cases=(
  '{"appId":"test1","secret":"secret1","params":{"action":"login"}}'
  '{"appId":"test2","secret":"secret2","params":{"userId":"123","action":"query"}}'
  '{"appId":"test3","secret":"secret3","params":{"orderId":"ORDER_001","amount":"99.99"}}'
)

echo "开始批量测试..."

for i in "${!test_cases[@]}"; do
  echo "测试用例 $((i+1)):"
  curl -X POST "http://localhost:8689/api/signature/generate" \
    -H "Content-Type: application/json" \
    -d "${test_cases[$i]}" | jq '.'
  echo "---"
done
```

#### Python 批量测试
```python
import requests
import json

test_cases = [
    {
        "appId": "python-test-1",
        "secret": "python_secret_1",
        "params": {"action": "login", "userId": "py_user_1"}
    },
    {
        "appId": "python-test-2", 
        "secret": "python_secret_2",
        "params": {"orderId": "PY_ORDER_001", "amount": "199.99"}
    },
    {
        "appId": "python-test-3",
        "secret": "python_secret_3",
        "params": {"fileId": "file_123", "action": "download"}
    }
]

url = "http://localhost:8689/api/signature/generate"

for i, test_case in enumerate(test_cases, 1):
    print(f"测试用例 {i}:")
    response = requests.post(url, json=test_case)
    print(json.dumps(response.json(), indent=2, ensure_ascii=False))
    print("---")
```

#### JavaScript/Node.js 测试
```javascript
const axios = require('axios');

const testCases = [
  {
    appId: "js-test-1",
    secret: "js_secret_1", 
    params: { action: "login", deviceId: "js_device_1" }
  },
  {
    appId: "js-test-2",
    secret: "js_secret_2",
    params: { endpoint: "/api/users", method: "GET" }
  }
];

const url = "http://localhost:8689/api/signature/generate";

async function runTests() {
  for (let i = 0; i < testCases.length; i++) {
    console.log(`测试用例 ${i + 1}:`);
    try {
      const response = await axios.post(url, testCases[i]);
      console.log(JSON.stringify(response.data, null, 2));
    } catch (error) {
      console.error('错误:', error.response?.data || error.message);
    }
    console.log('---');
  }
}

runTests();
```

### Postman 集合示例

#### 基础请求
```json
{
  "info": {
    "name": "签名生成接口测试",
    "description": "测试 /api/signature/generate 接口"
  },
  "item": [
    {
      "name": "基础签名生成",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"appId\": \"postman-test\",\n  \"secret\": \"postman_secret\"\n}"
        },
        "url": {
          "raw": "http://localhost:8689/api/signature/generate",
          "host": ["localhost"],
          "port": "8689",
          "path": ["api", "signature", "generate"]
        }
      }
    }
  ]
}
```

### 性能测试

#### 压力测试 (使用 ab)
```bash
# 安装 Apache Bench
# Ubuntu: sudo apt-get install apache2-utils
# macOS: brew install httpie (或使用系统自带的 ab)

# 创建测试数据文件
echo '{
  "appId": "perf-test",
  "secret": "perf_secret",
  "params": {
    "userId": "perf_user",
    "action": "performanceTest"
  }
}' > test_data.json

# 执行压力测试：100个请求，并发度10
ab -n 100 -c 10 -p test_data.json -T application/json \
  http://localhost:8689/api/signature/generate
```

#### 使用 wrk 进行压力测试
```bash
# 创建 Lua 脚本文件 test.lua
cat > test.lua << 'EOF'
wrk.method = "POST"
wrk.body = '{"appId":"wrk-test","secret":"wrk_secret","params":{"test":"value"}}'
wrk.headers["Content-Type"] = "application/json"
EOF

# 运行压力测试：2线程，10连接，持续30秒
wrk -t2 -c10 -d30s -s test.lua http://localhost:8689/api/signature/generate
```

### 实际使用流程

#### 1. 获取签名参数
```bash
# 第一步：生成签名
SIGNATURE_RESPONSE=$(curl -s -X POST "http://localhost:8689/api/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "my-app",
    "secret": "my-secret",
    "params": {
      "userId": "12345",
      "action": "getUserProfile"
    }
  }')

echo "签名响应: $SIGNATURE_RESPONSE"
```

#### 2. 提取签名数据
```bash
# 提取签名数据
SIGNATURE_DATA=$(echo "$SIGNATURE_RESPONSE" | jq -r '.data')
APP_ID=$(echo "$SIGNATURE_DATA" | jq -r '.appId')
TIMESTAMP=$(echo "$SIGNATURE_DATA" | jq -r '.timestamp')
NONCE=$(echo "$SIGNATURE_DATA" | jq -r '.nonce')
SIGNATURE=$(echo "$SIGNATURE_DATA" | jq -r '.sign')

echo "提取的签名信息:"
echo "  AppID: $APP_ID"
echo "  时间戳: $TIMESTAMP"  
echo "  Nonce: $NONCE"
echo "  签名: $SIGNATURE"
```

#### 3. 使用签名调用业务接口
```bash
# 第二步：使用签名调用实际的业务接口
curl -X GET "http://your-business-api.com/api/user/profile" \
  -H "X-App-Id: $APP_ID" \
  -H "X-Timestamp: $TIMESTAMP" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIGNATURE" \
  -G -d "userId=12345"
```

这些示例涵盖了签名生成接口的各种使用场景，从基础用法到复杂的业务场景，以及错误处理和性能测试。开发人员可以根据自己的需求选择合适的示例进行参考和测试。

## 联系方式

- 项目维护者：bgpay
- 邮箱：support@bgpay.com
- 项目地址：https://github.com/bgpay/signature-service 