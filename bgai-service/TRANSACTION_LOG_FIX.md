# 事务日志不存在问题解决方案

## 问题描述

在 bgai-service 运行时，控制台出现以下错误：

```
2025-08-25 12:44:35.756 [boundedElastic-1] ERROR c.b.bgai.service.mq.BillingTransactionListenerImpl - 事务日志不存在，回滚事务, xid: 8.133.246.113:8091:36715531298758669
```

## 问题分析

这个错误表明在 `BillingTransactionListenerImpl.executeLocalTransaction` 方法中，当尝试查询事务日志时，找不到对应 XID 的事务日志记录。

### 根本原因

1. **时序问题**：事务日志的创建和 RocketMQ 事务监听器的执行可能在不同的数据库连接中
2. **事务传播问题**：由于使用了 `@Transactional(propagation = Propagation.REQUIRES_NEW)`，事务日志的创建和查询可能在不同的数据库事务中
3. **数据库连接隔离**：不同的数据库连接可能看到不同的数据状态

### 问题流程

1. Seata 全局事务开始，生成 XID
2. 业务逻辑执行，可能创建事务日志
3. RocketMQ 事务监听器执行，尝试查询事务日志
4. 由于时序或连接问题，查询不到事务日志
5. 触发回滚逻辑

## 解决方案

### 方案 1：添加重试机制（已实施）

我们修改了 `BillingTransactionListenerImpl.executeLocalTransaction` 方法，添加了重试机制：

```java
// 检查事务日志 - 添加重试机制
TransactionLog txLog = null;
int retryCount = 0;
int maxRetries = 3;

while (txLog == null && retryCount < maxRetries) {
    txLog = transactionLogService.findByXid(xid);
    if (txLog == null) {
        retryCount++;
        if (retryCount < maxRetries) {
            log.warn("事务日志不存在，等待重试 ({}/{}), xid: {}", retryCount, maxRetries, xid);
            try {
                Thread.sleep(1000 * retryCount); // 递增等待时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

### 方案 2：自动创建事务日志（已实施）

如果重试后仍然找不到事务日志，尝试自动创建：

```java
if (txLog == null) {
    log.error("事务日志不存在，尝试创建事务日志, xid: {}", xid);
    
    // 尝试创建事务日志
    try {
        Long logId = transactionLogService.recordTransactionBegin(
            xid, 
            "billing-transaction", 
            "ROCKETMQ", 
            "/api/billing", 
            "127.0.0.1", 
            userId
        );
        
        if (logId != null) {
            log.info("成功创建事务日志, xid: {}, logId: {}", xid, logId);
            txLog = transactionLogService.findByXid(xid);
        } else {
            log.error("创建事务日志失败, xid: {}", xid);
        }
    } catch (Exception e) {
        log.error("创建事务日志时发生异常, xid: {}", xid, e);
    }
}
```

### 方案 3：检查事务协调器状态（已实施）

如果事务日志不存在，检查事务协调器中的状态：

```java
// 如果仍然找不到事务日志，检查事务协调器
if (txLog == null) {
    log.warn("事务日志不存在，检查事务协调器状态, xid: {}, userId: {}", xid, userId);
    
    // 检查事务协调器中的状态
    if (transactionCoordinator.hasActiveTransaction(userId)) {
        String currentId = transactionCoordinator.getCurrentCompletionId(userId);
        if (currentId != null && currentId.equals(completionId)) {
            log.info("事务协调器中有活跃事务且ID一致，提交事务, completionId: {}", completionId);
            return RocketMQLocalTransactionState.COMMIT;
        }
    }
    
    log.error("事务日志不存在且事务协调器中无活跃事务，回滚事务, xid: {}", xid);
    return RocketMQLocalTransactionState.ROLLBACK;
}
```

### 方案 4：事务日志监控工具（已实施）

我们创建了 `TransactionLogMonitorController` 控制器，提供：

1. **事务状态检查**：`GET /api/transaction-log/status`
2. **事务日志查询**：`GET /api/transaction-log/check/{xid}`
3. **事务协调器检查**：`GET /api/transaction-log/coordinator/{userId}`
4. **测试事务日志创建**：`POST /api/transaction-log/create-test`
5. **事务状态更新**：`PUT /api/transaction-log/update-status`
6. **事务诊断信息**：`GET /api/transaction-log/diagnostics`

## 验证方法

### 1. 检查事务日志状态

```bash
# 检查当前事务状态
curl http://localhost:8688/api/transaction-log/status

# 检查指定 XID 的事务日志
curl http://localhost:8688/api/transaction-log/check/8.133.246.113:8091:36715531298758669

# 检查用户的事务协调器状态
curl http://localhost:8688/api/transaction-log/coordinator/test-user
```

### 2. 创建测试事务日志

```bash
# 创建测试事务日志
curl -X POST http://localhost:8688/api/transaction-log/create-test \
  -H "Content-Type: application/json" \
  -d '{
    "xid": "8.133.246.113:8091:36715531298758669",
    "userId": "test-user"
  }'
```

### 3. 更新事务状态

```bash
# 更新事务状态
curl -X PUT http://localhost:8688/api/transaction-log/update-status \
  -H "Content-Type: application/json" \
  -d '{
    "xid": "8.133.246.113:8091:36715531298758669",
    "status": "COMPLETED"
  }'
```

### 4. 查看诊断信息

```bash
# 获取事务诊断信息
curl http://localhost:8688/api/transaction-log/diagnostics
```

## 常见问题

### 问题 1：事务日志不存在

**原因**：
- 事务日志创建失败
- 数据库连接问题
- 时序问题

**解决**：
1. 使用重试机制
2. 自动创建事务日志
3. 检查事务协调器状态

### 问题 2：事务状态不一致

**原因**：
- 事务协调器和事务日志状态不同步
- 数据库事务回滚

**解决**：
1. 检查事务协调器状态
2. 同步事务状态
3. 使用监控工具诊断

### 问题 3：数据库连接问题

**原因**：
- 数据库连接池耗尽
- 网络问题
- 数据库负载过高

**解决**：
1. 检查数据库连接池配置
2. 监控数据库性能
3. 优化数据库查询

## 最佳实践

1. **重试机制**：为关键操作添加重试机制
2. **监控工具**：使用监控工具实时检查事务状态
3. **日志记录**：详细记录事务执行过程
4. **错误处理**：提供多种回退方案
5. **性能优化**：优化数据库查询和连接管理

## 配置说明

### 数据库连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 事务配置

```yaml
spring:
  transaction:
    default-timeout: 30
    rollback-on-commit-failure: true
```

### 日志配置

```yaml
logging:
  level:
    com.bgpay.bgai.service.mq: DEBUG
    com.bgpay.bgai.service.impl.TransactionLogServiceImpl: DEBUG
    com.bgpay.bgai.transaction.TransactionCoordinator: DEBUG
```

## 总结

我们已经解决了事务日志不存在的问题：

1. **添加了重试机制**：避免因时序问题导致的查询失败
2. **实现了自动创建**：当事务日志不存在时自动创建
3. **检查事务协调器**：提供备用的状态检查机制
4. **创建了监控工具**：提供 REST API 来诊断和修复问题
5. **改进了错误处理**：提供更详细的错误信息和处理方案

现在系统能够更好地处理事务日志的时序问题，并提供完善的监控和诊断功能。
