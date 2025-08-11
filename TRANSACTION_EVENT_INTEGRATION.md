# 事务事件集成文档

## 概述

本文档描述了如何在 `messages-service` 和 `bgai-service` 之间集成实时事务事件发送功能。该集成使用 Apache Dubbo 实现服务间通信，在 `MessageSagaStateMachine` 状态机中实时发送事务状态详细信息给业务系统。

## 架构设计

```
messages-service                    bgai-service
     |                                 |
     |                                 |
MessageSagaStateMachine    TransactionEventController
     |                                 |
     |                                 |
TransactionEventSenderService    TransactionEventService
     |                                 |
     |                                 |
TransactionEventService (Dubbo)    TransactionEventDubboServiceImpl
     |                                 |
     |                                 |
     +-------- Dubbo RPC ----------+
```

## 功能特性

### 1. 实时事件发送
- 事务开始事件
- 事务执行事件（Saga步骤）
- 事务提交事件
- 事务回滚事件
- 消息发送/消费事件

### 2. 异步处理
- 使用 `CompletableFuture` 异步发送事件
- 配置专用线程池处理事件发送
- 非阻塞式事件处理

### 3. 容错机制
- Dubbo 服务降级处理
- 事件发送失败时的日志记录
- 优雅的错误处理

## 配置说明

### messages-service 配置

#### 1. 依赖配置 (pom.xml)
```xml
<!-- Dubbo Spring Boot Starter -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
</dependency>

<!-- Dubbo Nacos Registry -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-registry-nacos</artifactId>
</dependency>

<!-- Dubbo API -->
<dependency>
    <groupId>com.jiangyang</groupId>
    <artifactId>dubbo-api</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### 2. 应用配置 (application-dev.yml)
```yaml
# Dubbo配置
dubbo:
  application:
    name: messages-service
    qos-enable: false
  protocol:
    name: dubbo
    port: -1
  registry:
    address: nacos://localhost:8848
    timeout: 10000
  consumer:
    timeout: 5000
    retries: 2
    check: false
  provider:
    timeout: 5000
    retries: 2
```

#### 3. Dubbo 配置
- 在 `MessageServiceAutoConfiguration` 中移除 Feign 相关配置
- `AsyncConfig` 配置异步任务执行器
- 使用 `@DubboReference` 注解引用远程服务

### bgai-service 配置

#### 1. 新增 Dubbo 服务接口
- 实现 `dubbo-api` 中定义的 `TransactionEventService` 接口
- 提供事务事件处理、查询、统计等所有功能
- 支持服务注册和发现

#### 2. 数据模型
- `TransactionEvent` - 事务事件模型（本地和Dubbo各一份）
- `TransactionEventResponse` - 事件响应模型（本地和Dubbo各一份）
- `Result<T>` - 通用响应结果包装类

## 使用方法

### 1. 在 MessageSagaStateMachine 中发送事件

```java
@Autowired
private TransactionEventSenderService transactionEventSenderService;

