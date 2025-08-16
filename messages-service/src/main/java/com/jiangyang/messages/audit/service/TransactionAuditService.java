package com.jiangyang.messages.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.audit.entity.TransactionAuditLog;

/**
 * 事务审计服务接口
 * 提供事务审计日志的增删改查服务
 */
@DataSource("master")
public interface TransactionAuditService extends IService<TransactionAuditLog> {

    /**
     * 根据事务ID查询审计日志
     * @param transactionId 事务ID
     * @return 审计日志
     */
    TransactionAuditLog getByTransactionId(String transactionId);

    /**
     * 根据全局事务ID查询审计日志
     * @param globalTransactionId 全局事务ID
     * @return 审计日志列表
     */
    java.util.List<TransactionAuditLog> getByGlobalTransactionId(String globalTransactionId);

    /**
     * 根据业务事务ID查询审计日志
     * @param businessTransactionId 业务事务ID
     * @return 审计日志列表
     */
    java.util.List<TransactionAuditLog> getByBusinessTransactionId(String businessTransactionId);

    /**
     * 根据操作类型查询审计日志
     * @param operationType 操作类型
     * @return 审计日志列表
     */
    java.util.List<TransactionAuditLog> getByOperationType(String operationType);

    /**
     * 根据事务状态查询审计日志
     * @param transactionStatus 事务状态
     * @return 审计日志列表
     */
    java.util.List<TransactionAuditLog> getByTransactionStatus(String transactionStatus);

    /**
     * 更新事务状态
     * @param transactionId 事务ID
     * @param transactionStatus 新状态
     * @return 是否更新成功
     */
    boolean updateTransactionStatus(String transactionId, String transactionStatus);
}
