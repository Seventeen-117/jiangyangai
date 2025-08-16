package com.jiangyang.messages.rabbitmq;

import com.alibaba.fastjson2.JSON;
import com.jiangyang.messages.service.MessageListener;
import com.jiangyang.messages.utils.MessageServiceException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RabbitMQ消费者管理类
 * 负责消费者的创建、订阅、取消订阅以及消费逻辑的管理
 * 实现并发消费及高级特性
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

    private final Map<String, Connection> consumerConnections = new ConcurrentHashMap<>();
    private final Map<String, Channel> consumerChannels = new ConcurrentHashMap<>();
    private final Map<String, String> consumerTags = new ConcurrentHashMap<>();
    private final Map<String, MessageListener<?>> listeners = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> consumerExecutors = new ConcurrentHashMap<>();
    private final AtomicLong backlogCount = new AtomicLong(0);
    private ConnectionFactory connectionFactory;

    @Autowired
    private RabbitConsumerProperties rabbitProps;

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
}
