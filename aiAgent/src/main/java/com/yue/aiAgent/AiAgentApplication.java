package com.yue.aiAgent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

/**
 * AI智能代理服务主应用类
 * 
 * @author yue
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.yue.aiAgent")
@EnableDiscoveryClient
@EnableDubbo
public class AiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }

}
