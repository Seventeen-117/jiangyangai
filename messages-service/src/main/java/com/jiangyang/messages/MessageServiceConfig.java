package com.jiangyang.messages;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * 消息服务配置类
 * 用于从Nacos配置中心读取消息服务相关配置
 * 支持配置热更新和动态配置
 */
@Data
@RefreshScope
@ConfigurationProperties(prefix = "message.service")
public class MessageServiceConfig {

    /**
     * 消息服务类型，默认为rocketmq
     */
    private String type = "rocketmq";

    /**
     * RocketMQ配置
     */
    private RocketMQConfig rocketmq = new RocketMQConfig();

    /**
     * Kafka配置
     */
    private KafkaConfig kafka = new KafkaConfig();

    /**
     * RabbitMQ配置
     */
    private RabbitMQConfig rabbitmq = new RabbitMQConfig();

    /**
     * 是否启用消息轨迹追踪
     */
    private boolean traceEnabled = false;

    /**
     * 消息发送重试次数
     */
    private int sendRetryTimes = 3;

    /**
     * 消息发送超时时间（毫秒）
     */
    private long sendTimeoutMs = 3000;

    /**
     * 最大批量发送消息数量
     */
    private int maxBatchSize = 100;

    /**
     * 获取默认消息服务类型
     * @return 默认消息服务类型
     */
    public String getDefaultType() {
        return type;
    }

    /**
     * 根据类型获取对应的配置是否启用
     * @param type 消息服务类型
     * @return 是否启用
     */
    public boolean isServiceEnabled(String type) {
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

    /**
     * RocketMQ配置类
     */
    @Data
    public static class RocketMQConfig {
        /**
         * 是否启用RocketMQ
         */
        private boolean enabled = false;
        
        /**
         * Name Server地址
         */
        private String nameServer = "localhost:9876";

        /**
         * 生产者组
         */
        private String producerGroup = "DEFAULT_PRODUCER_GROUP";

        /**
         * 消费者组
         */
        private String consumerGroup = "DEFAULT_CONSUMER_GROUP";

        /**
         * 压缩策略：LZ4, SNAPPY, ZSTD
         */
        private String compressionStrategy = "LZ4";

        /**
         * 同步发送重试次数
         */
        private int retrySyncTimes = 3;

        /**
         * 异步发送重试次数
         */
        private int retryAsyncTimes = 3;

        /**
         * 每个Topic的队列数量
         */
        private int queueCountPerTopic = 4;

        /**
         * 消息保留时间（小时）
         */
        private int messageRetentionHours = 72;

        /**
         * 发送消息超时时间
         */
        private int sendMsgTimeout = 3000;

        /**
         * 压缩消息体阈值
         */
        private int compressMsgBodyOverHowmuch = 4096;

        /**
         * 最大消息大小
         */
        private int maxMessageSize = 4194304;

        /**
         * 是否启用消息轨迹
         */
        private boolean enableMsgTrace = true;

        /**
         * 消息轨迹存储类型
         */
        private String traceTopicName = "RMQ_SYS_TRACE_TOPIC";
    }

    /**
     * Kafka配置类
     */
    @Data
    public static class KafkaConfig {
        /**
         * 是否启用Kafka
         */
        private boolean enabled = false;

        /**
         * Bootstrap Servers地址
         */
        private String bootstrapServers = "localhost:9092";

        /**
         * 消费者组ID
         */
        private String consumerGroupId = "DEFAULT_CONSUMER_GROUP";

        /**
         * 生产者确认机制：acks
         */
        private String acks = "all";

        /**
         * 批量大小（字节）
         */
        private int batchSize = 16384;

        /**
         * 延迟时间（毫秒）
         */
        private int lingerMs = 1;

        /**
         * 缓冲区大小（字节）
         */
        private int bufferMemory = 33554432;

        /**
         * 生产者重试次数
         */
        private int retries = 3;

        /**
         * 消费者自动提交间隔
         */
        private int autoCommitIntervalMs = 1000;

        /**
         * 消费者会话超时时间
         */
        private int sessionTimeoutMs = 30000;

        /**
         * 消费者心跳间隔
         */
        private int heartbeatIntervalMs = 3000;

        /**
         * 消费者最大拉取记录数
         */
        private int maxPollRecords = 500;

        /**
         * 消费者最大拉取间隔
         */
        private int maxPollIntervalMs = 300000;
    }

    /**
     * RabbitMQ配置类
     */
    @Data
    public static class RabbitMQConfig {
        /**
         * 是否启用RabbitMQ
         */
        private boolean enabled = false;

        /**
         * 主机地址
         */
        private String host = "localhost";

        /**
         * 端口
         */
        private int port = 5672;

        /**
         * 用户名
         */
        private String username = "guest";

        /**
         * 密码
         */
        private String password = "guest";

        /**
         * 虚拟主机
         */
        private String virtualHost = "/";

        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 60000;

        /**
         * 通道RPC超时时间（毫秒）
         */
        private int channelRpcTimeout = 10000;

        /**
         * 请求心跳间隔（秒）
         */
        private int requestedHeartBeat = 60;

        /**
         * 连接恢复间隔（毫秒）
         */
        private int connectionRecoveryInterval = 10000;

        /**
         * 是否启用连接恢复
         */
        private boolean automaticRecoveryEnabled = true;

        /**
         * 是否启用拓扑恢复
         */
        private boolean topologyRecoveryEnabled = true;
    }
}
