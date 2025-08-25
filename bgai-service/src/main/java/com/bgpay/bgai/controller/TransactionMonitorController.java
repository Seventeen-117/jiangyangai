package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.TransactionLog;
import com.bgpay.bgai.service.BgaiTransactionService;
import com.bgpay.bgai.service.BgaiTransactionUtil;
import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import io.seata.core.context.RootContext;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 事务监控控制器
 * 提供 REST API 来检查和诊断事务状态
 */
@Slf4j
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionMonitorController {

    private final BgaiTransactionService bgaiTransactionService;
    private final BgaiTransactionUtil transactionUtil;
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

    /**
     * 获取详细的事务信息
     */
    @GetMapping("/info")
    public Map<String, Object> getTransactionInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String xid = RootContext.getXID();
            boolean inTransaction = xid != null;
            
            result.put("inTransaction", inTransaction);
            result.put("xid", xid);
            result.put("threadId", Thread.currentThread().getId());
            result.put("threadName", Thread.currentThread().getName());
            result.put("timestamp", System.currentTimeMillis());
            
            // 使用 BgaiTransactionService 的方法
            result.put("bgaiServiceXid", bgaiTransactionService.getCurrentXid());
            result.put("bgaiServiceHasActiveTransaction", bgaiTransactionService.hasActiveTransaction());
            
            if (inTransaction) {
                result.put("message", "当前线程在全局事务中");
                result.put("status", "ACTIVE");
            } else {
                result.put("message", "当前线程不在全局事务中");
                result.put("status", "INACTIVE");
            }
            
            log.info("详细事务信息: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("获取详细事务信息失败", e);
        }
        
        return result;
    }

    /**
     * 测试事务创建
     */
    @GetMapping("/test")
    public Map<String, Object> testTransaction() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("beforeTransaction", "开始测试事务");
            result.put("beforeXid", RootContext.getXID());
            
            // 执行一个简单的事务测试
            bgaiTransactionService.executeWithTransaction("test-transaction", 30000, () -> {
                String testXid = RootContext.getXID();
                log.info("测试事务内部 - XID: {}", testXid);
                return "测试成功";
            });
            
            result.put("afterTransaction", "测试事务完成");
            result.put("afterXid", RootContext.getXID());
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("测试事务失败", e);
        }
        
        return result;
    }

    /**
     * 验证事务工具类
     */
    @GetMapping("/util-test")
    public Map<String, Object> testTransactionUtil() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("currentXid", transactionUtil.getCurrentXid());
            result.put("isInGlobalTransaction", transactionUtil.isInGlobalTransaction());
            result.put("threadInfo", Thread.currentThread().getName() + " (ID: " + Thread.currentThread().getId() + ")");
            result.put("timestamp", System.currentTimeMillis());
            
            // 打印事务状态
            transactionUtil.printTransactionStatus("工具类测试", "test-util");
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("测试事务工具类失败", e);
        }
        
        return result;
    }

    /**
     * 检查指定 XID 的事务日志
     */
    @GetMapping("/log/{xid}")
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
    @PostMapping("/log/create-test")
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
    @PutMapping("/log/update-status")
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
     * 测试 Seata 连接
     */
    @GetMapping("/seata/connection")
    public Map<String, Object> testSeataConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", System.currentTimeMillis());
            
            // 尝试创建全局事务对象
            try {
                GlobalTransaction transaction = GlobalTransactionContext.getCurrentOrCreate();
                result.put("seataAvailable", true);
                result.put("transactionObject", transaction != null ? "GlobalTransaction" : "null");
                result.put("message", "Seata 连接正常");
            } catch (Exception e) {
                result.put("seataAvailable", false);
                result.put("error", e.getMessage());
                result.put("message", "Seata 连接失败");
            }
            
            log.info("Seata 连接测试: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("测试 Seata 连接失败", e);
        }
        
        return result;
    }
}
