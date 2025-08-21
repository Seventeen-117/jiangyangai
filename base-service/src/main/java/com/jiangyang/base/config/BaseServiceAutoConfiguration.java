package com.jiangyang.base.config;

import com.jiangyang.base.datasource.DynamicDataSourceManager;
import com.jiangyang.base.datasource.DynamicDataSourceRegistry;
import com.jiangyang.base.datasource.properties.DataSourceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * Base Service 自动配置类
 * 当引入 base-service 依赖时自动启用多数据源功能
 * 
 * 功能特性：
 * 1. 支持AbstractRoutingDataSource模式的动态数据源切换
 * 2. 支持编程式动态注册DataSource Bean（备用方案）
 * 3. 自动模式切换：当AbstractRoutingDataSource不可用时自动切换到编程式注册
 * 4. 集成Seata分布式事务支持
 * 5. 提供统一的数据源管理接口
 */
@Configuration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "spring.datasource.dynamic", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({
    DataSourceConfig.class,
    MyBatisPlusConfig.class,
    DynamicDataSourceRegistry.class,
    DynamicDataSourceManager.class,
    // Seata分布式事务配置
    com.jiangyang.base.seata.config.SeataAutoConfiguration.class,
    com.jiangyang.base.seata.config.SeataSagaConfig.class,
    com.jiangyang.base.seata.util.SeataConfigValidator.class
})
public class BaseServiceAutoConfiguration {
    
    /**
     * 自动配置说明：
     * 1. 当类路径中存在 DataSource 类时启用
     * 2. 当配置了 spring.datasource.dynamic.enabled=true 时启用（默认启用）
     * 3. 自动导入以下组件：
     *    - DataSourceConfig：数据源配置
     *    - MyBatisPlusConfig：MyBatis-Plus配置
     *    - DynamicDataSourceRegistry：动态数据源注册服务
     *    - DynamicDataSourceManager：动态数据源管理器
     * 4. 启用 DataSourceProperties 配置属性绑定
     * 
     * 使用方式：
     * 1. 通过@DataSource注解指定数据源
     * 2. 通过DynamicDataSourceManager编程式获取数据源
     * 3. 通过DynamicDataSourceRegistry动态注册新数据源
     */
}
