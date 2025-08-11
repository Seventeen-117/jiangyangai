package com.jiangyang.messages.config;

import io.seata.spring.annotation.GlobalTransactionScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seata 分布式事务配置类
 * 用于配置和启用 Seata 分布式事务功能
 */
@Configuration
public class SeataConfig {

    /**
     * 配置全局事务扫描器
     * 用于扫描和注册全局事务方法
     */
    @Bean
    public GlobalTransactionScanner globalTransactionScanner() {
        return new GlobalTransactionScanner("messages-service", "messages-service-group");
    }
}
