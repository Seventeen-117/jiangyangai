package com.bgpay.bgai.service;

import io.seata.core.context.RootContext;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * BgaiService 事务工具类
 * 提供事务状态检查和 XID 管理功能
 */
@Slf4j
@Component
public class BgaiTransactionUtil {

    /**
     * 获取当前线程的 XID
     */
    public String getCurrentXid() {
        return RootContext.getXID();
    }

    /**
     * 检查当前线程是否在全局事务中
     */
    public boolean isInGlobalTransaction() {
        return getCurrentXid() != null;
    }

    /**
     * 从全局事务对象获取 XID，如果为空则从当前线程上下文获取
     */
    public String getXidFromTransaction(GlobalTransaction transaction) {
        String xid = null;
        
        if (transaction != null) {
            xid = transaction.getXid();
        }
        
        if (xid == null) {
            xid = RootContext.getXID();
        }
        
        return xid;
    }

    /**
     * 打印事务状态信息
     */
    public void printTransactionStatus(String operation, String transactionName) {
        String xid = getCurrentXid();
        log.info("BgaiService {} - 事务名称: {}, XID: {}, 线程: {}", 
                operation, transactionName, xid, Thread.currentThread().getName());
    }

    /**
     * 验证事务状态
     */
    public boolean validateTransactionStatus(String operation, String transactionName) {
        String xid = getCurrentXid();
        boolean isValid = xid != null;
        
        if (isValid) {
            log.debug("BgaiService {} - 事务状态正常: XID={}, name={}", operation, xid, transactionName);
        } else {
            log.warn("BgaiService {} - 事务状态异常: XID为空, name={}", operation, transactionName);
        }
        
        return isValid;
    }

    /**
     * 创建并开始事务，返回有效的 XID
     */
    public String createAndBeginTransaction(String transactionName, int timeout) {
        try {
            GlobalTransaction transaction = GlobalTransactionContext.getCurrentOrCreate();
            transaction.begin(timeout, transactionName);
            
            String xid = getXidFromTransaction(transaction);
            log.info("BgaiService 创建并开始事务成功: XID={}, name={}, timeout={}", xid, transactionName, timeout);
            
            return xid;
        } catch (Exception e) {
            log.error("BgaiService 创建并开始事务失败: name={}, timeout={}", transactionName, timeout, e);
            throw new RuntimeException("创建并开始事务失败", e);
        }
    }

    /**
     * 提交事务
     */
    public void commitTransaction(String xid, String transactionName) {
        if (xid != null) {
            try {
                GlobalTransaction transaction = GlobalTransactionContext.reload(xid);
                transaction.commit();
                log.info("BgaiService 事务提交成功: XID={}, name={}", xid, transactionName);
            } catch (Exception e) {
                log.error("BgaiService 事务提交失败: XID={}, name={}", xid, transactionName, e);
                throw new RuntimeException("事务提交失败", e);
            }
        } else {
            log.warn("BgaiService 事务提交时XID为空，跳过提交: name={}", transactionName);
        }
    }

    /**
     * 回滚事务
     */
    public void rollbackTransaction(String xid, String transactionName) {
        if (xid != null) {
            try {
                GlobalTransaction transaction = GlobalTransactionContext.reload(xid);
                transaction.rollback();
                log.error("BgaiService 事务回滚成功: XID={}, name={}", xid, transactionName);
            } catch (Exception e) {
                log.error("BgaiService 事务回滚失败: XID={}, name={}", xid, transactionName, e);
                throw new RuntimeException("事务回滚失败", e);
            }
        } else {
            log.error("BgaiService 事务回滚时XID为空，无法回滚: name={}", transactionName);
        }
    }

    /**
     * 获取事务诊断信息
     */
    public String getTransactionDiagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== BgaiService 事务诊断信息 ===\n");
        sb.append("线程ID: ").append(Thread.currentThread().getId()).append("\n");
        sb.append("线程名称: ").append(Thread.currentThread().getName()).append("\n");
        sb.append("当前XID: ").append(getCurrentXid()).append("\n");
        sb.append("是否在事务中: ").append(isInGlobalTransaction()).append("\n");
        sb.append("时间戳: ").append(System.currentTimeMillis()).append("\n");
        sb.append("=== 诊断信息结束 ===");
        
        return sb.toString();
    }
}
