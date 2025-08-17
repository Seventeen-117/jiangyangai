# 动态数据源使用文档

## 概述

`base-service` 提供了两种动态数据源实现方式：

1. **AbstractRoutingDataSource模式**：基于Spring的`AbstractRoutingDataSource`，通过ThreadLocal实现数据源切换
2. **编程式注册模式**：通过`BeanDefinitionRegistry`动态注册DataSource Bean到Spring容器

系统会自动选择可用的模式，如果AbstractRoutingDataSource不可用，会自动切换到编程式注册模式。

## 核心组件

### 1. DynamicDataSourceRegistry
动态数据源注册服务，负责运行时动态注册DataSource Bean。

**主要方法：**
- `registerDataSource(String dataSourceName, DataSourceConfig config)`：注册单个数据源
- `registerDataSources(Map<String, DataSourceConfig> configs)`：批量注册数据源
- `removeDataSource(String dataSourceName)`：移除数据源
- `isDataSourceRegistered(String dataSourceName)`：检查数据源是否已注册

### 2. DynamicDataSourceManager
动态数据源管理器，提供统一的数据源访问接口。

**主要方法：**
- `getDataSource(String dataSourceName)`：获取数据源实例
- `initializeDataSources()`：批量初始化数据源
- `refreshDataSource(String dataSourceName)`：刷新数据源
- `isUsingProgrammaticRegistration()`：检查当前使用的模式

### 3. DynamicDataSource
继承`AbstractRoutingDataSource`的动态数据源实现。

## 配置示例

### 1. 配置文件 (application.yml)
```yaml
spring:
  datasource:
    dynamic:
      enabled: true
      primary: master
      strict: false
      seata: true
      datasource:
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/master_db
          username: root
          password: password
          type: com.alibaba.druid.pool.DruidDataSource
          pool:
            initial-size: 5
            min-idle: 5
            max-active: 20
            max-wait: 60000
            validation-query: SELECT 1
        slave:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/slave_db
          username: root
          password: password
          type: com.alibaba.druid.pool.DruidDataSource
          pool:
            initial-size: 3
            min-idle: 3
            max-active: 10
            max-wait: 60000
            validation-query: SELECT 1
```

### 2. 使用@DataSource注解
```java
@Service
@DataSource("master")
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public List<User> getUsers() {
        return userMapper.selectList(null);
    }
}

@Service
@DataSource("slave")
public class ReadOnlyUserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public List<User> getUsers() {
        return userMapper.selectList(null);
    }
}
```

### 3. 编程式使用
```java
@Service
public class DynamicUserService {
    
    @Autowired
    private DynamicDataSourceManager dataSourceManager;
    
    public void switchDataSource() {
        // 获取指定数据源
        DataSource masterDataSource = dataSourceManager.getDataSource("master");
        DataSource slaveDataSource = dataSourceManager.getDataSource("slave");
        
        // 检查当前模式
        boolean isProgrammatic = dataSourceManager.isUsingProgrammaticRegistration();
        System.out.println("当前使用编程式注册模式: " + isProgrammatic);
        
        // 刷新数据源
        dataSourceManager.refreshDataSource("master");
        
        // 获取所有已注册的数据源
        Map<String, String> registeredDataSources = dataSourceManager.getAllRegisteredDataSources();
        System.out.println("已注册的数据源: " + registeredDataSources);
    }
}
```

## 运行时动态注册数据源

### 1. 动态注册新数据源
```java
@Service
public class DataSourceManagementService {
    
    @Autowired
    private DynamicDataSourceRegistry dataSourceRegistry;
    
    public void addNewDataSource() {
        // 创建数据源配置
        DataSourceProperties.DataSourceConfig config = new DataSourceProperties.DataSourceConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUrl("jdbc:mysql://localhost:3306/new_db");
        config.setUsername("root");
        config.setPassword("password");
        
        // 配置连接池
        DataSourceProperties.DataSourceConfig.Pool pool = new DataSourceProperties.DataSourceConfig.Pool();
        pool.setInitialSize(5);
        pool.setMinIdle(5);
        pool.setMaxActive(20);
        pool.setMaxWait(60000);
        pool.setValidationQuery("SELECT 1");
        config.setPool(pool);
        
        // 注册数据源
        String beanName = dataSourceRegistry.registerDataSource("newDataSource", config);
        System.out.println("新数据源注册成功，Bean名称: " + beanName);
    }
    
    public void removeDataSource(String dataSourceName) {
        boolean success = dataSourceRegistry.removeDataSource(dataSourceName);
        System.out.println("移除数据源 " + dataSourceName + ": " + success);
    }
}
```

