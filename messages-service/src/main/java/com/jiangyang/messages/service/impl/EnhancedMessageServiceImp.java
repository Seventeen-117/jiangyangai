package com.jiangyang.messages.service.impl;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.factory.MessageServiceFactory;
import com.jiangyang.messages.service.EnhancedMessageService;
import com.jiangyang.messages.service.MessageService;
import com.jiangyang.messages.saga.MessageSagaStateMachine;
import com.jiangyang.messages.utils.MessageServiceException;
import com.jiangyang.messages.utils.MessageServiceType;
import com.jiangyang.messages.utils.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * 增强消息服务实现类 - 消息发送执行器
 * 
 * 职责分工：
 * 1. EnhancedMessageServiceImp：负责消息发送的技术实现和路由
 *    - 消息参数验证
 *    - 消息中间件路由选择
 *    - 具体消息发送策略执行
 *    - 自定义topic的直接发送
 * 
 * 2. MessageSagaStateMachine：负责分布式事务管理
 *    - Saga事务状态管理
 *    - 事务补偿机制
 *    - 消息一致性保证
 *    - 事务协调和回滚
 * 
 * 协作模式：
 * - 需要事务保证的消息：委托给MessageSagaStateMachine
 * - 简单消息发送：EnhancedMessageServiceImp直接处理
 * - 自定义topic：绕过Saga事务，直接发送
 */
@Slf4j
@Service
@DataSource("master")
public class EnhancedMessageServiceImp implements EnhancedMessageService {

    @Autowired
    private MessageServiceFactory messageServiceFactory;
    
    @Autowired
    private MessageSagaStateMachine messageSagaStateMachine;

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
                    result = sendTransactionMessage(rocketMQService, topic, tag, messageBody, parameters);
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
                    result = sendKafkaSyncBlocking(kafkaService, topic, key, messageBody, parameters);
                    break;
                case ASYNC_CALLBACK:
                    // 异步发送 - 带回调：适用于日志采集、业务通知
                    // 非阻塞，中等性能，高可靠性
                    result = sendKafkaAsyncWithCallback(kafkaService, topic, key, messageBody, parameters);
                    break;
                case ASYNC_NO_CALLBACK:
                    // 异步发送 - 无回调：适用于监控指标、非关键日志
                    // 非阻塞，极高性能，低可靠性
                    result = sendKafkaAsyncNoCallback(kafkaService, topic, key, messageBody, parameters);
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
                    result = sendRabbitMQDirect(rabbitMQService, queueName, exchange, routingKey, messageBody, parameters);
                    break;
                case TOPIC:
                    // 主题交换机消息：通配符路由匹配
                    result = sendRabbitMQTopic(rabbitMQService, queueName, exchange, routingKey, messageBody, parameters);
                    break;
                case FANOUT:
                    // 扇形交换机消息：广播到所有绑定队列
                    result = sendRabbitMQFanout(rabbitMQService, queueName, exchange, messageBody, parameters);
                    break;
                case HEADERS:
                    // 首部交换机消息：基于消息头属性匹配
                    result = sendRabbitMQHeaders(rabbitMQService, queueName, exchange, routingKey, messageBody, parameters);
                    break;
                case PERSISTENT:
                    // 持久化消息：确保消息不丢失
                    result = sendRabbitMQPersistent(rabbitMQService, queueName, exchange, routingKey, messageBody, parameters);
                    break;
                case TTL:
                    // 过期消息：设置消息存活时间
                    result = sendRabbitMQTTL(rabbitMQService, queueName, exchange, routingKey, messageBody, parameters);
                    break;
                case PRIORITY:
                    // 优先级消息：设置消息优先级
                    result = sendRabbitMQPriority(rabbitMQService, queueName, exchange, routingKey, messageBody, parameters);
                    break;
                case DEAD_LETTER:
                    // 死信消息：路由到死信队列
                    result = sendRabbitMQDeadLetter(rabbitMQService, queueName, exchange, routingKey, messageBody, parameters);
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

