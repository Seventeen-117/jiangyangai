# bgai-service 分布式事务优化指南

## 概述

本指南介绍如何在 `bgai-service` 中优化分布式事务的使用，充分利用 `base-service` 提供的 `SeataTransactionService` 功能。

## 优化前的问题

1. **Seata 配置被移除**：`application-dev.yml` 中 Seata 配置被完全移除
2. **包扫描配置缺失**：主应用类没有扫描 `com.jiangyang.base` 包
3. **事务服务未充分利用**：没有使用 `base-service` 提供的 `SeataTransactionService`
4. **事务管理复杂**：手动管理事务容易出错

## 优化方案

### 1. 配置优化

#### 1.1 恢复 Seata 配置
在 `application-dev.yml` 中添加完整的 Seata 配置：

```yaml
# Seata分布式事务配置
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: ${spring.application.name}-tx-group
  timeout: 60000
  enable-auto-data-source-proxy: true
  data-source-proxy-mode: AT

  # 注册中心配置
  registry:
    type: nacos
    nacos:
      server-addr: 8.133.246.113:8848
      namespace: 4a49080d-0e41-4a48-9013-4a30a5c3ffb6
      group: SEATA_GROUP
      cluster: default
      username: ""
      password: ""

  # 配置中心配置
  config:
    type: nacos
    nacos:
      server-addr: 8.133.246.113:8848
      namespace: 4a49080d-0e41-4a48-9013-4a30a5c3ffb6
      group: SEATA_GROUP
      data-id: seataServer.properties
      username: ""
      password: ""
```

#### 1.2 修复包扫描配置
在 `BgaiApplication.java` 中添加 `com.jiangyang.base` 包扫描：

```java
@ComponentScan(basePackages = {
    "org.apache.rocketmq.spring.autoconfigure",
    "org.apache.rocketmq.spring.core",
    "org.apache.rocketmq.spring.support",
    "com.bgpay.bgai",
    "com.jiangyang.base"  // 添加这一行
})
```

### 2. 创建 BgaiTransactionService

创建 `BgaiTransactionService` 类，集成 `base-service` 的 `SeataTransactionService`：

```java
@Service
@Slf4j
public class BgaiTransactionService {

    @Autowired
    private SeataTransactionService seataTransactionService;

    /**
     * 执行带事务的业务逻辑（推荐方式）
     */
    public <T> T executeWithTransaction(String transactionName, int timeout, Supplier<T> supplier) {
        GlobalTransaction tx = seataTransactionService.createGlobalTransaction();
        String xid = null;
        
        try {
            seataTransactionService.beginGlobalTransaction(transactionName, timeout);
            xid = tx.getXid();
            
            log.info("BgaiService 事务开始: XID={}, name={}", xid, transactionName);
            
            T result = supplier.get();
            
            seataTransactionService.commitGlobalTransaction(xid);
            log.info("BgaiService 事务提交成功: XID={}", xid);
            
            return result;
            
        } catch (Exception e) {
            if (xid != null) {
                seataTransactionService.rollbackGlobalTransaction(xid);
                log.error("BgaiService 事务回滚: XID={}, error={}", xid, e.getMessage(), e);
            }
            throw new RuntimeException("事务执行失败: " + e.getMessage(), e);
        }
    }

    // 其他方法...
}
```

### 3. 使用方式对比

#### 3.1 优化前：使用 @GlobalTransactional 注解

```java
@GlobalTransactional(name = "bgai-transaction", rollbackFor = Exception.class)
public void businessMethod() {
    // 业务逻辑
}
```

#### 3.2 优化后：使用 BgaiTransactionService（推荐）

```java
@Autowired
private BgaiTransactionService bgaiTransactionService;

public ChatResponse processChatRequest(String content, String userId) {
    return bgaiTransactionService.executeWithTransaction(
        "chat-process-tx", 
        60000, 
        () -> {
            // 业务逻辑
            String chatCompletionId = transactionCoordinator.prepare(userId);
            ChatResponse response = processChatInternal(content, userId);
            transactionCoordinator.commit(userId, chatCompletionId, "deepseek-chat");
            return response;
        }
    );
}
```

### 4. 高级功能

#### 4.1 带重试的事务处理

```java
public ChatResponse processChatRequestWithRetry(String content, String userId) {
    return bgaiTransactionService.executeWithTransactionAndRetry(
        "chat-process-retry-tx", 
        60000, 
        3, // 最大重试3次
        () -> {
            // 业务逻辑
            return processChatInternal(content, userId);
        }
    );
}
```

