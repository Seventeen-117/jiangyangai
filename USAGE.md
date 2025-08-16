# Base Service 使用指南

## 概述

`base-service` 是一个通用的多数据源切换服务，其他服务只需要引入依赖和添加注解即可实现数据源切换，无需自己实现多数据源逻辑。

## 在其他服务中使用

### 1. 引入依赖

在您的服务项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.jiangyang</groupId>
    <artifactId>base-service</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置数据源

在您的配置文件（`application.yml` 或 `bootstrap.yml`）中添加数据源配置：

```yaml
spring:
  datasource:
    dynamic:
      enabled: true
      primary: master
      strict: false
      seata: true
      
      datasource:
        # 主数据源
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://your-db-host:3306/your_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false
          username: your_username
          password: your_password
          type: com.alibaba.druid.pool.DruidDataSource
          pool:
            initial-size: 5
            min-idle: 10
            max-active: 20
            max-wait: 60000
            validation-query: SELECT 1
            test-while-idle: true
            filters: stat,wall
        
        # 从数据源
        slave:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://your-slave-host:3306/your_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false
          username: your_username
          password: your_password
          type: com.alibaba.druid.pool.DruidDataSource
          pool:
            initial-size: 5
            min-idle: 10
            max-active: 20
            max-wait: 60000
            validation-query: SELECT 1
            test-while-idle: true
            filters: stat,wall
        
        # 审计数据源
        audit:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://your-audit-host:3306/audit_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false
          username: your_username
          password: your_password
          type: com.alibaba.druid.pool.DruidDataSource
          pool:
            initial-size: 3
            min-idle: 5
            max-active: 10
            max-wait: 30000
            validation-query: SELECT 1
            test-while-idle: true
            filters: stat,wall
        
        # 可以继续添加更多数据源...
        user_db:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://your-user-host:3306/user_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false
          username: your_username
          password: your_password
          type: com.alibaba.druid.pool.DruidDataSource
          pool:
            initial-size: 5
            min-idle: 10
            max-active: 20
            max-wait: 60000
            validation-query: SELECT 1
            test-while-idle: true
            filters: stat,wall
```

### 3. 使用数据源切换

在您的服务类中使用 `@DataSource` 注解：

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 使用主数据源
     */
    @DataSource("master")
    public void createUser(User user) {
        userMapper.insert(user);
    }
    
    /**
     * 使用从数据源
     */
    @DataSource("slave")
    public List<User> getAllUsers() {
        return userMapper.selectList(null);
    }
    
    /**
     * 使用审计数据源
     */
    @DataSource("audit")
    public void logUserAction(String action, String userId) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUserId(userId);
        log.setTimestamp(new Date());
        auditLogMapper.insert(log);
    }
    
    /**
     * 使用用户数据库
     */
    @DataSource("user_db")
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
```

### 4. 类级别的数据源注解

您也可以在类级别设置数据源，整个类的所有方法都使用指定的数据源：

```java
@Service
@DataSource("slave")
public class ReadOnlyService {
    
    @Autowired
    private DataMapper dataMapper;
    
    public List<Data> getAllData() {
        // 自动使用 slave 数据源
        return dataMapper.selectList(null);
    }
    
    public Data getDataById(Long id) {
        // 自动使用 slave 数据源
        return dataMapper.selectById(id);
    }
}
```

### 5. 方法级别的优先级

方法级别的 `@DataSource` 注解优先级高于类级别：

```java
@Service
@DataSource("master")
public class MixedService {
    
    public void defaultOperation() {
        // 使用 master 数据源（类级别）
    }
    
    @DataSource("slave")
    public void readOperation() {
        // 使用 slave 数据源（方法级别优先级更高）
    }
    
