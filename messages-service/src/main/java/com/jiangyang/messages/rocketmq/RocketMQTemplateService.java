package com.jiangyang.messages.rocketmq;

import com.jiangyang.messages.service.MessageService;
import com.jiangyang.messages.consume.MessageServiceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RocketMQ消息服务实现类 - 使用RocketMQTemplate
 * 提供RocketMQ消息中间件的具体实现，基于Spring Boot的RocketMQTemplate
 */
@Slf4j
@Service
public class RocketMQTemplateService implements MessageService {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    private final ConcurrentHashMap<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> messageDedupMap = new ConcurrentHashMap<>();

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
            // 检查RocketMQTemplate是否可用
            if (rocketMQTemplate == null) {
                log.error("RocketMQTemplate未初始化，无法发送消息: topic={}, tag={}, key={}", topic, tag, key);
                return false;
            }

            // 消息去重检查
            String dedupKey = generateDedupKey(topic, tag, key, content);
            if (isDuplicateMessage(dedupKey)) {
                log.warn("检测到重复消息，跳过发送: dedupKey={}", dedupKey);
                return true;
            }

            // 构建消息
            MessageBuilder<String> messageBuilder = MessageBuilder.withPayload(content);
            
            // 设置消息头
            if (tag != null && !tag.trim().isEmpty()) {
                messageBuilder.setHeader(RocketMQHeaders.TAGS, tag);
            }
            if (key != null && !key.trim().isEmpty()) {
                messageBuilder.setHeader(RocketMQHeaders.KEYS, key);
            }
            
            Message<String> message = messageBuilder.build();
            
            // 同步发送消息
            SendResult sendResult = rocketMQTemplate.syncSend(topic, message);
            
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
            Message<String> message = MessageBuilder.withPayload(content).build();
            
            // 发送延迟消息
            SendResult sendResult = rocketMQTemplate.syncSend(topic, message, 3000, delayLevel);
            
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
            Message<String> message = MessageBuilder.withPayload(content).build();
            
            // 发送顺序消息
            SendResult sendResult = rocketMQTemplate.syncSendOrderly(topic, message, hashKey);
            
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

            List<Message<String>> messageList = messages.stream()
                    .map(content -> MessageBuilder.withPayload(content).build())
                    .collect(java.util.stream.Collectors.toList());

            // 批量发送消息
            for (Message<String> message : messageList) {
                SendResult sendResult = rocketMQTemplate.syncSend(topic, message);
                if (!sendResult.getSendStatus().name().equals("SEND_OK")) {
                    log.error("RocketMQ批量消息发送失败: topic={}, status={}", 
                            topic, sendResult.getSendStatus());
                    return false;
                }
            }
            
            log.info("RocketMQ批量消息发送成功: topic={}, messageCount={}", 
                    topic, messages.size());
            return true;
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
            // 检查RocketMQTemplate是否可用
            return rocketMQTemplate != null;
        } catch (Exception e) {
            log.error("检查RocketMQ服务状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void shutdown() {
        try {
            if (rocketMQTemplate != null) {
                // RocketMQTemplate由Spring管理，不需要手动关闭
                log.info("RocketMQ消息服务已关闭");
            }
        } catch (Exception e) {
            log.error("关闭RocketMQ消息服务失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean sendTransactionMessage(String topic, String tag, String messageBody, String transactionId, String businessKey, int timeout) {
        return false;
    }

    /**
     * 异步发送消息
     */
    public void sendMessageAsync(String topic, String tag, String key, String content, SendCallback callback) {
        try {
            MessageBuilder<String> messageBuilder = MessageBuilder.withPayload(content);
            
            if (tag != null && !tag.trim().isEmpty()) {
                messageBuilder.setHeader(RocketMQHeaders.TAGS, tag);
            }
            if (key != null && !key.trim().isEmpty()) {
                messageBuilder.setHeader(RocketMQHeaders.KEYS, key);
            }
            
            Message<String> message = messageBuilder.build();
            rocketMQTemplate.asyncSend(topic, message, callback);
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
}
