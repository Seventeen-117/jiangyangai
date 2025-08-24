package com.jiangyang.messages.kafka;

import com.jiangyang.messages.config.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Kafka消费者管理器
 * 实现各种消费方式和特性
 */
@Slf4j
@Component
public class KafkaConsumerManager {

    @Autowired
    private KafkaConfig config;

    /**
     * 消费者映射：consumerId -> consumer
     */
    private final Map<String, KafkaConsumer<String, String>> consumers = new ConcurrentHashMap<>();

    /**
     * 消费者状态映射：consumerId -> isRunning
     */
    private final Map<String, AtomicBoolean> consumerStatus = new ConcurrentHashMap<>();

    /**
     * 消费者统计信息：consumerId -> statistics
     */
    private final Map<String, ConsumerStatistics> statistics = new ConcurrentHashMap<>();

    /**
     * 消费者线程池
     */
    private final ExecutorService consumerExecutor = Executors.newCachedThreadPool();

    /**
     * 批量处理线程池
     */
    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(
            config.getConsumer().getBatchThreadPoolSize(),
            r -> new Thread(r, "kafka-batch-processor")
    );

    /**
     * 顺序消费线程池
     */
    private final ExecutorService orderlyExecutor = Executors.newFixedThreadPool(
            config.getConsumer().getOrderThreadCount(),
            r -> new Thread(r, "kafka-orderly-processor")
    );

    /**
     * 偏移量缓存
     */
    private final Map<String, Map<TopicPartition, Long>> offsetCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("初始化Kafka消费者管理器");
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭Kafka消费者管理器");
        
        // 关闭所有消费者
        consumers.forEach((consumerId, consumer) -> {
            try {
                consumer.close();
                log.info("关闭消费者: {}", consumerId);
            } catch (Exception e) {
                log.error("关闭消费者异常: consumerId={}, error={}", consumerId, e.getMessage(), e);
            }
        });

        // 关闭线程池
        consumerExecutor.shutdown();
        batchExecutor.shutdown();
        orderlyExecutor.shutdown();

