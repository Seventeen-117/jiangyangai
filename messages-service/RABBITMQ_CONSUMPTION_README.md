# RabbitMQ 消息消费服务使用说明

## 概述

本服务实现了基于配置的自动RabbitMQ消息消费功能，支持推模式和拉模式消费，手动确认、死信队列、延迟队列、优先级队列等特性，能够根据配置自动创建和管理RabbitMQ消费者。

## 特性

### 1. 消费模式 (ConsumeMode)
- **PUSH模式**: 消费者注册回调函数，RabbitMQ主动推送消息
- **PULL模式**: 消费者主动拉取消息，按需控制拉取频率

### 2. 确认机制 (AckMode)
- **AUTO确认**: 消息投递后立即确认，适合非关键业务
- **MANUAL确认**: 消息处理成功后手动确认，确保可靠性

### 3. 消费能力
- **单消费者**: 保证消息顺序，适合有状态依赖场景
- **多消费者**: 负载均衡，提高吞吐量

### 4. 特殊场景支持
- **死信队列**: 处理失败/过期消息
- **延迟队列**: 定时延迟处理
- **优先级队列**: 高优先级消息优先处理
- **批量消费**: 一次处理多条消息

## 配置说明

### 1. 数据库配置表

在 `message_consumer_config` 表中配置消费参数：

```sql
INSERT INTO message_consumer_config (
    service_name, instance_id, message_queue_type, consume_mode, 
    consume_type, consume_order, topic, exchange, routing_key, 
    consumer_group, batch_size, max_retry_times, timeout, enabled
) VALUES (
    'order-service', 'instance-001', 'RABBITMQ', 'PUSH', 
    'CLUSTERING', 'CONCURRENT', 'order_queue', 'order_exchange', 'order.created',
    'order-consumer-group', 100, 3, 30000, true
);
```

### 2. 配置文件

在 `application-rabbitmq.yml` 中配置RabbitMQ连接参数：

```yaml
rabbitmq:
  host: localhost
  port: 5672
  username: guest
  password: guest
  
  consumer:
    default-mode: PUSH
    default-ack-mode: MANUAL
    prefetch-count: 1
    batch-size: 100
    max-retry-times: 3
    timeout: 30000
  
  dead-letter:
    enabled: true
    exchange-prefix: dlx.
    routing-key-prefix: dlq.
```

## 使用方法

### 1. 启动自动消费服务

```java
@Autowired
private AutoConsumeService autoConsumeService;

// 启动自动消费
autoConsumeService.startAutoConsume();
```

### 2. 管理特定服务的消费

```java
// 启动特定服务的消费
autoConsumeService.startConsumeByService("order-service");

// 停止特定服务的消费
autoConsumeService.stopConsumeByService("order-service");

// 检查消费状态
boolean isConsuming = autoConsumeService.isConsuming("order-service");
```

### 3. 重新加载配置

```java
// 重新加载消费配置
autoConsumeService.reloadConsumeConfig();
```

## 消费配置参数详解

### 1. 基础参数
- `serviceName`: 服务名称，用于标识消费配置
- `instanceId`: 实例ID，用于区分同一服务的不同实例
- `messageQueueType`: 消息中间件类型（RABBITMQ）
- `enabled`: 是否启用该配置

### 2. 消费参数
- `consumeMode`: 消费模式（PUSH/PULL）
- `consumeType`: 消费类型（CLUSTERING/BROADCASTING）
- `consumeOrder`: 消费顺序（CONCURRENT/ORDERLY/PRIORITY）

### 3. 队列参数
- `topic`: 消费的队列名称
- `exchange`: 交换机名称（可选）
- `routingKey`: 路由键（可选）
- `consumerGroup`: 消费组名称

### 4. 性能参数
- `batchSize`: 批量处理大小
- `consumeInterval`: 消费间隔（毫秒）
- `maxRetryTimes`: 最大重试次数
- `timeout`: 超时时间（毫秒）

## 消费策略

### 1. 推模式 (PUSH)
- **特点**: RabbitMQ主动推送消息到消费者
- **优势**: 实时性高，无需手动轮询
- **适用场景**: 订单处理、即时通知等实时业务

### 2. 拉模式 (PULL)
- **特点**: 消费者主动拉取消息
- **优势**: 灵活性高，可按需控制拉取频率
- **适用场景**: 批量数据同步、定时任务等非实时业务

### 3. 手动确认 (Manual Ack)
- **特点**: 消息处理成功后手动确认
- **优势**: 确保消息可靠性，避免丢失
- **适用场景**: 核心业务（订单、支付、库存变更）

### 4. 自动确认 (Auto Ack)
- **特点**: 消息投递后立即确认
- **风险**: 可能丢失消息
- **适用场景**: 非关键业务（日志、临时通知）

