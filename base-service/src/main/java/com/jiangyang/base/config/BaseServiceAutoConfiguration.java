package com.jiangyang.base.config;

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
 */
@Configuration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "spring.datasource.dynamic", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({
    DataSourceConfig.class,
    MyBatisPlusConfig.class
})
public class BaseServiceAutoConfiguration {
    
    /**
     * 自动配置说明：
     * 1. 当类路径中存在 DataSource 类时启用
     * 2. 当配置了 spring.datasource.dynamic.enabled=true 时启用（默认启用）
     * 3. 自动导入 DataSourceConfig 和 MyBatisPlusConfig
     * 4. 启用 DataSourceProperties 配置属性绑定
     */
}
