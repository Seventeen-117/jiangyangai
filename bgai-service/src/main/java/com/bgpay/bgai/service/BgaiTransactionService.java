package com.bgpay.bgai.service;

import com.jiangyang.base.seata.service.SeataTransactionService;
import io.seata.core.context.RootContext;
import io.seata.tm.api.GlobalTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * BgaiService 事务服务
 * 集成 base-service 的 SeataTransactionService，提供更高级的事务管理功能
 */
@Service
@Slf4j
public class BgaiTransactionService {

    @Autowired
    private SeataTransactionService seataTransactionService;

    /**
     * 执行带事务的业务逻辑（推荐方式）
     * 
     * @param transactionName 事务名称
     * @param timeout 超时时间（毫秒）
     * @param supplier 业务逻辑提供者
     * @return 业务结果
     */
    public <T> T executeWithTransaction(String transactionName, int timeout, Supplier<T> supplier) {
        GlobalTransaction tx = seataTransactionService.createGlobalTransaction();
        String xid = null;
        
        try {
            // 开始全局事务
            seataTransactionService.beginGlobalTransaction(transactionName, timeout);
            xid = tx.getXid();
            
            log.info("BgaiService 事务开始: XID={}, name={}", xid, transactionName);
            
            // 执行业务逻辑
            T result = supplier.get();
            
            // 提交事务
            seataTransactionService.commitGlobalTransaction(xid);
            log.info("BgaiService 事务提交成功: XID={}", xid);
            
            return result;
            
        } catch (Exception e) {
            // 回滚事务
            if (xid != null) {
                seataTransactionService.rollbackGlobalTransaction(xid);
                log.error("BgaiService 事务回滚: XID={}, error={}", xid, e.getMessage(), e);
            }
            throw new RuntimeException("事务执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行带事务的业务逻辑（无返回值）
     * 
     * @param transactionName 事务名称
     * @param timeout 超时时间（毫秒）
     * @param runnable 业务逻辑
     */
    public void executeWithTransaction(String transactionName, int timeout, Runnable runnable) {
        executeWithTransaction(transactionName, timeout, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 执行带事务的业务逻辑（使用默认超时时间）
     * 
     * @param transactionName 事务名称
     * @param supplier 业务逻辑提供者
     * @return 业务结果
     */
    public <T> T executeWithTransaction(String transactionName, Supplier<T> supplier) {
        return executeWithTransaction(transactionName, 60000, supplier);
    }

    /**
     * 执行带事务的业务逻辑（无返回值，使用默认超时时间）
     * 
     * @param transactionName 事务名称
     * @param runnable 业务逻辑
     */
    public void executeWithTransaction(String transactionName, Runnable runnable) {
        executeWithTransaction(transactionName, 60000, runnable);
    }

    /**
     * 获取当前事务XID
     * 
     * @return 当前事务XID，如果没有事务则返回null
     */
    public String getCurrentXid() {
        return RootContext.getXID();
    }

    /**
     * 检查当前是否有活跃事务
     * 
     * @return 是否有活跃事务
     */
    public boolean hasActiveTransaction() {
        return getCurrentXid() != null;
    }

    /**
     * 手动开始事务（高级用法）
     * 
     * @param transactionName 事务名称
     * @param timeout 超时时间（毫秒）
     * @return 事务对象
     */
    public GlobalTransaction beginTransaction(String transactionName, int timeout) {
        GlobalTransaction tx = seataTransactionService.createGlobalTransaction();
        seataTransactionService.beginGlobalTransaction(transactionName, timeout);
        log.info("BgaiService 手动开始事务: XID={}, name={}", tx.getXid(), transactionName);
        return tx;
    }

    /**
     * 手动提交事务
     * 
     * @param xid 事务XID
     */
    public void commitTransaction(String xid) {
        seataTransactionService.commitGlobalTransaction(xid);
        log.info("BgaiService 手动提交事务: XID={}", xid);
    }

    /**
     * 手动回滚事务
     * 
     * @param xid 事务XID
     */
    public void rollbackTransaction(String xid) {
        seataTransactionService.rollbackGlobalTransaction(xid);
        log.error("BgaiService 手动回滚事务: XID={}", xid);
    }

    /**
     * 执行带重试的事务逻辑
     * 
     * @param transactionName 事务名称
     * @param timeout 超时时间（毫秒）
     * @param maxRetries 最大重试次数
     * @param supplier 业务逻辑提供者
     * @return 业务结果
     */
    public <T> T executeWithTransactionAndRetry(String transactionName, int timeout, int maxRetries, Supplier<T> supplier) {
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount <= maxRetries) {
            try {
                return executeWithTransaction(transactionName, timeout, supplier);
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                
                if (retryCount <= maxRetries) {
                    log.warn("BgaiService 事务执行失败，准备重试: XID={}, retryCount={}, error={}", 
                            getCurrentXid(), retryCount, e.getMessage());
                    
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(1000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("事务重试被中断", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("事务执行失败，已重试" + maxRetries + "次", lastException);
    }

    /**
     * 执行带补偿的事务逻辑（Saga模式）
     * 
     * @param transactionName 事务名称
     * @param timeout 超时时间（毫秒）
     * @param supplier 业务逻辑提供者
     * @param compensator 补偿逻辑提供者
     * @return 业务结果
     */
    public <T> T executeWithCompensation(String transactionName, int timeout, Supplier<T> supplier, Runnable compensator) {
        GlobalTransaction tx = seataTransactionService.createGlobalTransaction();
        String xid = null;
        
        try {
            seataTransactionService.beginGlobalTransaction(transactionName, timeout);
            xid = tx.getXid();
            
            log.info("BgaiService Saga事务开始: XID={}, name={}", xid, transactionName);
            
            T result = supplier.get();
            
            seataTransactionService.commitGlobalTransaction(xid);
            log.info("BgaiService Saga事务提交成功: XID={}", xid);
            
            return result;
            
        } catch (Exception e) {
            if (xid != null) {
                seataTransactionService.rollbackGlobalTransaction(xid);
                log.error("BgaiService Saga事务回滚: XID={}, error={}", xid, e.getMessage(), e);
                
                // 执行补偿逻辑
                try {
                    compensator.run();
                    log.info("BgaiService 补偿逻辑执行成功: XID={}", xid);
                } catch (Exception ce) {
                    log.error("BgaiService 补偿逻辑执行失败: XID={}, error={}", xid, ce.getMessage(), ce);
                }
            }
            throw new RuntimeException("Saga事务执行失败: " + e.getMessage(), e);
        }
    }
}
