package com.jiangyang.base.datasource;

import com.jiangyang.base.datasource.properties.DataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源管理器
 * 支持在AbstractRoutingDataSource不可用时自动切换到编程式注册
 */
@Slf4j
@Component
public class DynamicDataSourceManager {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Autowired
    private DynamicDataSourceRegistry dynamicDataSourceRegistry;

    // 缓存已获取的数据源实例
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    
    // 标记是否使用编程式注册模式
    private volatile boolean useProgrammaticRegistration = false;

    /**
     * 获取数据源
     * 优先使用AbstractRoutingDataSource，如果不可用则使用编程式注册
     * 
     * @param dataSourceName 数据源名称
     * @return 数据源实例
     */
    public DataSource getDataSource(String dataSourceName) {
        try {
            // 首先尝试从缓存获取
            if (dataSourceCache.containsKey(dataSourceName)) {
                return dataSourceCache.get(dataSourceName);
            }

            // 尝试使用AbstractRoutingDataSource模式
            if (!useProgrammaticRegistration) {
                try {
                    DataSource dataSource = getDataSourceFromRouting(dataSourceName);
                    if (dataSource != null) {
                        dataSourceCache.put(dataSourceName, dataSource);
                        return dataSource;
                    }
                } catch (Exception e) {
                    log.warn("AbstractRoutingDataSource模式不可用，切换到编程式注册模式: {}", e.getMessage());
                    useProgrammaticRegistration = true;
                }
            }

            // 使用编程式注册模式
            DataSource dataSource = getDataSourceFromProgrammaticRegistration(dataSourceName);
            if (dataSource != null) {
                dataSourceCache.put(dataSourceName, dataSource);
                return dataSource;
            }

            throw new RuntimeException("无法获取数据源: " + dataSourceName);

        } catch (Exception e) {
            log.error("获取数据源失败: {}, error: {}", dataSourceName, e.getMessage(), e);
            throw new RuntimeException("获取数据源失败: " + dataSourceName, e);
        }
    }

