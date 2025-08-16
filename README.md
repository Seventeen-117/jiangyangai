# 江阳AI微服务架构文档

## 项目概述

本项目是一个基于Spring Cloud Gateway的微服务架构，包含网关服务（gateway-service）、签名验证服务（signature-service）和AI服务（bgai-service）。项目实现了完整的API签名验证、JWT认证、SSO单点登录等功能。

## 服务架构

```
客户端 → 网关(路由+基础过滤) → signature-service(验证逻辑) → 业务服务
```

### 服务列表

- **gateway-service** (端口8080): API网关服务，负责路由和基础过滤
- **signature-service** (端口8689): 签名验证服务，提供完整的认证和授权功能
- **bgai-service** (端口8688): AI服务，提供业务功能
- **aiAgent-service** (端口8690): AI智能代理服务，集成多种AI模型，提供统一聊天接口
- **messages-service** (端口8687): 消息服务，处理消息队列和事务事件
- **deepSearch-service** (端口8691): 深度搜索服务，处理图片识别、AI逻辑分析和数据计算任务
- **base-service**: 基础服务模块，提供多数据源、通用配置等基础功能

## 目录

1. [网关架构优化](#网关架构优化)
2. [SSO单点登录](#sso单点登录)
3. [签名验证功能](#签名验证功能)
4. [数据计算服务](#数据计算服务)
5. [API使用指南](#api使用指南)
6. [统计监控](#统计监控)
7. [部署配置](#部署配置)

---

## 网关架构优化

### 职责分工

#### 网关职责 (Gateway Service)
1. **路由转发** - 将请求路由到正确的服务
2. **基础过滤** - 限流、熔断、日志、基础防御
3. **请求预处理** - 添加请求头、转换请求格式

#### signature-service职责
1. **API Key验证** - 验证API密钥的有效性
2. **签名验证** - 验证请求签名的正确性
3. **权限验证** - 验证用户权限和访问控制
4. **复杂认证逻辑** - JWT验证、OAuth2.0等
5. **安全策略** - 防重放攻击、时间戳验证等

### 网关过滤器配置

#### 保留的过滤器
```java
// 基础功能过滤器
- GlobalLogFilter          // 全局日志
- RateLimitFilter          // 限流
- CircuitBreakerFilter     // 熔断
- DefensiveFilter          // 基础防御
- LoggingFilter           // 请求日志
- ValidationFilter        // 验证过滤器（调用signature-service）
```

#### 路由配置示例
```yaml
gateway:
  routes:
    # 签名验证服务路由
    - id: signature-service
      uri: http://localhost:8689
      predicates:
        - Path=/api/signature/**,/api/keys/**,/api/auth/**,/api/sso/**
    
    # 需要验证的bgai服务路由
    - id: bgai-service-validated
      uri: http://localhost:8688
      predicates:
        - Path=/api/chatGatWay-internal/**,/api/admin/**
      filters:
        - ValidationFilter  # 自定义验证过滤器
        - AddRequestHeader=X-Gateway-Source, gateway-service
        - AddResponseHeader=X-Gateway-Response, true
    
    # 公开的bgai服务路由
    - id: bgai-service-public
      uri: http://localhost:8688
      predicates:
        - Path=/api/bgai/**,/api/public/**
```

### 验证流程

1. **请求到达gateway-service** (端口8080)
2. **路由匹配**: 根据路径匹配相应的路由
3. **ValidationFilter执行**: 对需要验证的路径调用signature-service进行验证
4. **验证通过**: gateway-service将请求转发到目标服务
5. **验证失败**: 直接返回401错误，不转发

---

## SSO单点登录

### 核心组件

- **SsoService**: 主要的SSO服务类，处理OAuth 2.0流程
- **AuthorizationCodeMapper**: 授权码数据访问层
- **OAuthClientMapper**: OAuth客户端数据访问层
- **SsoUserMapper**: SSO用户数据访问层
- **PasswordUtils**: 密码加密工具类
- **JwtUtils**: JWT令牌工具类

### 数据库表结构

#### authorization_code（授权码表）
```sql
CREATE TABLE `authorization_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(255) NOT NULL COMMENT '授权码',
    `client_id` VARCHAR(255) NOT NULL COMMENT '客户端ID',
    `user_id` VARCHAR(255) NOT NULL COMMENT '用户ID',
    `redirect_uri` VARCHAR(500) NOT NULL COMMENT '重定向URI',
    `scope` VARCHAR(500) DEFAULT NULL COMMENT '权限范围',
    `state` VARCHAR(255) DEFAULT NULL COMMENT '状态参数',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `used` BOOLEAN DEFAULT FALSE COMMENT '是否已使用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
);
```

#### oauth_client（OAuth客户端表）
```sql
CREATE TABLE `oauth_client` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `client_id` VARCHAR(255) NOT NULL COMMENT '客户端ID',
    `client_secret` VARCHAR(500) NOT NULL COMMENT '客户端密钥',
    `client_name` VARCHAR(255) NOT NULL COMMENT '客户端名称',
    `redirect_uri` VARCHAR(500) NOT NULL COMMENT '重定向URI',
    `scope` VARCHAR(500) DEFAULT NULL COMMENT '权限范围',
    `grant_types` VARCHAR(500) DEFAULT NULL COMMENT '授权类型',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_id` (`client_id`)
);
```

#### sso_user（SSO用户表）
```sql
CREATE TABLE `sso_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(255) NOT NULL COMMENT '用户ID',
    `username` VARCHAR(255) NOT NULL COMMENT '用户名',
    `password` VARCHAR(500) NOT NULL COMMENT '密码',
    `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
    `nickname` VARCHAR(255) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像',
    `role` VARCHAR(100) DEFAULT 'USER' COMMENT '角色',
    `department` VARCHAR(255) DEFAULT NULL COMMENT '部门',
    `position` VARCHAR(255) DEFAULT NULL COMMENT '职位',
    `phone` VARCHAR(50) DEFAULT NULL COMMENT '手机号',
    `gender` VARCHAR(20) DEFAULT NULL COMMENT '性别',
    `status` VARCHAR(50) DEFAULT 'ACTIVE' COMMENT '状态',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `locked` BOOLEAN DEFAULT FALSE COMMENT '是否锁定',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(100) DEFAULT NULL COMMENT '最后登录IP',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_username` (`username`)
);
```

### 管理接口

#### 授权码管理
```bash
# 创建授权码
POST /api/sso/admin/auth-code
{
    "clientId": "default-client",
    "userId": "user-001",
    "redirectUri": "http://localhost:8080/api/sso/callback",
    "scope": "read write",
    "state": "random-state"
}

# 清理过期授权码
DELETE /api/sso/admin/auth-code/cleanup
```

#### OAuth客户端管理
```bash
# 创建客户端
POST /api/sso/admin/client
{
    "clientId": "new-client",
    "clientSecret": "secret123",
    "clientName": "新客户端",
    "redirectUri": "http://localhost:3000/callback",
    "scope": "read write",
    "grantTypes": "authorization_code refresh_token password"
}

# 获取所有客户端
GET /api/sso/admin/clients
```

#### 用户管理
```bash
# 创建用户
POST /api/sso/admin/user
{
    "username": "newuser",
    "password": "password123",
    "email": "newuser@example.com",
    "nickname": "新用户",
    "role": "USER",
    "department": "IT",
    "position": "Developer"
}

# 更新用户密码
PUT /api/sso/admin/user/user-001/password
{
    "password": "newpassword123"
}

# 锁定/解锁用户
PUT /api/sso/admin/user/user-001/lock
{
    "locked": true
}
```

---

## 签名验证功能

### 签名算法

采用HMAC-SHA256签名算法：

1. **参数排序**: 将所有参数按字典序排序
2. **构造签名字符串**: `key1=value1&key2=value2&...`
3. **计算签名**: 使用HMAC-SHA256算法和密钥计算签名
4. **返回结果**: 十六进制字符串

### 签名参数

- **appId**: 应用ID（必需）
- **timestamp**: 时间戳（必需，毫秒级）
- **nonce**: 随机字符串（必需，防重放攻击）
- **sign**: 签名值（必需）
- **params**: 业务参数（可选）

### 验证规则

1. **时间戳验证**: 时间戳不能超过5分钟
2. **Nonce验证**: Nonce不能重复使用（30分钟内）
3. **签名验证**: 签名必须正确
4. **参数完整性**: 所有必需参数必须存在

---

## 数据计算服务

### 服务概述

**deepSearch-service** 是一个专门处理图片识别、AI逻辑分析和数据计算任务的微服务。该服务通过集成AI代理和BGAI服务，实现智能化的数据处理流程。

### 核心功能

1. **图片上传与识别**
   - 支持多图片上传（Base64或URL）
   - 异步发送图片到BGAI服务进行内容识别
   - 自动生成SQL语句并存储到MySQL数据库

2. **AI逻辑分析**
   - 自动请求AI代理服务分析业务逻辑
   - 生成完整的逻辑流程图和文字描述
   - 支持多种业务类型的智能分析

3. **数据计算处理**
   - 基于BGAI服务返回的计算规则执行数据计算
   - 支持同步和异步计算模式
   - 提供计算任务状态跟踪和取消功能

### 技术架构

- **Spring Boot 3.2.5**: 核心框架
- **Spring Cloud**: 服务发现和配置管理
- **Dubbo**: 服务间RPC通信
- **MyBatis Plus**: 数据持久化
- **Redis**: 缓存和会话管理
- **MySQL**: 数据存储

### 服务端口

- **开发环境**: 8691
- **测试环境**: 8691
- **生产环境**: 8691

### 主要接口

- `POST /api/calculation/upload`: 图片上传和识别
- `POST /api/calculation/execute`: 执行数据计算
- `GET /api/calculation/status/{taskId}`: 查询计算状态
- `POST /api/calculation/cancel/{taskId}`: 取消计算任务

### 业务流程

1. **图片上传** → 网关路由 → 认证服务验证
2. **异步识别** → BGAI服务处理图片 → 生成SQL并存储
3. **AI分析** → 请求AI代理 → 生成逻辑流程图
4. **逻辑提交** → 通过Dubbo发送到BGAI服务
5. **数据计算** → 执行计算规则 → 返回结果

---

## API使用指南

### 1. 生成API密钥

```bash
POST http://localhost:8689/api/keys/generate
Content-Type: application/json

{
  "clientId": "test-client-001",
  "clientName": "测试客户端",
  "description": "用于测试的API密钥"
}
```

**响应示例:**
```json
{
  "apiKey": "sk-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
  "clientId": "test-client-001",
  "clientName": "测试客户端",
  "description": "用于测试的API密钥",
  "createdAt": "2024-01-15T10:30:00",
  "expiresAt": "2025-01-15T10:30:00",
  "active": true
}
```

### 2. 生成签名

```bash
POST http://localhost:8689/api/signature/generate
Content-Type: application/json

{
  "appId": "test-app-001",
  "secret": "your-secret-key",
  "params": {
    "userId": "user123",
    "action": "getUserInfo",
    "data": "test-data"
  }
}
```

**响应示例:**
```json
{
  "success": true,
  "message": "Signature generated successfully",
  "data": {
    "appId": "test-app-001",
    "timestamp": "1703123456789",
    "nonce": "a1b2c3d4e5f678901234567890123456",
    "sign": "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567",
    "userId": "user123",
    "action": "getUserInfo",
    "data": "test-data"
  }
}
```

### 3. 验证签名

```bash
POST http://localhost:8689/api/signature/verify
Content-Type: application/json

{
  "appId": "test-app-001",
  "timestamp": "1703123456789",
  "nonce": "a1b2c3d4e5f678901234567890123456",
  "sign": "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567",
  "params": {
    "userId": "user123",
    "action": "getUserInfo",
    "data": "test-data"
  }
}
```

### 4. 通过网关访问

#### 需要验证的接口（通过ValidationFilter）
```bash
POST http://localhost:8080/api/chatGatWay-internal
X-API-Key: b9c4046cb9124dc6883376f86a6bf9a4
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "message": "test"
}
```

#### 无需验证的接口
```bash
POST http://localhost:8080/api/signature/generate
Content-Type: application/json

{
  "appId": "test-app-123",
  "secret": "test-secret",
  "params": {"message": "test"}
}
```

### SSO认证流程

#### 1. 授权码流程
```bash
# 1. 创建授权码
POST http://localhost:8080/api/sso/admin/auth-code
{
  "clientId": "default-client",
  "userId": "user-001",
  "redirectUri": "http://localhost:8080/api/sso/callback",
  "scope": "read write",
  "state": "random-state"
}

# 2. 使用授权码交换令牌
POST http://localhost:8080/api/sso/token
{
  "grant_type": "authorization_code",
  "code": "generated-auth-code",
  "client_id": "default-client",
  "client_secret": "your-client-secret"
}
```

#### 2. 密码授权流程
```bash
POST http://localhost:8080/api/sso/token
{
  "grant_type": "password",
  "username": "admin",
  "password": "123456",
  "client_id": "default-client",
  "client_secret": "your-client-secret"
}
```

#### 3. 刷新令牌流程
```bash
POST http://localhost:8080/api/sso/token
{
  "grant_type": "refresh_token",
  "refresh_token": "your-refresh-token",
  "client_id": "default-client",
  "client_secret": "your-client-secret"
}
```

---

## 统计监控

### 事件驱动架构

签名验证统计功能采用事件驱动架构：

- **SignatureVerificationEvent**: 签名验证事件
- **SignatureVerificationEventListener**: 事件监听器，收集统计信息
- **SignatureStatisticsService**: 统计服务，提供统计信息查询接口

### 统计API

#### 1. 获取应用统计信息
```bash
GET /api/signature/stats/app/{appId}
```

**响应示例:**
```json
{
    "appId": "test-app-001",
    "successCount": 150,
    "failureCount": 5,
    "replayAttackCount": 2,
    "totalCount": 155,
    "failureRate": 0.032,
    "averageVerificationTime": 45
}
```

#### 2. 获取所有应用统计信息
```bash
GET /api/signature/stats/all
```

#### 3. 重置统计信息
```bash
# 重置特定应用
DELETE /api/signature/stats/app/{appId}

# 重置所有应用
DELETE /api/signature/stats/all
```

### 告警机制

#### 失败率告警
当失败率超过10%时记录错误日志

#### 重放攻击告警
当重放攻击次数超过5次时记录错误日志

#### 性能告警
当验证时间超过1秒时记录警告日志

---

## 部署配置

### 1. 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/signature_service
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 2. SSO配置

```yaml
sso:
  client-id: "default-client"
  client-secret: "your-client-secret"
  redirect-uri: "http://localhost:8080/api/sso/callback"
  security:
    jwt-secret: "your-super-secret-jwt-key-here-must-be-at-least-256-bits-long"
    jwt-expiration: 3600000
    refresh-token-expiration: 2592000000
```

### 3. 网关配置

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      routes:
        - id: signature-service
          uri: http://localhost:8689
          predicates:
            - Path=/api/signature/**,/api/keys/**,/api/auth/**,/api/sso/**
        - id: bgai-service-validated
          uri: http://localhost:8688
          predicates:
            - Path=/api/chatGatWay-internal/**,/api/admin/**
          filters:
            - ValidationFilter
```

### 4. 签名服务配置

```yaml
server:
  port: 8689

signature:
  enabled: true
  timestamp-expire-seconds: 300
  nonce-cache-expire-seconds: 1800
  
feign:
  client:
    config:
      bgai-service:
        url: http://localhost:8688
```

## 安全特性

### 1. 密码安全
- **BCrypt加密**: 使用BCrypt算法加密密码
- **盐值**: 自动生成随机盐值
- **验证**: 安全的密码验证机制

### 2. 授权码安全
- **一次性使用**: 授权码只能使用一次
- **过期机制**: 授权码10分钟后自动过期
- **客户端验证**: 验证授权码与客户端的关联

### 3. 令牌安全
- **JWT签名**: 使用HMAC-SHA256签名
- **过期检查**: 自动检查令牌过期时间
- **黑名单**: 支持令牌撤销

### 4. 签名安全
- **防重放攻击**: Nonce机制防止重放攻击
- **时间戳验证**: 防止过期请求
- **参数完整性**: 确保请求参数完整性

## 最佳实践

### 1. 安全建议
- **强密码策略**: 实施强密码要求
- **定期清理**: 定期清理过期的授权码
- **监控日志**: 监控异常登录行为
- **HTTPS**: 使用HTTPS传输敏感数据

### 2. 性能优化
- **数据库索引**: 为常用查询字段添加索引
- **连接池**: 配置合适的数据库连接池
- **缓存**: 对频繁查询的数据进行缓存

### 3. 监控运维
- **日志监控**: 监控应用日志和错误率
- **性能监控**: 监控接口响应时间
- **资源监控**: 监控CPU、内存使用情况
- **告警机制**: 配置合适的告警阈值

## 常见问题

### 1. 服务未启动
- 确保signature-service在端口8689启动
- 确保gateway-service在端口8080启动
- 确保bgai-service在端口8688启动

### 2. 签名验证失败
- 检查appId和secret是否正确
- 检查时间戳是否在有效期内
- 检查nonce是否重复使用
- 检查参数排序是否正确

### 3. 网关访问失败
- 确保网关路由配置正确
- 检查网关日志中的错误信息
- 确认所有服务都已正确启动

### 4. JWT令牌问题
- 检查JWT密钥配置
- 验证令牌是否过期
- 确认令牌格式正确

## 总结

本项目实现了完整的微服务架构，包括：

1. **清晰的职责分工**: 网关专注路由，signature-service专注验证
2. **完整的OAuth 2.0流程**: 授权码、密码、刷新令牌授权
3. **安全的签名验证**: HMAC-SHA256签名算法
4. **灵活的用户管理**: 支持用户、客户端、权限管理
5. **完善的监控统计**: 详细的验证统计和告警机制
6. **高性能架构**: 事件驱动、缓存机制、异步处理

这个架构确保了系统的安全性、可靠性和可维护性，为企业级应用提供了完整的解决方案。

---

## Dubbo 微服务通信框架

### 🚀 升级概述

本项目已完成从 Feign HTTP 调用到 Apache Dubbo RPC 调用的完整迁移，实现了高性能的微服务间通信。

### 💎 核心价值

#### 🚀 性能提升
- **响应时间降低 30-50%**: 二进制协议减少序列化开销
- **吞吐量提升 2-3倍**: 长连接复用和高效序列化
- **资源使用优化 20-30%**: 更少的CPU和内存消耗

#### 🛡️ 服务治理增强
- **类型安全**: 编译时接口一致性检查
- **智能负载均衡**: 支持多种负载均衡算法
- **精细化监控**: 方法级调用统计和链路追踪
- **容错能力**: 熔断器、重试、降级等机制

### 🏗️ 技术架构

#### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                    江阳AI微服务生态系统                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐ Dubbo RPC ┌─────────────────┐              │
│  │   bgai-service  │◄─────────►│ signature-service│              │
│  │   (消费者/提供者) │           │   (提供者)      │              │
│  │   Port: 8688    │           │   Port: 8689    │              │
│  │   Dubbo: 20880  │           │   Dubbo: 20881  │              │
│  └─────────────────┘           └─────────────────┘              │
│           │                              │                      │
│           │                              │                      │
│  ┌─────────────────┐           ┌─────────────────┐              │
│  │  gateway-service│           │   dubbo-api     │              │
│  │   (API网关)     │           │   (公共接口)     │              │
│  │   Port: 8080    │           │                 │              │
│  └─────────────────┘           └─────────────────┘              │
│           │                              │                      │
│           └──────────────┬───────────────┘                      │
│                          │                                      │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────┐    │
│  │     Nacos       │   │     Dubbo       │   │    MySQL    │    │
│  │ (注册中心/配置)   │   │     Admin       │   │   (数据库)   │    │
│  │   Port: 8848    │   │   Port: 7001    │   │ Port: 3306  │    │
│  └─────────────────┘   └─────────────────┘   └─────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 技术栈

| 组件 | 技术选型 | 版本 | 说明 |
|------|----------|------|------|
| RPC框架 | Apache Dubbo | 3.2.8 | 高性能RPC通信框架 |
| 注册中心 | Nacos | 2.0+ | 服务注册发现和配置管理 |
| 序列化 | Hessian2 | 4.0.66 | 高效二进制序列化 |
| 负载均衡 | Dubbo内置 | - | 多种算法支持 |
| 服务治理 | Dubbo Admin | 3.2.8 | 可视化管理平台 |
| 监控追踪 | Micrometer | - | 指标收集和监控 |

### 📦 模块结构

```
jiangyangai/
├── bgai-service/              # 核心业务服务
├── signature-service/         # 签名验证服务
├── gateway-service/           # API网关服务
├── dubbo-api/                 # 公共API接口定义
│   ├── common/                # 通用模型
│   ├── signature/             # 签名服务接口
│   └── auth/                  # 认证服务接口
└── test-dubbo-integration.sh  # 测试脚本
```

### 🎯 核心接口设计

#### SignatureService 接口

```java
public interface SignatureService {
    // 基础功能
    Result<SignatureResponse> generateSignature(SignatureRequest request);
    Result<Boolean> verifySignature(ValidationRequest request);
    
    // 高级功能
    Result<List<Boolean>> batchVerifySignature(List<ValidationRequest> requests);
    CompletableFuture<Result<Boolean>> verifySignatureAsync(ValidationRequest request);
    Result<Boolean> verifySignatureQuick(ValidationRequest request);
    
    // 工具功能
    Result<SignatureResponse> generateExampleSignature(String appId, String secret);
    Result<SignatureStatsResponse> getSignatureStats(String appId);
}
```

#### 统一响应模型

```java
public class Result<T> {
    private int code;           // 响应码
    private String message;     // 响应消息
    private T data;            // 响应数据
    private long timestamp;     // 时间戳
    private String traceId;     // 链路追踪ID
}
```

### ⚙️ Dubbo 配置

#### 核心配置

```yaml
dubbo:
  application:
    name: ${spring.application.name}
    version: 1.0.0
  
  registry:
    address: nacos://localhost:8848
    namespace: dubbo
    group: DEFAULT_GROUP
    
  protocol:
    name: dubbo
    port: 20881
    serialization: hessian2
    
  provider:
    timeout: 5000
    retries: 0
    loadbalance: roundrobin
    
  consumer:
    timeout: 5000
    retries: 2
    loadbalance: roundrobin
    cluster: failover
```

#### 性能优化配置

```yaml
dubbo:
  protocol:
    threads: 200        # 业务线程池
    iothreads: 4        # IO线程池
    accepts: 1000       # 最大连接数
    payload: 8388608    # 8MB最大包大小
    
  consumer:
    connections: 4      # 每个提供者连接数
    actives: 200        # 最大并发调用数
```

---

## Dubbo 部署和使用指南

### 🚀 快速开始

#### 1. 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **Nacos**: 2.0+ (服务注册中心)
- **MySQL**: 8.0+ (可选，如需数据库功能)
- **Redis**: 6.0+ (可选，如需缓存功能)

#### 2. 启动 Nacos

```bash
# 使用 Docker 启动 Nacos
docker run -d \
  --name nacos-standalone \
  -e MODE=standalone \
  -e JVM_XMS=512m \
  -e JVM_XMX=512m \
  -p 8848:8848 \
  nacos/nacos-server:latest

# 检查 Nacos 状态
curl http://localhost:8848/nacos
```

访问 Nacos 控制台：http://localhost:8848/nacos
- 用户名：nacos
- 密码：nacos

#### 3. 编译项目

```bash
# 在项目根目录执行
mvn clean compile -DskipTests

# 编译 dubbo-api 模块
cd dubbo-api
mvn clean install -DskipTests
cd ..

# 编译 signature-service
cd signature-service
mvn clean compile -DskipTests
cd ..

# 编译 bgai-service
cd bgai-service
mvn clean compile -DskipTests
cd ..
```

#### 4. 启动服务

##### 4.1 启动 signature-service (Dubbo 提供者)

```bash
cd signature-service
mvn spring-boot:run

# 或者使用 java -jar
mvn clean package -DskipTests
java -jar target/signature-service-1.0.0-Final.jar
```

##### 4.2 启动 bgai-service (Dubbo 消费者)

```bash
cd bgai-service
mvn spring-boot:run

# 或者使用 java -jar
mvn clean package -DskipTests
java -jar target/bgai-service-1.0.0-Final.jar
```

#### 5. 验证部署

##### 5.1 运行自动化测试

```bash
# 给测试脚本执行权限（Linux/macOS）
chmod +x test-dubbo-integration.sh

# 运行测试
./test-dubbo-integration.sh
```

##### 5.2 手动验证

```bash
# 检查服务健康状态
curl http://localhost:8688/actuator/health  # bgai-service
curl http://localhost:8689/actuator/health  # signature-service

# 测试 Dubbo 调用
curl -X POST "http://localhost:8688/api/test/dubbo/signature/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "test-app-001",
    "secret": "test-secret-001",
    "params": {
      "userId": "12345",
      "action": "test"
    }
  }'
```

### 📊 服务端口分配

| 服务 | HTTP端口 | Dubbo端口 | 说明 |
|------|----------|-----------|------|
| bgai-service | 8688 | 20880 | 消费者/提供者 |
| signature-service | 8689 | 20881 | 提供者 |
| gateway-service | 8080 | - | API网关 |
| Nacos | 8848 | - | 注册中心 |

### 🧪 Dubbo 测试接口

bgai-service 提供了完整的 Dubbo 测试接口：

#### 1. 生成签名

```bash
POST /api/test/dubbo/signature/generate
Content-Type: application/json

{
  "appId": "test-app-001",
  "secret": "test-secret-001",
  "params": {
    "userId": "12345",
    "action": "test"
  }
}
```

#### 2. 验证签名

```bash
POST /api/test/dubbo/signature/verify
Content-Type: application/json

{
  "appId": "test-app-001",
  "timestamp": "1703123456789",
  "nonce": "abc123def456",
  "signature": "generated_signature_1703123456789",
  "params": {
    "userId": "12345"
  }
}
```

#### 3. 快速验证签名

```bash
POST /api/test/dubbo/signature/verify-quick
Content-Type: application/json

{
  "appId": "test-app-001",
  "signature": "test_signature",
  "params": {
    "userId": "12345"
  }
}
```

#### 4. 异步验证签名

```bash
POST /api/test/dubbo/signature/verify-async
Content-Type: application/json

{
  "appId": "test-app-001",
  "timestamp": "1703123456789",
  "nonce": "abc123def456",
  "signature": "generated_signature_1703123456789",
  "params": {
    "userId": "12345"
  }
}
```

#### 5. 生成示例签名

```bash
GET /api/test/dubbo/signature/example?appId=test-app-001&secret=test-secret-001
```

#### 6. 获取签名统计

```bash
GET /api/test/dubbo/signature/stats?appId=test-app-001
```

#### 7. 健康检查

```bash
GET /api/test/dubbo/health
```

### 📈 监控和管理

#### 1. 应用监控端点

```bash
# bgai-service 监控
curl http://localhost:8688/actuator/health
curl http://localhost:8688/actuator/metrics
curl http://localhost:8688/actuator/dubbo

# signature-service 监控
curl http://localhost:8689/actuator/health
curl http://localhost:8689/actuator/metrics
```

#### 2. Nacos 服务发现

访问 Nacos 控制台查看服务注册状态：
- URL: http://localhost:8848/nacos
- 服务列表 → 服务管理 → 服务列表

#### 3. 部署 Dubbo Admin（可选）

```bash
# 使用 Docker 部署 Dubbo Admin
docker run -d \
  --name dubbo-admin \
  -p 7001:7001 \
  -e admin.registry.address=nacos://localhost:8848 \
  -e admin.config-center=nacos://localhost:8848 \
  -e admin.metadata-report.address=nacos://localhost:8848 \
  apache/dubbo-admin:latest
```

访问 Dubbo Admin：http://localhost:7001

### 🔄 平滑迁移机制

bgai-service 提供了 SignatureServiceAdapter，支持 Dubbo 和 Feign 之间的平滑切换：

```java
@Autowired
private SignatureServiceAdapter signatureAdapter;

// 使用适配器调用
SignatureResponse response = signatureAdapter.generateSignature(appId, secret, params);
boolean isValid = signatureAdapter.verifySignature(appId, timestamp, nonce, signature, params);
```

#### 配置开关

```yaml
app:
  use-dubbo: true          # true-使用Dubbo，false-使用Feign
  dubbo-fallback: true     # true-启用降级，false-禁用降级
```

### 🐛 故障排除

#### 常见问题

##### 1. 服务注册失败

**症状**: 服务启动后在 Nacos 中看不到注册信息

**解决方案**:
```bash
# 检查 Nacos 连接
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=signature-service

# 检查网络连接
telnet localhost 8848

# 检查配置文件中的注册中心地址
```

##### 2. Dubbo 调用超时

**症状**: 调用时出现超时异常

**解决方案**:
```yaml
# 增加超时时间
dubbo:
  consumer:
    timeout: 10000  # 增加到10秒
  provider:
    timeout: 10000
```

##### 3. 序列化异常

**症状**: 调用时出现序列化错误

**解决方案**:
```java
// 确保 DTO 类实现 Serializable
public class SignatureRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

##### 4. 服务提供者未找到

**症状**: No provider available for the service

**解决方案**:
```bash
# 检查提供者是否启动
curl http://localhost:8689/actuator/health

# 检查消费者配置
dubbo:
  consumer:
    check: false  # 启动时不检查提供者
```

#### 日志配置

##### 启用详细日志

```yaml
logging:
  level:
    org.apache.dubbo: DEBUG
    com.jiangyang.dubbo: DEBUG
    org.apache.dubbo.registry: INFO
    org.apache.dubbo.remoting: INFO
```

##### 查看日志文件

```bash
# 查看服务日志
tail -f signature-service/logs/signature-service.log
tail -f bgai-service/logs/bgai-service.log

# 查看 Dubbo 相关日志
grep "Dubbo" signature-service/logs/signature-service.log
grep "Dubbo" bgai-service/logs/bgai-service.log
```

### 🚀 性能优化

#### 1. 连接池配置

```yaml
dubbo:
  consumer:
    connections: 4      # 每个提供者连接数
    actives: 200        # 最大并发调用数
    
  protocol:
    threads: 200        # 业务线程池
    iothreads: 4        # IO线程池
```

#### 2. 负载均衡策略

```yaml
dubbo:
  consumer:
    loadbalance: leastactive  # 最少活跃调用数
    # 其他策略：
    # roundrobin - 轮询
    # random - 随机
    # consistenthash - 一致性哈希
```

#### 3. 容错机制

```yaml
dubbo:
  consumer:
    cluster: failover   # 失败自动切换
    retries: 2          # 重试次数
```

### 📦 生产部署

#### 1. Docker 部署

##### Dockerfile 示例

```dockerfile
# signature-service/Dockerfile
FROM openjdk:17-jdk-slim

EXPOSE 8689 20881

COPY target/signature-service-1.0.0-Final.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar", \
  "--dubbo.protocol.host=${DUBBO_HOST:localhost}", \
  "--dubbo.registry.address=nacos://${NACOS_HOST:localhost}:8848"]
```

##### Docker Compose

```yaml
version: '3.8'
services:
  nacos:
    image: nacos/nacos-server:latest
    environment:
      - MODE=standalone
    ports:
      - "8848:8848"
    
  signature-service:
    build: ./signature-service
    environment:
      - NACOS_HOST=nacos
      - DUBBO_HOST=signature-service
    ports:
      - "8689:8689"
      - "20881:20881"
    depends_on:
      - nacos
    
  bgai-service:
    build: ./bgai-service
    environment:
      - NACOS_HOST=nacos
      - DUBBO_HOST=bgai-service
    ports:
      - "8688:8688"
      - "20880:20880"
    depends_on:
      - nacos
      - signature-service
```

#### 2. Kubernetes 部署

```yaml
# k8s/signature-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: signature-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: signature-service
  template:
    metadata:
      labels:
        app: signature-service
    spec:
      containers:
      - name: signature-service
        image: jiangyang/signature-service:1.0.0
        ports:
        - containerPort: 8689
        - containerPort: 20881
        env:
        - name: DUBBO_HOST
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
        - name: NACOS_HOST
          value: "nacos-service"
---
apiVersion: v1
kind: Service
metadata:
  name: signature-service
spec:
  selector:
    app: signature-service
  ports:
  - name: http
    port: 8689
    targetPort: 8689
  - name: dubbo
    port: 20881
    targetPort: 20881
```

### 📊 性能指标

#### 预期性能对比

| 指标 | Feign HTTP | Dubbo RPC | 提升幅度 |
|------|------------|-----------|----------|
| 响应时间 | 50-100ms | 30-50ms | **30-50%** |
| 吞吐量 | 1000 TPS | 2000-3000 TPS | **2-3倍** |
| 序列化效率 | JSON | Hessian2 | **3-5倍** |
| 连接复用 | 短连接 | 长连接 | **显著提升** |

### 💰 成本效益分析

#### 实施成本

| 项目 | 人力 | 时间 | 说明 |
|------|------|------|------|
| 方案设计 | 1人 | 2天 | 架构设计和技术选型 |
| 开发实施 | 2人 | 8天 | 代码开发和配置 |
| 测试验证 | 1人 | 3天 | 功能和性能测试 |
| 部署上线 | 1人 | 2天 | 生产环境部署 |
| **总计** | **2-3人** | **15天** | **约3周完成** |

#### 预期收益

- **短期收益（1-3个月）**
  - 响应时间降低30-50%
  - 系统吞吐量提升2-3倍
  - 资源成本节省20-30%

- **长期收益（6-12个月）**
  - 开发效率提升（类型安全）
  - 运维成本降低（更好的监控）
  - 系统稳定性提升（容错机制）

- **ROI计算**
  - 硬件成本节省：20-30%
  - 开发效率提升：25%
  - 运维成本降低：30%
  - **预期ROI：300-500%**

---

## 技术支持

如遇到问题，请按以下步骤排查：

1. **检查服务状态**: 确认所有服务正常启动
2. **检查网络连接**: 确认端口可访问
3. **查看日志**: 检查详细错误信息
4. **运行测试**: 使用提供的测试脚本验证
5. **检查配置**: 确认 Dubbo 和 Nacos 配置正确

### 相关链接

- **Nacos 控制台**: http://localhost:8848/nacos (nacos/nacos)
- **bgai-service**: http://localhost:8688
- **signature-service**: http://localhost:8689
- **Dubbo 测试接口**: http://localhost:8688/api/test/dubbo
- **测试脚本**: `test-dubbo-integration.sh`
