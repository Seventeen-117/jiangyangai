package com.jiangyang.base.seata.service.impl;

import com.jiangyang.base.seata.service.SeataTransactionService;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Seata事务服务实现类
 */
@Slf4j
@Service
public class SeataTransactionServiceImpl implements SeataTransactionService {

    @Override
    public String getCurrentXid() {
        return RootContext.getXID();
    }

    @Override
    public void bindXid(String xid) {
        if (xid != null && !xid.isEmpty()) {
            RootContext.bind(xid);
            log.debug("绑定全局事务ID到当前线程: XID={}", xid);
        }
    }

    @Override
    public void unbindXid() {
        String xid = RootContext.getXID();
        if (xid != null) {
            RootContext.unbind();
            log.debug("解绑当前线程的全局事务ID: XID={}", xid);
        }
    }

    @Override
    public GlobalTransaction createGlobalTransaction() {
        try {
            GlobalTransaction globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
            log.debug("创建新的全局事务: XID={}", globalTransaction.getXid());
            return globalTransaction;
        } catch (Exception e) {
            log.error("创建全局事务失败", e);
            throw new RuntimeException("创建全局事务失败", e);
        }
    }

    @Override
    public void beginGlobalTransaction(String name, int timeout) {
        try {
            GlobalTransaction globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
            globalTransaction.begin(timeout, name);
            log.info("开始全局事务: XID={}, name={}, timeout={}", 
                    globalTransaction.getXid(), name, timeout);
        } catch (Exception e) {
            log.error("开始全局事务失败: name={}, timeout={}", name, timeout, e);
            throw new RuntimeException("开始全局事务失败", e);
        }
    }

    @Override
    public void commitGlobalTransaction(String xid) {
        if (xid == null || xid.isEmpty()) {
            log.warn("全局事务ID为空，跳过提交");
            return;
        }
        
        try {
            GlobalTransaction globalTransaction = GlobalTransactionContext.reload(xid);
            globalTransaction.commit();
            log.info("提交全局事务成功: XID={}", xid);
        } catch (TransactionException e) {
            log.error("提交全局事务失败: XID={}", xid, e);
            throw new RuntimeException("提交全局事务失败", e);
        }
    }

    @Override
    public void rollbackGlobalTransaction(String xid) {
        if (xid == null || xid.isEmpty()) {
            log.warn("全局事务ID为空，跳过回滚");
            return;
        }
        
        try {
            GlobalTransaction globalTransaction = GlobalTransactionContext.reload(xid);
            globalTransaction.rollback();
            log.info("回滚全局事务成功: XID={}", xid);
        } catch (TransactionException e) {
            log.error("回滚全局事务失败: XID={}", xid, e);
            throw new RuntimeException("回滚全局事务失败", e);
        }
    }

    @Override
    public boolean isInGlobalTransaction() {
        return RootContext.getXID() != null;
    }

    @Override
    public int getGlobalTransactionStatus(String xid) {
        if (xid == null || xid.isEmpty()) {
            return -1;
        }
        
        try {
            GlobalTransaction globalTransaction = GlobalTransactionContext.reload(xid);
            return globalTransaction.getStatus().getCode();
        } catch (TransactionException e) {
            log.error("获取全局事务状态失败: XID={}", xid, e);
            return -1;
        }
    }
}
