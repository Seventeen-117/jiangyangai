package com.jiangyang.messages.service;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.dubbo.api.transaction.model.TransactionEvent;
import com.jiangyang.dubbo.api.transaction.model.TransactionEventResponse;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@DataSource("master")
public interface TransactionEventSenderService {
    TransactionEventResponse sendTransactionEvent(TransactionEvent event);

    @Async
    CompletableFuture<TransactionEventResponse> sendTransactionEventAsync(TransactionEvent event);

    TransactionEventResponse sendBatchTransactionEvents(List<TransactionEvent> events);

    @Async
    CompletableFuture<TransactionEventResponse> sendBatchTransactionEventsAsync(List<TransactionEvent> events);

    // ======== 便捷发送方法（使用本地构建器创建事件） ========
    TransactionEventResponse sendTransactionBeginEvent(String globalTransactionId, String transactionId,
                                                       String serviceName, String businessType, String businessId);

    @Async
    CompletableFuture<TransactionEventResponse> sendTransactionBeginEventAsync(String globalTransactionId,
                                                                               String transactionId,
                                                                               String serviceName,
                                                                               String businessType,
                                                                               String businessId);

    TransactionEventResponse sendTransactionCommitEvent(String globalTransactionId, String transactionId,
                                                        String serviceName, String businessType, String businessId);

    @Async
    CompletableFuture<TransactionEventResponse> sendTransactionCommitEventAsync(String globalTransactionId,
                                                                                String transactionId,
                                                                                String serviceName,
                                                                                String businessType,
                                                                                String businessId);

    TransactionEventResponse sendTransactionRollbackEvent(String globalTransactionId, String transactionId,
                                                          String serviceName, String businessType, String businessId,
                                                          String errorMessage);

    @Async
    CompletableFuture<TransactionEventResponse> sendTransactionRollbackEventAsync(String globalTransactionId,
                                                                                  String transactionId,
                                                                                  String serviceName,
                                                                                  String businessType,
                                                                                  String businessId,
                                                                                  String errorMessage);

    TransactionEventResponse sendMessageSendEvent(String globalTransactionId, String transactionId,
                                                  String serviceName, String businessType, String businessId,
                                                  String messageType);

    @Async
    CompletableFuture<TransactionEventResponse> sendMessageSendEventAsync(String globalTransactionId,
                                                                          String transactionId,
                                                                          String serviceName,
                                                                          String businessType,
                                                                          String businessId,
                                                                          String messageType);

    TransactionEventResponse sendMessageConsumeEvent(String globalTransactionId, String transactionId,
                                                     String serviceName, String businessType, String businessId,
                                                     String messageType);

    @Async
    CompletableFuture<TransactionEventResponse> sendMessageConsumeEventAsync(String globalTransactionId,
                                                                             String transactionId,
                                                                             String serviceName,
                                                                             String businessType,
                                                                             String businessId,
                                                                             String messageType);

    TransactionEventResponse sendSagaExecuteEvent(String globalTransactionId, String transactionId,
                                                  String serviceName, String businessType, String businessId,
                                                  String sagaStep);

    @Async
    CompletableFuture<TransactionEventResponse> sendSagaExecuteEventAsync(String globalTransactionId,
                                                                          String transactionId,
                                                                          String serviceName,
                                                                          String businessType,
                                                                          String businessId,
                                                                          String sagaStep);

    TransactionEventResponse sendSagaCompensateEvent(String globalTransactionId, String transactionId,
                                                     String serviceName, String businessType, String businessId,
                                                     String sagaStep, String errorMessage);

    @Async
    CompletableFuture<TransactionEventResponse> sendSagaCompensateEventAsync(String globalTransactionId,
                                                                             String transactionId,
                                                                             String serviceName,
                                                                             String businessType,
                                                                             String businessId,
                                                                             String sagaStep,
                                                                             String errorMessage);

    TransactionEventResponse sendCustomTransactionEvent(TransactionEvent event);

    @Async
    CompletableFuture<TransactionEventResponse> sendCustomTransactionEventAsync(TransactionEvent event);

    List<TransactionEvent> getTransactionEventHistory(String transactionId);

    List<TransactionEvent> getTransactionEventsByTimeRange(String startTime, String endTime, String status);

    Object getTransactionEventStatistics(String timeRange);
}
