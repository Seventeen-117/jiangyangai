package com.jiangyang.base.seata.config;

import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Seata自动配置类
 * 提供Seata的基础配置和自动装配
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SeataProperties.class)
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeataAutoConfiguration {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * 配置Seata属性
     */
    @Bean
    @Primary
    public SeataProperties seataProperties() {
        SeataProperties properties = new SeataProperties();
        
        log.info("Seata配置初始化完成: applicationName={}", applicationName);
        
        return properties;
    }

    /**
     * 初始化Seata系统属性
     */
    @Bean
    public SeataSystemPropertiesInitializer seataSystemPropertiesInitializer() {
        return new SeataSystemPropertiesInitializer();
    }
}
