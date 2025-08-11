package com.bgpay.bgai.controller;

import com.bgpay.bgai.model.TransactionEvent;
import com.bgpay.bgai.model.TransactionEventResponse;
import com.bgpay.bgai.service.TransactionEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 事务事件控制器
 * 接收来自 messages-service 的事务状态事件
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction-events")
@Tag(name = "事务事件管理", description = "接收和处理事务状态事件")
public class TransactionEventController {

    @Autowired
    private TransactionEventService transactionEventService;

    /**
     * 接收事务状态事件
     */
    @PostMapping("/receive")
    @Operation(summary = "接收事务状态事件", description = "接收来自 messages-service 的事务状态事件")
    public ResponseEntity<TransactionEventResponse> receiveTransactionEvent(
            @RequestBody TransactionEvent event) {
        log.info("接收到事务状态事件: {}", event);
        
        try {
            TransactionEventResponse response = transactionEventService.processTransactionEvent(event);
            log.info("事务事件处理成功: transactionId={}, status={}", 
                    event.getTransactionId(), event.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("处理事务事件失败: transactionId={}, error={}", 
                    event.getTransactionId(), e.getMessage(), e);
            TransactionEventResponse errorResponse = new TransactionEventResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("处理事务事件失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 批量接收事务状态事件
     */
    @PostMapping("/batch-receive")
    @Operation(summary = "批量接收事务状态事件", description = "批量接收来自 messages-service 的事务状态事件")
    public ResponseEntity<TransactionEventResponse> receiveBatchTransactionEvents(
            @RequestBody List<TransactionEvent> events) {
        log.info("接收到批量事务状态事件，数量: {}", events.size());
        
        try {
            TransactionEventResponse response = transactionEventService.processBatchTransactionEvents(events);
            log.info("批量事务事件处理成功，数量: {}", events.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("处理批量事务事件失败，数量: {}, error={}", events.size(), e.getMessage(), e);
            TransactionEventResponse errorResponse = new TransactionEventResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("处理批量事务事件失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 查询事务事件历史
     */
    @GetMapping("/history/{transactionId}")
    @Operation(summary = "查询事务事件历史", description = "根据事务ID查询事件历史")
    public ResponseEntity<List<TransactionEvent>> getTransactionEventHistory(
            @PathVariable String transactionId) {
        log.info("查询事务事件历史: transactionId={}", transactionId);
        
        try {
            List<TransactionEvent> events = transactionEventService.getTransactionEventHistory(transactionId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("查询事务事件历史失败: transactionId={}, error={}", 
                    transactionId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查询指定时间范围内的事务事件
     */
    @GetMapping("/time-range")
    @Operation(summary = "查询时间范围内的事务事件", description = "根据时间范围查询事务事件")
    public ResponseEntity<List<TransactionEvent>> getTransactionEventsByTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String status) {
        log.info("查询时间范围内的事务事件: startTime={}, endTime={}, status={}", 
                startTime, endTime, status);
        
        try {
            List<TransactionEvent> events = transactionEventService.getTransactionEventsByTimeRange(
                    startTime, endTime, status);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("查询时间范围内的事务事件失败: startTime={}, endTime={}, error={}", 
                    startTime, endTime, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取事务事件统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取事务事件统计信息", description = "获取事务事件的统计信息")
    public ResponseEntity<Object> getTransactionEventStatistics(
            @RequestParam(required = false) String timeRange) {
        log.info("获取事务事件统计信息: timeRange={}", timeRange);
        
        try {
            Object statistics = transactionEventService.getTransactionEventStatistics(timeRange);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取事务事件统计信息失败: timeRange={}, error={}", 
                    timeRange, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
