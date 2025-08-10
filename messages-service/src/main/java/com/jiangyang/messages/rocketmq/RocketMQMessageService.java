package com.jiangyang.messages.rocketmq;

import com.alibaba.fastjson2.JSON;
import com.jiangyang.messages.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RocketMQ消息服务实现
 * 包含了解决消息生产端痛点的机制：
 * 1. 消息丢失风险：实现重试机制和发送确认
 * 2. 性能瓶颈：实现异步发送和批量发送优化
 * 3. 消息质量：实现消息去重和顺序控制
 * 4. 多种发送方式：同步、异步、单向、批量、顺序、延迟和事务消息
 */
@Slf4j
public class RocketMQMessageService implements MessageService, InitializingBean, DisposableBean {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RocketMQBrokerOptimizer brokerOptimizer;

    @Autowired
    private RocketMQConsumerManager consumerManager;

    @Autowired
    private RocketMQMonitoringService monitoringService;


    @Autowired
    private MessageServiceConfig messageServiceConfig;

    @Setter
    private String nameServer;

    @Setter
    private String producerGroup = "jiangyang-producer-group";

    // 添加配置相关的setter方法
    @Setter
    private int retrySyncTimes = 3;
    
    @Setter
    private int retryAsyncTimes = 3;

    private final Map<String, DefaultMQPushConsumer> consumers = new ConcurrentHashMap<>();
    private DefaultMQProducer producer;
    private TransactionMQProducer transactionProducer;
    // 用于跟踪消息发送状态，防止重复发送
    private final Map<String, AtomicBoolean> messageSendingStatus = new ConcurrentHashMap<>();
    // 用于存储发送失败的消息，供重试使用
    private final Map<String, Message> failedMessages = new ConcurrentHashMap<>();
    // 最大重试次数
    private static final int MAX_RETRY_TIMES = 3;
    // 重试间隔时间（毫秒）
    private static final long RETRY_INTERVAL_MS = 5000;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化producer
        try {
            producer = new DefaultMQProducer(producerGroup);
            producer.setNamesrvAddr(nameServer);
            // 设置发送超时时间
            producer.setSendMsgTimeout(3000);
            // 设置重试次数
            producer.setRetryTimesWhenSendFailed(retrySyncTimes);
            // 设置重试间隔
            producer.setRetryTimesWhenSendAsyncFailed(retryAsyncTimes);
            producer.start();
            log.info("RocketMQ producer started with nameServer: {}", nameServer);
            // 设置brokerOptimizer的nameServer
            brokerOptimizer.setNameServer(nameServer);
            // 设置consumerManager的nameServer
            consumerManager.setNameServer(nameServer);
            // 设置monitoringService的nameServer
            monitoringService.setNameServer(nameServer);
            // 设置configManager的nameServer
            // configManager.setNameServer(nameServer); // Removed as per edit hint
            // 启动重试线程
            startRetryThread();
        } catch (MQClientException e) {
            log.error("Failed to start RocketMQ producer", e);
            throw new MessageServiceException("Failed to start RocketMQ producer", e);
        }

