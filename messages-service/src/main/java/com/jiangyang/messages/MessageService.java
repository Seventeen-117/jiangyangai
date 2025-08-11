package com.jiangyang.messages;

/**
 * 消息服务接口
 * 定义消息服务的基本操作方法
 */
public interface MessageService {

    /**
     * 发送消息
     * @param topic 主题
     * @param content 消息内容
     * @return 是否发送成功
     */
    boolean sendMessage(String topic, String content);

    /**
     * 发送消息到指定主题和标签
     * @param topic 主题
     * @param tag 标签
     * @param content 消息内容
     * @return 是否发送成功
     */
    boolean sendMessage(String topic, String tag, String content);

    /**
     * 发送消息到指定主题、标签和键
     * @param topic 主题
     * @param tag 标签
     * @param key 消息键
     * @param content 消息内容
     * @return 是否发送成功
     */
    boolean sendMessage(String topic, String tag, String key, String content);

    /**
     * 发送延迟消息
     * @param topic 主题
     * @param content 消息内容
     * @param delayLevel 延迟级别
     * @return 是否发送成功
     */
    boolean sendDelayMessage(String topic, String content, int delayLevel);

    /**
     * 发送顺序消息
     * @param topic 主题
     * @param content 消息内容
     * @param hashKey 哈希键
     * @return 是否发送成功
     */
    boolean sendOrderedMessage(String topic, String content, String hashKey);

    /**
     * 批量发送消息
     * @param topic 主题
     * @param messages 消息列表
     * @return 是否发送成功
     */
    boolean sendBatchMessages(String topic, java.util.List<String> messages);

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
