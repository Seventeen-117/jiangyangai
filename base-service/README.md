# Base Service - 通用多数据源服务

## 概述

`base-service` 是一个通用的多数据源切换服务，基于 `bgai-service` 的多数据源实现进行改造，支持动态数量的数据源配置和切换。

## 特性

- ✅ 支持任意数量的数据源（不限于固定的 master/slave）
- ✅ 自动配置，引入依赖即可使用
- ✅ 支持 Seata 分布式事务
- ✅ 基于 Druid 连接池
- ✅ 支持方法级和类级数据源切换
- ✅ 支持严格模式和宽松模式
- ✅ 完整的连接池配置支持

## 快速开始

### 1. 引入依赖

在您的项目中引入 `base-service` 依赖：

```xml
<dependency>
    <groupId>com.jiangyang</groupId>
    <artifactId>base-service</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置数据源

在您的配置文件中配置数据源：

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
          url: jdbc:mysql://localhost:3306/master_db
          username: root
          password: password
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
          url: jdbc:mysql://localhost:3306/slave_db
          username: root
          password: password
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
          url: jdbc:mysql://localhost:3306/audit_db
          username: root
          password: password
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
          url: jdbc:mysql://localhost:3306/user_db
          username: root
          password: password
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
    public List<User> getUsersFromMaster() {
        return userMapper.selectList(null);
    }
    
    /**
     * 使用从数据源
     */
    @DataSource("slave")
    public List<User> getUsersFromSlave() {
        return userMapper.selectList(null);
    }
    
    /**
     * 使用审计数据源
     */
    @DataSource("audit")
    public void logUserAction(String action) {
        // 记录用户操作到审计数据库
    }
    
    /**
     * 使用用户数据库
     */
    @DataSource("user_db")
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}

/**
 * 类级别的数据源注解
 */
@Service
@DataSource("slave")
public class SlaveDataService {
    
    public List<Data> getData() {
        // 整个类的所有方法都使用 slave 数据源
        return dataMapper.selectList(null);
    }
}
```

## 配置说明

### 核心配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.datasource.dynamic.enabled` | 是否启用动态数据源 | `true` |
| `spring.datasource.dynamic.primary` | 主数据源名称 | `master` |
| `spring.datasource.dynamic.strict` | 是否严格模式 | `false` |
| `spring.datasource.dynamic.seata` | 是否启用Seata | `true` |

### 数据源配置

每个数据源支持以下配置：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `driver-class-name` | 数据库驱动类名 | `com.mysql.cj.jdbc.Driver` |
| `url` | 数据库连接URL | 必填 |
| `username` | 数据库用户名 | 必填 |
| `password` | 数据库密码 | 必填 |
| `type` | 数据源类型 | `com.alibaba.druid.pool.DruidDataSource` |

### 连接池配置

每个数据源下的 `pool` 配置：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `initial-size` | 初始连接数 | `5` |
| `min-idle` | 最小空闲连接数 | `10` |
| `max-active` | 最大活跃连接数 | `20` |
| `max-wait` | 获取连接等待超时时间 | `60000` |
| `validation-query` | 验证查询SQL | `SELECT 1` |
| `test-while-idle` | 空闲时是否验证 | `true` |
| `filters` | 过滤器 | `stat,wall` |

## 使用场景

### 1. 读写分离

```java
@Service
public class OrderService {
    
    @DataSource("master")
    public void createOrder(Order order) {
        // 写操作使用主数据源
        orderMapper.insert(order);
    }
    
    @DataSource("slave")
    public List<Order> getOrders() {
        // 读操作使用从数据源
        return orderMapper.selectList(null);
    }
}
```

### 2. 多租户

```java
@Service
public class MultiTenantService {
    
    @DataSource("tenant_1_db")
    public void processTenant1() {
        // 处理租户1的数据
    }
    
    @DataSource("tenant_2_db")
    public void processTenant2() {
        // 处理租户2的数据
    }
}
```

### 3. 业务分离

```java
@Service
public class BusinessService {
    
    @DataSource("user_db")
    public User getUser(Long id) {
        // 用户相关操作
        return userMapper.selectById(id);
    }
    
    @DataSource("order_db")
    public Order getOrder(Long id) {
        // 订单相关操作
        return orderMapper.selectById(id);
    }
    
    @DataSource("audit")
    public void logOperation(String operation) {
        // 审计日志
        auditMapper.insert(new AuditLog(operation));
    }
}
```

## 注意事项

1. **数据源名称**：`@DataSource` 注解中的值必须与配置文件中的数据源名称完全一致
2. **事务管理**：在同一个事务中切换数据源可能会导致问题，建议在事务边界外进行数据源切换
3. **严格模式**：启用严格模式后，任何数据源创建失败都会导致应用启动失败
4. **Seata集成**：启用Seata后，数据源会自动包装为 `DataSourceProxy`

## 故障排除

### 常见问题

1. **数据源创建失败**
   - 检查数据库连接信息是否正确
   - 检查数据库是否可访问
   - 检查驱动类是否存在

2. **数据源切换不生效**
   - 检查 `@DataSource` 注解值是否与配置一致
   - 检查是否启用了动态数据源功能
   - 检查AOP是否正常工作

3. **Seata相关问题**
   - 检查Seata配置是否正确
   - 检查数据源代理是否正常创建

### 日志配置

启用调试日志以排查问题：

```yaml
logging:
  level:
    com.jiangyang.base: DEBUG
    org.springframework.jdbc: DEBUG
    com.alibaba.druid: DEBUG
```

## 示例项目

完整的使用示例请参考 `ExampleService` 类。

## 版本历史

- `1.0.0` - 初始版本，支持动态多数据源切换
- 基于 `bgai-service` 的多数据源实现进行改造
- 支持任意数量的数据源配置
