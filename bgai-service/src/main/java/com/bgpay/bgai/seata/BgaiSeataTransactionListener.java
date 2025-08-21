package com.bgpay.bgai.seata;

import com.bgpay.bgai.service.TransactionLogService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * BgaiService特有的Seata事务监听器
 * 扩展base-service的SeataTransactionListener，添加bgai-service特有的业务逻辑
 * 注意：此监听器与base-service的SeataTransactionListener共存，提供额外的业务功能
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BgaiSeataTransactionListener {

    @Autowired(required = false)
    private TransactionLogService transactionLogService;

    @PostConstruct
    public void init() {
        // 注册事务钩子，用于记录bgai-service特有的业务日志
        TransactionHookManager.registerHook(new TransactionHook() {
            @Override
            public void beforeBegin() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("BgaiService Transaction beginning: XID={}", xid);
                }
            }

            @Override
            public void afterBegin() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("BgaiService Transaction began: XID={}", xid);
                    try {
                        if (transactionLogService != null) {
                            transactionLogService.updateTransactionStatus(xid, "ACTIVE", null);
                        }
                    } catch (Exception e) {
                        log.error("Failed to record transaction begin: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void beforeCommit() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("BgaiService Transaction committing: XID={}", xid);
                    try {
                        if (transactionLogService != null) {
                            transactionLogService.updateTransactionStatus(xid, "COMMITTING", null);
                        }
                    } catch (Exception e) {
                        log.error("Failed to record transaction commit: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void afterCommit() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("BgaiService Transaction committed: XID={}", xid);
                    try {
                        if (transactionLogService != null) {
                            transactionLogService.updateTransactionStatus(xid, "COMMITTED", null);
                        }
                    } catch (Exception e) {
                        log.error("Failed to record transaction completion: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void beforeRollback() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("BgaiService Transaction rolling back: XID={}", xid);
                    try {
                        if (transactionLogService != null) {
                            transactionLogService.updateTransactionStatus(xid, "ROLLBACKING", null);
                        }
                    } catch (Exception e) {
                        log.error("Failed to record transaction rollback: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void afterRollback() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("BgaiService Transaction rolled back: XID={}", xid);
                    try {
                        if (transactionLogService != null) {
                            transactionLogService.updateTransactionStatus(xid, "ROLLBACKED", null);
                        }
                    } catch (Exception e) {
                        log.error("Failed to record transaction rollback completion: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void afterCompletion() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("BgaiService Transaction completed: XID={}", xid);
                }
            }
        });
    }

    /**
     * 在事务方法执行前记录（bgai-service特有）
     */
    @Before("@annotation(globalTransactional)")
    public void beforeTransaction(JoinPoint point, GlobalTransactional globalTransactional) {
        String xid = RootContext.getXID();
        if (xid != null) {
            String methodName = point.getSignature().getName();
            String className = point.getTarget().getClass().getSimpleName();
            log.info("BgaiService Transaction method starting: XID={}, class={}, method={}", xid, className, methodName);
        }
    }

    /**
     * 在事务方法执行后记录（bgai-service特有）
     */
    @After("@annotation(globalTransactional)")
    public void afterTransaction(JoinPoint point, GlobalTransactional globalTransactional) {
        String xid = RootContext.getXID();
        if (xid != null) {
            String methodName = point.getSignature().getName();
            String className = point.getTarget().getClass().getSimpleName();
            log.info("BgaiService Transaction method completed: XID={}, class={}, method={}", xid, className, methodName);
        }
    }
}
