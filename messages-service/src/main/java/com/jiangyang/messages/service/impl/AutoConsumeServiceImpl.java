package com.jiangyang.messages.service.impl;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.entity.MessageConsumerConfig;
import com.jiangyang.messages.service.AutoConsumeService;
import com.jiangyang.messages.service.MessageConsumerConfigService;
import com.jiangyang.messages.utils.ConsumeMode;
import com.jiangyang.messages.utils.ConsumeOrder;
import com.jiangyang.messages.utils.ConsumeType;
import com.jiangyang.messages.utils.MessageServiceType;
import com.jiangyang.messages.rabbitmq.RabbitMQConsumerManager;
import com.jiangyang.messages.config.KafkaConfig;
import com.jiangyang.messages.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 自动消费服务实现类
 * 根据配置自动启动消息消费
 */
@Slf4j
@Service
@DataSource("master")
public class AutoConsumeServiceImpl implements AutoConsumeService {

    @Autowired
    private MessageConsumerConfigService messageConsumerConfigService;
    
    @Autowired
    private KafkaConfig kafkaConfig;
    
    @Autowired
    private RabbitMQConsumerManager rabbitMQConsumerManager;

    /**
     * 消费器映射表：serviceName -> consumer
     */
    private final Map<String, Object> consumers = new ConcurrentHashMap<>();

    /**
     * 消费状态映射表：serviceName -> isConsuming
     */
    private final Map<String, Boolean> consumeStatus = new ConcurrentHashMap<>();

    /**
     * Kafka消费者映射表：serviceName -> KafkaConsumer
     */
    private final Map<String, KafkaConsumer<String, String>> kafkaConsumers = new ConcurrentHashMap<>();

    /**
     * RabbitMQ消费者映射表：serviceName -> ConfigBasedConsumer
     */
    private final Map<String, RabbitMQConsumerManager.ConfigBasedConsumer> rabbitMQConsumers = new ConcurrentHashMap<>();

    /**
     * 消费者线程映射表：serviceName -> Thread
     */
    private final Map<String, Thread> consumerThreads = new ConcurrentHashMap<>();

    /**
     * 消费者停止标志：serviceName -> stopFlag
     */
    private final Map<String, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();

    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 是否已启动
     */
    private volatile boolean started = false;

    @PostConstruct
    public void init() {
        log.info("自动消费服务初始化完成");
    }

    @PreDestroy
    public void destroy() {
        stopAutoConsume();
        scheduler.shutdown();
        log.info("自动消费服务已销毁");
    }

