package com.jiangyang.messages.controller;

import com.jiangyang.messages.rocketmq.TransactionMessageProducer;
import com.jiangyang.messages.transaction.OrderTransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 事务消息控制器
 * 提供事务消息发送和状态查询的REST接口
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction")
public class TransactionMessageController {

    @Autowired
    private TransactionMessageProducer transactionMessageProducer;

    @Autowired
    private OrderTransactionExecutor orderTransactionExecutor;

    /**
     * 发送订单事务消息
     */
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> sendOrderTransactionMessage(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.getOrDefault("topic", "order-transaction-events");
            String tag = (String) request.getOrDefault("tag", "order-commit");
            String businessKey = (String) request.getOrDefault("businessKey", "order_" + UUID.randomUUID().toString());
            String messageBody = (String) request.getOrDefault("messageBody", "{\"orderId\":\"" + businessKey + "\",\"amount\":100.00}");
            Integer timeout = (Integer) request.getOrDefault("timeout", 5000);

            // 生成事务ID
            String transactionId = transactionMessageProducer.generateTransactionId();

            log.info("发送订单事务消息: transactionId={}, topic={}, tag={}, businessKey={}", 
                    transactionId, topic, tag, businessKey);

            // 发送事务消息
            boolean success = transactionMessageProducer.sendTransactionMessage(topic, tag, messageBody, 
                    transactionId, businessKey, timeout);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("transactionId", transactionId);
            response.put("topic", topic);
            response.put("tag", tag);
            response.put("businessKey", businessKey);
            response.put("message", success ? "订单事务消息发送成功" : "订单事务消息发送失败");
            response.put("timestamp", System.currentTimeMillis());

            if (success) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("发送订单事务消息异常: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "发送订单事务消息异常: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 发送自定义事务消息
     */
    @PostMapping("/custom")
    public ResponseEntity<Map<String, Object>> sendCustomTransactionMessage(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.get("topic");
            String tag = (String) request.get("tag");
            String businessKey = (String) request.get("businessKey");
            String messageBody = (String) request.get("messageBody");
            Integer timeout = (Integer) request.getOrDefault("timeout", 3000);

            // 参数验证
            if (topic == null || topic.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("topic不能为空"));
            }
            if (businessKey == null || businessKey.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("businessKey不能为空"));
            }
            if (messageBody == null || messageBody.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("messageBody不能为空"));
            }

            // 生成事务ID
            String transactionId = transactionMessageProducer.generateTransactionId();

            log.info("发送自定义事务消息: transactionId={}, topic={}, tag={}, businessKey={}", 
                    transactionId, topic, tag, businessKey);

            // 发送事务消息
            boolean success = transactionMessageProducer.sendTransactionMessage(topic, tag, messageBody, 
                    transactionId, businessKey, timeout);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("transactionId", transactionId);
            response.put("topic", topic);
            response.put("tag", tag);
            response.put("businessKey", businessKey);
            response.put("message", success ? "自定义事务消息发送成功" : "自定义事务消息发送失败");
            response.put("timestamp", System.currentTimeMillis());

            if (success) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("发送自定义事务消息异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("发送自定义事务消息异常: " + e.getMessage()));
        }
    }

    /**
     * 查询事务状态
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<Map<String, Object>> getTransactionStatus(@PathVariable String transactionId) {
        try {
            log.info("查询事务状态: transactionId={}", transactionId);

            // 查询事务状态
            TransactionMessageProducer.TransactionStatus status = transactionMessageProducer.getTransactionStatus(transactionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", transactionId);
            response.put("status", status.name());
            response.put("statusDescription", getStatusDescription(status));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询事务状态异常: transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("查询事务状态异常: " + e.getMessage()));
        }
    }

    /**
     * 查询订单事务状态
     */
    @GetMapping("/order/status/{transactionId}")
    public ResponseEntity<Map<String, Object>> getOrderTransactionStatus(@PathVariable String transactionId) {
        try {
            log.info("查询订单事务状态: transactionId={}", transactionId);

            // 查询订单事务状态
            OrderTransactionExecutor.OrderTransactionStatus status = orderTransactionExecutor.getOrderTransactionStatus(transactionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", transactionId);
            response.put("status", status != null ? status.name() : "UNKNOWN");
            response.put("statusDescription", getOrderStatusDescription(status));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询订单事务状态异常: transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("查询订单事务状态异常: " + e.getMessage()));
        }
    }

    /**
     * 清理事务状态
     */
    @DeleteMapping("/status/{transactionId}")
    public ResponseEntity<Map<String, Object>> clearTransactionStatus(@PathVariable String transactionId) {
        try {
            log.info("清理事务状态: transactionId={}", transactionId);

            // 清理事务状态
            transactionMessageProducer.clearTransactionStatus(transactionId);
            orderTransactionExecutor.clearOrderTransactionStatus(transactionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", transactionId);
            response.put("message", "事务状态清理成功");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("清理事务状态异常: transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("清理事务状态异常: " + e.getMessage()));
        }
    }

    /**
     * 获取事务消息发送器状态
     */
    @GetMapping("/producer/status")
    public ResponseEntity<Map<String, Object>> getProducerStatus() {
        try {
            boolean isRunning = transactionMessageProducer.isRunning();
            String stats = transactionMessageProducer.getProducerStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isRunning", isRunning);
            response.put("stats", stats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取事务消息发送器状态异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取事务消息发送器状态异常: " + e.getMessage()));
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "TransactionMessageController");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "事务消息服务运行正常");
        
        return ResponseEntity.ok(health);
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

    /**
     * 获取事务状态描述
     */
    private String getStatusDescription(TransactionMessageProducer.TransactionStatus status) {
        if (status == null) return "未知状态";
        
        switch (status) {
            case COMMITTED:
                return "已提交";
            case ROLLBACK:
                return "已回滚";
            case FAILED:
                return "失败";
            case UNKNOWN:
            default:
                return "未知状态";
        }
    }

    /**
     * 获取订单事务状态描述
     */
    private String getOrderStatusDescription(OrderTransactionExecutor.OrderTransactionStatus status) {
        if (status == null) return "未知状态";
        
        switch (status) {
            case PROCESSING:
                return "处理中";
            case COMMITTED:
                return "已提交";
            case FAILED:
                return "失败";
            default:
                return "未知状态";
        }
    }
}
