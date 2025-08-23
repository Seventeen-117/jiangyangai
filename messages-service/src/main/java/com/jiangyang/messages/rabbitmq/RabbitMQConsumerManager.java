package com.jiangyang.messages.rabbitmq;

import com.alibaba.fastjson2.JSON;
import com.jiangyang.messages.service.MessageListener;
import com.jiangyang.messages.utils.MessageServiceException;
import com.jiangyang.messages.entity.MessageConsumerConfig;
import com.jiangyang.messages.config.RabbitMQConfig;
import com.rabbitmq.client.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RabbitMQ消费者管理类
 * 负责消费者的创建、订阅、取消订阅以及消费逻辑的管理
 * 实现并发消费及高级特性，合并了原RabbitMQConsumer的功能
 */
@Slf4j
@Component
public class RabbitMQConsumerManager implements InitializingBean, DisposableBean {

    @Setter
    private String host;

    @Setter
    private int port;

    @Setter
    private String username;

    @Setter
    private String password;

    @Setter
    private String virtualHost;

    @Setter
    private int maxConsumeThreads;

    // 原有的消费者管理相关
    private final Map<String, Connection> consumerConnections = new ConcurrentHashMap<>();
    private final Map<String, Channel> consumerChannels = new ConcurrentHashMap<>();
    private final Map<String, String> consumerTags = new ConcurrentHashMap<>();
    private final Map<String, MessageListener<?>> listeners = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> consumerExecutors = new ConcurrentHashMap<>();
    private final AtomicLong backlogCount = new AtomicLong(0);
    
    // 新增：从RabbitMQConsumer合并的功能
    private final Map<String, ConfigBasedConsumer> configBasedConsumers = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    
    private ConnectionFactory connectionFactory;

    @Autowired
    private RabbitConsumerProperties rabbitProps;
    
    @Autowired(required = false)
    private RabbitMQConfig rabbitMQConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 首选自定义前缀配置，从 Nacos/本地 yml 绑定
        this.host = coalesce(rabbitProps.getHost(), System.getProperty("spring.rabbitmq.host"));
        this.port = coalesce(rabbitProps.getPort(), Integer.getInteger("spring.rabbitmq.port"), 5672);
        this.username = coalesce(rabbitProps.getUsername(), System.getProperty("spring.rabbitmq.username"));
        this.password = coalesce(rabbitProps.getPassword(), System.getProperty("spring.rabbitmq.password"));
        this.virtualHost = coalesce(rabbitProps.getVirtualHost(), System.getProperty("spring.rabbitmq.virtual-host"), "/");
        this.maxConsumeThreads = coalesce(rabbitProps.getMaxConsumeThreads(), 64);

        // 初始化connection factory
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        log.info("RabbitMQConsumerManager initialized with host={}, port={}, vhost={}", host, port, virtualHost);
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private static Integer coalesce(Integer... values) {
        for (Integer v : values) {
            if (v != null) return v;
        }
        return null;
    }

    @Override
    public void destroy() throws Exception {
        // 关闭所有消费者channel和connection
        for (Channel channel : consumerChannels.values()) {
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (Exception e) {
                    log.error("Failed to close RabbitMQ consumer channel", e);
                }
            }
        }
        consumerChannels.clear();
        consumerTags.clear();
        listeners.clear();
        
