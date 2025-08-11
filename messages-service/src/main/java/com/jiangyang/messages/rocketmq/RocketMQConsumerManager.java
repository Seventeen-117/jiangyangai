package com.jiangyang.messages.rocketmq;

import com.alibaba.fastjson2.JSON;
import com.jiangyang.messages.service.MessageListener;
import com.jiangyang.messages.utils.MessageServiceException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ消费者管理类
 * 负责消费者的创建、订阅、取消订阅以及消费逻辑的管理
 * 实现集群消费、广播消费、并发消费、顺序消费及高级特性
 */
@Slf4j
@Component
public class RocketMQConsumerManager implements InitializingBean, DisposableBean {

    @Setter
    private String nameServer;

    @Setter
    private int minConsumeThreads = 20;

    @Setter
    private int maxConsumeThreads = 64;

    @Setter
    private int consumeTimeoutMinutes = 15;

    @Setter
    private int pullBatchSize = 32;

    @Setter
    private int consumeMessageBatchMaxSize = 32;

    @Setter
    private int maxReconsumeTimes = 16;

    @Setter
    private long suspendCurrentQueueTimeMillis = 1000;

    private final Map<String, DefaultMQPushConsumer> consumers = new ConcurrentHashMap<>();
    private final Map<String, MessageListener<?>> listeners = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> consumerExecutors = new ConcurrentHashMap<>();
    private final AtomicLong backlogCount = new AtomicLong(0);

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("RocketMQConsumerManager initialized");
    }

    @Override
    public void destroy() throws Exception {
        // 关闭所有消费者
        for (DefaultMQPushConsumer consumer : consumers.values()) {
            if (consumer != null) {
                consumer.shutdown();
            }
        }
        consumers.clear();
        listeners.clear();
        
        // 关闭所有线程池
        for (ExecutorService executor : consumerExecutors.values()) {
            if (executor != null) {
                executor.shutdown();
            }
        }
        consumerExecutors.clear();
        log.info("RocketMQConsumerManager shutdown");
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
        return subscribe(topic, consumerGroup, listener, MessageModel.CLUSTERING, false, null);
    }

    /**
     * 订阅主题，支持指定消费模式和是否顺序消费
     * @param topic 主题
     * @param consumerGroup 消费者组
     * @param listener 消息监听器
     * @param messageModel 消费模式（集群或广播）
     * @param isOrderly 是否顺序消费
     * @param filterExpression 过滤表达式（支持Tag或SQL92）
     * @param <T> 消息类型
     * @return 是否订阅成功
     */
    public <T> boolean subscribe(String topic, String consumerGroup, MessageListener<T> listener, 
                               MessageModel messageModel, boolean isOrderly, String filterExpression) {
        String consumerKey = generateConsumerKey(topic, consumerGroup);
        if (consumers.containsKey(consumerKey)) {
            log.warn("Consumer already subscribed for topic: {}, group: {}", topic, consumerGroup);
            return false;
        }

        try {
            DefaultMQPushConsumer consumer = createConsumer(consumerGroup, messageModel);
            // 订阅主题
            if (filterExpression != null && !filterExpression.isEmpty()) {
                if (filterExpression.startsWith("tags=") || filterExpression.contains(" AND ")) {
                    // SQL92过滤
                    consumer.subscribe(topic, MessageSelector.bySql(filterExpression));
                } else {
                    // Tag过滤
                    consumer.subscribe(topic, filterExpression);
                }
            } else {
                consumer.subscribe(topic, "*");
            }

            // 注册监听器
            if (isOrderly) {
                consumer.registerMessageListener(createOrderlyListener(listener, consumerKey));
            } else {
                consumer.registerMessageListener(createConcurrentListener(listener, consumerKey));
            }

            // 启动消费者
            consumer.start();
            consumers.put(consumerKey, consumer);
            listeners.put(consumerKey, listener);
            log.info("Subscribed to topic: {}, group: {}, model: {}, orderly: {}", topic, consumerGroup, messageModel, isOrderly);
            return true;
        } catch (MQClientException e) {
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
        DefaultMQPushConsumer consumer = consumers.remove(consumerKey);
        if (consumer != null) {
            consumer.shutdown();
            listeners.remove(consumerKey);
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
    private DefaultMQPushConsumer createConsumer(String consumerGroup, MessageModel messageModel) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setMessageModel(messageModel);
        // 设置消费线程数
        consumer.setConsumeThreadMin(minConsumeThreads);
        consumer.setConsumeThreadMax(maxConsumeThreads);
        // 设置消费超时时间
        consumer.setConsumeTimeout(consumeTimeoutMinutes);
        // 设置拉取批量大小
        consumer.setPullBatchSize(pullBatchSize);
        // 设置消费批量大小
        consumer.setConsumeMessageBatchMaxSize(consumeMessageBatchMaxSize);
        // 设置最大重试次数
        consumer.setMaxReconsumeTimes(maxReconsumeTimes);
        // 设置队列挂起时间
        consumer.setSuspendCurrentQueueTimeMillis(suspendCurrentQueueTimeMillis);
        // 设置消费起始位置
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        return consumer;
    }

    /**
     * 创建并发消费监听器
     */
    private <T> MessageListenerConcurrently createConcurrentListener(MessageListener<T> listener, String consumerKey) {
        // 为每个消费者创建独立的线程池，用于处理IO密集型操作
        ExecutorService executor = Executors.newFixedThreadPool(maxConsumeThreads);
        consumerExecutors.put(consumerKey, executor);

        return (List<MessageExt> msgs, ConsumeConcurrentlyContext context) -> {
            // 流量控制
            if (backlogCount.get() > 1000) {
                log.warn("Consumer backlog too high, rejecting messages. Backlog: {}", backlogCount.get());
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }

            backlogCount.addAndGet(msgs.size());
            try {
                for (MessageExt msg : msgs) {
                    // 提交到线程池处理，防止阻塞消费线程
                    executor.execute(() -> {
                        try {
                            processMessage(msg, listener);
                        } catch (Exception e) {
                            log.error("Error processing message, msgId: {}", msg.getMsgId(), e);
                            // 触发重试
                            context.setAckIndex(-1);
                        } finally {
                            backlogCount.decrementAndGet();
                        }
                    });
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception e) {
                log.error("Error in concurrent listener, topic: {}, group: {}", context.getMessageQueue().getTopic(), consumerKey, e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        };
    }

    /**
     * 创建顺序消费监听器
     */
    private <T> MessageListenerOrderly createOrderlyListener(MessageListener<T> listener, String consumerKey) {
        return (List<MessageExt> msgs, ConsumeOrderlyContext context) -> {
            // 顺序消费，单队列串行处理
            try {
                for (MessageExt msg : msgs) {
                    processMessage(msg, listener);
                }
                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("Error in orderly listener, topic: {}, group: {}", context.getMessageQueue().getTopic(), consumerKey, e);
                // 顺序消费异常时，返回挂起状态，让RocketMQ自动处理
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        };
    }

    /**
     * 处理单条消息
     */
    private <T> void processMessage(MessageExt msg, MessageListener<T> listener) {
        try {
            String messageBody = new String(msg.getBody(), "UTF-8");
            // 使用泛型类型进行反序列化
            Class<T> messageType = getMessageType(listener);
            T message = JSON.parseObject(messageBody, messageType);
            log.debug("Processing message, msgId: {}, topic: {}, queueId: {}", msg.getMsgId(), msg.getTopic(), msg.getQueueId());
            listener.onMessage(message);
            log.debug("Message processed successfully, msgId: {}", msg.getMsgId());
        } catch (Exception e) {
            log.error("Failed to process message, msgId: {}, topic: {}", msg.getMsgId(), msg.getTopic(), e);
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
