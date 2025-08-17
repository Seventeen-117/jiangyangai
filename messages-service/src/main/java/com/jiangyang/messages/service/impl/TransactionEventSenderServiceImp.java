package com.jiangyang.messages.service.impl;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.dubbo.api.transaction.TransactionEventService;
import com.jiangyang.dubbo.api.transaction.model.TransactionEvent;
import com.jiangyang.dubbo.api.transaction.model.TransactionEventResponse;
import com.jiangyang.dubbo.api.common.Result;
import com.jiangyang.messages.service.TransactionEventSenderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 事务事件发送服务
 * 负责向 bgai-service 发送事务状态事件
 */
@Slf4j
@Service
@DataSource("master")
public class TransactionEventSenderServiceImp implements TransactionEventSenderService {

    @DubboReference(version = "1.0.0", timeout = 3000, retries = 0, check = false, cluster = "failfast")
    private TransactionEventService transactionEventService;

    /**
     * 同步发送单个事务事件
     */
    @Override
    public TransactionEventResponse sendTransactionEvent(TransactionEvent event) {
        try {
            log.info("开始发送事务事件: transactionId={}, operationType={}, status={}", 
                    event.getTransactionId(), event.getOperationType(), event.getStatus());

            // 检查Dubbo服务是否可用
            if (transactionEventService == null) {
                log.warn("TransactionEventService不可用，跳过事务事件发送: transactionId={}", event.getTransactionId());
                return TransactionEventResponse.builder()
                        .success(false)
                        .message("TransactionEventService不可用")
                        .transactionId(event.getTransactionId())
                        .errorMessage("TransactionEventService不可用")
                        .build();
            }

            Result<TransactionEventResponse> result = transactionEventService.processTransactionEvent(event);
            
            if (Boolean.TRUE.equals(result.getSuccess()) && result.getData() != null) {
                TransactionEventResponse response = result.getData();
                log.info("事务事件发送成功: eventId={}, transactionId={}", 
                        event.getEventId(), event.getTransactionId());
                return response;
            } else {
                log.warn("事务事件发送失败: eventId={}, transactionId={}, error={}", 
                        event.getEventId(), event.getTransactionId(), result.getMessage());
                return TransactionEventResponse.builder()
                        .success(false)
                        .message(result.getMessage())
                        .transactionId(event.getTransactionId())
                        .errorMessage(result.getMessage())
                        .build();
            }
            
        } catch (Exception e) {
            // 检查是否是Dubbo服务不可用的错误
            if (e.getMessage() != null && e.getMessage().contains("No provider available")) {
                log.warn("TransactionEventService提供者不可用，跳过事务事件发送: transactionId={}, error={}", 
                        event.getTransactionId(), e.getMessage());
                return TransactionEventResponse.builder()
                        .success(true) // 标记为成功，因为这是预期的降级行为
                        .message("TransactionEventService提供者不可用，事件已跳过")
                        .transactionId(event.getTransactionId())
                        .errorMessage("TransactionEventService提供者不可用")
                        .build();
            }
            
            log.error("发送事务事件异常: transactionId={}, error={}", 
                    event.getTransactionId(), e.getMessage(), e);
            
            return TransactionEventResponse.builder()
                    .success(false)
                    .message("发送事务事件异常")
                    .transactionId(event.getTransactionId())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 异步发送单个事务事件
     */
    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendTransactionEventAsync(TransactionEvent event) {
        return CompletableFuture.completedFuture(sendTransactionEvent(event));
    }

    /**
     * 同步批量发送事务事件
     */
    @Override
    public TransactionEventResponse sendBatchTransactionEvents(List<TransactionEvent> events) {
        try {
            log.info("开始批量发送事务事件，数量: {}", events.size());

            Result<TransactionEventResponse> result = transactionEventService.processBatchTransactionEvents(events);
            
            if (Boolean.TRUE.equals(result.getSuccess()) && result.getData() != null) {
                TransactionEventResponse response = result.getData();
                log.info("批量事务事件发送成功，数量: {}", events.size());
                return response;
            } else {
                log.warn("批量事务事件发送失败，数量: {}, error: {}", events.size(), result.getMessage());
                return TransactionEventResponse.builder()
                        .success(false)
                        .message(result.getMessage())
                        .transactionId("batch")
                        .errorMessage(result.getMessage())
                        .build();
            }
            
        } catch (Exception e) {
            // 检查是否是Dubbo服务不可用的错误
            if (e.getMessage() != null && e.getMessage().contains("No provider available")) {
                log.warn("TransactionEventService提供者不可用，跳过批量事务事件发送，数量: {}, error={}", 
                        events.size(), e.getMessage());
                return TransactionEventResponse.builder()
                        .success(true) // 标记为成功，因为这是预期的降级行为
                        .message("TransactionEventService提供者不可用，批量事件已跳过")
                        .transactionId("batch")
                        .errorMessage("TransactionEventService提供者不可用")
                        .build();
            }
            
            log.error("批量发送事务事件异常，数量: {}, error: {}", 
                    events.size(), e.getMessage(), e);
            
            return TransactionEventResponse.builder()
                    .success(false)
                    .message("批量发送事务事件异常: " + e.getMessage())
                    .transactionId("batch")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 异步批量发送事务事件
     */
    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendBatchTransactionEventsAsync(List<TransactionEvent> events) {
        return CompletableFuture.completedFuture(sendBatchTransactionEvents(events));
    }

    // ======== 便捷发送方法（使用本地构建器创建事件） ========
    @Override
    public TransactionEventResponse sendTransactionBeginEvent(String globalTransactionId, String transactionId,
                                                              String serviceName, String businessType, String businessId) {
        TransactionEvent event = buildEvent(globalTransactionId, transactionId, serviceName, businessType, businessId,
                "TRANSACTION_BEGIN", "BEGIN", null, null, null);
        return sendTransactionEvent(event);
    }

    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendTransactionBeginEventAsync(String globalTransactionId,
                                                                                      String transactionId,
                                                                                      String serviceName,
                                                                                      String businessType,
                                                                                      String businessId) {
        return CompletableFuture.completedFuture(sendTransactionBeginEvent(
                globalTransactionId, transactionId, serviceName, businessType, businessId));
    }
    @Override
    public TransactionEventResponse sendTransactionCommitEvent(String globalTransactionId, String transactionId,
                                                               String serviceName, String businessType, String businessId) {
        TransactionEvent event = buildEvent(globalTransactionId, transactionId, serviceName, businessType, businessId,
                "TRANSACTION_COMMIT", "SUCCESS", null, null, null);
        return sendTransactionEvent(event);
    }

    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendTransactionCommitEventAsync(String globalTransactionId,
                                                                                       String transactionId,
                                                                                       String serviceName,
                                                                                       String businessType,
                                                                                       String businessId) {
        return CompletableFuture.completedFuture(sendTransactionCommitEvent(
                globalTransactionId, transactionId, serviceName, businessType, businessId));
    }
    @Override
    public TransactionEventResponse sendTransactionRollbackEvent(String globalTransactionId, String transactionId,
                                                                 String serviceName, String businessType, String businessId,
                                                                 String errorMessage) {
        TransactionEvent event = buildEvent(globalTransactionId, transactionId, serviceName, businessType, businessId,
                "TRANSACTION_ROLLBACK", "ROLLBACK", null, errorMessage, null);
        return sendTransactionEvent(event);
    }

    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendTransactionRollbackEventAsync(String globalTransactionId,
                                                                                         String transactionId,
                                                                                         String serviceName,
                                                                                         String businessType,
                                                                                         String businessId,
                                                                                         String errorMessage) {
        return CompletableFuture.completedFuture(sendTransactionRollbackEvent(
                globalTransactionId, transactionId, serviceName, businessType, businessId, errorMessage));
    }
    @Override
    public TransactionEventResponse sendMessageSendEvent(String globalTransactionId, String transactionId,
                                                         String serviceName, String businessType, String businessId,
                                                         String messageType) {
        TransactionEvent event = buildEvent(globalTransactionId, transactionId, serviceName, businessType, businessId,
                "MESSAGE_SEND", "PROCESSING", messageType, null, null);
        return sendTransactionEvent(event);
    }

    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendMessageSendEventAsync(String globalTransactionId,
                                                                                 String transactionId,
                                                                                 String serviceName,
                                                                                 String businessType,
                                                                                 String businessId,
                                                                                 String messageType) {
        return CompletableFuture.completedFuture(sendMessageSendEvent(
                globalTransactionId, transactionId, serviceName, businessType, businessId, messageType));
    }
    @Override
    public TransactionEventResponse sendMessageConsumeEvent(String globalTransactionId, String transactionId,
                                                            String serviceName, String businessType, String businessId,
                                                            String messageType) {
        TransactionEvent event = buildEvent(globalTransactionId, transactionId, serviceName, businessType, businessId,
                "MESSAGE_CONSUME", "PROCESSING", messageType, null, null);
        return sendTransactionEvent(event);
    }

    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendMessageConsumeEventAsync(String globalTransactionId,
                                                                                    String transactionId,
                                                                                    String serviceName,
                                                                                    String businessType,
                                                                                    String businessId,
                                                                                    String messageType) {
        return CompletableFuture.completedFuture(sendMessageConsumeEvent(
                globalTransactionId, transactionId, serviceName, businessType, businessId, messageType));
    }
    @Override
    public TransactionEventResponse sendSagaExecuteEvent(String globalTransactionId, String transactionId,
                                                         String serviceName, String businessType, String businessId,
                                                         String sagaStep) {
        TransactionEvent event = buildEvent(globalTransactionId, transactionId, serviceName, businessType, businessId,
                "SAGA_EXECUTE", "PROCESSING", null, null, sagaStep);
        return sendTransactionEvent(event);
    }

    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendSagaExecuteEventAsync(String globalTransactionId,
                                                                                 String transactionId,
                                                                                 String serviceName,
                                                                                 String businessType,
                                                                                 String businessId,
                                                                                 String sagaStep) {
        return CompletableFuture.completedFuture(sendSagaExecuteEvent(
                globalTransactionId, transactionId, serviceName, businessType, businessId, sagaStep));
    }
    @Override
    public TransactionEventResponse sendSagaCompensateEvent(String globalTransactionId, String transactionId,
                                                            String serviceName, String businessType, String businessId,
                                                            String sagaStep, String errorMessage) {
        TransactionEvent event = buildEvent(globalTransactionId, transactionId, serviceName, businessType, businessId,
                "SAGA_COMPENSATE", "FAILED", null, errorMessage, sagaStep);
        return sendTransactionEvent(event);
    }

    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendSagaCompensateEventAsync(String globalTransactionId,
                                                                                    String transactionId,
                                                                                    String serviceName,
                                                                                    String businessType,
                                                                                    String businessId,
                                                                                    String sagaStep,
                                                                                    String errorMessage) {
        return CompletableFuture.completedFuture(sendSagaCompensateEvent(
                globalTransactionId, transactionId, serviceName, businessType, businessId, sagaStep, errorMessage));
    }
    @Override
    public TransactionEventResponse sendCustomTransactionEvent(TransactionEvent event) {
        return sendTransactionEvent(event);
    }

    @Async
    @Override
    public CompletableFuture<TransactionEventResponse> sendCustomTransactionEventAsync(TransactionEvent event) {
        return CompletableFuture.completedFuture(sendCustomTransactionEvent(event));
    }
    @Override
    public List<TransactionEvent> getTransactionEventHistory(String transactionId) {
        try {
            log.info("查询事务事件历史: transactionId={}", transactionId);

            Result<List<TransactionEvent>> result = transactionEventService.getTransactionEventHistory(transactionId);
            
            if (Boolean.TRUE.equals(result.getSuccess()) && result.getData() != null) {
                List<TransactionEvent> events = result.getData();
                log.info("查询事务事件历史成功，数量: {}", events.size());
                return events;
            } else {
                log.warn("查询事务事件历史失败: transactionId={}, error={}", transactionId, result.getMessage());
                return List.of();
            }
            
        } catch (Exception e) {
            log.error("查询事务事件历史异常: transactionId={}, error={}", 
                    transactionId, e.getMessage(), e);
            return List.of();
        }
    }
    @Override
    public List<TransactionEvent> getTransactionEventsByTimeRange(String startTime, String endTime, String status) {
        try {
            log.info("根据时间范围查询事务事件: startTime={}, endTime={}, status={}", startTime, endTime, status);

            Result<List<TransactionEvent>> result = transactionEventService.getTransactionEventsByTimeRange(startTime, endTime, status);
            
            if (Boolean.TRUE.equals(result.getSuccess()) && result.getData() != null) {
                List<TransactionEvent> events = result.getData();
                log.info("根据时间范围查询事务事件成功，数量: {}", events.size());
                return events;
            } else {
                log.warn("根据时间范围查询事务事件失败: startTime={}, endTime={}, status={}, error={}", 
                        startTime, endTime, status, result.getMessage());
                return List.of();
            }
            
        } catch (Exception e) {
            log.error("根据时间范围查询事务事件异常: startTime={}, endTime={}, status={}, error={}", 
                    startTime, endTime, status, e.getMessage(), e);
            return List.of();
        }
    }
    @Override
    public Object getTransactionEventStatistics(String timeRange) {
        try {
            log.info("获取事务事件统计信息: timeRange={}", timeRange);

            Result<Object> result = transactionEventService.getTransactionEventStatistics(timeRange);
            
            if (Boolean.TRUE.equals(result.getSuccess()) && result.getData() != null) {
                Object statistics = result.getData();
                log.info("获取事务事件统计信息成功");
                return statistics;
            } else {
                log.warn("获取事务事件统计信息失败: timeRange={}, error={}", timeRange, result.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("获取事务事件统计信息异常: timeRange={}, error={}", 
                    timeRange, e.getMessage(), e);
            return null;
        }
    }

    // ======== 辅助方法：构建事件对象 ========
    private TransactionEvent buildEvent(String globalTransactionId,
                                        String transactionId,
                                        String serviceName,
                                        String businessType,
                                        String businessId,
                                        String operationType,
                                        String status,
                                        String messageType,
                                        String errorMessage,
                                        String description) {
        LocalDateTime now = LocalDateTime.now();
        return TransactionEvent.builder()
                .globalTransactionId(globalTransactionId)
                .transactionId(transactionId)
                .serviceName(serviceName)
                .businessType(businessType)
                .businessId(businessId)
                .operationType(operationType)
                .status(status)
                .messageType(messageType)
                .errorMessage(errorMessage)
                .description(description)
                .createTime(now)
                .updateTime(now)
                .processTime(now)
                .version("1.0")
                .source("messages-service")
                .priority(0)
                .asyncProcess(false)
                .build();
    }
}
