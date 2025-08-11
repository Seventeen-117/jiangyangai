package com.jiangyang.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 数据源配置类
 * 网关服务不需要数据库功能，禁用所有数据源相关配置
 * 
 * @author jiangyang
 * @since 2024-01-01
 */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.enabled", havingValue = "false", matchIfMissing = true)
public class DataSourceConfig {
    
    // 网关服务不需要数据库功能，此类仅用于禁用数据源自动配置
} 