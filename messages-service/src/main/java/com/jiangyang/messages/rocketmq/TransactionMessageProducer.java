package com.jiangyang.messages.rocketmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ事务消息发送器
 * 实现真正的事务消息支持，包括本地事务执行和事务状态回查
 */
@Slf4j
@Component
public class TransactionMessageProducer {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.producer.transaction.timeout:3000}")
    private int transactionTimeout;

    @Value("${rocketmq.producer.transaction.check-thread-pool-min:1}")
    private int checkThreadPoolMin;

    @Value("${rocketmq.producer.transaction.check-thread-pool-max:1}")
    private int checkThreadPoolMax;

    @Value("${rocketmq.producer.transaction.check-request-hold-max:2000}")
    private int checkRequestHoldMax;

    /**
     * 事务消息发送器
     */
    private TransactionMQProducer producer;

    /**
     * 事务检查线程池
     */
    private ExecutorService checkExecutor;

    /**
     * 事务ID生成器
     */
    private final AtomicLong transactionIdGenerator = new AtomicLong(0);

    /**
     * 本地事务执行器映射
     */
    private final ConcurrentHashMap<String, LocalTransactionExecutor> localTransactionExecutors = new ConcurrentHashMap<>();

    /**
     * 事务状态缓存
     */
    private final ConcurrentHashMap<String, TransactionStatus> transactionStatusCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws Exception {
        log.info("初始化RocketMQ事务消息发送器: nameServer={}, producerGroup={}", nameServer, producerGroup);

        // 创建事务检查线程池
        checkExecutor = new ThreadPoolExecutor(
                checkThreadPoolMin,
                checkThreadPoolMax,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(checkRequestHoldMax),
                r -> new Thread(r, "transaction-check-thread"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 创建事务消息发送器
        producer = new TransactionMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        // 注意：TransactionMQProducer没有setTransactionTimeout方法，超时通过其他方式控制
        producer.setTransactionCheckListener(new TransactionCheckListenerImpl());
        producer.setExecutorService(checkExecutor);

        // 启动发送器
        producer.start();

        log.info("RocketMQ事务消息发送器初始化成功");
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            producer.shutdown();
            log.info("RocketMQ事务消息发送器已关闭");
        }
        if (checkExecutor != null) {
            checkExecutor.shutdown();
            log.info("事务检查线程池已关闭");
        }
    }

    /**
     * 发送事务消息
     * 
     * @param topic 主题
     * @param tag 标签
     * @param messageBody 消息体
     * @param transactionId 事务ID
     * @param businessKey 业务键
     * @param timeout 超时时间（毫秒）
     * @param localTransactionExecutor 本地事务执行器
     * @return 是否发送成功
     */
    public boolean sendTransactionMessage(String topic, String tag, String messageBody,
                                       String transactionId, String businessKey, int timeout,
                                       LocalTransactionExecutor localTransactionExecutor) {
        try {
            log.info("发送RocketMQ事务消息: topic={}, tag={}, transactionId={}, businessKey={}, timeout={}",
                    topic, tag, transactionId, businessKey, timeout);

            // 创建消息
            Message message = new Message(topic, tag, businessKey, messageBody.getBytes(StandardCharsets.UTF_8));
            
            // 设置事务相关属性
            message.putUserProperty("transactionId", transactionId);
            message.putUserProperty("businessKey", businessKey);
            message.putUserProperty("messageType", "TRANSACTION");
            message.putUserProperty("timestamp", String.valueOf(System.currentTimeMillis()));

            // 注册本地事务执行器
            if (localTransactionExecutor != null) {
                localTransactionExecutors.put(transactionId, localTransactionExecutor);
            }

            // 发送事务消息
            TransactionSendResult sendResult = producer.sendMessageInTransaction(message, localTransactionExecutor);

            // 检查发送结果
            if (sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE) {
                log.info("RocketMQ事务消息发送成功: topic={}, tag={}, transactionId={}, msgId={}, state={}",
                        topic, tag, transactionId, sendResult.getMsgId(), sendResult.getLocalTransactionState());
                
                // 缓存事务状态
                transactionStatusCache.put(transactionId, TransactionStatus.COMMITTED);
                return true;
                
            } else if (sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
                log.warn("RocketMQ事务消息发送回滚: topic={}, tag={}, transactionId={}, state={}",
                        topic, tag, transactionId, sendResult.getLocalTransactionState());
                
                // 缓存事务状态
                transactionStatusCache.put(transactionId, TransactionStatus.ROLLBACK);
                return false;
                
            } else {
                log.warn("RocketMQ事务消息发送未知状态: topic={}, tag={}, transactionId={}, state={}",
                        topic, tag, transactionId, sendResult.getLocalTransactionState());
                
                // 缓存事务状态
                transactionStatusCache.put(transactionId, TransactionStatus.UNKNOWN);
                return false;
            }

        } catch (Exception e) {
            log.error("RocketMQ事务消息发送异常: topic={}, tag={}, transactionId={}, error={}",
                    topic, tag, transactionId, e.getMessage(), e);
            
            // 缓存事务状态
            transactionStatusCache.put(transactionId, TransactionStatus.FAILED);
            return false;
        }
    }

    /**
     * 发送事务消息（使用默认本地事务执行器）
     */
    public boolean sendTransactionMessage(String topic, String tag, String messageBody,
                                       String transactionId, String businessKey, int timeout) {
        // 创建默认的本地事务执行器
        LocalTransactionExecutor defaultExecutor = new DefaultLocalTransactionExecutor();
        return sendTransactionMessage(topic, tag, messageBody, transactionId, businessKey, timeout, defaultExecutor);
    }

    /**
     * 生成事务ID
     */
    public String generateTransactionId() {
        return "tx_" + System.currentTimeMillis() + "_" + transactionIdGenerator.incrementAndGet();
    }

    /**
     * 获取事务状态
     */
    public TransactionStatus getTransactionStatus(String transactionId) {
        return transactionStatusCache.getOrDefault(transactionId, TransactionStatus.UNKNOWN);
    }

    /**
     * 清理事务状态缓存
     */
    public void clearTransactionStatus(String transactionId) {
        transactionStatusCache.remove(transactionId);
        localTransactionExecutors.remove(transactionId);
    }

    /**
     * 事务检查监听器实现
     */
    private class TransactionCheckListenerImpl implements TransactionCheckListener {
        @Override
        public LocalTransactionState checkLocalTransactionState(MessageExt msg) {
            try {
                String transactionId = msg.getUserProperty("transactionId");
                String businessKey = msg.getUserProperty("businessKey");
                
                log.info("检查本地事务状态: transactionId={}, businessKey={}, msgId={}",
                        transactionId, businessKey, msg.getMsgId());

                if (transactionId == null) {
                    log.warn("消息中未找到事务ID，回滚消息");
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }

                // 获取本地事务执行器
                LocalTransactionExecutor executor = localTransactionExecutors.get(transactionId);
                if (executor == null) {
                    log.warn("未找到本地事务执行器，回滚消息: transactionId={}", transactionId);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }

                // 执行事务状态检查
                LocalTransactionState state = executor.checkLocalTransactionState(transactionId, businessKey, msg);
                
                log.info("本地事务状态检查结果: transactionId={}, businessKey={}, state={}",
                        transactionId, businessKey, state);
                
                return state;

            } catch (Exception e) {
                log.error("检查本地事务状态异常: msgId={}, error={}", msg.getMsgId(), e.getMessage(), e);
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        }
    }

    /**
     * 本地事务执行器接口
     */
    public interface LocalTransactionExecutor {
        /**
         * 执行本地事务
         * 
         * @param transactionId 事务ID
         * @param businessKey 业务键
         * @param message 消息
         * @return 本地事务状态
         */
        LocalTransactionState executeLocalTransaction(String transactionId, String businessKey, Message message);

        /**
         * 检查本地事务状态
         * 
         * @param transactionId 事务ID
         * @param businessKey 业务键
         * @param message 消息
         * @return 本地事务状态
         */
        LocalTransactionState checkLocalTransactionState(String transactionId, String businessKey, MessageExt message);
    }

    /**
     * 默认本地事务执行器
     */
    private static class DefaultLocalTransactionExecutor implements LocalTransactionExecutor {
        @Override
        public LocalTransactionState executeLocalTransaction(String transactionId, String businessKey, Message message) {
            try {
                log.info("执行默认本地事务: transactionId={}, businessKey={}", transactionId, businessKey);
                
                // 模拟本地事务执行
                // 实际项目中这里应该执行具体的业务逻辑
                Thread.sleep(100);
                
                // 模拟成功率90%
                boolean success = Math.random() > 0.1;
                
                if (success) {
                    log.info("默认本地事务执行成功: transactionId={}, businessKey={}", transactionId, businessKey);
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else {
                    log.warn("默认本地事务执行失败: transactionId={}, businessKey={}", transactionId, businessKey);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                
            } catch (Exception e) {
                log.error("默认本地事务执行异常: transactionId={}, businessKey={}, error={}",
                        transactionId, businessKey, e.getMessage(), e);
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        }

        @Override
        public LocalTransactionState checkLocalTransactionState(String transactionId, String businessKey, MessageExt message) {
            try {
                log.info("检查默认本地事务状态: transactionId={}, businessKey={}", transactionId, businessKey);
                
                // 模拟事务状态检查
                // 实际项目中这里应该查询数据库或缓存来获取真实的事务状态
                Thread.sleep(50);
                
                // 模拟检查结果：80%概率提交，20%概率回滚
                boolean shouldCommit = Math.random() > 0.2;
                
                if (shouldCommit) {
                    log.info("默认本地事务状态检查：提交消息: transactionId={}, businessKey={}", transactionId, businessKey);
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else {
                    log.warn("默认本地事务状态检查：回滚消息: transactionId={}, businessKey={}", transactionId, businessKey);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                
            } catch (Exception e) {
                log.error("检查默认本地事务状态异常: transactionId={}, businessKey={}, error={}",
                        transactionId, businessKey, e.getMessage(), e);
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        }
    }

    /**
     * 事务状态枚举
     */
    public enum TransactionStatus {
        UNKNOWN,        // 未知状态
        COMMITTED,      // 已提交
        ROLLBACK,       // 已回滚
        FAILED          // 失败
    }

    /**
     * 获取发送器状态
     */
    public boolean isRunning() {
        return producer != null && producer.getDefaultMQProducerImpl().getServiceState().name().equals("RUNNING");
    }

    /**
     * 获取发送器统计信息
     */
    public String getProducerStats() {
        if (producer != null) {
            return String.format("ProducerGroup: %s, State: %s, TransactionTimeout: %dms",
                    producerGroup, producer.getDefaultMQProducerImpl().getServiceState(), transactionTimeout);
        }
        return "Producer not initialized";
    }
}
