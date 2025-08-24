package com.jiangyang.messages.service;

import com.jiangyang.base.datasource.annotation.DataSource;

@DataSource("master")
public interface MessageSagaService {

    void sendMessageWithSaga(String messageId, String content, String messageType);

    void consumeMessageWithSaga(String messageId, String content);

    void processBatchMessagesWithSaga(String batchId, String[] messageIds);

    void processTransactionMessageWithSaga(String transactionId, String messageId, String content);

    boolean consumeMessageSync(String messageId, String content);
}
