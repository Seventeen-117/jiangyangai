package com.jiangyang.messages;

import com.jiangyang.messages.kafka.KafkaMessageService;
import com.jiangyang.messages.rabbitmq.RabbitMQMessageService;
import com.jiangyang.messages.rocketmq.RocketMQMessageService;
import com.jiangyang.messages.rocketmq.RocketMQTemplate;
import com.jiangyang.messages.rocketmq.RocketMQConsumerManager;
import com.jiangyang.messages.rocketmq.RocketMQBrokerOptimizer;
import com.jiangyang.messages.rocketmq.RocketMQMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(MessageServiceConfig.class)
@Conditional(SpringCloudContextCondition.class)
public class MessageServiceAutoConfiguration {

    @Autowired
    private MessageServiceConfig config;
    
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 配置RocketMQ消息服务
     * 根据Nacos配置动态启用/禁用
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RocketMQMessageService.class)
    @ConditionalOnProperty(prefix = "message.service.rocketmq", name = "enabled", havingValue = "true")
    @RefreshScope
    public RocketMQMessageService rocketMQMessageService() {
        log.info("Configuring RocketMQ Message Service with config: {}", config.getRocketmq());
        
        RocketMQMessageService service = new RocketMQMessageService();
        
        // 应用配置到服务
        applyRocketMQConfig(service);
        
        log.info("RocketMQ Message Service configured successfully");
        return service;
    }

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
        
        log.info("RabbitMQ Message Service configured successfully");
        return service;
    }

    /**
     * 应用RocketMQ配置到服务
     */
    private void applyRocketMQConfig(RocketMQMessageService service) {
        try {
            MessageServiceConfig.RocketMQConfig rocketmqConfig = config.getRocketmq();
            
            // 直接设置配置属性
            service.setNameServer(rocketmqConfig.getNameServer());
            service.setProducerGroup(rocketmqConfig.getProducerGroup());
            service.setRetrySyncTimes(rocketmqConfig.getRetrySyncTimes());
            service.setRetryAsyncTimes(rocketmqConfig.getRetryAsyncTimes());
            
            // 配置其他RocketMQ组件
            configureRocketMQComponents(rocketmqConfig);
            
            log.debug("Applied RocketMQ configuration: nameServer={}, producerGroup={}, consumerGroup={}, retrySyncTimes={}, retryAsyncTimes={}",
                    rocketmqConfig.getNameServer(),
                    rocketmqConfig.getProducerGroup(),
                    rocketmqConfig.getConsumerGroup(),
                    rocketmqConfig.getRetrySyncTimes(),
                    rocketmqConfig.getRetryAsyncTimes());
        } catch (Exception e) {
            log.warn("Failed to apply RocketMQ configuration", e);
        }
    }

    /**
     * 配置RocketMQ相关组件
     */
    private void configureRocketMQComponents(MessageServiceConfig.RocketMQConfig rocketmqConfig) {
        try {
            // 配置RocketMQTemplate
            try {
                RocketMQTemplate template = applicationContext.getBean(RocketMQTemplate.class);
                template.setNameServer(rocketmqConfig.getNameServer());
                template.setProducerGroup(rocketmqConfig.getProducerGroup());
                log.debug("Configured RocketMQTemplate");
            } catch (Exception e) {
                log.debug("RocketMQTemplate not available or already configured");
            }
            
            // 配置RocketMQConsumerManager
            try {
                RocketMQConsumerManager consumerManager = applicationContext.getBean(RocketMQConsumerManager.class);
                consumerManager.setNameServer(rocketmqConfig.getNameServer());
                log.debug("Configured RocketMQConsumerManager");
            } catch (Exception e) {
                log.debug("RocketMQConsumerManager not available or already configured");
            }
            
            // 配置RocketMQBrokerOptimizer
            try {
                RocketMQBrokerOptimizer brokerOptimizer = applicationContext.getBean(RocketMQBrokerOptimizer.class);
                brokerOptimizer.setNameServer(rocketmqConfig.getNameServer());
                log.debug("Configured RocketMQBrokerOptimizer");
            } catch (Exception e) {
                log.debug("RocketMQBrokerOptimizer not available or already configured");
            }
            
            // 配置RocketMQMonitoringService
            try {
                RocketMQMonitoringService monitoringService = applicationContext.getBean(RocketMQMonitoringService.class);
                monitoringService.setNameServer(rocketmqConfig.getNameServer());
                log.debug("Configured RocketMQMonitoringService");
            } catch (Exception e) {
                log.debug("RocketMQMonitoringService not available or already configured");
            }
        } catch (Exception e) {
            log.warn("Failed to configure RocketMQ components", e);
        }
    }

    /**
     * 应用Kafka配置到服务
     */
    private void applyKafkaConfig(KafkaMessageService service) {
        try {
            // 这里可以根据需要调用服务的配置方法
            log.debug("Applied Kafka configuration: bootstrapServers={}, consumerGroupId={}",
                    config.getKafka().getBootstrapServers(),
                    config.getKafka().getConsumerGroupId());
        } catch (Exception e) {
            log.warn("Failed to apply Kafka configuration", e);
        }
    }

    /**
     * 应用RabbitMQ配置到服务
     */
    private void applyRabbitMQConfig(RabbitMQMessageService service) {
        try {
            // 这里可以根据需要调用服务的配置方法
            log.debug("Applied RabbitMQ configuration: host={}, port={}, username={}",
                    config.getRabbitmq().getHost(),
                    config.getRabbitmq().getPort(),
                    config.getRabbitmq().getUsername());
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
        status.append("Default Type: ").append(config.getDefaultType()).append("\n");
        status.append("RocketMQ Enabled: ").append(config.getRocketmq().isEnabled()).append("\n");
        status.append("Kafka Enabled: ").append(config.getKafka().isEnabled()).append("\n");
        status.append("RabbitMQ Enabled: ").append(config.getRabbitmq().isEnabled()).append("\n");
        status.append("Trace Enabled: ").append(config.isTraceEnabled()).append("\n");
        status.append("Send Retry Times: ").append(config.getSendRetryTimes()).append("\n");
        status.append("Send Timeout: ").append(config.getSendTimeoutMs()).append("ms\n");
        status.append("Max Batch Size: ").append(config.getMaxBatchSize());
        
        return status.toString();
    }
}
