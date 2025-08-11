package com.jiangyang.messages;

/**
 * 消息监听器接口
 * 用于处理接收到的消息
 */
public interface MessageListener<T> {
    
    /**
     * 处理消息
     * @param message 消息内容
     */
    void onMessage(T message);
    
    /**
     * 处理错误
     * @param error 错误信息
     */
    default void onError(Throwable error) {
        // 默认实现，可以由具体监听器覆盖
        error.printStackTrace();
    }
    
    /**
     * 获取消息类型
     * @return 消息类型
     */
    default String getMessageType() {
        return "default";
    }
}
