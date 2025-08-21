package com.jiangyang.base.seata.listener;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Seata事务监听器
 * 用于处理Seata相关事件，特别是在应用启动时进行一些初始化工作
 */
@Slf4j
@Aspect
@Component
public class SeataTransactionListener implements ApplicationListener<ApplicationReadyEvent>, InitializingBean {

    @PostConstruct
    public void init() {
        // 注册事务钩子
        TransactionHookManager.registerHook(new TransactionHook() {
            @Override
            public void beforeBegin() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction beginning: XID={}", xid);
                }
            }

            @Override
            public void afterBegin() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction began: XID={}", xid);
                }
            }

            @Override
            public void beforeCommit() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction committing: XID={}", xid);
                }
            }

            @Override
            public void afterCommit() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction committed: XID={}", xid);
                }
            }

            @Override
            public void beforeRollback() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction rolling back: XID={}", xid);
                }
            }

            @Override
            public void afterRollback() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction rolled back: XID={}", xid);
                }
            }

            @Override
            public void afterCompletion() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction completed: XID={}", xid);
                }
            }
        });
    }

    /**
     * 在事务方法执行前记录
     */
    @Before("@annotation(globalTransactional)")
    public void beforeTransaction(JoinPoint point, GlobalTransactional globalTransactional) {
        String xid = RootContext.getXID();
        if (xid != null) {
            String methodName = point.getSignature().getName();
            String className = point.getTarget().getClass().getSimpleName();
            log.info("Transaction method starting: XID={}, class={}, method={}", xid, className, methodName);
        }
    }

    /**
     * 在事务方法执行后记录
     */
    @After("@annotation(globalTransactional)")
    public void afterTransaction(JoinPoint point, GlobalTransactional globalTransactional) {
        String xid = RootContext.getXID();
        if (xid != null) {
            String methodName = point.getSignature().getName();
            String className = point.getTarget().getClass().getSimpleName();
            log.info("Transaction method completed: XID={}, class={}, method={}", xid, className, methodName);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Seata事务监听器初始化完成");
    }
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("应用程序已就绪，Seata事务监听器激活");
        
        // 输出Seata相关配置信息
        log.info("Seata配置信息:");
        log.info(" - seata.saga.state-machine.auto-register: {}", 
                System.getProperty("seata.saga.state-machine.auto-register", "未设置"));
        log.info(" - seata.enabled: {}", 
                System.getProperty("seata.enabled", "未设置"));
    }
}
