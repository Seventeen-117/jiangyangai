# Enhanced Message API Documentation

## 概述

增强消息API支持根据`messageType`动态路由消息到不同的消息中间件（RocketMQ、Kafka、RabbitMQ），每种消息中间件支持特定的参数配置。

## API端点

### 1. 增强消息发送 `/api/messages/saga/send/enhanced`

支持动态路由到不同消息中间件的增强消息发送接口。

#### 请求方法
```
POST /api/messages/saga/send/enhanced
```

#### 请求头
```
Content-Type: application/json
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| messageId | String | 是 | 消息唯一标识 |
| messageType | String | 是 | 消息中间件类型：ROCKETMQ、KAFKA、RABBITMQ |
| messageType | String | 否 | 消息类型，根据中间件不同：<br/>**RocketMQ**: NORMAL（普通）、DELAY（定时）、ORDERED（顺序）、TRANSACTION（事务）<br/>**Kafka**: SYNC_BLOCKING（同步阻塞）、ASYNC_CALLBACK（异步回调）、ASYNC_NO_CALLBACK（异步无回调）<br/>**RabbitMQ**: DIRECT（直接）、TOPIC（主题）、FANOUT（扇形）、HEADERS（首部）、PERSISTENT（持久化）、TTL（过期）、PRIORITY（优先级）、DEAD_LETTER（死信）<br/>默认为NORMAL |

#### 根据messageType的特定参数

##### RocketMQ (messageType: "ROCKETMQ")

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| topic | String | 是 | 消息主题 |
| tag | String | 否 | 消息标签 |
| messageType | String | 否 | 消息类型：NORMAL（普通）、DELAY（定时）、ORDERED（顺序）、TRANSACTION（事务），默认为NORMAL |
| messageBody | String | 是 | 消息体内容 |
| delayLevel | Integer | 否 | 延迟级别（当messageType为DELAY时使用，默认为1） |
| hashKey | String | 否 | 哈希键（当messageType为ORDERED时使用，默认为messageId） |

**示例请求体：**

**普通消息：**
```json
{
  "messageId": "msg-001",
  "messageType": "ROCKETMQ",
  "messageType": "NORMAL",
  "topic": "ORDER_TOPIC",
  "tag": "PAY_SUCCESS",
  "messageBody": "{\"orderId\":\"123\",\"amount\":100.00}"
}
```

**定时消息：**
```json
{
  "messageId": "msg-002",
  "messageType": "ROCKETMQ",
  "messageType": "DELAY",
  "topic": "ORDER_TOPIC",
  "tag": "PAY_SUCCESS",
  "delayLevel": 3,
  "messageBody": "{\"orderId\":\"123\",\"amount\":100.00}"
}
```

**顺序消息：**
```json
{
  "messageId": "msg-003",
  "messageType": "ROCKETMQ",
  "messageType": "ORDERED",
  "topic": "ORDER_TOPIC",
  "tag": "PAY_SUCCESS",
  "hashKey": "user123",
  "messageBody": "{\"orderId\":\"123\",\"amount\":100.00}"
}
```

##### Kafka (messageType: "KAFKA")

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| topic | String | 是 | 消息主题 |
| key | String | 否 | 分区键（相同key的消息会发送到同一个分区） |
| messageType | String | 否 | 消息类型：SYNC_BLOCKING（同步阻塞）、ASYNC_CALLBACK（异步回调）、ASYNC_NO_CALLBACK（异步无回调），默认为SYNC_BLOCKING |
| messageBody | String | 是 | 消息体内容 |

**示例请求体：**

**同步阻塞发送（交易订单、核心业务数据）：**
```json
{
  "messageId": "msg-sync-001",
  "messageType": "KAFKA",
  "messageType": "SYNC_BLOCKING",
  "topic": "ORDER_TOPIC",
  "key": "user123",
  "messageBody": "{\"orderId\":\"123\",\"amount\":100.00,\"type\":\"PAYMENT\"}"
}
```

**异步回调发送（日志采集、业务通知）：**
```json
{
  "messageId": "msg-async-callback-001",
  "messageType": "KAFKA",
  "messageType": "ASYNC_CALLBACK",
  "topic": "LOG_TOPIC",
  "key": "service123",
  "messageBody": "{\"serviceId\":\"123\",\"logLevel\":\"INFO\",\"message\":\"User login success\"}"
}
```

**异步无回调发送（监控指标、非关键日志）：**
```json
{
  "messageId": "msg-async-no-callback-001",
  "messageType": "KAFKA",
  "messageType": "ASYNC_NO_CALLBACK",
  "topic": "METRICS_TOPIC",
  "key": "metric123",
  "messageBody": "{\"metricName\":\"cpu_usage\",\"value\":75.5,\"timestamp\":\"2024-08-23T15:40:30.123+08:00\"}"
}
```

##### RabbitMQ (messageType: "RABBITMQ")

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| queueName | String | 是 | 队列名称 |
| durable | Boolean | 否 | 是否持久化（默认true） |
| exclusive | Boolean | 否 | 是否排他（默认false） |
| autoDelete | Boolean | 否 | 是否自动删除（默认false） |
| exchange | String | 否 | 交换机名称（空表示默认交换机） |
| routingKey | String | 是 | 路由键 |
| messageType | String | 否 | 消息类型：DIRECT（直接）、TOPIC（主题）、FANOUT（扇形）、HEADERS（首部）、PERSISTENT（持久化）、TTL（过期）、PRIORITY（优先级）、DEAD_LETTER（死信），默认为DIRECT |
| otherProperties | Object | 否 | 其他属性配置 |
| messageBody | String | 是 | 消息体内容 |

**示例请求体：**

**直接交换机消息（精确路由）：**
```json
{
  "messageId": "msg-direct-001",
  "messageType": "RABBITMQ",
  "messageType": "DIRECT",
  "queueName": "order_queue",
  "durable": true,
  "exclusive": false,
  "autoDelete": false,
  "exchange": "order_exchange",
  "routingKey": "order.pay",
  "messageBody": "{\"orderId\":\"123\",\"amount\":100.00,\"status\":\"PAID\"}"
}
```

**主题交换机消息（通配符路由）：**
```json
{
  "messageId": "msg-topic-001",
  "messageType": "RABBITMQ",
  "messageType": "TOPIC",
  "queueName": "notification_queue",
  "durable": true,
  "exclusive": false,
  "autoDelete": false,
  "exchange": "notification_exchange",
  "routingKey": "user.notification",
  "messageBody": "{\"userId\":\"123\",\"type\":\"EMAIL\",\"content\":\"Welcome to our service\"}"
}
```

**扇形交换机消息（广播）：**
```json
{
  "messageId": "msg-fanout-001",
  "messageType": "RABBITMQ",
  "messageType": "FANOUT",
  "queueName": "broadcast_queue",
  "durable": true,
  "exclusive": false,
  "autoDelete": false,
  "exchange": "broadcast_exchange",
  "routingKey": "",
  "messageBody": "{\"announcement\":\"System maintenance at 2:00 AM\",\"priority\":\"HIGH\"}"
}
```

**持久化消息（确保不丢失）：**
```json
{
  "messageId": "msg-persistent-001",
  "messageType": "RABBITMQ",
  "messageType": "PERSISTENT",
  "queueName": "payment_queue",
  "durable": true,
  "exclusive": false,
  "autoDelete": false,
  "exchange": "payment_exchange",
  "routingKey": "payment.success",
  "otherProperties": {
    "deliveryMode": 2,
    "persistent": true
  },
  "messageBody": "{\"paymentId\":\"pay123\",\"amount\":1000.00,\"status\":\"SUCCESS\"}"
}
```

#### 响应格式

**成功响应：**
```json
{
  "success": true,
  "message": "增强消息发送成功",
  "data": {
    "messageId": "msg-001",
    "messageType": "ROCKETMQ",
    "status": "SENT",
    "details": "消息已成功路由到ROCKETMQ消息中间件"
  },
  "timestamp": 1640995200000
}
```

**错误响应：**
```json
{
  "success": false,
  "message": "增强消息发送失败: RocketMQ服务不可用",
  "timestamp": 1640995200000
}
```

### 2. 兼容消息发送 `/api/messages/saga/send`

保持向后兼容的简化消息发送接口。

#### 请求方法
```
POST /api/messages/saga/send
```

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| messageId | String | 是 | 消息唯一标识 |
| messageType | String | 否 | 消息类型（默认ROCKETMQ） |
| content | String | 是 | 消息内容 |

**示例请求体：**
```json
{
  "messageId": "msg-001",
  "messageType": "ROCKETMQ",
  "content": "Hello World"
}
```

## 使用示例

### 使用Postman测试

1. **发送RocketMQ消息：**
   - URL: `POST http://localhost:8081/api/messages/saga/send/enhanced`
   - Body (raw JSON):
   ```json
   {
     "messageId": "test-rocket-001",
     "messageType": "ROCKETMQ",
     "topic": "TEST_TOPIC",
     "tag": "TEST_TAG",
     "messageBody": "{\"test\":\"data\",\"timestamp\":\"2024-08-23T15:40:30.123+08:00\"}"
   }
   ```

