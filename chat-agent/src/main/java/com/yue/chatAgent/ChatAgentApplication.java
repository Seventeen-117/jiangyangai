package com.yue.chatAgent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;

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
        DataSourceTransactionManagerAutoConfiguration.class,
        RedisAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class,
        ElasticsearchRestClientAutoConfiguration.class,
        OpenAiChatAutoConfiguration.class,
        AzureOpenAiChatAutoConfiguration.class,
        OllamaChatAutoConfiguration.class
    }
)
@EnableDiscoveryClient
@EnableDubbo
public class ChatAgentApplication {

    public static void main(String[] args) {
        // 在Spring Boot启动之前设置系统属性，确保启动信息显示
        setupSystemProperties();
        
        SpringApplication.run(ChatAgentApplication.class, args);
    }
    
    /**
     * 设置系统属性，启用启动信息显示
     */
    private static void setupSystemProperties() {
        // 启用启动信息
        System.setProperty("spring.main.log-startup-info", "true");
        
        // 启用banner
        System.setProperty("spring.main.banner-mode", "console");
        
        // 禁用Redis自动配置
        System.setProperty("spring.autoconfigure.exclude", 
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration," +
            "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration," +
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration," +
            "org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration," +
            "org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration");
        
        // 禁用健康检查
        System.setProperty("management.health.redis.enabled", "false");
        System.setProperty("management.health.elasticsearch.enabled", "false");
        System.setProperty("management.health.db.enabled", "false");
        System.setProperty("management.health.datasource.enabled", "false");
        System.setProperty("management.health.cache.enabled", "false");
        
        System.out.println("Chat-Agent 启动信息配置已设置");
    }

}
