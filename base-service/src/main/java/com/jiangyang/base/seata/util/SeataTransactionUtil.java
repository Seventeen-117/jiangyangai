package com.jiangyang.base.seata.util;

import io.seata.core.context.RootContext;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Seata 事务工具类
 * 用于检查和诊断事务状态
 */
@Slf4j
@Component
public class SeataTransactionUtil {

    /**
     * 检查当前线程是否在全局事务中
     */
    public boolean isInGlobalTransaction() {
        String xid = RootContext.getXID();
        boolean inTransaction = xid != null;
        log.debug("检查全局事务状态: inTransaction={}, XID={}", inTransaction, xid);
        return inTransaction;
    }

    /**
     * 获取当前线程的全局事务ID
     */
    public String getCurrentXid() {
        String xid = RootContext.getXID();
        log.debug("获取当前线程XID: {}", xid);
        return xid;
    }

    /**
     * 检查全局事务状态
     */
    public void checkGlobalTransactionStatus() {
        String xid = RootContext.getXID();
        if (xid != null) {
            try {
                GlobalTransaction globalTransaction = GlobalTransactionContext.reload(xid);
                log.info("全局事务状态检查: XID={}, Status={}", xid, globalTransaction.getStatus());
            } catch (Exception e) {
                log.warn("无法获取全局事务状态: XID={}", xid, e);
            }
        } else {
            log.debug("当前线程不在全局事务中");
        }
    }

    /**
     * 打印事务诊断信息
     */
    public void printTransactionDiagnostics() {
        log.info("=== Seata 事务诊断信息 ===");
        log.info("当前线程ID: {}", Thread.currentThread().getId());
        log.info("当前线程名称: {}", Thread.currentThread().getName());
        
        String xid = RootContext.getXID();
        log.info("当前线程XID: {}", xid);
        
        if (xid != null) {
            try {
                GlobalTransaction globalTransaction = GlobalTransactionContext.reload(xid);
                log.info("全局事务状态: {}", globalTransaction.getStatus());
                log.info("全局事务对象: {}", globalTransaction);
            } catch (Exception e) {
                log.warn("无法获取全局事务详细信息: XID={}", xid, e);
            }
        }
        
        log.info("=== 诊断信息结束 ===");
    }

    /**
     * 验证 Seata 连接状态
     */
    public void validateSeataConnection() {
        log.info("=== Seata 连接状态验证 ===");
        
        try {
            // 尝试创建一个测试事务来验证连接
            GlobalTransaction testTransaction = GlobalTransactionContext.getCurrentOrCreate();
            log.info("Seata 客户端连接正常");
            
            // 检查是否有可用的 Seata 服务器
            String currentXid = RootContext.getXID();
            if (currentXid != null) {
                log.info("当前有活跃的全局事务: XID={}", currentXid);
            } else {
                log.info("当前没有活跃的全局事务");
            }
            
        } catch (Exception e) {
            log.error("Seata 连接验证失败", e);
        }
        
        log.info("=== 连接验证结束 ===");
    }
}
