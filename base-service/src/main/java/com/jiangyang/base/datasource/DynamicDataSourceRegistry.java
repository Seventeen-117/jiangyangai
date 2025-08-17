package com.jiangyang.base.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.jiangyang.base.datasource.properties.DataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源注册服务
 * 支持运行时动态注册DataSource Bean到Spring容器
 * 作为AbstractRoutingDataSource的备用方案
 */
@Slf4j
@Component
public class DynamicDataSourceRegistry implements ApplicationContextAware {

    @Autowired
    private DataSourceProperties dataSourceProperties;

    private ConfigurableApplicationContext applicationContext;
    
    // 缓存已注册的数据源Bean名称
    private final Map<String, String> registeredDataSources = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    /**
     * 动态注册数据源Bean
     * 
     * @param dataSourceName 数据源名称
     * @param config 数据源配置
     * @return 注册的Bean名称
     */
    public String registerDataSource(String dataSourceName, DataSourceProperties.DataSourceConfig config) {
        try {
            // 检查是否已经注册
            if (registeredDataSources.containsKey(dataSourceName)) {
                log.warn("数据源 {} 已经注册，Bean名称: {}", dataSourceName, registeredDataSources.get(dataSourceName));
                return registeredDataSources.get(dataSourceName);
            }

            // 生成Bean名称
            String beanName = "dynamicDataSource_" + dataSourceName;
            
            // 创建数据源
            DataSource dataSource = createDataSource(config);
            
            // 注册Bean到Spring容器
            applicationContext.getBeanFactory().registerSingleton(beanName, dataSource);
            
            // 缓存注册信息
            registeredDataSources.put(dataSourceName, beanName);
            
            log.info("成功动态注册数据源Bean: {} -> {}", dataSourceName, beanName);
            
            return beanName;
            
        } catch (Exception e) {
            log.error("动态注册数据源失败: {}, error: {}", dataSourceName, e.getMessage(), e);
            throw new RuntimeException("动态注册数据源失败: " + dataSourceName, e);
        }
    }

    /**
     * 批量注册数据源
     * 
     * @param dataSourceConfigs 数据源配置映射
     * @return 注册的Bean名称映射
     */
    public Map<String, String> registerDataSources(Map<String, DataSourceProperties.DataSourceConfig> dataSourceConfigs) {
        Map<String, String> registeredBeans = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, DataSourceProperties.DataSourceConfig> entry : dataSourceConfigs.entrySet()) {
            String dataSourceName = entry.getKey();
            DataSourceProperties.DataSourceConfig config = entry.getValue();
            
            try {
                String beanName = registerDataSource(dataSourceName, config);
                registeredBeans.put(dataSourceName, beanName);
            } catch (Exception e) {
                log.error("批量注册数据源时失败: {}, error: {}", dataSourceName, e.getMessage());
                if (dataSourceProperties.getDynamic().isStrict()) {
                    throw new RuntimeException("严格模式下数据源注册失败", e);
                }
            }
        }
        
        return registeredBeans;
    }

    /**
     * 移除数据源Bean
     * 
     * @param dataSourceName 数据源名称
     * @return 是否成功移除
     */
    public boolean removeDataSource(String dataSourceName) {
        try {
            String beanName = registeredDataSources.get(dataSourceName);
            if (beanName == null) {
                log.warn("数据源 {} 未注册，无法移除", dataSourceName);
                return false;
            }

            // 从Spring容器中移除Bean
            if (applicationContext.getBeanFactory().containsBean(beanName)) {
                applicationContext.getBeanFactory().destroyBean(beanName);
                registeredDataSources.remove(dataSourceName);
                log.info("成功移除数据源Bean: {} -> {}", dataSourceName, beanName);
                return true;
            } else {
                log.warn("Bean {} 不存在于Spring容器中", beanName);
                return false;
            }
            
        } catch (Exception e) {
            log.error("移除数据源失败: {}, error: {}", dataSourceName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取已注册的数据源Bean名称
     * 
     * @param dataSourceName 数据源名称
     * @return Bean名称，如果未注册则返回null
     */
    public String getRegisteredBeanName(String dataSourceName) {
        return registeredDataSources.get(dataSourceName);
    }

    /**
     * 获取所有已注册的数据源
     * 
     * @return 数据源名称到Bean名称的映射
     */
    public Map<String, String> getAllRegisteredDataSources() {
        return new ConcurrentHashMap<>(registeredDataSources);
    }

    /**
     * 检查数据源是否已注册
     * 
     * @param dataSourceName 数据源名称
     * @return 是否已注册
     */
    public boolean isDataSourceRegistered(String dataSourceName) {
        return registeredDataSources.containsKey(dataSourceName);
    }

    /**
     * 创建数据源实例
     * 
     * @param config 数据源配置
     * @return 数据源实例
     */
    private DataSource createDataSource(DataSourceProperties.DataSourceConfig config) {
        // 验证配置
        validateDataSourceConfig(config);
        
        try {
            // 创建Druid数据源
            DruidDataSource druidDataSource = new DruidDataSource();
            
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
            
            // 检查Seata是否启用
            if (!dataSourceProperties.getDynamic().isSeata()) {
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
                
                // 尝试创建DataSourceProxy
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
     * 
     * @param config 数据源配置
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
    }
}