@GlobalTransactional(name = "message-send-saga", rollbackFor = Exception.class)
public void executeMessageSendSaga(String messageId, String content) {
    String globalTransactionId = RootContext.getXID();
    String transactionId = "msg_send_" + messageId;
    
    try {
        // 发送事务开始事件
        sendTransactionBeginEvent(globalTransactionId, transactionId, "message-send", messageId);
        
        // 执行业务逻辑
        sendMessage(messageId, content);
        
        // 发送消息发送事件
        sendMessageSendEvent(globalTransactionId, transactionId, messageId, content);
        
        // 发送事务提交事件
        sendTransactionCommitEvent(globalTransactionId, transactionId, "message-send", messageId);
        
    } catch (Exception e) {
        // 发送事务回滚事件
        sendTransactionRollbackEvent(globalTransactionId, transactionId, "message-send", messageId, e.getMessage());
        throw e;
    }
}
```

### 2. 事件类型和用途

| 事件类型 | 方法名 | 用途 |
|---------|--------|------|
| 事务开始 | `sendTransactionBeginEvent` | Saga 事务开始时发送 |
| 消息发送 | `sendMessageSendEvent` | 消息发送成功后发送 |
| 消息消费 | `sendMessageConsumeEvent` | 消息消费成功后发送 |
| Saga执行 | `sendSagaExecuteEvent` | Saga 步骤执行时发送 |
| Saga补偿 | `sendSagaCompensateEvent` | Saga 补偿操作时发送 |
| 事务提交 | `sendTransactionCommitEvent` | Saga 事务成功提交时发送 |
| 事务回滚 | `sendTransactionRollbackEvent` | Saga 事务回滚时发送 |

### 3. 事件数据结构

```json
{
  "eventId": "evt_123456",
  "globalTransactionId": "192.168.1.100:8091:123456789",
  "transactionId": "msg_send_msg001",
  "status": "BEGIN",
  "operationType": "TRANSACTION_BEGIN",
  "serviceName": "messages-service",
  "businessType": "message-send",
  "businessId": "msg001",
  "source": "messages-service",
  "description": "事务开始",
  "createTime": "2025-01-27 10:30:00",
  "priority": 1,
  "version": "1.0"
}
```

## 监控和调试

### 1. 日志配置
```yaml
logging:
  level:
    com.jiangyang.messages: DEBUG
    com.jiangyang.messages.feign: DEBUG
    com.jiangyang.messages.saga: DEBUG
```

### 2. 健康检查
- 通过 Spring Boot Actuator 监控服务健康状态
- Dubbo 服务连接状态监控
- 事件发送成功率统计

### 3. 调试技巧
- 启用 Dubbo 详细日志：`dubbo.application.qos-enable: true`
- 查看事件发送详情：`log.debug("事务事件已发送: {}", event)`
- 监控异步任务执行：线程池状态和队列深度

## 部署注意事项

### 1. 环境配置
- 开发环境：`dubbo.registry.address=nacos://localhost:8848`
- 测试环境：`dubbo.registry.address=nacos://test-nacos:8848`
- 生产环境：`dubbo.registry.address=nacos://prod-nacos:8848`

### 2. 网络配置
- 确保 `messages-service` 可以访问 Nacos 注册中心
- 配置适当的超时时间和重试策略
- 使用 Nacos 进行服务发现和注册

### 3. 性能优化
- 调整异步线程池大小
- 配置事件批量发送
- 监控内存使用和 GC 情况

## 故障排除

### 1. 常见问题

#### Dubbo 连接失败
```
错误：No provider available for the service
解决：检查 bgai-service 是否启动并注册到 Nacos，网络连接是否正常
```

#### 事件发送超时
```
错误：Invocation timeout
解决：增加 Dubbo 超时配置，检查网络延迟
```

#### 异步任务执行失败
```
错误：Task rejected by ThreadPoolTaskExecutor
解决：调整线程池大小，增加队列容量
```

### 2. 调试步骤
1. 检查服务启动日志
2. 验证 Nacos 注册中心配置
3. 测试 Dubbo 服务连接
4. 查看事件发送日志
5. 检查 bgai-service 接收日志

## 扩展功能

### 1. 事件持久化
- 将事件存储到数据库
- 支持事件重放和审计
- 实现事件版本控制

### 2. 事件过滤和路由
- 根据事件类型路由到不同服务
- 实现事件优先级处理
- 支持事件订阅和推送

### 3. 监控和告警
- 集成 Prometheus + Grafana
- 设置事件发送失败告警
- 实现 SLA 监控

## 总结

通过本集成方案，`messages-service` 可以实时向 `bgai-service` 发送详细的事务状态事件，实现了：

1. **实时性**：事件在 Saga 执行过程中实时发送
2. **可靠性**：异步处理 + 容错机制
3. **可观测性**：完整的事件追踪和日志记录
4. **可扩展性**：支持多种事件类型和业务场景

该集成为分布式事务监控、业务审计和系统运维提供了强有力的支持。
