package com.jiangyang.messages.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ配置类
 * 读取application-rabbitmq.yml中的配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQConfig {
    
    /**
     * 基础连接配置
     */
    private String host = "localhost";
    private int port = 5672;
    private String username = "guest";
    private String password = "guest";
    private String virtualHost = "/";
    
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
        private int timeout = 30000;
        private int heartbeat = 60;
        private boolean automaticRecovery = true;
        private int networkRecoveryInterval = 5000;
    }
    
    @Data
    public static class Consumer {
        private String defaultMode = "PUSH";
        private String defaultAckMode = "MANUAL";
        private int prefetchCount = 1;
        private int batchSize = 100;
        private int consumeInterval = 1000;
        private int maxRetryTimes = 3;
        private int timeout = 30000;
    }
    
    @Data
    public static class Producer {
        private String confirmMode = "CORRELATED";
        private String returnMode = "BASIC";
        private int batchSize = 100;
        private int timeout = 5000;
    }
    
    @Data
    public static class Queue {
        private String defaultType = "CLASSIC";
        private boolean durable = true;
        private boolean exclusive = false;
        private boolean autoDelete = false;
        private int maxPriority = 10;
        private int maxLength = 10000;
        private long messageTtl = 86400000;
    }
    
    @Data
    public static class Exchange {
        private String defaultType = "DIRECT";
        private boolean durable = true;
        private boolean autoDelete = false;
        private boolean internal = false;
    }
    
    @Data
    public static class DeadLetter {
        private String exchangePrefix = "dlx.";
        private String routingKeyPrefix = "dlq.";
        private String queuePrefix = "dlq.";
        private boolean enabled = true;
    }
    
    @Data
    public static class Delay {
        private boolean enabled = true;
        private String exchangePrefix = "delay.";
        private String queuePrefix = "delay.";
        private long defaultTtl = 30000;
    }
    
    @Data
    public static class Priority {
        private boolean enabled = true;
        private int maxPriority = 10;
        private int defaultPriority = 5;
    }
    
    @Data
    public static class Cluster {
        private boolean enabled = false;
        private String[] nodes = {};
        private String loadBalance = "ROUND_ROBIN";
    }
    
    @Data
    public static class Monitoring {
        private boolean enabled = true;
        private int interval = 30;
        private boolean verboseLogging = false;
    }
}
