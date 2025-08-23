package com.jiangyang.messages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka配置类
 * 读取application-kafka.yml中的配置
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
        private String servers = "localhost:9092";
    }
    
    @Data
    public static class Security {
        private String protocol = "PLAINTEXT";
        private Sasl sasl = new Sasl();
        
        @Data
        public static class Sasl {
            private String mechanism = "PLAIN";
            private String username = "";
            private String password = "";
        }
    }
    
    @Data
    public static class Consumer {
        private String groupId = "default-consumer-group";
        private boolean enableAutoCommit = true;
        private int autoCommitInterval = 1000;
        private int sessionTimeout = 30000;
        private int heartbeatInterval = 10000;
        private int requestTimeout = 40000;
        private int retryBackoff = 1000;
        private int maxPollInterval = 300000;
        private int maxPollRecords = 500;
        private String partitionAssignmentStrategy = "org.apache.kafka.clients.consumer.RangeAssignor";
        private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
        private String valueDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
    }
    
    @Data
    public static class Producer {
        private String bootstrapServers = "localhost:9092";
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String acks = "all";
        private int retries = 3;
        private int batchSize = 16384;
        private int lingerMs = 1;
        private int bufferMemory = 33554432;
    }
    
    @Data
    public static class Topics {
        private String defaultTopic = "default-topic";
        private String dlq = "dlq-topic";
        private String retry = "retry-topic";
    }
    
    @Data
    public static class Consume {
        private String defaultMode = "PUSH";
        private String defaultType = "CLUSTERING";
        private String defaultOrder = "CONCURRENT";
        private int defaultBatchSize = 100;
        private int defaultMaxRetry = 3;
        private int defaultTimeout = 30000;
    }
}
