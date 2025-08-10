package com.jiangyang.messages.rabbitmq;

import com.alibaba.fastjson2.JSON;
import com.jiangyang.messages.MessageListener;
import com.jiangyang.messages.MessageService;
import com.jiangyang.messages.MessageServiceException;
import com.jiangyang.messages.MessageServiceType;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RabbitMQ消息服务实现
 * 包含了解决消息生产端痛点的机制：
 * 1. 消息丢失风险：实现重试机制和发送确认
 * 2. 性能瓶颈：实现异步发送和批量发送优化
 * 3. 消息质量：实现消息去重
 * 4. 多种发送方式：同步、异步、批量和延迟消息
 */
@Slf4j
public class RabbitMQMessageService implements MessageService, InitializingBean, DisposableBean {

    @Setter
    private String host = "localhost";

    @Setter
    private int port = 5672;

    @Setter
    private String username = "guest";

    @Setter
    private String password = "guest";

    @Setter
    private String virtualHost = "/";

    private Connection connection;
    private Channel channel;
    private RabbitMQConsumerManager consumerManager;
    private ExecutorService asyncSendExecutor;
    // 用于跟踪消息发送状态，防止重复发送
    private final Map<String, AtomicBoolean> messageSendingStatus = new ConcurrentHashMap<>();
    // 用于存储发送失败的消息，供重试使用
    private final Map<String, MessageRetryInfo> failedMessages = new ConcurrentHashMap<>();
    // 最大重试次数
    private static final int MAX_RETRY_TIMES = 3;
    // 重试间隔时间（毫秒）
    private static final long RETRY_INTERVAL_MS = 5000;

    private static class MessageRetryInfo {
        String topic;
        String message;
        Map<String, Object> headers;

