package com.bgpay.bgai.service.mq;

/**
 * 消息处理器接口
 * 用于处理来自messages-service转发的消息
 */
public interface MessageHandler {
    
    /**
     * 处理消息
     * @param message 消息内容
     * @param topic 主题
     * @param tag 标签
     * @return 处理结果
     */
    boolean handleMessage(String message, String topic, String tag);
    
    /**
     * 获取支持的主题
     * @return 主题名称
     */
    String getSupportedTopic();
    
    /**
     * 获取支持的标签
     * @return 标签名称
     */
    String getSupportedTag();
}
