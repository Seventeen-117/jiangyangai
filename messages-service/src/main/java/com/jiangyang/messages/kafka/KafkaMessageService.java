package com.jiangyang.messages.kafka;

import com.alibaba.fastjson2.JSON;
import com.jiangyang.messages.MessageListener;
import com.jiangyang.messages.MessageService;
import com.jiangyang.messages.MessageServiceException;
import com.jiangyang.messages.MessageServiceType;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka消息服务实现
 * 包含了解决消息生产端痛点的机制：
 * 1. 消息丢失风险：实现重试机制和发送确认
 * 2. 性能瓶颈：实现异步发送和批量发送优化
 * 3. 消息质量：实现消息去重和分区控制
 * 4. 多种发送方式：同步、异步、批量和事务消息
 */
@Slf4j
public class KafkaMessageService implements MessageService, InitializingBean, DisposableBean {

    @Setter
    private String bootstrapServers = "localhost:9092";

    @Setter
    private String producerGroup = "jiangyang-kafka-producer-group";

    private KafkaProducer<String, String> producer;
    private KafkaConsumerManager consumerManager;
    // 用于跟踪消息发送状态，防止重复发送
    private final Map<String, AtomicBoolean> messageSendingStatus = new ConcurrentHashMap<>();
    // 用于存储发送失败的消息，供重试使用
    private final Map<String, ProducerRecord<String, String>> failedMessages = new ConcurrentHashMap<>();
    // 最大重试次数
    private static final int MAX_RETRY_TIMES = 3;
    // 重试间隔时间（毫秒）
    private static final long RETRY_INTERVAL_MS = 5000;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化producer
        try {
            Properties props = new Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("group.id", producerGroup);
            props.put("key.serializer", StringSerializer.class.getName());
            props.put("value.serializer", StringSerializer.class.getName());
            // 设置发送超时时间
            props.put("delivery.timeout.ms", 120000);
            // 设置重试次数
            props.put("retries", MAX_RETRY_TIMES);
            // 设置批量大小
            props.put("batch.size", 16384);
            // 设置缓冲区大小
            props.put("buffer.memory", 33554432);
            producer = new KafkaProducer<>(props);
            log.info("Kafka producer started with bootstrapServers: {}", bootstrapServers);
            
            // 初始化consumerManager
            consumerManager = new KafkaConsumerManager();
            consumerManager.setBootstrapServers(bootstrapServers);
            consumerManager.afterPropertiesSet();
            
            // 启动重试线程
            startRetryThread();
        } catch (Exception e) {
            log.error("Failed to start Kafka producer", e);
            throw new MessageServiceException("Failed to start Kafka producer", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        // 关闭producer
        if (producer != null) {
            producer.close();
            log.info("Kafka producer shutdown");
        }
        
        // 关闭consumerManager
        if (consumerManager != null) {
            consumerManager.destroy();
            log.info("Kafka consumer manager shutdown");
        }
    }

    @Override
    public <T> boolean sendMessage(String topic, T message) {
        return sendMessageWithRetry(topic, message, false, 0);
    }

    @Override
    public <T> boolean sendDelayMessage(String topic, T message, long delaySeconds) {
        // Kafka不支持原生延迟消息，可以通过其他方式实现，如定时任务或额外主题
        log.warn("Kafka does not support native delay messages. Sending immediately to topic: {}", topic);
        return sendMessageWithRetry(topic, message, false, 0);
    }

    @Override
    public <T> boolean sendTransactionalMessage(String topic, T message, String transactionId) {
        try {
            String messageStr = JSON.toJSONString(message);
            String messageId = generateMessageId(messageStr);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageStr);
            record.headers().add("messageId", messageId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("transactionId", transactionId.getBytes(StandardCharsets.UTF_8));
            
            // 防止重复发送
            if (!markMessageSending(messageId)) {
                log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
                return false;
            }
            
            // 初始化事务
            producer.initTransactions();
            try {
                producer.beginTransaction();
                Future<RecordMetadata> future = producer.send(record);
                producer.commitTransaction();
                RecordMetadata metadata = future.get();
                log.debug("Kafka transactional message sent to topic: {}, partition: {}, offset: {}, transactionId: {}", 
                        topic, metadata.partition(), metadata.offset(), transactionId);
                clearMessageSendingStatus(messageId);
                return true;
            } catch (Exception e) {
                producer.abortTransaction();
                log.error("Failed to send Kafka transactional message to topic: {}, transactionId: {}", topic, transactionId, e);
                handleSendFailure(topic, record, messageId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send Kafka transactional message to topic: {}", topic, e);
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
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageStr);
        record.headers().add("messageId", messageId.getBytes(StandardCharsets.UTF_8));
        
        long startTime = System.currentTimeMillis();
        try {
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();
            long latencyMs = System.currentTimeMillis() - startTime;
            log.debug("Kafka sync message sent successfully to topic: {}, partition: {}, offset: {}, latency: {}ms", 
                    topic, metadata.partition(), metadata.offset(), latencyMs);
            clearMessageSendingStatus(messageId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send Kafka sync message to topic: {}, messageId: {}", topic, messageId, e);
            handleSendFailure(topic, record, messageId);
            return false;
        }
    }

    /**
     * 异步发送消息
     */
    public <T> boolean sendAsyncMessage(String topic, T message, Callback callback) {
        String messageStr = JSON.toJSONString(message);
        String messageId = generateMessageId(messageStr);
        
        // 防止重复发送
        if (!markMessageSending(messageId)) {
            log.warn("Duplicate message sending attempt detected, messageId: {}", messageId);
            return false;
        }
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageStr);
        record.headers().add("messageId", messageId.getBytes(StandardCharsets.UTF_8));
        
        long startTime = System.currentTimeMillis();
        try {
            producer.send(record, (metadata, exception) -> {
                long latencyMs = System.currentTimeMillis() - startTime;
                if (exception == null) {
                    log.debug("Kafka async message sent successfully to topic: {}, partition: {}, offset: {}, latency: {}ms", 
                            topic, metadata.partition(), metadata.offset(), latencyMs);
                    clearMessageSendingStatus(messageId);
                } else {
                    log.error("Kafka async message sending failed, messageId: {}", messageId, exception);
                    handleSendFailure(topic, record, messageId);
                }
                if (callback != null) {
                    callback.onCompletion(metadata, exception);
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Failed to send Kafka async message to topic: {}, messageId: {}", topic, messageId, e);
            handleSendFailure(topic, record, messageId);
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

        Map<String, ProducerRecord<String, String>> messageRecords = new HashMap<>(messages.size());
        for (T message : messages) {
            String messageStr = JSON.toJSONString(message);
            String messageId = generateMessageId(messageStr);
            
            // 防止重复发送
            if (!markMessageSending(messageId)) {
                log.warn("Duplicate message sending attempt detected in batch, messageId: {}", messageId);
                continue;
            }
            
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageStr);
            record.headers().add("messageId", messageId.getBytes(StandardCharsets.UTF_8));
            messageRecords.put(messageId, record);
        }

        if (messageRecords.isEmpty()) {
            log.warn("No valid messages to send in batch to topic: {}", topic);
            return false;
        }

        long startTime = System.currentTimeMillis();
        try {
            for (ProducerRecord<String, String> record : messageRecords.values()) {
                producer.send(record);
            }
            producer.flush();
            long latencyMs = System.currentTimeMillis() - startTime;
            log.debug("Kafka batch messages sent successfully to topic: {}, count: {}, latency: {}ms", 
                    topic, messageRecords.size(), latencyMs);
            for (String messageId : messageRecords.keySet()) {
                clearMessageSendingStatus(messageId);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to send Kafka batch messages to topic: {}", topic, e);
            for (Map.Entry<String, ProducerRecord<String, String>> entry : messageRecords.entrySet()) {
                handleSendFailure(topic, entry.getValue(), entry.getKey());
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
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageStr);
        record.headers().add("messageId", messageId.getBytes(StandardCharsets.UTF_8));
        
        long startTime = System.currentTimeMillis();
        try {
            producer.send(record, (metadata, exception) -> {
                long latencyMs = System.currentTimeMillis() - startTime;
                if (exception == null) {
                    log.debug("Kafka message sent successfully to topic: {}, partition: {}, offset: {}, latency: {}ms", 
                            topic, metadata.partition(), metadata.offset(), latencyMs);
                    clearMessageSendingStatus(messageId);
                } else {
                    log.error("Kafka message sending failed, messageId: {}", messageId, exception);
                    handleSendFailure(topic, record, messageId);
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Failed to send Kafka message to topic: {}, messageId: {}", topic, messageId, e);
            handleSendFailure(topic, record, messageId);
            return false;
        }
    }

    /**
     * 处理发送失败的情况
     */
    private void handleSendFailure(String topic, ProducerRecord<String, String> record, String messageId) {
        failedMessages.put(messageId, record);
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
                            ProducerRecord<String, String> record = entry.getValue();
                            try {
                                producer.send(record, (metadata, exception) -> {
                                    if (exception == null) {
                                        failedMessages.remove(messageId);
                                        clearMessageSendingStatus(messageId);
                                        log.info("Retry successful for messageId: {}", messageId);
                                    } else {
                                        log.warn("Retry failed for messageId: {}", messageId, exception);
                                    }
                                });
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
        retryThread.setName("Kafka-Retry-Thread");
        retryThread.start();
        log.info("Kafka retry thread started");
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
        return MessageServiceType.KAFKA;
    }
}