    @DataSource("audit")
    public void auditOperation() {
        // 使用 audit 数据源（方法级别优先级更高）
    }
}
```

## 配置说明

### 核心配置项

| 配置项 | 说明 | 默认值 | 是否必填 |
|--------|------|--------|----------|
| `spring.datasource.dynamic.enabled` | 是否启用动态数据源 | `true` | 否 |
| `spring.datasource.dynamic.primary` | 主数据源名称 | `master` | 否 |
| `spring.datasource.dynamic.strict` | 是否严格模式 | `false` | 否 |
| `spring.datasource.dynamic.seata` | 是否启用Seata | `true` | 否 |

### 数据源配置项

每个数据源支持以下配置：

| 配置项 | 说明 | 默认值 | 是否必填 |
|--------|------|--------|----------|
| `driver-class-name` | 数据库驱动类名 | `com.mysql.cj.jdbc.Driver` | 否 |
| `url` | 数据库连接URL | - | 是 |
| `username` | 数据库用户名 | - | 是 |
| `password` | 数据库密码 | - | 是 |
| `type` | 数据源类型 | `com.alibaba.druid.pool.DruidDataSource` | 否 |

### 连接池配置项

每个数据源下的 `pool` 配置：

| 配置项 | 说明 | 默认值 | 是否必填 |
|--------|------|--------|----------|
| `initial-size` | 初始连接数 | `5` | 否 |
| `min-idle` | 最小空闲连接数 | `10` | 否 |
| `max-active` | 最大活跃连接数 | `20` | 否 |
| `max-wait` | 获取连接等待超时时间（毫秒） | `60000` | 否 |
| `validation-query` | 验证查询SQL | `SELECT 1` | 否 |
| `test-while-idle` | 空闲时是否验证 | `true` | 否 |
| `test-on-borrow` | 借用连接时是否验证 | `false` | 否 |
| `test-on-return` | 归还连接时是否验证 | `false` | 否 |
| `filters` | 过滤器 | `stat,wall` | 否 |

## 使用场景示例

### 1. 读写分离

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    // 写操作使用主数据源
    @DataSource("master")
    public void createOrder(Order order) {
        orderMapper.insert(order);
    }
    
    // 读操作使用从数据源
    @DataSource("slave")
    public List<Order> getOrders() {
        return orderMapper.selectList(null);
    }
    
    // 读操作使用从数据源
    @DataSource("slave")
    public Order getOrderById(Long id) {
        return orderMapper.selectById(id);
    }
}
```

### 2. 多租户架构

```java
@Service
public class MultiTenantService {
    
    @Autowired
    private UserMapper userMapper;
    
    @DataSource("tenant_1_db")
    public List<User> getTenant1Users() {
        return userMapper.selectList(null);
    }
    
    @DataSource("tenant_2_db")
    public List<User> getTenant2Users() {
        return userMapper.selectList(null);
    }
    
    @DataSource("tenant_3_db")
    public List<User> getTenant3Users() {
        return userMapper.selectList(null);
    }
}
```

### 3. 业务分离

```java
@Service
public class BusinessService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private AuditLogMapper auditLogMapper;
    
    // 用户相关操作
    @DataSource("user_db")
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    // 订单相关操作
    @DataSource("order_db")
    public Order getOrderById(Long id) {
        return orderMapper.selectById(id);
    }
    
    // 审计日志
    @DataSource("audit")
    public void logOperation(String operation, String userId) {
        AuditLog log = new AuditLog();
        log.setOperation(operation);
        log.setUserId(userId);
        log.setTimestamp(new Date());
        auditLogMapper.insert(log);
    }
}
```

### 4. 数据分析场景

```java
@Service
public class DataAnalysisService {
    
    @Autowired
    private AnalysisMapper analysisMapper;
    
    // 实时数据
    @DataSource("realtime_db")
    public List<RealTimeData> getRealTimeData() {
        return analysisMapper.getRealTimeData();
    }
    
    // 历史数据
    @DataSource("history_db")
    public List<HistoryData> getHistoryData(Date startDate, Date endDate) {
        return analysisMapper.getHistoryData(startDate, endDate);
    }
    
    // 统计数据
    @DataSource("stats_db")
    public List<StatisticsData> getStatisticsData() {
        return analysisMapper.getStatisticsData();
    }
}
```

