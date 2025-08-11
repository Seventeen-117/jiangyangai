# Messages Service

<<<<<<< HEAD
统一消息服务，集成RocketMQ、Kafka、RabbitMQ等消息中间件，提供统一的消息发送和消费接口。

## 功能特性

### 消息中间件支持
- **RocketMQ**: 支持同步/异步/单向/批量/顺序/延迟/事务消息
- **Kafka**: 支持同步/异步/批量消息发送和消费
- **RabbitMQ**: 支持同步/异步/批量/延迟消息

### 分布式事务追溯
- **Seata Saga**: 基于状态机的分布式事务管理
- **独立审计日志库**: 完整的事务生命周期记录
- **消息生命周期追踪**: 从生产到消费的完整轨迹
- **业务调用链路**: 服务间调用的完整追踪

### 高可用特性
- **消息可靠性**: 重试机制、消息去重、死信队列处理
- **性能优化**: 批量处理、异步处理、连接池管理
- **监控告警**: 实时监控、性能统计、异常告警

## 技术架构

### 核心组件

```
messages-service/
├── audit/                    # 审计日志模块
│   ├── entity/              # 审计日志实体
│   ├── mapper/              # 数据访问层
│   ├── service/             # 审计日志服务
│   ├── controller/          # REST API接口
│   └── config/              # 配置类
├── saga/                    # Seata Saga状态机
├── rocketmq/                # RocketMQ实现
├── kafka/                   # Kafka实现
├── rabbitmq/                # RabbitMQ实现
└── config/                  # 配置管理
```

### 审计日志数据库设计

#### 1. 事务审计日志表 (transaction_audit_log)
记录分布式事务的完整生命周期：
- 全局事务ID (Seata XID)
- 业务事务ID
- 操作类型和状态
- 请求参数和响应结果
- 执行耗时和错误信息

#### 2. 消息生命周期日志表 (message_lifecycle_log)
记录消息从生产到消费的完整轨迹：
- 消息ID和业务消息ID
- 生命周期阶段 (PRODUCE, SEND, STORE, CONSUME, ACK, FAIL)
- 阶段状态和处理耗时
- 死信队列和重试信息

#### 3. 业务轨迹日志表 (business_trace_log)
记录服务间调用的完整链路：
- 调用链ID和Span ID
- 服务名称和操作类型
- 请求参数和响应结果
- 调用耗时和错误信息

## 快速开始

### 1. 环境准备

```bash
# 创建审计日志数据库
CREATE DATABASE messages_audit_log CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 执行数据库表结构
mysql -u root -p messages_audit_log < src/main/resources/sql/audit_log_tables.sql
```

### 2. 配置Seata

```yaml
# application-audit.yml
seata:
  enabled: true
  application-id: messages-service
  tx-service-group: messages-service-group
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      data-id: seataServer.properties
```

### 3. 使用示例

#### 发送消息时的事务追溯

```java
@Service
public class MessageService {
    
    @Autowired
    private TransactionTraceManager transactionTraceManager;
    
    @Transactional
    public void sendMessage(String businessTransactionId, String topic, String content) {
        try {
            // 1. 开始事务追溯
            transactionTraceManager.beginTransactionTrace(businessTransactionId, "SEND", "ROCKETMQ");
            
            // 2. 记录消息生命周期
            transactionTraceManager.recordMessageLifecycle(
                "MSG_" + System.currentTimeMillis(),
                businessTransactionId,
                "PRODUCE",
                "PROCESSING",
                topic,
                "ORDER",
                businessTransactionId,
                content,
                new HashMap<>()
            );
            
            // 3. 发送消息
            // ... 消息发送逻辑
            
            // 4. 记录业务调用轨迹
            transactionTraceManager.recordBusinessTrace(
                "sendMessage",
                "MQ",
                "OUTBOUND",
                "rocketmq-broker",
                "sendMessage",
                "{\"topic\":\"" + topic + "\"}",
                "{\"messageId\":\"MSG_123456\"}",
                200,
                "SUCCESS",
                150L,
                null
            );
            
            // 5. 结束事务追溯
            transactionTraceManager.endTransactionTrace("SUCCESS", null);
            
        } catch (Exception e) {
            transactionTraceManager.endTransactionTrace("FAILED", e.getMessage());
            throw e;
        }
=======
## 概述

Messages Service 是一个统一的消息服务抽象层，支持 RocketMQ、Kafka 和 RabbitMQ 三种消息中间件。该服务通过 Nacos 配置中心进行动态配置管理，支持配置热更新和服务动态启停。

## 配置结构

### 1. 配置文件说明

- **`application.yml`**: 包含默认配置和Nacos客户端配置
- **`bootstrap.yml`**: Spring Cloud Bootstrap配置，用于Nacos配置中心和注册中心
- **`nacos-config.yml`**: 用于导入到Nacos配置中心的完整配置模板

### 2. 配置前缀

所有配置都使用 `message.service` 前缀，例如：
```yaml
message:
  service:
    type: rocketmq
    rocketmq:
      enabled: true
      name-server: 8.133.246.113:9876
