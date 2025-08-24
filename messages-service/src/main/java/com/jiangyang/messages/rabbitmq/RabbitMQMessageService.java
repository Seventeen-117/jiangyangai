package com.jiangyang.messages.rabbitmq;

import com.jiangyang.messages.service.MessageService;
import com.jiangyang.messages.utils.MessageServiceType;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * RabbitMQ消息服务实现类
 * 提供RabbitMQ消息中间件的具体实现
 */
@Slf4j
public class RabbitMQMessageService implements MessageService {

    private String host;

    private int port;

    private String username;

    private String password;

    private String virtualHost;

    private int connectionTimeout;

    private int requestedHeartbeat;

    private boolean automaticRecovery;

    private Connection connection;
    private Channel channel;
    private final ConcurrentHashMap<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> messageDedupMap = new ConcurrentHashMap<>();

    public void init() {
        try {
            // 检查必要的配置属性
            if (host == null || host.trim().isEmpty()) {
                log.warn("RabbitMQ host is not configured, skipping initialization");
                return;
            }
            
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username != null ? username : "guest");
            factory.setPassword(password != null ? password : "guest");
            factory.setVirtualHost(virtualHost != null ? virtualHost : "/");
            factory.setConnectionTimeout(connectionTimeout);
            factory.setRequestedHeartbeat(requestedHeartbeat);
            factory.setAutomaticRecoveryEnabled(automaticRecovery);

            connection = factory.newConnection();
            channel = connection.createChannel();
            
            // 启用发布确认
            channel.confirmSelect();
            
            log.info("RabbitMQ连接建立成功: host={}, port={}, virtualHost={}", host, port, virtualHost);
        } catch (Exception e) {
            log.error("RabbitMQ连接建立失败: {}", e.getMessage(), e);
            throw new RuntimeException("RabbitMQ连接建立失败", e);
        }
    }

    @Override
    public boolean sendMessage(String topic, String content) {
        return sendMessage(topic, null, null, content);
    }

    @Override
    public boolean sendMessage(String topic, String tag, String content) {
        return sendMessage(topic, tag, null, content);
    }

    @Override
    public boolean sendMessage(String topic, String tag, String key, String content) {
        try {
            // 消息去重检查
            String dedupKey = generateDedupKey(topic, tag, key, content);
            if (isDuplicateMessage(dedupKey)) {
                log.warn("检测到重复消息，跳过发送: dedupKey={}", dedupKey);
                return true;
            }

            // 确保队列存在
            channel.queueDeclare(topic, true, false, false, null);
            
            // 构建消息属性
            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
            if (tag != null) {
                propsBuilder.type(tag);
            }
            if (key != null) {
                propsBuilder.messageId(key);
            }
            propsBuilder.deliveryMode(2); // 持久化消息
            propsBuilder.timestamp(Date.from(Instant.now()));
            
            AMQP.BasicProperties properties = propsBuilder.build();
            
            // 发送消息
            channel.basicPublish("", topic, properties, content.getBytes(StandardCharsets.UTF_8));
            
            // 等待发布确认
            if (channel.waitForConfirms(5000)) {
                log.info("RabbitMQ消息发送成功: topic={}, tag={}, key={}", topic, tag, key);
                
                // 记录消息去重
                recordMessageDedup(dedupKey);
                return true;
            } else {
                log.error("RabbitMQ消息发送确认超时: topic={}, tag={}, key={}", topic, tag, key);
                return false;
            }
            
        } catch (Exception e) {
            log.error("RabbitMQ消息发送异常: topic={}, tag={}, key={}, error={}", 
                    topic, tag, key, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendDelayMessage(String topic, String content, int delayLevel) {
        try {
            // RabbitMQ通过死信队列实现延迟消息
            String delayQueueName = topic + "-delay-" + delayLevel;
            String exchangeName = topic + "-delay-exchange";
            
            // 声明延迟交换机
            channel.exchangeDeclare(exchangeName, "direct", true);
            
            // 声明延迟队列，设置TTL
            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-dead-letter-exchange", "");
            queueArgs.put("x-dead-letter-routing-key", topic);
            queueArgs.put("x-message-ttl", delayLevel * 1000); // 转换为毫秒
            
            channel.queueDeclare(delayQueueName, true, false, false, queueArgs);
            channel.queueBind(delayQueueName, exchangeName, delayQueueName);
            
            // 确保目标队列存在
            channel.queueDeclare(topic, true, false, false, null);
            
            // 发送延迟消息
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .timestamp(Date.from(Instant.now()))
                    .build();
            
            channel.basicPublish(exchangeName, delayQueueName, properties, content.getBytes(StandardCharsets.UTF_8));
            
            if (channel.waitForConfirms(5000)) {
                log.info("RabbitMQ延迟消息发送成功: topic={}, delayLevel={}, delayQueue={}", 
                        topic, delayLevel, delayQueueName);
                return true;
            } else {
                log.error("RabbitMQ延迟消息发送确认超时: topic={}, delayLevel={}", topic, delayLevel);
                return false;
            }
            
        } catch (Exception e) {
            log.error("RabbitMQ延迟消息发送异常: topic={}, delayLevel={}, error={}", 
                    topic, delayLevel, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendOrderedMessage(String topic, String content, String hashKey) {
        try {
            // RabbitMQ通过单队列实现顺序消息
            String orderedQueueName = topic + "-ordered";
            
            // 声明顺序队列
            channel.queueDeclare(orderedQueueName, true, false, false, null);
            
            // 构建消息属性
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .messageId(hashKey)
                    .timestamp(Date.from(Instant.now()))
                    .build();
            
            // 发送顺序消息
            channel.basicPublish("", orderedQueueName, properties, content.getBytes(StandardCharsets.UTF_8));
            
            if (channel.waitForConfirms(5000)) {
                log.info("RabbitMQ顺序消息发送成功: topic={}, hashKey={}, orderedQueue={}", 
                        topic, hashKey, orderedQueueName);
                return true;
            } else {
                log.error("RabbitMQ顺序消息发送确认超时: topic={}, hashKey={}", topic, hashKey);
                return false;
            }
            
        } catch (Exception e) {
            log.error("RabbitMQ顺序消息发送异常: topic={}, hashKey={}, error={}", 
                    topic, hashKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendBatchMessages(String topic, List<String> messages) {
        try {
            if (messages == null || messages.isEmpty()) {
                log.warn("批量消息列表为空，跳过发送");
                return true;
            }

            // 确保队列存在
            channel.queueDeclare(topic, true, false, false, null);
            
            // 批量发送消息
            for (String content : messages) {
                AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                        .deliveryMode(2)
                        .timestamp(Date.from(Instant.now()))
                        .build();
                
                channel.basicPublish("", topic, properties, content.getBytes(StandardCharsets.UTF_8));
            }
            
            // 等待所有消息确认
            if (channel.waitForConfirms(10000)) {
                log.info("RabbitMQ批量消息发送成功: topic={}, messageCount={}", topic, messages.size());
                return true;
            } else {
                log.error("RabbitMQ批量消息发送确认超时: topic={}, messageCount={}", topic, messages.size());
                return false;
            }
            
        } catch (Exception e) {
            log.error("RabbitMQ批量消息发送异常: topic={}, messageCount={}, error={}", 
                    topic, messages.size(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public MessageServiceType getServiceType() {
        return MessageServiceType.RABBITMQ;
    }

    @Override
    public boolean isAvailable() {
        try {
            return connection != null && connection.isOpen() && channel != null && channel.isOpen();
        } catch (Exception e) {
            log.error("检查RabbitMQ服务状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @PreDestroy
    public void shutdown() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            log.info("RabbitMQ消息服务已关闭");
        } catch (IOException | TimeoutException e) {
            log.error("关闭RabbitMQ消息服务失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步发送消息
     */
    public void sendMessageAsync(String topic, String tag, String key, String content) {
        try {
            // 确保队列存在
            channel.queueDeclare(topic, true, false, false, null);
            
            // 构建消息属性
            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
            if (tag != null) {
                propsBuilder.type(tag);
            }
            if (key != null) {
                propsBuilder.messageId(key);
            }
            propsBuilder.deliveryMode(2);
            propsBuilder.timestamp(Date.from(Instant.now()));
            
            AMQP.BasicProperties properties = propsBuilder.build();
            
            // 异步发送消息
            channel.basicPublish("", topic, properties, content.getBytes(StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            log.error("RabbitMQ异步消息发送异常: topic={}, tag={}, key={}, error={}", 
                    topic, tag, key, e.getMessage(), e);
        }
    }

    /**
     * 生成消息去重键
     */
    private String generateDedupKey(String topic, String tag, String key, String content) {
        return String.format("%s:%s:%s:%s", topic, tag, key, content);
    }

    /**
     * 检查是否为重复消息
     */
    private boolean isDuplicateMessage(String dedupKey) {
        Long timestamp = messageDedupMap.get(dedupKey);
        if (timestamp == null) {
            return false;
        }
        
        // 5分钟内的消息认为是重复消息
        long currentTime = System.currentTimeMillis();
        return (currentTime - timestamp) < 300000;
    }

    /**
     * 记录消息去重
     */
    private void recordMessageDedup(String dedupKey) {
        messageDedupMap.put(dedupKey, System.currentTimeMillis());
        
        // 清理过期的去重记录（超过5分钟）
        long currentTime = System.currentTimeMillis();
        messageDedupMap.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 300000);
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
            return channel.messageCount(queueName);
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
            channel.queuePurge(queueName);
            log.info("队列清空成功: queueName={}", queueName);
            return true;
        } catch (IOException e) {
            log.error("队列清空失败: queueName={}, error={}", queueName, e.getMessage(), e);
            return false;
        }
    }

    // ==================== 配置方法 ====================
    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setVirtualHost(String virtualHost) { this.virtualHost = virtualHost; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public void setRequestedHeartbeat(int requestedHeartbeat) { this.requestedHeartbeat = requestedHeartbeat; }
    public void setAutomaticRecovery(boolean automaticRecovery) { this.automaticRecovery = automaticRecovery; }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getVirtualHost() { return virtualHost; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getRequestedHeartbeat() { return requestedHeartbeat; }
    public boolean isAutomaticRecovery() { return automaticRecovery; }

    @Override
    public boolean sendTransactionMessage(String topic, String tag, String messageBody, 
                                       String transactionId, String businessKey, int timeout) {
        try {
            log.info("发送RabbitMQ事务消息: topic={}, tag={}, transactionId={}, businessKey={}, timeout={}", 
                    topic, tag, transactionId, businessKey, timeout);
            
            if (channel == null || !channel.isOpen()) {
                log.warn("RabbitMQ channel not available, skip transaction message send");
                return false;
            }
            
            // 创建事务消息属性
            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
            propsBuilder.deliveryMode(2); // 持久化消息
            propsBuilder.priority(0); // 默认优先级
            
            // 设置事务相关属性
            Map<String, Object> headers = new HashMap<>();
            headers.put("transactionId", transactionId);
            headers.put("businessKey", businessKey != null ? businessKey : "");
            headers.put("messageType", "TRANSACTION");
            headers.put("timestamp", System.currentTimeMillis());
            propsBuilder.headers(headers);
            
            AMQP.BasicProperties props = propsBuilder.build();
            
            // 发送事务消息
            // 注意：RabbitMQ的事务消息需要启用事务模式
            // 目前先使用普通发送，后续可以扩展为真正的事务消息
            channel.basicPublish("", topic, props, messageBody.getBytes(StandardCharsets.UTF_8));
            
            // 等待发布确认
            if (channel.waitForConfirms(timeout)) {
                log.info("RabbitMQ事务消息发送成功: topic={}, tag={}, transactionId={}", 
                        topic, tag, transactionId);
                return true;
            } else {
                log.error("RabbitMQ事务消息发送失败: topic={}, tag={}, transactionId={}", 
                        topic, tag, transactionId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("RabbitMQ事务消息发送异常: topic={}, tag={}, transactionId={}, error={}", 
                    topic, tag, transactionId, e.getMessage(), e);
            return false;
        }
    }
}