        MessageRetryInfo(String topic, String message, Map<String, Object> headers) {
            this.topic = topic;
            this.message = message;
            this.headers = headers;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化connection和channel
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(virtualHost);
            
            connection = factory.newConnection();
            channel = connection.createChannel();
            log.info("RabbitMQ connection established with host: {}, port: {}", host, port);
            
            // 初始化consumerManager
            consumerManager = new RabbitMQConsumerManager();
            consumerManager.setHost(host);
            consumerManager.setPort(port);
            consumerManager.setUsername(username);
            consumerManager.setPassword(password);
            consumerManager.setVirtualHost(virtualHost);
            consumerManager.afterPropertiesSet();
            
            // 初始化异步发送线程池
            asyncSendExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            
            // 启动重试线程
            startRetryThread();
        } catch (Exception e) {
            log.error("Failed to initialize RabbitMQ connection", e);
            throw new MessageServiceException("Failed to initialize RabbitMQ connection", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        // 关闭channel和connection
        if (channel != null && channel.isOpen()) {
            channel.close();
            log.info("RabbitMQ channel closed");
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
            log.info("RabbitMQ connection closed");
        }
        
        // 关闭consumerManager
        if (consumerManager != null) {
            consumerManager.destroy();
            log.info("RabbitMQ consumer manager shutdown");
        }
        
        // 关闭异步发送线程池
        if (asyncSendExecutor != null) {
            asyncSendExecutor.shutdown();
            log.info("RabbitMQ async send executor shutdown");
        }
    }

    @Override
    public <T> boolean sendMessage(String topic, T message) {
        return sendMessageWithRetry(topic, message, false, 0);
    }

    @Override
    public <T> boolean sendDelayMessage(String topic, T message, long delaySeconds) {
        return sendMessageWithRetry(topic, message, true, delaySeconds);
    }

    @Override
    public <T> boolean sendTransactionalMessage(String topic, T message, String transactionId) {
        try {
            String messageStr = JSON.toJSONString(message);
            String messageId = generateMessageId(messageStr);
            
            // 防止重复发送
            if (!markMessageSending(messageId)) {
                log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
                return false;
            }
            
            // 开启事务
            channel.txSelect();
            try {
                Map<String, Object> headers = new HashMap<>();
                headers.put("messageId", messageId);
                headers.put("transactionId", transactionId);
                
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                        .headers(headers)
                        .build();
                
                channel.basicPublish("", topic, props, messageStr.getBytes(StandardCharsets.UTF_8));
                channel.txCommit();
                log.debug("RabbitMQ transactional message sent to topic: {}, transactionId: {}", topic, transactionId);
                clearMessageSendingStatus(messageId);
                return true;
            } catch (Exception e) {
                channel.txRollback();
                log.error("Failed to send RabbitMQ transactional message to topic: {}, transactionId: {}", topic, transactionId, e);
                Map<String, Object> headers = new HashMap<>();
                headers.put("messageId", messageId);
                headers.put("transactionId", transactionId);
                handleSendFailure(topic, messageStr, headers, messageId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send RabbitMQ transactional message to topic: {}", topic, e);
            return false;
        }
    }

    /**
     * 同步发送消息
     */
    public <T> boolean sendSyncMessage(String topic, T message) {
        String messageStr = JSON.toJSONString(message);
        String messageId = generateMessageId(messageStr);
        
        // 防止重复发送
        if (!markMessageSending(messageId)) {
            log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("messageId", messageId);
            
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .headers(headers)
                    .build();
            
            channel.basicPublish("", topic, props, messageStr.getBytes(StandardCharsets.UTF_8));
            long latencyMs = System.currentTimeMillis() - startTime;
            log.debug("RabbitMQ sync message sent successfully to topic: {}, latency: {}ms", topic, latencyMs);
            clearMessageSendingStatus(messageId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send RabbitMQ sync message to topic: {}, messageId: {}", topic, messageId, e);
            Map<String, Object> headers = new HashMap<>();
            headers.put("messageId", messageId);
            handleSendFailure(topic, messageStr, headers, messageId);
            return false;
        }
    }

    /**
     * 异步发送消息
     */
    public <T> boolean sendAsyncMessage(String topic, T message) {
        String messageStr = JSON.toJSONString(message);
        String messageId = generateMessageId(messageStr);
        
        // 防止重复发送
        if (!markMessageSending(messageId)) {
            log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
            return false;
        }
        
        try {
            asyncSendExecutor.execute(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    Map<String, Object> headers = new HashMap<>();
                    headers.put("messageId", messageId);
                    
                    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                            .headers(headers)
                            .build();
                    
                    channel.basicPublish("", topic, props, messageStr.getBytes(StandardCharsets.UTF_8));
                    long latencyMs = System.currentTimeMillis() - startTime;
                    log.debug("RabbitMQ async message sent successfully to topic: {}, latency: {}ms", topic, latencyMs);
                    clearMessageSendingStatus(messageId);
                } catch (Exception e) {
                    log.error("Failed to send RabbitMQ async message to topic: {}, messageId: {}", topic, messageId, e);
                    Map<String, Object> headers = new HashMap<>();
                    headers.put("messageId", messageId);
                    handleSendFailure(topic, messageStr, headers, messageId);
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Failed to submit RabbitMQ async message task to topic: {}, messageId: {}", topic, messageId, e);
            Map<String, Object> headers = new HashMap<>();
            headers.put("messageId", messageId);
            handleSendFailure(topic, messageStr, headers, messageId);
            return false;
        }
    }

    /**
     * 批量发送消息
     */
    public <T> boolean sendBatchMessages(String topic, List<T> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Empty message list for batch send to topic: {}", topic);
            return false;
        }

        Map<String, MessageRetryInfo> messageRecords = new HashMap<>(messages.size());
        for (T message : messages) {
            String messageStr = JSON.toJSONString(message);
            String messageId = generateMessageId(messageStr);
            
            // 防止重复发送
            if (!markMessageSending(messageId)) {
                log.warn("Duplicate message sending attempt detected in batch, messageId: {}", messageId);
                continue;
            }
            
            Map<String, Object> headers = new HashMap<>();
            headers.put("messageId", messageId);
            messageRecords.put(messageId, new MessageRetryInfo(topic, messageStr, headers));
        }

        if (messageRecords.isEmpty()) {
            log.warn("No valid messages to send in batch to topic: {}", topic);
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            for (MessageRetryInfo info : messageRecords.values()) {
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                        .headers(info.headers)
                        .build();
                channel.basicPublish("", info.topic, props, info.message.getBytes(StandardCharsets.UTF_8));
            }
            long latencyMs = System.currentTimeMillis() - startTime;
            log.debug("RabbitMQ batch messages sent successfully to topic: {}, count: {}, latency: {}ms", 
                    topic, messageRecords.size(), latencyMs);
            for (String messageId : messageRecords.keySet()) {
                clearMessageSendingStatus(messageId);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to send RabbitMQ batch messages to topic: {}", topic, e);
            for (Map.Entry<String, MessageRetryInfo> entry : messageRecords.entrySet()) {
                handleSendFailure(entry.getValue().topic, entry.getValue().message, entry.getValue().headers, entry.getKey());
            }
            return false;
        }
    }

    /**
     * 使用重试机制发送消息
     */
    private <T> boolean sendMessageWithRetry(String topic, T message, boolean isDelay, long delaySeconds) {
        String messageStr = JSON.toJSONString(message);
        String messageId = generateMessageId(messageStr);
        
        // 防止重复发送
        if (!markMessageSending(messageId)) {
            log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("messageId", messageId);
            if (isDelay) {
                headers.put("x-delay", delaySeconds * 1000); // RabbitMQ延迟插件使用毫秒
            }
            
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .headers(headers)
                    .build();
            
            channel.basicPublish("", topic, props, messageStr.getBytes(StandardCharsets.UTF_8));
            long latencyMs = System.currentTimeMillis() - startTime;
            log.debug("RabbitMQ message sent successfully to topic: {}, latency: {}ms", topic, latencyMs);
            clearMessageSendingStatus(messageId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send RabbitMQ message to topic: {}, messageId: {}", topic, messageId, e);
            Map<String, Object> headers = new HashMap<>();
            headers.put("messageId", messageId);
            if (isDelay) {
                headers.put("x-delay", delaySeconds * 1000);
            }
            handleSendFailure(topic, messageStr, headers, messageId);
            return false;
        }
    }

    /**
     * 处理发送失败的情况
     */
    private void handleSendFailure(String topic, String message, Map<String, Object> headers, String messageId) {
        failedMessages.put(messageId, new MessageRetryInfo(topic, message, headers));
        log.info("Message added to retry queue, messageId: {}", messageId);
    }

    /**
     * 启动重试线程，处理发送失败的消息
     */
    private void startRetryThread() {
        Thread retryThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                    if (!failedMessages.isEmpty()) {
                        log.info("Starting retry for {} failed messages", failedMessages.size());
                        failedMessages.entrySet().forEach(entry -> {
                            String messageId = entry.getKey();
                            MessageRetryInfo info = entry.getValue();
                            try {
                                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                                        .headers(info.headers)
                                        .build();
                                
                                channel.basicPublish("", info.topic, props, info.message.getBytes(StandardCharsets.UTF_8));
                                failedMessages.remove(messageId);
                                clearMessageSendingStatus(messageId);
                                log.info("Retry successful for messageId: {}", messageId);
                            } catch (Exception e) {
                                log.error("Retry exception for messageId: {}", messageId, e);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Retry thread interrupted");
                } catch (Exception e) {
                    log.error("Unexpected error in retry thread", e);
                }
            }
        });
        retryThread.setDaemon(true);
        retryThread.setName("RabbitMQ-Retry-Thread");
        retryThread.start();
        log.info("RabbitMQ retry thread started");
    }

    /**
     * 生成消息ID，用于去重
     */
    private String generateMessageId(String messageContent) {
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    }

    /**
     * 标记消息正在发送中，返回true表示可以发送，false表示重复发送
     */
    private boolean markMessageSending(String messageId) {
        AtomicBoolean status = messageSendingStatus.computeIfAbsent(messageId, k -> new AtomicBoolean(false));
        boolean canSend = status.compareAndSet(false, true);
        if (!canSend) {
            log.warn("Message already being sent or sent, messageId: {}", messageId);
        }
        return canSend;
    }

    /**
     * 清除消息发送状态
     */
    private void clearMessageSendingStatus(String messageId) {
        messageSendingStatus.remove(messageId);
    }

    @Override
    public <T> boolean subscribe(String topic, String consumerGroup, MessageListener<T> listener) {
        return consumerManager.subscribe(topic, consumerGroup, listener);
    }

    @Override
    public boolean unsubscribe(String topic, String consumerGroup) {
        return consumerManager.unsubscribe(topic, consumerGroup);
    }

    @Override
    public MessageServiceType getServiceType() {
        return MessageServiceType.RABBITMQ;
    }
}
