package com.jiangyang.messages.service.impl;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.factory.MessageServiceFactory;
import com.jiangyang.messages.service.EnhancedMessageService;
import com.jiangyang.messages.service.MessageService;
import com.jiangyang.messages.utils.MessageServiceException;
import com.jiangyang.messages.utils.MessageServiceType;
import com.jiangyang.messages.utils.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 增强消息服务实现类
 * 根据消息类型动态路由到不同的消息中间件
 */
@Slf4j
@Service
@DataSource("master")
public class EnhancedMessageServiceImp implements EnhancedMessageService {

    @Autowired
    private MessageServiceFactory messageServiceFactory;

    @Override
    public boolean sendToRocketMQ(String messageId, String topic, String tag, MessageType messageType, String messageBody, Map<String, Object> parameters) {
        log.info("发送消息到RocketMQ: messageId={}, topic={}, tag={}, messageType={}", messageId, topic, tag, messageType);
        
        try {
            if (!messageServiceFactory.isServiceAvailable(MessageServiceType.ROCKETMQ)) {
                throw new MessageServiceException("RocketMQ服务不可用");
            }
            
            MessageService rocketMQService = messageServiceFactory.getMessageService(MessageServiceType.ROCKETMQ);
            boolean result = false;
            
            // 根据消息类型选择不同的发送方法
            switch (messageType) {
                case NORMAL:
                    // 普通消息：立即投递
                    result = rocketMQService.sendMessage(topic, tag, messageBody);
                    break;
                case DELAY:
                    // 定时消息：支持延迟级别配置
                    int delayLevel = (Integer) parameters.getOrDefault("delayLevel", 1);
                    result = rocketMQService.sendDelayMessage(topic, messageBody, delayLevel);
                    break;
                case ORDERED:
                    // 顺序消息：通过哈希键保证顺序
                    String hashKey = (String) parameters.getOrDefault("hashKey", messageId);
                    result = rocketMQService.sendOrderedMessage(topic, messageBody, hashKey);
                    break;
                case TRANSACTION:
                    // 事务消息：使用事务发送器，确保消息一致性
                    // 这里先使用普通消息发送，后续可扩展为真正的事务消息
                    result = rocketMQService.sendMessage(topic, tag, messageBody);
                    break;
                default:
                    throw new MessageServiceException("不支持的RocketMQ消息类型: " + messageType);
            }
            
            if (result) {
                log.info("RocketMQ消息发送成功: messageId={}, topic={}, tag={}, messageType={}", messageId, topic, tag, messageType);
            } else {
                log.error("RocketMQ消息发送失败: messageId={}, topic={}, tag={}, messageType={}", messageId, topic, tag, messageType);
            }
            
            return result;
        } catch (Exception e) {
            log.error("发送RocketMQ消息异常: messageId={}, topic={}, tag={}, messageType={}, error={}", 
                    messageId, topic, tag, messageType, e.getMessage(), e);
            throw new RuntimeException("发送RocketMQ消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendToKafka(String messageId, String topic, String key, MessageType messageType, String messageBody, Map<String, Object> parameters) {
        log.info("发送消息到Kafka: messageId={}, topic={}, key={}, messageType={}", messageId, topic, key, messageType);
        
        try {
            if (!messageServiceFactory.isServiceAvailable(MessageServiceType.KAFKA)) {
                throw new MessageServiceException("Kafka服务不可用");
            }
            
            MessageService kafkaService = messageServiceFactory.getMessageService(MessageServiceType.KAFKA);
            boolean result = false;
            
            // 根据消息类型选择不同的发送方法
            switch (messageType) {
                case SYNC_BLOCKING:
                    // 同步发送 - 阻塞：适用于交易订单、核心业务数据
                    // 高可靠性，中等性能
                    result = kafkaService.sendMessage(topic, null, key, messageBody);
                    break;
                case ASYNC_CALLBACK:
                    // 异步发送 - 带回调：适用于日志采集、业务通知
                    // 非阻塞，中等性能，高可靠性
                    result = kafkaService.sendMessage(topic, null, key, messageBody);
                    break;
                case ASYNC_NO_CALLBACK:
                    // 异步发送 - 无回调：适用于监控指标、非关键日志
                    // 非阻塞，极高性能，低可靠性
                    result = kafkaService.sendMessage(topic, null, key, messageBody);
                    break;
                default:
                    throw new MessageServiceException("不支持的Kafka消息类型: " + messageType);
            }
            
            if (result) {
                log.info("Kafka消息发送成功: messageId={}, topic={}, key={}, messageType={}", messageId, topic, key, messageType);
            } else {
                log.error("Kafka消息发送失败: messageId={}, topic={}, key={}, messageType={}", messageId, topic, key, messageType);
            }
            
            return result;
        } catch (Exception e) {
            log.error("发送Kafka消息异常: messageId={}, topic={}, key={}, messageType={}, error={}", 
                    messageId, topic, key, messageType, e.getMessage(), e);
            throw new RuntimeException("发送Kafka消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendToRabbitMQ(String messageId, String queueName, boolean durable, boolean exclusive, 
                                 boolean autoDelete, String exchange, String routingKey, 
                                 MessageType messageType, Map<String, Object> otherProperties, 
                                 String messageBody, Map<String, Object> parameters) {
        log.info("发送消息到RabbitMQ: messageId={}, queueName={}, exchange={}, routingKey={}, messageType={}", 
                messageId, queueName, exchange, routingKey, messageType);
        
        try {
            if (!messageServiceFactory.isServiceAvailable(MessageServiceType.RABBITMQ)) {
                throw new MessageServiceException("RabbitMQ服务不可用");
            }
            
            MessageService rabbitMQService = messageServiceFactory.getMessageService(MessageServiceType.RABBITMQ);
            boolean result = false;
            
            // 根据消息类型选择不同的发送方法
            switch (messageType) {
                case DIRECT:
                    // 直接交换机消息：精确路由匹配
                    result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
                    break;
                case TOPIC:
                    // 主题交换机消息：通配符路由匹配
                    result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
                    break;
                case FANOUT:
                    // 扇形交换机消息：广播到所有绑定队列
                    result = rabbitMQService.sendMessage(queueName, null, "", messageBody);
                    break;
                case HEADERS:
                    // 首部交换机消息：基于消息头属性匹配
                    result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
                    break;
                case PERSISTENT:
                    // 持久化消息：确保消息不丢失
                    result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
                    break;
                case TTL:
                    // 过期消息：设置消息存活时间
                    result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
                    break;
                case PRIORITY:
                    // 优先级消息：设置消息优先级
                    result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
                    break;
                case DEAD_LETTER:
                    // 死信消息：路由到死信队列
                    result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
                    break;
                default:
                    throw new MessageServiceException("不支持的RabbitMQ消息类型: " + messageType);
            }
            
            if (result) {
                log.info("RabbitMQ消息发送成功: messageId={}, queueName={}, routingKey={}, messageType={}", 
                        messageId, queueName, routingKey, messageType);
            } else {
                log.error("RabbitMQ消息发送失败: messageId={}, queueName={}, routingKey={}, messageType={}", 
                        messageId, queueName, routingKey, messageType);
            }
            
            return result;
        } catch (Exception e) {
            log.error("发送RabbitMQ消息异常: messageId={}, queueName={}, routingKey={}, messageType={}, error={}", 
                    messageId, queueName, routingKey, messageType, e.getMessage(), e);
            throw new RuntimeException("发送RabbitMQ消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendMessage(String messageId, String messageType, Map<String, Object> parameters) {
        log.info("动态发送消息: messageId={}, messageType={}, parameters={}", messageId, messageType, parameters);
        
        try {
            MessageServiceType type = MessageServiceType.fromCode(messageType.toUpperCase());
            
            // 获取消息类型，默认为普通消息
            String messageTypeStr = (String) parameters.getOrDefault("messageType", "NORMAL");
            MessageType msgType = MessageType.fromCode(messageTypeStr);
            
            switch (type) {
                case ROCKETMQ:
                    return sendToRocketMQ(
                        messageId,
                        (String) parameters.get("topic"),
                        (String) parameters.get("tag"),
                        msgType,
                        (String) parameters.get("messageBody"),
                        parameters
                    );
                    
                case KAFKA:
                    return sendToKafka(
                        messageId,
                        (String) parameters.get("topic"),
                        (String) parameters.get("key"),
                        msgType,
                        (String) parameters.get("messageBody"),
                        parameters
                    );
                    
                case RABBITMQ:
                    return sendToRabbitMQ(
                        messageId,
                        (String) parameters.get("queueName"),
                        (Boolean) parameters.getOrDefault("durable", true),
                        (Boolean) parameters.getOrDefault("exclusive", false),
                        (Boolean) parameters.getOrDefault("autoDelete", false),
                        (String) parameters.getOrDefault("exchange", ""),
                        (String) parameters.get("routingKey"),
                        msgType,
                        (Map<String, Object>) parameters.getOrDefault("otherProperties", Map.of()),
                        (String) parameters.get("messageBody"),
                        parameters
                    );
                    
                default:
                    throw new MessageServiceException("不支持的消息类型: " + messageType);
            }
        } catch (Exception e) {
            log.error("动态发送消息失败: messageId={}, messageType={}, error={}", 
                    messageId, messageType, e.getMessage(), e);
            throw new RuntimeException("动态发送消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public MessageServiceType getServiceType() {
        // 这是一个复合服务，返回默认类型
        return MessageServiceType.ROCKETMQ;
    }

    @Override
    public boolean isAvailable() {
        // 检查至少有一种消息服务可用
        return messageServiceFactory.isServiceAvailable(MessageServiceType.ROCKETMQ) ||
               messageServiceFactory.isServiceAvailable(MessageServiceType.KAFKA) ||
               messageServiceFactory.isServiceAvailable(MessageServiceType.RABBITMQ);
    }

    @Override
    public void shutdown() {
        // 关闭所有可用的消息服务
        try {
            if (messageServiceFactory.isServiceAvailable(MessageServiceType.ROCKETMQ)) {
                MessageService rocketMQService = messageServiceFactory.getMessageService(MessageServiceType.ROCKETMQ);
                rocketMQService.shutdown();
            }
            if (messageServiceFactory.isServiceAvailable(MessageServiceType.KAFKA)) {
                MessageService kafkaService = messageServiceFactory.getMessageService(MessageServiceType.KAFKA);
                kafkaService.shutdown();
            }
            if (messageServiceFactory.isServiceAvailable(MessageServiceType.RABBITMQ)) {
                MessageService rabbitMQService = messageServiceFactory.getMessageService(MessageServiceType.RABBITMQ);
                rabbitMQService.shutdown();
            }
            log.info("所有消息服务已关闭");
        } catch (Exception e) {
            log.error("关闭消息服务时发生异常: {}", e.getMessage(), e);
        }
    }
}
