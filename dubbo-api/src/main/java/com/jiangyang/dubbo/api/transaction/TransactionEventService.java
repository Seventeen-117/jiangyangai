package com.jiangyang.dubbo.api.transaction;

import com.jiangyang.dubbo.api.common.Result;
import com.jiangyang.dubbo.api.transaction.model.TransactionEvent;
import com.jiangyang.dubbo.api.transaction.model.TransactionEventResponse;

import java.util.List;

/**
 * 事务事件服务接口
 * 用于接收和处理事务状态事件
 */
public interface TransactionEventService {

    /**
     * 处理单个事务事件
     *
     * @param event 事务事件
     * @return 处理结果
     */
    Result<TransactionEventResponse> processTransactionEvent(TransactionEvent event);

    /**
     * 批量处理事务事件
     *
     * @param events 事务事件列表
     * @return 处理结果
     */
    Result<TransactionEventResponse> processBatchTransactionEvents(List<TransactionEvent> events);

    /**
     * 查询事务事件历史
     *
     * @param transactionId 事务ID
     * @return 事件列表
     */
    Result<List<TransactionEvent>> getTransactionEventHistory(String transactionId);

    /**
     * 根据时间范围查询事务事件
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param status    状态
     * @return 事件列表
     */
    Result<List<TransactionEvent>> getTransactionEventsByTimeRange(String startTime, String endTime, String status);

    /**
     * 获取事务事件统计信息
     *
     * @param timeRange 时间范围
     * @return 统计信息
     */
    Result<Object> getTransactionEventStatistics(String timeRange);

    /**
     * 更新事务事件状态
     *
     * @param eventId      事件ID
     * @param status      状态
     * @param message     消息
     * @return 更新结果
     */
    Result<Boolean> updateTransactionEventStatus(String eventId, String status, String message);

    /**
     * 删除过期的事务事件
     *
     * @param daysToKeep 保留天数
     * @return 删除结果
     */
    Result<Boolean> deleteExpiredTransactionEvents(int daysToKeep);
}