2. **发送Kafka消息：**
   - URL: `POST http://localhost:8081/api/messages/saga/send/enhanced`
   - Body (raw JSON):
   ```json
   {
     "messageId": "test-kafka-001",
     "messageType": "KAFKA",
     "topic": "TEST_TOPIC",
     "key": "test-key-123",
     "messageBody": "{\"test\":\"data\",\"timestamp\":\"2024-08-23T15:40:30.123+08:00\"}"
   }
   ```

3. **发送RabbitMQ消息：**
   - URL: `POST http://localhost:8081/api/messages/saga/send/enhanced`
   - Body (raw JSON):
   ```json
   {
     "messageId": "test-rabbit-001",
     "messageType": "RABBITMQ",
     "queueName": "test_queue",
     "durable": true,
     "exclusive": false,
     "autoDelete": false,
     "exchange": "",
     "routingKey": "test",
     "messageBody": "{\"test\":\"data\",\"timestamp\":\"2024-08-23T15:40:30.123+08:00\"}"
   }
   ```

## 错误处理

### 常见错误码

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查必填参数是否完整 |
| 500 | 消息中间件服务不可用 | 检查对应消息中间件的配置和连接状态 |
| 500 | 不支持的消息类型 | 确保messageType为ROCKETMQ、KAFKA或RABBITMQ |