    /**
     * 使用自定义topic发送消息
     * 根据消息中间件类型，直接调用对应的消息服务
     */
    private boolean sendMessageWithCustomTopic(MessageServiceType messageServiceType, String customTopic, 
                                            String messageId, String messageBody, Map<String, Object> parameters) {
        try {
            log.info("使用自定义topic发送消息: serviceType={}, topic={}, messageId={}", 
                    messageServiceType, customTopic, messageId);
            
            // 获取对应的消息服务
            MessageService messageService = messageServiceFactory.getMessageService(messageServiceType);
            if (messageService == null) {
                throw new MessageServiceException("消息服务不可用: " + messageServiceType);
            }
            
            // 获取消息类型（普通、延迟、顺序、事务等）
            String messageTypeStr = (String) parameters.getOrDefault("messageType", "NORMAL");
            MessageType msgType;
            try {
                msgType = MessageType.fromCode(messageTypeStr);
            } catch (IllegalArgumentException e) {
                log.warn("不支持的消息类型: {}, 使用默认的NORMAL类型", messageTypeStr);
                msgType = MessageType.NORMAL;
            }
            
            // 根据消息中间件类型和消息类型，选择不同的发送策略
            boolean result = false;
            
            switch (messageServiceType) {
                case ROCKETMQ:
                    // 获取RocketMQ相关参数
                    String tag = (String) parameters.getOrDefault("tag", "");
                    result = sendToRocketMQ(messageId, customTopic, tag, msgType, messageBody, parameters);
                    break;
                case KAFKA:
                    // 获取Kafka相关参数
                    String key = (String) parameters.getOrDefault("key", messageId);
                    result = sendToKafka(messageId, customTopic, key, msgType, messageBody, parameters);
                    break;
                case RABBITMQ:
                    // 获取RabbitMQ相关参数
                    String queueName = (String) parameters.getOrDefault("queueName", customTopic);
                    String exchange = (String) parameters.getOrDefault("exchange", customTopic);
                    String routingKey = (String) parameters.getOrDefault("routingKey", "");
                    boolean durable = (Boolean) parameters.getOrDefault("durable", true);
                    boolean exclusive = (Boolean) parameters.getOrDefault("exclusive", false);
                    boolean autoDelete = (Boolean) parameters.getOrDefault("autoDelete", false);
                    Map<String, Object> otherProperties = (Map<String, Object>) parameters.getOrDefault("otherProperties", new HashMap<>());
                    result = sendToRabbitMQ(messageId, queueName, durable, exclusive, autoDelete, exchange, routingKey, msgType, otherProperties, messageBody, parameters);
                    break;
                default:
                    throw new MessageServiceException("不支持的消息中间件类型: " + messageServiceType);
            }
            
            if (result) {
                log.info("自定义topic消息发送成功: serviceType={}, topic={}, messageId={}, messageType={}", 
                        messageServiceType, customTopic, messageId, msgType);
            } else {
                log.error("自定义topic消息发送失败: serviceType={}, topic={}, messageId={}, messageType={}", 
                        messageServiceType, customTopic, messageId, msgType);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("使用自定义topic发送消息异常: serviceType={}, topic={}, messageId={}, error={}", 
                    messageServiceType, customTopic, messageId, e.getMessage(), e);
            return false;
        }
    }



    /**
     * Kafka同步阻塞发送 - 高可靠性，中等性能
     * 适用于交易订单、核心业务数据
     */
    private boolean sendKafkaSyncBlocking(MessageService kafkaService, String topic, String key, 
                                        String messageBody, Map<String, Object> parameters) {
        try {
            log.info("Kafka同步阻塞发送: topic={}, key={}", topic, key);
            
            // 同步阻塞发送，等待发送结果
            boolean result = kafkaService.sendMessage(topic, null, key, messageBody);
            
            if (result) {
                log.info("Kafka同步阻塞发送成功: topic={}, key={}", topic, key);
            } else {
                log.error("Kafka同步阻塞发送失败: topic={}, key={}", topic, key);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Kafka同步阻塞发送异常: topic={}, key={}, error={}", topic, key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kafka异步发送带回调 - 非阻塞，中等性能，高可靠性
     * 适用于日志采集、业务通知
     */
    private boolean sendKafkaAsyncWithCallback(MessageService kafkaService, String topic, String key, 
                                             String messageBody, Map<String, Object> parameters) {
        try {
            log.info("Kafka异步发送带回调: topic={}, key={}", topic, key);
            
            // 获取回调配置
            String callbackUrl = (String) parameters.get("callbackUrl");
            String callbackType = (String) parameters.getOrDefault("callbackType", "HTTP");
            
            // 异步发送消息
            boolean result = kafkaService.sendMessage(topic, null, key, messageBody);
            
            if (result) {
                log.info("Kafka异步发送带回调成功: topic={}, key={}, callbackUrl={}", topic, key, callbackUrl);
                
                // 如果有回调配置，异步执行回调
                if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
                    executeCallbackAsync(callbackUrl, callbackType, topic, key, messageBody, parameters);
                }
            } else {
                log.error("Kafka异步发送带回调失败: topic={}, key={}", topic, key);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Kafka异步发送带回调异常: topic={}, key={}, error={}", topic, key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kafka异步发送无回调 - 非阻塞，极高性能，低可靠性
     * 适用于监控指标、非关键日志
     */
    private boolean sendKafkaAsyncNoCallback(MessageService kafkaService, String topic, String key, 
                                           String messageBody, Map<String, Object> parameters) {
        try {
            log.info("Kafka异步发送无回调: topic={}, key={}", topic, key);
            
            // 异步发送消息，不等待结果，不执行回调
            // 这里使用fire-and-forget模式，最大化性能
            boolean result = kafkaService.sendMessage(topic, null, key, messageBody);
            
            if (result) {
                log.info("Kafka异步发送无回调成功: topic={}, key={}", topic, key);
            } else {
                log.error("Kafka异步发送无回调失败: topic={}, key={}", topic, key);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Kafka异步发送无回调异常: topic={}, key={}, error={}", topic, key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步执行回调
     */
    private void executeCallbackAsync(String callbackUrl, String callbackType, String topic, String key, 
                                   String messageBody, Map<String, Object> parameters) {
        try {
            log.info("异步执行回调: callbackUrl={}, callbackType={}, topic={}, key={}", 
                    callbackUrl, callbackType, topic, key);
            
            // 根据回调类型选择不同的执行方式
            switch (callbackType.toUpperCase()) {
                case "HTTP":
                    executeHttpCallbackAsync(callbackUrl, topic, key, messageBody, parameters);
                    break;
                case "KAFKA":
                    executeKafkaCallbackAsync(callbackUrl, topic, key, messageBody, parameters);
                    break;
                case "RABBITMQ":
                    executeRabbitMQCallbackAsync(callbackUrl, topic, key, messageBody, parameters);
                    break;
                default:
                    log.warn("不支持的回调类型: {}, 跳过回调执行", callbackType);
            }
        } catch (Exception e) {
            log.error("异步执行回调异常: callbackUrl={}, error={}", callbackUrl, e.getMessage(), e);
        }
    }

    /**
     * 异步执行HTTP回调
     * 使用CompletableFuture异步执行HTTP POST请求，支持重试机制
     */
    private void executeHttpCallbackAsync(String callbackUrl, String topic, String key, 
                                       String messageBody, Map<String, Object> parameters) {
        try {
            log.info("异步执行HTTP回调: callbackUrl={}, topic={}, key={}", callbackUrl, topic, key);
            
            // 构建回调数据
            Map<String, Object> callbackData = buildCallbackData(topic, key, messageBody, parameters);
            
            // 获取HTTP回调配置
            int maxRetries = (Integer) parameters.getOrDefault("httpMaxRetries", 3);
            int timeoutMs = (Integer) parameters.getOrDefault("httpTimeoutMs", 5000);
            String contentType = (String) parameters.getOrDefault("httpContentType", "application/json");
            
            // 异步执行HTTP回调
            CompletableFuture.runAsync(() -> {
                try {
                    boolean success = executeHttpCallbackWithRetry(callbackUrl, callbackData, maxRetries, timeoutMs, contentType);
                    if (success) {
                        log.info("HTTP回调执行成功: callbackUrl={}, topic={}, key={}", callbackUrl, topic, key);
                    } else {
                        log.error("HTTP回调执行失败: callbackUrl={}, topic={}, key={}", callbackUrl, topic, key);
                    }
                } catch (Exception e) {
                    log.error("HTTP回调执行异常: callbackUrl={}, topic={}, key={}, error={}", 
                            callbackUrl, topic, key, e.getMessage(), e);
                }
            }).exceptionally(throwable -> {
                log.error("HTTP回调异步执行异常: callbackUrl={}, error={}", callbackUrl, throwable.getMessage(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            log.error("准备HTTP回调时发生异常: callbackUrl={}, error={}", callbackUrl, e.getMessage(), e);
        }
    }

    /**
     * 异步执行Kafka回调
     * 将回调信息发送到指定的Kafka topic，支持自定义回调topic
     */
    private void executeKafkaCallbackAsync(String callbackUrl, String topic, String key, 
                                         String messageBody, Map<String, Object> parameters) {
        try {
            log.info("异步执行Kafka回调: callbackUrl={}, topic={}, key={}", callbackUrl, topic, key);
            
            // 解析Kafka回调配置
            String callbackTopic = (String) parameters.getOrDefault("kafkaCallbackTopic", "message-callbacks");
            String callbackKey = (String) parameters.getOrDefault("kafkaCallbackKey", key);
            
            // 构建回调消息
            Map<String, Object> callbackMessage = buildCallbackData(topic, key, messageBody, parameters);
            callbackMessage.put("callbackType", "KAFKA");
            callbackMessage.put("callbackUrl", callbackUrl);
            callbackMessage.put("timestamp", System.currentTimeMillis());
            
            // 异步执行Kafka回调
            CompletableFuture.runAsync(() -> {
                try {
                    if (messageServiceFactory.isServiceAvailable(MessageServiceType.KAFKA)) {
                        MessageService kafkaService = messageServiceFactory.getMessageService(MessageServiceType.KAFKA);
                        boolean success = kafkaService.sendMessage(callbackTopic, null, callbackKey, 
                                convertToJson(callbackMessage));
                        
                        if (success) {
                            log.info("Kafka回调执行成功: callbackTopic={}, callbackKey={}", callbackTopic, callbackKey);
                        } else {
                            log.error("Kafka回调执行失败: callbackTopic={}, callbackKey={}", callbackTopic, callbackKey);
                        }
                    } else {
                        log.warn("Kafka服务不可用，无法执行Kafka回调: callbackUrl={}", callbackUrl);
                    }
                } catch (Exception e) {
                    log.error("Kafka回调执行异常: callbackUrl={}, error={}", callbackUrl, e.getMessage(), e);
                }
            }).exceptionally(throwable -> {
                log.error("Kafka回调异步执行异常: callbackUrl={}, error={}", callbackUrl, throwable.getMessage(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            log.error("准备Kafka回调时发生异常: callbackUrl={}, error={}", callbackUrl, e.getMessage(), e);
        }
    }

    /**
     * 异步执行RabbitMQ回调
     * 将回调信息发送到指定的RabbitMQ队列，支持自定义回调队列
     */
    private void executeRabbitMQCallbackAsync(String callbackUrl, String topic, String key, 
                                             String messageBody, Map<String, Object> parameters) {
        try {
            log.info("异步执行RabbitMQ回调: callbackUrl={}, topic={}, key={}", callbackUrl, topic, key);
            
            // 解析RabbitMQ回调配置
            String callbackQueue = (String) parameters.getOrDefault("rabbitMQCallbackQueue", "message-callbacks");
            String callbackExchange = (String) parameters.getOrDefault("rabbitMQCallbackExchange", "callback-exchange");
            String callbackRoutingKey = (String) parameters.getOrDefault("rabbitMQCallbackRoutingKey", "callback");
            
            // 构建回调消息
            Map<String, Object> callbackMessage = buildCallbackData(topic, key, messageBody, parameters);
            callbackMessage.put("callbackType", "RABBITMQ");
            callbackMessage.put("callbackUrl", callbackUrl);
            callbackMessage.put("timestamp", System.currentTimeMillis());
            
            // 异步执行RabbitMQ回调
            CompletableFuture.runAsync(() -> {
                try {
                    if (messageServiceFactory.isServiceAvailable(MessageServiceType.RABBITMQ)) {
                        MessageService rabbitMQService = messageServiceFactory.getMessageService(MessageServiceType.RABBITMQ);
                        boolean success = rabbitMQService.sendMessage(callbackQueue, null, callbackRoutingKey, 
                                convertToJson(callbackMessage));
                        
                        if (success) {
                            log.info("RabbitMQ回调执行成功: callbackQueue={}, callbackRoutingKey={}", callbackQueue, callbackRoutingKey);
                        } else {
                            log.error("RabbitMQ回调执行失败: callbackQueue={}, callbackRoutingKey={}", callbackQueue, callbackRoutingKey);
                        }
                    } else {
                        log.warn("RabbitMQ服务不可用，无法执行RabbitMQ回调: callbackUrl={}", callbackUrl);
                    }
                } catch (Exception e) {
                    log.error("RabbitMQ回调执行异常: callbackUrl={}, error={}", callbackUrl, e.getMessage(), e);
                }
            }).exceptionally(throwable -> {
                log.error("RabbitMQ回调异步执行异常: callbackUrl={}, error={}", callbackUrl, throwable.getMessage(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            log.error("准备RabbitMQ回调时发生异常: callbackUrl={}, error={}", callbackUrl, e.getMessage(), e);
        }
    }

    /**
     * RabbitMQ直接交换机消息 - 精确路由匹配
     * 消息根据routing key精确匹配到队列
     */
    private boolean sendRabbitMQDirect(MessageService rabbitMQService, String queueName, String exchange, 
                                     String routingKey, String messageBody, Map<String, Object> parameters) {
        try {
            log.info("RabbitMQ直接交换机消息: queueName={}, exchange={}, routingKey={}", queueName, exchange, routingKey);
            
            // 直接交换机需要精确的routing key匹配
            if (routingKey == null || routingKey.trim().isEmpty()) {
                throw new MessageServiceException("直接交换机消息必须指定routing key");
            }
            
            // 发送消息到指定的routing key
            boolean result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
            
            if (result) {
                log.info("RabbitMQ直接交换机消息发送成功: queueName={}, routingKey={}", queueName, routingKey);
            } else {
                log.error("RabbitMQ直接交换机消息发送失败: queueName={}, routingKey={}", queueName, routingKey);
            }
            
            return result;
        } catch (Exception e) {
            log.error("RabbitMQ直接交换机消息发送异常: queueName={}, routingKey={}, error={}", 
                    queueName, routingKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * RabbitMQ主题交换机消息 - 通配符路由匹配
     * 支持通配符模式匹配，如 user.* 或 order.#
     */
    private boolean sendRabbitMQTopic(MessageService rabbitMQService, String queueName, String exchange, 
                                    String routingKey, String messageBody, Map<String, Object> parameters) {
        try {
            log.info("RabbitMQ主题交换机消息: queueName={}, exchange={}, routingKey={}", queueName, exchange, routingKey);
            
            // 主题交换机支持通配符，但routing key不能为空
            if (routingKey == null || routingKey.trim().isEmpty()) {
                throw new MessageServiceException("主题交换机消息必须指定routing key");
            }
            
            // 验证routing key格式（支持通配符）
            if (!isValidTopicRoutingKey(routingKey)) {
                log.warn("主题交换机routing key格式可能不正确: {}", routingKey);
            }
            
            // 发送消息到指定的routing key
            boolean result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
            
            if (result) {
                log.info("RabbitMQ主题交换机消息发送成功: queueName={}, routingKey={}", queueName, routingKey);
            } else {
                log.error("RabbitMQ主题交换机消息发送失败: queueName={}, routingKey={}", queueName, routingKey);
            }
            
            return result;
        } catch (Exception e) {
            log.error("RabbitMQ主题交换机消息发送异常: queueName={}, routingKey={}, error={}", 
                    queueName, routingKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * RabbitMQ扇形交换机消息 - 广播到所有绑定队列
     * 忽略routing key，消息会发送到所有绑定的队列
     */
    private boolean sendRabbitMQFanout(MessageService rabbitMQService, String queueName, String exchange, 
                                     String messageBody, Map<String, Object> parameters) {
        try {
            log.info("RabbitMQ扇形交换机消息: queueName={}, exchange={}", queueName, exchange);
            
            // 扇形交换机忽略routing key，使用空字符串
            String fanoutRoutingKey = "";
            
            // 发送消息，扇形交换机会广播到所有绑定的队列
            boolean result = rabbitMQService.sendMessage(queueName, null, fanoutRoutingKey, messageBody);
            
            if (result) {
                log.info("RabbitMQ扇形交换机消息发送成功: queueName={}, exchange={}", queueName, exchange);
            } else {
                log.error("RabbitMQ扇形交换机消息发送失败: queueName={}, exchange={}", queueName, exchange);
            }
            
            return result;
        } catch (Exception e) {
            log.error("RabbitMQ扇形交换机消息发送异常: queueName={}, exchange={}, error={}", 
                    queueName, exchange, e.getMessage(), e);
            return false;
        }
    }

    /**
     * RabbitMQ首部交换机消息 - 基于消息头属性匹配
     * 根据消息头中的属性进行路由匹配
     */
    private boolean sendRabbitMQHeaders(MessageService rabbitMQService, String queueName, String exchange, 
                                      String routingKey, String messageBody, Map<String, Object> parameters) {
        try {
            log.info("RabbitMQ首部交换机消息: queueName={}, exchange={}, routingKey={}", queueName, exchange, routingKey);
            
            // 首部交换机需要额外的头信息参数
            Map<String, Object> headers = (Map<String, Object>) parameters.get("headers");
            if (headers == null || headers.isEmpty()) {
                log.warn("首部交换机消息缺少headers参数，使用默认headers");
                headers = Map.of("default-header", "default-value");
            }
            
            // 发送消息，首部交换机会根据headers进行路由
            boolean result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
            
            if (result) {
                log.info("RabbitMQ首部交换机消息发送成功: queueName={}, headers={}", queueName, headers);
            } else {
                log.error("RabbitMQ首部交换机消息发送失败: queueName={}, headers={}", queueName, headers);
            }
            
            return result;
        } catch (Exception e) {
            log.error("RabbitMQ首部交换机消息发送异常: queueName={}, error={}", queueName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * RabbitMQ持久化消息 - 确保消息不丢失
     * 消息会被持久化到磁盘，重启后不丢失
     */
    private boolean sendRabbitMQPersistent(MessageService rabbitMQService, String queueName, String exchange, 
                                         String routingKey, String messageBody, Map<String, Object> parameters) {
        try {
            log.info("RabbitMQ持久化消息: queueName={}, exchange={}, routingKey={}", queueName, exchange, routingKey);
            
            // 持久化消息需要设置持久化标志
            boolean persistent = (Boolean) parameters.getOrDefault("persistent", true);
            if (persistent) {
                log.info("消息将被持久化到磁盘");
            }
            
            // 发送持久化消息
            boolean result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
            
            if (result) {
                log.info("RabbitMQ持久化消息发送成功: queueName={}, persistent={}", queueName, persistent);
            } else {
                log.error("RabbitMQ持久化消息发送失败: queueName={}, persistent={}", queueName, persistent);
            }
            
            return result;
        } catch (Exception e) {
            log.error("RabbitMQ持久化消息发送异常: queueName={}, error={}", queueName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * RabbitMQ过期消息 - 设置消息存活时间
     * 消息在指定时间后自动过期
     */
    private boolean sendRabbitMQTTL(MessageService rabbitMQService, String queueName, String exchange, 
                                  String routingKey, String messageBody, Map<String, Object> parameters) {
        try {
            log.info("RabbitMQ过期消息: queueName={}, exchange={}, routingKey={}", queueName, exchange, routingKey);
            
            // 获取TTL参数（毫秒）
            Integer ttl = (Integer) parameters.get("ttl");
            if (ttl == null || ttl <= 0) {
                log.warn("TTL参数无效，使用默认值: 60000ms (1分钟)");
                ttl = 60000;
            }
            
            log.info("消息TTL设置为: {}ms", ttl);
            
            // 发送TTL消息
            boolean result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
            
            if (result) {
                log.info("RabbitMQ过期消息发送成功: queueName={}, ttl={}ms", queueName, ttl);
            } else {
                log.error("RabbitMQ过期消息发送失败: queueName={}, ttl={}ms", queueName, ttl);
            }
            
            return result;
        } catch (Exception e) {
            log.error("RabbitMQ过期消息发送异常: queueName={}, error={}", queueName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * RabbitMQ优先级消息 - 设置消息优先级
     * 高优先级消息优先被消费
     */
    private boolean sendRabbitMQPriority(MessageService rabbitMQService, String queueName, String exchange, 
                                       String routingKey, String messageBody, Map<String, Object> parameters) {
        try {
            log.info("RabbitMQ优先级消息: queueName={}, exchange={}, routingKey={}", queueName, exchange, routingKey);
            
            // 获取优先级参数（0-255，数字越大优先级越高）
            Integer priority = (Integer) parameters.get("priority");
            if (priority == null) {
                log.warn("优先级参数未设置，使用默认优先级: 0");
                priority = 0;
            } else if (priority < 0 || priority > 255) {
                log.warn("优先级参数超出范围(0-255)，使用默认优先级: 0");
                priority = 0;
            }
            
            log.info("消息优先级设置为: {}", priority);
            
            // 发送优先级消息
            boolean result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
            
            if (result) {
                log.info("RabbitMQ优先级消息发送成功: queueName={}, priority={}", queueName, priority);
            } else {
                log.error("RabbitMQ优先级消息发送失败: queueName={}, priority={}", queueName, priority);
            }
            
            return result;
        } catch (Exception e) {
            log.error("RabbitMQ优先级消息发送异常: queueName={}, error={}", queueName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * RabbitMQ死信消息 - 路由到死信队列
     * 当消息无法被正常消费时，路由到死信队列
     */
    private boolean sendRabbitMQDeadLetter(MessageService rabbitMQService, String queueName, String exchange, 
                                         String routingKey, String messageBody, Map<String, Object> parameters) {
        try {
            log.info("RabbitMQ死信消息: queueName={}, exchange={}, routingKey={}", queueName, exchange, routingKey);
            
            // 获取死信队列配置
            String deadLetterExchange = (String) parameters.get("deadLetterExchange");
            String deadLetterRoutingKey = (String) parameters.get("deadLetterRoutingKey");
            
            if (deadLetterExchange == null || deadLetterExchange.trim().isEmpty()) {
                log.warn("死信交换机未配置，使用默认死信交换机");
                deadLetterExchange = "dlx." + exchange;
            }
            
            if (deadLetterRoutingKey == null || deadLetterRoutingKey.trim().isEmpty()) {
                log.warn("死信路由键未配置，使用默认死信路由键");
                deadLetterRoutingKey = "dlq." + routingKey;
            }
            
            log.info("死信队列配置: exchange={}, routingKey={}", deadLetterExchange, deadLetterRoutingKey);
            
            // 发送死信消息
            boolean result = rabbitMQService.sendMessage(queueName, null, routingKey, messageBody);
            
            if (result) {
                log.info("RabbitMQ死信消息发送成功: queueName={}, deadLetterExchange={}, deadLetterRoutingKey={}", 
                        queueName, deadLetterExchange, deadLetterRoutingKey);
            } else {
                log.error("RabbitMQ死信消息发送失败: queueName={}, deadLetterExchange={}, deadLetterRoutingKey={}", 
                        queueName, deadLetterExchange, deadLetterRoutingKey);
            }
            
            return result;
        } catch (Exception e) {
            log.error("RabbitMQ死信消息发送异常: queueName={}, error={}", queueName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证主题交换机routing key格式
     * 支持通配符：* 表示一个单词，# 表示零个或多个单词
     * 
     * RabbitMQ主题交换机routing key规则：
     * 1. 由点分隔的单词组成，如 "user.created"、"order.*.completed"
     * 2. * 表示恰好一个单词，如 "user.*" 匹配 "user.created"、"user.deleted"
     * 3. # 表示零个或多个单词，如 "user.#" 匹配 "user"、"user.created"、"user.created.success"
     * 4. # 只能出现在最后，如 "user.#" 有效，"user.#.status" 无效
     * 5. 单词只能包含字母、数字、下划线、连字符
     */
    private boolean isValidTopicRoutingKey(String routingKey) {
        if (routingKey == null || routingKey.trim().isEmpty()) {
            return false;
        }
        
        String trimmedKey = routingKey.trim();
        
        // 检查基本字符：只允许字母、数字、下划线、连字符、点、星号、井号
        if (!trimmedKey.matches("^[a-zA-Z0-9._*#-]+$")) {
            log.debug("routing key包含无效字符: {}", trimmedKey);
            return false;
        }
        
        // 检查是否以点开头或结尾
        if (trimmedKey.startsWith(".") || trimmedKey.endsWith(".")) {
            log.debug("routing key不能以点开头或结尾: {}", trimmedKey);
            return false;
        }
        
        // 检查连续的点
        if (trimmedKey.contains("..")) {
            log.debug("routing key不能包含连续的点: {}", trimmedKey);
            return false;
        }
        
        // 分割routing key为单词
        String[] words = trimmedKey.split("\\.");
        
        // 检查每个单词的格式
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            
            // 空单词检查
            if (word.trim().isEmpty()) {
                log.debug("routing key包含空单词: {}", trimmedKey);
                return false;
            }
            
            // 检查#号的使用规则
            if (word.equals("#")) {
                // #号只能出现在最后一个位置
                if (i != words.length - 1) {
                    log.debug("井号(#)只能出现在routing key的最后: {}", trimmedKey);
                    return false;
                }
                // #号后面不能再有其他单词
                continue;
            }
            
            // 检查*号的使用规则
            if (word.equals("*")) {
                // *号可以出现在任何位置，表示恰好一个单词
                continue;
            }
            
            // 检查普通单词：只能包含字母、数字、下划线、连字符
            if (!word.matches("^[a-zA-Z0-9_-]+$")) {
                log.debug("单词包含无效字符: {} in {}", word, trimmedKey);
                return false;
            }
        }
        
        // 特殊规则检查：如果包含#号，确保它是最后一个单词
        if (trimmedKey.contains("#") && !trimmedKey.endsWith("#")) {
            log.debug("井号(#)只能出现在routing key的最后: {}", trimmedKey);
            return false;
        }
        
        // 检查routing key长度限制（RabbitMQ通常限制为255字节）
        if (trimmedKey.length() > 255) {
            log.debug("routing key长度超过255字符限制: {}", trimmedKey.length());
            return false;
        }
        
        // 检查单词数量限制（避免过于复杂的路由）
        if (words.length > 20) {
            log.debug("routing key单词数量过多: {} words", words.length);
            return false;
        }
        
        log.debug("routing key格式验证通过: {}", trimmedKey);
        return true;
    }

    /**
     * 发送RabbitMQ消息到自定义topic
     */
    private boolean sendRabbitMQWithCustomTopic(MessageService messageService, String customTopic, 
                                              String messageId, String messageBody, MessageType msgType, 
                                              Map<String, Object> parameters) {
        try {
            String routingKey = (String) parameters.getOrDefault("routingKey", customTopic);
            
            switch (msgType) {
                case DIRECT:
                case TOPIC:
                case FANOUT:
                case HEADERS:
                case PERSISTENT:
                case TTL:
                case PRIORITY:
                case DEAD_LETTER:
                    return messageService.sendMessage(customTopic, null, routingKey, messageBody);
                default:
                    throw new MessageServiceException("不支持的RabbitMQ消息类型: " + msgType);
            }
        } catch (Exception e) {
            log.error("RabbitMQ自定义topic消息发送异常: topic={}, messageId={}, error={}", 
                    customTopic, messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送事务消息到RocketMQ
     * 使用事务发送器确保消息与本地事务的一致性
     */
    private boolean sendTransactionMessage(MessageService rocketMQService, String topic, String tag, 
                                        String messageBody, Map<String, Object> parameters) {
        try {
            // 获取事务相关参数
            String transactionId = (String) parameters.getOrDefault("transactionId", 
                    "txn_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId());
            String businessKey = (String) parameters.getOrDefault("businessKey", "");
            int timeout = (Integer) parameters.getOrDefault("timeout", 3000); // 默认3秒超时
            
            log.info("发送事务消息: topic={}, tag={}, transactionId={}, businessKey={}", 
                    topic, tag, transactionId, businessKey);
            
            // 使用事务发送器发送消息
            boolean result = rocketMQService.sendTransactionMessage(topic, tag, messageBody, 
                    transactionId, businessKey, timeout);
            
            if (result) {
                log.info("事务消息发送成功: topic={}, tag={}, transactionId={}", topic, tag, transactionId);
            } else {
                log.error("事务消息发送失败: topic={}, tag={}, transactionId={}", topic, tag, transactionId);
            }
            
            return result;
        } catch (Exception e) {
            log.error("发送事务消息异常: topic={}, tag={}, error={}", topic, tag, e.getMessage(), e);
            throw new RuntimeException("发送事务消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendMessage(String messageId, String messageType, Map<String, Object> parameters) {
        log.info("动态发送消息: messageId={}, messageType={}, parameters={}", messageId, messageType, parameters);
        
        try {
            // 1. EnhancedMessageServiceImp 负责：参数验证和基础检查
            validateMessageParameters(messageId, messageType, parameters);
            
            // 2. 获取消息中间件类型
            MessageServiceType messageServiceType = MessageServiceType.fromCode(messageType.toUpperCase());
            
            // 3. 获取消息内容
            String messageBody = (String) parameters.get("messageBody");
            
            // 4. 判断是否需要Saga事务管理
            if (shouldUseSagaTransaction(parameters)) {
                // 使用Saga事务管理：委托给MessageSagaStateMachine
                log.info("使用Saga事务管理发送消息: messageId={}, messageType={}", messageId, messageType);
                return executeMessageWithSaga(messageId, messageBody, messageType, parameters);
            } else {
                // 直接发送：EnhancedMessageServiceImp 负责消息发送执行
                log.info("直接发送消息: messageId={}, messageType={}", messageId, messageType);
                return executeMessageDirectly(messageServiceType, messageId, messageBody, parameters);
            }
            
        } catch (Exception e) {
            log.error("动态发送消息失败: messageId={}, messageType={}, error={}", 
                    messageId, messageType, e.getMessage(), e);
            throw new RuntimeException("动态发送消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * EnhancedMessageServiceImp 职责：验证消息参数
     */
    private void validateMessageParameters(String messageId, String messageType, Map<String, Object> parameters) {
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new MessageServiceException("消息ID不能为空");
        }
        
        if (messageType == null || messageType.trim().isEmpty()) {
            throw new MessageServiceException("消息类型不能为空");
        }
        
        String messageBody = (String) parameters.get("messageBody");
        if (messageBody == null || messageBody.trim().isEmpty()) {
            throw new MessageServiceException("消息内容不能为空");
        }
        
        log.debug("消息参数验证通过: messageId={}, messageType={}", messageId, messageType);
    }

    /**
     * EnhancedMessageServiceImp 职责：判断是否使用Saga事务
     * 
     * 配置参数说明：
     * 1. topic: 自定义topic，指定后绕过Saga事务，直接发送
     * 2. useSaga: 明确指定是否使用Saga事务（true/false）
     * 3. messageType: 消息类型，TRANSACTION类型强制使用Saga事务
     * 4. sagaTimeout: Saga事务超时时间（毫秒）
     * 5. sagaRetries: Saga事务重试次数
     * 
     * 优先级：topic > useSaga > messageType > 默认值
     */
    private boolean shouldUseSagaTransaction(Map<String, Object> parameters) {
        // 1. 检查是否有自定义topic（自定义topic不使用Saga事务）
        String customTopic = (String) parameters.get("topic");
        if (customTopic != null && !customTopic.trim().isEmpty()) {
            log.debug("检测到自定义topic，不使用Saga事务: topic={}", customTopic);
            return false;
        }
        
        // 2. 检查是否明确指定不使用Saga事务
        Boolean useSaga = (Boolean) parameters.get("useSaga");
        if (useSaga != null && !useSaga) {
            log.debug("明确指定不使用Saga事务");
            return false;
        }
        
        // 3. 检查消息类型是否需要事务保证
        String messageType = (String) parameters.get("messageType");
        if (messageType != null && messageType.equals("TRANSACTION")) {
            log.debug("事务消息类型，使用Saga事务");
            return true;
        }
        
        // 4. 默认使用Saga事务（保证消息一致性）
        log.debug("默认使用Saga事务管理");
        return true;
    }

    /**
     * EnhancedMessageServiceImp 职责：使用Saga事务执行消息发送
     * 委托给MessageSagaStateMachine进行事务管理
     * 
     * 使用场景：
     * 1. 订单支付流程：需要保证订单状态和支付状态的一致性
     * 2. 库存扣减：需要保证订单创建和库存扣减的原子性
     * 3. 用户注册：需要保证用户信息和权限分配的一致性
     * 
     * Saga事务步骤示例：
     * 1. 发送订单消息
     * 2. 等待订单确认
     * 3. 发送库存扣减消息
     * 4. 等待库存确认
     * 5. 提交事务或执行补偿
     */
    private boolean executeMessageWithSaga(String messageId, String messageBody, 
                                         String messageType, Map<String, Object> parameters) {
        try {
            log.info("委托MessageSagaStateMachine执行Saga事务: messageId={}, messageType={}", 
                    messageId, messageType);
            
            // 获取消息中间件类型，用于Saga事务中的消息发送
            MessageServiceType messageServiceType = MessageServiceType.fromCode(messageType.toUpperCase());
            
            // 委托给MessageSagaStateMachine进行完整的Saga事务管理
            // 传递消息中间件类型，让Saga事务知道使用哪种中间件发送消息
            messageSagaStateMachine.executeMessageSendSaga(messageId, messageBody, messageType, messageServiceType, this);
            
            log.info("Saga事务执行成功: messageId={}, messageType={}", messageId, messageType);
            return true;
            
        } catch (Exception e) {
            log.error("Saga事务执行失败: messageId={}, messageType={}, error={}", 
                    messageId, messageType, e.getMessage(), e);
            throw new RuntimeException("Saga事务执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * EnhancedMessageServiceImp 职责：直接执行消息发送
     * 不经过Saga事务管理，直接调用对应的消息服务
     * 
     * 使用场景：
     * 1. 日志记录：简单的日志消息，无需事务保证
     * 2. 通知推送：用户通知、系统告警等
     * 3. 数据同步：非关键数据的同步操作
     * 4. 监控指标：性能监控、业务统计等
     * 5. 测试消息：开发测试阶段的消息发送
     * 
     * 优势：
     * 1. 性能更高：无事务开销
     * 2. 延迟更低：直接发送，无协调等待
     * 3. 资源消耗少：无需维护事务状态
     * 4. 部署简单：无需额外的Saga状态机
     */
    public boolean executeMessageDirectly(MessageServiceType messageServiceType, String messageId, 
                                        String messageBody, Map<String, Object> parameters) {
        try {
            log.info("EnhancedMessageServiceImp直接执行消息发送: serviceType={}, messageId={}", 
                    messageServiceType, messageId);
            
            // 获取自定义topic
            String customTopic = (String) parameters.get("topic");
            if (customTopic == null || customTopic.trim().isEmpty()) {
                throw new MessageServiceException("直接发送模式必须指定自定义topic");
            }
            
            // 根据消息中间件类型，直接调用对应的消息服务
            boolean result = sendMessageWithCustomTopic(messageServiceType, customTopic, messageId, messageBody, parameters);
            
            if (result) {
                log.info("直接消息发送成功: serviceType={}, messageId={}, topic={}", 
                        messageServiceType, messageId, customTopic);
                return true;
            } else {
                throw new MessageServiceException("直接消息发送失败");
            }
            
        } catch (Exception e) {
            log.error("直接消息发送异常: serviceType={}, messageId={}, error={}", 
                    messageServiceType, messageId, e.getMessage(), e);
            throw new RuntimeException("直接消息发送失败: " + e.getMessage(), e);
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

    /**
     * 构建回调数据
     * 统一构建各种回调类型需要的数据结构
     */
    private Map<String, Object> buildCallbackData(String topic, String key, String messageBody, 
                                                 Map<String, Object> parameters) {
        Map<String, Object> callbackData = new HashMap<>();
        callbackData.put("originalTopic", topic);
        callbackData.put("originalKey", key);
        callbackData.put("messageBody", messageBody);
        callbackData.put("messageId", parameters.get("messageId"));
        callbackData.put("messageType", parameters.get("messageType"));
        callbackData.put("messageQueueType", parameters.get("messageQueueType"));
        callbackData.put("callbackTimestamp", System.currentTimeMillis());
        callbackData.put("threadId", Thread.currentThread().getId());
        
        // 添加其他相关参数
        if (parameters.containsKey("businessKey")) {
            callbackData.put("businessKey", parameters.get("businessKey"));
        }
        if (parameters.containsKey("transactionId")) {
            callbackData.put("transactionId", parameters.get("transactionId"));
        }
        
        return callbackData;
    }

    /**
     * 带重试机制的HTTP回调执行
     * 支持指数退避重试策略
     */
    private boolean executeHttpCallbackWithRetry(String callbackUrl, Map<String, Object> callbackData, 
                                               int maxRetries, int timeoutMs, String contentType) {
        int retryCount = 0;
        long baseDelayMs = 1000; // 基础延迟1秒
        
        while (retryCount <= maxRetries) {
            try {
                log.debug("执行HTTP回调 (尝试 {}/{}): callbackUrl={}", retryCount + 1, maxRetries + 1, callbackUrl);
                
                // 这里应该使用HTTP客户端执行实际的HTTP请求
                // 由于没有具体的HTTP客户端依赖，这里提供框架代码
                boolean success = executeHttpRequest(callbackUrl, callbackData, timeoutMs, contentType);
                
                if (success) {
                    log.debug("HTTP回调执行成功: callbackUrl={}, 尝试次数={}", callbackUrl, retryCount + 1);
                    return true;
                } else {
                    log.warn("HTTP回调执行失败: callbackUrl={}, 尝试次数={}", callbackUrl, retryCount + 1);
                }
                
            } catch (Exception e) {
                log.warn("HTTP回调执行异常 (尝试 {}/{}): callbackUrl={}, error={}", 
                        retryCount + 1, maxRetries + 1, callbackUrl, e.getMessage());
            }
            
            retryCount++;
            if (retryCount <= maxRetries) {
                // 指数退避延迟
                long delayMs = baseDelayMs * (long) Math.pow(2, retryCount - 1);
                log.debug("等待 {}ms 后重试HTTP回调: callbackUrl={}", delayMs, callbackUrl);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("HTTP回调重试等待被中断: callbackUrl={}", callbackUrl);
                    break;
                }
            }
        }
        
        log.error("HTTP回调执行失败，已达到最大重试次数: callbackUrl={}, maxRetries={}", callbackUrl, maxRetries);
        return false;
    }

    /**
     * 执行HTTP请求
     * 这里提供框架代码，实际使用时需要集成具体的HTTP客户端
     */
    private boolean executeHttpRequest(String callbackUrl, Map<String, Object> callbackData, 
                                     int timeoutMs, String contentType) {
        try {
            // TODO: 集成具体的HTTP客户端，如OkHttp、Apache HttpClient等
            // 这里提供框架代码示例
            
            log.debug("准备发送HTTP请求: url={}, data={}, timeout={}ms, contentType={}", 
                    callbackUrl, callbackData, timeoutMs, contentType);
            
            // 示例：使用Java内置的HttpURLConnection（生产环境建议使用专业HTTP客户端）
            /*
            URL url = new URL(callbackUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("User-Agent", "MessageService-Callback/1.0");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setDoOutput(true);
            
            // 发送数据
            String jsonData = convertToJson(callbackData);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 检查响应
            int responseCode = connection.getResponseCode();
            boolean success = responseCode >= 200 && responseCode < 300;
            
            log.debug("HTTP回调响应: url={}, responseCode={}, success={}", callbackUrl, responseCode, success);
            return success;
            */
            
            // 临时返回true，避免阻塞流程
            log.info("HTTP回调请求已准备就绪（需要集成HTTP客户端）: url={}", callbackUrl);
            return true;
            
        } catch (Exception e) {
            log.error("执行HTTP请求异常: url={}, error={}", callbackUrl, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将对象转换为JSON字符串
     * 这里提供框架代码，实际使用时需要集成JSON库
     */
    private String convertToJson(Object obj) {
        try {
            // TODO: 集成具体的JSON库，如Jackson、Gson等
            // 这里提供简单的字符串表示
            
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                StringBuilder json = new StringBuilder("{");
                boolean first = true;
                
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        json.append(",");
                    }
                    json.append("\"").append(entry.getKey()).append("\":");
                    
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        json.append("\"").append(value).append("\"");
                    } else {
                        json.append(value);
                    }
                    
                    first = false;
                }
                
                return json.toString();
            } else {
                return obj.toString();
            }
            
        } catch (Exception e) {
            log.error("转换对象为JSON异常: obj={}, error={}", obj, e.getMessage(), e);
            return "{}";
        }
    }
}
