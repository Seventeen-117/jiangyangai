package com.jiangyang.messages.app;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.mybatis.spring.annotation.MapperScan;

/**
 * Messages Service application entrypoint.
 */
@SpringBootApplication(
    scanBasePackages = {"com.jiangyang.messages", "com.jiangyang.base"},
    exclude = {
        SqlInitializationAutoConfiguration.class, MybatisPlusAutoConfiguration.class
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
}


