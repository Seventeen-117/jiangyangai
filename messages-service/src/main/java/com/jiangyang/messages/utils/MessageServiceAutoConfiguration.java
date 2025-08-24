package com.jiangyang.messages.utils;
import com.jiangyang.messages.config.MessageServiceConfig;

import com.jiangyang.messages.kafka.KafkaMessageService;
import com.jiangyang.messages.rabbitmq.RabbitMQMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.ApplicationContext;

/**
 * 消息服务自动配置类
 * 根据Nacos配置中心动态配置消息服务
 * 支持配置热更新和动态服务启停
 */
@Slf4j
@Configuration
@Conditional(SpringCloudContextCondition.class)
public class MessageServiceAutoConfiguration {

    @Autowired
    private MessageServiceConfig config;

    /**
     * 配置Kafka消息服务
     * 根据Nacos配置动态启用/禁用
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(KafkaMessageService.class)
    @ConditionalOnProperty(prefix = "message.service.kafka", name = "enabled", havingValue = "true")
    @RefreshScope
    public KafkaMessageService kafkaMessageService() {
        log.info("Configuring Kafka Message Service with config: {}", config.getKafka());
        
        KafkaMessageService service = new KafkaMessageService();
        
        // 应用配置到服务
        applyKafkaConfig(service);
        
        // 初始化服务
        service.init();
        
        log.info("Kafka Message Service configured successfully");
        return service;
    }

    /**
     * 配置RabbitMQ消息服务
     * 根据Nacos配置动态启用/禁用
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RabbitMQMessageService.class)
    @ConditionalOnProperty(prefix = "message.service.rabbitmq", name = "enabled", havingValue = "true")
    @RefreshScope
    public RabbitMQMessageService rabbitMQMessageService() {
        log.info("Configuring RabbitMQ Message Service with config: {}", config.getRabbitmq());
        
        RabbitMQMessageService service = new RabbitMQMessageService();
        
        // 应用配置到服务
        applyRabbitMQConfig(service);
        
        // 初始化服务
        service.init();
        
        log.info("RabbitMQ Message Service configured successfully");
        return service;
    }

    /**
     * 应用Kafka配置到服务
     */
    private void applyKafkaConfig(KafkaMessageService service) {
        try {
            MessageServiceConfig.Kafka kafkaConfig = config.getKafka();
            
            // 直接设置配置属性
            service.setBootstrapServers(kafkaConfig.getBootstrapServers());
            // 注意：Kafka配置中没有直接的acks、retries等字段，需要从producer配置中获取
            // 这里暂时使用默认值，实际项目中需要根据具体需求调整
            service.setAcks("1");
            service.setRetries(3);
            service.setBatchSize(16384);
            service.setLingerMs(1);
            service.setBufferMemory(33554432);
            
            log.debug("Applied Kafka configuration: bootstrapServers={}, acks={}, retries={}, batchSize={}, lingerMs={}, bufferMemory={}",
                    kafkaConfig.getBootstrapServers(),
                    "1", 3, 16384, 1, 33554432);
        } catch (Exception e) {
            log.warn("Failed to apply Kafka configuration", e);
        }
    }

    /**
     * 应用RabbitMQ配置到服务
     */
    private void applyRabbitMQConfig(RabbitMQMessageService service) {
        try {
            MessageServiceConfig.RabbitMQ rabbitmqConfig = config.getRabbitmq();
            
            // 直接设置配置属性
            service.setHost(rabbitmqConfig.getHost());
            service.setPort(rabbitmqConfig.getPort());
            service.setUsername(rabbitmqConfig.getUsername());
            service.setPassword(rabbitmqConfig.getPassword());
            service.setVirtualHost(rabbitmqConfig.getVirtualHost());
            service.setConnectionTimeout(rabbitmqConfig.getConnection().getTimeout());
            service.setRequestedHeartbeat(rabbitmqConfig.getConnection().getHeartbeat());
            service.setAutomaticRecovery(rabbitmqConfig.getConnection().getAutomaticRecovery());
            
            log.debug("Applied RabbitMQ configuration: host={}, port={}, username={}, virtualHost={}, connectionTimeout={}, requestedHeartBeat={}, automaticRecoveryEnabled={}",
                    rabbitmqConfig.getHost(),
                    rabbitmqConfig.getPort(),
                    rabbitmqConfig.getUsername(),
                    rabbitmqConfig.getVirtualHost(),
                    rabbitmqConfig.getConnection().getTimeout(),
                    rabbitmqConfig.getConnection().getHeartbeat(),
                    rabbitmqConfig.getConnection().getAutomaticRecovery());
        } catch (Exception e) {
            log.warn("Failed to apply RabbitMQ configuration", e);
        }
    }

    /**
     * 获取当前配置状态
     * 用于监控和调试
     */
    public String getConfigurationStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Message Service Configuration Status:\n");
        status.append("Default Type: ").append(config.getCommon().getDefaultType()).append("\n");
        status.append("RocketMQ Name Server: ").append(config.getRocketmq().getNameServer()).append("\n");
        status.append("Kafka Bootstrap Servers: ").append(config.getKafka().getBootstrapServers()).append("\n");
        status.append("RabbitMQ Host: ").append(config.getRabbitmq().getHost()).append("\n");
        status.append("Common Consume Mode: ").append(config.getCommon().getConsume().getDefaultMode()).append("\n");
        status.append("Retry Enabled: ").append(config.getCommon().getRetry().getEnabled()).append("\n");
        status.append("Max Retries: ").append(config.getCommon().getRetry().getMaxRetries()).append("\n");
        status.append("Monitoring Enabled: ").append(config.getCommon().getMonitoring().getEnabled());
        
        return status.toString();
    }
}
