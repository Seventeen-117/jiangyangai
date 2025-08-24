package com.jiangyang.messages.service.impl;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.entity.MessageConsumerConfig;
import com.jiangyang.messages.service.AutoConsumeService;
import com.jiangyang.messages.service.MessageConsumerConfigService;
import com.jiangyang.messages.consume.ConsumeMode;
import com.jiangyang.messages.consume.ConsumeOrder;
import com.jiangyang.messages.consume.ConsumeType;
import com.jiangyang.messages.consume.MessageServiceType;
import com.jiangyang.messages.rabbitmq.RabbitMQConsumerManager;
import com.jiangyang.messages.rocketmq.RocketMQConsumerManager;
import com.jiangyang.messages.config.KafkaConfig;
import com.jiangyang.messages.config.ServiceCallbackConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 自动消费服务实现类
 * 根据配置自动启动消息消费，并将消息转发给配置的消费者服务
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

    @Autowired
    private RocketMQConsumerManager rocketMQConsumerManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ServiceCallbackConfig serviceCallbackConfig;

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
     * RocketMQ消费者映射表：serviceName -> ConfigBasedConsumer
     */
    private final Map<String, RocketMQConsumerManager.ConfigBasedConsumer> rocketMQConsumers = new ConcurrentHashMap<>();

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
            rocketMQConsumers.clear();
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
        String serviceName = config.getServiceName();
        log.info("创建RocketMQ消费者: serviceName={}, consumeMode={}, consumeType={}, consumeOrder={}", 
                config.getServiceName(), config.getConsumeMode(), config.getConsumeType(), config.getConsumeOrder());
        
        try {
            // 使用RocketMQConsumerManager创建基于配置的消费者
            RocketMQConsumerManager.ConfigBasedConsumer rocketMQConsumer = 
                rocketMQConsumerManager.createConfigBasedConsumer(config);
            
            // 存储消费者引用
            rocketMQConsumers.put(serviceName, rocketMQConsumer);
            
            // 根据消费模式启动消费者
            if (ConsumeMode.PUSH.name().equals(config.getConsumeMode())) {
                rocketMQConsumer.startPushMode();
            } else {
                rocketMQConsumer.startPullMode();
            }
            
            log.info("RocketMQ消费者创建成功: serviceName={}, topic={}, consumerGroup={}, consumeMode={}", 
                    serviceName, config.getTopic(), config.getConsumerGroup(), config.getConsumeMode());
            
            return rocketMQConsumer;
            
        } catch (Exception e) {
            log.error("创建RocketMQ消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
            throw new RuntimeException("创建RocketMQ消费者失败", e);
        }
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
                
                // 关键：将消息转发给配置的消费者服务
                forwardMessageToConsumerService(serviceName, record, config);
                
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
     * 适用于：交易订单、核心业务数据
     * 特点：高可靠性，同步处理，需要确认结果
     */
    private void processSyncBlockingMessage(String serviceName, ConsumerRecord<String, String> record, 
                                          MessageConsumerConfig config) {
        log.info("处理同步阻塞消息: serviceName={}, key={}, topic={}, partition={}, offset={}", 
                serviceName, record.key(), record.topic(), record.partition(), record.offset());
        
        try {
            // 1. 消息预处理和验证
            if (!validateMessage(record)) {
                log.warn("同步阻塞消息验证失败，跳过处理: serviceName={}, key={}", serviceName, record.key());
                return;
            }
            
            // 2. 业务逻辑处理（同步执行）
            boolean processResult = executeBusinessLogic(serviceName, record, config);
            
            if (processResult) {
                log.info("同步阻塞消息处理成功: serviceName={}, key={}", serviceName, record.key());
                
                // 3. 记录成功处理日志
                recordSuccessLog(serviceName, record, config, "SYNC_BLOCKING");
                
                // 4. 发送处理成功通知（可选）
                sendSuccessNotification(serviceName, record, config);
                
            } else {
                log.error("同步阻塞消息处理失败: serviceName={}, key={}", serviceName, record.key());
                
                // 5. 记录失败日志
                recordFailureLog(serviceName, record, config, "SYNC_BLOCKING", "业务处理失败");
                
                // 6. 发送失败告警
                sendFailureAlert(serviceName, record, config, "业务处理失败");
            }
            
        } catch (Exception e) {
            log.error("处理同步阻塞消息异常: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
            
            // 7. 记录异常日志
            recordFailureLog(serviceName, record, config, "SYNC_BLOCKING", e.getMessage());
            
            // 8. 发送异常告警
            sendFailureAlert(serviceName, record, config, e.getMessage());
            
            // 9. 重新抛出异常，触发重试机制
            throw e;
        }
    }

    /**
     * 处理异步回调消息
     * 适用于：日志采集、业务通知
     * 特点：非阻塞，带回调确认，中等可靠性
     */
    private void processAsyncCallbackMessage(String serviceName, ConsumerRecord<String, String> record, 
                                          MessageConsumerConfig config) {
        log.info("处理异步回调消息: serviceName={}, key={}, topic={}, partition={}, offset={}", 
                serviceName, record.key(), record.topic(), record.partition(), record.offset());
        
        try {
            // 1. 消息预处理和验证
            if (!validateMessage(record)) {
                log.warn("异步回调消息验证失败，跳过处理: serviceName={}, key={}", serviceName, record.key());
                return;
            }
            
            // 2. 异步处理业务逻辑
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return executeBusinessLogic(serviceName, record, config);
                } catch (Exception e) {
                    log.error("异步回调消息业务处理异常: serviceName={}, key={}, error={}", 
                            serviceName, record.key(), e.getMessage(), e);
                    return false;
                }
            });
            
            // 3. 设置回调处理
            future.thenAcceptAsync(result -> {
                try {
                    if (result) {
                        log.info("异步回调消息处理成功: serviceName={}, key={}", serviceName, record.key());
                        
                        // 4. 记录成功处理日志
                        recordSuccessLog(serviceName, record, config, "ASYNC_CALLBACK");
                        
                        // 5. 执行成功回调
                        executeSuccessCallback(serviceName, record, config);
                        
                        // 6. 发送处理成功通知
                        sendSuccessNotification(serviceName, record, config);
                        
                    } else {
                        log.error("异步回调消息处理失败: serviceName={}, key={}", serviceName, record.key());
                        
                        // 7. 记录失败日志
                        recordFailureLog(serviceName, record, config, "ASYNC_CALLBACK", "业务处理失败");
                        
                        // 8. 执行失败回调
                        executeFailureCallback(serviceName, record, config, "业务处理失败");
                        
                        // 9. 发送失败告警
                        sendFailureAlert(serviceName, record, config, "业务处理失败");
                    }
                } catch (Exception e) {
                    log.error("异步回调消息回调处理异常: serviceName={}, key={}, error={}", 
                            serviceName, record.key(), e.getMessage(), e);
                }
            });
            
            // 10. 设置超时处理
            future.orTimeout(30, TimeUnit.SECONDS).exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    log.error("异步回调消息处理超时: serviceName={}, key={}", serviceName, record.key());
                    
                    // 11. 记录超时日志
                    recordFailureLog(serviceName, record, config, "ASYNC_CALLBACK", "处理超时");
                    
                    // 12. 执行超时回调
                    executeTimeoutCallback(serviceName, record, config);
                    
                    // 13. 发送超时告警
                    sendTimeoutAlert(serviceName, record, config);
                }
                return false;
            });
            
        } catch (Exception e) {
            log.error("处理异步回调消息异常: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
            
            // 14. 记录异常日志
            recordFailureLog(serviceName, record, config, "ASYNC_CALLBACK", e.getMessage());
            
            // 15. 发送异常告警
            sendFailureAlert(serviceName, record, config, e.getMessage());
        }
    }

    /**
     * 处理无回调异步消息
     * 适用于：监控指标、非关键日志
     * 特点：非阻塞，无回调，高性能，低可靠性
     */
    private void processAsyncNoCallbackMessage(String serviceName, ConsumerRecord<String, String> record, 
                                            MessageConsumerConfig config) {
        log.info("处理无回调异步消息: serviceName={}, key={}, topic={}, partition={}, offset={}", 
                serviceName, record.key(), record.topic(), record.partition(), record.offset());
        
        try {
            // 1. 轻量级消息验证（快速检查）
            if (!validateMessageLightweight(record)) {
                log.warn("无回调异步消息验证失败，跳过处理: serviceName={}, key={}", serviceName, record.key());
                return;
            }
            
            // 2. 异步处理业务逻辑（无回调）
            CompletableFuture.runAsync(() -> {
                try {
                    // 3. 执行轻量级业务逻辑
                    executeLightweightBusinessLogic(serviceName, record, config);
                    
                    // 4. 记录处理成功日志（异步）
                    recordAsyncSuccessLog(serviceName, record, config, "ASYNC_NO_CALLBACK");
                    
                    log.debug("无回调异步消息处理完成: serviceName={}, key={}", serviceName, record.key());
                    
                } catch (Exception e) {
                    log.warn("无回调异步消息处理异常（非关键）: serviceName={}, key={}, error={}", 
                            serviceName, record.key(), e.getMessage());
                    
                    // 5. 记录异常日志（异步，不阻塞）
                    recordAsyncFailureLog(serviceName, record, config, "ASYNC_NO_CALLBACK", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("无回调异步消息处理异常: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
            
            // 6. 记录异常日志（同步记录，避免丢失）
            recordFailureLog(serviceName, record, config, "ASYNC_NO_CALLBACK", e.getMessage());
        }
    }

    /**
     * 处理默认消息
     * 适用于：未知类型的消息，通用处理逻辑
     */
    private void processDefaultMessage(String serviceName, ConsumerRecord<String, String> record, 
                                    MessageConsumerConfig config) {
        log.info("处理默认消息: serviceName={}, key={}, topic={}, partition={}, offset={}", 
                serviceName, record.key(), record.topic(), record.partition(), record.offset());
        
        try {
            // 1. 基础消息验证
            if (!validateMessageBasic(record)) {
                log.warn("默认消息基础验证失败，跳过处理: serviceName={}, key={}", serviceName, record.key());
                return;
            }
            
            // 2. 尝试识别消息类型
            String detectedMessageType = detectMessageType(record);
            log.info("检测到消息类型: serviceName={}, key={}, detectedType={}", 
                    serviceName, record.key(), detectedMessageType);
            
            // 3. 根据检测到的类型选择处理策略
            switch (detectedMessageType) {
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
                    // 4. 执行通用处理逻辑
                    executeGenericBusinessLogic(serviceName, record, config);
                    
                    // 5. 记录通用处理日志
                    recordGenericLog(serviceName, record, config, "DEFAULT", "通用处理完成");
                    break;
            }
            
        } catch (Exception e) {
            log.error("处理默认消息异常: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
            
            // 6. 记录异常日志
            recordFailureLog(serviceName, record, config, "DEFAULT", e.getMessage());
            
            // 7. 发送异常告警
            sendFailureAlert(serviceName, record, config, e.getMessage());
        }
    }

    /**
     * 处理失败的消息
     * 实现完整的失败消息处理机制
     */
    private void handleFailedMessage(String serviceName, ConsumerRecord<String, String> record, 
                                   MessageConsumerConfig config, Exception error) {
        log.warn("处理失败的消息: serviceName={}, topic={}, partition={}, offset={}, error={}", 
                serviceName, record.topic(), record.partition(), record.offset(), error.getMessage());
        
        try {
            // 1. 记录失败日志
            recordFailureLog(serviceName, record, config, "FAILED", error.getMessage());
            
            // 2. 检查重试次数和策略
            int retryCount = getRetryCount(serviceName, record);
            int maxRetries = config.getMaxRetryTimes() != null ? config.getMaxRetryTimes() : 3;
            
            if (retryCount < maxRetries) {
                // 3. 执行重试机制
                handleRetry(serviceName, record, config, error, retryCount, maxRetries);
            } else {
                // 4. 达到最大重试次数，发送到死信队列
                sendToDeadLetterQueue(serviceName, record, config, error, retryCount);
                
                // 5. 发送最终失败告警
                sendFinalFailureAlert(serviceName, record, config, error, retryCount);
                
                // 6. 记录死信队列日志
                recordDeadLetterLog(serviceName, record, config, error, retryCount);
            }
            
        } catch (Exception e) {
            log.error("处理失败消息时发生异常: serviceName={}, key={}, originalError={}, newError={}", 
                    serviceName, record.key(), error.getMessage(), e.getMessage(), e);
            
            // 7. 记录处理失败消息时的异常
            recordFailureLog(serviceName, record, config, "FAILED_HANDLER_ERROR", e.getMessage());
            
            // 8. 发送紧急告警
            sendEmergencyAlert(serviceName, record, config, "失败消息处理器异常: " + e.getMessage());
        }
    }

    /**
     * 将消息转发给配置的消费者服务
     * 这是关键方法：当messages-service消费到消息后，自动转发给bgai-service等消费者服务
     */
    private void forwardMessageToConsumerService(String serviceName, ConsumerRecord<String, String> record, 
                                               MessageConsumerConfig config) {
        try {
            log.info("开始转发消息给消费者服务: serviceName={}, topic={}, key={}", 
                    serviceName, record.topic(), record.key());
            
            // 构建转发消息的数据结构
            Map<String, Object> forwardMessage = buildForwardMessage(record, config);
            
            // 获取消费者服务的回调地址
            String callbackUrl = getConsumerServiceCallbackUrl(serviceName, config);
            
            if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
                // 异步转发消息给消费者服务
                CompletableFuture.runAsync(() -> {
                    try {
                        boolean success = sendMessageToConsumerService(callbackUrl, forwardMessage);
                        if (success) {
                            log.info("消息转发成功: serviceName={}, topic={}, key={}, callbackUrl={}", 
                                    serviceName, record.topic(), record.key(), callbackUrl);
                        } else {
                            log.error("消息转发失败: serviceName={}, topic={}, key={}, callbackUrl={}", 
                                    serviceName, record.topic(), record.key(), callbackUrl);
                        }
                    } catch (Exception e) {
                        log.error("消息转发异常: serviceName={}, topic={}, key={}, callbackUrl={}, error={}", 
                                serviceName, record.topic(), record.key(), callbackUrl, e.getMessage(), e);
                    }
                });
            } else {
                log.warn("消费者服务未配置回调地址，跳过消息转发: serviceName={}, topic={}", 
                        serviceName, record.topic());
            }
            
        } catch (Exception e) {
            log.error("消息转发处理异常: serviceName={}, topic={}, key={}, error={}", 
                    serviceName, record.topic(), record.key(), e.getMessage(), e);
        }
    }

    /**
     * 构建转发消息的数据结构
     */
    private Map<String, Object> buildForwardMessage(ConsumerRecord<String, String> record, MessageConsumerConfig config) {
        Map<String, Object> message = new HashMap<>();
        
        // 基础消息信息
        message.put("messageId", record.key() != null ? record.key() : UUID.randomUUID().toString());
        message.put("topic", record.topic());
        message.put("partition", record.partition());
        message.put("offset", record.offset());
        message.put("timestamp", record.timestamp());
        message.put("messageBody", record.value());
        
        // 消费配置信息
        message.put("consumerService", config.getServiceName());
        message.put("consumerGroup", config.getConsumerGroup());
        message.put("messageQueueType", config.getMessageQueueType());
        message.put("consumeMode", config.getConsumeMode());
        message.put("consumeType", config.getConsumeType());
        message.put("consumeOrder", config.getConsumeOrder());
        
        // 消息头信息（如果有）
        if (record.headers() != null) {
            Map<String, String> headers = new HashMap<>();
            for (org.apache.kafka.common.header.Header header : record.headers()) {
                headers.put(header.key(), new String(header.value()));
            }
            message.put("headers", headers);
        }
        
        // 转发时间
        message.put("forwardTime", System.currentTimeMillis());
        message.put("forwardService", "messages-service");
        
        return message;
    }

    /**
     * 获取消费者服务的回调地址
     * 这里可以根据配置返回不同的回调地址
     */
    private String getConsumerServiceCallbackUrl(String serviceName, MessageConsumerConfig config) {
        // 方案1：从配置中获取回调地址
        if (config.getDescription() != null && config.getDescription().contains("callbackUrl=")) {
            String[] parts = config.getDescription().split("callbackUrl=");
            if (parts.length > 1) {
                return parts[1].split(",")[0]; // 提取第一个逗号前的内容
            }
        }
        
        // 方案2：从配置文件获取回调地址（推荐）
        String configCallbackUrl = serviceCallbackConfig.getCallbackUrl(serviceName);
        if (configCallbackUrl != null && !configCallbackUrl.trim().isEmpty()) {
            log.debug("从配置文件获取到回调地址: serviceName={}, callbackUrl={}", serviceName, configCallbackUrl);
            return configCallbackUrl;
        }
        
        // 方案3：从环境变量获取
        String envCallbackUrl = System.getenv(serviceName.toUpperCase() + "_CALLBACK_URL");
        if (envCallbackUrl != null && !envCallbackUrl.trim().isEmpty()) {
            log.debug("从环境变量获取到回调地址: serviceName={}, callbackUrl={}", serviceName, envCallbackUrl);
            return envCallbackUrl;
        }
        
        // 方案4：从数据库配置中获取（需要扩展MessageConsumerConfig表）
        // TODO: 在MessageConsumerConfig表中添加callbackUrl字段
        
        log.warn("未找到服务 {} 的回调地址配置", serviceName);
        return null;
    }

    /**
     * 发送消息给消费者服务
     */
    private boolean sendMessageToConsumerService(String callbackUrl, Map<String, Object> message) {
        try {
            // 设置HTTP请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "messages-service/1.0");
            headers.set("X-Forwarded-By", "messages-service");
            
            // 创建HTTP请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(message, headers);
            
            // 发送POST请求
            ResponseEntity<String> response = restTemplate.postForEntity(callbackUrl, requestEntity, String.class);
            
            // 检查响应状态
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("消息转发成功: callbackUrl={}, statusCode={}, response={}", 
                        callbackUrl, response.getStatusCode(), response.getBody());
                return true;
            } else {
                log.warn("消息转发失败: callbackUrl={}, statusCode={}, response={}", 
                        callbackUrl, response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            log.error("消息转发异常: callbackUrl={}, error={}", callbackUrl, e.getMessage(), e);
            return false;
        }
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
            } else if (consumer instanceof RocketMQConsumerManager.ConfigBasedConsumer) {
                // 停止RocketMQ消费者
                stopRocketMQConsumer(serviceName);
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
     * 停止RocketMQ消费者
     */
    private void stopRocketMQConsumer(String serviceName) {
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
            
            // 关闭RocketMQ消费者
            RocketMQConsumerManager.ConfigBasedConsumer consumer = rocketMQConsumers.get(serviceName);
            if (consumer != null) {
                consumer.shutdown();
                rocketMQConsumers.remove(serviceName);
            }
            
            // 清理资源
            consumerThreads.remove(serviceName);
            stopFlags.remove(serviceName);
            
            log.info("RocketMQ消费者已停止: serviceName={}", serviceName);
            
        } catch (Exception e) {
            log.error("停止RocketMQ消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
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

    // ==================== 消息处理辅助方法 ====================

    /**
     * 验证消息（完整验证）
     */
    private boolean validateMessage(ConsumerRecord<String, String> record) {
        try {
            // 基础验证
            if (!validateMessageBasic(record)) {
                return false;
            }
            
            // 业务规则验证
            if (record.value() == null || record.value().trim().isEmpty()) {
                log.warn("消息内容为空: topic={}, key={}", record.topic(), record.key());
                return false;
            }
            
            // 消息格式验证（假设是JSON格式）
            if (!isValidJsonFormat(record.value())) {
                log.warn("消息格式无效: topic={}, key={}, value={}", record.topic(), record.key(), record.value());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("消息验证异常: topic={}, key={}, error={}", record.topic(), record.key(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 基础消息验证
     */
    private boolean validateMessageBasic(ConsumerRecord<String, String> record) {
        return record != null && record.topic() != null && record.key() != null;
    }

    /**
     * 轻量级消息验证
     */
    private boolean validateMessageLightweight(ConsumerRecord<String, String> record) {
        return record != null && record.value() != null;
    }

    /**
     * 验证JSON格式
     */
    private boolean isValidJsonFormat(String value) {
        try {
            // 简单的JSON格式验证
            return value.trim().startsWith("{") && value.trim().endsWith("}");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行业务逻辑
     */
    private boolean executeBusinessLogic(String serviceName, ConsumerRecord<String, String> record, 
                                       MessageConsumerConfig config) {
        try {
            log.debug("执行业务逻辑: serviceName={}, key={}", serviceName, record.key());
            
            // 模拟业务逻辑执行
            // 实际项目中这里应该调用具体的业务服务
            Thread.sleep(100); // 模拟处理时间
            
            // 模拟成功率95%
            boolean success = Math.random() > 0.05;
            
            if (success) {
                log.debug("业务逻辑执行成功: serviceName={}, key={}", serviceName, record.key());
            } else {
                log.warn("业务逻辑执行失败: serviceName={}, key={}", serviceName, record.key());
            }
            
            return success;
        } catch (Exception e) {
            log.error("执行业务逻辑异常: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行轻量级业务逻辑
     */
    private void executeLightweightBusinessLogic(String serviceName, ConsumerRecord<String, String> record, 
                                               MessageConsumerConfig config) {
        try {
            log.debug("执行轻量级业务逻辑: serviceName={}, key={}", serviceName, record.key());
            
            // 模拟轻量级处理
            Thread.sleep(10); // 模拟快速处理
            
            log.debug("轻量级业务逻辑执行完成: serviceName={}, key={}", serviceName, record.key());
        } catch (Exception e) {
            log.warn("执行轻量级业务逻辑异常: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 执行通用业务逻辑
     */
    private void executeGenericBusinessLogic(String serviceName, ConsumerRecord<String, String> record, 
                                           MessageConsumerConfig config) {
        try {
            log.debug("执行通用业务逻辑: serviceName={}, key={}", serviceName, record.key());
            
            // 模拟通用处理
            Thread.sleep(50); // 模拟中等处理时间
            
            log.debug("通用业务逻辑执行完成: serviceName={}, key={}", serviceName, record.key());
        } catch (Exception e) {
            log.error("执行通用业务逻辑异常: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 检测消息类型
     */
    private String detectMessageType(ConsumerRecord<String, String> record) {
        try {
            String value = record.value();
            if (value == null) {
                return "UNKNOWN";
            }
            
            // 从消息头检测
            if (record.headers() != null) {
                for (org.apache.kafka.common.header.Header header : record.headers()) {
                    if ("messageType".equals(header.key())) {
                        return new String(header.value());
                    }
                }
            }
            
            // 从消息内容检测
            if (value.contains("SYNC_BLOCKING")) return "SYNC_BLOCKING";
            if (value.contains("ASYNC_CALLBACK")) return "ASYNC_CALLBACK";
            if (value.contains("ASYNC_NO_CALLBACK")) return "ASYNC_NO_CALLBACK";
            
            return "UNKNOWN";
        } catch (Exception e) {
            log.warn("检测消息类型失败: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * 记录成功日志
     */
    private void recordSuccessLog(String serviceName, ConsumerRecord<String, String> record, 
                                 MessageConsumerConfig config, String messageType) {
        try {
            log.info("记录成功日志: serviceName={}, key={}, messageType={}, topic={}, partition={}, offset={}", 
                    serviceName, record.key(), messageType, record.topic(), record.partition(), record.offset());
            
            // 实际项目中这里应该记录到数据库或日志系统
            // TODO: 实现具体的日志记录逻辑
        } catch (Exception e) {
            log.error("记录成功日志失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 记录异步成功日志
     */
    private void recordAsyncSuccessLog(String serviceName, ConsumerRecord<String, String> record, 
                                      MessageConsumerConfig config, String messageType) {
        CompletableFuture.runAsync(() -> {
            recordSuccessLog(serviceName, record, config, messageType);
        });
    }

    /**
     * 记录失败日志
     */
    private void recordFailureLog(String serviceName, ConsumerRecord<String, String> record, 
                                 MessageConsumerConfig config, String messageType, String errorMessage) {
        try {
            log.error("记录失败日志: serviceName={}, key={}, messageType={}, error={}, topic={}, partition={}, offset={}", 
                    serviceName, record.key(), messageType, errorMessage, record.topic(), record.partition(), record.offset());
            
            // 实际项目中这里应该记录到数据库或日志系统
            // TODO: 实现具体的日志记录逻辑
        } catch (Exception e) {
            log.error("记录失败日志失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 记录异步失败日志
     */
    private void recordAsyncFailureLog(String serviceName, ConsumerRecord<String, String> record, 
                                      MessageConsumerConfig config, String messageType, String errorMessage) {
        CompletableFuture.runAsync(() -> {
            recordFailureLog(serviceName, record, config, messageType, errorMessage);
        });
    }

    /**
     * 记录通用日志
     */
    private void recordGenericLog(String serviceName, ConsumerRecord<String, String> record, 
                                 MessageConsumerConfig config, String messageType, String message) {
        try {
            log.info("记录通用日志: serviceName={}, key={}, messageType={}, message={}", 
                    serviceName, record.key(), messageType, message);
            
            // TODO: 实现具体的日志记录逻辑
        } catch (Exception e) {
            log.error("记录通用日志失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 记录死信队列日志
     */
    private void recordDeadLetterLog(String serviceName, ConsumerRecord<String, String> record, 
                                    MessageConsumerConfig config, Exception error, int retryCount) {
        try {
            log.error("记录死信队列日志: serviceName={}, key={}, retryCount={}, error={}", 
                    serviceName, record.key(), retryCount, error.getMessage());
            
            // TODO: 实现具体的死信队列日志记录逻辑
        } catch (Exception e) {
            log.error("记录死信队列日志失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 发送成功通知
     */
    private void sendSuccessNotification(String serviceName, ConsumerRecord<String, String> record, 
                                       MessageConsumerConfig config) {
        try {
            log.debug("发送成功通知: serviceName={}, key={}", serviceName, record.key());
            
            // TODO: 实现具体的成功通知逻辑（邮件、短信、钉钉等）
        } catch (Exception e) {
            log.error("发送成功通知失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 发送失败告警
     */
    private void sendFailureAlert(String serviceName, ConsumerRecord<String, String> record, 
                                 MessageConsumerConfig config, String errorMessage) {
        try {
            log.warn("发送失败告警: serviceName={}, key={}, error={}", serviceName, record.key(), errorMessage);
            
            // TODO: 实现具体的失败告警逻辑（邮件、短信、钉钉等）
        } catch (Exception e) {
            log.error("发送失败告警失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 发送超时告警
     */
    private void sendTimeoutAlert(String serviceName, ConsumerRecord<String, String> record, 
                                 MessageConsumerConfig config) {
        try {
            log.warn("发送超时告警: serviceName={}, key={}", serviceName, record.key());
            
            // TODO: 实现具体的超时告警逻辑
        } catch (Exception e) {
            log.error("发送超时告警失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 发送最终失败告警
     */
    private void sendFinalFailureAlert(String serviceName, ConsumerRecord<String, String> record, 
                                      MessageConsumerConfig config, Exception error, int retryCount) {
        try {
            log.error("发送最终失败告警: serviceName={}, key={}, retryCount={}, error={}", 
                    serviceName, record.key(), retryCount, error.getMessage());
            
            // TODO: 实现具体的最终失败告警逻辑
        } catch (Exception e) {
            log.error("发送最终失败告警失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 发送紧急告警
     */
    private void sendEmergencyAlert(String serviceName, ConsumerRecord<String, String> record, 
                                   MessageConsumerConfig config, String message) {
        try {
            log.error("发送紧急告警: serviceName={}, key={}, message={}", serviceName, record.key(), message);
            
            // TODO: 实现具体的紧急告警逻辑
        } catch (Exception e) {
            log.error("发送紧急告警失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 执行成功回调
     */
    private void executeSuccessCallback(String serviceName, ConsumerRecord<String, String> record, 
                                       MessageConsumerConfig config) {
        try {
            log.debug("执行成功回调: serviceName={}, key={}", serviceName, record.key());
            
            // TODO: 实现具体的成功回调逻辑
        } catch (Exception e) {
            log.error("执行成功回调失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 执行失败回调
     */
    private void executeFailureCallback(String serviceName, ConsumerRecord<String, String> record, 
                                       MessageConsumerConfig config, String errorMessage) {
        try {
            log.debug("执行失败回调: serviceName={}, key={}, error={}", serviceName, record.key(), errorMessage);
            
            // TODO: 实现具体的失败回调逻辑
        } catch (Exception e) {
            log.error("执行失败回调失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 执行超时回调
     */
    private void executeTimeoutCallback(String serviceName, ConsumerRecord<String, String> record, 
                                       MessageConsumerConfig config) {
        try {
            log.debug("执行超时回调: serviceName={}, key={}", serviceName, record.key());
            
            // TODO: 实现具体的超时回调逻辑
        } catch (Exception e) {
            log.error("执行超时回调失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 获取重试次数
     */
    private int getRetryCount(String serviceName, ConsumerRecord<String, String> record) {
        try {
            // 实际项目中应该从数据库或缓存中获取重试次数
            // 这里简单返回0，表示第一次处理
            return 0;
        } catch (Exception e) {
            log.error("获取重试次数失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 处理重试
     */
    private void handleRetry(String serviceName, ConsumerRecord<String, String> record, 
                             MessageConsumerConfig config, Exception error, int retryCount, int maxRetries) {
        try {
            log.info("执行重试机制: serviceName={}, key={}, retryCount={}, maxRetries={}", 
                    serviceName, record.key(), retryCount + 1, maxRetries);
            
            // 计算延迟时间（指数退避）
            long delayMs = calculateRetryDelay(retryCount);
            
            // 异步延迟重试
            CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> {
                try {
                    log.info("开始重试处理: serviceName={}, key={}, retryCount={}", 
                            serviceName, record.key(), retryCount + 1);
                    
                    // 重新处理消息
                    processKafkaMessages(serviceName, 
                            new ConsumerRecords<>(Collections.singletonMap(
                                    new TopicPartition(record.topic(), record.partition()),
                                    Collections.singletonList(record))), config);
                    
                } catch (Exception e) {
                    log.error("重试处理异常: serviceName={}, key={}, retryCount={}, error={}", 
                            serviceName, record.key(), retryCount + 1, e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("处理重试异常: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

    /**
     * 计算重试延迟时间
     */
    private long calculateRetryDelay(int retryCount) {
        // 指数退避策略：1s, 2s, 4s, 8s...
        return Math.min(1000L * (1L << retryCount), 30000L); // 最大30秒
    }

    /**
     * 发送到死信队列
     */
    private void sendToDeadLetterQueue(String serviceName, ConsumerRecord<String, String> record, 
                                      MessageConsumerConfig config, Exception error, int retryCount) {
        try {
            log.error("发送到死信队列: serviceName={}, key={}, retryCount={}, error={}", 
                    serviceName, record.key(), retryCount, error.getMessage());
            
            // TODO: 实现具体的死信队列发送逻辑
            // 1. 构建死信消息
            // 2. 发送到死信队列
            // 3. 记录死信队列信息
            
        } catch (Exception e) {
            log.error("发送到死信队列失败: serviceName={}, key={}, error={}", 
                    serviceName, record.key(), e.getMessage(), e);
        }
    }

}
