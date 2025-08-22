package com.yue.chatAgent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

/**
 * AI智能代理服务主应用类
 * 
 * @author yue
 * @version 1.0.0
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.yue.chatAgent",
        "com.jiangyang.base.config"
    },
    exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
    }
)
@EnableDiscoveryClient
@EnableDubbo
public class ChatAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatAgentApplication.class, args);
    }

}
