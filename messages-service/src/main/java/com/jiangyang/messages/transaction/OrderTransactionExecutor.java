package com.jiangyang.messages.transaction;

import com.jiangyang.messages.rocketmq.TransactionMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单事务执行器
 * 实现订单创建和库存扣减的本地事务
 */
@Slf4j
@Component
public class OrderTransactionExecutor implements TransactionMessageProducer.LocalTransactionExecutor {

    /**
     * 订单事务状态缓存
     */
    private final ConcurrentHashMap<String, OrderTransactionStatus> orderStatusCache = new ConcurrentHashMap<>();

    /**
     * 订单ID生成器
     */
    private final AtomicLong orderIdGenerator = new AtomicLong(0);

    @Override
    public LocalTransactionState executeLocalTransaction(String transactionId, String businessKey, Message message) {
        try {
            log.info("执行订单本地事务: transactionId={}, businessKey={}", transactionId, businessKey);

            // 1. 解析业务数据
            String messageBody = new String(message.getBody());
            log.info("订单事务消息内容: {}", messageBody);

            // 2. 执行本地事务：创建订单
            boolean orderCreated = createOrder(transactionId, businessKey, messageBody);
            if (!orderCreated) {
                log.error("订单创建失败，回滚事务: transactionId={}, businessKey={}", transactionId, businessKey);
                updateOrderStatus(transactionId, OrderTransactionStatus.FAILED);
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }

            // 3. 执行本地事务：扣减库存
            boolean inventoryDeducted = deductInventory(transactionId, businessKey);
            if (!inventoryDeducted) {
                log.error("库存扣减失败，回滚事务: transactionId={}, businessKey={}", transactionId, businessKey);
                updateOrderStatus(transactionId, OrderTransactionStatus.FAILED);
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }

            // 4. 事务执行成功
            log.info("订单本地事务执行成功: transactionId={}, businessKey={}", transactionId, businessKey);
            updateOrderStatus(transactionId, OrderTransactionStatus.COMMITTED);
            return LocalTransactionState.COMMIT_MESSAGE;

        } catch (Exception e) {
            log.error("订单本地事务执行异常: transactionId={}, businessKey={}, error={}",
                    transactionId, businessKey, e.getMessage(), e);
            updateOrderStatus(transactionId, OrderTransactionStatus.FAILED);
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    @Override
    public LocalTransactionState checkLocalTransactionState(String transactionId, String businessKey, MessageExt message) {
        try {
            log.info("检查订单本地事务状态: transactionId={}, businessKey={}", transactionId, businessKey);

            // 从缓存中获取事务状态
            OrderTransactionStatus status = orderStatusCache.get(transactionId);
            if (status == null) {
                log.warn("未找到订单事务状态，尝试从数据库查询: transactionId={}", transactionId);
                status = queryOrderStatusFromDatabase(transactionId);
            }

            if (status == null) {
                log.warn("无法确定订单事务状态，回滚消息: transactionId={}", transactionId);
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }

            switch (status) {
                case COMMITTED:
                    log.info("订单事务已提交: transactionId={}", transactionId);
                    return LocalTransactionState.COMMIT_MESSAGE;
                case FAILED:
                    log.warn("订单事务执行失败，回滚消息: transactionId={}", transactionId);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                case PROCESSING:
                    log.info("订单事务正在处理中: transactionId={}", transactionId);
                    return LocalTransactionState.UNKNOW;
                default:
                    log.warn("订单事务状态未知，回滚消息: transactionId={}", transactionId);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
            }

        } catch (Exception e) {
            log.error("检查订单本地事务状态异常: transactionId={}, businessKey={}, error={}",
                    transactionId, businessKey, e.getMessage(), e);
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    /**
     * 创建订单
     */
    private boolean createOrder(String transactionId, String businessKey, String messageBody) {
        try {
            log.info("开始创建订单: transactionId={}, businessKey={}", transactionId, businessKey);

            // 模拟订单创建过程
            // 实际项目中这里应该：
            // 1. 验证订单数据
            // 2. 检查用户权限
            // 3. 验证商品信息
            // 4. 计算订单金额
            // 5. 插入订单记录到数据库

            // 模拟处理时间
            Thread.sleep(200);

            // 模拟成功率95%
            boolean success = Math.random() > 0.05;
            if (success) {
                // 生成订单ID
                String orderId = "ORDER_" + orderIdGenerator.incrementAndGet();
                log.info("订单创建成功: transactionId={}, businessKey={}, orderId={}", 
                        transactionId, businessKey, orderId);
                
                // 缓存订单信息
                orderStatusCache.put(transactionId, OrderTransactionStatus.PROCESSING);
                return true;
            } else {
                log.warn("订单创建失败: transactionId={}, businessKey={}", transactionId, businessKey);
                return false;
            }

        } catch (Exception e) {
            log.error("创建订单异常: transactionId={}, businessKey={}, error={}",
                    transactionId, businessKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 扣减库存
     */
    private boolean deductInventory(String transactionId, String businessKey) {
        try {
            log.info("开始扣减库存: transactionId={}, businessKey={}", transactionId, businessKey);

            // 模拟库存扣减过程
            // 实际项目中这里应该：
            // 1. 查询当前库存
            // 2. 检查库存是否充足
            // 3. 执行库存扣减
            // 4. 记录库存变更日志
            // 5. 更新库存记录

            // 模拟处理时间
            Thread.sleep(150);

            // 模拟成功率98%
            boolean success = Math.random() > 0.02;
            if (success) {
                log.info("库存扣减成功: transactionId={}, businessKey={}", transactionId, businessKey);
                return true;
            } else {
                log.warn("库存扣减失败: transactionId={}, businessKey={}", transactionId, businessKey);
                return false;
            }

        } catch (Exception e) {
            log.error("扣减库存异常: transactionId={}, businessKey={}, error={}",
                    transactionId, businessKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从数据库查询订单状态
     */
    private OrderTransactionStatus queryOrderStatusFromDatabase(String transactionId) {
        try {
            log.info("从数据库查询订单状态: transactionId={}", transactionId);

            // 模拟数据库查询
            // 实际项目中这里应该：
            // 1. 根据transactionId查询订单表
            // 2. 检查订单状态
            // 3. 检查库存扣减状态
            // 4. 返回综合状态

            Thread.sleep(100);

            // 模拟查询结果：70%概率已提交，30%概率失败
            boolean committed = Math.random() > 0.3;
            OrderTransactionStatus status = committed ? OrderTransactionStatus.COMMITTED : OrderTransactionStatus.FAILED;

            // 缓存查询结果
            orderStatusCache.put(transactionId, status);

            log.info("数据库查询结果: transactionId={}, status={}", transactionId, status);
            return status;

        } catch (Exception e) {
            log.error("查询订单状态异常: transactionId={}, error={}", transactionId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 更新订单状态
     */
    private void updateOrderStatus(String transactionId, OrderTransactionStatus status) {
        orderStatusCache.put(transactionId, status);
        log.info("更新订单事务状态: transactionId={}, status={}", transactionId, status);
    }

    /**
     * 获取订单事务状态
     */
    public OrderTransactionStatus getOrderTransactionStatus(String transactionId) {
        return orderStatusCache.get(transactionId);
    }

    /**
     * 清理订单事务状态
     */
    public void clearOrderTransactionStatus(String transactionId) {
        orderStatusCache.remove(transactionId);
        log.info("清理订单事务状态: transactionId={}", transactionId);
    }

    /**
     * 订单事务状态枚举
     */
    public enum OrderTransactionStatus {
        PROCESSING,     // 处理中
        COMMITTED,      // 已提交
        FAILED          // 失败
    }
}