## 注意事项

### 1. 数据源名称一致性

`@DataSource` 注解中的值必须与配置文件中的数据源名称完全一致：

```java
// 正确：注解值与配置名称一致
@DataSource("master")
public void method() {}

// 错误：注解值与配置名称不一致
@DataSource("main")  // 配置中没有 "main" 数据源
public void method() {}
```

### 2. 事务管理

在同一个事务中切换数据源可能会导致问题，建议在事务边界外进行数据源切换：

```java
@Service
public class TransactionService {
    
    @Transactional
    public void processWithTransaction() {
        // 在事务中不要切换数据源
        processData();
    }
    
    // 在事务外切换数据源
    @DataSource("slave")
    public void processData() {
        // 这里的数据源切换是安全的
    }
}
```

### 3. 严格模式

启用严格模式后，任何数据源创建失败都会导致应用启动失败：

```yaml
spring:
  datasource:
    dynamic:
      strict: true  # 严格模式
```

### 4. Seata集成

启用Seata后，数据源会自动包装为 `DataSourceProxy`，支持分布式事务：

```yaml
spring:
  datasource:
    dynamic:
      seata: true  # 启用Seata
```

## 故障排除

### 1. 数据源创建失败

**错误信息**：`创建数据源失败: 数据库URL为空，请检查配置`

**解决方案**：
- 检查配置文件中的数据源配置是否正确
- 检查数据库连接信息（URL、用户名、密码）
- 检查数据库是否可访问

### 2. 数据源切换不生效

**错误信息**：数据源切换没有效果

**解决方案**：
- 检查 `@DataSource` 注解值是否与配置一致
- 检查是否启用了动态数据源功能（`spring.datasource.dynamic.enabled=true`）
- 检查AOP是否正常工作

### 3. Seata相关问题

**错误信息**：Seata相关错误

**解决方案**：
- 检查Seata配置是否正确
- 检查数据源代理是否正常创建
- 检查分布式事务配置

### 4. 日志配置

启用调试日志以排查问题：

```yaml
logging:
  level:
    com.jiangyang.base: DEBUG
    org.springframework.jdbc: DEBUG
    com.alibaba.druid: DEBUG
    io.seata: DEBUG
```

## 最佳实践

### 1. 数据源命名规范

建议使用有意义的名称来命名数据源：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        # 业务数据源
        business_master: {...}
        business_slave: {...}
        
        # 用户数据源
        user_db: {...}
        
        # 订单数据源
        order_db: {...}
        
        # 审计数据源
        audit_log: {...}
        
        # 报表数据源
        report_db: {...}
```

### 2. 连接池配置优化

根据业务需求优化连接池配置：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          pool:
            initial-size: 10      # 高并发场景增加初始连接数
            min-idle: 20          # 保持足够的空闲连接
            max-active: 100       # 根据并发量调整最大连接数
            max-wait: 30000       # 减少等待时间
            time-between-eviction-runs-millis: 30000  # 更频繁的检测
            min-evictable-idle-time-millis: 180000    # 减少空闲时间
```

### 3. 监控和统计

启用Druid的监控功能：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          pool:
            filters: stat,wall,log4j2
            connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
```

### 4. 错误处理

在严格模式下，确保所有数据源都能正常创建：

```yaml
spring:
  datasource:
    dynamic:
      strict: true  # 严格模式，确保数据源质量
```

## 总结

`base-service` 提供了完整的动态多数据源解决方案，其他服务只需要：

1. 引入依赖
2. 配置数据源
3. 使用 `@DataSource` 注解

即可实现灵活的数据源切换，无需关心底层实现细节。这种设计大大简化了多数据源的配置和使用，提高了开发效率。
