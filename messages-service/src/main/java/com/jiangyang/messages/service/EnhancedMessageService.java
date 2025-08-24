package com.jiangyang.messages.service;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.consume.MessageServiceType;
import com.jiangyang.messages.consume.MessageType;

import java.util.Map;

/**
 * 增强消息服务接口
 * 支持不同消息中间件的特定参数配置
 */
@DataSource("master")
public interface EnhancedMessageService {

    /**
     * 发送消息到RocketMQ
     * 
     * @param messageId 消息ID
     * @param topic 主题
     * @param tag 标签
     * @param messageType 消息类型（普通消息、定时消息、顺序消息、事务消息）
     * @param messageBody 消息体
     * @param parameters 额外参数（如延迟级别、哈希键等）
     * @return 是否发送成功
     */
    boolean sendToRocketMQ(String messageId, String topic, String tag, MessageType messageType, String messageBody, Map<String, Object> parameters);

    /**
     * 发送消息到Kafka
     * 
     * @param messageId 消息ID
     * @param topic 主题
     * @param key 分区键（可选，用于分区路由）
     * @param messageType 消息类型（普通消息、定时消息、顺序消息、事务消息）
     * @param messageBody 消息体
     * @param parameters 额外参数
     * @return 是否发送成功
     */
    boolean sendToKafka(String messageId, String topic, String key, MessageType messageType, String messageBody, Map<String, Object> parameters);

    /**
     * 发送消息到RabbitMQ
     * 
     * @param messageId 消息ID
     * @param queueName 队列名
     * @param durable 是否持久化
     * @param exclusive 是否排他
     * @param autoDelete 是否自动删除
     * @param exchange 交换机（空表示默认交换机）
     * @param routingKey 路由键
     * @param messageType 消息类型（普通消息、定时消息、顺序消息、事务消息）
     * @param otherProperties 其他属性
     * @param messageBody 消息体
     * @param parameters 额外参数
     * @return 是否发送成功
     */
    boolean sendToRabbitMQ(String messageId, String queueName, boolean durable, boolean exclusive, 
                          boolean autoDelete, String exchange, String routingKey, 
                          MessageType messageType, Map<String, Object> otherProperties, 
                          String messageBody, Map<String, Object> parameters);

    /**
     * 根据消息类型动态发送消息
     * 
     * @param messageId 消息ID
     * @param messageType 消息类型 (ROCKETMQ, KAFKA, RABBITMQ)
     * @param parameters 消息参数（根据类型包含不同的参数）
     * @return 是否发送成功
     */
    boolean sendMessage(String messageId, String messageType, Map<String, Object> parameters);

    /**
     * 获取消息服务类型
     * @return 消息服务类型
     */
    MessageServiceType getServiceType();

    /**
     * 检查服务是否可用
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 关闭服务
     */
    void shutdown();
}
