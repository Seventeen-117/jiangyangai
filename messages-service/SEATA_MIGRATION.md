# 消息服务 Seata 迁移指南

## 概述

本项目已将消息服务的分布式事务管理从 Spring StateMachine 迁移到 Alibaba Cloud Seata，以提供更强大和灵活的分布式事务支持。

## 迁移内容

### 1. 依赖变更

**移除的依赖：**
```xml
<!-- Spring StateMachine -->
<dependency>
    <groupId>org.springframework.statemachine</groupId>
    <artifactId>spring-statemachine-starter</artifactId>
    <version>3.2.1</version>
</dependency>
```

**新增的依赖：**
```xml
<!-- Alibaba Cloud Seata -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
    <version>2023.0.1.2</version>
</dependency>
```

### 2. 核心类变更

#### 原实现：MessageSagaStateMachine.java
- 使用 Spring StateMachine 构建状态机
- 通过状态和事件管理事务流程
- 需要手动管理状态转换

#### 新实现：MessageSagaStateMachine.java
- 使用 Seata 的 `@GlobalTransactional` 注解
- 通过分布式事务自动管理事务流程
- 自动处理事务回滚和补偿

### 3. 新增组件

#### SeataConfig.java
- 配置 Seata 全局事务扫描器
- 启用分布式事务功能

#### MessageSagaService.java
- 提供高级业务接口
- 封装 Saga 事务调用逻辑
- 提供同步和异步两种调用方式

#### MessageSagaController.java
- 提供 RESTful API 接口
- 支持各种消息处理场景
- 包含健康检查接口

## 使用方法

### 1. 基本配置

确保在 `application.yml` 中配置了 Seata：

```yaml
spring:
  cloud:
    alibaba:
      seata:
        tx-service-group: messages-service-group
        service:
          vgroup-mapping:
            messages-service-group: default
          grouplist:
            default: 127.0.0.1:8091
        enable-auto-data-source-proxy: true
```

### 2. API 使用示例

#### 发送消息（Saga事务）
```bash
POST /api/messages/saga/send
Content-Type: application/json

{
  "messageId": "msg-001",
  "content": "Hello World"
}
```

#### 消费消息（Saga事务）
```bash
POST /api/messages/saga/consume
Content-Type: application/json

{
  "messageId": "msg-001",
  "content": "Hello World"
}
```

#### 批量处理消息（Saga事务）
```bash
POST /api/messages/saga/batch
Content-Type: application/json

{
  "batchId": "batch-001",
  "messageIds": ["msg-001", "msg-002", "msg-003"]
}
```

#### 事务消息处理（Saga事务）
```bash
POST /api/messages/saga/transaction
Content-Type: application/json

{
  "transactionId": "tx-001",
  "messageId": "msg-001",
  "content": "Hello World"
}
```

### 3. 编程方式使用

```java
@Autowired
private MessageSagaService messageSagaService;

// 发送消息
messageSagaService.sendMessageWithSaga("msg-001", "Hello World");

// 消费消息
messageSagaService.consumeMessageWithSaga("msg-001", "Hello World");

// 批量处理
String[] messageIds = {"msg-001", "msg-002", "msg-003"};
messageSagaService.processBatchMessagesWithSaga("batch-001", messageIds);

// 事务消息处理
messageSagaService.processTransactionMessageWithSaga("tx-001", "msg-001", "Hello World");
```

## 优势对比

### Spring StateMachine 的局限性
1. 状态机配置复杂，需要定义所有状态和转换
2. 缺乏分布式事务支持
3. 需要手动处理事务回滚
4. 扩展性有限

### Seata 的优势
1. **自动分布式事务管理**：使用 `@GlobalTransactional` 注解自动管理事务
2. **自动补偿机制**：失败时自动回滚和补偿
3. **高性能**：基于 AT 模式，性能优异
4. **易于扩展**：支持多种事务模式（AT、TCC、Saga）
5. **监控友好**：提供完善的监控和日志

## 事务模式

### AT 模式（自动事务）
- 适用于关系型数据库
- 自动生成反向 SQL
- 零侵入性

### TCC 模式（手动事务）
- 适用于非关系型数据库
- 需要手动实现 Try、Confirm、Cancel
- 性能更好

### Saga 模式（长事务）
- 适用于长业务流程
- 支持正向和反向操作
- 适合复杂业务场景

## 监控和日志

### 事务日志
Seata 会自动记录事务日志，包括：
- 全局事务 ID（XID）
- 分支事务信息
- 事务状态变化
- 补偿操作记录

### 监控接口
```bash
GET /api/messages/saga/health
```

## 注意事项

1. **Seata Server 部署**：确保 Seata Server 已正确部署并运行
2. **数据库配置**：确保数据库支持 Seata 的 AT 模式
3. **网络连接**：确保应用能够连接到 Seata Server
4. **事务超时**：合理设置事务超时时间
5. **异常处理**：正确处理事务异常和补偿逻辑

## 故障排查

### 常见问题

1. **Seata Server 连接失败**
   - 检查 Seata Server 是否启动
   - 检查网络连接
   - 检查配置文件中的地址和端口

2. **事务回滚失败**
   - 检查数据库连接
   - 检查反向 SQL 是否正确生成
   - 检查事务日志

3. **性能问题**
   - 调整事务超时时间
   - 优化数据库查询
   - 考虑使用 TCC 模式

### 日志查看
```bash
# 查看 Seata 日志
tail -f logs/seata.log

# 查看应用日志
tail -f logs/messages-service.log
```

## 迁移检查清单

- [ ] 更新 pom.xml 依赖
- [ ] 配置 Seata 相关配置
- [ ] 更新业务代码使用 `@GlobalTransactional`
- [ ] 测试各种事务场景
- [ ] 验证事务回滚和补偿
- [ ] 更新文档和 API 说明
- [ ] 部署 Seata Server
- [ ] 性能测试和优化

## 总结

通过迁移到 Alibaba Cloud Seata，消息服务获得了更强大的分布式事务支持，简化了事务管理，提高了系统的可靠性和可维护性。Seata 的自动补偿机制和丰富的监控功能使得分布式事务处理变得更加简单和可靠。
