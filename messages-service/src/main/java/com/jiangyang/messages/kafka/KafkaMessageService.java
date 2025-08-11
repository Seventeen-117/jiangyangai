package com.jiangyang.messages.kafka;

import com.jiangyang.messages.MessageService;
import com.jiangyang.messages.MessageServiceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka消息服务实现类
 * 提供Kafka消息中间件的具体实现
 */
@Slf4j
@Service
public class KafkaMessageService implements MessageService {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.client-id:default-kafka-producer}")
    private String clientId;

    @Value("${kafka.acks:all}")
    private String acks;

    @Value("${kafka.retries:3}")
    private int retries;

    @Value("${kafka.batch-size:16384}")
    private int batchSize;

    @Value("${kafka.linger-ms:1}")
    private int lingerMs;

    @Value("${kafka.buffer-memory:33554432}")
    private long bufferMemory;

    private KafkaProducer<String, String> producer;
    private final ConcurrentHashMap<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> messageDedupMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, acks);
            props.put(ProducerConfig.RETRIES_CONFIG, retries);
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
            props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
            props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
            props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

            producer = new KafkaProducer<>(props);
            log.info("Kafka生产者启动成功: bootstrapServers={}, clientId={}", bootstrapServers, clientId);
        } catch (Exception e) {
            log.error("Kafka生产者启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka生产者启动失败", e);
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

            // 构建消息key（优先使用key，其次使用tag）
            String messageKey = key != null ? key : (tag != null ? tag : null);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageKey, content);
            
            // 同步发送消息
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();
            
            log.info("Kafka消息发送成功: topic={}, tag={}, key={}, partition={}, offset={}", 
                    topic, tag, key, metadata.partition(), metadata.offset());
            
            // 记录消息去重
            recordMessageDedup(dedupKey);
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Kafka消息发送被中断: topic={}, tag={}, key={}, error={}", 
                    topic, tag, key, e.getMessage(), e);
            return false;
        } catch (ExecutionException e) {
            log.error("Kafka消息发送执行失败: topic={}, tag={}, key={}, error={}", 
                    topic, tag, key, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Kafka消息发送异常: topic={}, tag={}, key={}, error={}", 
                    topic, tag, key, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendDelayMessage(String topic, String content, int delayLevel) {
        try {
            // Kafka本身不支持延迟消息，这里通过自定义逻辑实现
            // 可以使用定时任务或者延迟队列来实现
            log.info("Kafka延迟消息发送: topic={}, delayLevel={}, 使用定时任务实现", topic, delayLevel);
            
            // TODO: 实现延迟消息逻辑，可以使用定时任务或者延迟队列
            // 这里暂时直接发送到延迟主题
            String delayTopic = topic + "-delay-" + delayLevel;
            return sendMessage(delayTopic, null, null, content);
            
        } catch (Exception e) {
            log.error("Kafka延迟消息发送异常: topic={}, delayLevel={}, error={}", 
                    topic, delayLevel, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendOrderedMessage(String topic, String content, String hashKey) {
        try {
            // Kafka通过相同的key来保证消息顺序
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, hashKey, content);
            
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();
            
            log.info("Kafka顺序消息发送成功: topic={}, hashKey={}, partition={}, offset={}", 
                    topic, hashKey, metadata.partition(), metadata.offset());
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Kafka顺序消息发送被中断: topic={}, hashKey={}, error={}", 
                    topic, hashKey, e.getMessage(), e);
            return false;
        } catch (ExecutionException e) {
            log.error("Kafka顺序消息发送执行失败: topic={}, hashKey={}, error={}", 
                    topic, hashKey, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Kafka顺序消息发送异常: topic={}, hashKey={}, error={}", 
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

            boolean allSuccess = true;
            for (String content : messages) {
                boolean success = sendMessage(topic, null, null, content);
                if (!success) {
                    allSuccess = false;
                    log.error("批量消息发送失败: topic={}, content={}", topic, content);
                }
            }
            
            if (allSuccess) {
                log.info("Kafka批量消息发送成功: topic={}, messageCount={}", topic, messages.size());
            } else {
                log.warn("Kafka批量消息部分发送失败: topic={}, messageCount={}", topic, messages.size());
            }
            
            return allSuccess;
            
        } catch (Exception e) {
            log.error("Kafka批量消息发送异常: topic={}, messageCount={}, error={}", 
                    topic, messages.size(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public MessageServiceType getServiceType() {
        return MessageServiceType.KAFKA;
    }

    @Override
    public boolean isAvailable() {
        try {
            return producer != null && !producer.metrics().isEmpty();
        } catch (Exception e) {
            log.error("检查Kafka服务状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void shutdown() {
        try {
            if (producer != null) {
                producer.flush();
                producer.close();
                log.info("Kafka消息服务已关闭");
            }
        } catch (Exception e) {
            log.error("关闭Kafka消息服务失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步发送消息
     */
    public void sendMessageAsync(String topic, String tag, String key, String content, Callback callback) {
        try {
            String messageKey = key != null ? key : (tag != null ? tag : null);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageKey, content);
            producer.send(record, callback);
        } catch (Exception e) {
            log.error("Kafka异步消息发送异常: topic={}, tag={}, key={}, error={}", 
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

}
