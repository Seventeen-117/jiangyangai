package com.jiangyang.messages.controller;

import com.jiangyang.messages.service.MessageSagaService;
import com.jiangyang.messages.service.EnhancedMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 消息Saga控制器
 * 提供基于Seata的分布式事务消息处理API
 */
@Slf4j
@RestController
@RequestMapping("/api/messages/saga")
public class MessageSagaController {

    @Autowired
    private MessageSagaService messageSagaService;

    @Autowired
    private EnhancedMessageService enhancedMessageService;

    /**
     * 发送消息（使用Saga事务）
     * 支持动态路由到不同的消息中间件
     * 
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessageWithSaga(@RequestBody Map<String, Object> request) {
        String messageId = (String) request.getOrDefault("messageId", UUID.randomUUID().toString());
        String messageQueueType = (String) request.getOrDefault("messageQueueType", "ROCKETMQ"); // 消息中间件类型
        String messageType = (String) request.getOrDefault("messageType", "NORMAL"); // 具体的消息类型
        
        if (messageId == null || messageId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息ID不能为空"));
        }
        
        try {
            // 根据消息中间件类型动态路由
            boolean success = enhancedMessageService.sendMessage(messageId, messageQueueType, request);
            
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("消息发送成功", Map.of(
                    "messageId", messageId,
                    "messageQueueType", messageQueueType,
                    "messageType", messageType,
                    "status", "SENT"
                )));
            } else {
                return ResponseEntity.internalServerError().body(createErrorResponse("消息发送失败"));
            }
        } catch (Exception e) {
            log.error("发送消息失败: messageId={}, messageQueueType={}, messageType={}, error={}", 
                    messageId, messageQueueType, messageType, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("消息发送失败: " + e.getMessage()));
        }
    }

    /**
     * 消费消息（使用Saga事务）
     * 
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/consume")
    public ResponseEntity<Map<String, Object>> consumeMessageWithSaga(@RequestBody Map<String, String> request) {
        String messageId = request.getOrDefault("messageId", UUID.randomUUID().toString());
        String content = request.get("content");
        
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息内容不能为空"));
        }
        
        try {
            messageSagaService.consumeMessageWithSaga(messageId, content);
            return ResponseEntity.ok(createSuccessResponse("消息消费成功", messageId));
        } catch (Exception e) {
            log.error("消费消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("消息消费失败: " + e.getMessage()));
        }
    }

    /**
     * 批量处理消息（使用Saga事务）
     * 
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> processBatchMessagesWithSaga(@RequestBody Map<String, Object> request) {
        String batchId = (String) request.getOrDefault("batchId", UUID.randomUUID().toString());
        Object messageIdsObj = request.get("messageIds");
        
        if (!(messageIdsObj instanceof String[])) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息ID数组格式错误"));
        }
        
        String[] messageIds = (String[]) messageIdsObj;
        if (messageIds.length == 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息ID数组不能为空"));
        }
        
        try {
            messageSagaService.processBatchMessagesWithSaga(batchId, messageIds);
            return ResponseEntity.ok(createSuccessResponse("批量消息处理成功", batchId));
        } catch (Exception e) {
            log.error("批量处理消息失败: batchId={}, error={}", batchId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("批量消息处理失败: " + e.getMessage()));
        }
    }

    /**
     * 处理事务消息（使用Saga事务）
     * 
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/transaction")
    public ResponseEntity<Map<String, Object>> processTransactionMessageWithSaga(@RequestBody Map<String, String> request) {
        String transactionId = request.getOrDefault("transactionId", UUID.randomUUID().toString());
        String messageId = request.getOrDefault("messageId", UUID.randomUUID().toString());
        String content = request.get("content");
        
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息内容不能为空"));
        }
        
        try {
            messageSagaService.processTransactionMessageWithSaga(transactionId, messageId, content);
            return ResponseEntity.ok(createSuccessResponse("事务消息处理成功", Map.of(
                "transactionId", transactionId,
                "messageId", messageId
            )));
        } catch (Exception e) {
            log.error("处理事务消息失败: transactionId={}, messageId={}, error={}", 
                    transactionId, messageId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("事务消息处理失败: " + e.getMessage()));
        }
    }

    /**
     * 同步发送消息
     * 
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/send/sync")
    public ResponseEntity<Map<String, Object>> sendMessageSync(@RequestBody Map<String, String> request) {
        String messageId = request.getOrDefault("messageId", UUID.randomUUID().toString());
        String content = request.get("content");
        
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息内容不能为空"));
        }
        
        boolean success = messageSagaService.sendMessageSync(messageId, content);
        
        if (success) {
            return ResponseEntity.ok(createSuccessResponse("同步消息发送成功", messageId));
        } else {
            return ResponseEntity.internalServerError().body(createErrorResponse("同步消息发送失败"));
        }
    }

    /**
     * 同步消费消息
     * 
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/consume/sync")
    public ResponseEntity<Map<String, Object>> consumeMessageSync(@RequestBody Map<String, String> request) {
        String messageId = request.getOrDefault("messageId", UUID.randomUUID().toString());
        String content = request.get("content");
        
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息内容不能为空"));
        }
        
        boolean success = messageSagaService.consumeMessageSync(messageId, content);
        
        if (success) {
            return ResponseEntity.ok(createSuccessResponse("同步消息消费成功", messageId));
        } else {
            return ResponseEntity.internalServerError().body(createErrorResponse("同步消息消费失败"));
        }
    }

    /**
     * 发送消息到指定消息中间件（增强版本）
     * 支持详细的参数配置和消息类型
     * 
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/send/enhanced")
    public ResponseEntity<Map<String, Object>> sendEnhancedMessage(@RequestBody Map<String, Object> request) {
        String messageId = (String) request.getOrDefault("messageId", UUID.randomUUID().toString());
        String messageQueueType = (String) request.get("messageQueueType"); // 消息中间件类型
        String messageType = (String) request.getOrDefault("messageType", "NORMAL"); // 消息类型（普通、定时、顺序、事务）
        
        if (messageId == null || messageId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息ID不能为空"));
        }
        
        if (messageQueueType == null || messageQueueType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息中间件类型不能为空"));
        }
        
        try {
            // 根据消息中间件类型动态路由
            boolean success = enhancedMessageService.sendMessage(messageId, messageQueueType, request);
            
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("增强消息发送成功", Map.of(
                    "messageId", messageId,
                    "messageQueueType", messageQueueType,
                    "messageType", messageType,
                    "status", "SENT",
                    "details", "消息已成功路由到" + messageQueueType + "消息中间件，消息类型：" + messageType
                )));
            } else {
                return ResponseEntity.internalServerError().body(createErrorResponse("增强消息发送失败"));
            }
        } catch (Exception e) {
            log.error("增强消息发送失败: messageId={}, messageQueueType={}, messageType={}, error={}", 
                    messageId, messageQueueType, messageType, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("增强消息发送失败: " + e.getMessage()));
        }
    }

    /**
     * 发送消息到指定消息中间件（兼容版本）
     * 
     * @param request 请求参数
     * @return 响应结果
     */
    @PostMapping("/send/to")
    public ResponseEntity<Map<String, Object>> sendMessageToSpecificMQ(@RequestBody Map<String, String> request) {
        String messageId = request.getOrDefault("messageId", UUID.randomUUID().toString());
        String content = request.get("content");
        String messageType = request.get("messageType");
        String topic = request.getOrDefault("topic", "default-topic");
        
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息内容不能为空"));
        }
        
        if (messageType == null || messageType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("消息类型不能为空"));
        }
        
        try {
            messageSagaService.sendMessageWithSaga(messageId, content, messageType.toUpperCase());
            return ResponseEntity.ok(createSuccessResponse("消息发送成功", Map.of(
                "messageId", messageId,
                "messageType", messageType.toUpperCase(),
                "topic", topic
            )));
        } catch (Exception e) {
            log.error("发送消息失败: messageId={}, messageType={}, topic={}, error={}", 
                    messageId, messageType, topic, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("消息发送失败: " + e.getMessage()));
        }
    }

    /**
     * 健康检查
     * 
     * @return 响应结果
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Message Saga Service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
