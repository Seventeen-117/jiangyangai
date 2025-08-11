package com.jiangyang.messages.service;

import com.jiangyang.messages.saga.MessageSagaStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息Saga服务类
 * 提供基于Seata的分布式事务消息处理服务
 */
@Slf4j
@Service
public class MessageSagaService {

    @Autowired
    private MessageSagaStateMachine messageSagaStateMachine;

    /**
     * 发送消息（使用Saga事务）
     * 
     * @param messageId 消息ID
     * @param content 消息内容
     */
    public void sendMessageWithSaga(String messageId, String content) {
        log.info("开始发送消息，使用Saga事务: messageId={}, content={}", messageId, content);
        
        try {
            messageSagaStateMachine.executeMessageSendSaga(messageId, content);
            log.info("消息发送成功: messageId={}", messageId);
        } catch (Exception e) {
            log.error("消息发送失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    /**
     * 消费消息（使用Saga事务）
     * 
     * @param messageId 消息ID
     * @param content 消息内容
     */
    public void consumeMessageWithSaga(String messageId, String content) {
        log.info("开始消费消息，使用Saga事务: messageId={}, content={}", messageId, content);
        
        try {
            messageSagaStateMachine.executeMessageConsumeSaga(messageId, content);
            log.info("消息消费成功: messageId={}", messageId);
        } catch (Exception e) {
            log.error("消息消费失败: messageId={}, error={}", messageId, e.getMessage(), e);
            throw new RuntimeException("消息消费失败", e);
        }
    }

    /**
     * 批量处理消息（使用Saga事务）
     * 
     * @param batchId 批次ID
     * @param messageIds 消息ID数组
     */
    public void processBatchMessagesWithSaga(String batchId, String[] messageIds) {
        log.info("开始批量处理消息，使用Saga事务: batchId={}, messageCount={}", batchId, messageIds.length);
        
        try {
            messageSagaStateMachine.executeBatchMessageSaga(batchId, messageIds);
            log.info("批量消息处理成功: batchId={}", batchId);
        } catch (Exception e) {
            log.error("批量消息处理失败: batchId={}, error={}", batchId, e.getMessage(), e);
            throw new RuntimeException("批量消息处理失败", e);
        }
    }

    /**
     * 处理事务消息（使用Saga事务）
     * 
     * @param transactionId 事务ID
     * @param messageId 消息ID
     * @param content 消息内容
     */
    public void processTransactionMessageWithSaga(String transactionId, String messageId, String content) {
        log.info("开始处理事务消息，使用Saga事务: transactionId={}, messageId={}, content={}", 
                transactionId, messageId, content);
        
        try {
            messageSagaStateMachine.executeTransactionMessageSaga(transactionId, messageId, content);
            log.info("事务消息处理成功: transactionId={}, messageId={}", transactionId, messageId);
        } catch (Exception e) {
            log.error("事务消息处理失败: transactionId={}, messageId={}, error={}", 
                    transactionId, messageId, e.getMessage(), e);
            throw new RuntimeException("事务消息处理失败", e);
        }
    }

    /**
     * 发送消息（同步方式）
     * 
     * @param messageId 消息ID
     * @param content 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessageSync(String messageId, String content) {
        log.info("同步发送消息: messageId={}, content={}", messageId, content);
        
        try {
            messageSagaStateMachine.sendMessage(messageId, content);
            messageSagaStateMachine.confirmMessage(messageId);
            log.info("同步消息发送成功: messageId={}", messageId);
            return true;
        } catch (Exception e) {
            log.error("同步消息发送失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 消费消息（同步方式）
     * 
     * @param messageId 消息ID
     * @param content 消息内容
     * @return 是否消费成功
     */
    public boolean consumeMessageSync(String messageId, String content) {
        log.info("同步消费消息: messageId={}, content={}", messageId, content);
        
        try {
            messageSagaStateMachine.receiveMessage(messageId, content);
            messageSagaStateMachine.processBusinessLogic(messageId, content);
            messageSagaStateMachine.confirmConsumption(messageId);
            log.info("同步消息消费成功: messageId={}", messageId);
            return true;
        } catch (Exception e) {
            log.error("同步消息消费失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return false;
        }
    }
}
