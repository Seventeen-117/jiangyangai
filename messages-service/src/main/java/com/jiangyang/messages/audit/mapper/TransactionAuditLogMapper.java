package com.jiangyang.messages.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiangyang.messages.audit.entity.TransactionAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事务审计日志Mapper接口
 */
@Mapper
public interface TransactionAuditLogMapper extends BaseMapper<TransactionAuditLog> {

    /**
     * 根据全局事务ID查询事务审计日志
     */
    List<TransactionAuditLog> selectByGlobalTransactionId(@Param("globalTransactionId") String globalTransactionId);

    /**
     * 根据业务事务ID查询事务审计日志
     */
    List<TransactionAuditLog> selectByBusinessTransactionId(@Param("businessTransactionId") String businessTransactionId);

    /**
     * 根据分支事务ID查询事务审计日志
     */
    List<TransactionAuditLog> selectByBranchTransactionId(@Param("branchTransactionId") String branchTransactionId);

    /**
     * 根据事务状态查询事务审计日志
     */
    List<TransactionAuditLog> selectByTransactionStatus(@Param("transactionStatus") String transactionStatus);

    /**
     * 根据操作状态查询事务审计日志
     */
    List<TransactionAuditLog> selectByOperationStatus(@Param("operationStatus") String operationStatus);

    /**
     * 根据服务名称查询事务审计日志
     */
    List<TransactionAuditLog> selectByServiceName(@Param("serviceName") String serviceName);

    /**
     * 根据操作类型查询事务审计日志
     */
    List<TransactionAuditLog> selectByOperationType(@Param("operationType") String operationType);

    /**
     * 根据消息类型查询事务审计日志
     */
    List<TransactionAuditLog> selectByMessageType(@Param("messageType") String messageType);

    /**
     * 根据时间范围查询事务审计日志
     */
    List<TransactionAuditLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 根据操作链ID查询事务审计日志
     */
    List<TransactionAuditLog> selectByOperationChainId(@Param("operationChainId") String operationChainId);

    /**
     * 查询失败的事务
     */
    List<TransactionAuditLog> selectFailedTransactions(@Param("startTime") LocalDateTime startTime, 
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查询超时的事务
     */
    List<TransactionAuditLog> selectTimeoutTransactions(@Param("startTime") LocalDateTime startTime, 
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的事务数量
     */
    Long countTransactionsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的失败事务数量
     */
    Long countFailedTransactionsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的超时事务数量
     */
    Long countTimeoutTransactionsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询事务性能统计
     */
    List<TransactionAuditLog> selectTransactionPerformanceStats(@Param("startTime") LocalDateTime startTime, 
                                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询完整的事务操作链
     */
    List<TransactionAuditLog> selectCompleteTransactionChain(@Param("operationChainId") String operationChainId);
}