### 错误排查

1. **RocketMQ服务不可用：**
   - 检查RocketMQ服务是否启动
   - 检查配置文件中的nameServer和producerGroup配置

2. **Kafka服务不可用：**
   - 检查Kafka服务是否启动
   - 检查配置文件中的bootstrapServers配置

3. **RabbitMQ服务不可用：**
   - 检查RabbitMQ服务是否启动
   - 检查配置文件中的host、port、username、password配置

## 配置说明

确保在`application.yml`中正确配置了各种消息中间件的连接参数：

```yaml
message:
  service:
    default-type: ROCKETMQ
    rocketmq:
      enabled: true
      name-server: localhost:9876
      producer-group: default-producer-group
    kafka:
      enabled: true
      bootstrap-servers: localhost:9092
      client-id: default-kafka-producer
    rabbitmq:
      enabled: true
      host: localhost
      port: 5672
      username: guest
      password: guest
      virtual-host: /
```

## 注意事项

1. **消息去重：** 系统会自动进行消息去重，避免重复发送
2. **异步发送：** 消息发送是异步的，立即返回发送状态
3. **重试机制：** 发送失败时会自动重试，重试次数可配置
4. **事务支持：** 支持Seata分布式事务，确保消息发送的一致性
5. **监控日志：** 所有消息发送操作都会记录详细的日志信息
