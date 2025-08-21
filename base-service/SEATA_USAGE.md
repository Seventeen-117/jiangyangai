# Seata分布式事务使用说明

## 概述

`base-service` 已经集成了完整的 Seata 分布式事务功能，其他服务只需要引入 `base-service` 依赖即可使用分布式事务。

**重要提示**：由于移除了硬编码默认值，使用前必须正确配置 Seata 相关参数。

## 功能特性

- ✅ 自动配置 Seata 相关组件
- ✅ 支持 AT 模式（自动模式）
- ✅ 支持 Saga 模式（可选）
- ✅ 提供事务监听和拦截
- ✅ 提供事务服务接口
- ✅ 提供工具类方法
- ✅ 支持 Nacos 注册中心和配置中心
- ✅ 完全外部化配置，无硬编码默认值

## 快速开始

### 1. 引入依赖

在你的服务 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.jiangyang</groupId>
    <artifactId>base-service</artifactId>
    <version>1.0.0-Final</version>
</dependency>
```

### 2. 配置 Seata（必需）

**重要**：由于移除了硬编码默认值，以下配置是必需的：

在你的服务配置文件（如 `application.yml` 或 `application-dev.yml`）中添加：

```yaml
seata:
  # 基础配置
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: ${spring.application.name}-tx-group
  timeout: 60000
  enable-auto-data-source-proxy: true
  data-source-proxy-mode: AT
  
  # 注册中心配置（必需）
  registry:
    type: nacos
    server-addr: localhost:8848
    group: SEATA_GROUP
  
  # 配置中心配置（必需）
  config:
    type: nacos
    server-addr: localhost:8848
    group: SEATA_GROUP
  
  # 事务存储配置
  store:
    mode: file
    file-dir: file_store/data
```

### 3. 使用 @GlobalTransactional 注解

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OrderService orderService;
    
    @GlobalTransactional(name = "create-user-order", rollbackFor = Exception.class)
    public void createUserAndOrder(User user, Order order) {
        // 创建用户
        userMapper.insert(user);
        
        // 创建订单
        orderService.createOrder(order);
        
        // 如果任何一步失败，整个事务会回滚
    }
}
```

## 配置说明

### 必需配置项

以下配置项是必需的，缺少任何一个都可能导致 Seata 无法正常工作：

| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| `seata.enabled` | 是否启用 Seata | `true` |
| `seata.application-id` | 应用ID | `${spring.application.name}` |
| `seata.tx-service-group` | 事务服务组 | `${spring.application.name}-tx-group` |
| `seata.registry.type` | 注册中心类型 | `nacos` |
| `seata.registry.server-addr` | 注册中心地址 | `localhost:8848` |
| `seata.registry.group` | 注册中心分组 | `SEATA_GROUP` |
| `seata.config.type` | 配置中心类型 | `nacos` |
| `seata.config.server-addr` | 配置中心地址 | `localhost:8848` |
| `seata.config.group` | 配置中心分组 | `SEATA_GROUP` |

### 可选配置项

| 配置项 | 说明 | 默认值 | 示例值 |
|--------|------|--------|--------|
| `seata.timeout` | 事务超时时间（毫秒） | 无默认值 | `60000` |
| `seata.enable-auto-data-source-proxy` | 是否启用数据源代理 | 无默认值 | `true` |
| `seata.data-source-proxy-mode` | 数据源代理模式 | 无默认值 | `AT` |
| `seata.store.mode` | 存储模式 | 无默认值 | `file` 或 `db` |

### 完整配置示例

```yaml
seata:
  # 基础配置
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: ${spring.application.name}-tx-group
  timeout: 60000
  enable-auto-data-source-proxy: true
  data-source-proxy-mode: AT
  
  # 注册中心配置
  registry:
    type: nacos
    server-addr: localhost:8848
    namespace: ""
    group: SEATA_GROUP
    cluster: default
    username: ""
    password: ""
  
  # 配置中心配置
  config:
    type: nacos
    server-addr: localhost:8848
    namespace: ""
    group: SEATA_GROUP
    username: ""
    password: ""
  
  # 事务存储配置
  store:
    # 文件存储模式
    mode: file
    file-dir: file_store/data
    
    # 数据库存储模式（可选）
    # mode: db
    # database:
    #   datasource: druid
    #   db-type: mysql
    #   driver-class-name: com.mysql.cj.jdbc.Driver
    #   url: jdbc:mysql://localhost:3306/seata
    #   username: root
    #   password: password
    #   min-conn: 1
    #   max-conn: 20
    #   global-table: global_table
    #   branch-table: branch_table
    #   lock-table: lock_table
    #   query-timeout: 60
  
  # Saga配置（可选）
  saga:
    enabled: false
    state-machine-auto-register: false
```

## 高级用法