    @Override
    public void startAutoConsume() {
        if (started) {
            log.warn("自动消费服务已经启动");
            return;
        }

        try {
            log.info("开始启动自动消费服务...");
            
            // 获取所有启用的消费配置
            List<MessageConsumerConfig> configs = messageConsumerConfigService.getEnabledConfigs();
            
            for (MessageConsumerConfig config : configs) {
                try {
                    startConsumeByConfig(config);
                } catch (Exception e) {
                    log.error("启动消费配置失败: serviceName={}, error={}", 
                            config.getServiceName(), e.getMessage(), e);
                }
            }

            // 启动定时任务，定期检查配置变更
            startConfigMonitor();
            
            started = true;
            log.info("自动消费服务启动成功，共启动 {} 个消费配置", configs.size());
            
        } catch (Exception e) {
            log.error("启动自动消费服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("启动自动消费服务失败", e);
        }
    }

    @Override
    public void stopAutoConsume() {
        if (!started) {
            log.warn("自动消费服务未启动");
            return;
        }

        try {
            log.info("开始停止自动消费服务...");
            
            // 停止所有消费者
            for (String serviceName : consumers.keySet()) {
                try {
                    stopConsumeByServiceName(serviceName);
                } catch (Exception e) {
                    log.error("停止服务 {} 的消费失败: {}", serviceName, e.getMessage(), e);
                }
            }
            
            // 清空状态
            consumers.clear();
            consumeStatus.clear();
            kafkaConsumers.clear();
            rabbitMQConsumers.clear();
            consumerThreads.clear();
            stopFlags.clear();
            
            started = false;
            log.info("自动消费服务已停止");
            
        } catch (Exception e) {
            log.error("停止自动消费服务失败: {}", e.getMessage(), e);
            throw new RuntimeException("停止自动消费服务失败", e);
        }
    }

    @Override
    public void reloadConsumeConfig() {
        try {
            log.info("开始重新加载消费配置...");
            
            // 停止所有现有消费者
            stopAutoConsume();
            
            // 重新启动
            startAutoConsume();
            
            log.info("消费配置重新加载完成");
            
        } catch (Exception e) {
            log.error("重新加载消费配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("重新加载消费配置失败", e);
        }
    }

    @Override
    public boolean isConsuming(String serviceName) {
        return consumeStatus.getOrDefault(serviceName, false);
    }

    @Override
    public void startConsumeByService(String serviceName) {
        try {
            List<MessageConsumerConfig> configs = messageConsumerConfigService.getConfigsByService(serviceName);
            
            for (MessageConsumerConfig config : configs) {
                if (config.getEnabled()) {
                    startConsumeByConfig(config);
                }
            }
            
            log.info("服务 {} 的消费配置启动完成", serviceName);
            
        } catch (Exception e) {
            log.error("启动服务 {} 的消费配置失败: {}", serviceName, e.getMessage(), e);
            throw new RuntimeException("启动消费配置失败", e);
        }
    }

    @Override
    public void stopConsumeByService(String serviceName) {
        try {
            stopConsumeByServiceName(serviceName);
            log.info("服务 {} 的消费已停止", serviceName);
        } catch (Exception e) {
            log.error("停止服务 {} 的消费失败: {}", serviceName, e.getMessage(), e);
        }
    }

    @Override
    public List<MessageConsumerConfig> getAllConsumeConfigs() {
        return messageConsumerConfigService.getAllConfigs();
    }

    @Override
    public List<MessageConsumerConfig> getConsumeConfigsByService(String serviceName) {
        return messageConsumerConfigService.getConfigsByService(serviceName);
    }

    @Override
    public Object getConsumeStatistics(String serviceName) {
        // 这里可以实现具体的统计逻辑
        return Map.of(
            "serviceName", serviceName,
            "isConsuming", isConsuming(serviceName),
            "consumerCount", consumers.containsKey(serviceName) ? 1 : 0,
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 根据配置启动消费
     */
    private void startConsumeByConfig(MessageConsumerConfig config) {
        String serviceName = config.getServiceName();
        String messageQueueType = config.getMessageQueueType();
        
        try {
            Object consumer = null;
            
            switch (MessageServiceType.fromCode(messageQueueType.toUpperCase())) {
                case ROCKETMQ:
                    consumer = createRocketMQConsumer(config);
                    break;
                case KAFKA:
                    consumer = createKafkaConsumer(config);
                    break;
                case RABBITMQ:
                    consumer = createRabbitMQConsumer(config);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的消息中间件类型: " + messageQueueType);
            }
            
            if (consumer != null) {
                consumers.put(serviceName, consumer);
                consumeStatus.put(serviceName, true);
                log.info("服务 {} 的消费配置启动成功: messageQueueType={}, consumeMode={}, consumeType={}, consumeOrder={}", 
                        serviceName, messageQueueType, config.getConsumeMode(), config.getConsumeType(), config.getConsumeOrder());
            }
            
        } catch (Exception e) {
            log.error("启动服务 {} 的消费配置失败: {}", serviceName, e.getMessage(), e);
            throw new RuntimeException("启动消费配置失败", e);
        }
    }

    /**
     * 创建RocketMQ消费者
     */
    private Object createRocketMQConsumer(MessageConsumerConfig config) {
        // 这里实现RocketMQ消费者的创建逻辑
        // 根据消费模式、消费类型、顺序性等配置创建相应的消费者
        log.info("创建RocketMQ消费者: serviceName={}, consumeMode={}, consumeType={}, consumeOrder={}", 
                config.getServiceName(), config.getConsumeMode(), config.getConsumeType(), config.getConsumeOrder());
        
        // TODO: 实现具体的RocketMQ消费者创建逻辑
        return new Object(); // 临时返回，实际应该返回真正的消费者对象
    }

    /**
     * 创建Kafka消费者
     */
    private Object createKafkaConsumer(MessageConsumerConfig config) {
        String serviceName = config.getServiceName();
        log.info("创建Kafka消费者: serviceName={}, consumeMode={}, consumeType={}, consumeOrder={}", 
                serviceName, config.getConsumeMode(), config.getConsumeType(), config.getConsumeOrder());
        
        try {
            // 创建Kafka消费者配置
            Properties props = createKafkaConsumerProperties(config);
            
            // 创建Kafka消费者
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            
            // 订阅主题
            if (config.getTag() != null && !config.getTag().isEmpty()) {
                // 如果有tag，使用正则表达式订阅
                consumer.subscribe(Collections.singletonList(config.getTopic()), 
                    new ConsumerRebalanceListener() {
                        @Override
                        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                            log.info("服务 {} 的分区被撤销: {}", serviceName, partitions);
                        }
                        
                        @Override
                        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                            log.info("服务 {} 的分区被分配: {}", serviceName, partitions);
                        }
                    });
            } else {
                consumer.subscribe(Collections.singletonList(config.getTopic()));
            }
            
            // 存储消费者引用
            kafkaConsumers.put(serviceName, consumer);
            
            // 创建停止标志
            AtomicBoolean stopFlag = new AtomicBoolean(false);
            stopFlags.put(serviceName, stopFlag);
            
            // 启动消费线程
            startKafkaConsumeThread(serviceName, consumer, config, stopFlag);
            
            return consumer;
            
        } catch (Exception e) {
            log.error("创建Kafka消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
            throw new RuntimeException("创建Kafka消费者失败", e);
        }
    }

    /**
     * 创建RabbitMQ消费者
     */
    private Object createRabbitMQConsumer(MessageConsumerConfig config) {
        String serviceName = config.getServiceName();
        log.info("创建RabbitMQ消费者: serviceName={}, consumeMode={}, consumeType={}, consumeOrder={}", 
                serviceName, config.getConsumeMode(), config.getConsumeType(), config.getConsumeOrder());
        
        try {
            // 创建RabbitMQ消费者
            RabbitMQConsumerManager.ConfigBasedConsumer rabbitMQConsumer = 
                rabbitMQConsumerManager.createConfigBasedConsumer(config);
            
            // 存储消费者引用
            rabbitMQConsumers.put(serviceName, rabbitMQConsumer);
            
            // 创建停止标志
            AtomicBoolean stopFlag = new AtomicBoolean(false);
            stopFlags.put(serviceName, stopFlag);
            
            // 启动消费线程
            startRabbitMQConsumeThread(serviceName, rabbitMQConsumer, config, stopFlag);
            
            return rabbitMQConsumer;
            
        } catch (Exception e) {
            log.error("创建RabbitMQ消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
            throw new RuntimeException("创建RabbitMQ消费者失败", e);
        }
    }

    /**
     * 启动RabbitMQ消费线程
     */
    private void startRabbitMQConsumeThread(String serviceName, RabbitMQConsumerManager.ConfigBasedConsumer consumer, 
                                          MessageConsumerConfig config, AtomicBoolean stopFlag) {
        Thread consumeThread = new Thread(() -> {
            log.info("RabbitMQ消费线程启动: serviceName={}, queue={}, exchange={}", 
                    serviceName, config.getTopic(), config.getExchange());
            
            try {
                while (!stopFlag.get()) {
                    try {
                        // 根据消费模式选择推模式或拉模式
                        if (ConsumeMode.PUSH.name().equals(config.getConsumeMode())) {
                            // 推模式：启动推模式消费
                            consumer.startPushMode();
                        } else {
                            // 拉模式：启动拉模式消费
                            consumer.startPullMode();
                        }
                        
                        // 等待一段时间
                        Thread.sleep(1000);
                        
                    } catch (Exception e) {
                        log.error("服务 {} RabbitMQ消费时发生错误: {}", serviceName, e.getMessage(), e);
                        Thread.sleep(5000); // 发生错误时等待5秒再重试
                    }
                }
                
            } catch (Exception e) {
                log.error("RabbitMQ消费线程异常退出: serviceName={}, error={}", serviceName, e.getMessage(), e);
            } finally {
                try {
                    consumer.shutdown();
                    log.info("RabbitMQ消费者已关闭: serviceName={}", serviceName);
                } catch (Exception e) {
                    log.error("关闭RabbitMQ消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
                }
            }
        }, "rabbitmq-consumer-" + serviceName);
        
        consumeThread.setDaemon(true);
        consumeThread.start();
        
        // 存储线程引用
        consumerThreads.put(serviceName, consumeThread);
    }

    /**
     * 创建Kafka消费者配置
     */
    private Properties createKafkaConsumerProperties(MessageConsumerConfig config) {
        Properties props = new Properties();
        
        // 基础配置
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers().getServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getConsumerGroup() != null ? config.getConsumerGroup() : kafkaConfig.getConsumer().getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConfig.getConsumer().getKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConfig.getConsumer().getValueDeserializer());
        
        // 消费类型配置
        if (ConsumeType.BROADCASTING.name().equals(config.getConsumeType())) {
            // 广播消费：每个消费者都有独立的消费组
            props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getConsumerGroup() + "_" + config.getInstanceId());
        }
        
        // 偏移量管理配置
        if (config.getConsumeMode() != null && "MANUAL".equals(config.getConsumeMode())) {
            // 手动提交偏移量
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        } else {
            // 自动提交偏移量
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(kafkaConfig.getConsumer().isEnableAutoCommit()));
            props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, String.valueOf(kafkaConfig.getConsumer().getAutoCommitInterval()));
        }
        
        // 顺序性配置
        if (ConsumeOrder.ORDERLY.name().equals(config.getConsumeOrder())) {
            // 顺序消费：确保分区内消息顺序
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        } else {
            // 并发消费：批量拉取提高性能
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 
                    String.valueOf(config.getBatchSize() != null ? config.getBatchSize() : kafkaConfig.getConsumer().getMaxPollRecords()));
        }
        
        // 超时配置
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, String.valueOf(kafkaConfig.getConsumer().getSessionTimeout()));
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, String.valueOf(kafkaConfig.getConsumer().getHeartbeatInterval()));
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(kafkaConfig.getConsumer().getRequestTimeout()));
        
        // 重试配置
        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, String.valueOf(kafkaConfig.getConsumer().getRetryBackoff()));
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, String.valueOf(kafkaConfig.getConsumer().getMaxPollInterval()));
        
        // 分区分配策略
        if (ConsumeOrder.ORDERLY.name().equals(config.getConsumeOrder())) {
            // 顺序消费使用StickyAssignor确保分区分配稳定
            props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, 
                    "org.apache.kafka.clients.consumer.StickyAssignor");
        } else {
            // 使用配置的分区分配策略
            props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, 
                    kafkaConfig.getConsumer().getPartitionAssignmentStrategy());
        }
        
        return props;
    }

    /**
     * 启动Kafka消费线程
     */
    private void startKafkaConsumeThread(String serviceName, KafkaConsumer<String, String> consumer, 
                                       MessageConsumerConfig config, AtomicBoolean stopFlag) {
        Thread consumeThread = new Thread(() -> {
            log.info("Kafka消费线程启动: serviceName={}, topic={}, consumerGroup={}", 
                    serviceName, config.getTopic(), config.getConsumerGroup());
            
            try {
                while (!stopFlag.get()) {
                    try {
                        // 拉取消息
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                        
                        if (!records.isEmpty()) {
                            log.info("服务 {} 拉取到 {} 条消息", serviceName, records.count());
                            
                            // 处理消息
                            processKafkaMessages(serviceName, records, config);
                            
                            // 手动提交偏移量（如果配置为手动提交）
                            if (config.getConsumeMode() != null && "MANUAL".equals(config.getConsumeMode())) {
                                consumer.commitSync();
                                log.debug("服务 {} 手动提交偏移量成功", serviceName);
                            }
                        }
                        
                    } catch (Exception e) {
                        log.error("服务 {} 消费消息时发生错误: {}", serviceName, e.getMessage(), e);
                        
                        // 如果是手动提交模式，发生错误时回滚偏移量
                        if (config.getConsumeMode() != null && "MANUAL".equals(config.getConsumeMode())) {
                            try {
                                consumer.commitSync();
                                log.warn("服务 {} 发生错误后提交偏移量", serviceName);
                            } catch (Exception commitEx) {
                                log.error("服务 {} 提交偏移量失败: {}", serviceName, commitEx.getMessage(), commitEx);
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                log.error("Kafka消费线程异常退出: serviceName={}, error={}", serviceName, e.getMessage(), e);
            } finally {
                try {
                    consumer.close();
                    log.info("Kafka消费者已关闭: serviceName={}", serviceName);
                } catch (Exception e) {
                    log.error("关闭Kafka消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
                }
            }
        }, "kafka-consumer-" + serviceName);
        
        consumeThread.setDaemon(true);
        consumeThread.start();
        
        // 存储线程引用
        consumerThreads.put(serviceName, consumeThread);
    }

    /**
     * 处理Kafka消息
     */
    private void processKafkaMessages(String serviceName, ConsumerRecords<String, String> records, 
                                    MessageConsumerConfig config) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                log.info("处理消息: serviceName={}, topic={}, partition={}, offset={}, key={}, value={}", 
                        serviceName, record.topic(), record.partition(), record.offset(), 
                        record.key(), record.value());
                
                // 根据消息类型进行不同的处理
                String messageType = determineMessageType(record);
                
                switch (messageType) {
                    case "SYNC_BLOCKING":
                        processSyncBlockingMessage(serviceName, record, config);
                        break;
                    case "ASYNC_CALLBACK":
                        processAsyncCallbackMessage(serviceName, record, config);
                        break;
                    case "ASYNC_NO_CALLBACK":
                        processAsyncNoCallbackMessage(serviceName, record, config);
                        break;
                    default:
                        processDefaultMessage(serviceName, record, config);
                        break;
                }
                
            } catch (Exception e) {
                log.error("处理消息失败: serviceName={}, topic={}, partition={}, offset={}, error={}", 
                        serviceName, record.topic(), record.partition(), record.offset(), e.getMessage(), e);
                
                // 根据重试策略处理失败的消息
                handleFailedMessage(serviceName, record, config, e);
            }
        }
    }

    /**
     * 确定消息类型
     */
    private String determineMessageType(ConsumerRecord<String, String> record) {
        try {
            // 从消息头或值中解析消息类型
            if (record.headers() != null) {
                for (org.apache.kafka.common.header.Header header : record.headers()) {
                    if ("messageType".equals(header.key())) {
                        return new String(header.value());
                    }
                }
            }
            
            // 从消息值中解析（假设是JSON格式）
            if (record.value() != null && record.value().contains("\"messageType\"")) {
                // 简单的JSON解析，实际项目中建议使用JSON库
                if (record.value().contains("SYNC_BLOCKING")) return "SYNC_BLOCKING";
                if (record.value().contains("ASYNC_CALLBACK")) return "ASYNC_CALLBACK";
                if (record.value().contains("ASYNC_NO_CALLBACK")) return "ASYNC_NO_CALLBACK";
            }
            
        } catch (Exception e) {
            log.warn("解析消息类型失败: {}", e.getMessage());
        }
        
        return "DEFAULT";
    }

    /**
     * 处理同步阻塞消息
     */
    private void processSyncBlockingMessage(String serviceName, ConsumerRecord<String, String> record, 
                                          MessageConsumerConfig config) {
        log.info("处理同步阻塞消息: serviceName={}, key={}", serviceName, record.key());
        // TODO: 实现同步阻塞消息处理逻辑
        // 适用于：交易订单、核心业务数据
    }

    /**
     * 处理异步回调消息
     */
    private void processAsyncCallbackMessage(String serviceName, ConsumerRecord<String, String> record, 
                                          MessageConsumerConfig config) {
        log.info("处理异步回调消息: serviceName={}, key={}", serviceName, record.key());
        // TODO: 实现异步回调消息处理逻辑
        // 适用于：日志采集、业务通知
    }

    /**
     * 处理无回调异步消息
     */
    private void processAsyncNoCallbackMessage(String serviceName, ConsumerRecord<String, String> record, 
                                            MessageConsumerConfig config) {
        log.info("处理无回调异步消息: serviceName={}, key={}", serviceName, record.key());
        // TODO: 实现无回调异步消息处理逻辑
        // 适用于：监控指标、非关键日志
    }

    /**
     * 处理默认消息
     */
    private void processDefaultMessage(String serviceName, ConsumerRecord<String, String> record, 
                                    MessageConsumerConfig config) {
        log.info("处理默认消息: serviceName={}, key={}", serviceName, record.key());
        // TODO: 实现默认消息处理逻辑
    }

    /**
     * 处理失败的消息
     */
    private void handleFailedMessage(String serviceName, ConsumerRecord<String, String> record, 
                                   MessageConsumerConfig config, Exception error) {
        log.warn("处理失败的消息: serviceName={}, topic={}, partition={}, offset={}, error={}", 
                serviceName, record.topic(), record.partition(), record.offset(), error.getMessage());
        
        // TODO: 实现失败消息处理逻辑
        // 1. 记录失败日志
        // 2. 发送到死信队列
        // 3. 重试机制
        // 4. 告警通知
    }

    /**
     * 停止消费者
     */
    private void stopConsumer(Object consumer, String serviceName) {
        try {
            if (consumer instanceof KafkaConsumer) {
                // 停止Kafka消费者
                stopKafkaConsumer(serviceName);
            } else if (consumer instanceof RabbitMQConsumerManager.ConfigBasedConsumer) {
                // 停止RabbitMQ消费者
                stopRabbitMQConsumer(serviceName);
            } else {
                // TODO: 根据消费者类型调用相应的停止方法
                log.info("消费者已停止: serviceName={}", serviceName);
            }
        } catch (Exception e) {
            log.error("停止消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
        }
    }

    /**
     * 停止Kafka消费者
     */
    private void stopKafkaConsumer(String serviceName) {
        try {
            // 设置停止标志
            AtomicBoolean stopFlag = stopFlags.get(serviceName);
            if (stopFlag != null) {
                stopFlag.set(true);
            }
            
            // 等待消费线程结束
            Thread consumeThread = consumerThreads.get(serviceName);
            if (consumeThread != null && consumeThread.isAlive()) {
                consumeThread.join(5000); // 等待最多5秒
            }
            
            // 关闭Kafka消费者
            KafkaConsumer<String, String> consumer = kafkaConsumers.get(serviceName);
            if (consumer != null) {
                consumer.close();
                kafkaConsumers.remove(serviceName);
            }
            
            // 清理资源
            consumerThreads.remove(serviceName);
            stopFlags.remove(serviceName);
            
            log.info("Kafka消费者已停止: serviceName={}", serviceName);
            
        } catch (Exception e) {
            log.error("停止Kafka消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
        }
    }

    /**
     * 停止RabbitMQ消费者
     */
    private void stopRabbitMQConsumer(String serviceName) {
        try {
            // 设置停止标志
            AtomicBoolean stopFlag = stopFlags.get(serviceName);
            if (stopFlag != null) {
                stopFlag.set(true);
            }
            
            // 等待消费线程结束
            Thread consumeThread = consumerThreads.get(serviceName);
            if (consumeThread != null && consumeThread.isAlive()) {
                consumeThread.join(5000); // 等待最多5秒
            }
            
            // 关闭RabbitMQ消费者
            RabbitMQConsumerManager.ConfigBasedConsumer consumer = rabbitMQConsumers.get(serviceName);
            if (consumer != null) {
                consumer.shutdown();
                rabbitMQConsumers.remove(serviceName);
            }
            
            // 清理资源
            consumerThreads.remove(serviceName);
            stopFlags.remove(serviceName);
            
            log.info("RabbitMQ消费者已停止: serviceName={}", serviceName);
            
        } catch (Exception e) {
            log.error("停止RabbitMQ消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
        }
    }

    /**
     * 根据服务名停止消费
     */
    private void stopConsumeByServiceName(String serviceName) {
        Object consumer = consumers.get(serviceName);
        if (consumer != null) {
            stopConsumer(consumer, serviceName);
            consumers.remove(serviceName);
            consumeStatus.put(serviceName, false);
        }
    }

    /**
     * 启动配置监控定时任务
     */
    private void startConfigMonitor() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                // 检查配置是否有变更，如果有则重新加载
                if (messageConsumerConfigService.hasConfigChanged()) {
                    log.info("检测到消费配置变更，开始重新加载...");
                    reloadConsumeConfig();
                }
            } catch (Exception e) {
                log.error("配置监控任务执行失败: {}", e.getMessage(), e);
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        log.info("配置监控定时任务已启动");
    }

}
