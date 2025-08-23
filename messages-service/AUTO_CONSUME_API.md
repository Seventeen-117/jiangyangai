# 自动消费服务 API 文档

## 概述

自动消费服务（Auto Consume Service）是一个智能的消息消费管理系统，它能够根据配置自动启动和管理不同服务的消息消费。当其他服务配置了消费消息类型（RocketMQ、Kafka、RabbitMQ）时，系统会自动请求messages-service进行消费，无需手动干预。

## 核心特性

### 1. 自动消费管理
- **配置驱动**：通过数据库配置自动启动消费
- **动态加载**：支持运行时重新加载配置
- **状态监控**：实时监控消费状态和统计信息

### 2. 多消息中间件支持
- **RocketMQ**：支持推/拉模式、集群/广播消费、并发/顺序消费
- **Kafka**：支持拉模式、集群消费、并发消费
- **RabbitMQ**：支持推模式、集群消费、并发消费

### 3. 灵活的消费配置
- **消费模式**：PUSH（推模式）、PULL（拉模式）
- **消费类型**：CLUSTERING（集群消费）、BROADCASTING（广播消费）
- **顺序性**：CONCURRENT（并发消费）、ORDERLY（顺序消费）

## API 端点

### 1. 服务控制

#### 启动自动消费服务
```http
POST /api/messages/auto-consume/start
```

**响应示例：**
```json
{
  "success": true,
  "message": "自动消费服务启动成功",
  "timestamp": 1640995200000
}
```

#### 停止自动消费服务
```http
POST /api/messages/auto-consume/stop
```

#### 重新加载消费配置
```http
POST /api/messages/auto-consume/reload
```

### 2. 服务级控制

#### 启动指定服务的消费
```http
POST /api/messages/auto-consume/start/{serviceName}
```

**路径参数：**
- `serviceName`：服务名称（如：order-service）

#### 停止指定服务的消费
```http
POST /api/messages/auto-consume/stop/{serviceName}
```

### 3. 配置管理

#### 获取所有消费配置
```http
GET /api/messages/auto-consume/configs
```

**响应示例：**
```json
{
  "success": true,
  "message": "获取消费配置成功",
  "data": [
    {
      "id": 1,
      "serviceName": "order-service",
      "messageQueueType": "ROCKETMQ",
      "consumeMode": "PUSH",
      "consumeType": "CLUSTERING",
      "consumeOrder": "CONCURRENT",
      "topic": "ORDER_TOPIC",
      "tag": "ORDER_CREATE",
      "consumerGroup": "order-consumer-group",
      "enabled": true,
      "description": "订单服务消费订单创建消息"
    }
  ],
  "timestamp": 1640995200000
}
```

#### 获取指定服务的消费配置
```http
GET /api/messages/auto-consume/configs/{serviceName}
```

#### 保存消费配置
```http
POST /api/messages/auto-consume/configs
```

**请求体示例：**
```json
{
  "serviceName": "new-service",
  "messageQueueType": "ROCKETMQ",
  "consumeMode": "PUSH",
  "consumeType": "CLUSTERING",
  "consumeOrder": "CONCURRENT",
  "topic": "NEW_TOPIC",
  "tag": "NEW_TAG",
  "consumerGroup": "new-consumer-group",
  "enabled": true,
  "description": "新服务的消费配置"
}
```

#### 更新消费配置
```http
PUT /api/messages/auto-consume/configs/{id}
```

#### 删除消费配置
```http
DELETE /api/messages/auto-consume/configs/{id}
```

#### 启用消费配置
```http
POST /api/messages/auto-consume/configs/{id}/enable
```

#### 禁用消费配置
```http
POST /api/messages/auto-consume/configs/{id}/disable
```

### 4. 状态监控

#### 检查消费状态
```http
GET /api/messages/auto-consume/status/{serviceName}
```

**响应示例：**
```json
{
  "success": true,
  "message": "获取消费状态成功",
  "data": {
    "serviceName": "order-service",
    "isConsuming": true,
    "timestamp": 1640995200000
  },
  "timestamp": 1640995200000
}
```

#### 获取消费统计信息
```http
GET /api/messages/auto-consume/statistics/{serviceName}
```

#### 健康检查
```http
GET /api/messages/auto-consume/health
```

## 配置说明

### 消费配置字段

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| serviceName | String | 是 | 服务名称 |
| instanceId | String | 否 | 服务实例ID |
| messageQueueType | String | 是 | 消息中间件类型：ROCKETMQ、KAFKA、RABBITMQ |
| consumeMode | String | 是 | 消费模式：PUSH、PULL |
| consumeType | String | 是 | 消费类型：CLUSTERING、BROADCASTING |
| consumeOrder | String | 是 | 顺序性：CONCURRENT、ORDERLY |
| topic | String | 是 | 主题/队列名称 |
| tag | String | 否 | 标签（RocketMQ专用） |
| consumerGroup | String | 是 | 消费组名称 |
| partitionKey | String | 否 | 分区键（Kafka专用） |
| exchange | String | 否 | 交换机名称（RabbitMQ专用） |
| routingKey | String | 否 | 路由键（RabbitMQ专用） |
| enabled | Boolean | 否 | 是否启用（默认true） |
| consumeInterval | Long | 否 | 消费间隔（毫秒，拉模式专用，默认1000） |
| batchSize | Integer | 否 | 批量消费大小（默认1） |
| maxRetryTimes | Integer | 否 | 最大重试次数（默认3） |
| timeout | Long | 否 | 超时时间（毫秒，默认30000） |
| description | String | 否 | 配置描述 |

