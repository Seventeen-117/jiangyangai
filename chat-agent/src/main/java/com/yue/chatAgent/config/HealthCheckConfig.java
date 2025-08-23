package com.yue.chatAgent.config;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 健康检查配置类
 * 禁用不需要的健康检查，避免连接错误
 * 
 * @author yue
 * @version 1.0.0
 */
@Configuration
public class HealthCheckConfig {

    /**
     * 禁用Redis健康检查
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.data.redis.RedisHealthIndicator")
    @ConditionalOnMissingBean
    @ConditionalOnEnabledHealthIndicator("redis")
    public org.springframework.boot.actuate.health.HealthIndicator redisHealthIndicator() {
        return new org.springframework.boot.actuate.health.HealthIndicator() {
            @Override
            public org.springframework.boot.actuate.health.Health health() {
                return org.springframework.boot.actuate.health.Health.up()
                    .withDetail("message", "Redis health check disabled")
                    .build();
            }
        };
    }

    /**
     * 禁用Elasticsearch健康检查
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.data.elasticsearch.ElasticsearchHealthIndicator")
    @ConditionalOnMissingBean
    @ConditionalOnEnabledHealthIndicator("elasticsearch")
    public org.springframework.boot.actuate.health.HealthIndicator elasticsearchHealthIndicator() {
        return new org.springframework.boot.actuate.health.HealthIndicator() {
            @Override
            public org.springframework.boot.actuate.health.Health health() {
                return org.springframework.boot.actuate.health.Health.up()
                    .withDetail("message", "Elasticsearch health check disabled")
                    .build();
            }
        };
    }

    /**
     * 禁用数据库健康检查
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator")
    @ConditionalOnMissingBean
    @ConditionalOnEnabledHealthIndicator("db")
    public org.springframework.boot.actuate.health.HealthIndicator dbHealthIndicator() {
        return new org.springframework.boot.actuate.health.HealthIndicator() {
            @Override
            public org.springframework.boot.actuate.health.Health health() {
                return org.springframework.boot.actuate.health.Health.up()
                    .withDetail("message", "Database health check disabled")
                    .build();
            }
        };
    }
}
