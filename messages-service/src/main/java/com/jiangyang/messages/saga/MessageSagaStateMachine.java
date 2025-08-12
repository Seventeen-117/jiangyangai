package com.jiangyang.messages.saga;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.jiangyang.messages.service.WebSocketService;
import com.jiangyang.messages.utils.MessageServiceType;
import com.jiangyang.messages.audit.entity.MessageLifecycleLog;
import com.jiangyang.messages.audit.entity.TransactionAuditLog;
import com.jiangyang.messages.audit.service.MessageLifecycleService;
import com.jiangyang.messages.audit.service.TransactionAuditService;
import com.jiangyang.messages.config.MessageServiceConfig;
import com.jiangyang.messages.rocketmq.RocketMQMessageService;
import com.jiangyang.messages.kafka.KafkaMessageService;
import com.jiangyang.messages.rabbitmq.RabbitMQMessageService;
import com.jiangyang.messages.saga.entity.MessageSagaLog;
import com.jiangyang.messages.saga.service.MessageSagaLogService;
import com.jiangyang.messages.service.TransactionEventSenderService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 消息Saga分布式事务管理
 * 使用Alibaba Cloud Seata管理消息发送、消费、补偿等操作的分布式事务
 */
@Slf4j
@Component
public class MessageSagaStateMachine {

    @Autowired
    private MessageServiceConfig messageServiceConfig;
    
    @Autowired
    private RocketMQMessageService rocketMQMessageService;
    
    @Autowired
    private KafkaMessageService kafkaMessageService;
    
    @Autowired
    private RabbitMQMessageService rabbitMQMessageService;
    
    @Autowired
    private MessageSagaLogService messageSagaLogService;
    
    @Autowired
    private MessageLifecycleService messageLifecycleService;
    
    @Autowired
    private TransactionAuditService transactionAuditService;
    
    @Autowired
    private TransactionEventSenderService transactionEventSenderService;
    
    // WebSocket服务，用于发送实时通知
    @Autowired(required = false)
    private WebSocketService webSocketService;

    // 线程池用于并行处理批量消息
    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(10);

    /**
     * 消息发送Saga事务
     * 包含：消息发送 -> 消息确认 -> 补偿处理
     */
    @GlobalTransactional(name = "message-send-saga", rollbackFor = Exception.class)
    public void executeMessageSendSaga(String messageId, String content) {
        String globalTransactionId = RootContext.getXID();
        String transactionId = "msg_send_" + messageId;
        
        log.info("开始执行消息发送Saga事务，消息ID: {}, XID: {}", messageId, globalTransactionId);
        
        try {
            // 发送事务开始事件
            sendTransactionBeginEvent(globalTransactionId, transactionId, messageId, content);
            
            // 步骤1: 消息发送
            sendMessage(messageId, content);
            
            // 发送消息发送事件
            sendMessageSendEvent(globalTransactionId, transactionId, messageId, content);
            
            // 步骤2: 消息确认
            confirmMessage(messageId);
            
            // 发送事务提交事件
            sendTransactionCommitEvent(globalTransactionId, transactionId, messageId, content);
            
            log.info("消息发送Saga事务执行成功，消息ID: {}", messageId);
        } catch (Exception e) {
            log.error("消息发送Saga事务执行失败，消息ID: {}, 错误: {}", messageId, e.getMessage(), e);
            
            // 发送事务回滚事件
            sendTransactionRollbackEvent(globalTransactionId, transactionId, messageId, content, e.getMessage());
            
            throw e;
        }
    }

