package com.bgpay.bgai.service.mq;

import com.alibaba.fastjson2.JSON;
import com.bgpay.bgai.entity.TransactionLog;
import com.bgpay.bgai.entity.UsageCalculationDTO;
import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.service.UsageInfoService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jiangyang.base.datasource.annotation.DataSource;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQTemplate")
@Slf4j
@DataSource("master")
public class BillingTransactionListenerImpl implements RocketMQLocalTransactionListener {
    private static final String PROCESSED_KEY_PREFIX = "PROCESSED:";
    private static final int LOCAL_CACHE_MAX_SIZE = 100_000;
    private static final int LOCAL_CACHE_EXPIRE_MINUTES = 5;
    private static final int REDIS_CACHE_EXPIRE_HOURS = 24;

    private final Cache<String, Boolean> localCache = Caffeine.newBuilder()
            .maximumSize(LOCAL_CACHE_MAX_SIZE)
            .expireAfterWrite(LOCAL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .build();
            
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private TransactionLogService transactionLogService;
    
    @Autowired
    private UsageInfoService usageInfoService;
    
    @Autowired
    private TransactionCoordinator transactionCoordinator;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String xid = RootContext.getXID();
        String userId = msg.getHeaders().get("USER_ID", String.class);
        String completionId = extractCompletionId(msg, arg);
        
        try {
            log.info("开始执行本地事务, xid: {}, userId: {}, completionId: {}", xid, userId, completionId);
            
            // 检查是否已处理
            if (checkProcessed(completionId)) {
                log.info("消息已处理，提交事务, completionId: {}", completionId);
                return RocketMQLocalTransactionState.COMMIT;
            }
            
            // 检查事务日志 - 添加重试机制
            TransactionLog txLog = null;
            int retryCount = 0;
            int maxRetries = 3;
            
            while (txLog == null && retryCount < maxRetries) {
                txLog = transactionLogService.findByXid(xid);
                if (txLog == null) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        log.warn("事务日志不存在，等待重试 ({}/{}), xid: {}", retryCount, maxRetries, xid);
                        try {
                            Thread.sleep(1000 * retryCount); // 递增等待时间
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            if (txLog == null) {
                log.error("事务日志不存在，尝试创建事务日志, xid: {}", xid);
                
                // 尝试创建事务日志
                try {
                    Long logId = transactionLogService.recordTransactionBegin(
                        xid, 
                        "billing-transaction", 
                        "ROCKETMQ", 
                        "/api/billing", 
                        "127.0.0.1", 
                        userId
                    );
                    
                    if (logId != null) {
                        log.info("成功创建事务日志, xid: {}, logId: {}", xid, logId);
                        txLog = transactionLogService.findByXid(xid);
                    } else {
                        log.error("创建事务日志失败, xid: {}", xid);
                    }
                } catch (Exception e) {
                    log.error("创建事务日志时发生异常, xid: {}", xid, e);
                }
            }
            
            // 如果仍然找不到事务日志，检查事务协调器
            if (txLog == null) {
                log.warn("事务日志不存在，检查事务协调器状态, xid: {}, userId: {}", xid, userId);
                
                // 检查事务协调器中的状态
                if (transactionCoordinator.hasActiveTransaction(userId)) {
                    String currentId = transactionCoordinator.getCurrentCompletionId(userId);
                    if (currentId != null && currentId.equals(completionId)) {
                        log.info("事务协调器中有活跃事务且ID一致，提交事务, completionId: {}", completionId);
                        return RocketMQLocalTransactionState.COMMIT;
                    }
                }
                
                log.error("事务日志不存在且事务协调器中无活跃事务，回滚事务, xid: {}", xid);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            
            // 检查事务状态
            if ("COMPLETED".equals(txLog.getStatus())) {
                log.info("事务已完成，提交事务, xid: {}", xid);
                return RocketMQLocalTransactionState.COMMIT;
            }
            
            // 验证事务ID是否存在且一致
            String currentId = transactionCoordinator.getCurrentCompletionId(userId);
            if (currentId == null || !currentId.equals(completionId)) {
                log.error("事务ID不一致或不存在，回滚事务, expected: {}, actual: {}", completionId, currentId);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            
            // 更新事务状态为已完成
            transactionLogService.updateTransactionStatus(xid, "COMPLETED", null);
            
            log.info("本地事务执行成功，提交事务, xid: {}, completionId: {}", xid, completionId);
            return RocketMQLocalTransactionState.COMMIT;
            
        } catch (Exception e) {
            log.error("本地事务执行异常，回滚事务, xid: {}, completionId: {}", xid, completionId, e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String completionId = extractCompletionId(msg, null);
        String userId = msg.getHeaders().get("USER_ID", String.class);
        
        try {
            log.info("检查本地事务状态, userId: {}, completionId: {}", userId, completionId);
            
            // 检查事务状态
            if (transactionCoordinator.hasActiveTransaction(userId)) {
                String currentId = transactionCoordinator.getCurrentCompletionId(userId);
                if (completionId.equals(currentId)) {
                    log.info("事务活跃且ID一致，提交事务, completionId: {}", completionId);
                    return RocketMQLocalTransactionState.COMMIT;
                }
            }
            
            // 检查是否已处理
            if (checkProcessed(completionId)) {
                log.info("消息已处理，提交事务, completionId: {}", completionId);
                return RocketMQLocalTransactionState.COMMIT;
            }
            
            // 回滚事务
            log.warn("未找到活跃事务或消息未处理，回滚事务, completionId: {}", completionId);
            return RocketMQLocalTransactionState.ROLLBACK;
            
        } catch (Exception e) {
            log.error("检查本地事务状态异常, completionId: {}", completionId, e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    private String extractCompletionId(Message msg, Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        }

        String keys = msg.getHeaders().get(RocketMQHeaders.KEYS, String.class);
        if (keys != null) {
            return keys;
        }

        try {
            byte[] payload = (byte[]) msg.getPayload();
            UsageCalculationDTO dto = JSON.parseObject(payload, UsageCalculationDTO.class);
            return dto.getChatCompletionId();
        } catch (Exception e) {
            log.error("消息体解析失败", e);
            return null;
        }
    }

    private boolean checkProcessed(String completionId) {
        if (completionId == null) {
            return false;
        }
        
        // 检查本地缓存
        if (localCache.getIfPresent(completionId) != null) {
            return true;
        }
        
        // 检查Redis缓存
        String redisKey = PROCESSED_KEY_PREFIX + completionId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            localCache.put(completionId, true);
            return true;
        }
        
        // 检查数据库
        try {
            boolean dbExists = usageInfoService.existsByCompletionId(completionId);
            if (dbExists) {
                // 异步更新缓存
                redisTemplate.opsForValue().set(redisKey, "1", REDIS_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                localCache.put(completionId, true);
            }
            return dbExists;
        } catch (Exception e) {
            log.error("数据库检查失败，completionId: {}", completionId, e);
            return false;
        }
    }
}