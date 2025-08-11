package com.jiangyang.messages.audit.service;

import com.jiangyang.messages.audit.entity.BusinessTraceLog;
import com.jiangyang.messages.audit.entity.MessageLifecycleLog;
import com.jiangyang.messages.audit.entity.TransactionAuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务接口
 * 提供事务追溯和业务处理轨迹记录功能
 */
public interface AuditLogService {

    /**
     * 记录事务审计日志
     */
    void recordTransactionAuditLog(TransactionAuditLog auditLog);

    /**
     * 记录消息生命周期日志
     */
    void recordMessageLifecycleLog(MessageLifecycleLog lifecycleLog);

    /**
     * 记录业务轨迹日志
     */
    void recordBusinessTraceLog(BusinessTraceLog traceLog);

    /**
     * 批量记录事务审计日志
     */
    void batchRecordTransactionAuditLog(List<TransactionAuditLog> auditLogs);

    /**
     * 批量记录消息生命周期日志
     */
    void batchRecordMessageLifecycleLog(List<MessageLifecycleLog> lifecycleLogs);

    /**
     * 批量记录业务轨迹日志
     */
    void batchRecordBusinessTraceLog(List<BusinessTraceLog> traceLogs);

    /**
     * 根据全局事务ID查询完整的事务轨迹
     */
    Map<String, Object> getTransactionTrace(String globalTransactionId);

    /**
     * 根据业务事务ID查询完整的事务轨迹
     */
    Map<String, Object> getBusinessTransactionTrace(String businessTransactionId);

    /**
     * 根据消息ID查询完整的消息生命周期
     */
    Map<String, Object> getMessageLifecycleTrace(String messageId);

    /**
     * 根据调用链ID查询完整的业务调用轨迹
     */
    Map<String, Object> getBusinessTraceChain(String traceId);

    /**
     * 查询指定时间范围内的事务统计
     */
    Map<String, Object> getTransactionStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间范围内的消息统计
     */
    Map<String, Object> getMessageStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间范围内的调用统计
     */
    Map<String, Object> getCallStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询失败的事务列表
     */
    List<TransactionAuditLog> getFailedTransactions(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询死信消息列表
     */
    List<MessageLifecycleLog> getDeadLetterMessages(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询失败的调用列表
     */
    List<BusinessTraceLog> getFailedCalls(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 清理过期的审计日志
     */
    void cleanExpiredAuditLogs(int retentionDays);

    /**
     * 导出审计日志
     */
    byte[] exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String format);
}
