package com.jiangyang.messages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息服务统一配置类
 * 合并Kafka、RabbitMQ、RocketMQ的配置
 * 读取application-message-service.yml中的配置
 * 所有配置都从配置文件读取，支持环境变量覆盖
 */
@Data
@Component
@ConfigurationProperties(prefix = "message.service")
public class MessageServiceConfig {

    /**
     * Kafka配置
     */
    private Kafka kafka = new Kafka();
    
    /**
     * RabbitMQ配置
     */
    private RabbitMQ rabbitmq = new RabbitMQ();
    
    /**
     * RocketMQ配置
     */
    private RocketMQ rocketmq = new RocketMQ();
    
    /**
     * 通用消息服务配置
     */
    private Common common = new Common();
    
    /**
     * Kafka配置
     */
    @Data
    public static class Kafka {
        /**
         * 是否启用Kafka服务
         */
        private Boolean enabled = true;
        
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
    
    /**
     * RabbitMQ配置
     */
    @Data
    public static class RabbitMQ {
        /**
         * 是否启用RabbitMQ服务
         */
        private Boolean enabled = true;
        
        /**
         * 基础连接配置
         */
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String virtualHost;
        
        /**
         * 连接池配置
         */
        private Connection connection = new Connection();
        
        /**
         * 消费者配置
         */
        private Consumer consumer = new Consumer();
        
        /**
         * 生产者配置
         */
        private Producer producer = new Producer();
        
        /**
         * 队列配置
         */
        private Queue queue = new Queue();
        
        /**
         * 交换机配置
         */
        private Exchange exchange = new Exchange();
        
        /**
         * 死信队列配置
         */
        private DeadLetter deadLetter = new DeadLetter();
        
        /**
         * 延迟队列配置
         */
        private Delay delay = new Delay();
        
        /**
         * 优先级队列配置
         */
        private Priority priority = new Priority();
        
        /**
         * 集群配置
         */
        private Cluster cluster = new Cluster();
        
        /**
         * 监控配置
         */
        private Monitoring monitoring = new Monitoring();
        
        @Data
        public static class Connection {
            private Integer timeout;
            private Integer heartbeat;
            private Boolean automaticRecovery;
            private Integer networkRecoveryInterval;
        }
        
        @Data
        public static class Consumer {
            private String defaultMode;
            private String defaultAckMode;
            private Integer prefetchCount;
            private Integer batchSize;
            private Integer consumeInterval;
            private Integer maxRetryTimes;
            private Integer timeout;
        }
        
        @Data
        public static class Producer {
            private String confirmMode;
            private String returnMode;
            private Integer batchSize;
            private Integer timeout;
        }
        
        @Data
        public static class Queue {
            private String defaultType;
            private Boolean durable;
            private Boolean exclusive;
            private Boolean autoDelete;
            private Integer maxPriority;
            private Integer maxLength;
            private Long messageTtl;
        }
        
        @Data
        public static class Exchange {
            private String defaultType;
            private Boolean durable;
            private Boolean autoDelete;
            private Boolean internal;
        }
        
        @Data
        public static class DeadLetter {
            private String exchangePrefix;
            private String routingKeyPrefix;
            private String queuePrefix;
            private Boolean enabled;
        }
        
        @Data
        public static class Delay {
            private Boolean enabled;
            private String exchangePrefix;
            private String queuePrefix;
            private Long defaultTtl;
        }
        
        @Data
        public static class Priority {
            private Boolean enabled;
            private Integer maxPriority;
            private Integer defaultPriority;
        }
        
        @Data
        public static class Cluster {
            private Boolean enabled;
            private String nodes; // 从配置文件读取为逗号分隔的字符串
            private String loadBalance;
            
            /**
             * 获取集群节点数组
             */
            public String[] getNodesArray() {
                if (nodes == null || nodes.trim().isEmpty()) {
                    return new String[0];
                }
                return nodes.split(",");
            }
        }
        
        @Data
        public static class Monitoring {
            private Boolean enabled;
            private Integer interval;
            private Boolean verboseLogging;
        }
    }
    
    /**
     * RocketMQ配置
     */
    @Data
    public static class RocketMQ {
        /**
         * 是否启用RocketMQ服务
         */
        private Boolean enabled = true;
        
        /**
         * NameServer地址
         */
        private String nameServer;
        
        /**
         * 生产者组
         */
        private String producerGroup;
        
        /**
         * 消费者组
         */
        private String consumerGroup;
        
        /**
         * 生产者配置
         */
        private Producer producer = new Producer();
        
        /**
         * 消费者配置
         */
        private Consumer consumer = new Consumer();
        
        /**
         * 事务消息配置
         */
        private Transaction transaction = new Transaction();
        
        @Data
        public static class Producer {
            private Integer sendMsgTimeout;
            private Integer retryTimesWhenSendFailed;
            private Integer retryTimesWhenSendAsyncFailed;
            private Integer compressMsgBodyOverHowmuch;
            private Integer maxMessageSize;
            private Boolean vipChannelEnabled;
            private String instanceName;
    }

    @Data
        public static class Consumer {
            private String consumeFromWhere;
            private Integer consumeTimeout;
            private Integer pullBatchSize;
            private Integer pullInterval;
            private Integer consumeMessageBatchMaxSize;
            private Integer maxReconsumeTimes;
    }

    @Data
        public static class Transaction {
            private Boolean enabled;
            private Integer checkThreadPoolMinSize;
            private Integer checkThreadPoolMaxSize;
            private Integer checkRequestHoldMax;
            private String checkRequestHoldMaxValue;
        }
    }
    
    /**
     * 通用消息服务配置
     */
    @Data
    public static class Common {
        /**
         * 消息服务类型
         */
        private String defaultType; // kafka, rabbitmq, rocketmq
        
        /**
         * 默认消息类型
         */
        private String defaultMessageType; // normal, delay, ordered, transaction
        
        /**
         * 默认主题
         */
        private String defaultTopic; // 默认消息主题
        
        /**
         * 消息发送配置
         */
        private Send send = new Send();
        
        /**
         * 消息消费配置
         */
        private Consume consume = new Consume();
        
        /**
         * 重试配置
         */
        private Retry retry = new Retry();
        
        /**
         * 监控配置
         */
        private Monitoring monitoring = new Monitoring();
        
        @Data
        public static class Send {
            private Boolean asyncEnabled;
            private Integer maxRetries;
            private Integer retryInterval;
            private Boolean confirmEnabled;
            private Integer confirmTimeout;
        }
        
        @Data
        public static class Consume {
            private String defaultMode; // push, pull
            private String defaultType; // clustering, broadcasting
            private String defaultOrder; // concurrent, orderly
            private Integer maxConcurrency;
            private Integer batchSize;
            private Integer timeout;
        }
        
        @Data
        public static class Retry {
            private Boolean enabled;
            private Integer maxRetries;
            private Integer retryInterval;
            private String backoffStrategy; // fixed, exponential
            private Double backoffMultiplier;
            private Integer maxRetryInterval;
        }
        
        @Data
        public static class Monitoring {
            private Boolean enabled;
            private Integer interval;
            private Boolean verboseLogging;
            private String metricsBackend; // prometheus, influxdb, custom
        }
    }
}
