package com.jiangyang.messages.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * SQL初始化配置类
 * 用于禁用Spring Boot的自动SQL初始化功能
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    SqlInitializationAutoConfiguration.class
})
@ConditionalOnProperty(name = "spring.sql.init.mode", havingValue = "never", matchIfMissing = true)
public class SqlInitConfig {
    
    // 此配置类将禁用Spring Boot的自动SQL初始化
    // 包括ddlApplicationRunner bean的创建
    // 不再手动创建ddlApplicationRunner bean，避免与MyBatis-Plus冲突
}
