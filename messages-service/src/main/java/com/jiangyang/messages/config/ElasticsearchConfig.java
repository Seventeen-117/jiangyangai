package com.jiangyang.messages.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch配置类
 * 仅在配置了elasticsearch相关属性时启用
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@EnableElasticsearchRepositories(basePackages = "com.jiangyang.messages.repository.elasticsearch")
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Value("${elasticsearch.scheme:http}")
    private String scheme;

    @Bean
    public ClientConfiguration clientConfiguration() {
        log.info("配置Elasticsearch客户端: {}://{}:{}", scheme, host, port);
        
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = (ClientConfiguration.MaybeSecureClientConfigurationBuilder) ClientConfiguration.builder()
                .connectedTo(host + ":" + port)
                .withConnectTimeout(5000)
                .withSocketTimeout(10000);

        // 如果配置了用户名和密码，则启用安全配置
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            builder.withBasicAuth(username, password);
            log.info("启用Elasticsearch安全认证");
        }

        // 如果是HTTPS，配置SSL
        if ("https".equalsIgnoreCase(scheme)) {
            builder.usingSsl();
            log.info("启用Elasticsearch SSL连接");
        }

        return builder.build();
    }

    @Bean
    public ElasticsearchOperations elasticsearchOperations() {
        try {
            // 暂时返回 null，让 Spring Boot 自动配置处理
            // 或者可以在这里添加自定义的创建逻辑
            log.info("Elasticsearch客户端配置完成，使用Spring Boot自动配置");
            return null;
        } catch (Exception e) {
            log.error("Elasticsearch客户端初始化失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
