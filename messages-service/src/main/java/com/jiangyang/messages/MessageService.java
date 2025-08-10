package com.jiangyang.messages;

/**
 * 消息服务通用接口
 * 提供统一的消息发送和消费接口，屏蔽底层消息中间件的实现细节
 */
public interface MessageService {
    
    /**
     * 发送消息
     * @param topic 主题/队列名称
     * @param message 消息内容
     * @param <T> 消息类型
     * @return 是否发送成功
     */
    <T> boolean sendMessage(String topic, T message);
    
    /**
     * 发送延迟消息
     * @param topic 主题/队列名称
     * @param message 消息内容
     * @param delaySeconds 延迟秒数
     * @param <T> 消息类型
     * @return 是否发送成功
     */
    <T> boolean sendDelayMessage(String topic, T message, long delaySeconds);
    
    /**
     * 发送事务消息
     * @param topic 主题/队列名称
     * @param message 消息内容
     * @param transactionId 事务ID
     * @param <T> 消息类型
     * @return 是否发送成功
     */
    <T> boolean sendTransactionalMessage(String topic, T message, String transactionId);
    
    /**
     * 订阅消息
     * @param topic 主题/队列名称
     * @param consumerGroup 消费者组
     * @param listener 消息监听器
     * @param <T> 消息类型
     * @return 是否订阅成功
     */
    <T> boolean subscribe(String topic, String consumerGroup, MessageListener<T> listener);
    
    /**
     * 取消订阅
     * @param topic 主题/队列名称
     * @param consumerGroup 消费者组
     * @return 是否取消成功
     */
    boolean unsubscribe(String topic, String consumerGroup);
    
    /**
     * 获取服务类型
     * @return 服务类型
     */
    MessageServiceType getServiceType();
}
