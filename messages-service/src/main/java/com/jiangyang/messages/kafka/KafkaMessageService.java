package com.jiangyang.messages.kafka;

import com.jiangyang.messages.service.MessageService;
import com.jiangyang.messages.consume.MessageServiceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.net.InetAddress;
import java.util.Arrays;
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
public class KafkaMessageService implements MessageService {

    private String bootstrapServers;

    private String clientId;

    private String acks;

    private int retries;

    private int batchSize;

    private int lingerMs;

    private long bufferMemory;

    private KafkaProducer<String, String> producer;
    private final ConcurrentHashMap<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> messageDedupMap = new ConcurrentHashMap<>();

    public void init() {
        try {
            // 检查必要的配置属性
            if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
                log.warn("Kafka bootstrapServers is not configured, skipping initialization");
                return;
            }

            // 预检查：解析并验证 bootstrapServers 中的主机是否可解析，避免 UnknownHostException 警告刷屏
            if (!canResolveBootstrapServers(bootstrapServers)) {
                log.warn("Kafka bootstrapServers contains unreachable host(s), skip init: {}", bootstrapServers);
                return;
            }
            
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId != null ? clientId : "default-kafka-producer");
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            // 设置acks配置
            String acksValue = acks != null ? acks : "all";
            props.put(ProducerConfig.ACKS_CONFIG, acksValue);
            
            // 只有当acks为"all"时才启用幂等性，否则Kafka会抛出异常
            boolean enableIdempotence = "all".equals(acksValue);
            props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
            
            // 如果启用幂等性，设置相关配置
            if (enableIdempotence) {
                props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
            } else {
                // 当不启用幂等性时，可以设置更高的并发请求数
                props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 10);
            }
            
            props.put(ProducerConfig.RETRIES_CONFIG, retries);
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
            props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);

            producer = new KafkaProducer<>(props);
            log.info("Kafka生产者启动成功: bootstrapServers={}, clientId={}, acks={}, idempotence={}", 
                    bootstrapServers, clientId, acksValue, enableIdempotence);
        } catch (Exception e) {
            log.error("Kafka生产者启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka生产者启动失败", e);
        }
    }

    /**
     * 解析并验证 bootstrap.servers 的主机是否可解析
     */
    private boolean canResolveBootstrapServers(String servers) {
        try {
            List<String> endpoints = Arrays.asList(servers.split(","));
            for (String endpoint : endpoints) {
                String trimmed = endpoint.trim();
                String hostPort = trimmed.contains("://") ? trimmed.substring(trimmed.indexOf("://") + 3) : trimmed;
                String host = hostPort.contains(":") ? hostPort.substring(0, hostPort.indexOf(":")) : hostPort;
                // 忽略明显的本地和IP情况
                if (host.equalsIgnoreCase("localhost")) {
                    continue;
                }
                // 解析域名/主机
                InetAddress.getByName(host);
            }
            return true;
        } catch (Exception e) {
            log.warn("无法解析Kafka主机: {}, error={}", servers, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendMessage(String topic, String content) {
        if (producer == null) {
            log.warn("Kafka producer not initialized, skip send. topic={}", topic);
            return false;
        }
        return sendMessage(topic, null, null, content);
    }

    @Override
    public boolean sendMessage(String topic, String tag, String content) {
        if (producer == null) {
            log.warn("Kafka producer not initialized, skip send. topic={}, tag={}", topic, tag);
            return false;
        }
        return sendMessage(topic, tag, null, content);
    }

    @Override
    public boolean sendMessage(String topic, String tag, String key, String content) {
        try {
            if (producer == null) {
                log.warn("Kafka producer not initialized, skip send. topic={}, tag={}, key={}", topic, tag, key);
                return false;
            }
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
    @PreDestroy
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

    // setters for configuration injection
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setAcks(String acks) { this.acks = acks; }
    public void setRetries(int retries) { this.retries = retries; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public void setLingerMs(int lingerMs) { this.lingerMs = lingerMs; }
    public void setBufferMemory(long bufferMemory) { this.bufferMemory = bufferMemory; }

    /**
     * 异步发送消息
     */
    public void sendMessageAsync(String topic, String tag, String key, String content, Callback callback) {
        try {
            if (producer == null) {
                log.warn("Kafka producer not initialized, skip async send. topic={}, tag={}, key={}", topic, tag, key);
                return;
            }
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

    @Override
    public boolean sendTransactionMessage(String topic, String tag, String messageBody, 
                                       String transactionId, String businessKey, int timeout) {
        try {
            log.info("发送Kafka事务消息: topic={}, tag={}, transactionId={}, businessKey={}, timeout={}", 
                    topic, tag, transactionId, businessKey, timeout);
            
            if (producer == null) {
                log.warn("Kafka producer not initialized, skip transaction message send");
                return false;
            }
            
            // 创建事务消息记录
            String messageKey = businessKey != null ? businessKey : transactionId;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageKey, messageBody);
            
            // 设置事务相关属性到消息头
            record.headers().add("transactionId", transactionId.getBytes());
            record.headers().add("businessKey", businessKey != null ? businessKey.getBytes() : "".getBytes());
            record.headers().add("messageType", "TRANSACTION".getBytes());
            
            // 发送事务消息
            // 注意：Kafka的事务消息需要配置事务ID和启用幂等性
            // 目前先使用普通发送，后续可以扩展为真正的事务消息
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get(); // 同步等待发送结果
            
            log.info("Kafka事务消息发送成功: topic={}, tag={}, transactionId={}, partition={}, offset={}", 
                    topic, tag, transactionId, metadata.partition(), metadata.offset());
            return true;
            
        } catch (Exception e) {
            log.error("Kafka事务消息发送异常: topic={}, tag={}, transactionId={}, error={}", 
                    topic, tag, transactionId, e.getMessage(), e);
            return false;
        }
    }
}
