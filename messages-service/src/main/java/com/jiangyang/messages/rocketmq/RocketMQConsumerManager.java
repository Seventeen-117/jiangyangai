package com.jiangyang.messages.rocketmq;

import com.jiangyang.messages.entity.MessageConsumerConfig;
import com.jiangyang.messages.consume.ConsumeMode;
import com.jiangyang.messages.consume.ConsumeType;
import com.jiangyang.messages.consume.ConsumeOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ消费者管理器
 * 支持不同的消费模式、消费类型和顺序性配置
 */
@Slf4j
@Component
public class RocketMQConsumerManager {

    /**
     * 消费者配置映射：serviceName -> consumer
     */
    private final Map<String, Object> consumers = new ConcurrentHashMap<>();

    /**
     * 消费者状态映射：serviceName -> isRunning
     */
    private final Map<String, AtomicBoolean> consumerStatus = new ConcurrentHashMap<>();

    /**
     * 消息处理统计：serviceName -> statistics
     */
    private final Map<String, ConsumerStatistics> statistics = new ConcurrentHashMap<>();

    /**
     * 线程池执行器
     */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 创建基于配置的消费者
     */
    public ConfigBasedConsumer createConfigBasedConsumer(MessageConsumerConfig config) {
        String serviceName = config.getServiceName();
        
        try {
            // 根据消费模式创建不同类型的消费者
            if (ConsumeMode.PUSH.name().equals(config.getConsumeMode())) {
                return createPushConsumer(config);
            } else {
                return createPullConsumer(config);
            }
        } catch (Exception e) {
            log.error("创建RocketMQ消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
            throw new RuntimeException("创建RocketMQ消费者失败", e);
        }
    }

    /**
     * 创建推模式消费者
     */
    private ConfigBasedConsumer createPushConsumer(MessageConsumerConfig config) throws MQClientException {
        String serviceName = config.getServiceName();
        String consumerGroup = config.getConsumerGroup();
        String topic = config.getTopic();
        String tag = config.getTag();
        
        log.info("创建推模式消费者: serviceName={}, consumerGroup={}, topic={}, tag={}", 
                serviceName, consumerGroup, topic, tag);

        // 创建推模式消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        
        // 设置NameServer地址（实际项目中应该从配置中获取）
        consumer.setNamesrvAddr("localhost:9876");
        
        // 设置消费类型
        if (ConsumeType.BROADCASTING.name().equals(config.getConsumeType())) {
            consumer.setMessageModel(MessageModel.BROADCASTING);
        } else {
            consumer.setMessageModel(MessageModel.CLUSTERING);
        }

        // 设置消费者实例名称
        if (config.getInstanceId() != null) {
            consumer.setInstanceName(config.getInstanceId());
        }

        // 设置最大重试次数
        if (config.getMaxRetryTimes() != null) {
            consumer.setMaxReconsumeTimes(config.getMaxRetryTimes());
        }

        // 设置消息监听器
        if (ConsumeOrder.ORDERLY.name().equals(config.getConsumeOrder())) {
            // 顺序消费监听器
            consumer.registerMessageListener(new MessageListenerOrderly() {
                @Override
                public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                    return processMessagesOrderly(serviceName, msgs, config);
                }
            });
        } else {
            // 并发消费监听器
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                    return processMessagesConcurrently(serviceName, msgs, config);
                }
            });
        }

        // 订阅主题和标签
        if (tag != null && !tag.trim().isEmpty()) {
            consumer.subscribe(topic, tag);
        } else {
            consumer.subscribe(topic, "*");
        }

        // 启动消费者
        consumer.start();
        
        log.info("推模式消费者创建成功: serviceName={}, consumerGroup={}, topic={}", 
                serviceName, consumerGroup, topic);

        return new PushConsumerWrapper(consumer, config);
    }

    /**
     * 创建拉模式消费者
     */
    private ConfigBasedConsumer createPullConsumer(MessageConsumerConfig config) throws MQClientException {
        String serviceName = config.getServiceName();
        String consumerGroup = config.getConsumerGroup();
        String topic = config.getTopic();
        
        log.info("创建拉模式消费者: serviceName={}, consumerGroup={}, topic={}", 
                serviceName, consumerGroup, topic);

        // 创建拉模式消费者
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(consumerGroup);
        
        // 设置NameServer地址
        consumer.setNamesrvAddr("localhost:9876");
        
        // 设置消费者实例名称
        if (config.getInstanceId() != null) {
            consumer.setInstanceName(config.getInstanceId());
        }

        // 启动消费者
        consumer.start();
        
        log.info("拉模式消费者创建成功: serviceName={}, consumerGroup={}, topic={}", 
                serviceName, consumerGroup, topic);

        return new PullConsumerWrapper(consumer, config);
    }

    /**
     * 顺序处理消息
     */
    private ConsumeOrderlyStatus processMessagesOrderly(String serviceName, List<MessageExt> msgs, MessageConsumerConfig config) {
        try {
            log.debug("顺序处理消息: serviceName={}, messageCount={}", serviceName, msgs.size());
            
            // 更新统计信息
            updateStatistics(serviceName, msgs.size(), true);
            
            // 模拟消息处理
            for (MessageExt msg : msgs) {
                try {
                    // 实际项目中这里应该调用具体的业务处理逻辑
                    processMessage(serviceName, msg, config);
                    
                    // 模拟处理时间
                    Thread.sleep(10);
                    
                } catch (Exception e) {
                    log.error("顺序处理消息失败: serviceName={}, msgId={}, error={}", 
                            serviceName, msg.getMsgId(), e.getMessage(), e);
                    
                    // 更新失败统计
                    updateStatistics(serviceName, 1, false);
                    
                    // 返回SUSPEND_CURRENT_QUEUE_A_MOMENT，稍后重试
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }
            }
            
            return ConsumeOrderlyStatus.SUCCESS;
            
        } catch (Exception e) {
            log.error("顺序处理消息异常: serviceName={}, error={}", serviceName, e.getMessage(), e);
            return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
        }
    }

    /**
     * 并发处理消息
     */
    private ConsumeConcurrentlyStatus processMessagesConcurrently(String serviceName, List<MessageExt> msgs, MessageConsumerConfig config) {
        try {
            log.debug("并发处理消息: serviceName={}, messageCount={}", serviceName, msgs.size());
            
            // 更新统计信息
            updateStatistics(serviceName, msgs.size(), true);
            
            // 并发处理消息
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            
            for (MessageExt msg : msgs) {
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        processMessage(serviceName, msg, config);
                        return true;
                    } catch (Exception e) {
                        log.error("并发处理消息失败: serviceName={}, msgId={}, error={}", 
                                serviceName, msg.getMsgId(), e.getMessage(), e);
                        
                        // 更新失败统计
                        updateStatistics(serviceName, 1, false);
                        return false;
                    }
                }, executorService);
                
                futures.add(future);
            }
            
            // 等待所有消息处理完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // 检查是否有失败的消息
            boolean hasFailure = futures.stream().anyMatch(f -> {
                try {
                    return !f.get();
                } catch (Exception e) {
                    return true;
                }
            });
            
            if (hasFailure) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
            
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            
        } catch (Exception e) {
            log.error("并发处理消息异常: serviceName={}, error={}", serviceName, e.getMessage(), e);
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

    /**
     * 处理单条消息
     */
    private void processMessage(String serviceName, MessageExt msg, MessageConsumerConfig config) {
        try {
            log.debug("处理消息: serviceName={}, msgId={}, topic={}, tag={}, body={}", 
                    serviceName, msg.getMsgId(), msg.getTopic(), msg.getTags(), new String(msg.getBody()));
            
            // 实际项目中这里应该调用具体的业务处理逻辑
            // 例如：调用业务服务、记录日志、发送通知等
            
            // 模拟业务处理
            simulateBusinessProcessing(msg, config);
            
        } catch (Exception e) {
            log.error("处理消息异常: serviceName={}, msgId={}, error={}", 
                    serviceName, msg.getMsgId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 模拟业务处理
     */
    private void simulateBusinessProcessing(MessageExt msg, MessageConsumerConfig config) {
        try {
            // 根据消息标签进行不同的业务处理
            String tags = msg.getTags();
            if (tags != null) {
                switch (tags) {
                    case "user-action":
                        simulateUserActionProcessing(msg);
                        break;
                    case "order-event":
                        simulateOrderEventProcessing(msg);
                        break;
                    case "system-notification":
                        simulateSystemNotificationProcessing(msg);
                        break;
                    default:
                        simulateDefaultProcessing(msg);
                        break;
                }
            } else {
                simulateDefaultProcessing(msg);
            }
            
        } catch (Exception e) {
            log.error("模拟业务处理异常: msgId={}, error={}", msg.getMsgId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 模拟用户行为处理
     */
    private void simulateUserActionProcessing(MessageExt msg) {
        log.debug("模拟用户行为处理: msgId={}", msg.getMsgId());
        // 模拟处理时间
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟订单事件处理
     */
    private void simulateOrderEventProcessing(MessageExt msg) {
        log.debug("模拟订单事件处理: msgId={}", msg.getMsgId());
        // 模拟处理时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟系统通知处理
     */
    private void simulateSystemNotificationProcessing(MessageExt msg) {
        log.debug("模拟系统通知处理: msgId={}", msg.getMsgId());
        // 模拟处理时间
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟默认处理
     */
    private void simulateDefaultProcessing(MessageExt msg) {
        log.debug("模拟默认处理: msgId={}", msg.getMsgId());
        // 模拟处理时间
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics(String serviceName, int messageCount, boolean success) {
        ConsumerStatistics stats = statistics.computeIfAbsent(serviceName, k -> new ConsumerStatistics());
        
        if (success) {
            stats.incrementSuccessCount(messageCount);
        } else {
            stats.incrementFailureCount(messageCount);
        }
        
        stats.updateLastProcessTime();
    }

    /**
     * 获取消费者统计信息
     */
    public ConsumerStatistics getConsumerStatistics(String serviceName) {
        return statistics.getOrDefault(serviceName, new ConsumerStatistics());
    }

    /**
     * 关闭所有消费者
     */
    public void shutdownAllConsumers() {
        log.info("开始关闭所有RocketMQ消费者...");
        
        for (Map.Entry<String, Object> entry : consumers.entrySet()) {
            String serviceName = entry.getKey();
            Object consumer = entry.getValue();
            
            try {
                if (consumer instanceof PushConsumerWrapper) {
                    ((PushConsumerWrapper) consumer).shutdown();
                } else if (consumer instanceof PullConsumerWrapper) {
                    ((PullConsumerWrapper) consumer).shutdown();
                }
                
                log.info("消费者已关闭: serviceName={}", serviceName);
                
            } catch (Exception e) {
                log.error("关闭消费者失败: serviceName={}, error={}", serviceName, e.getMessage(), e);
            }
        }
        
        consumers.clear();
        consumerStatus.clear();
        
        // 关闭线程池
        executorService.shutdown();
        
        log.info("所有RocketMQ消费者已关闭");
    }

    /**
     * 消费者统计信息
     */
    public static class ConsumerStatistics {
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private volatile long lastProcessTime = 0;
        private volatile long startTime = System.currentTimeMillis();

        public void incrementSuccessCount(int count) {
            successCount.addAndGet(count);
        }

        public void incrementFailureCount(int count) {
            failureCount.addAndGet(count);
        }

        public void updateLastProcessTime() {
            lastProcessTime = System.currentTimeMillis();
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public long getFailureCount() {
            return failureCount.get();
        }

        public long getTotalCount() {
            return successCount.get() + failureCount.get();
        }

        public long getLastProcessTime() {
            return lastProcessTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public double getSuccessRate() {
            long total = getTotalCount();
            return total > 0 ? (double) successCount.get() / total : 0.0;
        }

        public long getRunningTime() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * 基于配置的消费者接口
     */
    public interface ConfigBasedConsumer {
        /**
         * 启动推模式消费
         */
        void startPushMode();

        /**
         * 启动拉模式消费
         */
        void startPullMode();

        /**
         * 关闭消费者
         */
        void shutdown();

        /**
         * 是否正在运行
         */
        boolean isRunning();
    }

    /**
     * 推模式消费者包装器
     */
    private class PushConsumerWrapper implements ConfigBasedConsumer {
        private final DefaultMQPushConsumer consumer;
        private final MessageConsumerConfig config;
        private final AtomicBoolean running = new AtomicBoolean(false);

        public PushConsumerWrapper(DefaultMQPushConsumer consumer, MessageConsumerConfig config) {
            this.consumer = consumer;
            this.config = config;
        }

        @Override
        public void startPushMode() {
            if (running.compareAndSet(false, true)) {
                try {
                    // 推模式消费者已经在创建时启动，这里只需要标记状态
                    log.info("推模式消费者已启动: serviceName={}", config.getServiceName());
                } catch (Exception e) {
                    running.set(false);
                    log.error("启动推模式消费者失败: serviceName={}, error={}", 
                            config.getServiceName(), e.getMessage(), e);
                    throw new RuntimeException("启动推模式消费者失败", e);
                }
            }
        }

        @Override
        public void startPullMode() {
            log.warn("推模式消费者不支持拉模式: serviceName={}", config.getServiceName());
        }

        @Override
        public void shutdown() {
            if (running.compareAndSet(true, false)) {
                try {
                    consumer.shutdown();
                    log.info("推模式消费者已关闭: serviceName={}", config.getServiceName());
                } catch (Exception e) {
                    log.error("关闭推模式消费者失败: serviceName={}, error={}", 
                            config.getServiceName(), e.getMessage(), e);
                }
            }
        }

        @Override
        public boolean isRunning() {
            return running.get();
        }
    }

    /**
     * 拉模式消费者包装器
     */
    private class PullConsumerWrapper implements ConfigBasedConsumer {
        private final DefaultMQPullConsumer consumer;
        private final MessageConsumerConfig config;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        public PullConsumerWrapper(DefaultMQPullConsumer consumer, MessageConsumerConfig config) {
            this.consumer = consumer;
            this.config = config;
        }

        @Override
        public void startPushMode() {
            log.warn("拉模式消费者不支持推模式: serviceName={}", config.getServiceName());
        }

        @Override
        public void startPullMode() {
            if (running.compareAndSet(false, true)) {
                try {
                    // 启动定时拉取任务
                    startPullTask();
                    log.info("拉模式消费者已启动: serviceName={}", config.getServiceName());
                } catch (Exception e) {
                    running.set(false);
                    log.error("启动拉模式消费者失败: serviceName={}, error={}", 
                            config.getServiceName(), e.getMessage(), e);
                    throw new RuntimeException("启动拉模式消费者失败", e);
                }
            }
        }

        /**
         * 启动拉取任务
         */
        private void startPullTask() {
            scheduler.scheduleWithFixedDelay(() -> {
                if (!running.get()) {
                    return;
                }

                try {
                    // 获取消息队列
                    Set<MessageQueue> mqs = consumer.fetchSubscribeMessageQueues(config.getTopic());
                    
                    for (MessageQueue mq : mqs) {
                        if (!running.get()) {
                            break;
                        }

                        try {
                            // 拉取消息
                            PullResult pullResult = consumer.pull(mq, "*", 0L, 32);
                            
                                                         if ("FOUND".equals(pullResult.getPullStatus().name())) {
                                // 处理拉取到的消息
                                List<MessageExt> msgs = pullResult.getMsgFoundList();
                                if (!msgs.isEmpty()) {
                                    log.info("拉取到消息: serviceName={}, queue={}, messageCount={}", 
                                            config.getServiceName(), mq, msgs.size());
                                    
                                    // 根据顺序性配置处理消息
                                    if (ConsumeOrder.ORDERLY.name().equals(config.getConsumeOrder())) {
                                        processMessagesOrderly(config.getServiceName(), msgs, config);
                                    } else {
                                        processMessagesConcurrently(config.getServiceName(), msgs, config);
                                    }
                                }
                            }
                            
                        } catch (Exception e) {
                            log.error("拉取消息失败: serviceName={}, queue={}, error={}", 
                                    config.getServiceName(), mq, e.getMessage(), e);
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("拉取任务执行异常: serviceName={}, error={}", 
                            config.getServiceName(), e.getMessage(), e);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS); // 每秒执行一次
        }

        @Override
        public void shutdown() {
            if (running.compareAndSet(true, false)) {
                try {
                    scheduler.shutdown();
                    consumer.shutdown();
                    log.info("拉模式消费者已关闭: serviceName={}", config.getServiceName());
                } catch (Exception e) {
                    log.error("关闭拉模式消费者失败: serviceName={}, error={}", 
                            config.getServiceName(), e.getMessage(), e);
                }
            }
        }

        @Override
        public boolean isRunning() {
            return running.get();
        }
    }
}
