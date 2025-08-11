package com.bgpay.bgai.service.impl;

import com.bgpay.bgai.model.TransactionEvent;
import com.bgpay.bgai.model.TransactionEventResponse;
import com.bgpay.bgai.service.TransactionEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 事务事件服务实现类
 * 注意：这是一个内存实现，生产环境应该使用数据库存储
 */
@Slf4j
@Service
public class TransactionEventServiceImpl implements TransactionEventService {

    // 内存存储，生产环境应该使用数据库
    private final Map<String, TransactionEvent> eventStore = new ConcurrentHashMap<>();
    private final Map<String, List<String>> transactionEventIndex = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public TransactionEventResponse processTransactionEvent(TransactionEvent event) {
        try {
            log.info("开始处理事务事件: transactionId={}, operationType={}, status={}", 
                    event.getTransactionId(), event.getOperationType(), event.getStatus());

            // 生成事件ID
            if (event.getEventId() == null) {
                event.setEventId("evt_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
            }

            // 设置事件来源
            event.setSource("messages-service");
            event.setUpdateTime(LocalDateTime.now());

            // 保存事件
            saveTransactionEvent(event);

            // 根据操作类型进行特殊处理
            switch (event.getOperationType()) {
                case "TRANSACTION_BEGIN":
                    handleTransactionBegin(event);
                    break;
                case "TRANSACTION_COMMIT":
                    handleTransactionCommit(event);
                    break;
                case "TRANSACTION_ROLLBACK":
                    handleTransactionRollback(event);
                    break;
                case "MESSAGE_SEND":
                    handleMessageSend(event);
                    break;
                case "MESSAGE_CONSUME":
                    handleMessageConsume(event);
                    break;
                case "SAGA_EXECUTE":
                    handleSagaExecute(event);
                    break;
                case "SAGA_COMPENSATE":
                    handleSagaCompensate(event);
                    break;
                default:
                    log.warn("未知的操作类型: {}", event.getOperationType());
            }

            log.info("事务事件处理成功: eventId={}, transactionId={}", 
                    event.getEventId(), event.getTransactionId());

            return TransactionEventResponse.success("事务事件处理成功", event.getTransactionId());

        } catch (Exception e) {
            log.error("处理事务事件失败: transactionId={}, error={}", 
                    event.getTransactionId(), e.getMessage(), e);
            return TransactionEventResponse.failure("处理事务事件失败", 
                    event.getTransactionId(), e.getMessage());
        }
    }

    @Override
    public TransactionEventResponse processBatchTransactionEvents(List<TransactionEvent> events) {
        try {
            log.info("开始批量处理事务事件，数量: {}", events.size());

            List<TransactionEventResponse> responses = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (TransactionEvent event : events) {
                try {
                    TransactionEventResponse response = processTransactionEvent(event);
                    responses.add(response);
                    if (response.getSuccess()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("批量处理中单条事件失败: transactionId={}, error={}", 
                            event.getTransactionId(), e.getMessage(), e);
                    responses.add(TransactionEventResponse.failure("单条事件处理失败", 
                            event.getTransactionId(), e.getMessage()));
                    failureCount++;
                }
            }

            log.info("批量事务事件处理完成，成功: {}, 失败: {}", successCount, failureCount);

            TransactionEventResponse batchResponse = TransactionEventResponse.success(
                    String.format("批量处理完成，成功: %d, 失败: %d", successCount, failureCount));
            batchResponse.addDetail("totalCount", events.size());
            batchResponse.addDetail("successCount", successCount);
            batchResponse.addDetail("failureCount", failureCount);
            batchResponse.addDetail("responses", responses);

            return batchResponse;

        } catch (Exception e) {
            log.error("批量处理事务事件失败: error={}", e.getMessage(), e);
            return TransactionEventResponse.failure("批量处理事务事件失败: " + e.getMessage());
        }
    }

    @Override
    public List<TransactionEvent> getTransactionEventHistory(String transactionId) {
        List<String> eventIds = transactionEventIndex.get(transactionId);
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        return eventIds.stream()
                .map(eventStore::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TransactionEvent::getCreateTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionEvent> getTransactionEventsByTimeRange(String startTime, String endTime, String status) {
        LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);

        return eventStore.values().stream()
                .filter(event -> {
                    boolean timeMatch = event.getCreateTime() != null &&
                            !event.getCreateTime().isBefore(start) &&
                            !event.getCreateTime().isAfter(end);
                    
                    boolean statusMatch = status == null || status.equals(event.getStatus());
                    
                    return timeMatch && statusMatch;
                })
                .sorted(Comparator.comparing(TransactionEvent::getCreateTime))
                .collect(Collectors.toList());
    }

    @Override
    public Object getTransactionEventStatistics(String timeRange) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总事件数
        statistics.put("totalEvents", eventStore.size());
        
        // 按状态统计
        Map<String, Long> statusStats = eventStore.values().stream()
                .collect(Collectors.groupingBy(
                        event -> event.getStatus() != null ? event.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));
        statistics.put("statusStatistics", statusStats);
        
        // 按操作类型统计
        Map<String, Long> operationStats = eventStore.values().stream()
                .collect(Collectors.groupingBy(
                        event -> event.getOperationType() != null ? event.getOperationType() : "UNKNOWN",
                        Collectors.counting()
                ));
        statistics.put("operationStatistics", operationStats);
        
        // 按服务名称统计
        Map<String, Long> serviceStats = eventStore.values().stream()
                .collect(Collectors.groupingBy(
                        event -> event.getServiceName() != null ? event.getServiceName() : "UNKNOWN",
                        Collectors.counting()
                ));
        statistics.put("serviceStatistics", serviceStats);
        
        // 最近24小时事件数
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        long recentEvents = eventStore.values().stream()
                .filter(event -> event.getCreateTime() != null && event.getCreateTime().isAfter(yesterday))
                .count();
        statistics.put("recent24HoursEvents", recentEvents);
        
        return statistics;
    }

    @Override
    public void saveTransactionEvent(TransactionEvent event) {
        eventStore.put(event.getEventId(), event);
        
        // 建立事务ID到事件ID的索引
        String transactionId = event.getTransactionId();
        if (transactionId != null) {
            transactionEventIndex.computeIfAbsent(transactionId, k -> new ArrayList<>())
                    .add(event.getEventId());
        }
        
        log.debug("事务事件已保存: eventId={}, transactionId={}", 
                event.getEventId(), event.getTransactionId());
    }

    @Override
    public void updateTransactionEventStatus(String eventId, String status, String message) {
        TransactionEvent event = eventStore.get(eventId);
        if (event != null) {
            event.setStatus(status);
            event.setErrorMessage(message);
            event.setUpdateTime(LocalDateTime.now());
            log.info("事务事件状态已更新: eventId={}, status={}, message={}", 
                    eventId, status, message);
        } else {
            log.warn("未找到要更新的事件: eventId={}", eventId);
        }
    }

    @Override
    public void deleteExpiredTransactionEvents(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        
        List<String> expiredEventIds = eventStore.values().stream()
                .filter(event -> event.getCreateTime() != null && event.getCreateTime().isBefore(cutoffDate))
                .map(TransactionEvent::getEventId)
                .collect(Collectors.toList());
        
        for (String eventId : expiredEventIds) {
            TransactionEvent event = eventStore.remove(eventId);
            if (event != null && event.getTransactionId() != null) {
                // 从索引中移除
                List<String> eventIds = transactionEventIndex.get(event.getTransactionId());
                if (eventIds != null) {
                    eventIds.remove(eventId);
                    if (eventIds.isEmpty()) {
                        transactionEventIndex.remove(event.getTransactionId());
                    }
                }
            }
        }
        
        log.info("已删除过期的事务事件，数量: {}, 保留天数: {}", expiredEventIds.size(), daysToKeep);
    }

    // ==================== 私有方法 ====================

    private void handleTransactionBegin(TransactionEvent event) {
        log.info("处理事务开始事件: transactionId={}", event.getTransactionId());
        // 可以在这里添加事务开始的特殊逻辑
        // 例如：记录事务开始时间、初始化事务上下文等
    }

    private void handleTransactionCommit(TransactionEvent event) {
        log.info("处理事务提交事件: transactionId={}", event.getTransactionId());
        // 可以在这里添加事务提交的特殊逻辑
        // 例如：更新事务状态、发送通知等
    }

    private void handleTransactionRollback(TransactionEvent event) {
        log.info("处理事务回滚事件: transactionId={}, errorMessage={}", 
                event.getTransactionId(), event.getErrorMessage());
        // 可以在这里添加事务回滚的特殊逻辑
        // 例如：记录回滚原因、发送告警等
    }

    private void handleMessageSend(TransactionEvent event) {
        log.info("处理消息发送事件: transactionId={}, messageType={}", 
                event.getTransactionId(), event.getMessageType());
        // 可以在这里添加消息发送的特殊逻辑
        // 例如：更新消息状态、记录发送日志等
    }

    private void handleMessageConsume(TransactionEvent event) {
        log.info("处理消息消费事件: transactionId={}, messageType={}", 
                event.getTransactionId(), event.getMessageType());
        // 可以在这里添加消息消费的特殊逻辑
        // 例如：更新消费状态、记录处理日志等
    }

    private void handleSagaExecute(TransactionEvent event) {
        log.info("处理Saga执行事件: transactionId={}", event.getTransactionId());
        // 可以在这里添加Saga执行的特殊逻辑
        // 例如：更新Saga状态、记录执行步骤等
    }

    private void handleSagaCompensate(TransactionEvent event) {
        log.info("处理Saga补偿事件: transactionId={}, errorMessage={}", 
                event.getTransactionId(), event.getErrorMessage());
        // 可以在这里添加Saga补偿的特殊逻辑
        // 例如：记录补偿原因、发送补偿通知等
    }
}
