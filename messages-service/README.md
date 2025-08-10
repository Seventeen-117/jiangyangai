# Messages Service

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
    }
}
```

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
    }
}
```

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