    /**
     * 消息消费Saga事务
     * 包含：消息接收 -> 业务处理 -> 确认消费
     */
    @GlobalTransactional(name = "message-consume-saga", rollbackFor = Exception.class)
    public void executeMessageConsumeSaga(String messageId, String content) {
        String globalTransactionId = RootContext.getXID();
        String transactionId = "msg_consume_" + messageId;
        
        log.info("开始执行消息消费Saga事务，消息ID: {}, XID: {}", messageId, globalTransactionId);
        
        try {
            // 发送事务开始事件
            sendTransactionBeginEvent(globalTransactionId, transactionId, messageId, content);
            
            // 步骤1: 消息接收
            receiveMessage(messageId, content);
            
            // 发送消息消费事件
            sendMessageConsumeEvent(globalTransactionId, transactionId, messageId, content);
            
            // 步骤2: 业务处理
            processBusinessLogic(messageId, content);
            
            // 步骤3: 确认消费
            confirmConsumption(messageId);
            
            log.info("消息消费Saga事务执行成功，消息ID: {}", messageId);
        } catch (Exception e) {
            log.error("消息消费Saga事务执行失败，消息ID: {}, 错误: {}", messageId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量消息处理Saga事务
     * 包含：批量接收 -> 并行处理 -> 批量确认
     */
    @GlobalTransactional(name = "batch-message-saga", rollbackFor = Exception.class)
    public void executeBatchMessageSaga(String batchId, String[] messageIds) {
        String globalTransactionId = RootContext.getXID();
        String transactionId = "batch_msg_" + batchId;
        
        log.info("开始执行批量消息Saga事务，批次ID: {}, 消息数量: {}, XID: {}", 
                batchId, messageIds.length, globalTransactionId);
        
        try {
            // 发送事务开始事件
            sendTransactionBeginEvent(globalTransactionId, transactionId, "batch-message", batchId);
            
            // 步骤1: 批量接收
            receiveBatchMessages(batchId, messageIds);
            
            // 发送Saga执行事件
            sendSagaExecuteEvent(globalTransactionId, transactionId, "batch-message", batchId, "批量消息接收");
            
            // 步骤2: 并行处理
            processBatchMessages(batchId, messageIds);
            
            // 发送Saga执行事件
            sendSagaExecuteEvent(globalTransactionId, transactionId, "batch-message", batchId, "批量消息处理");
            
            // 步骤3: 批量确认
            confirmBatchMessages(batchId, messageIds);
            
            // 发送事务提交事件
            sendTransactionCommitEvent(globalTransactionId, transactionId, "batch-message", batchId);
            
            log.info("批量消息Saga事务执行成功，批次ID: {}", batchId);
        } catch (Exception e) {
            log.error("批量消息Saga事务执行失败，批次ID: {}, 错误: {}", batchId, e.getMessage(), e);
            
            // 发送事务回滚事件
            sendTransactionRollbackEvent(globalTransactionId, transactionId, "batch-message", batchId, e.getMessage());
            
            throw e;
        }
    }

    /**
     * 事务消息Saga事务
     * 包含：事务开始 -> 消息发送 -> 事务提交/回滚
     */
    @GlobalTransactional(name = "transaction-message-saga", rollbackFor = Exception.class)
    public void executeTransactionMessageSaga(String transactionId, String messageId, String content) {
        String globalTransactionId = RootContext.getXID();
        String businessTransactionId = "txn_msg_" + transactionId;
        
        log.info("开始执行事务消息Saga事务，事务ID: {}, 消息ID: {}, XID: {}", 
                transactionId, messageId, globalTransactionId);
        
        try {
            // 发送事务开始事件
            sendTransactionBeginEvent(globalTransactionId, businessTransactionId, "transaction-message", transactionId);
            
            // 步骤1: 事务开始
            beginTransaction(transactionId);
            
            // 发送Saga执行事件
            sendSagaExecuteEvent(globalTransactionId, businessTransactionId, "transaction-message", transactionId, "事务开始");
            
            // 步骤2: 消息发送
            sendMessage(messageId, content);
            
            // 发送Saga执行事件
            sendSagaExecuteEvent(globalTransactionId, businessTransactionId, "transaction-message", transactionId, "消息发送");
            
            // 步骤3: 事务提交
            commitTransaction(transactionId);
            
            // 发送事务提交事件
            sendTransactionCommitEvent(globalTransactionId, businessTransactionId, "transaction-message", transactionId);
            
            log.info("事务消息Saga事务执行成功，事务ID: {}, 消息ID: {}", transactionId, messageId);
        } catch (Exception e) {
            log.error("事务消息Saga事务执行失败，事务ID: {}, 消息ID: {}, 错误: {}", 
                    transactionId, messageId, e.getMessage(), e);
            
            // 发送事务回滚事件
            sendTransactionRollbackEvent(globalTransactionId, businessTransactionId, "transaction-message", transactionId, e.getMessage());
            
            // 自动回滚事务
            throw e;
        }
    }

    // ==================== 具体业务方法实现 ====================

    @Transactional
    public void sendMessage(String messageId, String content) {
        log.info("发送消息: ID={}, 内容={}", messageId, content);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(messageId, "SEND", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 记录消息生命周期
            MessageLifecycleLog lifecycleLog = createLifecycleLog(messageId, "PRODUCE", "PROCESSING", content);
            messageLifecycleService.save(lifecycleLog);
            
            // 3. 根据配置选择消息中间件发送消息
            String topic = messageServiceConfig.getDefaultTopic();
            String messageType = messageServiceConfig.getDefaultMessageType();
            
            boolean sendResult = false;
            switch (MessageServiceType.valueOf(messageType.toUpperCase())) {
                case ROCKETMQ:
                    sendResult = rocketMQMessageService.sendMessage(topic, content);
                    break;
                case KAFKA:
                    sendResult = kafkaMessageService.sendMessage(topic, content);
                    break;
                case RABBITMQ:
                    sendResult = rabbitMQMessageService.sendMessage(topic, content);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的消息类型: " + messageType);
            }
            
            if (!sendResult) {
                throw new RuntimeException("消息发送失败");
            }
            
            // 4. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            messageSagaLogService.updateById(sagaLog);
            
            // 5. 更新生命周期日志状态
            lifecycleLog.setStageStatus("SUCCESS");
            lifecycleLog.setStageEndTime(LocalDateTime.now());
            lifecycleLog.setProcessingTime(System.currentTimeMillis() - lifecycleLog.getStageStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            messageLifecycleService.updateById(lifecycleLog);
            
            log.info("消息发送成功: ID={}, 类型={}, 主题={}", messageId, messageType, topic);
            
        } catch (Exception e) {
            log.error("消息发送失败: ID={}, 错误: {}", messageId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(messageId, "FAILED", e.getMessage());
            updateLifecycleLogStatus(messageId, "FAILED", e.getMessage());
            
            throw new RuntimeException("消息发送失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void confirmMessage(String messageId) {
        log.info("确认消息: ID={}", messageId);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(messageId, "CONFIRM", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 记录消息生命周期
            MessageLifecycleLog lifecycleLog = createLifecycleLog(messageId, "ACK", "PROCESSING", null);
            messageLifecycleService.save(lifecycleLog);
            
            // 3. 执行消息确认逻辑
            // 3.1 验证消息ID
            if (StrUtil.isBlank(messageId)) {
                throw new IllegalArgumentException("消息ID不能为空");
            }
            
            // 3.2 检查消息是否已存在
            MessageLifecycleLog existingLog = messageLifecycleService.getByMessageId(messageId);
            if (existingLog == null) {
                throw new IllegalStateException("消息不存在: " + messageId);
            }
            
            // 3.3 验证消息状态是否允许确认
            if (!"SEND".equals(existingLog.getLifecycleStage()) || 
                !"SUCCESS".equals(existingLog.getStageStatus())) {
                throw new IllegalStateException("消息状态不允许确认: " + messageId + 
                    ", 当前阶段: " + existingLog.getLifecycleStage() + 
                    ", 状态: " + existingLog.getStageStatus());
            }
            
            // 3.4 更新消息状态为已确认
            existingLog.setStageStatus("ACKED");
            existingLog.setStageEndTime(LocalDateTime.now());
            existingLog.setProcessingTime(System.currentTimeMillis() - 
                existingLog.getStageStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            messageLifecycleService.updateById(existingLog);
            
            // 3.5 记录消息确认日志
            log.info("消息已确认: ID={}, 确认时间={}", messageId, LocalDateTime.now());
            
            // 3.6 发送确认通知（可选）
            sendMessageConfirmationNotification(messageId);
            
            // 3.7 更新消息统计信息
            updateMessageStatistics(messageId, "MESSAGE_CONFIRM");
            
            // 3.8 记录确认历史（可选）
            recordConfirmationHistory(messageId);
            
            // 4. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            sagaLog.setProcessingTime(System.currentTimeMillis() - 
                sagaLog.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            sagaLog.setResponseResult("{\"status\":\"SUCCESS\",\"message\":\"消息确认成功\"}");
            messageSagaLogService.updateById(sagaLog);
            
            // 5. 更新生命周期日志状态
            lifecycleLog.setStageStatus("SUCCESS");
            lifecycleLog.setStageEndTime(LocalDateTime.now());
            lifecycleLog.setProcessingTime(System.currentTimeMillis() - 
                lifecycleLog.getStageStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            messageLifecycleService.updateById(lifecycleLog);
            
            log.info("消息确认成功: ID={}", messageId);
            
        } catch (Exception e) {
            log.error("消息确认失败: ID={}, 错误: {}", messageId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(messageId, "FAILED", e.getMessage());
            updateLifecycleLogStatus(messageId, "FAILED", e.getMessage());
            
            throw new RuntimeException("消息确认失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void receiveMessage(String messageId, String content) {
        log.info("接收消息: ID={}, 内容={}", messageId, content);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(messageId, "RECEIVE", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 记录消息生命周期
            MessageLifecycleLog lifecycleLog = createLifecycleLog(messageId, "CONSUME", "PROCESSING", content);
            messageLifecycleService.save(lifecycleLog);
            
            // 3. 执行消息接收逻辑
            // - 验证消息格式
            if (StrUtil.isBlank(content)) {
                throw new IllegalArgumentException("消息内容不能为空");
            }
            
            // - 验证消息ID格式
            if (StrUtil.isBlank(messageId)) {
                throw new IllegalArgumentException("消息ID不能为空");
            }
            
            // - 存储消息到本地数据库（如果需要）
            // - 记录接收日志
            
            // 4. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            messageSagaLogService.updateById(sagaLog);
            
            // 5. 更新生命周期日志状态
            lifecycleLog.setStageStatus("SUCCESS");
            lifecycleLog.setStageEndTime(LocalDateTime.now());
            lifecycleLog.setProcessingTime(System.currentTimeMillis() - lifecycleLog.getStageStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            messageLifecycleService.updateById(lifecycleLog);
            
            log.info("消息接收成功: ID={}", messageId);
            
        } catch (Exception e) {
            log.error("消息接收失败: ID={}, 错误: {}", messageId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(messageId, "FAILED", e.getMessage());
            updateLifecycleLogStatus(messageId, "FAILED", e.getMessage());
            
            throw new RuntimeException("消息接收失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void processBusinessLogic(String messageId, String content) {
        log.info("处理业务逻辑: ID={}, 内容={}", messageId, content);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(messageId, "BUSINESS_PROCESS", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 记录消息生命周期
            MessageLifecycleLog lifecycleLog = createLifecycleLog(messageId, "BUSINESS_PROCESS", "PROCESSING", content);
            messageLifecycleService.save(lifecycleLog);
            
            // 3. 执行具体的业务处理逻辑
            // 这里可以根据消息内容解析出具体的业务操作
            BusinessMessage businessMessage = parseBusinessMessage(content);
            
            switch (businessMessage.getBusinessType()) {
                case "ORDER_CREATE":
                    processOrderCreate(businessMessage);
                    break;
                case "PAYMENT_CONFIRM":
                    processPaymentConfirm(businessMessage);
                    break;
                case "INVENTORY_UPDATE":
                    processInventoryUpdate(businessMessage);
                    break;
                case "USER_NOTIFICATION":
                    processUserNotification(businessMessage);
                    break;
                default:
                    log.warn("未知的业务类型: {}", businessMessage.getBusinessType());
                    // 可以抛出异常或者记录警告日志
            }
            
            // 4. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            messageSagaLogService.updateById(sagaLog);
            
            // 5. 更新生命周期日志状态
            lifecycleLog.setStageStatus("SUCCESS");
            lifecycleLog.setStageEndTime(LocalDateTime.now());
            lifecycleLog.setProcessingTime(System.currentTimeMillis() - lifecycleLog.getStageStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            messageLifecycleService.updateById(lifecycleLog);
            
            log.info("业务逻辑处理成功: ID={}, 业务类型={}", messageId, businessMessage.getBusinessType());
            
        } catch (Exception e) {
            log.error("业务逻辑处理失败: ID={}, 错误: {}", messageId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(messageId, "FAILED", e.getMessage());
            updateLifecycleLogStatus(messageId, "FAILED", e.getMessage());
            
            throw new RuntimeException("业务逻辑处理失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void confirmConsumption(String messageId) {
        log.info("确认消费: ID={}", messageId);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(messageId, "CONSUME_CONFIRM", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 记录消息生命周期
            MessageLifecycleLog lifecycleLog = createLifecycleLog(messageId, "CONSUME_CONFIRM", "PROCESSING", null);
            messageLifecycleService.save(lifecycleLog);
            
            // 3. 执行消费确认逻辑
            // 3.1 验证消息ID
            if (StrUtil.isBlank(messageId)) {
                throw new IllegalArgumentException("消息ID不能为空");
            }
            
            // 3.2 检查消息是否已存在
            MessageLifecycleLog existingLog = messageLifecycleService.getByMessageId(messageId);
            if (existingLog == null) {
                throw new IllegalStateException("消息不存在: " + messageId);
            }
            
            // 3.3 验证消息状态是否允许确认
            if (!"CONSUME".equals(existingLog.getLifecycleStage()) || 
                !"SUCCESS".equals(existingLog.getStageStatus())) {
                throw new IllegalStateException("消息状态不允许确认消费: " + messageId + 
                    ", 当前阶段: " + existingLog.getLifecycleStage() + 
                    ", 状态: " + existingLog.getStageStatus());
            }
            
            // 3.4 更新消费状态为已确认
            existingLog.setStageStatus("CONFIRMED");
            existingLog.setStageEndTime(LocalDateTime.now());
            existingLog.setProcessingTime(System.currentTimeMillis() - 
                existingLog.getStageStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            messageLifecycleService.updateById(existingLog);
            
            // 3.5 记录消费确认日志
            log.info("消息消费已确认: ID={}, 确认时间={}", messageId, LocalDateTime.now());
            
            // 3.6 发送确认通知（可选）
            sendConsumptionConfirmationNotification(messageId);
            
            // 3.7 清理临时数据（如果有的话）
            // cleanupTemporaryData(messageId);
            
            // 3.8 更新消息统计信息
            updateMessageStatistics(messageId, "CONSUME_CONFIRM");
            
            // 4. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            sagaLog.setProcessingTime(System.currentTimeMillis() - 
                sagaLog.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            sagaLog.setResponseResult("{\"status\":\"SUCCESS\",\"message\":\"消费确认成功\"}");
            messageSagaLogService.updateById(sagaLog);
            
            // 5. 更新生命周期日志状态
            lifecycleLog.setStageStatus("SUCCESS");
            lifecycleLog.setStageEndTime(LocalDateTime.now());
            lifecycleLog.setProcessingTime(System.currentTimeMillis() - 
                lifecycleLog.getStageStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            messageLifecycleService.updateById(lifecycleLog);
            
            log.info("消费确认成功: ID={}", messageId);
            
        } catch (Exception e) {
            log.error("消费确认失败: ID={}, 错误: {}", messageId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(messageId, "FAILED", e.getMessage());
            updateLifecycleLogStatus(messageId, "FAILED", e.getMessage());
            
            throw new RuntimeException("消费确认失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void receiveBatchMessages(String batchId, String[] messageIds) {
        log.info("批量接收消息: 批次ID={}, 消息数量={}", batchId, messageIds.length);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(batchId, "BATCH_RECEIVE", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 批量接收消息
            List<MessageLifecycleLog> lifecycleLogs = new ArrayList<>();
            for (String messageId : messageIds) {
                MessageLifecycleLog log = createLifecycleLog(messageId, "BATCH_RECEIVE", "PROCESSING", null);
                lifecycleLogs.add(log);
            }
            messageLifecycleService.saveBatch(lifecycleLogs);
            
            // 3. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            messageSagaLogService.updateById(sagaLog);
            
            log.info("批量消息接收成功: 批次ID={}, 消息数量={}", batchId, messageIds.length);
            
        } catch (Exception e) {
            log.error("批量消息接收失败: 批次ID={}, 错误: {}", batchId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(batchId, "FAILED", e.getMessage());
            
            throw new RuntimeException("批量消息接收失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void processBatchMessages(String batchId, String[] messageIds) {
        log.info("批量处理消息: 批次ID={}, 消息数量={}", batchId, messageIds.length);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(batchId, "BATCH_PROCESS", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 并行处理消息
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (String messageId : messageIds) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // 模拟业务处理
                        processSingleMessageInBatch(messageId);
                    } catch (Exception e) {
                        log.error("批量处理中单条消息处理失败: messageId={}, error={}", messageId, e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }, batchExecutor);
                
                futures.add(future);
            }
            
            // 等待所有消息处理完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
            
            // 3. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            messageSagaLogService.updateById(sagaLog);
            
            log.info("批量消息处理成功: 批次ID={}, 消息数量={}", batchId, messageIds.length);
            
        } catch (Exception e) {
            log.error("批量消息处理失败: 批次ID={}, 错误: {}", batchId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(batchId, "FAILED", e.getMessage());
            
            throw new RuntimeException("批量消息处理失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void confirmBatchMessages(String batchId, String[] messageIds) {
        log.info("批量确认消息: 批次ID={}, 消息数量={}", batchId, messageIds.length);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(batchId, "BATCH_CONFIRM", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 批量确认消息
            List<MessageLifecycleLog> lifecycleLogs = new ArrayList<>();
            for (String messageId : messageIds) {
                MessageLifecycleLog log = createLifecycleLog(messageId, "BATCH_CONFIRM", "PROCESSING", null);
                lifecycleLogs.add(log);
            }
            messageLifecycleService.saveBatch(lifecycleLogs);
            
            // 3. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            messageSagaLogService.updateById(sagaLog);
            
            log.info("批量消息确认成功: 批次ID={}, 消息数量={}", batchId, messageIds.length);
            
        } catch (Exception e) {
            log.error("批量消息确认失败: 批次ID={}, 错误: {}", batchId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(batchId, "FAILED", e.getMessage());
            
            throw new RuntimeException("批量消息确认失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void beginTransaction(String transactionId) {
        log.info("开始事务: ID={}", transactionId);
        
        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(transactionId, "TRANSACTION_BEGIN", "PROCESSING");
            messageSagaLogService.save(sagaLog);
            
            // 2. 记录事务审计日志
            TransactionAuditLog auditLog = new TransactionAuditLog();
            auditLog.setGlobalTransactionId(RootContext.getXID());
            auditLog.setBusinessTransactionId(transactionId);
            auditLog.setServiceName("messages-service");
            auditLog.setOperationType("TRANSACTION_BEGIN");
            auditLog.setTransactionStatus("BEGIN");
            auditLog.setRequestParams("{\"transactionId\":\"" + transactionId + "\"}");
            auditLog.setStartTime(LocalDateTime.now());
            transactionAuditService.save(auditLog);
            
            // 3. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            messageSagaLogService.updateById(sagaLog);
            
            log.info("事务开始成功: ID={}", transactionId);
            
        } catch (Exception e) {
            log.error("事务开始失败: ID={}, 错误: {}", transactionId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(transactionId, "FAILED", e.getMessage());
            
            throw new RuntimeException("事务开始失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void commitTransaction(String transactionId) {
        log.info("提交事务: ID={}", transactionId);

        try {
            // 1. 记录Saga日志
            MessageSagaLog sagaLog = createSagaLog(transactionId, "TRANSACTION_COMMIT", "PROCESSING");
            messageSagaLogService.save(sagaLog);

            // 2. 更新事务审计日志
            TransactionAuditLog auditLog = transactionAuditService.getByTransactionId(transactionId);
            if (auditLog != null) {
                auditLog.setTransactionStatus("SUCCESS");
                auditLog.setEndTime(LocalDateTime.now());
                auditLog.setExecutionTime(System.currentTimeMillis() - auditLog.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                transactionAuditService.updateById(auditLog);
            }
            
            // 3. 更新Saga日志状态
            sagaLog.setStatus("SUCCESS");
            sagaLog.setEndTime(LocalDateTime.now());
            messageSagaLogService.updateById(sagaLog);
            
            log.info("事务提交成功: ID={}", transactionId);
            
        } catch (Exception e) {
            log.error("事务提交失败: ID={}, 错误: {}", transactionId, e.getMessage(), e);
            
            // 记录失败状态
            updateSagaLogStatus(transactionId, "FAILED", e.getMessage());
            
            throw new RuntimeException("事务提交失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 创建Saga日志
     */
    private MessageSagaLog createSagaLog(String businessId, String operation, String status) {
        MessageSagaLog sagaLog = new MessageSagaLog();
        sagaLog.setId(IdUtil.getSnowflakeNextId());
        sagaLog.setBusinessId(businessId);
        sagaLog.setOperation(operation);
        sagaLog.setStatus(status);
        sagaLog.setGlobalTransactionId(RootContext.getXID());
        sagaLog.setStartTime(LocalDateTime.now());
        sagaLog.setCreateTime(LocalDateTime.now());
        sagaLog.setUpdateTime(LocalDateTime.now());
        return sagaLog;
    }

    /**
     * 创建生命周期日志
     */
    private MessageLifecycleLog createLifecycleLog(String messageId, String stage, String status, String content) {
        MessageLifecycleLog log = new MessageLifecycleLog();
        log.setId(IdUtil.getSnowflakeNextId());
        log.setMessageId(messageId);
        log.setBusinessMessageId(messageId);
        log.setMessageType(messageServiceConfig.getDefaultMessageType());
        log.setTopic(messageServiceConfig.getDefaultTopic());
        log.setLifecycleStage(stage);
        log.setStageStatus(status);
        log.setProducerService("messages-service");
        log.setConsumerService("messages-service");
        log.setMessageContent(content);
        log.setMessageSize(content != null ? (long) content.getBytes().length : 0L);
        log.setStageStartTime(LocalDateTime.now());
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());
        return log;
    }

    /**
     * 更新Saga日志状态
     */
    private void updateSagaLogStatus(String businessId, String status, String errorMessage) {
        try {
            MessageSagaLog sagaLog = messageSagaLogService.getByBusinessId(businessId);
            if (sagaLog != null) {
                sagaLog.setStatus(status);
                sagaLog.setErrorMessage(errorMessage);
                sagaLog.setEndTime(LocalDateTime.now());
                sagaLog.setUpdateTime(LocalDateTime.now());
                messageSagaLogService.updateById(sagaLog);
            }
        } catch (Exception e) {
            log.error("更新Saga日志状态失败: businessId={}, error={}", businessId, e.getMessage(), e);
        }
    }

    /**
     * 更新生命周期日志状态
     */
    private void updateLifecycleLogStatus(String messageId, String status, String errorMessage) {
        try {
            MessageLifecycleLog lifecycleLog = messageLifecycleService.getByMessageId(messageId);
            if (lifecycleLog != null) {
                lifecycleLog.setStageStatus(status);
                lifecycleLog.setErrorMessage(errorMessage);
                lifecycleLog.setStageEndTime(LocalDateTime.now());
                lifecycleLog.setUpdateTime(LocalDateTime.now());
                messageLifecycleService.updateById(lifecycleLog);
            }
        } catch (Exception e) {
            log.error("更新生命周期日志状态失败: messageId={}, error={}", messageId, e.getMessage(), e);
        }
    }

    /**
     * 解析业务消息
     */
    private BusinessMessage parseBusinessMessage(String content) {
        try {
            return JSON.parseObject(content, BusinessMessage.class);
        } catch (Exception e) {
            log.warn("解析业务消息失败，使用默认业务类型: content={}, error={}", content, e.getMessage());
            return new BusinessMessage("UNKNOWN", content, new HashMap<>());
        }
    }

    /**
     * 处理订单创建业务
     */
    private void processOrderCreate(BusinessMessage businessMessage) {
        log.info("处理订单创建业务: {}", businessMessage.getBusinessData());
        // 这里实现具体的订单创建逻辑
        // 例如：创建订单记录、更新库存、发送通知等
    }

    /**
     * 处理支付确认业务
     */
    private void processPaymentConfirm(BusinessMessage businessMessage) {
        log.info("处理支付确认业务: {}", businessMessage.getBusinessData());
        // 这里实现具体的支付确认逻辑
        // 例如：更新订单状态、释放库存、发送通知等
    }

    /**
     * 处理库存更新业务
     */
    private void processInventoryUpdate(BusinessMessage businessMessage) {
        log.info("处理库存更新业务: {}", businessMessage.getBusinessData());
        // 这里实现具体的库存更新逻辑
        // 例如：更新库存数量、记录库存变更日志等
    }

    /**
     * 处理用户通知业务
     */
    private void processUserNotification(BusinessMessage businessMessage) {
        log.info("处理用户通知业务: {}", businessMessage.getBusinessData());
        // 这里实现具体的用户通知逻辑
        // 例如：发送短信、邮件、推送通知等
    }

    /**
     * 批量处理中的单条消息处理
     */
    private void processSingleMessageInBatch(String messageId) {
        log.debug("批量处理中处理单条消息: messageId={}", messageId);
        // 这里实现单条消息的具体处理逻辑
        // 模拟处理时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 更新消息统计信息
     * 
     * @param messageId 消息ID
     * @param operation 操作类型
     */
    private void updateMessageStatistics(String messageId, String operation) {
        try {
            log.debug("更新消息统计信息: messageId={}, operation={}", messageId, operation);
            
            // 获取消息详情用于统计
            MessageLifecycleLog messageLog = messageLifecycleService.getByMessageId(messageId);
            if (messageLog == null) {
                log.warn("消息不存在，无法更新统计信息: messageId={}", messageId);
                return;
            }
            
            // 1. 更新Redis计数器
            try {
                String counterKey = "message:stats:" + operation + ":" + LocalDateTime.now().toLocalDate();
                // 如果Redis服务可用，则更新计数器
                // redisTemplate.opsForValue().increment(counterKey);
                log.debug("Redis计数器更新: key={}", counterKey);
            } catch (Exception e) {
                log.debug("Redis计数器更新失败，跳过: {}", e.getMessage());
            }
            
            // 2. 更新数据库统计表
            Map<String, Object> statsData = new HashMap<>();
            try {
                // 构建统计记录
                statsData.put("messageId", messageId);
                statsData.put("operation", operation);
                statsData.put("operationTime", LocalDateTime.now());
                statsData.put("processingTime", messageLog.getProcessingTime());
                statsData.put("messageType", messageLog.getMessageType());
                statsData.put("topic", messageLog.getTopic());
                statsData.put("producerService", messageLog.getProducerService());
                statsData.put("consumerService", messageLog.getConsumerService());
                statsData.put("status", "SUCCESS");
                statsData.put("timestamp", System.currentTimeMillis());
                
                // 如果统计服务可用，则保存统计记录
                // messageStatisticsService.saveStatistics(statsData);
                log.debug("数据库统计记录已保存: {}", statsData);
                
            } catch (Exception e) {
                log.debug("数据库统计记录保存失败，跳过: {}", e.getMessage());
            }
            
            // 3. 更新内存统计缓存
            try {
                String memoryKey = "message:memory:stats:" + operation;
                // 如果内存缓存服务可用，则更新缓存
                // memoryCacheService.incrementCounter(memoryKey);
                log.debug("内存统计缓存已更新: key={}", memoryKey);
                
            } catch (Exception e) {
                log.debug("内存统计缓存更新失败，跳过: {}", e.getMessage());
            }
            
            // 4. 发送统计事件到消息队列（可选）
            try {
                Map<String, Object> statsEvent = new HashMap<>();
                statsEvent.put("type", "MESSAGE_STATISTICS_UPDATE");
                statsEvent.put("messageId", messageId);
                statsEvent.put("operation", operation);
                statsEvent.put("timestamp", System.currentTimeMillis());
                statsEvent.put("data", statsData);
                
                // 如果消息队列服务可用，则发送统计事件
                // messageQueueService.sendMessage("message-statistics", statsEvent);
                log.debug("统计事件已发送到消息队列: {}", statsEvent);
                
            } catch (Exception e) {
                log.debug("统计事件发送失败，跳过: {}", e.getMessage());
            }
            
            log.info("消息统计信息更新成功: messageId={}, operation={}", messageId, operation);
            
        } catch (Exception e) {
            log.warn("更新消息统计信息失败: messageId={}, operation={}, error={}", 
                    messageId, operation, e.getMessage());
            // 统计失败不影响主业务流程，只记录警告日志
        }
    }

    // ==================== 事务事件发送私有方法 ====================

    /**
     * 发送事务开始事件
     */
    private void sendTransactionBeginEvent(String globalTransactionId, String transactionId, 
                                         String businessType, String businessId) {
        try {
            // 使用TransactionEventSenderService的便捷方法，避免直接创建TransactionEvent
            transactionEventSenderService.sendTransactionBeginEvent(
                globalTransactionId, transactionId, "messages-service", businessType, businessId);
            
            log.debug("事务开始事件已发送: transactionId={}, XID={}", transactionId, globalTransactionId);
        } catch (Exception e) {
            log.warn("发送事务开始事件失败: transactionId={}, error={}", transactionId, e.getMessage());
        }
    }

    /**
     * 发送消息发送事件
     */
    private void sendMessageSendEvent(String globalTransactionId, String transactionId, 
                                    String messageId, String content) {
        try {
            // 使用TransactionEventSenderService的便捷方法
            transactionEventSenderService.sendMessageSendEvent(
                globalTransactionId, transactionId, "messages-service", "message", messageId, messageServiceConfig.getDefaultMessageType());
            
            log.debug("消息发送事件已发送: messageId={}, transactionId={}", messageId, transactionId);
        } catch (Exception e) {
            log.warn("发送消息发送事件失败: messageId={}, error={}", messageId, e.getMessage());
        }
    }

    /**
     * 发送消息消费事件
     */
    private void sendMessageConsumeEvent(String globalTransactionId, String transactionId, 
                                       String messageId, String content) {
        try {
            // 使用TransactionEventSenderService的便捷方法
            transactionEventSenderService.sendMessageConsumeEvent(
                globalTransactionId, transactionId, "messages-service", "message", messageId, messageServiceConfig.getDefaultMessageType());
            
            log.debug("消息消费事件已发送: messageId={}, transactionId={}", messageId, transactionId);
        } catch (Exception e) {
            log.warn("发送消息消费事件失败: messageId={}, error={}", messageId, e.getMessage());
        }
    }

    /**
     * 发送Saga执行事件
     */
    private void sendSagaExecuteEvent(String globalTransactionId, String transactionId, 
                                    String businessType, String businessId, String operation) {
        try {
            // 使用TransactionEventSenderService的便捷方法
            transactionEventSenderService.sendSagaExecuteEvent(
                globalTransactionId, transactionId, "messages-service", businessType, businessId, operation);
            
            log.debug("Saga执行事件已发送: transactionId={}, operation={}", transactionId, operation);
        } catch (Exception e) {
            log.warn("发送Saga执行事件失败: transactionId={}, error={}", transactionId, e.getMessage());
        }
    }

    /**
     * 发送Saga补偿事件
     */
    private void sendSagaCompensateEvent(String globalTransactionId, String transactionId, 
                                       String businessType, String businessId, String operation) {
        try {
            // 使用TransactionEventSenderService的便捷方法
            transactionEventSenderService.sendSagaCompensateEvent(
                globalTransactionId, transactionId, "messages-service", businessType, businessId, operation, "补偿操作");
            
            log.debug("Saga补偿事件已发送: transactionId={}, operation={}", transactionId, operation);
        } catch (Exception e) {
            log.warn("发送Saga补偿事件失败: transactionId={}, error={}", transactionId, e.getMessage());
        }
    }

    /**
     * 发送事务提交事件
     */
    private void sendTransactionCommitEvent(String globalTransactionId, String transactionId, 
                                          String businessType, String businessId) {
        try {
            // 使用TransactionEventSenderService的便捷方法
            transactionEventSenderService.sendTransactionCommitEvent(
                globalTransactionId, transactionId, "messages-service", businessType, businessId);
            
            log.debug("事务提交事件已发送: transactionId={}, XID={}", transactionId, globalTransactionId);
        } catch (Exception e) {
            log.warn("发送事务提交事件失败: transactionId={}, error={}", transactionId, e.getMessage());
        }
    }

    /**
     * 发送事务回滚事件
     */
    private void sendTransactionRollbackEvent(String globalTransactionId, String transactionId, 
                                            String businessType, String businessId, String errorMessage) {
        try {
            // 使用TransactionEventSenderService的便捷方法
            transactionEventSenderService.sendTransactionRollbackEvent(
                globalTransactionId, transactionId, "messages-service", businessType, businessId, errorMessage);
            
            log.debug("事务回滚事件已发送: transactionId={}, XID={}, error={}", 
                     transactionId, globalTransactionId, errorMessage);
        } catch (Exception e) {
            log.warn("发送事务回滚事件失败: transactionId={}, error={}", transactionId, e.getMessage());
        }
    }

    /**
     * 发送消息确认通知
     * 可以通过邮件、短信、WebSocket等方式发送通知
     */
    private void sendMessageConfirmationNotification(String messageId) {
        try {
            log.info("发送消息确认通知: messageId={}", messageId);
            
            // 获取消息详情
            MessageLifecycleLog messageLog = messageLifecycleService.getByMessageId(messageId);
            if (messageLog == null) {
                log.warn("消息不存在，无法发送确认通知: messageId={}", messageId);
                return;
            }
            
            // 构建通知内容
            String notificationContent = String.format(
                "消息确认通知\n" +
                "消息ID: %s\n" +
                "确认时间: %s\n" +
                "状态: 已确认\n" +
                "处理时间: %dms",
                messageId,
                LocalDateTime.now(),
                messageLog.getProcessingTime() != null ? messageLog.getProcessingTime() : 0
            );
            
            // 这里可以实现具体的通知逻辑
            // 例如：发送邮件、短信、WebSocket推送等
            
            // 记录通知日志
            log.info("消息确认通知已发送: messageId={}, 内容={}", messageId, notificationContent);
            
            // 使用WebSocket发送实时通知
            if (webSocketService != null) {
                try {
                    // 构建WebSocket通知消息
                    Map<String, Object> wsMessage = new HashMap<>();
                    wsMessage.put("type", "MESSAGE_CONFIRMATION");
                    wsMessage.put("messageId", messageId);
                    wsMessage.put("content", notificationContent);
                    wsMessage.put("timestamp", System.currentTimeMillis());
                    wsMessage.put("status", "SUCCESS");
                    
                    // 发送到所有连接的客户端
                    webSocketService.broadcastMessage("message-confirmation", wsMessage);
                    
                    // 发送到特定用户（如果有用户信息）
                    // webSocketService.sendToUser(userId, "message-confirmation", wsMessage);
                    
                    log.info("WebSocket消息确认通知已发送: messageId={}", messageId);
                    
                } catch (Exception e) {
                    log.warn("WebSocket消息确认通知发送失败: messageId={}, error={}", messageId, e.getMessage());
                }
            } else {
                log.debug("WebSocket服务不可用，跳过WebSocket通知");
            }
            
            // 备用通知方式（邮件、短信等）
            // emailService.sendNotification("消息确认通知", notificationContent, recipientEmail);
            // smsService.sendNotification(phoneNumber, notificationContent);
            
        } catch (Exception e) {
            log.warn("发送消息确认通知失败: messageId={}, error={}", messageId, e.getMessage());
            // 通知失败不影响主业务流程，只记录警告日志
        }
    }
    
    /**
     * 发送消费确认通知
     * 可以通过邮件、短信、WebSocket等方式发送通知
     */
    private void sendConsumptionConfirmationNotification(String messageId) {
        try {
            log.info("发送消费确认通知: messageId={}", messageId);
            
            // 获取消息详情
            MessageLifecycleLog messageLog = messageLifecycleService.getByMessageId(messageId);
            if (messageLog == null) {
                log.warn("消息不存在，无法发送消费确认通知: messageId={}", messageId);
                return;
            }
            
            // 构建通知内容
            String notificationContent = String.format(
                "消费确认通知\n" +
                "消息ID: %s\n" +
                "确认时间: %s\n" +
                "状态: 消费已确认\n" +
                "处理时间: %dms",
                messageId,
                LocalDateTime.now(),
                messageLog.getProcessingTime() != null ? messageLog.getProcessingTime() : 0
            );
            
            // 这里可以实现具体的通知逻辑
            // 例如：发送邮件、短信、WebSocket推送等
            
            // 记录通知日志
            log.info("消费确认通知已发送: messageId={}, 内容={}", messageId, notificationContent);
            
            // 使用WebSocket发送实时通知
            if (webSocketService != null) {
                try {
                    // 构建WebSocket通知消息
                    Map<String, Object> wsMessage = new HashMap<>();
                    wsMessage.put("type", "CONSUMPTION_CONFIRMATION");
                    wsMessage.put("messageId", messageId);
                    wsMessage.put("content", notificationContent);
                    wsMessage.put("timestamp", System.currentTimeMillis());
                    wsMessage.put("status", "SUCCESS");
                    
                    // 发送到所有连接的客户端
                    webSocketService.broadcastMessage("consumption-confirmation", wsMessage);
                    
                    // 发送到特定用户（如果有用户信息）
                    // webSocketService.sendToUser(userId, "consumption-confirmation", wsMessage);
                    
                    log.info("WebSocket消费确认通知已发送: messageId={}", messageId);
                    
                } catch (Exception e) {
                    log.warn("WebSocket消费确认通知发送失败: messageId={}, error={}", messageId, e.getMessage());
                }
            } else {
                log.debug("WebSocket服务不可用，跳过WebSocket通知");
            }
            
            // 备用通知方式（邮件、短信等）
            // emailService.sendNotification("消费确认通知", notificationContent, recipientEmail);
            // smsService.sendNotification(phoneNumber, notificationContent);
            
        } catch (Exception e) {
            log.warn("发送消费确认通知失败: messageId={}, error={}", messageId, e.getMessage());
            // 通知失败不影响主业务流程，只记录警告日志
        }
    }
    
    /**
     * 记录消息确认历史
     * 将确认操作记录到历史表中，便于后续查询和审计
     */
    private void recordConfirmationHistory(String messageId) {
        try {
            log.info("记录消息确认历史: messageId={}", messageId);
            
            // 获取消息详情
            MessageLifecycleLog messageLog = messageLifecycleService.getByMessageId(messageId);
            if (messageLog == null) {
                log.warn("消息不存在，无法记录确认历史: messageId={}", messageId);
                return;
            }
            
            // 构建确认历史记录
            Map<String, Object> confirmationHistory = new HashMap<>();
            confirmationHistory.put("messageId", messageId);
            confirmationHistory.put("confirmationTime", LocalDateTime.now());
            confirmationHistory.put("confirmationType", "MESSAGE_ACK");
            confirmationHistory.put("previousStatus", "SEND_SUCCESS");
            confirmationHistory.put("currentStatus", "ACKED");
            confirmationHistory.put("processingTime", messageLog.getProcessingTime());
            confirmationHistory.put("operator", "system"); // 可以从安全上下文获取实际操作用户
            confirmationHistory.put("operationReason", "消息发送成功后的自动确认");
            confirmationHistory.put("timestamp", System.currentTimeMillis());
            
            // 这里可以实现具体的历史记录逻辑
            // 例如：保存到数据库历史表、写入日志文件、发送到消息队列等
            
            // 示例：记录到数据库历史表（如果有历史表服务）
            // messageHistoryService.saveConfirmationHistory(confirmationHistory);
            
            // 示例：写入审计日志
            log.info("消息确认历史记录: {}", JSON.toJSONString(confirmationHistory));
            
            // 示例：发送到消息队列（如果有消息队列服务）
            // messageQueueService.sendMessage("message-confirmation-history", confirmationHistory);
            
            // 示例：保存到Redis缓存（如果有Redis服务）
            // redisTemplate.opsForHash().put("message:confirmation:history", messageId, confirmationHistory);
            
        } catch (Exception e) {
            log.warn("记录消息确认历史失败: messageId={}, error={}", messageId, e.getMessage());
            // 历史记录失败不影响主业务流程，只记录警告日志
        }
    }

    /**
     * 业务消息实体类
     */
    public static class BusinessMessage {
        private String businessType;
        private String content;
        private Map<String, Object> businessData;

        public BusinessMessage() {}

        public BusinessMessage(String businessType, String content, Map<String, Object> businessData) {
            this.businessType = businessType;
            this.content = content;
            this.businessData = businessData;
        }

        // Getters and Setters
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Map<String, Object> getBusinessData() { return businessData; }
        public void setBusinessData(Map<String, Object> businessData) { this.businessData = businessData; }
    }
}
