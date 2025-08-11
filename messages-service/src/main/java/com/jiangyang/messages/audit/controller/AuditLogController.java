package com.jiangyang.messages.audit.controller;

import com.jiangyang.messages.audit.entity.BusinessTraceLog;
import com.jiangyang.messages.audit.entity.MessageLifecycleLog;
import com.jiangyang.messages.audit.entity.TransactionAuditLog;
import com.jiangyang.messages.audit.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志REST控制器
 * 提供事务追溯和业务处理轨迹查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 根据全局事务ID查询完整的事务轨迹
     */
    @GetMapping("/transaction/{globalTransactionId}")
    public ResponseEntity<Map<String, Object>> getTransactionTrace(@PathVariable String globalTransactionId) {
        try {
            Map<String, Object> result = auditLogService.getTransactionTrace(globalTransactionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询事务轨迹失败: {}", globalTransactionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查询事务轨迹失败: " + e.getMessage()));
        }
    }

    /**
     * 根据业务事务ID查询完整的事务轨迹
     */
    @GetMapping("/business-transaction/{businessTransactionId}")
    public ResponseEntity<Map<String, Object>> getBusinessTransactionTrace(@PathVariable String businessTransactionId) {
        try {
            Map<String, Object> result = auditLogService.getBusinessTransactionTrace(businessTransactionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询业务事务轨迹失败: {}", businessTransactionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查询业务事务轨迹失败: " + e.getMessage()));
        }
    }

    /**
     * 根据消息ID查询完整的消息生命周期
     */
    @GetMapping("/message/{messageId}")
    public ResponseEntity<Map<String, Object>> getMessageLifecycleTrace(@PathVariable String messageId) {
        try {
            Map<String, Object> result = auditLogService.getMessageLifecycleTrace(messageId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询消息生命周期轨迹失败: {}", messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查询消息生命周期轨迹失败: " + e.getMessage()));
        }
    }

    /**
     * 根据调用链ID查询完整的业务调用轨迹
     */
    @GetMapping("/trace/{traceId}")
    public ResponseEntity<Map<String, Object>> getBusinessTraceChain(@PathVariable String traceId) {
        try {
            Map<String, Object> result = auditLogService.getBusinessTraceChain(traceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询业务调用轨迹失败: {}", traceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查询业务调用轨迹失败: " + e.getMessage()));
        }
    }

    /**
     * 查询指定时间范围内的事务统计
     */
    @GetMapping("/statistics/transaction")
    public ResponseEntity<Map<String, Object>> getTransactionStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Map<String, Object> result = auditLogService.getTransactionStatistics(startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询事务统计失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查询事务统计失败: " + e.getMessage()));
        }
    }

    /**
     * 查询指定时间范围内的消息统计
     */
    @GetMapping("/statistics/message")
    public ResponseEntity<Map<String, Object>> getMessageStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Map<String, Object> result = auditLogService.getMessageStatistics(startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询消息统计失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查询消息统计失败: " + e.getMessage()));
        }
    }

    /**
     * 查询指定时间范围内的调用统计
     */
    @GetMapping("/statistics/call")
    public ResponseEntity<Map<String, Object>> getCallStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Map<String, Object> result = auditLogService.getCallStatistics(startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询调用统计失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "查询调用统计失败: " + e.getMessage()));
        }
    }

    /**
     * 查询失败的事务列表
     */
    @GetMapping("/failed/transactions")
    public ResponseEntity<List<TransactionAuditLog>> getFailedTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<TransactionAuditLog> result = auditLogService.getFailedTransactions(startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询失败事务列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 查询死信消息列表
     */
    @GetMapping("/failed/messages")
    public ResponseEntity<List<MessageLifecycleLog>> getDeadLetterMessages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<MessageLifecycleLog> result = auditLogService.getDeadLetterMessages(startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询死信消息列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 查询失败的调用列表
     */
    @GetMapping("/failed/calls")
    public ResponseEntity<List<BusinessTraceLog>> getFailedCalls(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<BusinessTraceLog> result = auditLogService.getFailedCalls(startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询失败调用列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 清理过期的审计日志
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanExpiredAuditLogs(@RequestParam(defaultValue = "30") int retentionDays) {
        try {
            auditLogService.cleanExpiredAuditLogs(retentionDays);
            return ResponseEntity.ok(Map.of("message", "清理过期审计日志成功", "retentionDays", retentionDays));
        } catch (Exception e) {
            log.error("清理过期审计日志失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "清理过期审计日志失败: " + e.getMessage()));
        }
    }

    /**
     * 导出审计日志
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            byte[] data = auditLogService.exportAuditLogs(startTime, endTime, format);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            String filename = String.format("audit_logs_%s_%s.%s", 
                    startTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                    endTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                    format.toLowerCase());
            
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (Exception e) {
            log.error("导出审计日志失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "audit-log-service"
        ));
    }
}
