package com.jiangyang.messages.app;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Messages Service application entrypoint.
 */
@SpringBootApplication(scanBasePackages = "com.jiangyang.messages")
@EnableDubbo
@MapperScan({
    "com.jiangyang.messages.audit.mapper",
    "com.jiangyang.messages.saga.mapper"
})
public class MessagesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagesServiceApplication.class, args);
    }
}


