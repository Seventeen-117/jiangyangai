package com.jiangyang.messages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka配置类
 * 读取application-kafka.yml中的配置
 * 包含基础连接、安全、消费者、生产者、主题和消费配置
 * 所有配置都从配置文件读取，支持环境变量覆盖
 */
@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfig {
    
    /**
     * 基础连接配置
     */
    private BootstrapServers bootstrapServers = new BootstrapServers();
    
    /**
     * 安全配置
     */
    private Security security = new Security();
    
    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();
    
    /**
     * 生产者配置
     */
    private Producer producer = new Producer();
    
    /**
     * 主题配置
     */
    private Topics topics = new Topics();
    
    /**
     * 消费配置
     */
    private Consume consume = new Consume();
    
    @Data
    public static class BootstrapServers {
        private String servers;
    }
    
    @Data
    public static class Security {
        private String protocol;
        private Sasl sasl = new Sasl();
        
        @Data
        public static class Sasl {
            private String mechanism;
            private String username;
            private String password;
        }
    }
    
    @Data
    public static class Consumer {
        // 基础配置
        private String groupId;
        private String clientId;
        private Boolean enableAutoCommit;
        private Integer autoCommitInterval;
        private Integer sessionTimeout;
        private Integer heartbeatInterval;
        private Integer requestTimeout;
        private Integer retryBackoff;
        private Integer maxPollInterval;
        private Integer maxPollRecords;
        private Integer maxPollBytes;
        private Integer connectionsMaxIdleMs;
        private Integer metadataMaxAgeMs;
        private Integer reconnectBackoffMs;
        private Integer reconnectBackoffMaxMs;
        private Integer retryBackoffMaxMs;
        private String partitionAssignmentStrategy;
        private String keyDeserializer;
        private String valueDeserializer;
        
        // 消费组配置
        private String protocol;
        private Integer rebalanceTimeoutMs;
        private String coordinatorAddress;
        private Boolean groupEnabled;
        
        // 偏移量管理配置
        private String autoOffsetReset;
        private Integer commitTimeoutMs;
        private Integer commitRetryCount;
        private Integer commitRetryIntervalMs;
        private Boolean enableOffsetCache;
        private Integer offsetCacheSize;
        private Integer offsetCacheExpireMs;
        
        // 批量消费配置
        private Boolean batchEnabled;
        private Integer batchSize;
        private Integer batchTimeoutMs;
        private Integer batchThreadPoolSize;
        private Integer batchQueueSize;
        private String batchStrategy;
        private Integer batchMaxWaitTimeMs;
        
        // 顺序消费配置
        private Boolean orderEnabled;
        private Integer orderPartitionCount;
        private Integer orderThreadCount;
        private Integer orderTimeoutMs;
        private Integer orderRetryCount;
        private Integer orderRetryIntervalMs;
        private String orderStrategy;
        
        // 重试配置
        private Boolean retryEnabled;
        private Integer maxRetries;
        private Integer retryIntervalMs;
        private String retryBackoffStrategy;
        private Double retryBackoffMultiplier;
        private Integer maxRetryIntervalMs;
        private String retryableExceptions; // 从配置文件读取为逗号分隔的字符串
        
        /**
         * 获取重试异常数组
         */
        public String[] getRetryableExceptionsArray() {
            if (retryableExceptions == null || retryableExceptions.trim().isEmpty()) {
                return new String[0];
            }
            return retryableExceptions.split(",");
        }
    }
    
    @Data
    public static class Producer {
        private String bootstrapServers;
        private String keySerializer;
        private String valueSerializer;
        private String acks;
        private Integer retries;
        private Integer batchSize;
        private Integer lingerMs;
        private Integer bufferMemory;
    }
    
    @Data
    public static class Topics {
        private String defaultTopic;
        private String dlq;
        private String retry;
    }
    
    @Data
    public static class Consume {
        private String defaultMode;
        private String defaultType;
        private String defaultOrder;
        private Integer defaultBatchSize;
        private Integer defaultMaxRetry;
        private Integer defaultTimeout;
    }
    
    /**
     * 获取完整的Kafka消费者配置属性
     */
    public Map<String, Object> getConsumerKafkaProperties() {
        Map<String, Object> props = new HashMap<>();

        // 基础配置
        props.put("bootstrap.servers", bootstrapServers.getServers());
        props.put("group.id", consumer.getGroupId());
        props.put("client.id", consumer.getClientId());
        props.put("session.timeout.ms", consumer.getSessionTimeout());
        props.put("heartbeat.interval.ms", consumer.getHeartbeatInterval());
        props.put("max.poll.records", consumer.getMaxPollRecords());
        props.put("max.poll.bytes", consumer.getMaxPollBytes());
        props.put("connections.max.idle.ms", consumer.getConnectionsMaxIdleMs());
        props.put("request.timeout.ms", consumer.getRequestTimeout());
        props.put("metadata.max.age.ms", consumer.getMetadataMaxAgeMs());
        props.put("reconnect.backoff.ms", consumer.getReconnectBackoffMs());
        props.put("reconnect.backoff.max.ms", consumer.getReconnectBackoffMaxMs());
        props.put("retry.backoff.ms", consumer.getRetryBackoff());
        props.put("retry.backoff.max.ms", consumer.getRetryBackoffMaxMs());

        // 消费组配置
        props.put("partition.assignment.strategy", consumer.getPartitionAssignmentStrategy());
        props.put("rebalance.timeout.ms", consumer.getRebalanceTimeoutMs());
        props.put("max.poll.interval.ms", consumer.getMaxPollInterval());

        // 偏移量管理配置
        props.put("enable.auto.commit", consumer.getEnableAutoCommit());
        props.put("auto.commit.interval.ms", consumer.getAutoCommitInterval());
        props.put("auto.offset.reset", consumer.getAutoOffsetReset());

        // 序列化器配置
        props.put("key.deserializer", consumer.getKeyDeserializer());
        props.put("value.deserializer", consumer.getValueDeserializer());

        return props;
    }
    
    /**
     * 获取特定消费方式的配置
     */
    public Map<String, Object> getConsumerProperties(ConsumerType consumerType) {
        Map<String, Object> props = getConsumerKafkaProperties();

        switch (consumerType) {
            case CLUSTER:
                // 集群消费配置
                props.put("enable.auto.commit", false); // 关闭自动提交
                props.put("auto.offset.reset", "earliest"); // 从头开始消费
                break;
            case BROADCAST:
                // 广播消费配置
                props.put("enable.auto.commit", true); // 开启自动提交
                props.put("auto.offset.reset", "latest"); // 从最新位置消费
                break;
            case ORDERLY:
                // 顺序消费配置
                props.put("enable.auto.commit", false); // 关闭自动提交
                props.put("max.poll.records", 1); // 每次只拉取一条消息
                break;
            case BATCH:
                // 批量消费配置
                props.put("enable.auto.commit", false); // 关闭自动提交
                props.put("max.poll.records", consumer.getBatchSize()); // 设置批量大小
                break;
            default:
                // 默认配置
                break;
        }

        return props;
    }
    
    /**
     * 消费者类型枚举
     */
    public enum ConsumerType {
        CLUSTER,    // 集群消费
        BROADCAST,  // 广播消费
        ORDERLY,    // 顺序消费
        BATCH       // 批量消费
    }
}
