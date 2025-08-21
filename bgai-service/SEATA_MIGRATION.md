# BgaiService Seata 迁移说明

## 概述

本文档说明如何将 bgai-service 中的 Seata 分布式事务配置迁移到使用 base-service 提供的统一 Seata 功能。

## 迁移内容

### 已删除的配置类

以下配置类已被删除，其功能由 base-service 提供：

1. **SeataAuthConfig.java** - Seata认证配置
   - 功能：设置系统属性，禁用状态机自动注册
   - 替代：由 base-service 的 `SeataSystemPropertiesInitializer` 提供

2. **SeataSagaConfig.java** - Seata Saga配置
   - 功能：自定义 DbStateMachineConfig，避免重复注册
   - 替代：由 base-service 的 `SeataSagaConfig` 提供

3. **SeataTransactionListener.java** - Seata事务监听器
   - 功能：监听事务生命周期，记录事务状态
   - 替代：由 base-service 的 `SeataTransactionListener` 提供

### 保留的配置类

以下配置类被保留，因为它们是 bgai-service 特有的功能：

1. **SagaStateMachineConfig.java** - Saga状态机配置
   - 原因：bgai-service 有特殊的 Saga 状态机需求
   - 功能：自定义状态机定义文件加载、自定义解析器等
   - 修改：移除了与 base-service 冲突的配置

2. **BgaiSeataTransactionListener.java** - BgaiService特有的Seata事务监听器
   - 原因：bgai-service 需要记录业务特有的事务日志
   - 功能：扩展 base-service 的事务监听，添加 TransactionLogService 集成

## 配置变更

### 配置文件变更

**application-dev.yml** 中的 Seata 配置已更新：

```yaml
# 旧配置
seata:
  enabled: true
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default
    grouplist:
      default: 8.133.246.113:8091
  registry:
    type: file
    file:
      name: file.conf
  config:
    type: file
    file:
      name: file.conf

# 新配置
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
    server-addr: ${NACOS_HOST:8.133.246.113}:${NACOS_PORT:8848}
    namespace: ${NACOS_NAMESPACE:d750d92e-152f-4055-a641-3bc9dda85a29}
    group: SEATA_GROUP
    cluster: default
  
  # 配置中心配置
  config:
    type: nacos
    server-addr: ${NACOS_HOST:8.133.246.113}:${NACOS_PORT:8848}
    namespace: ${NACOS_NAMESPACE:d750d92e-152f-4055-a641-3bc9dda85a29}
    group: SEATA_GROUP
  
  # 事务存储配置
  store:
    mode: file
    file-dir: file_store/data
  
  # Saga配置（bgai-service特有）
  saga:
    enabled: true
    state-machine-auto-register: false
```

### 主要变更点

1. **配置方式**：从文件配置改为 Nacos 配置中心
2. **事务服务组**：从 `default_tx_group` 改为 `${spring.application.name}-tx-group`
3. **注册中心**：从文件注册改为 Nacos 注册
4. **配置中心**：从文件配置改为 Nacos 配置中心
5. **新增配置**：添加了数据源代理、超时时间等配置

## 功能对比

### Base-Service 提供的功能

✅ **自动配置** - 自动启用 Seata 相关组件  
✅ **事务监听** - 基础的事务生命周期监听  
✅ **事务拦截** - 自动增强 @GlobalTransactional 方法  
✅ **配置验证** - 启动时自动验证配置  
✅ **系统属性设置** - 自动设置必要的系统属性  
✅ **Saga 基础配置** - 提供基础的 Saga 配置  

### BgaiService 特有的功能

✅ **自定义 Saga 状态机** - 特殊的状态机定义文件加载  
✅ **自定义 JSON 解析器** - CustomSagaJsonParser  
✅ **业务事务日志** - TransactionLogService 集成  
✅ **扩展事务监听** - 额外的业务日志记录  

## 使用方式

### 1. 基础事务使用

```java
@GlobalTransactional(name = "bgai-transaction", rollbackFor = Exception.class)
public void businessMethod() {
    // 业务逻辑
}
```

### 2. 使用 Base-Service 的工具类

```java
@Autowired
private SeataTransactionService seataTransactionService;

public void executeWithTransaction() {
    GlobalTransaction tx = seataTransactionService.createGlobalTransaction();
    try {
        seataTransactionService.beginGlobalTransaction("bgai-tx", 60000);
        // 业务逻辑
        seataTransactionService.commitGlobalTransaction(tx.getXid());
    } catch (Exception e) {
        seataTransactionService.rollbackGlobalTransaction(tx.getXid());
        throw e;
    }
}
```

### 3. 使用工具类

```java
SeataUtils.executeInTransaction("bgai-business-tx", 30000, () -> {
    // 业务逻辑
    return "success";
});
```

## 验证迁移

### 启动日志验证

启动应用时，应该看到以下日志：

```
开始验证Seata配置...
验证基础配置...
✅ seata.application-id = bgai-service
✅ seata.tx-service-group = bgai-service-tx-group
验证注册中心配置...
✅ seata.registry.type = nacos
✅ seata.registry.server-addr = 8.133.246.113:8848
✅ seata.registry.group = SEATA_GROUP
验证配置中心配置...
✅ seata.config.type = nacos
✅ seata.config.server-addr = 8.133.246.113:8848
✅ seata.config.group = SEATA_GROUP
验证存储配置...
✅ seata.store.mode = file
✅ Seata配置验证通过
Seata配置初始化完成: applicationName=bgai-service
Seata系统属性初始化完成
Seata事务监听器初始化完成
应用程序已就绪，Seata事务监听器激活
==================== Saga状态机加载报告 ====================
Saga状态机配置加载状态: 成功
已加载的状态机定义文件数量: X
基础Seata功能由base-service提供
=============================================================
```

### 功能验证

1. **基础事务功能**：使用 @GlobalTransactional 注解的方法正常工作
2. **Saga 功能**：自定义状态机定义文件正常加载
3. **事务日志**：TransactionLogService 正常记录事务状态
4. **配置验证**：启动时配置验证正常通过

## 注意事项

1. **配置必需性**：所有 Seata 配置项都必须从配置文件中读取，无硬编码默认值
2. **Nacos 连接**：确保 Nacos 服务正常运行，配置中心和注册中心可访问
3. **Seata Server**：确保 Seata Server 已启动并正确配置
4. **数据源代理**：确保数据源被正确代理
5. **事务传播**：注意事务的传播行为

## 故障排除

### 常见问题

1. **配置验证失败**
   - 检查 Seata 配置是否完整
   - 确保 Nacos 连接正常
   - 检查配置项名称是否正确

2. **事务不生效**
   - 检查 @GlobalTransactional 注解是否正确
   - 检查数据源是否被代理
   - 检查 Seata Server 是否正常运行

3. **Saga 状态机加载失败**
   - 检查状态机定义文件是否存在
   - 检查 CustomSagaJsonParser 是否正常工作
   - 检查数据库连接是否正常

### 日志调试

启用详细日志：

```yaml
logging:
  level:
    io.seata: DEBUG
    com.jiangyang.base.seata: DEBUG
    com.bgpay.bgai.seata: DEBUG
```

## 总结

通过本次迁移，bgai-service 现在使用 base-service 提供的统一 Seata 功能，同时保留了自身特有的 Saga 功能。这样可以：

1. **减少重复代码** - 基础 Seata 功能由 base-service 统一提供
2. **提高维护性** - 配置更加标准化和统一
3. **增强功能** - 获得 base-service 提供的配置验证、工具类等功能
4. **保持灵活性** - 保留 bgai-service 特有的业务功能
