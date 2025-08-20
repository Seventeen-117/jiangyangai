package com.jiangyang.messages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 统一的消息服务配置类
 * - 汇总默认消息参数与各中间件(RocketMQ/Kafka/RabbitMQ)配置
 * - 单一入口：message.service.*
 */
@Data
@Component("messageServiceConfig")
@Primary
@ConfigurationProperties(prefix = "message.service", ignoreUnknownFields = true)
public class MessageServiceConfig {

    // ========== 通用/默认配置 ==========

    /**
     * 默认消息类型 (ROCKETMQ, KAFKA, RABBITMQ)
     */
    private String defaultMessageType = "ROCKETMQ";

    /**
     * 兼容旧字段：type (如配置存在则作为默认类型)
     */
    private String type = "rocketmq";

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

    /** 是否启用消息轨迹追踪 */
    private boolean traceEnabled = false;

    /** 消息发送重试次数 */
    private int sendRetryTimes = 3;

    /** 消息发送超时时间（毫秒） */
    private long sendTimeoutMs = 3000;

    /** 最大批量发送消息数量 */
    private int maxBatchSize = 100;

    // ========== 各中间件配置 ==========

    private RocketMQConfig rocketmq = new RocketMQConfig();
    private KafkaConfig kafka = new KafkaConfig();
    private RabbitMQConfig rabbitmq = new RabbitMQConfig();

    // ========== 便捷方法 ==========

    /** 兼容旧方法：获取默认类型（优先 defaultMessageType，其次 type） */
    public String getDefaultType() {
        return defaultMessageType != null ? defaultMessageType : type;
    }

    /** 判定某种类型是否启用 */
    public boolean isServiceEnabled(String type) {
        if (type == null) {
            return false;
        }
        switch (type.toLowerCase()) {
            case "rocketmq":
                return rocketmq.isEnabled();
            case "kafka":
                return kafka.isEnabled();
            case "rabbitmq":
                return rabbitmq.isEnabled();
            default:
                return false;
        }
    }

    // ========== 嵌套配置类 ==========

    @Data
    public static class RocketMQConfig {
        /** 是否启用RocketMQ */
        private boolean enabled = false;
        /** Name Server地址（必填） */
        private String nameServer;
        /** 生产者组（必填） */
        private String producerGroup;
        /** 消费者组（可选） */
        private String consumerGroup;
        /** 压缩策略：LZ4, SNAPPY, ZSTD */
        private String compressionStrategy = "LZ4";
        /** 同步/异步重试次数 */
        private int retrySyncTimes = 3;
        private int retryAsyncTimes = 3;
        /** Topic 队列数量 */
        private int queueCountPerTopic = 4;
        /** 消息保留时间（小时） */
        private int messageRetentionHours = 72;
        /** 发送消息超时时间 */
        private int sendMsgTimeout = 3000;
        /** 压缩阈值/最大大小 */
        private int compressMsgBodyOverHowmuch = 4096;
        private int maxMessageSize = 4194304;
        /** 轨迹 */
        private boolean enableMsgTrace = true;
        private String traceTopicName = "RMQ_SYS_TRACE_TOPIC";
    }

    @Data
    public static class KafkaConfig {
        /** 是否启用Kafka */
        private boolean enabled = false;
        /** Bootstrap Servers（必填） */
        private String bootstrapServers;
        /** 其他可选参数 */
        private String consumerGroupId = "DEFAULT_CONSUMER_GROUP";
        private String acks = "all";
        private int batchSize = 16384;
        private int lingerMs = 1;
        private long bufferMemory = 33554432L;
        private int retries = 3;
        private int autoCommitIntervalMs = 1000;
        private int sessionTimeoutMs = 30000;
        private int heartbeatIntervalMs = 3000;
        private int maxPollRecords = 500;
        private int maxPollIntervalMs = 300000;
    }

    @Data
    public static class RabbitMQConfig {
        /** 是否启用RabbitMQ */
        private boolean enabled = false;
        /** 主机地址（必填） */
        private String host;
        /** 端口（默认5672） */
        private int port = 5672;
        /** 凭证/虚拟主机 */
        private String username;
        private String password;
        private String virtualHost = "/";
        /** 连接/心跳/恢复 */
        private int connectionTimeout = 60000;
        private int channelRpcTimeout = 10000;
        private int requestedHeartBeat = 60;
        private int connectionRecoveryInterval = 10000;
        private boolean automaticRecoveryEnabled = true;
        private boolean topologyRecoveryEnabled = true;
    }
}
