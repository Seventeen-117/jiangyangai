package com.jiangyang.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import io.seata.spring.boot.autoconfigure.SeataAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 * 
 * @author jiangyang
 * @since 2024-01-01
 */
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        SeataAutoConfiguration.class  // 禁用Seata分布式事务
    }
)
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        // 设置系统属性，禁用数据源
        System.setProperty("spring.datasource.enabled", "false");
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
} 