### 1. 使用事务服务接口

```java
@Service
public class TransactionService {
    
    @Autowired
    private SeataTransactionService seataTransactionService;
    
    public void executeWithTransaction() {
        // 创建全局事务
        GlobalTransaction globalTransaction = seataTransactionService.createGlobalTransaction();
        
        try {
            // 开始事务
            seataTransactionService.beginGlobalTransaction("my-transaction", 60000);
            
            // 执行业务逻辑
            // ...
            
            // 提交事务
            seataTransactionService.commitGlobalTransaction(globalTransaction.getXid());
            
        } catch (Exception e) {
            // 回滚事务
            seataTransactionService.rollbackGlobalTransaction(globalTransaction.getXid());
            throw e;
        }
    }
}
```

### 2. 使用工具类

```java
@Service
public class BusinessService {
    
    public void executeInTransaction() {
        // 在事务中执行操作
        String result = SeataUtils.executeInTransaction("business-transaction", 30000, () -> {
            // 执行业务逻辑
            return "success";
        });
    }
    
    public void propagateTransaction() {
        // 获取当前事务ID
        String xid = SeataUtils.getCurrentXid();
        
        // 将事务ID传播到消息
        String messageXid = SeataUtils.getXidForMessage();
        
        // 在消息消费时恢复事务ID
        SeataUtils.restoreXidFromMessage(messageXid);
    }
}
```

### 3. 手动管理事务

```java
@Service
public class ManualTransactionService {
    
    public void manualTransaction() {
        String xid = null;
        
        try {
            // 创建全局事务
            GlobalTransaction globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
            globalTransaction.begin(60000, "manual-transaction");
            xid = globalTransaction.getXid();
            
            // 执行业务逻辑
            // ...
            
            // 提交事务
            globalTransaction.commit();
            
        } catch (Exception e) {
            // 回滚事务
            if (xid != null) {
                try {
                    GlobalTransactionContext.reload(xid).rollback();
                } catch (TransactionException te) {
                    log.error("回滚事务失败", te);
                }
            }
            throw e;
        }
    }
}
```

## 注意事项

1. **配置必需性**：由于移除了硬编码默认值，所有配置项都必须从配置文件中读取
2. **确保 Seata Server 已启动**：Seata 需要独立的 Seata Server 服务
3. **数据库表结构**：使用 AT 模式时需要创建 `undo_log` 表
4. **数据源代理**：确保数据源被正确代理
5. **事务传播**：注意事务的传播行为
6. **异常处理**：合理处理事务异常

## 故障排除

### 常见问题

1. **配置缺失错误**
   - 检查是否配置了所有必需的配置项
   - 确保配置文件的格式正确
   - 检查配置项的名称是否正确

2. **事务不生效**
   - 检查 `@GlobalTransactional` 注解是否正确
   - 检查 Seata 配置是否正确
   - 检查数据源是否被代理

3. **事务回滚失败**
   - 检查异常是否被正确抛出
   - 检查 `rollbackFor` 配置
   - 检查数据库连接状态

4. **性能问题**
   - 调整事务超时时间
   - 优化数据库操作
   - 使用合适的事务隔离级别

### 日志调试

启用 Seata 调试日志：

```yaml
logging:
  level:
    io.seata: DEBUG
    com.jiangyang.base.seata: DEBUG
```

### 配置验证

启动应用时，检查日志中是否有以下信息：

```
开始验证Seata配置...
验证基础配置...
✅ seata.application-id = your-service-name
✅ seata.tx-service-group = your-service-name-tx-group
验证注册中心配置...
✅ seata.registry.type = nacos
✅ seata.registry.server-addr = localhost:8848
✅ seata.registry.group = SEATA_GROUP
验证配置中心配置...
✅ seata.config.type = nacos
✅ seata.config.server-addr = localhost:8848
✅ seata.config.group = SEATA_GROUP
验证存储配置...
✅ seata.store.mode = file
✅ Seata配置验证通过
Seata配置初始化完成: applicationName=your-service-name
Seata系统属性初始化完成
Seata事务监听器初始化完成
应用程序已就绪，Seata事务监听器激活
```

如果看到 `❌ Seata配置验证失败` 的错误信息，请检查相应的配置项。

## 示例项目

参考 `bgai-service` 中的 Seata 使用示例：

- `SeataTransactionListener`：事务监听器
- `SeataTransactionInterceptor`：事务拦截器
- `@GlobalTransactional` 注解的使用

## 更多信息

- [Seata 官方文档](https://seata.io/zh-cn/docs/overview/what-is-seata.html)
- [Spring Cloud Alibaba Seata](https://github.com/alibaba/spring-cloud-alibaba/wiki/Seata)
- [Seata 配置参数说明](https://seata.io/zh-cn/docs/user/configurations.html)
