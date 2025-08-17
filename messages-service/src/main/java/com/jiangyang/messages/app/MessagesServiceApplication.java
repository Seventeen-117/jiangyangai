package com.jiangyang.messages.app;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;

/**
 * Messages Service application entrypoint.
 */
@SpringBootApplication(
    scanBasePackages = {"com.jiangyang.messages", "com.jiangyang.base"},
    exclude = {
        SqlInitializationAutoConfiguration.class
    }
)
@EnableDiscoveryClient
@EnableDubbo
@MapperScan(basePackages = {
    "com.jiangyang.messages.saga.mapper",
    "com.jiangyang.messages.audit.mapper"
})
public class MessagesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagesServiceApplication.class, args);
    }

    /**
     * 提供一个空的ApplicationRunner来替代缺失的ddlApplicationRunner
     */
    @Bean
    public ApplicationRunner ddlApplicationRunner() {
        return args -> {
            // 空实现，只是为了满足Spring Boot的Bean要求
        };
    }
}


