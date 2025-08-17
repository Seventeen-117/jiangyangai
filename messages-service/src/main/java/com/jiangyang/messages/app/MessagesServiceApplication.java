package com.jiangyang.messages.app;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Messages Service application entrypoint.
 */
@SpringBootApplication(
    exclude = {
        SqlInitializationAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class
    }
)
@EnableDiscoveryClient
@EnableDubbo
public class MessagesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagesServiceApplication.class, args);
    }
}