### 消费模式说明

#### RocketMQ 消费模式

**推模式（PUSH）+ 集群消费（CLUSTERING）+ 并发消费（CONCURRENT）**
- 适用场景：常规业务消费，如订单处理、通知推送
- 特点：Broker主动推送，消费者多线程并发处理，不保证顺序

**推模式（PUSH）+ 集群消费（CLUSTERING）+ 顺序消费（ORDERLY）**
- 适用场景：有状态依赖的业务，如订单状态变更
- 特点：Broker主动推送，保证消息按发送顺序被消费

**推模式（PUSH）+ 广播消费（BROADCASTING）+ 并发消费（CONCURRENT）**
- 适用场景：配置更新、全量节点通知
- 特点：同消费组内的所有消费者都会收到并处理同一条消息

#### Kafka 消费模式

**拉模式（PULL）+ 集群消费（CLUSTERING）+ 并发消费（CONCURRENT）**
- 适用场景：批量数据处理、流量峰值控制
- 特点：消费者主动拉取，可自定义拉取策略

#### RabbitMQ 消费模式

**推模式（PUSH）+ 集群消费（CLUSTERING）+ 并发消费（CONCURRENT）**
- 适用场景：队列消息消费
- 特点：消费者多线程并发处理队列消息

## 使用流程

### 1. 配置消费参数
在数据库中插入或更新消费配置，指定：
- 服务名称
- 消息中间件类型
- 消费模式、类型、顺序性
- 主题、标签、消费组等参数

### 2. 启动自动消费服务
调用启动接口，系统会自动：
- 读取所有启用的消费配置
- 根据配置创建相应的消费者
- 启动消息消费

### 3. 监控和管理
- 查看消费状态
- 获取统计信息
- 动态启用/禁用配置
- 重新加载配置

## 示例配置

### RocketMQ 顺序消费配置
```sql
INSERT INTO message_consumer_config (
  service_name, message_queue_type, consume_mode, consume_type, consume_order,
  topic, tag, consumer_group, enabled, description
) VALUES (
  'payment-service', 'ROCKETMQ', 'PUSH', 'CLUSTERING', 'ORDERLY',
  'PAYMENT_TOPIC', 'PAYMENT_SUCCESS', 'payment-consumer-group', 1,
  '支付服务消费支付成功消息，保证顺序'
);
```

### Kafka 拉模式配置
```sql
INSERT INTO message_consumer_config (
  service_name, message_queue_type, consume_mode, consume_type, consume_order,
  topic, consumer_group, consume_interval, batch_size, enabled, description
) VALUES (
  'log-service', 'KAFKA', 'PULL', 'CLUSTERING', 'CONCURRENT',
  'LOG_TOPIC', 'log-consumer-group', 5000, 100, 1,
  '日志服务消费日志消息，拉模式，5秒间隔，批量100条'
);
```

### RabbitMQ 配置
```sql
INSERT INTO message_consumer_config (
  service_name, message_queue_type, consume_mode, consume_type, consume_order,
  topic, exchange, routing_key, consumer_group, enabled, description
) VALUES (
  'inventory-service', 'RABBITMQ', 'PUSH', 'CLUSTERING', 'CONCURRENT',
  'inventory_queue', 'inventory_exchange', 'inventory.update', 'inventory-consumer-group', 1,
  '库存服务消费库存更新消息'
);
```

## 注意事项

1. **配置唯一性**：同一服务的同一主题和消费组只能有一个配置
2. **启动顺序**：确保消息中间件服务已启动再启动自动消费服务
3. **配置变更**：修改配置后需要重新加载或重启服务
4. **监控告警**：建议配置消费状态的监控和告警
5. **资源管理**：合理配置消费间隔和批量大小，避免资源浪费

## 故障排查

### 常见问题

1. **消费启动失败**
   - 检查消息中间件连接配置
   - 验证消费配置参数是否正确
   - 查看日志中的具体错误信息

2. **消息消费异常**
   - 检查消费者状态
   - 验证消息格式和业务逻辑
   - 查看重试次数和错误日志

3. **配置加载失败**
   - 检查数据库连接
   - 验证配置表结构
   - 查看配置监控日志

### 日志查看
- 自动消费服务日志：`AutoConsumeServiceImpl`
- 配置服务日志：`MessageConsumerConfigServiceImpl`
- 控制器日志：`AutoConsumeController`
