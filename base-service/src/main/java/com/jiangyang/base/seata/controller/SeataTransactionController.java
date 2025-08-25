package com.jiangyang.base.seata.controller;

import com.jiangyang.base.seata.util.SeataTransactionUtil;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Seata 事务监控控制器
 * 提供 REST API 来检查和诊断事务状态
 */
@Slf4j
@RestController
@RequestMapping("/api/seata")
@RequiredArgsConstructor
public class SeataTransactionController {

    private final SeataTransactionUtil seataTransactionUtil;

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
     * 打印事务诊断信息
     */
    @GetMapping("/diagnostics")
    public Map<String, Object> getTransactionDiagnostics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            seataTransactionUtil.printTransactionDiagnostics();
            result.put("message", "诊断信息已打印到日志");
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("获取事务诊断信息失败", e);
        }
        
        return result;
    }

    /**
     * 验证 Seata 连接状态
     */
    @GetMapping("/connection")
    public Map<String, Object> validateConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            seataTransactionUtil.validateSeataConnection();
            result.put("message", "连接验证完成，请查看日志");
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("验证 Seata 连接失败", e);
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
            
            if (inTransaction) {
                // 尝试获取更多事务信息
                try {
                    seataTransactionUtil.checkGlobalTransactionStatus();
                    result.put("statusCheck", "已执行状态检查，请查看日志");
                } catch (Exception e) {
                    result.put("statusCheckError", e.getMessage());
                }
            }
            
            log.info("详细事务信息: {}", result);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("获取详细事务信息失败", e);
        }
        
        return result;
    }
}
