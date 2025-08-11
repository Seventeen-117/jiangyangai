package com.jiangyang.messages.rocketmq;

import com.jiangyang.messages.MessageService;
import com.jiangyang.messages.MessageServiceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RocketMQ消息服务实现类
 * 提供RocketMQ消息中间件的具体实现
 */
@Slf4j
@Service
public class RocketMQMessageService implements MessageService {

    @Value("${rocketmq.name-server:localhost:9876}")
    private String nameServer;

    @Value("${rocketmq.producer-group:default-producer-group}")
    private String producerGroup;

    @Value("${rocketmq.max-retry-times:3}")
    private int maxRetryTimes;

    @Value("${rocketmq.send-msg-timeout:3000}")
    private int sendMsgTimeout;

    private DefaultMQProducer producer;
    private final ConcurrentHashMap<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> messageDedupMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            producer = new DefaultMQProducer(producerGroup);
            producer.setNamesrvAddr(nameServer);
            producer.setRetryTimesWhenSendAsyncFailed(maxRetryTimes);
            producer.setSendMsgTimeout(sendMsgTimeout);
            producer.start();
            log.info("RocketMQ生产者启动成功: nameServer={}, producerGroup={}", nameServer, producerGroup);
        } catch (Exception e) {
            log.error("RocketMQ生产者启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("RocketMQ生产者启动失败", e);
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

            Message message = new Message(topic, tag, key, content.getBytes(StandardCharsets.UTF_8));
            
            // 同步发送消息
            SendResult sendResult = producer.send(message);
            
            if (sendResult.getSendStatus().name().equals("SEND_OK")) {
                log.info("RocketMQ消息发送成功: topic={}, tag={}, key={}, msgId={}", 
                        topic, tag, key, sendResult.getMsgId());
                
                // 记录消息去重
                recordMessageDedup(dedupKey);
                return true;
            } else {
                log.error("RocketMQ消息发送失败: topic={}, tag={}, key={}, status={}", 
                        topic, tag, key, sendResult.getSendStatus());
                return false;
            }
        } catch (Exception e) {
            log.error("RocketMQ消息发送异常: topic={}, tag={}, key={}, error={}", 
                    topic, tag, key, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendDelayMessage(String topic, String content, int delayLevel) {
        try {
            Message message = new Message(topic, content.getBytes(StandardCharsets.UTF_8));
            message.setDelayTimeLevel(delayLevel);
            
            SendResult sendResult = producer.send(message);
            
            if (sendResult.getSendStatus().name().equals("SEND_OK")) {
                log.info("RocketMQ延迟消息发送成功: topic={}, delayLevel={}, msgId={}", 
                        topic, delayLevel, sendResult.getMsgId());
                return true;
            } else {
                log.error("RocketMQ延迟消息发送失败: topic={}, delayLevel={}, status={}", 
                        topic, delayLevel, sendResult.getSendStatus());
                return false;
            }
        } catch (Exception e) {
            log.error("RocketMQ延迟消息发送异常: topic={}, delayLevel={}, error={}", 
                    topic, delayLevel, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendOrderedMessage(String topic, String content, String hashKey) {
        try {
            Message message = new Message(topic, content.getBytes(StandardCharsets.UTF_8));
            
            // 使用hashKey进行顺序消息发送
            SendResult sendResult = producer.send(message, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    int index = Math.abs(hashKey.hashCode()) % mqs.size();
                    return mqs.get(index);
                }
            }, hashKey);
            
            if (sendResult.getSendStatus().name().equals("SEND_OK")) {
                log.info("RocketMQ顺序消息发送成功: topic={}, hashKey={}, msgId={}", 
                        topic, hashKey, sendResult.getMsgId());
                return true;
            } else {
                log.error("RocketMQ顺序消息发送失败: topic={}, hashKey={}, status={}", 
                        topic, hashKey, sendResult.getSendStatus());
                return false;
            }
        } catch (Exception e) {
            log.error("RocketMQ顺序消息发送异常: topic={}, hashKey={}, error={}", 
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

            List<Message> messageList = messages.stream()
                    .map(content -> new Message(topic, content.getBytes(StandardCharsets.UTF_8)))
                    .collect(java.util.stream.Collectors.toList());

            SendResult sendResult = producer.send(messageList);
            
            if (sendResult.getSendStatus().name().equals("SEND_OK")) {
                log.info("RocketMQ批量消息发送成功: topic={}, messageCount={}, msgId={}", 
                        topic, messages.size(), sendResult.getMsgId());
                return true;
            } else {
                log.error("RocketMQ批量消息发送失败: topic={}, messageCount={}, status={}", 
                        topic, messages.size(), sendResult.getSendStatus());
                return false;
            }
        } catch (Exception e) {
            log.error("RocketMQ批量消息发送异常: topic={}, messageCount={}, error={}", 
                    topic, messages.size(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public MessageServiceType getServiceType() {
        return MessageServiceType.ROCKETMQ;
    }

    @Override
    public boolean isAvailable() {
        try {
            return producer != null && producer.getDefaultMQProducerImpl().getServiceState().name().equals("RUNNING");
        } catch (Exception e) {
            log.error("检查RocketMQ服务状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void shutdown() {
        try {
            if (producer != null) {
                producer.shutdown();
                log.info("RocketMQ消息服务已关闭");
            }
        } catch (Exception e) {
            log.error("关闭RocketMQ消息服务失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步发送消息
     */
    public void sendMessageAsync(String topic, String tag, String key, String content, SendCallback callback) {
        try {
            Message message = new Message(topic, tag, key, content.getBytes(StandardCharsets.UTF_8));
            producer.send(message, callback);
        } catch (Exception e) {
            log.error("RocketMQ异步消息发送异常: topic={}, tag={}, key={}, error={}", 
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

    // ==================== 配置方法 ====================
    
    /**
     * 设置Name Server地址
     */
    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
        // 如果生产者已经初始化，则重新设置
        if (producer != null) {
            producer.setNamesrvAddr(nameServer);
        }
    }

    /**
     * 设置生产者组
     */
    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
        // 如果生产者已经初始化，则重新设置
        if (producer != null) {
            producer.setProducerGroup(producerGroup);
        }
    }

    /**
     * 设置同步重试次数
     */
    public void setRetrySyncTimes(int retrySyncTimes) {
        this.maxRetryTimes = retrySyncTimes;
        // 如果生产者已经初始化，则重新设置
        if (producer != null) {
            producer.setRetryTimesWhenSendAsyncFailed(retrySyncTimes);
        }
    }

    /**
     * 设置异步重试次数
     */
    public void setRetryAsyncTimes(int retryAsyncTimes) {
        // 如果生产者已经初始化，则重新设置
        if (producer != null) {
            producer.setRetryTimesWhenSendAsyncFailed(retryAsyncTimes);
        }
    }

    /**
     * 设置发送消息超时时间
     */
    public void setSendMsgTimeout(int sendMsgTimeout) {
        this.sendMsgTimeout = sendMsgTimeout;
        // 如果生产者已经初始化，则重新设置
        if (producer != null) {
            producer.setSendMsgTimeout(sendMsgTimeout);
        }
    }

    /**
     * 获取Name Server地址
     */
    public String getNameServer() {
        return nameServer;
    }

    /**
     * 获取生产者组
     */
    public String getProducerGroup() {
        return producerGroup;
    }

    /**
     * 获取同步重试次数
     */
    public int getRetrySyncTimes() {
        return maxRetryTimes;
    }

    /**
     * 获取异步重试次数
     */
    public int getRetryAsyncTimes() {
        return maxRetryTimes;
    }
}
