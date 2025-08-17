package com.jiangyang.messages.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * RocketMQ自动配置启用类
 * 显式启用RocketMQ的自动配置
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.rocketmq.name-server")
@Import(RocketMQAutoConfiguration.class)
public class RocketMQAutoConfigurationEnabler {

    public RocketMQAutoConfigurationEnabler() {
        log.info("RocketMQ自动配置已启用");
    }
}
