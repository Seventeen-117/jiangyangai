package com.bgpay.bgai.service;

import com.bgpay.bgai.model.TransactionEvent;
import com.bgpay.bgai.model.TransactionEventResponse;
import com.jiangyang.base.datasource.annotation.DataSource;

import java.util.List;

/**
 * 事务事件服务接口
 */
@DataSource("master")
public interface TransactionEventService {

    /**
     * 处理单个事务事件
     */
    TransactionEventResponse processTransactionEvent(TransactionEvent event);

    /**
     * 批量处理事务事件
     */
    TransactionEventResponse processBatchTransactionEvents(List<TransactionEvent> events);

    /**
     * 获取事务事件历史
     */
    List<TransactionEvent> getTransactionEventHistory(String transactionId);

    /**
     * 根据时间范围查询事务事件
     */
    List<TransactionEvent> getTransactionEventsByTimeRange(String startTime, String endTime, String status);

    /**
     * 获取事务事件统计信息
     */
    Object getTransactionEventStatistics(String timeRange);

    /**
     * 保存事务事件
     */
    void saveTransactionEvent(TransactionEvent event);

    /**
     * 更新事务事件状态
     */
    void updateTransactionEventStatus(String eventId, String status, String message);

    /**
     * 删除过期的事务事件
     */
    void deleteExpiredTransactionEvents(int daysToKeep);
}
