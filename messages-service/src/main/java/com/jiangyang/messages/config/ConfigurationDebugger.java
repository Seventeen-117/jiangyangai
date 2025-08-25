package com.jiangyang.messages.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 配置调试工具
 * 在应用启动时检查配置加载情况
 */
@Slf4j
@Component
public class ConfigurationDebugger implements ApplicationRunner {

    @Autowired
    private Environment environment;

    @Autowired
    private MessageServiceConfig messageServiceConfig;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 配置调试工具启动 ===");
        
        // 检查环境信息
        log.info("环境信息:");
        log.info("  - activeProfiles: {}", Arrays.toString(environment.getActiveProfiles()));
        log.info("  - defaultProfiles: {}", Arrays.toString(environment.getDefaultProfiles()));
        
        // 检查关键配置属性
        log.info("关键配置属性:");
        log.info("  - spring.profiles.active: {}", environment.getProperty("spring.profiles.active"));
        log.info("  - message.service.kafka.enabled: {}", environment.getProperty("message.service.kafka.enabled"));
        log.info("  - message.service.kafka.bootstrap-servers: {}", environment.getProperty("message.service.kafka.bootstrap-servers"));
        log.info("  - message.service.common.default-type: {}", environment.getProperty("message.service.common.default-type"));
        
        // 检查MessageServiceConfig对象
        if (messageServiceConfig != null) {
            log.info("MessageServiceConfig对象状态:");
            log.info("  - config对象: {}", messageServiceConfig);
            
            if (messageServiceConfig.getKafka() != null) {
                log.info("  - kafka.enabled: {}", messageServiceConfig.getKafka().getEnabled());
                log.info("  - kafka.bootstrapServers: {}", messageServiceConfig.getKafka().getBootstrapServers());
                log.info("  - kafka.consumer: {}", messageServiceConfig.getKafka().getConsumer() != null ? "已配置" : "未配置");
                log.info("  - kafka.producer: {}", messageServiceConfig.getKafka().getProducer() != null ? "已配置" : "未配置");
            } else {
                log.warn("  - kafka配置为null");
            }
            
            if (messageServiceConfig.getCommon() != null) {
                log.info("  - common.defaultType: {}", messageServiceConfig.getCommon().getDefaultType());
                log.info("  - common.defaultMessageType: {}", messageServiceConfig.getCommon().getDefaultMessageType());
                log.info("  - common.defaultTopic: {}", messageServiceConfig.getCommon().getDefaultTopic());
            } else {
                log.warn("  - common配置为null");
            }
        } else {
            log.error("MessageServiceConfig对象为null");
        }
        
        log.info("=== 配置调试工具完成 ===");
    }
}
