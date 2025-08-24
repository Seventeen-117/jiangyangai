package com.jiangyang.messages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ配置类
 * 读取application-rabbitmq.yml中的配置
 * 所有配置都从配置文件读取，支持环境变量覆盖
 */
@Data
@Component
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQConfig {
    
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
