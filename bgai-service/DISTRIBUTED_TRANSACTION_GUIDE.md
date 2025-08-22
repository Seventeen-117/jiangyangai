# bgai-service 分布式事务查看指南

## 概述

本指南介绍如何在 `bgai-service` 中查看和监控分布式事务的执行过程。

## 事务日志查看

### 1. 应用日志

启动应用后，在控制台或日志文件中可以看到以下类型的事务日志：

```
[main] INFO com.bgpay.bgai.service.BgaiTransactionService - BgaiService 事务开始: XID=192.168.1.100:8091:1234567890, name=deepseek-process-tx
[main] INFO com.bgpay.bgai.transaction.TransactionCoordinator - Transaction prepared for user test-user-001: chatCompletionId=chat-uuid-123
[main] INFO com.bgpay.bgai.service.BgaiTransactionService - BgaiService 事务提交成功: XID=192.168.1.100:8091:1234567890
```

### 2. Seata 事务日志

Seata 会在数据库中记录事务信息，可以通过以下 SQL 查询查看：

```sql
-- 查看全局事务表
SELECT * FROM global_table ORDER BY begin_time DESC LIMIT 10;

-- 查看分支事务表
SELECT * FROM branch_table ORDER BY begin_time DESC LIMIT 10;

-- 查看锁表
SELECT * FROM lock_table ORDER BY gmt_create DESC LIMIT 10;
```

### 3. 业务日志表

查看业务相关的日志表：

```sql
-- 查看聊天完成记录
SELECT * FROM chat_completions ORDER BY created DESC LIMIT 10;

-- 查看使用信息
SELECT * FROM usage_info ORDER BY created_at DESC LIMIT 10;

-- 查看事务协调器日志
SELECT * FROM transaction_log ORDER BY created_time DESC LIMIT 10;
```

## 事务状态监控

### 1. 通过 API 查看事务状态

```bash
# 查看特定用户的事务状态
curl -X GET http://localhost:8080/api/transaction/status/{userId}
```

### 2. 通过日志查看事务状态

在应用日志中搜索以下关键词：
- `BgaiService 事务开始`
- `BgaiService 事务提交成功`
- `BgaiService 事务回滚`
- `Transaction prepared`
- `Transaction committed`
- `Transaction rollback`

## 事务流程说明

### 1. 正常流程

1. **事务开始**: `BgaiTransactionService.executeWithTransaction()` 被调用
2. **准备阶段**: `TransactionCoordinator.prepare()` 创建事务记录
3. **业务执行**: 执行 DeepSeek API 调用和数据处理
4. **提交阶段**: `TransactionCoordinator.commit()` 提交事务
5. **事务完成**: Seata 全局事务提交成功

### 2. 异常流程

1. **事务开始**: `BgaiTransactionService.executeWithTransaction()` 被调用
2. **准备阶段**: `TransactionCoordinator.prepare()` 创建事务记录
3. **业务执行**: 执行过程中发生异常
4. **回滚阶段**: `TransactionCoordinator.rollback()` 回滚事务
5. **事务回滚**: Seata 全局事务回滚成功

## 调试技巧

### 1. 启用详细日志

在 `application-dev.yml` 中设置日志级别：

```yaml
logging:
  level:
    io.seata: DEBUG
    com.bgpay.bgai: DEBUG
    com.jiangyang.base: DEBUG
```

### 2. 查看事务 XID

每个事务都有唯一的 XID，可以通过以下方式查看：

```java
// 在代码中获取当前事务 XID
String currentXid = bgaiTransactionService.getCurrentXid();
log.info("Current transaction XID: {}", currentXid);
```

### 3. 检查事务状态

```java
// 检查是否有活跃事务
boolean hasActiveTransaction = bgaiTransactionService.hasActiveTransaction();
log.info("Has active transaction: {}", hasActiveTransaction);
```

## 常见问题排查

### 1. 事务未开始

**现象**: 没有看到 "BgaiService 事务开始" 日志

**可能原因**:
- Seata 配置不正确
- Nacos 连接失败
- 数据源代理未启用

**排查步骤**:
1. 检查 Seata 配置是否正确
2. 确认 Nacos 服务是否可用
3. 检查数据源配置

### 2. 事务提交失败

**现象**: 看到 "BgaiService 事务回滚" 日志

**可能原因**:
- 业务逻辑异常
- 数据库连接问题
- 网络超时

**排查步骤**:
1. 查看异常堆栈信息
2. 检查数据库连接状态
3. 确认网络连接正常

### 3. 事务状态不一致

**现象**: 业务数据与事务状态不一致

**可能原因**:
- 事务超时
- 补偿机制未正确执行
- 数据源切换问题

**排查步骤**:
1. 检查事务超时配置
2. 查看补偿逻辑是否正确
3. 确认数据源配置

## 性能监控

### 1. 事务执行时间

监控事务执行时间，避免长事务：

```java
long startTime = System.currentTimeMillis();
// 执行事务
long endTime = System.currentTimeMillis();
log.info("Transaction execution time: {}ms", endTime - startTime);
```

### 2. 事务成功率

通过日志统计事务成功率：

```bash
# 统计成功事务数
grep "BgaiService 事务提交成功" application.log | wc -l

# 统计失败事务数
grep "BgaiService 事务回滚" application.log | wc -l
```

## 最佳实践

1. **合理设置超时时间**: 根据业务复杂度设置合适的事务超时时间
2. **避免长事务**: 不要在事务中执行耗时操作
3. **正确处理异常**: 确保异常能够正确触发事务回滚
4. **监控事务状态**: 定期检查事务执行情况
5. **记录详细日志**: 保留足够的事务执行日志用于问题排查

## 总结

通过以上方法，可以全面监控和查看 `bgai-service` 中分布式事务的执行过程。建议在生产环境中启用详细的日志记录，并定期检查事务执行情况，确保系统的稳定性和可靠性。
