package com.jiangyang.base.config;

import com.jiangyang.base.datasource.DynamicDataSource;
import com.jiangyang.base.datasource.DynamicDataSourceManager;
import com.jiangyang.base.datasource.properties.DataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置
 * 支持动态数量的数据源配置
 * 集成DynamicDataSourceManager，支持在AbstractRoutingDataSource不可用时自动切换到编程式注册
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.dynamic", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSourceConfig {

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    /**
     * 动态数据源 - 这是应用中使用的主要数据源
     */
    @Primary
    @Bean(name = "dynamicDataSource")
    public DataSource dynamicDataSource() {
        try {
            DynamicDataSource dynamicDataSource = new DynamicDataSource();
            Map<Object, Object> targetDataSources = new HashMap<>();
            
            // 获取配置中的所有数据源
            Map<String, DataSourceProperties.DataSourceConfig> datasourceConfigs = 
                dataSourceProperties.getDynamic().getDatasource();
            
            if (datasourceConfigs.isEmpty()) {
                throw new IllegalStateException("未配置任何数据源，请检查配置文件");
            }
            
            // 创建所有数据源
            for (Map.Entry<String, DataSourceProperties.DataSourceConfig> entry : datasourceConfigs.entrySet()) {
                String dataSourceName = entry.getKey();
                DataSourceProperties.DataSourceConfig config = entry.getValue();
                
                try {
                    DataSource dataSource = createDataSource(config);
                    targetDataSources.put(dataSourceName, dataSource);
                    log.info("成功创建数据源: {}", dataSourceName);
                } catch (Exception e) {
                    log.error("创建数据源 {} 失败: {}", dataSourceName, e.getMessage());
                    if (dataSourceProperties.getDynamic().isStrict()) {
                        throw new RuntimeException("严格模式下数据源创建失败", e);
                    }
                }
            }
            
            // 设置目标数据源
            dynamicDataSource.setTargetDataSources(targetDataSources);
            
            // 设置默认数据源
            String primaryDataSourceName = dataSourceProperties.getDynamic().getPrimary();
            if (!targetDataSources.containsKey(primaryDataSourceName)) {
                throw new IllegalStateException("主数据源 '" + primaryDataSourceName + "' 不存在");
            }
            
            Object defaultDataSource = targetDataSources.get(primaryDataSourceName);
            dynamicDataSource.setDefaultTargetDataSource(defaultDataSource);
            
            // 初始化
            try {
                dynamicDataSource.afterPropertiesSet();
            } catch (Exception e) {
                throw new RuntimeException("动态数据源初始化失败", e);
            }
            
            log.info("动态数据源初始化完成，共配置 {} 个数据源，主数据源: {}", 
                    targetDataSources.size(), primaryDataSourceName);
            
            return dynamicDataSource;
            
        } catch (Exception e) {
            log.error("AbstractRoutingDataSource模式初始化失败，将使用编程式注册模式: {}", e.getMessage());
            
            // 如果AbstractRoutingDataSource模式失败，初始化编程式注册模式
            try {
                dynamicDataSourceManager.initializeDataSources();
                
                // 返回一个代理数据源，委托给DynamicDataSourceManager
                return new ProxyDataSource(dynamicDataSourceManager);
                
            } catch (Exception ex) {
                log.error("编程式注册模式也失败: {}", ex.getMessage(), ex);
                throw new RuntimeException("所有数据源模式都失败", ex);
            }
        }
    }

    /**
     * 代理数据源
     * 当AbstractRoutingDataSource不可用时，委托给DynamicDataSourceManager
     */
    private static class ProxyDataSource implements DataSource {
        
        private final DynamicDataSourceManager manager;
        
        public ProxyDataSource(DynamicDataSourceManager manager) {
            this.manager = manager;
        }
        
        @Override
        public java.sql.Connection getConnection() throws java.sql.SQLException {
            // 获取主数据源
            String primaryDataSourceName = manager.getApplicationContext()
                .getBean(DataSourceProperties.class)
                .getDynamic()
                .getPrimary();
            
            DataSource primaryDataSource = manager.getDataSource(primaryDataSourceName);
            return primaryDataSource.getConnection();
        }
        
        @Override
        public java.sql.Connection getConnection(String username, String password) throws java.sql.SQLException {
            // 获取主数据源
            String primaryDataSourceName = manager.getApplicationContext()
                .getBean(DataSourceProperties.class)
                .getDynamic()
                .getPrimary();
            
            DataSource primaryDataSource = manager.getDataSource(primaryDataSourceName);
            return primaryDataSource.getConnection(username, password);
        }
        
        // 其他DataSource接口方法的默认实现
        @Override
        public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
            throw new java.sql.SQLException("ProxyDataSource does not support unwrapping");
        }
        
        @Override
        public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
            return false;
        }
        
        @Override
        public java.io.PrintWriter getLogWriter() throws java.sql.SQLException {
            return null;
        }
        
        @Override
        public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException {
        }
        
        @Override
        public void setLoginTimeout(int seconds) throws java.sql.SQLException {
        }
        
        @Override
        public int getLoginTimeout() throws java.sql.SQLException {
            return 0;
        }
        
        @Override
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
            throw new java.sql.SQLFeatureNotSupportedException("ProxyDataSource does not support getParentLogger");
        }
    }

    /**
     * 创建单个数据源
     */
    private DataSource createDataSource(DataSourceProperties.DataSourceConfig config) {
        // 验证配置
        validateDataSourceConfig(config);
        
        try {
            // 创建Druid数据源
            com.alibaba.druid.pool.DruidDataSource druidDataSource = new com.alibaba.druid.pool.DruidDataSource();
            
            // 设置基本属性
            druidDataSource.setDriverClassName(config.getDriverClassName());
            druidDataSource.setUrl(config.getUrl());
            druidDataSource.setUsername(config.getUsername());
            druidDataSource.setPassword(config.getPassword());
            
            // 设置连接池属性
            DataSourceProperties.DataSourceConfig.Pool pool = config.getPool();
            druidDataSource.setInitialSize(pool.getInitialSize());
            druidDataSource.setMinIdle(pool.getMinIdle());
            druidDataSource.setMaxActive(pool.getMaxActive());
            druidDataSource.setMaxWait(pool.getMaxWait());
            druidDataSource.setTimeBetweenEvictionRunsMillis(pool.getTimeBetweenEvictionRunsMillis());
            druidDataSource.setMinEvictableIdleTimeMillis(pool.getMinEvictableIdleTimeMillis());
            druidDataSource.setValidationQuery(pool.getValidationQuery());
            druidDataSource.setTestWhileIdle(pool.isTestWhileIdle());
            druidDataSource.setTestOnBorrow(pool.isTestOnBorrow());
            druidDataSource.setTestOnReturn(pool.isTestOnReturn());
            druidDataSource.setPoolPreparedStatements(pool.isPoolPreparedStatements());
            druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(pool.getMaxPoolPreparedStatementPerConnectionSize());
            druidDataSource.setFilters(pool.getFilters());
            druidDataSource.setConnectionProperties(pool.getConnectionProperties());
            
            // 初始化数据源
            druidDataSource.init();
            
            // 检查Seata是否真正启用
            String seataEnabled = environment.getProperty("seata.enabled", "true");
            if ("false".equals(seataEnabled) || !dataSourceProperties.getDynamic().isSeata()) {
                log.info("Seata已禁用，使用原始数据源");
                return druidDataSource;
            }
            
            // 如果启用Seata，则包装为DataSourceProxy
            try {
                // 检查Seata类是否可用
                try {
                    Class.forName("io.seata.rm.datasource.DataSourceProxy");
                } catch (ClassNotFoundException e) {
                    log.warn("Seata类不可用，使用原始数据源");
                    return druidDataSource;
                }
                
                // 尝试创建DataSourceProxy，如果Seata类不可用则使用原始数据源
                return new io.seata.rm.datasource.DataSourceProxy(druidDataSource);
            } catch (NoClassDefFoundError | Exception e) {
                log.warn("Seata DataSourceProxy不可用，使用原始数据源: {}", e.getMessage());
                return druidDataSource;
            }

        } catch (Exception e) {
            throw new RuntimeException("创建数据源失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证数据源配置
     */
    private void validateDataSourceConfig(DataSourceProperties.DataSourceConfig config) {
        if (config.getDriverClassName() == null) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            log.warn("数据源驱动类名为空，使用默认的MySQL驱动");
        }
        
        if (config.getUrl() == null) {
            throw new RuntimeException("数据库URL为空，请检查配置");
        }
        
        if (config.getUsername() == null) {
            throw new RuntimeException("数据库用户名为空，请检查配置");
        }
        
        if (config.getPassword() == null) {
            throw new RuntimeException("数据库密码为空，请检查配置");
        }
        
        // 验证驱动类
        try {
            Class.forName(config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("找不到数据库驱动类: " + config.getDriverClassName(), e);
        }
        
        // 验证数据库连接
        try {
            DriverManager.getDriver(config.getUrl());
        } catch (SQLException e) {
            throw new RuntimeException("数据库URL无效: " + config.getUrl(), e);
        }
    }
}