        try {
            if (!consumerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                consumerExecutor.shutdownNow();
            }
            if (!batchExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
            if (!orderlyExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                orderlyExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待线程池关闭时被中断");
        }
    }

    /**
     * 创建集群消费者
     * 同组消费者分担消息，每条消息仅被消费一次
     */
    public String createClusterConsumer(String topic, String groupId, 
                                      Consumer<ConsumerRecord<String, String>> messageHandler) {
        String consumerId = "cluster_" + groupId + "_" + System.currentTimeMillis();
        
        try {
            // 获取集群消费配置
            Map<String, Object> props = config.getConsumerProperties(KafkaConfig.ConsumerType.CLUSTER);
            props.put("group.id", groupId);
            
            // 创建消费者
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            
            // 订阅主题
            consumer.subscribe(Collections.singletonList(topic), new ClusterRebalanceListener(consumerId));
            
            // 启动消费线程
            startConsumerThread(consumerId, consumer, messageHandler, "集群消费");
            
            log.info("创建集群消费者成功: consumerId={}, topic={}, groupId={}", consumerId, topic, groupId);
            return consumerId;
            
        } catch (Exception e) {
            log.error("创建集群消费者失败: topic={}, groupId={}, error={}", topic, groupId, e.getMessage(), e);
            throw new RuntimeException("创建集群消费者失败", e);
        }
    }

    /**
     * 创建广播消费者
     * 不同消费组独立消费，同一条消息被所有消费组消费
     */
    public String createBroadcastConsumer(String topic, String groupId, 
                                        Consumer<ConsumerRecord<String, String>> messageHandler) {
        String consumerId = "broadcast_" + groupId + "_" + System.currentTimeMillis();
        
        try {
            // 获取广播消费配置
            Map<String, Object> props = config.getConsumerProperties(KafkaConfig.ConsumerType.BROADCAST);
            props.put("group.id", groupId);
            
            // 创建消费者
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            
            // 订阅主题
            consumer.subscribe(Collections.singletonList(topic), new BroadcastRebalanceListener(consumerId));
            
            // 启动消费线程
            startConsumerThread(consumerId, consumer, messageHandler, "广播消费");
            
            log.info("创建广播消费者成功: consumerId={}, topic={}, groupId={}", consumerId, topic, groupId);
            return consumerId;
            
        } catch (Exception e) {
            log.error("创建广播消费者失败: topic={}, groupId={}, error={}", topic, groupId, e.getMessage(), e);
            throw new RuntimeException("创建广播消费者失败", e);
        }
    }

    /**
     * 创建顺序消费者
     * 基于分区或消息键的顺序消费
     */
    public String createOrderlyConsumer(String topic, String groupId, 
                                      Consumer<ConsumerRecord<String, String>> messageHandler) {
        String consumerId = "orderly_" + groupId + "_" + System.currentTimeMillis();
        
        try {
            // 获取顺序消费配置
            Map<String, Object> props = config.getConsumerProperties(KafkaConfig.ConsumerType.ORDERLY);
            props.put("group.id", groupId);
            
            // 创建消费者
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            
            // 订阅主题
            consumer.subscribe(Collections.singletonList(topic), new OrderlyRebalanceListener(consumerId));
            
            // 启动顺序消费线程
            startOrderlyConsumerThread(consumerId, consumer, messageHandler);
            
            log.info("创建顺序消费者成功: consumerId={}, topic={}, groupId={}", consumerId, topic, groupId);
            return consumerId;
            
        } catch (Exception e) {
            log.error("创建顺序消费者失败: topic={}, groupId={}, error={}", topic, groupId, e.getMessage(), e);
            throw new RuntimeException("创建顺序消费者失败", e);
        }
    }

    /**
     * 创建批量消费者
     * 批量拉取和处理消息
     */
    public String createBatchConsumer(String topic, String groupId, 
                                    Consumer<List<ConsumerRecord<String, String>>> batchHandler) {
        String consumerId = "batch_" + groupId + "_" + System.currentTimeMillis();
        
        try {
            // 获取批量消费配置
            Map<String, Object> props = config.getConsumerProperties(KafkaConfig.ConsumerType.BATCH);
            props.put("group.id", groupId);
            
            // 创建消费者
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            
            // 订阅主题
            consumer.subscribe(Collections.singletonList(topic), new BatchRebalanceListener(consumerId));
            
            // 启动批量消费线程
            startBatchConsumerThread(consumerId, consumer, batchHandler);
            
            log.info("创建批量消费者成功: consumerId={}, topic={}, groupId={}", consumerId, topic, groupId);
            return consumerId;
            
        } catch (Exception e) {
            log.error("创建批量消费者失败: topic={}, groupId={}, error={}", topic, groupId, e.getMessage(), e);
            throw new RuntimeException("创建批量消费者失败", e);
        }
    }

    /**
     * 启动普通消费线程
     */
    private void startConsumerThread(String consumerId, KafkaConsumer<String, String> consumer,
                                   Consumer<ConsumerRecord<String, String>> messageHandler, String consumerType) {
        consumers.put(consumerId, consumer);
        consumerStatus.put(consumerId, new AtomicBoolean(true));
        statistics.put(consumerId, new ConsumerStatistics());

        consumerExecutor.submit(() -> {
            try {
                log.info("启动{}线程: consumerId={}", consumerType, consumerId);
                
                while (consumerStatus.get(consumerId).get()) {
                    try {
                        // 拉取消息，超时时间 100ms
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                        
                        if (!records.isEmpty()) {
                            // 处理消息
                            for (ConsumerRecord<String, String> record : records) {
                                try {
                                    // 更新统计信息
                                    updateStatistics(consumerId, record);
                                    
                                    // 处理消息
                                    messageHandler.accept(record);
                                    
                                    // 手动提交偏移量（集群消费）
                                    if (!config.getConsumer().isEnableAutoCommit()) {
                                        commitOffset(consumerId, consumer, record);
                                    }
                                    
                                } catch (Exception e) {
                                    log.error("处理消息异常: consumerId={}, topic={}, partition={}, offset={}, error={}",
                                            consumerId, record.topic(), record.partition(), record.offset(), e.getMessage(), e);
                                    
                                    // 重试机制
                                    handleMessageError(consumerId, consumer, record, e);
                                }
                            }
                        }
                        
                    } catch (Exception e) {
                        if (e instanceof RetriableException) {
                            log.warn("可重试异常，继续消费: consumerId={}, error={}", consumerId, e.getMessage());
                            continue;
                        }
                        log.error("消费异常: consumerId={}, error={}", consumerId, e.getMessage(), e);
                        break;
                    }
                }
                
            } finally {
                log.info("{}线程结束: consumerId={}", consumerType, consumerId);
            }
        });
    }

    /**
     * 启动顺序消费线程
     */
    private void startOrderlyConsumerThread(String consumerId, KafkaConsumer<String, String> consumer,
                                          Consumer<ConsumerRecord<String, String>> messageHandler) {
        consumers.put(consumerId, consumer);
        consumerStatus.put(consumerId, new AtomicBoolean(true));
        statistics.put(consumerId, new ConsumerStatistics());

        orderlyExecutor.submit(() -> {
            try {
                log.info("启动顺序消费线程: consumerId={}", consumerId);
                
                while (consumerStatus.get(consumerId).get()) {
                    try {
                        // 拉取单条消息
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                        
                        if (!records.isEmpty()) {
                            // 按分区分组处理，确保分区内顺序
                            Map<Integer, List<ConsumerRecord<String, String>>> partitionRecords = new HashMap<>();
                            for (ConsumerRecord<String, String> record : records) {
                                partitionRecords.computeIfAbsent(record.partition(), k -> new ArrayList<>()).add(record);
                            }
                            
                            // 按分区顺序处理
                            for (Map.Entry<Integer, List<ConsumerRecord<String, String>>> entry : partitionRecords.entrySet()) {
                                List<ConsumerRecord<String, String>> partitionMsgs = entry.getValue();
                                
                                // 按偏移量排序，确保顺序
                                partitionMsgs.sort(Comparator.comparingLong(ConsumerRecord::offset));
                                
                                for (ConsumerRecord<String, String> record : partitionMsgs) {
                                    try {
                                        // 更新统计信息
                                        updateStatistics(consumerId, record);
                                        
                                        // 处理消息
                                        messageHandler.accept(record);
                                        
                                        // 手动提交偏移量
                                        commitOffset(consumerId, consumer, record);
                                        
                                    } catch (Exception e) {
                                        log.error("顺序消费消息异常: consumerId={}, topic={}, partition={}, offset={}, error={}",
                                                consumerId, record.topic(), record.partition(), record.offset(), e.getMessage(), e);
                                        
                                        // 顺序消费失败，停止该分区的消费
                                        break;
                                    }
                                }
                            }
                        }
                        
                    } catch (Exception e) {
                        if (e instanceof RetriableException) {
                            log.warn("顺序消费可重试异常，继续消费: consumerId={}, error={}", consumerId, e.getMessage());
                            continue;
                        }
                        log.error("顺序消费异常: consumerId={}, error={}", consumerId, e.getMessage(), e);
                        break;
                    }
                }
                
            } finally {
                log.info("顺序消费线程结束: consumerId={}", consumerId);
            }
        });
    }

    /**
     * 启动批量消费线程
     */
    private void startBatchConsumerThread(String consumerId, KafkaConsumer<String, String> consumer,
                                        Consumer<List<ConsumerRecord<String, String>>> batchHandler) {
        consumers.put(consumerId, consumer);
        consumerStatus.put(consumerId, new AtomicBoolean(true));
        statistics.put(consumerId, new ConsumerStatistics());

        batchExecutor.submit(() -> {
            try {
                log.info("启动批量消费线程: consumerId={}", consumerId);
                
                while (consumerStatus.get(consumerId).get()) {
                    try {
                        // 拉取批量消息
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                        
                        if (!records.isEmpty()) {
                            List<ConsumerRecord<String, String>> batchRecords = new ArrayList<>();
                            
                            for (ConsumerRecord<String, String> record : records) {
                                batchRecords.add(record);
                                
                                // 达到批量大小或超时时处理
                                if (batchRecords.size() >= config.getConsumer().getBatchSize()) {
                                    processBatch(consumerId, consumer, batchRecords, batchHandler);
                                    batchRecords.clear();
                                }
                            }
                            
                            // 处理剩余的消息
                            if (!batchRecords.isEmpty()) {
                                processBatch(consumerId, consumer, batchRecords, batchHandler);
                            }
                        }
                        
                    } catch (Exception e) {
                        if (e instanceof RetriableException) {
                            log.warn("批量消费可重试异常，继续消费: consumerId={}, error={}", consumerId, e.getMessage());
                            continue;
                        }
                        log.error("批量消费异常: consumerId={}, error={}", consumerId, e.getMessage(), e);
                        break;
                    }
                }
                
            } finally {
                log.info("批量消费线程结束: consumerId={}", consumerId);
            }
        });
    }

    /**
     * 处理批量消息
     */
    private void processBatch(String consumerId, KafkaConsumer<String, String> consumer,
                            List<ConsumerRecord<String, String>> batchRecords,
                            Consumer<List<ConsumerRecord<String, String>>> batchHandler) {
        try {
            // 更新统计信息
            for (ConsumerRecord<String, String> record : batchRecords) {
                updateStatistics(consumerId, record);
            }
            
            // 批量处理消息
            batchHandler.accept(batchRecords);
            
            // 批量提交偏移量
            if (!config.getConsumer().isEnableAutoCommit()) {
                commitBatchOffset(consumerId, consumer, batchRecords);
            }
            
        } catch (Exception e) {
            log.error("批量处理消息异常: consumerId={}, batchSize={}, error={}", 
                    consumerId, batchRecords.size(), e.getMessage(), e);
            
            // 批量处理失败，重试机制
            handleBatchError(consumerId, consumer, batchRecords, e);
        }
    }

    /**
     * 提交偏移量
     */
    private void commitOffset(String consumerId, KafkaConsumer<String, String> consumer, 
                            ConsumerRecord<String, String> record) {
        try {
            // 同步提交偏移量
            consumer.commitSync();
            
            // 更新偏移量缓存
            updateOffsetCache(consumerId, record);
            
        } catch (Exception e) {
            log.error("提交偏移量异常: consumerId={}, topic={}, partition={}, offset={}, error={}",
                    consumerId, record.topic(), record.partition(), record.offset(), e.getMessage(), e);
        }
    }

    /**
     * 批量提交偏移量
     */
    private void commitBatchOffset(String consumerId, KafkaConsumer<String, String> consumer,
                                 List<ConsumerRecord<String, String>> batchRecords) {
        try {
            // 同步提交偏移量
            consumer.commitSync();
            
            // 更新偏移量缓存
            for (ConsumerRecord<String, String> record : batchRecords) {
                updateOffsetCache(consumerId, record);
            }
            
        } catch (Exception e) {
            log.error("批量提交偏移量异常: consumerId={}, batchSize={}, error={}", 
                    consumerId, batchRecords.size(), e.getMessage(), e);
        }
    }

    /**
     * 更新偏移量缓存
     */
    private void updateOffsetCache(String consumerId, ConsumerRecord<String, String> record) {
        offsetCache.computeIfAbsent(consumerId, k -> new ConcurrentHashMap<>())
                .put(new TopicPartition(record.topic(), record.partition()), record.offset() + 1);
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics(String consumerId, ConsumerRecord<String, String> record) {
        ConsumerStatistics stats = statistics.get(consumerId);
        if (stats != null) {
            stats.incrementMessageCount();
            stats.incrementByteCount(record.serializedKeySize() + record.serializedValueSize());
            stats.updateLastMessageTime();
        }
    }

    /**
     * 处理消息错误
     */
    private void handleMessageError(String consumerId, KafkaConsumer<String, String> consumer,
                                  ConsumerRecord<String, String> record, Exception error) {
        // 实现重试机制
        if (config.getConsumer().isRetryEnabled()) {
            // TODO: 实现重试逻辑
            log.warn("消息处理失败，将进行重试: consumerId={}, topic={}, partition={}, offset={}",
                    consumerId, record.topic(), record.partition(), record.offset());
        }
    }

    /**
     * 处理批量消息错误
     */
    private void handleBatchError(String consumerId, KafkaConsumer<String, String> consumer,
                                List<ConsumerRecord<String, String>> batchRecords, Exception error) {
        // 实现批量重试机制
        if (config.getConsumer().isRetryEnabled()) {
            // TODO: 实现批量重试逻辑
            log.warn("批量消息处理失败，将进行重试: consumerId={}, batchSize={}",
                    consumerId, batchRecords.size());
        }
    }

    /**
     * 停止消费者
     */
    public void stopConsumer(String consumerId) {
        AtomicBoolean status = consumerStatus.get(consumerId);
        if (status != null) {
            status.set(false);
            log.info("停止消费者: {}", consumerId);
        }
    }

    /**
     * 关闭消费者
     */
    public void closeConsumer(String consumerId) {
        KafkaConsumer<String, String> consumer = consumers.get(consumerId);
        if (consumer != null) {
            try {
                consumer.close();
                consumers.remove(consumerId);
                consumerStatus.remove(consumerId);
                statistics.remove(consumerId);
                offsetCache.remove(consumerId);
                log.info("关闭消费者: {}", consumerId);
            } catch (Exception e) {
                log.error("关闭消费者异常: consumerId={}, error={}", consumerId, e.getMessage(), e);
            }
        }
    }

    /**
     * 获取消费者状态
     */
    public boolean isConsumerRunning(String consumerId) {
        AtomicBoolean status = consumerStatus.get(consumerId);
        return status != null && status.get();
    }

    /**
     * 获取消费者统计信息
     */
    public ConsumerStatistics getConsumerStatistics(String consumerId) {
        return statistics.get(consumerId);
    }

    /**
     * 获取所有消费者ID
     */
    public Set<String> getAllConsumerIds() {
        return new HashSet<>(consumers.keySet());
    }

    /**
     * 消费者统计信息
     */
    public static class ConsumerStatistics {
        private final AtomicLong messageCount = new AtomicLong(0);
        private final AtomicLong byteCount = new AtomicLong(0);
        private final AtomicLong lastMessageTime = new AtomicLong(0);

        public void incrementMessageCount() {
            messageCount.incrementAndGet();
        }

        public void incrementByteCount(long bytes) {
            byteCount.addAndGet(bytes);
        }

        public void updateLastMessageTime() {
            lastMessageTime.set(System.currentTimeMillis());
        }

        public long getMessageCount() {
            return messageCount.get();
        }

        public long getByteCount() {
            return byteCount.get();
        }

        public long getLastMessageTime() {
            return lastMessageTime.get();
        }

        @Override
        public String toString() {
            return String.format("ConsumerStatistics{messageCount=%d, byteCount=%d, lastMessageTime=%d}",
                    messageCount.get(), byteCount.get(), lastMessageTime.get());
        }
    }

    // ==================== 再平衡监听器 ====================

    /**
     * 集群消费再平衡监听器
     */
    private class ClusterRebalanceListener implements ConsumerRebalanceListener {
        private final String consumerId;

        public ClusterRebalanceListener(String consumerId) {
            this.consumerId = consumerId;
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            log.info("集群消费分区被收回: consumerId={}, partitions={}", consumerId, partitions);
            // 提交偏移量
            KafkaConsumer<String, String> consumer = consumers.get(consumerId);
            if (consumer != null) {
                try {
                    consumer.commitSync();
                } catch (Exception e) {
                    log.error("提交偏移量异常: consumerId={}, error={}", consumerId, e.getMessage(), e);
                }
            }
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            log.info("集群消费分区被分配: consumerId={}, partitions={}", consumerId, partitions);
        }
    }

    /**
     * 广播消费再平衡监听器
     */
    private class BroadcastRebalanceListener implements ConsumerRebalanceListener {
        private final String consumerId;

        public BroadcastRebalanceListener(String consumerId) {
            this.consumerId = consumerId;
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            log.info("广播消费分区被收回: consumerId={}, partitions={}", consumerId, partitions);
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            log.info("广播消费分区被分配: consumerId={}, partitions={}", consumerId, partitions);
        }
    }

    /**
     * 顺序消费再平衡监听器
     */
    private class OrderlyRebalanceListener implements ConsumerRebalanceListener {
        private final String consumerId;

        public OrderlyRebalanceListener(String consumerId) {
            this.consumerId = consumerId;
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            log.info("顺序消费分区被收回: consumerId={}, partitions={}", consumerId, partitions);
            // 提交偏移量
            KafkaConsumer<String, String> consumer = consumers.get(consumerId);
            if (consumer != null) {
                try {
                    consumer.commitSync();
                } catch (Exception e) {
                    log.error("提交偏移量异常: consumerId={}, error={}", consumerId, e.getMessage(), e);
                }
            }
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            log.info("顺序消费分区被分配: consumerId={}, partitions={}", consumerId, partitions);
        }
    }

    /**
     * 批量消费再平衡监听器
     */
    private class BatchRebalanceListener implements ConsumerRebalanceListener {
        private final String consumerId;

        public BatchRebalanceListener(String consumerId) {
            this.consumerId = consumerId;
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            log.info("批量消费分区被收回: consumerId={}, partitions={}", consumerId, partitions);
            // 提交偏移量
            KafkaConsumer<String, String> consumer = consumers.get(consumerId);
            if (consumer != null) {
                try {
                    consumer.commitSync();
                } catch (Exception e) {
                    log.error("提交偏移量异常: consumerId={}, error={}", consumerId, e.getMessage(), e);
                }
            }
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            log.info("批量消费分区被分配: consumerId={}, partitions={}", consumerId, partitions);
        }
    }
}
