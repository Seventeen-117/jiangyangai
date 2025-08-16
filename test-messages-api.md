# Messages Service API 测试指南

## 服务配置
- **服务端口**: 8081 (messages-service)
- **网关端口**: 8080 (gateway-service)
- **RocketMQ**: 8.133.246.113:9876

## 前置准备

### 1. 创建数据库表
在MySQL中执行以下SQL创建必要的表：

```sql
-- 消息Saga日志表
CREATE TABLE IF NOT EXISTS `message_saga_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` varchar(128) DEFAULT NULL COMMENT '业务ID (消息ID、批次ID、事务ID等)',
  `operation` varchar(32) DEFAULT NULL COMMENT '操作类型 (SEND, CONFIRM, RECEIVE, BUSINESS_PROCESS, etc.)',
  `status` varchar(16) DEFAULT NULL COMMENT '操作状态 (PROCESSING, SUCCESS, FAILED, COMPENSATED)',
  `global_transaction_id` varchar(128) DEFAULT NULL COMMENT '全局事务ID (Seata XID)',
  `branch_transaction_id` varchar(128) DEFAULT NULL COMMENT '分支事务ID',
  `start_time` datetime DEFAULT NULL COMMENT '操作开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '操作结束时间',
  `processing_time` bigint(20) DEFAULT NULL COMMENT '处理耗时 (毫秒)',
  `error_message` text COMMENT '错误信息',
  `error_stack_trace` text COMMENT '错误堆栈',
  `request_params` text COMMENT '请求参数 (JSON格式)',
  `response_result` text COMMENT '响应结果 (JSON格式)',
  `compensate_status` int(11) DEFAULT '0' COMMENT '补偿状态 (0-未补偿, 1-已补偿, 2-补偿失败)',
  `compensate_time` datetime DEFAULT NULL COMMENT '补偿时间',
  `compensate_count` int(11) DEFAULT '0' COMMENT '补偿次数',
  `max_compensate_count` int(11) DEFAULT '16' COMMENT '最大补偿次数',
  `retry_count` int(11) DEFAULT '0' COMMENT '重试次数',
  `max_retry_count` int(11) DEFAULT '16' COMMENT '最大重试次数',
  `business_context` text COMMENT '业务上下文 (JSON格式)',
  `extended_fields` text COMMENT '扩展字段 (JSON格式)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `deleted` int(11) DEFAULT '0' COMMENT '逻辑删除标识 (0-未删除, 1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_global_transaction_id` (`global_transaction_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息Saga日志表';
```

### 2. 确保服务正常运行
- messages-service 运行在8081端口
- gateway-service 运行在8080端口
- RocketMQ NameServer 可访问 (8.133.246.113:9876)

## API 测试

### 1. 健康检查
```bash
GET http://localhost:8081/api/messages/saga/health
```

### 2. 发送消息到RocketMQ (默认)
```bash
POST http://localhost:8081/api/messages/saga/send
Content-Type: application/json

{
    "messageId": "test-001",
    "content": "Hello RocketMQ! 这是一条测试消息"
}
```

### 3. 发送消息到指定消息中间件
```bash
POST http://localhost:8081/api/messages/saga/send/to
Content-Type: application/json

{
    "messageId": "test-002",
    "content": "Hello RocketMQ! 这是一条指定发送到RocketMQ的测试消息",
    "messageType": "ROCKETMQ",
    "topic": "test-topic"
}
```

### 4. 同步发送消息
```bash
POST http://localhost:8081/api/messages/saga/send/sync
Content-Type: application/json

{
    "messageId": "sync-test-001",
    "content": "这是一条同步发送的测试消息"
}
```

### 5. 通过网关访问
```bash
POST http://localhost:8080/api/messages/saga/send
Content-Type: application/json

{
    "messageId": "test-003",
    "content": "Hello RocketMQ! 通过网关发送的测试消息"
}
```

## 支持的MessageType
- `ROCKETMQ` - 发送到RocketMQ
- `KAFKA` - 发送到Kafka (需要Kafka服务可用)
- `RABBITMQ` - 发送到RabbitMQ (需要RabbitMQ服务可用)

## 预期响应格式

### 成功响应
```json
{
    "success": true,
    "message": "消息发送成功",
    "data": {
        "messageId": "test-001",
        "messageType": "ROCKETMQ",
        "topic": "default-topic"
    },
    "timestamp": 1703123456789
}
```

### 错误响应
```json
{
    "success": false,
    "message": "消息发送失败: 具体错误信息",
    "timestamp": 1703123456789
}
```

## 故障排除

### 1. 如果遇到Kafka连接错误
确保在配置文件中设置：
```yaml
message:
  service:
    kafka:
      enabled: false
```

### 2. 如果遇到RocketMQ连接错误
检查RocketMQ服务是否正常运行：
```bash
telnet 8.133.246.113 9876
```

### 3. 如果遇到Dubbo服务调用错误
```
org.apache.dubbo.rpc.RpcException: No provider available from registry
```
这个错误通常是因为：
- bgai-service 没有启动
- 服务没有正确注册到Nacos
- 网络连接问题

**解决方案**：
- 启动bgai-service
- 检查Nacos服务注册状态
- 或者直接访问messages-service (端口8081)

### 4. 如果遇到网关错误
确保网关路由配置正确，或者直接访问messages-service：
```bash
# 通过网关访问
POST http://localhost:8080/api/messages/saga/send

# 直接访问messages-service
POST http://localhost:8081/api/messages/saga/send
```

### 5. 如果遇到数据库表不存在错误
执行上面的建表SQL语句，创建 `message_saga_log` 表。

## 服务启动顺序

1. **启动Nacos** (8.133.246.113:8848)
2. **启动RocketMQ** (8.133.246.113:9876)
3. **启动bgai-service** (端口8688)
4. **启动messages-service** (端口8081)
5. **启动gateway-service** (端口8080)

## 验证服务状态

```bash
# 检查端口占用
netstat -ano | findstr :8080
netstat -ano | findstr :8081
netstat -ano | findstr :8688

# 检查Nacos服务注册
# 访问 http://8.133.246.113:8848/nacos
# 查看服务列表
```
