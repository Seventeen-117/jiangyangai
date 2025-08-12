package com.jiangyang.messages.service.impl;

import com.jiangyang.messages.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebSocket服务实现类
 * 这是一个简单的实现，实际项目中可以集成具体的WebSocket框架
 * 
 * @author jiangyang
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastMessage(String topic, Map<String, Object> message) {
        try {
            String destination = "/topic/" + topic;
            messagingTemplate.convertAndSend(destination, message);
            log.info("WebSocket广播消息发送成功: destination={}, message={}", destination, message);
        } catch (Exception e) {
            log.error("WebSocket广播消息发送失败: topic={}, error={}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void sendToUser(String userId, String topic, Map<String, Object> message) {
        try {
            String destination = "/topic/" + topic;
            messagingTemplate.convertAndSendToUser(userId, destination, message);
            log.info("WebSocket用户消息发送成功: userId={}, destination={}, message={}", userId, destination, message);
        } catch (Exception e) {
            log.error("WebSocket用户消息发送失败: userId={}, topic={}, error={}", userId, topic, e.getMessage(), e);
        }
    }

    @Override
    public void sendToSession(String sessionId, String topic, Map<String, Object> message) {
        try {
            // 通常按用户或广播推送，按sessionId需要自定义映射；这里退化为广播到topic
            broadcastMessage(topic, message);
            log.debug("WebSocket会话消息已广播代替: sessionId={}, topic={}", sessionId, topic);
        } catch (Exception e) {
            log.error("WebSocket会话消息发送失败: sessionId={}, topic={}, error={}", sessionId, topic, e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
