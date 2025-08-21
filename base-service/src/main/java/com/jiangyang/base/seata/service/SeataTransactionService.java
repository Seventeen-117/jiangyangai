package com.jiangyang.base.seata.service;

import io.seata.core.context.RootContext;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;

/**
 * Seata事务服务接口
 * 提供分布式事务的基础操作
 */
public interface SeataTransactionService {

    /**
     * 获取当前全局事务ID
     */
    String getCurrentXid();

    /**
     * 绑定全局事务ID到当前线程
     */
    void bindXid(String xid);

    /**
     * 解绑当前线程的全局事务ID
     */
    void unbindXid();

    /**
     * 创建新的全局事务
     */
    GlobalTransaction createGlobalTransaction();

    /**
     * 开始全局事务
     */
    void beginGlobalTransaction(String name, int timeout);

    /**
     * 提交全局事务
     */
    void commitGlobalTransaction(String xid);

    /**
     * 回滚全局事务
     */
    void rollbackGlobalTransaction(String xid);

    /**
     * 检查是否在全局事务中
     */
    boolean isInGlobalTransaction();

    /**
     * 获取全局事务状态
     */
    int getGlobalTransactionStatus(String xid);
}
