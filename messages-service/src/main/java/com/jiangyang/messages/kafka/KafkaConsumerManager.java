package com.jiangyang.messages.kafka;

import com.alibaba.fastjson2.JSON;
import com.jiangyang.messages.MessageListener;
import com.jiangyang.messages.MessageServiceException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka消费者管理类
 * 负责消费者的创建、订阅、取消订阅以及消费逻辑的管理
 * 实现集群消费、并发消费及高级特性
 */
@Slf4j
public class KafkaConsumerManager implements InitializingBean, DisposableBean {

    @Setter
    private String bootstrapServers = "localhost:9092";

    @Setter
    private int minConsumeThreads = 20;

    @Setter
    private int maxConsumeThreads = 64;

    @Setter
    private int pollTimeoutMs = 100;

    @Setter
    private int maxPollRecords = 500;

    @Setter
    private boolean enableAutoCommit = true;

    @Setter
    private int autoCommitIntervalMs = 1000;

    private final Map<String, KafkaConsumer<String, String>> consumers = new ConcurrentHashMap<>();
    private final Map<String, Thread> consumerThreads = new ConcurrentHashMap<>();
    private final Map<String, MessageListener<?>> listeners = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> consumerExecutors = new ConcurrentHashMap<>();
    private final AtomicLong backlogCount = new AtomicLong(0);

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("KafkaConsumerManager initialized");
    }

    @Override
    public void destroy() throws Exception {
        // 关闭所有消费者
        for (KafkaConsumer<String, String> consumer : consumers.values()) {
            if (consumer != null) {
                consumer.close();
            }
        }
        consumers.clear();
        listeners.clear();
        
        // 停止所有消费者线程
        for (Thread thread : consumerThreads.values()) {
            if (thread != null) {
                thread.interrupt();
            }
        }
        consumerThreads.clear();
        
        // 关闭所有线程池
        for (ExecutorService executor : consumerExecutors.values()) {
            if (executor != null) {
                executor.shutdown();
            }
        }
        consumerExecutors.clear();
        log.info("KafkaConsumerManager shutdown");
    }

    /**
     * 订阅主题
     * @param topic 主题
     * @param consumerGroup 消费者组
     * @param listener 消息监听器
     * @param <T> 消息类型
     * @return 是否订阅成功
     */
    public <T> boolean subscribe(String topic, String consumerGroup, MessageListener<T> listener) {
        String consumerKey = generateConsumerKey(topic, consumerGroup);
        if (consumers.containsKey(consumerKey)) {
            log.warn("Consumer already subscribed for topic: {}, group: {}", topic, consumerGroup);
            return false;
        }

        try {
            KafkaConsumer<String, String> consumer = createConsumer(consumerGroup);
            consumer.subscribe(Arrays.asList(topic));
            consumers.put(consumerKey, consumer);
            listeners.put(consumerKey, listener);
            
            // 为每个消费者创建独立的线程池，用于处理IO密集型操作
            ExecutorService executor = Executors.newFixedThreadPool(maxConsumeThreads);
            consumerExecutors.put(consumerKey, executor);
            
            // 启动消费者线程
            startConsumerThread(consumerKey, consumer, listener, executor);
            log.info("Subscribed to topic: {}, group: {}", topic, consumerGroup);
            return true;
        } catch (Exception e) {
            log.error("Failed to subscribe to topic: {}, group: {}", topic, consumerGroup, e);
            return false;
        }
    }

    /**
     * 取消订阅
     * @param topic 主题
     * @param consumerGroup 消费者组
     * @return 是否取消成功
     */
    public boolean unsubscribe(String topic, String consumerGroup) {
        String consumerKey = generateConsumerKey(topic, consumerGroup);
        KafkaConsumer<String, String> consumer = consumers.remove(consumerKey);
        if (consumer != null) {
            consumer.close();
            listeners.remove(consumerKey);
            
            Thread consumerThread = consumerThreads.remove(consumerKey);
            if (consumerThread != null) {
                consumerThread.interrupt();
            }
            
            ExecutorService executor = consumerExecutors.remove(consumerKey);
            if (executor != null) {
                executor.shutdown();
            }
            log.info("Unsubscribed from topic: {}, group: {}", topic, consumerGroup);
            return true;
        }
        log.warn("No subscription found for topic: {}, group: {}", topic, consumerGroup);
        return false;
    }

    /**
     * 创建消费者
     */
    private KafkaConsumer<String, String> createConsumer(String consumerGroup) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", consumerGroup);
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        props.put("enable.auto.commit", enableAutoCommit);
        props.put("auto.commit.interval.ms", autoCommitIntervalMs);
        props.put("max.poll.records", maxPollRecords);
        return new KafkaConsumer<>(props);
    }

    /**
     * 启动消费者线程
     */
    private <T> void startConsumerThread(String consumerKey, KafkaConsumer<String, String> consumer, 
                                       MessageListener<T> listener, ExecutorService executor) {
        Thread consumerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollTimeoutMs));
                    if (!records.isEmpty()) {
                        // 流量控制
                        if (backlogCount.get() > 1000) {
                            log.warn("Consumer backlog too high, pausing consumption. Backlog: {}", backlogCount.get());
                            Thread.sleep(100);
                            continue;
                        }
                        
                        backlogCount.addAndGet(records.count());
                        for (ConsumerRecord<String, String> record : records) {
                            // 提交到线程池处理，防止阻塞消费线程
                            executor.execute(() -> {
                                try {
                                    processMessage(record, listener);
                                } catch (Exception e) {
                                    log.error("Error processing message, key: {}, topic: {}, partition: {}, offset: {}", 
                                            record.key(), record.topic(), record.partition(), record.offset(), e);
                                } finally {
                                    backlogCount.decrementAndGet();
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Kafka consumer error for key: {}", consumerKey, e);
            } finally {
                consumer.close();
                log.info("Kafka consumer closed for key: {}", consumerKey);
            }
        });
        consumerThread.setDaemon(true);
        consumerThread.setName("Kafka-Consumer-" + consumerKey);
        consumerThread.start();
        consumerThreads.put(consumerKey, consumerThread);
        log.info("Kafka consumer thread started for key: {}", consumerKey);
    }

    /**
     * 处理单条消息
     */
    private <T> void processMessage(ConsumerRecord<String, String> record, MessageListener<T> listener) {
        try {
            String messageBody = record.value();
            // 使用泛型类型进行反序列化
            Class<T> messageType = getMessageType(listener);
            T message = JSON.parseObject(messageBody, messageType);
            log.debug("Processing message, key: {}, topic: {}, partition: {}, offset: {}", 
                    record.key(), record.topic(), record.partition(), record.offset());
            listener.onMessage(message);
            log.debug("Message processed successfully, key: {}, topic: {}, partition: {}, offset: {}", 
                    record.key(), record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            log.error("Failed to process message, key: {}, topic: {}, partition: {}, offset: {}", 
                    record.key(), record.topic(), record.partition(), record.offset(), e);
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
