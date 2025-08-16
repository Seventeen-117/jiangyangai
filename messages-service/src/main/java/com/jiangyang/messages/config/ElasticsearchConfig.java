package com.jiangyang.messages.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch配置类
 * 启用Elasticsearch Repository扫描
 * 让Spring Boot自动配置处理Elasticsearch客户端
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.jiangyang.messages.repository.elasticsearch")
public class ElasticsearchConfig {
    // Spring Boot会自动配置Elasticsearch客户端和ElasticsearchTemplate
}