```

### 3. 配置属性

#### 通用配置
- `type`: 默认消息服务类型 (rocketmq/kafka/rabbitmq)
- `trace-enabled`: 是否启用消息轨迹追踪
- `send-retry-times`: 消息发送重试次数
- `send-timeout-ms`: 消息发送超时时间
- `max-batch-size`: 最大批量发送消息数量

#### RocketMQ配置
- `enabled`: 是否启用RocketMQ
- `name-server`: Name Server地址
- `producer-group`: 生产者组
- `consumer-group`: 消费者组
- `compression-strategy`: 压缩策略
- `retry-sync-times`: 同步发送重试次数
- `retry-async-times`: 异步发送重试次数
- `queue-count-per-topic`: 每个Topic的队列数量
- `message-retention-hours`: 消息保留时间
- `send-msg-timeout`: 发送消息超时时间
- `compress-msg-body-over-howmuch`: 压缩消息体阈值
- `max-message-size`: 最大消息大小
- `enable-msg-trace`: 是否启用消息轨迹
- `trace-topic-name`: 消息轨迹存储Topic

#### Kafka配置
- `enabled`: 是否启用Kafka
- `bootstrap-servers`: Bootstrap Servers地址
- `consumer-group-id`: 消费者组ID
- `acks`: 生产者确认机制
- `batch-size`: 批量大小
- `linger-ms`: 延迟时间
- `buffer-memory`: 缓冲区大小
- `retries`: 生产者重试次数
- `auto-commit-interval-ms`: 消费者自动提交间隔
- `session-timeout-ms`: 消费者会话超时时间
- `heartbeat-interval-ms`: 消费者心跳间隔
- `max-poll-records`: 消费者最大拉取记录数
- `max-poll-interval-ms`: 消费者最大拉取间隔

#### RabbitMQ配置
- `enabled`: 是否启用RabbitMQ
- `host`: 主机地址
- `port`: 端口
- `username`: 用户名
- `password`: 密码
- `virtual-host`: 虚拟主机
- `connection-timeout`: 连接超时时间
- `channel-rpc-timeout`: 通道RPC超时时间
- `requested-heart-beat`: 请求心跳间隔
- `connection-recovery-interval`: 连接恢复间隔
- `automatic-recovery-enabled`: 是否启用连接恢复
- `topology-recovery-enabled`: 是否启用拓扑恢复

## 使用方法

### 1. 在Nacos中导入配置

将 `nacos-config.yml` 文件导入到Nacos配置中心：
- Data ID: `messages-service.yml`
- Group: `JIANGYANG`
- 格式: YAML

### 2. 环境变量配置

```bash
# Nacos服务器地址
export NACOS_SERVER_ADDR=8.133.246.113:8848

# 配置分组
export NACOS_CONFIG_GROUP=JIANGYANG

# 命名空间（可选）
export NACOS_NAMESPACE=your-namespace

# RocketMQ配置
export ROCKETMQ_NAMESRV_ADDR=8.133.246.113:9876

# Kafka配置
export KAFKA_BOOTSTRAP_SERVERS=8.133.246.113:9092