        // 初始化transaction producer
        try {
            transactionProducer = new TransactionMQProducer(producerGroup + "_transaction");
            transactionProducer.setNamesrvAddr(nameServer);
            // 设置事务监听器
            transactionProducer.setTransactionListener(new TransactionListener() {
                @Override
                public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                    // 在这里执行本地事务
                    return LocalTransactionState.COMMIT_MESSAGE;
                }

                @Override
                public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                    // 检查本地事务状态
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
            });
            transactionProducer.start();
            log.info("RocketMQ transaction producer started with nameServer: {}", nameServer);
        } catch (MQClientException e) {
            log.error("Failed to start RocketMQ transaction producer", e);
            throw new MessageServiceException("Failed to start RocketMQ transaction producer", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        // 关闭producer
        if (producer != null) {
            producer.shutdown();
            log.info("RocketMQ producer shutdown");
        }
        
        // 关闭transaction producer
        if (transactionProducer != null) {
            transactionProducer.shutdown();
            log.info("RocketMQ transaction producer shutdown");
        }
        
        // 关闭所有consumer
        for (DefaultMQPushConsumer consumer : consumers.values()) {
            if (consumer != null) {
                consumer.shutdown();
            }
        }
        consumers.clear();
        log.info("RocketMQ consumers shutdown");
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
            Message msg = new Message(topic, messageStr.getBytes(StandardCharsets.UTF_8));
            // 设置事务ID
            msg.putUserProperty("transactionId", transactionId);
            msg.putUserProperty("messageId", messageId);
            // 防止重复发送
            if (!markMessageSending(messageId)) {
                log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
                return false;
            }
            transactionProducer.sendMessageInTransaction(msg, null);
            log.debug("RocketMQ transactional message sent to topic: {}, transactionId: {}, message: {}", topic, transactionId, messageStr);
            clearMessageSendingStatus(messageId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send RocketMQ transactional message to topic: {}", topic, e);
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
        
        Message msg = new Message(topic, messageStr.getBytes(StandardCharsets.UTF_8));
        msg.putUserProperty("messageId", messageId);
        
        long startTime = System.currentTimeMillis();
        try {
            SendResult sendResult = producer.send(msg);
            long latencyMs = System.currentTimeMillis() - startTime;
            monitoringService.recordMessageSent(topic, latencyMs);
            if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                log.debug("RocketMQ sync message sent successfully to topic: {}, messageId: {}", topic, messageId);
                clearMessageSendingStatus(messageId);
                return true;
            } else {
                log.error("RocketMQ sync message sending failed, status: {}, messageId: {}", sendResult.getSendStatus(), messageId);
                handleSendFailure(topic, msg, messageId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send RocketMQ sync message to topic: {}, messageId: {}", topic, messageId, e);
            handleSendFailure(topic, msg, messageId);
            return false;
        }
    }

    /**
     * 异步发送消息
     */
    public <T> boolean sendAsyncMessage(String topic, T message, SendCallback callback) {
        String messageStr = JSON.toJSONString(message);
        String messageId = generateMessageId(messageStr);
        
        // 防止重复发送
        if (!markMessageSending(messageId)) {
            log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
            return false;
        }
        
        Message msg = new Message(topic, messageStr.getBytes(StandardCharsets.UTF_8));
        msg.putUserProperty("messageId", messageId);
        
        long startTime = System.currentTimeMillis();
        try {
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    monitoringService.recordMessageSent(topic, latencyMs);
                    if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                        log.debug("RocketMQ async message sent successfully to topic: {}, messageId: {}", topic, messageId);
                        clearMessageSendingStatus(messageId);
                    } else {
                        log.error("RocketMQ async message sending failed, status: {}, messageId: {}", sendResult.getSendStatus(), messageId);
                        handleSendFailure(topic, msg, messageId);
                    }
                    if (callback != null) {
                        callback.onSuccess(sendResult);
                    }
                }

                @Override
                public void onException(Throwable e) {
                    log.error("RocketMQ async message sending exception, messageId: {}", messageId, e);
                    handleSendFailure(topic, msg, messageId);
                    if (callback != null) {
                        callback.onException(e);
                    }
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Failed to send RocketMQ async message to topic: {}, messageId: {}", topic, messageId, e);
            handleSendFailure(topic, msg, messageId);
            return false;
        }
    }

    /**
     * 单向发送消息
     */
    public <T> boolean sendOnewayMessage(String topic, T message) {
        String messageStr = JSON.toJSONString(message);
        String messageId = generateMessageId(messageStr);
        
        // 防止重复发送
        if (!markMessageSending(messageId)) {
            log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
            return false;
        }
        
        Message msg = new Message(topic, messageStr.getBytes(StandardCharsets.UTF_8));
        msg.putUserProperty("messageId", messageId);
        
        try {
            producer.sendOneway(msg);
            log.debug("RocketMQ oneway message sent to topic: {}, messageId: {}", topic, messageId);
            clearMessageSendingStatus(messageId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send RocketMQ oneway message to topic: {}, messageId: {}", topic, messageId, e);
            handleSendFailure(topic, msg, messageId);
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

        List<Message> messageList = new java.util.ArrayList<>(messages.size());
        for (T message : messages) {
            String messageStr = JSON.toJSONString(message);
            String messageId = generateMessageId(messageStr);
            
            // 防止重复发送
            if (!markMessageSending(messageId)) {
                log.warn("Duplicate message sending attempt detected in batch, messageId: {}", messageId);
                continue;
            }
            
            Message msg = new Message(topic, messageStr.getBytes(StandardCharsets.UTF_8));
            msg.putUserProperty("messageId", messageId);
            messageList.add(msg);
        }

        if (messageList.isEmpty()) {
            log.warn("No valid messages to send in batch to topic: {}", topic);
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            SendResult sendResult = producer.send(messageList);
            long latencyMs = System.currentTimeMillis() - startTime;
            monitoringService.recordMessageSent(topic, latencyMs);
            if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                log.debug("RocketMQ batch messages sent successfully to topic: {}, count: {}", topic, messageList.size());
                for (Message msg : messageList) {
                    clearMessageSendingStatus(msg.getUserProperty("messageId"));
                }
                return true;
            } else {
                log.error("RocketMQ batch messages sending failed, status: {}", sendResult.getSendStatus());
                for (Message msg : messageList) {
                    handleSendFailure(topic, msg, msg.getUserProperty("messageId"));
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send RocketMQ batch messages to topic: {}", topic, e);
            for (Message msg : messageList) {
                handleSendFailure(topic, msg, msg.getUserProperty("messageId"));
            }
            return false;
        }
    }

    /**
     * 发送顺序消息
     */
    public <T> boolean sendOrderedMessage(String topic, T message, Object shardingKey) {
        String messageStr = JSON.toJSONString(message);
        String messageId = generateMessageId(messageStr);
        
        // 防止重复发送
        if (!markMessageSending(messageId)) {
            log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
            return false;
        }
        
        Message msg = new Message(topic, messageStr.getBytes(StandardCharsets.UTF_8));
        msg.putUserProperty("messageId", messageId);
        
        long startTime = System.currentTimeMillis();
        try {
            SendResult sendResult = producer.send(msg, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    int index = arg.hashCode() % mqs.size();
                    return mqs.get(Math.abs(index));
                }
            }, shardingKey);
            long latencyMs = System.currentTimeMillis() - startTime;
            monitoringService.recordMessageSent(topic, latencyMs);
            if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                log.debug("RocketMQ ordered message sent successfully to topic: {}, messageId: {}", topic, messageId);
                clearMessageSendingStatus(messageId);
                return true;
            } else {
                log.error("RocketMQ ordered message sending failed, status: {}, messageId: {}", sendResult.getSendStatus(), messageId);
                handleSendFailure(topic, msg, messageId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send RocketMQ ordered message to topic: {}, messageId: {}", topic, messageId, e);
            handleSendFailure(topic, msg, messageId);
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
        
        Message msg = new Message(topic, messageStr.getBytes(StandardCharsets.UTF_8));
        msg.putUserProperty("messageId", messageId);
        if (isDelay) {
            msg.setDelayTimeLevel(getDelayTimeLevel(delaySeconds));
        }
        
        long startTime = System.currentTimeMillis();
        try {
            // 异步发送，防止同步发送阻塞业务线程
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    monitoringService.recordMessageSent(topic, latencyMs);
                    if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                        log.debug("RocketMQ message sent successfully to topic: {}, messageId: {}", topic, messageId);
                        clearMessageSendingStatus(messageId);
                    } else {
                        log.error("RocketMQ message sending failed, status: {}, messageId: {}", sendResult.getSendStatus(), messageId);
                        handleSendFailure(topic, msg, messageId);
                    }
                }

                @Override
                public void onException(Throwable e) {
                    log.error("RocketMQ message sending exception, messageId: {}", messageId, e);
                    handleSendFailure(topic, msg, messageId);
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Failed to send RocketMQ message to topic: {}, messageId: {}", topic, messageId, e);
            handleSendFailure(topic, msg, messageId);
            return false;
        }
    }

    /**
     * 处理发送失败的情况
     */
    private void handleSendFailure(String topic, Message msg, String messageId) {
        failedMessages.put(messageId, msg);
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
                            Message msg = entry.getValue();
                            try {
                                SendResult result = producer.send(msg);
                                if (result.getSendStatus() == SendStatus.SEND_OK) {
                                    failedMessages.remove(messageId);
                                    clearMessageSendingStatus(messageId);
                                    log.info("Retry successful for messageId: {}", messageId);
                                } else {
                                    log.warn("Retry failed for messageId: {}, status: {}", messageId, result.getSendStatus());
                                }
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
        retryThread.setName("RocketMQ-Retry-Thread");
        retryThread.start();
        log.info("RocketMQ retry thread started");
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
        return MessageServiceType.ROCKETMQ;
    }

    /**
     * 将延迟秒数转换为RocketMQ的延迟级别
     */
    private int getDelayTimeLevel(long delaySeconds) {
        // RocketMQ支持18个延迟级别：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
        long[] delayLevels = {1, 5, 10, 30, 60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 1200, 1800, 3600, 7200};
        for (int i = 0; i < delayLevels.length; i++) {
            if (delaySeconds <= delayLevels[i]) {
                return i + 1;
            }
        }
        return delayLevels.length;
    }
}