        for (Connection connection : consumerConnections.values()) {
            if (connection != null && connection.isOpen()) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.error("Failed to close RabbitMQ consumer connection", e);
                }
            }
        }
        consumerConnections.clear();
        
        // 关闭所有线程池
        for (ExecutorService executor : consumerExecutors.values()) {
            if (executor != null) {
                executor.shutdown();
            }
        }
        consumerExecutors.clear();
        log.info("RabbitMQConsumerManager shutdown");
    }

    /**
     * 订阅主题
     * @param topic 主题（队列名）
     * @param consumerGroup 消费者组
     * @param listener 消息监听器
     * @param <T> 消息类型
     * @return 是否订阅成功
     */
    public <T> boolean subscribe(String topic, String consumerGroup, MessageListener<T> listener) {
        String consumerKey = generateConsumerKey(topic, consumerGroup);
        if (consumerConnections.containsKey(consumerKey)) {
            log.warn("Consumer already subscribed for topic: {}, group: {}", topic, consumerGroup);
            return false;
        }

        try {
            // 创建新的连接和通道
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            // 声明队列
            channel.queueDeclare(topic, true, false, false, null);
            
            // 为每个消费者创建独立的线程池，用于处理IO密集型操作
            ExecutorService executor = Executors.newFixedThreadPool(maxConsumeThreads);
            consumerExecutors.put(consumerKey, executor);
            
            // 设置消费者
            String consumerTag = channel.basicConsume(topic, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    // 流量控制
                    if (backlogCount.get() > 1000) {
                        log.warn("Consumer backlog too high, rejecting message. Backlog: {}", backlogCount.get());
                        channel.basicNack(envelope.getDeliveryTag(), false, true);
                        return;
                    }
                    
                    backlogCount.incrementAndGet();
                    // 提交到线程池处理，防止阻塞消费线程
                    executor.execute(() -> {
                        try {
                            processMessage(body, listener);
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (Exception e) {
                            log.error("Error processing message, deliveryTag: {}", envelope.getDeliveryTag(), e);
                            try {
                                channel.basicNack(envelope.getDeliveryTag(), false, true);
                            } catch (IOException ioe) {
                                log.error("Failed to nack message, deliveryTag: {}", envelope.getDeliveryTag(), ioe);
                            }
                        } finally {
                            backlogCount.decrementAndGet();
                        }
                    });
                }
            });
            
            consumerConnections.put(consumerKey, connection);
            consumerChannels.put(consumerKey, channel);
            consumerTags.put(consumerKey, consumerTag);
            listeners.put(consumerKey, listener);
            log.info("Subscribed to topic: {}, group: {}", topic, consumerGroup);
            return true;
        } catch (Exception e) {
            log.error("Failed to subscribe to topic: {}, group: {}", topic, consumerGroup, e);
            return false;
        }
    }

    /**
     * 取消订阅
     * @param topic 主题（队列名）
     * @param consumerGroup 消费者组
     * @return 是否取消成功
     */
    public boolean unsubscribe(String topic, String consumerGroup) {
        String consumerKey = generateConsumerKey(topic, consumerGroup);
        Channel channel = consumerChannels.remove(consumerKey);
        Connection connection = consumerConnections.remove(consumerKey);
        String consumerTag = consumerTags.remove(consumerKey);
        if (channel != null && connection != null) {
            try {
                if (consumerTag != null) {
                    channel.basicCancel(consumerTag);
                }
                if (channel.isOpen()) {
                    channel.close();
                }
                if (connection.isOpen()) {
                    connection.close();
                }
                listeners.remove(consumerKey);
                
                ExecutorService executor = consumerExecutors.remove(consumerKey);
                if (executor != null) {
                    executor.shutdown();
                }
                log.info("Unsubscribed from topic: {}, group: {}", topic, consumerGroup);
                return true;
            } catch (Exception e) {
                log.error("Failed to unsubscribe from topic: {}, group: {}", topic, consumerGroup, e);
                return false;
            }
        }
        log.warn("No subscription found for topic: {}, group: {}", topic, consumerGroup);
        return false;
    }

    /**
     * 处理单条消息
     */
    private <T> void processMessage(byte[] body, MessageListener<T> listener) {
        try {
            String messageBody = new String(body, StandardCharsets.UTF_8);
            // 使用泛型类型进行反序列化
            Class<T> messageType = getMessageType(listener);
            T message = JSON.parseObject(messageBody, messageType);
            log.debug("Processing message");
            listener.onMessage(message);
            log.debug("Message processed successfully");
        } catch (Exception e) {
            log.error("Failed to process message", e);
            throw new MessageServiceException("Failed to process message", e);
        }
    }
    
    /**
     * 获取消息类型
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> getMessageType(MessageListener<T> listener) {
        try {
            // 尝试从监听器获取类型信息
            return (Class<T>) listener.getClass().getMethod("getMessageType").getReturnType();
        } catch (Exception e) {
            // 如果无法获取，返回Object类型
            return (Class<T>) Object.class;
        }
    }

    /**
     * 生成消费者唯一标识
     */
    private String generateConsumerKey(String topic, String consumerGroup) {
        return topic + "_" + consumerGroup;
    }

    /**
     * 获取当前积压消息数量
     */
    public long getBacklogCount() {
        return backlogCount.get();
    }

    // ==================== 从RabbitMQConsumer合并的功能 ====================

    /**
     * 创建基于配置的消费者
     * @param config 消费者配置
     * @return ConfigBasedConsumer实例
     */
    public ConfigBasedConsumer createConfigBasedConsumer(MessageConsumerConfig config) {
        String consumerKey = config.getServiceName() + "_" + config.getTopic();
        ConfigBasedConsumer consumer = new ConfigBasedConsumer(config, rabbitMQConfig);
        configBasedConsumers.put(consumerKey, consumer);
        return consumer;
    }

    /**
     * 移除基于配置的消费者
     * @param serviceName 服务名
     * @param topic 主题
     */
    public void removeConfigBasedConsumer(String serviceName, String topic) {
        String consumerKey = serviceName + "_" + topic;
        ConfigBasedConsumer consumer = configBasedConsumers.remove(consumerKey);
        if (consumer != null) {
            consumer.shutdown();
        }
    }

    /**
     * 获取重试次数
     */
    public int getRetryCount(String messageKey) {
        AtomicInteger retryCount = retryCountMap.get(messageKey);
        return retryCount != null ? retryCount.get() : 0;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount(String messageKey) {
        retryCountMap.computeIfAbsent(messageKey, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 重置重试次数
     */
    public void resetRetryCount(String messageKey) {
        retryCountMap.remove(messageKey);
    }

    /**
     * 获取队列消息数量
     */
    public long getQueueMessageCount(String queueName) {
        try {
            // 使用第一个可用的channel来查询
            for (Channel channel : consumerChannels.values()) {
                if (channel != null && channel.isOpen()) {
                    return channel.messageCount(queueName);
                }
            }
            log.warn("无可用channel查询队列消息数量: {}", queueName);
            return -1;
        } catch (IOException e) {
            log.error("获取队列消息数量失败: queueName={}, error={}", queueName, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 清空队列
     */
    public boolean purgeQueue(String queueName) {
        try {
            // 使用第一个可用的channel来清空队列
            for (Channel channel : consumerChannels.values()) {
                if (channel != null && channel.isOpen()) {
                    channel.queuePurge(queueName);
                    log.info("队列清空成功: queueName={}", queueName);
                    return true;
                }
            }
            log.warn("无可用channel清空队列: {}", queueName);
            return false;
        } catch (IOException e) {
            log.error("队列清空失败: queueName={}, error={}", queueName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 基于配置的消费者内部类
     * 整合了原RabbitMQConsumer的功能
     */
    public static class ConfigBasedConsumer {
        private final MessageConsumerConfig config;
        private final RabbitMQConfig rabbitMQConfig;
        
        private Connection connection;
        private Channel channel;
        private volatile boolean isRunning = false;
        private final AtomicBoolean stopFlag;
        private String consumerTag;

        public ConfigBasedConsumer(MessageConsumerConfig config, RabbitMQConfig rabbitMQConfig) {
            this.config = config;
            this.rabbitMQConfig = rabbitMQConfig;
            this.stopFlag = new AtomicBoolean(false);
        }

        /**
         * 启动推模式消费
         */
        public void startPushMode() throws Exception {
            if (isRunning) {
                return;
            }

            try {
                // 建立连接
                establishConnection();
                
                // 声明交换机和队列
                declareExchangeAndQueue();
                
                // 设置QoS（预取数量）
                if (config.getBatchSize() != null && config.getBatchSize() > 0) {
                    channel.basicQos(config.getBatchSize());
                } else {
                    channel.basicQos(rabbitMQConfig.getConsumer().getPrefetchCount());
                }
                
                // 定义消息处理回调
                DeliverCallback deliverCallback = (tag, delivery) -> {
                    try {
                        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                        log.info("收到消息: serviceName={}, queue={}, message={}", 
                                config.getServiceName(), config.getTopic(), message);
                        
                        // 处理消息
                        processMessage(message);
                        
                        // 根据配置决定是否手动确认
                        if (config.getConsumeMode() != null && "MANUAL".equals(config.getConsumeMode())) {
                            // 手动确认
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                            log.debug("消息手动确认成功: deliveryTag={}", delivery.getEnvelope().getDeliveryTag());
                        }
                        // 如果是自动确认模式，RabbitMQ会自动确认
                        
                    } catch (Exception e) {
                        log.error("处理消息失败: serviceName={}, error={}", config.getServiceName(), e.getMessage(), e);
                        
                        // 处理失败时的策略
                        handleFailedMessage(delivery, e);
                    }
                };
                
                // 定义取消回调
                CancelCallback cancelCallback = tag -> {
                    log.warn("消费者被取消: serviceName={}, consumerTag={}", config.getServiceName(), tag);
                };
                
                // 启动推模式消费
                consumerTag = channel.basicConsume(
                    config.getTopic(), 
                    !("MANUAL".equals(config.getConsumeMode())), // autoAck: 手动确认时为false
                    deliverCallback, 
                    cancelCallback
                );
                
                log.info("推模式消费启动成功: serviceName={}, consumerTag={}", config.getServiceName(), consumerTag);
                isRunning = true;
                
            } catch (Exception e) {
                log.error("启动推模式消费失败: serviceName={}, error={}", config.getServiceName(), e.getMessage(), e);
                throw e;
            }
        }

        /**
         * 启动拉模式消费
         */
        public void startPullMode() throws Exception {
            if (isRunning) {
                return;
            }

            try {
                // 建立连接
                establishConnection();
                
                // 声明交换机和队列
                declareExchangeAndQueue();
                
                log.info("拉模式消费启动成功: serviceName={}", config.getServiceName());
                isRunning = true;
                
                // 拉模式需要手动循环拉取消息
                while (!stopFlag.get() && isRunning) {
                    try {
                        // 拉取单条消息
                        GetResponse response = channel.basicGet(config.getTopic(), false);
                        
                        if (response != null) {
                            String message = new String(response.getBody(), StandardCharsets.UTF_8);
                            log.info("拉取到消息: serviceName={}, queue={}, message={}", 
                                    config.getServiceName(), config.getTopic(), message);
                            
                            try {
                                // 处理消息
                                processMessage(message);
                                
                                // 手动确认
                                channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                                log.debug("消息手动确认成功: deliveryTag={}", response.getEnvelope().getDeliveryTag());
                                
                            } catch (Exception e) {
                                log.error("处理消息失败: serviceName={}, error={}", config.getServiceName(), e.getMessage(), e);
                                
                                // 处理失败时拒绝消息
                                channel.basicNack(response.getEnvelope().getDeliveryTag(), false, true);
                                log.warn("消息处理失败，已拒绝并重新入队: deliveryTag={}", response.getEnvelope().getDeliveryTag());
                            }
                        } else {
                            // 没有消息时等待一段时间
                            Thread.sleep(rabbitMQConfig.getConsumer().getConsumeInterval());
                        }
                        
                    } catch (Exception e) {
                        log.error("拉模式消费异常: serviceName={}, error={}", config.getServiceName(), e.getMessage(), e);
                        Thread.sleep(5000); // 发生错误时等待5秒
                    }
                }
                
            } catch (Exception e) {
                log.error("启动拉模式消费失败: serviceName={}, error={}", config.getServiceName(), e.getMessage(), e);
                throw e;
            }
        }

        /**
         * 建立RabbitMQ连接
         */
        private void establishConnection() throws Exception {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(rabbitMQConfig.getHost());
            factory.setPort(rabbitMQConfig.getPort());
            factory.setUsername(rabbitMQConfig.getUsername());
            factory.setPassword(rabbitMQConfig.getPassword());
            factory.setVirtualHost(rabbitMQConfig.getVirtualHost());
            factory.setConnectionTimeout(rabbitMQConfig.getConnection().getTimeout());
            factory.setRequestedHeartbeat(rabbitMQConfig.getConnection().getHeartbeat());
            factory.setAutomaticRecoveryEnabled(rabbitMQConfig.getConnection().isAutomaticRecovery());
            factory.setNetworkRecoveryInterval(rabbitMQConfig.getConnection().getNetworkRecoveryInterval());

            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.confirmSelect(); // 启用发布确认

            log.info("RabbitMQ连接建立成功: serviceName={}, host={}, port={}, virtualHost={}",
                    config.getServiceName(), rabbitMQConfig.getHost(), rabbitMQConfig.getPort(), rabbitMQConfig.getVirtualHost());
        }

        /**
         * 声明交换机和队列
         */
        private void declareExchangeAndQueue() throws Exception {
            // 声明交换机（如果配置了）
            if (config.getExchange() != null && !config.getExchange().isEmpty()) {
                channel.exchangeDeclare(
                    config.getExchange(), 
                    rabbitMQConfig.getExchange().getDefaultType(), 
                    rabbitMQConfig.getExchange().isDurable(), 
                    rabbitMQConfig.getExchange().isAutoDelete(), 
                    null
                );
                log.info("交换机声明成功: exchange={}", config.getExchange());
            }
            
            // 声明队列
            Map<String, Object> queueArgs = new HashMap<>();
            
            // 配置死信队列（如果配置了）
            if (rabbitMQConfig.getDeadLetter().isEnabled() && config.getMaxRetryTimes() != null && config.getMaxRetryTimes() > 0) {
                queueArgs.put("x-dead-letter-exchange", rabbitMQConfig.getDeadLetter().getExchangePrefix() + config.getExchange());
                queueArgs.put("x-dead-letter-routing-key", rabbitMQConfig.getDeadLetter().getRoutingKeyPrefix() + config.getRoutingKey());
            }
            
            // 配置消息TTL（如果配置了）
            if (config.getTimeout() != null && config.getTimeout() > 0) {
                queueArgs.put("x-message-ttl", config.getTimeout());
            }
            
            // 配置优先级（如果配置了）
            if (rabbitMQConfig.getPriority().isEnabled() && 
                config.getConsumeOrder() != null && "PRIORITY".equals(config.getConsumeOrder())) {
                queueArgs.put("x-max-priority", rabbitMQConfig.getPriority().getMaxPriority());
            }
            
            // 配置队列长度限制
            if (rabbitMQConfig.getQueue().getMaxLength() > 0) {
                queueArgs.put("x-max-length", rabbitMQConfig.getQueue().getMaxLength());
            }
            
            channel.queueDeclare(
                config.getTopic(), 
                rabbitMQConfig.getQueue().isDurable(), 
                rabbitMQConfig.getQueue().isExclusive(), 
                rabbitMQConfig.getQueue().isAutoDelete(), 
                queueArgs
            );
            log.info("队列声明成功: queue={}, args={}", config.getTopic(), queueArgs);
            
            // 绑定队列到交换机（如果配置了）
            if (config.getExchange() != null && !config.getExchange().isEmpty()) {
                String routingKey = config.getRoutingKey() != null ? config.getRoutingKey() : "";
                channel.queueBind(config.getTopic(), config.getExchange(), routingKey);
                log.info("队列绑定成功: queue={}, exchange={}, routingKey={}", 
                        config.getTopic(), config.getExchange(), routingKey);
            }
        }

        /**
         * 处理消息
         */
        private void processMessage(String message) {
            try {
                log.info("处理RabbitMQ消息: serviceName={}, message={}", config.getServiceName(), message);
                
                // TODO: 根据业务需求实现具体的消息处理逻辑
                // 这里可以调用业务服务处理消息
                
                // 模拟消息处理
                Thread.sleep(100);
                
                log.info("RabbitMQ消息处理完成: serviceName={}", config.getServiceName());
                
            } catch (Exception e) {
                log.error("处理RabbitMQ消息失败: serviceName={}, error={}", config.getServiceName(), e.getMessage(), e);
                throw new RuntimeException("消息处理失败", e);
            }
        }

        /**
         * 处理失败的消息
         */
        private void handleFailedMessage(Delivery delivery, Exception error) {
            try {
                log.warn("处理失败的消息: serviceName={}, deliveryTag={}, error={}", 
                        config.getServiceName(), delivery.getEnvelope().getDeliveryTag(), error.getMessage());
                
                // 根据配置决定处理策略
                if (config.getMaxRetryTimes() != null && config.getMaxRetryTimes() > 0) {
                    // 有重试次数限制，拒绝消息并重新入队
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                    log.info("消息处理失败，已拒绝并重新入队: deliveryTag={}", delivery.getEnvelope().getDeliveryTag());
                } else {
                    // 无重试次数限制，拒绝消息并进入死信队列
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    log.info("消息处理失败，已拒绝并进入死信队列: deliveryTag={}", delivery.getEnvelope().getDeliveryTag());
                }
                
            } catch (Exception e) {
                log.error("处理失败消息时发生异常: serviceName={}, error={}", config.getServiceName(), e.getMessage(), e);
            }
        }

        /**
         * 检查连接状态
         */
        public boolean isAvailable() {
            try {
                return connection != null && connection.isOpen() && channel != null && channel.isOpen();
            } catch (Exception e) {
                log.error("检查RabbitMQ服务状态失败: {}", e.getMessage(), e);
                return false;
            }
        }

        /**
         * 关闭消费者
         */
        public void shutdown() {
            try {
                isRunning = false;
                stopFlag.set(true);
                
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                
                if (connection != null && connection.isOpen()) {
                    connection.close();
                }
                
                log.info("RabbitMQ消费者已关闭: serviceName={}", config.getServiceName());
                
            } catch (Exception e) {
                log.error("关闭RabbitMQ消费者失败: serviceName={}, error={}", config.getServiceName(), e.getMessage(), e);
            }
        }

        /**
         * 检查是否正在运行
         */
        public boolean isRunning() {
            return isRunning;
        }

        /**
         * 获取消费者标签
         */
        public String getConsumerTag() {
            return consumerTag;
        }
    }
}
