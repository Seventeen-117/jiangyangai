package com.jiangyang.messages.service;

import java.util.Map;

/**
 * WebSocket服务接口
 * 用于发送实时通知到前端客户端
 * 
 * @author jiangyang
 * @version 1.0.0
 */
public interface WebSocketService {
    
    /**
     * 广播消息到所有连接的客户端
     * 
     * @param topic 主题
     * @param message 消息内容
     */
    void broadcastMessage(String topic, Map<String, Object> message);
    
    /**
     * 发送消息到特定用户
     * 
     * @param userId 用户ID
     * @param topic 主题
     * @param message 消息内容
     */
    void sendToUser(String userId, String topic, Map<String, Object> message);
    
    /**
     * 发送消息到特定会话
     * 
     * @param sessionId 会话ID
     * @param topic 主题
     * @param message 消息内容
     */
    void sendToSession(String sessionId, String topic, Map<String, Object> message);
    
    /**
     * 检查WebSocket服务是否可用
     * 
     * @return true if available, false otherwise
     */
    boolean isAvailable();
}
