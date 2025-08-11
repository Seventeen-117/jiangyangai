package com.jiangyang.messages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息服务配置类
 * 配置消息中间件的相关参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "message.service")
public class MessageServiceConfig {

    /**
     * 默认消息类型 (ROCKETMQ, KAFKA, RABBITMQ)
     */
    private String defaultMessageType = "ROCKETMQ";

    /**
     * 默认主题
     */
    private String defaultTopic = "default-topic";

    /**
     * 默认消费者组
     */
    private String defaultConsumerGroup = "default-consumer-group";

    /**
     * 默认生产者组
     */
    private String defaultProducerGroup = "default-producer-group";

    /**
     * 消息超时时间 (毫秒)
     */
    private Long messageTimeout = 30000L;

    /**
     * 消息重试次数
     */
    private Integer messageRetryCount = 3;

    /**
     * 批量消息大小
     */
    private Integer batchMessageSize = 100;

    /**
     * 消息处理线程池大小
     */
    private Integer messageThreadPoolSize = 10;

    /**
     * 是否启用消息持久化
     */
    private Boolean enableMessagePersistence = true;

    /**
     * 是否启用消息审计
     */
    private Boolean enableMessageAudit = true;

    /**
     * 是否启用Saga事务
     */
    private Boolean enableSagaTransaction = true;
}
