package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.TransactionLog;
import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 事务日志监控控制器
 * 提供 REST API 来检查和诊断事务日志问题
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction-log")
@RequiredArgsConstructor
public class TransactionLogMonitorController {

    private final TransactionLogService transactionLogService;
    private final TransactionCoordinator transactionCoordinator;

    /**
     * 获取当前事务状态
     */
    @GetMapping("/status")
    public Map<String, Object> getTransactionStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String xid = RootContext.getXID();
            boolean inTransaction = xid != null;
            
            result.put("inTransaction", inTransaction);
            result.put("xid", xid);
            result.put("threadId", Thread.currentThread().getId());
            result.put("threadName", Thread.currentThread().getName());
            result.put("timestamp", System.currentTimeMillis());
            
            if (inTransaction) {
                result.put("message", "当前线程在全局事务中");
            } else {
                result.put("message", "当前线程不在全局事务中");
            }
            
            log.info("事务状态检查: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("获取事务状态失败", e);
        }
        
        return result;
    }

    /**
     * 检查指定 XID 的事务日志
     */
    @GetMapping("/check/{xid}")
    public Map<String, Object> checkTransactionLog(@PathVariable String xid) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("xid", xid);
            result.put("timestamp", System.currentTimeMillis());
            
            // 查询事务日志
            TransactionLog txLog = transactionLogService.findByXid(xid);
            if (txLog != null) {
                result.put("found", true);
                result.put("transactionLog", Map.of(
                    "id", txLog.getId(),
                    "status", txLog.getStatus(),
                    "transactionName", txLog.getTransactionName(),
                    "userId", txLog.getUserId(),
                    "startTime", txLog.getStartTime(),
                    "endTime", txLog.getEndTime(),
                    "createTime", txLog.getCreateTime(),
                    "updateTime", txLog.getUpdateTime()
                ));
                result.put("message", "找到事务日志");
            } else {
                result.put("found", false);
                result.put("message", "未找到事务日志");
            }
            
            log.info("事务日志检查: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("检查事务日志失败, xid: {}", xid, e);
        }
        
        return result;
    }

    /**
     * 检查用户的事务协调器状态
     */
    @GetMapping("/coordinator/{userId}")
    public Map<String, Object> checkTransactionCoordinator(@PathVariable String userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("userId", userId);
            result.put("timestamp", System.currentTimeMillis());
            
            // 检查事务协调器状态
            boolean hasActiveTransaction = transactionCoordinator.hasActiveTransaction(userId);
            String currentCompletionId = transactionCoordinator.getCurrentCompletionId(userId);
            
            result.put("hasActiveTransaction", hasActiveTransaction);
            result.put("currentCompletionId", currentCompletionId);
            
            if (hasActiveTransaction) {
                result.put("message", "用户有活跃事务");
            } else {
                result.put("message", "用户无活跃事务");
            }
            
            log.info("事务协调器检查: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("检查事务协调器失败, userId: {}", userId, e);
        }
        
        return result;
    }

    /**
     * 创建测试事务日志
     */
    @PostMapping("/create-test")
    public Map<String, Object> createTestTransactionLog(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String xid = request.get("xid");
            String userId = request.get("userId");
            
            if (xid == null || userId == null) {
                result.put("error", "xid 和 userId 不能为空");
                return result;
            }
            
            result.put("xid", xid);
            result.put("userId", userId);
            result.put("timestamp", System.currentTimeMillis());
            
            // 创建测试事务日志
            Long logId = transactionLogService.recordTransactionBegin(
                xid,
                "test-transaction",
                "TEST",
                "/api/test",
                "127.0.0.1",
                userId
            );
            
            if (logId != null) {
                result.put("success", true);
                result.put("logId", logId);
                result.put("message", "成功创建测试事务日志");
                
                // 验证创建的事务日志
                TransactionLog txLog = transactionLogService.findByXid(xid);
                if (txLog != null) {
                    result.put("verified", true);
                    result.put("status", txLog.getStatus());
                } else {
                    result.put("verified", false);
                    result.put("message", "创建成功但验证失败");
                }
            } else {
                result.put("success", false);
                result.put("message", "创建测试事务日志失败");
            }
            
            log.info("创建测试事务日志: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("创建测试事务日志失败", e);
        }
        
        return result;
    }

    /**
     * 更新事务日志状态
     */
    @PutMapping("/update-status")
    public Map<String, Object> updateTransactionStatus(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String xid = request.get("xid");
            String status = request.get("status");
            
            if (xid == null || status == null) {
                result.put("error", "xid 和 status 不能为空");
                return result;
            }
            
            result.put("xid", xid);
            result.put("status", status);
            result.put("timestamp", System.currentTimeMillis());
            
            // 更新事务状态
            transactionLogService.updateTransactionStatus(xid, status, null);
            
            result.put("success", true);
            result.put("message", "成功更新事务状态");
            
            // 验证更新
            TransactionLog txLog = transactionLogService.findByXid(xid);
            if (txLog != null) {
                result.put("verified", true);
                result.put("currentStatus", txLog.getStatus());
            } else {
                result.put("verified", false);
                result.put("message", "更新成功但验证失败");
            }
            
            log.info("更新事务状态: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("更新事务状态失败", e);
        }
        
        return result;
    }

    /**
     * 获取事务诊断信息
     */
    @GetMapping("/diagnostics")
    public Map<String, Object> getTransactionDiagnostics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String xid = RootContext.getXID();
            
            result.put("currentXid", xid);
            result.put("threadId", Thread.currentThread().getId());
            result.put("threadName", Thread.currentThread().getName());
            result.put("timestamp", System.currentTimeMillis());
            
            if (xid != null) {
                // 检查事务日志
                TransactionLog txLog = transactionLogService.findByXid(xid);
                result.put("transactionLogExists", txLog != null);
                
                if (txLog != null) {
                    result.put("transactionLogStatus", txLog.getStatus());
                    result.put("transactionLogId", txLog.getId());
                }
            }
            
            log.info("事务诊断信息: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("获取事务诊断信息失败", e);
        }
        
        return result;
    }
}