# RabbitMQ配置
export RABBITMQ_HOST=8.133.246.113
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest
```

### 3. 在Spring Boot应用中使用

```java
@SpringBootApplication
@EnableConfigurationProperties(MessageServiceConfig.class)
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
>>>>>>> c3070b04cf2333c93875cfa642f6d5bdc9532b68
    }
}
```

<<<<<<< HEAD
#### 消费消息时的事务追溯

```java
@Service
public class MessageConsumer {
    
    @Autowired
    private TransactionTraceManager transactionTraceManager;
    
    @Transactional
    public void consumeMessage(String messageId, String businessTransactionId, String content) {
        try {
            // 1. 开始事务追溯
            transactionTraceManager.beginTransactionTrace(businessTransactionId, "CONSUME", "ROCKETMQ");
            
            // 2. 记录消息接收
            transactionTraceManager.recordMessageLifecycle(
                messageId,
                businessTransactionId,
                "RECEIVE",
                "PROCESSING",
                "ORDER_TOPIC",
                "ORDER",
                businessTransactionId,
                content,
                new HashMap<>()
            );
            
            // 3. 业务处理
            // ... 业务处理逻辑
            
            // 4. 记录业务调用轨迹
            transactionTraceManager.recordBusinessTrace(
                "processOrder",
                "BUSINESS",
                "INTERNAL",
                "order-service",
                "processOrder",
                "{\"orderId\":\"" + businessTransactionId + "\"}",
                "{\"status\":\"PROCESSED\"}",
                200,
                "SUCCESS",
                500L,
                null
            );
            
            // 5. 结束事务追溯
            transactionTraceManager.endTransactionTrace("SUCCESS", null);
            
        } catch (Exception e) {
            transactionTraceManager.endTransactionTrace("FAILED", e.getMessage());
            throw e;
        }
=======
### 4. 注入和使用消息服务

```java
@Service
public class YourService {
    
    @Autowired
    private MessageServiceFactory messageServiceFactory;
    
    public void sendMessage() {
        // 获取默认消息服务
        MessageService messageService = messageServiceFactory.getDefaultMessageService();
        
        // 或者根据类型获取特定服务
        MessageService rocketMQService = messageServiceFactory.getMessageService(MessageServiceType.ROCKETMQ);
        
        // 发送消息
        messageService.sendMessage("topic", "message");
>>>>>>> c3070b04cf2333c93875cfa642f6d5bdc9532b68
    }
}
```

<<<<<<< HEAD
## API接口

### 审计日志查询接口

#### 1. 查询事务轨迹
```http
GET /api/audit/transaction/{globalTransactionId}
```

#### 2. 查询消息生命周期
```http
GET /api/audit/message/{messageId}
```

#### 3. 查询业务调用轨迹
```http
GET /api/audit/trace/{traceId}
```

#### 4. 查询统计信息
```http
GET /api/audit/statistics/transaction?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00
GET /api/audit/statistics/message?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00
GET /api/audit/statistics/call?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00
```

#### 5. 查询失败记录
```http
GET /api/audit/failed/transactions?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00
GET /api/audit/failed/messages?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00
GET /api/audit/failed/calls?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00
```

#### 6. 导出审计日志
```http
GET /api/audit/export?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00&format=csv
```

## 配置说明

### 审计日志配置

```yaml
audit:
  log:
    enabled: true                    # 是否启用审计日志
    retention-days: 30              # 审计日志保留天数
    batch-size: 100                 # 批量插入大小
    async:
      core-pool-size: 10            # 异步线程池核心线程数
      max-pool-size: 50             # 异步线程池最大线程数
      queue-capacity: 1000          # 队列容量
    message:
      store-content: true           # 是否存储消息内容
      max-content-length: 10000     # 消息内容最大长度
      calculate-digest: true        # 是否计算消息摘要
    performance:
      enabled: true                 # 是否启用性能监控
      slow-query-threshold: 1000    # 慢查询阈值（毫秒）
    export:
      max-file-size: 100           # 导出文件最大大小（MB）
      max-record-count: 100000     # 导出记录数限制
```

### 数据源配置

```yaml
spring:
  datasource:
    audit-log:
      url: jdbc:mysql://localhost:3306/messages_audit_log
      username: root
      password: password
      initial-size: 5
      min-idle: 5
      max-active: 20
```

## 监控和运维

### 1. 性能监控
- 事务成功率统计
- 消息处理延迟监控
- 调用链路性能分析
- 异常情况告警

### 2. 数据清理
```bash
# 清理过期审计日志
curl -X DELETE "http://localhost:8080/api/audit/cleanup?retentionDays=30"
```

### 3. 数据导出
```bash
# 导出CSV格式
curl -o audit_logs.csv "http://localhost:8080/api/audit/export?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00&format=csv"

# 导出JSON格式
curl -o audit_logs.json "http://localhost:8080/api/audit/export?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00&format=json"
```

## 最佳实践

### 1. 事务追溯最佳实践
- 为每个业务操作生成唯一的业务事务ID
- 在事务开始时调用 `beginTransactionTrace`
- 在关键节点记录业务调用轨迹
- 在事务结束时调用 `endTransactionTrace`

### 2. 性能优化
- 使用异步方式记录审计日志
- 合理设置批量插入大小
- 定期清理过期数据
- 使用数据库索引优化查询

### 3. 监控告警
- 设置事务成功率告警阈值
- 监控消息处理延迟
- 关注死信队列数量
- 定期检查审计日志存储空间

## 故障排查

### 1. 常见问题
- **审计日志记录失败**: 检查数据库连接和权限
- **事务追溯不完整**: 确保在异常情况下也调用 `endTransactionTrace`
- **性能问题**: 检查异步线程池配置和数据库性能

### 2. 日志分析
```sql
-- 查询失败的事务
SELECT * FROM transaction_audit_log 
WHERE transaction_status = 'FAILED' 
AND create_time > DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- 查询死信消息
SELECT * FROM message_lifecycle_log 
WHERE dead_letter_flag = 1 
AND create_time > DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- 查询慢调用
SELECT * FROM business_trace_log 
WHERE call_duration > 5000 
AND create_time > DATE_SUB(NOW(), INTERVAL 1 HOUR);
```

## 版本历史

- **v1.0.0**: 初始版本，支持RocketMQ、Kafka、RabbitMQ
- **v1.1.0**: 新增Seata Saga分布式事务支持
- **v1.2.0**: 新增独立审计日志库和事务追溯功能

## 贡献指南

欢迎提交Issue和Pull Request来改进这个项目。

## 许可证

本项目采用MIT许可证。
=======
## 配置热更新

服务支持配置热更新，当Nacos中的配置发生变化时，相关服务会自动刷新：

1. 修改Nacos中的配置
2. 服务会自动检测配置变更
3. 相关Bean会自动刷新
4. 新的配置立即生效

## 监控和调试

### 1. 配置状态查看

通过 `MessageServiceAutoConfiguration.getConfigurationStatus()` 方法可以查看当前配置状态。

### 2. 管理端点

服务暴露了以下管理端点：
- `/actuator/health`: 健康检查
- `/actuator/info`: 应用信息
- `/actuator/configprops`: 配置属性
- `/actuator/env`: 环境变量

### 3. 日志配置

默认日志级别为DEBUG，可以通过以下方式调整：
```yaml
logging:
  level:
    com.jiangyang.messages: INFO
```

## 注意事项

1. **依赖管理**: 确保项目中包含了必要的消息中间件依赖
2. **配置优先级**: Nacos配置 > 环境变量 > 默认配置
3. **服务启停**: 通过 `enabled` 字段控制各消息中间件的启用/禁用
4. **配置验证**: 服务启动时会验证配置的有效性
5. **错误处理**: 配置错误时服务会记录警告日志，但不会阻止启动

## 故障排除

### 1. 配置无法加载
- 检查Nacos服务器连接
- 验证配置文件格式
- 确认配置前缀正确

### 2. 服务无法启动
- 检查消息中间件连接
- 验证配置参数有效性
- 查看启动日志

### 3. 配置热更新不生效
- 确认Nacos配置变更
- 检查RefreshScope注解
- 验证配置监听器

## 版本信息

- Spring Boot: 3.x
- Spring Cloud: 2023.x
- Spring Cloud Alibaba: 2022.x
- Java: 17+
>>>>>>> c3070b04cf2333c93875cfa642f6d5bdc9532b68
