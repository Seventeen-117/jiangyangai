package com.jiangyang.base.seata.util;

import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Seata工具类
 * 提供常用的Seata操作工具方法
 */
@Slf4j
public class SeataUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private SeataUtils() {}

    /**
     * 获取当前全局事务ID
     */
    public static String getCurrentXid() {
        return RootContext.getXID();
    }

    /**
     * 检查是否在全局事务中
     */
    public static boolean isInGlobalTransaction() {
        return RootContext.getXID() != null;
    }

    /**
     * 绑定全局事务ID到当前线程
     */
    public static void bindXid(String xid) {
        if (xid != null && !xid.isEmpty()) {
            RootContext.bind(xid);
            log.debug("绑定全局事务ID到当前线程: XID={}", xid);
        }
    }

    /**
     * 解绑当前线程的全局事务ID
     */
    public static void unbindXid() {
        String xid = RootContext.getXID();
        if (xid != null) {
            RootContext.unbind();
            log.debug("解绑当前线程的全局事务ID: XID={}", xid);
        }
    }

    /**
     * 在全局事务中执行操作
     * 如果当前没有全局事务，会创建一个新的
     */
    public static <T> T executeInTransaction(String transactionName, int timeout, Supplier<T> supplier) {
        String currentXid = getCurrentXid();
        GlobalTransaction globalTransaction = null;
        
        try {
            // 如果没有全局事务，创建一个新的
            if (currentXid == null) {
                globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
                globalTransaction.begin(timeout, transactionName);
                currentXid = globalTransaction.getXid();
                log.info("创建新的全局事务: XID={}, name={}, timeout={}", currentXid, transactionName, timeout);
            }
            
            // 执行操作
            T result = supplier.get();
            
            // 如果是新创建的事务，提交
            if (globalTransaction != null) {
                globalTransaction.commit();
                log.info("全局事务提交成功: XID={}", currentXid);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("全局事务执行失败: XID={}, name={}", currentXid, transactionName, e);
            
            // 如果是新创建的事务，回滚
            if (globalTransaction != null) {
                try {
                    globalTransaction.rollback();
                    log.info("全局事务回滚成功: XID={}", currentXid);
                } catch (TransactionException rollbackException) {
                    log.error("全局事务回滚失败: XID={}", currentXid, rollbackException);
                }
            }
            
            throw new RuntimeException("全局事务执行失败", e);
        }
    }

    /**
     * 在全局事务中执行操作（无返回值）
     */
    public static void executeInTransaction(String transactionName, int timeout, Runnable runnable) {
        executeInTransaction(transactionName, timeout, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 传播全局事务ID到消息属性
     */
    public static String getXidForMessage() {
        String xid = getCurrentXid();
        if (xid != null) {
            log.debug("传播全局事务ID到消息: XID={}", xid);
        }
        return xid;
    }

    /**
     * 从消息属性恢复全局事务ID
     */
    public static void restoreXidFromMessage(String xid) {
        if (xid != null && !xid.isEmpty()) {
            bindXid(xid);
            log.debug("从消息恢复全局事务ID: XID={}", xid);
        }
    }

    /**
     * 清理全局事务上下文
     */
    public static void clearTransactionContext() {
        String xid = getCurrentXid();
        if (xid != null) {
            RootContext.unbind();
            log.debug("清理全局事务上下文: XID={}", xid);
        }
    }
}
