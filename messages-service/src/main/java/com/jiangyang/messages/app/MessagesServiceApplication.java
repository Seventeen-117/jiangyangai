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
    "com.jiangyang.messages.audit.mapper",
    "com.jiangyang.messages.mapper"
})
public class MessagesServiceApplication {

    public static void main(String[] args) {
        // 在Spring Boot启动之前设置系统属性，禁用Dubbo监控
        setupSystemProperties();
        
        SpringApplication.run(MessagesServiceApplication.class, args);
    }
    
    /**
     * 设置系统属性，禁用Dubbo监控
     */
    private static void setupSystemProperties() {
        // 禁用Dubbo监控
        System.setProperty("dubbo.monitor.enabled", "false");
        System.setProperty("dubbo.monitor.protocol", "registry");
        System.setProperty("dubbo.monitor.address", "");
        
        // 禁用Dubbo监控过滤器
        System.setProperty("dubbo.consumer.filter", "-monitor");
        System.setProperty("dubbo.provider.filter", "-monitor");
        
        System.out.println("Dubbo监控已禁用");
    }
}