    /**
     * 从AbstractRoutingDataSource获取数据源
     * 
     * @param dataSourceName 数据源名称
     * @return 数据源实例
     */
    private DataSource getDataSourceFromRouting(String dataSourceName) {
        try {
            // 尝试获取动态数据源Bean
            if (applicationContext.containsBean("dynamicDataSource")) {
                DataSource dynamicDataSource = applicationContext.getBean("dynamicDataSource", DataSource.class);
                if (dynamicDataSource instanceof DynamicDataSource) {
                    // 设置当前线程的数据源上下文
                    DataSourceContextHolder.setDataSourceKey(dataSourceName);
                    
                    // 获取连接来验证数据源是否可用
                    try (var connection = dynamicDataSource.getConnection()) {
                        log.debug("通过AbstractRoutingDataSource成功获取数据源: {}", dataSourceName);
                        return dynamicDataSource;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("AbstractRoutingDataSource获取数据源失败: {}, error: {}", dataSourceName, e.getMessage());
        }
        return null;
    }

    /**
     * 从编程式注册获取数据源
     * 
     * @param dataSourceName 数据源名称
     * @return 数据源实例
     */
    private DataSource getDataSourceFromProgrammaticRegistration(String dataSourceName) {
        try {
            // 检查是否已经注册
            if (!dynamicDataSourceRegistry.isDataSourceRegistered(dataSourceName)) {
                // 获取配置并注册数据源
                Map<String, DataSourceProperties.DataSourceConfig> dataSourceConfigs = 
                    dataSourceProperties.getDynamic().getDatasource();
                
                DataSourceProperties.DataSourceConfig config = dataSourceConfigs.get(dataSourceName);
                if (config == null) {
                    throw new RuntimeException("未找到数据源配置: " + dataSourceName);
                }

                // 注册数据源
                String beanName = dynamicDataSourceRegistry.registerDataSource(dataSourceName, config);
                log.info("通过编程式注册成功注册数据源: {} -> {}", dataSourceName, beanName);
            }

            // 获取已注册的Bean名称
            String beanName = dynamicDataSourceRegistry.getRegisteredBeanName(dataSourceName);
            if (beanName == null) {
                throw new RuntimeException("数据源未注册: " + dataSourceName);
            }

            // 从Spring容器获取数据源实例
            DataSource dataSource = applicationContext.getBean(beanName, DataSource.class);
            log.debug("通过编程式注册成功获取数据源: {}", dataSourceName);
            return dataSource;

        } catch (Exception e) {
            log.error("编程式注册获取数据源失败: {}, error: {}", dataSourceName, e.getMessage(), e);
            throw new RuntimeException("编程式注册获取数据源失败: " + dataSourceName, e);
        }
    }

    /**
     * 批量初始化数据源
     * 在应用启动时调用，预注册所有配置的数据源
     */
    public void initializeDataSources() {
        try {
            Map<String, DataSourceProperties.DataSourceConfig> dataSourceConfigs = 
                dataSourceProperties.getDynamic().getDatasource();
            
            if (dataSourceConfigs.isEmpty()) {
                log.warn("未配置任何数据源");
                return;
            }

            log.info("开始初始化数据源，共 {} 个", dataSourceConfigs.size());

            // 尝试使用AbstractRoutingDataSource模式
            if (!useProgrammaticRegistration) {
                try {
                    if (applicationContext.containsBean("dynamicDataSource")) {
                        log.info("使用AbstractRoutingDataSource模式初始化数据源");
                        // AbstractRoutingDataSource模式不需要预初始化
                        return;
                    }
                } catch (Exception e) {
                    log.warn("AbstractRoutingDataSource不可用，切换到编程式注册模式: {}", e.getMessage());
                    useProgrammaticRegistration = true;
                }
            }

            // 使用编程式注册模式
            if (useProgrammaticRegistration) {
                log.info("使用编程式注册模式初始化数据源");
                Map<String, String> registeredBeans = dynamicDataSourceRegistry.registerDataSources(dataSourceConfigs);
                log.info("编程式注册模式初始化完成，共注册 {} 个数据源", registeredBeans.size());
            }

        } catch (Exception e) {
            log.error("初始化数据源失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化数据源失败", e);
        }
    }

    /**
     * 刷新数据源
     * 清除缓存并重新获取数据源
     * 
     * @param dataSourceName 数据源名称
     */
    public void refreshDataSource(String dataSourceName) {
        dataSourceCache.remove(dataSourceName);
        log.info("已刷新数据源缓存: {}", dataSourceName);
    }

    /**
     * 刷新所有数据源
     */
    public void refreshAllDataSources() {
        dataSourceCache.clear();
        log.info("已刷新所有数据源缓存");
    }

    /**
     * 移除数据源
     * 
     * @param dataSourceName 数据源名称
     * @return 是否成功移除
     */
    public boolean removeDataSource(String dataSourceName) {
        try {
            // 从缓存移除
            dataSourceCache.remove(dataSourceName);
            
            // 从注册表移除
            if (useProgrammaticRegistration) {
                return dynamicDataSourceRegistry.removeDataSource(dataSourceName);
            }
            
            return true;
        } catch (Exception e) {
            log.error("移除数据源失败: {}, error: {}", dataSourceName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取当前使用的模式
     * 
     * @return true表示使用编程式注册模式，false表示使用AbstractRoutingDataSource模式
     */
    public boolean isUsingProgrammaticRegistration() {
        return useProgrammaticRegistration;
    }

    /**
     * 获取已缓存的数据源数量
     * 
     * @return 缓存的数据源数量
     */
    public int getCachedDataSourceCount() {
        return dataSourceCache.size();
    }

    /**
     * 获取ApplicationContext
     * 
     * @return ApplicationContext实例
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 获取所有已注册的数据源信息
     * 
     * @return 数据源名称到Bean名称的映射
     */
    public Map<String, String> getAllRegisteredDataSources() {
        if (useProgrammaticRegistration) {
            return dynamicDataSourceRegistry.getAllRegisteredDataSources();
        } else {
            // AbstractRoutingDataSource模式下，返回配置的数据源
            return dataSourceProperties.getDynamic().getDatasource().keySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    name -> name,
                    name -> "dynamicDataSource"
                ));
        }
    }
}
