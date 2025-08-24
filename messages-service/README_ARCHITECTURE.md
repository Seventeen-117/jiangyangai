# 消息服务架构设计文档

## 架构概述

消息服务采用职责分离的架构设计，将消息发送执行和分布式事务管理完全分离，提高系统的可维护性、可扩展性和性能。

## 核心组件

### 1. EnhancedMessageServiceImp - 消息发送执行器

**主要职责：**
- 消息参数验证和基础检查
- 消息中间件路由选择
- 具体消息发送策略执行
- 自定义topic的直接发送
- 消息发送结果处理

**技术特点：**
- 支持多种消息中间件（RocketMQ、Kafka、RabbitMQ）
- 支持多种消息类型（普通、延迟、顺序、事务等）
- 异步回调机制
- 重试和错误处理

### 2. MessageSagaStateMachine - Saga分布式事务管理器

**主要职责：**
- Saga事务状态管理
- 事务补偿机制
- 消息一致性保证
- 事务协调和回滚
- 分布式事务生命周期管理

**技术特点：**
- 基于状态机的Saga模式
- 支持事务回滚和补偿
- 事务超时和重试机制
- 分布式事务状态持久化

## 职责分工

### EnhancedMessageServiceImp 负责：

1. **消息参数验证**
   ```java
   private void validateMessageParameters(String messageId, String messageType, Map<String, Object> parameters)
   ```

2. **事务使用判断**
   ```java
   private boolean shouldUseSagaTransaction(Map<String, Object> parameters)
   ```

3. **直接消息发送**
   ```java
   private boolean executeMessageDirectly(MessageServiceType messageServiceType, String messageId, String messageBody, Map<String, Object> parameters)
   ```

4. **消息中间件路由**
   ```java
   public boolean sendToRocketMQ(...)
   public boolean sendToKafka(...)
   public boolean sendToRabbitMQ(...)
   ```

### MessageSagaStateMachine 负责：

1. **Saga事务执行**
   ```java
   public void executeMessageSendSaga(String messageId, String messageBody, String messageType)
   ```

2. **事务状态管理**
   ```java
   private void startSaga(String messageId)
   private void commitSaga(SagaContext context)
   private void rollbackSaga(SagaContext context, Exception e)
   ```

3. **补偿机制**
   ```java
   private void executeCompensationStep(SagaContext context)
   ```

## 协作模式

### 模式1：Saga事务管理（默认）

```java
// 请求参数
{
    "messageId": "order-123",
    "messageType": "ROCKETMQ",
    "messageBody": "订单创建消息",
    "useSaga": true  // 明确使用Saga事务
}

// 执行流程
1. EnhancedMessageServiceImp.validateMessageParameters() - 参数验证
2. EnhancedMessageServiceImp.shouldUseSagaTransaction() - 判断使用Saga
3. EnhancedMessageServiceImp.executeMessageWithSaga() - 委托给Saga
4. MessageSagaStateMachine.executeMessageSendSaga() - 执行Saga事务
```

### 模式2：直接发送（绕过Saga）

```java
// 请求参数
{
    "messageId": "log-456",
    "messageType": "KAFKA",
    "messageBody": "系统日志",
    "topic": "system-logs",  // 自定义topic
    "useSaga": false  // 明确不使用Saga
}

// 执行流程
1. EnhancedMessageServiceImp.validateMessageParameters() - 参数验证
2. EnhancedMessageServiceImp.shouldUseSagaTransaction() - 判断不使用Saga
3. EnhancedMessageServiceImp.executeMessageDirectly() - 直接发送
4. EnhancedMessageServiceImp.sendMessageWithCustomTopic() - 执行发送
```

## 配置参数

### Saga事务配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `useSaga` | Boolean | true | 是否使用Saga事务 |
| `sagaTimeout` | Integer | 30000 | Saga事务超时时间（毫秒） |
| `sagaRetries` | Integer | 3 | Saga事务重试次数 |
| `messageType` | String | - | 消息类型，TRANSACTION强制使用Saga |

### 直接发送配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `topic` | String | - | 自定义topic（必填） |
| `useSaga` | Boolean | false | 明确不使用Saga事务 |
| `callbackUrl` | String | - | 回调URL |
| `callbackType` | String | HTTP | 回调类型 |

## 使用场景

### 使用Saga事务的场景

1. **订单支付流程**
   - 需要保证订单状态和支付状态的一致性
   - 支持事务回滚和补偿

2. **库存扣减**
   - 需要保证订单创建和库存扣减的原子性
   - 失败时自动回滚库存

3. **用户注册**
   - 需要保证用户信息和权限分配的一致性
   - 支持分步骤的事务处理

### 直接发送的场景

1. **日志记录**
   - 简单的日志消息，无需事务保证
   - 追求高性能和低延迟

2. **通知推送**
   - 用户通知、系统告警等
   - 失败影响较小

3. **数据同步**
   - 非关键数据的同步操作
   - 支持重试机制

4. **监控指标**
   - 性能监控、业务统计等
   - 实时性要求高

## 性能对比

| 指标 | Saga事务模式 | 直接发送模式 |
|------|-------------|-------------|
| 延迟 | 较高（事务协调开销） | 较低（直接发送） |
| 吞吐量 | 中等（事务状态管理） | 较高（无事务开销） |
| 一致性 | 强一致性 | 最终一致性 |
| 资源消耗 | 较高（事务状态） | 较低（无状态） |
| 复杂度 | 高（状态机管理） | 低（简单发送） |

## 最佳实践

### 1. 选择合适的事务模式

- **强一致性要求**：使用Saga事务模式
- **高性能要求**：使用直接发送模式
- **混合场景**：根据消息类型动态选择

### 2. 参数配置建议

```java
// 关键业务消息
{
    "useSaga": true,
    "sagaTimeout": 60000,
    "sagaRetries": 5
}

// 非关键消息
{
    "topic": "non-critical-topic",
    "useSaga": false,
    "callbackUrl": "http://callback.example.com"
}
```

### 3. 监控和告警

- 监控Saga事务的成功率和耗时
- 监控直接发送的成功率和延迟
- 设置事务超时和重试告警
- 监控消息积压和消费延迟

## 扩展性设计

### 1. 新增消息中间件

只需在 `EnhancedMessageServiceImp` 中添加新的发送方法：
```java
public boolean sendToNewMQ(String messageId, String topic, String messageBody, Map<String, Object> parameters)
```

### 2. 新增Saga事务步骤

只需在 `MessageSagaStateMachine` 中添加新的状态和步骤：
```java
private void executeNewStep(SagaContext context)
```

### 3. 新增消息类型

在 `MessageType` 枚举中添加新类型，并在相应的发送方法中实现具体逻辑。

## 总结

通过职责分离的架构设计，消息服务实现了：

1. **清晰的职责边界**：消息发送和事务管理各司其职
2. **灵活的协作模式**：支持Saga事务和直接发送两种模式
3. **良好的扩展性**：便于添加新的消息中间件和事务步骤
4. **优秀的性能**：根据业务需求选择合适的事务模式
5. **高可维护性**：代码结构清晰，便于理解和修改

这种架构设计既满足了复杂业务场景的分布式事务需求，又支持了高性能的简单消息发送场景，是一个非常合理和实用的设计。