#### 4.2 Saga 模式事务处理（带补偿）

```java
public ChatResponse processChatRequestWithSaga(String content, String userId) {
    return bgaiTransactionService.executeWithCompensation(
        "chat-process-saga-tx", 
        60000, 
        () -> {
            // 业务逻辑
            return processChatInternal(content, userId);
        },
        () -> {
            // 补偿逻辑
            transactionCoordinator.rollback(userId);
        }
    );
}
```

#### 4.3 手动管理事务（高级用法）

```java
public ChatResponse processChatRequestManual(String content, String userId) {
    String xid = null;
    try {
        var tx = bgaiTransactionService.beginTransaction("chat-process-manual-tx", 60000);
        xid = tx.getXid();
        
        // 业务逻辑
        ChatResponse response = processChatInternal(content, userId);
        
        bgaiTransactionService.commitTransaction(xid);
        return response;
        
    } catch (Exception e) {
        if (xid != null) {
            bgaiTransactionService.rollbackTransaction(xid);
        }
        throw new RuntimeException("手动事务处理失败", e);
    }
}
```

### 5. 事务状态监控

#### 5.1 检查事务状态

```java
public void checkTransactionStatus(String userId) {
    String currentXid = bgaiTransactionService.getCurrentXid();
    boolean hasActiveTransaction = bgaiTransactionService.hasActiveTransaction();
    
    log.info("事务状态检查: userId={}, currentXid={}, hasActiveTransaction={}", 
            userId, currentXid, hasActiveTransaction);
}
```

### 6. 最佳实践

#### 6.1 事务命名规范

```java
// 好的命名
"chat-process-tx"
"user-billing-tx"
"file-upload-tx"

// 避免的命名
"tx1"
"transaction"
"test"
```

#### 6.2 超时时间设置

```java
// 根据业务复杂度设置合适的超时时间
bgaiTransactionService.executeWithTransaction("simple-tx", 30000, supplier);  // 简单业务
bgaiTransactionService.executeWithTransaction("complex-tx", 120000, supplier); // 复杂业务
```

#### 6.3 异常处理

```java
public ChatResponse processChatRequest(String content, String userId) {
    try {
        return bgaiTransactionService.executeWithTransaction(
            "chat-process-tx", 
            () -> {
                // 业务逻辑
                return processChatInternal(content, userId);
            }
        );
    } catch (Exception e) {
        log.error("聊天请求处理失败: userId={}, error={}", userId, e.getMessage(), e);
        // 返回错误响应
        return createErrorResponse(e.getMessage());
    }
}
```

### 7. 监控和调试

#### 7.1 日志配置

```yaml
logging:
  level:
    io.seata: DEBUG
    com.bgpay.bgai: DEBUG
    com.jiangyang.base: DEBUG
```

#### 7.2 事务日志示例

```
[main] INFO com.bgpay.bgai.service.BgaiTransactionService - BgaiService 事务开始: XID=192.168.1.100:8091:1234567890, name=chat-process-tx
[main] INFO com.bgpay.bgai.transaction.TransactionCoordinator - Transaction prepared for user test-user-001: chatCompletionId=chat-uuid-123
[main] INFO com.bgpay.bgai.service.BgaiTransactionService - BgaiService 事务提交成功: XID=192.168.1.100:8091:1234567890
```

### 8. 性能优化

#### 8.1 避免长事务

```java
// 避免在事务中执行耗时操作
bgaiTransactionService.executeWithTransaction("chat-process-tx", () -> {
    // 快速的数据操作
    String chatCompletionId = transactionCoordinator.prepare(userId);
    
    // 避免在事务中执行耗时操作
    // CompletableFuture.runAsync(() -> {
    //     processChatInternal(content, userId); // 异步处理
    // });
    
    return "success";
});
```

#### 8.2 合理设置重试次数

```java
// 根据业务特点设置重试次数
bgaiTransactionService.executeWithTransactionAndRetry(
    "chat-process-retry-tx", 
    60000, 
    2, // 对于聊天业务，重试2次通常足够
    supplier
);
```

## 总结

通过以上优化，`bgai-service` 可以获得以下好处：

1. **更好的事务管理**：使用 `BgaiTransactionService` 简化事务管理
2. **更强的容错能力**：支持重试和补偿机制
3. **更好的监控**：详细的事务日志和状态监控
4. **更高的性能**：合理的事务超时和重试策略
5. **更好的可维护性**：统一的事务管理接口

建议优先使用 `BgaiTransactionService` 的简化方法，对于特殊场景可以使用手动管理或 Saga 模式。
