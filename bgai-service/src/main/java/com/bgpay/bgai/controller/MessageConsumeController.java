package com.bgpay.bgai.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.bgpay.bgai.service.mq.MessageHandler;
import com.bgpay.bgai.service.mq.impl.UserActionMessageHandler;

import java.util.Map;
import java.util.HashMap;

/**
 * 消息消费控制器
 * 用于接收来自messages-service转发的消息
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageConsumeController {

    @Autowired
    private UserActionMessageHandler userActionMessageHandler;

    /**
     * 接收转发的消息
     * 这是messages-service自动转发的消息入口
     */
    @PostMapping("/consume")
    public ResponseEntity<Map<String, Object>> consumeMessage(@RequestBody Map<String, Object> message) {
        try {
            log.info("收到来自messages-service的转发消息: {}", message);
            
            // 提取消息信息
            String messageId = (String) message.get("messageId");
            String topic = (String) message.get("topic");
            String messageBody = (String) message.get("messageBody");
            String consumerService = (String) message.get("consumerService");
            String messageQueueType = (String) message.get("messageQueueType");
            
            // 验证消息来源
            if (!"bgai-service".equals(consumerService)) {
                log.warn("消息不是发给本服务的，跳过处理: consumerService={}", consumerService);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "消息不是发给本服务的",
                    "consumerService", consumerService
                ));
            }
            
            // 根据主题和标签选择对应的消息处理器
            boolean processResult = false;
            String tag = extractTagFromMessage(message);
            
            if (userActionMessageHandler.getSupportedTopic().equals(topic) && 
                userActionMessageHandler.getSupportedTag().equals(tag)) {
                // 处理用户行为消息
                processResult = userActionMessageHandler.handleMessage(messageBody, topic, tag);
            } else {
                log.warn("没有找到合适的消息处理器: topic={}, tag={}", topic, tag);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "没有找到合适的消息处理器",
                    "topic", topic,
                    "tag", tag
                ));
            }
            
            // 返回处理结果
            Map<String, Object> response = new HashMap<>();
            response.put("success", processResult);
            response.put("messageId", messageId);
            response.put("topic", topic);
            response.put("tag", tag);
            response.put("processResult", processResult);
            response.put("timestamp", System.currentTimeMillis());
            
            if (processResult) {
                log.info("消息处理成功: messageId={}, topic={}, tag={}", messageId, topic, tag);
                response.put("message", "消息处理成功");
            } else {
                log.error("消息处理失败: messageId={}, topic={}, tag={}", messageId, topic, tag);
                response.put("message", "消息处理失败");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("处理转发消息时发生异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "消息处理异常: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "bgai-service");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "消息消费服务运行正常");
        
        return ResponseEntity.ok(health);
    }

    /**
     * 从消息中提取标签
     */
    private String extractTagFromMessage(Map<String, Object> message) {
        // 尝试从不同位置提取标签
        String tag = (String) message.get("tag");
        if (tag != null && !tag.trim().isEmpty()) {
            return tag;
        }
        
        // 从消息头中提取
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) message.get("headers");
        if (headers != null) {
            tag = headers.get("tag");
            if (tag != null && !tag.trim().isEmpty()) {
                return tag;
            }
        }
        
        // 从消息体中提取（假设是JSON格式）
        String messageBody = (String) message.get("messageBody");
        if (messageBody != null && messageBody.contains("\"tag\"")) {
            // 简单的JSON解析，实际项目中建议使用JSON库
            int tagIndex = messageBody.indexOf("\"tag\"");
            if (tagIndex > 0) {
                int startIndex = messageBody.indexOf("\"", tagIndex + 6);
                if (startIndex > 0) {
                    int endIndex = messageBody.indexOf("\"", startIndex + 1);
                    if (endIndex > startIndex) {
                        return messageBody.substring(startIndex + 1, endIndex);
                    }
                }
            }
        }
        
        // 默认标签
        return "default";
    }
}