## 特殊场景实现

### 1. 死信队列

配置死信队列处理失败消息：

```yaml
rabbitmq:
  dead-letter:
    enabled: true
    exchange-prefix: dlx.
    routing-key-prefix: dlq.
    queue-prefix: dlq.
```

队列声明时自动配置死信参数：

```java
Map<String, Object> queueArgs = new HashMap<>();
queueArgs.put("x-dead-letter-exchange", "dlx." + exchange);
queueArgs.put("x-dead-letter-routing-key", "dlq." + routingKey);
channel.queueDeclare(queueName, true, false, false, queueArgs);
```

### 2. 延迟队列

通过TTL + 死信交换机实现延迟队列：

```yaml
rabbitmq:
  delay:
    enabled: true
    exchange-prefix: delay.
    queue-prefix: delay.
    default-ttl: 30000
```

### 3. 优先级队列

支持消息优先级处理：

```yaml
rabbitmq:
  priority:
    enabled: true
    max-priority: 10
    default-priority: 5
```

### 4. 批量消费

配置预取数量实现批量处理：

```yaml
rabbitmq:
  consumer:
    prefetch-count: 10
    batch-size: 100
```

## 错误处理

### 1. 重试机制
- 配置最大重试次数
- 失败消息重新入队
- 超过重试次数进入死信队列

### 2. 死信队列
- 自动路由失败消息
- 支持手动重试
- 异常订单人工干预

### 3. 监控告警
- 消费状态监控
- 错误率统计
- 异常情况告警

## 性能优化

### 1. 预取设置
- 合理设置预取数量
- 平衡延迟和吞吐量
- 避免消息积压

### 2. 批量处理
- 配置合适的批量大小
- 减少网络交互
- 提高处理效率

### 3. 连接管理
- 连接池复用
- 自动重连机制
- 心跳保活

## 监控指标

### 1. 消费状态
- 是否正在消费
- 消费者数量
- 最后消费时间

### 2. 性能指标
- 消息处理速率
- 消费延迟
- 错误率

### 3. 资源使用
- 连接状态
- 队列深度
- 内存使用

## 故障排查

### 1. 常见问题
- 消费者无法启动
- 消息消费失败
- 连接断开

### 2. 排查步骤
- 检查配置参数
- 查看连接状态
- 验证队列存在
- 检查权限设置

### 3. 解决方案
- 调整配置参数
- 重启消费服务
- 检查网络连接
- 联系运维支持

## 最佳实践

### 1. 配置管理
- 使用配置文件管理参数
- 支持环境差异化配置
- 定期检查和更新配置

### 2. 监控告警
- 建立完善的监控体系
- 设置合理的告警阈值
- 及时响应异常情况

### 3. 性能调优
- 根据业务需求调整参数
- 定期进行性能测试
- 持续优化消费策略

### 4. 容错设计
- 实现优雅降级
- 支持快速故障恢复
- 建立备份机制

## 示例配置

### 1. 订单处理服务

```sql
INSERT INTO message_consumer_config (
    service_name, instance_id, message_queue_type, consume_mode, 
    consume_type, consume_order, topic, exchange, routing_key, 
    consumer_group, batch_size, max_retry_times, timeout, enabled
) VALUES (
    'order-service', 'instance-001', 'RABBITMQ', 'PUSH', 
    'CLUSTERING', 'CONCURRENT', 'order_queue', 'order_exchange', 'order.created',
    'order-consumer-group', 50, 3, 30000, true
);
```

### 2. 日志收集服务

```sql
INSERT INTO message_consumer_config (
    service_name, instance_id, message_queue_type, consume_mode, 
    consume_type, consume_order, topic, exchange, routing_key, 
    consumer_group, batch_size, max_retry_times, timeout, enabled
) VALUES (
    'log-service', 'instance-001', 'RABBITMQ', 'PULL', 
    'BROADCASTING', 'CONCURRENT', 'log_queue', 'log_exchange', 'log.*',
    'log-consumer-group', 200, 1, 10000, true
);
```

### 3. 延迟任务服务

```sql
INSERT INTO message_consumer_config (
    service_name, instance_id, message_queue_type, consume_mode, 
    consume_type, consume_order, topic, exchange, routing_key, 
    consumer_group, batch_size, max_retry_times, timeout, enabled
) VALUES (
    'delay-service', 'instance-001', 'RABBITMQ', 'PUSH', 
    'CLUSTERING', 'ORDERLY', 'delay_queue', 'delay_exchange', 'delay.task',
    'delay-consumer-group', 10, 2, 60000, true
);
```

## 总结

RabbitMQ消费服务提供了灵活的配置和强大的功能，能够满足各种业务场景的需求。通过合理的配置和优化，可以实现高可靠、高性能的消息消费服务。