### 2. 批量注册数据源
```java
@Service
public class BatchDataSourceService {
    
    @Autowired
    private DynamicDataSourceRegistry dataSourceRegistry;
    
    public void registerMultipleDataSources() {
        Map<String, DataSourceProperties.DataSourceConfig> configs = new HashMap<>();
        
        // 配置多个数据源
        for (int i = 1; i <= 3; i++) {
            DataSourceProperties.DataSourceConfig config = new DataSourceProperties.DataSourceConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setUrl("jdbc:mysql://localhost:3306/db" + i);
            config.setUsername("root");
            config.setPassword("password");
            
            DataSourceProperties.DataSourceConfig.Pool pool = new DataSourceProperties.DataSourceConfig.Pool();
            pool.setInitialSize(3);
            pool.setMinIdle(3);
            pool.setMaxActive(10);
            pool.setMaxWait(60000);
            pool.setValidationQuery("SELECT 1");
            config.setPool(pool);
            
            configs.put("dynamicDataSource" + i, config);
        }
        
        // 批量注册
        Map<String, String> registeredBeans = dataSourceRegistry.registerDataSources(configs);
        System.out.println("批量注册结果: " + registeredBeans);
    }
}
```

## 故障处理

### 1. AbstractRoutingDataSource模式失败
当AbstractRoutingDataSource模式初始化失败时，系统会自动切换到编程式注册模式：

```java
// 检查当前使用的模式
@Autowired
private DynamicDataSourceManager dataSourceManager;

public void checkMode() {
    if (dataSourceManager.isUsingProgrammaticRegistration()) {
        System.out.println("当前使用编程式注册模式");
    } else {
        System.out.println("当前使用AbstractRoutingDataSource模式");
    }
}
```

### 2. 数据源连接失败
```java
@Service
public class DataSourceHealthCheckService {
    
    @Autowired
    private DynamicDataSourceManager dataSourceManager;
    
    public void checkDataSourceHealth(String dataSourceName) {
        try {
            DataSource dataSource = dataSourceManager.getDataSource(dataSourceName);
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("数据源 " + dataSourceName + " 连接正常");
            }
        } catch (Exception e) {
            System.err.println("数据源 " + dataSourceName + " 连接失败: " + e.getMessage());
            // 刷新数据源缓存
            dataSourceManager.refreshDataSource(dataSourceName);
        }
    }
}
```

## 性能优化

### 1. 数据源缓存
DynamicDataSourceManager会自动缓存已获取的数据源实例，避免重复创建：

```java
// 获取缓存的数据源数量
int cachedCount = dataSourceManager.getCachedDataSourceCount();
System.out.println("当前缓存的数据源数量: " + cachedCount);

// 清除所有缓存
dataSourceManager.refreshAllDataSources();
```

### 2. 连接池配置
建议根据实际负载调整连接池参数：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          pool:
            initial-size: 10      # 初始连接数
            min-idle: 10          # 最小空闲连接数
            max-active: 50        # 最大活跃连接数
            max-wait: 30000       # 最大等待时间
            time-between-eviction-runs-millis: 60000  # 空闲连接检测间隔
            min-evictable-idle-time-millis: 300000    # 最小空闲时间
```

## 注意事项

1. **线程安全**：DynamicDataSourceRegistry和DynamicDataSourceManager都是线程安全的
2. **Bean生命周期**：动态注册的Bean会在应用关闭时自动销毁
3. **配置验证**：系统会自动验证数据源配置的有效性
4. **Seata集成**：支持Seata分布式事务，会自动包装为DataSourceProxy
5. **严格模式**：在严格模式下，任何数据源创建失败都会导致应用启动失败

## 监控和日志

系统提供了详细的日志记录，可以通过以下方式监控：

```yaml
logging:
  level:
    com.jiangyang.base.datasource: DEBUG
    com.alibaba.druid: INFO
```

关键日志包括：
- 数据源创建和注册
- 模式切换
- 连接池状态
- 错误和异常信息
