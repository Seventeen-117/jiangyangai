package com.jiangyang.base.seata.interceptor;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seata分布式事务拦截器
 * 用于拦截@GlobalTransactional注解的方法，提供事务增强功能
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class SeataTransactionInterceptor {

    /**
     * 环绕通知，拦截@GlobalTransactional注解的方法
     */
    @Around("@annotation(globalTransactional)")
    public Object around(ProceedingJoinPoint point, GlobalTransactional globalTransactional) throws Throwable {
        String xid = RootContext.getXID();
        String methodName = point.getSignature().getName();
        String className = point.getTarget().getClass().getSimpleName();
        
        log.info("Seata事务拦截器开始: XID={}, class={}, method={}", xid, className, methodName);
        
        try {
            // 如果没有全局事务上下文，创建一个新的
            if (xid == null) {
                GlobalTransaction globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
                globalTransaction.begin(globalTransactional.timeoutMills(), globalTransactional.name());
                xid = globalTransaction.getXid();
                log.info("创建新的全局事务: XID={}", xid);
            }
            
            // 设置分支类型 - 默认使用AT模式
            RootContext.bindBranchType(BranchType.AT);
            
            // 执行原方法
            Object result = point.proceed();
            
            log.info("Seata事务执行成功: XID={}, class={}, method={}", xid, className, methodName);
            return result;
            
        } catch (Throwable e) {
            log.error("Seata事务执行失败: XID={}, class={}, method={}, error={}", 
                    xid, className, methodName, e.getMessage(), e);
            
            // 如果是新创建的事务，需要回滚
            if (xid != null && RootContext.getXID() == null) {
                try {
                    GlobalTransactionContext.reload(xid).rollback();
                    log.info("全局事务回滚完成: XID={}", xid);
                } catch (Exception rollbackException) {
                    log.error("全局事务回滚失败: XID={}", xid, rollbackException);
                }
            }
            
            throw e;
        } finally {
            // 清理分支类型绑定
            RootContext.unbindBranchType();
            
            // 如果是新创建的事务，需要提交
            if (xid != null && RootContext.getXID() == null) {
                try {
                    GlobalTransactionContext.reload(xid).commit();
                    log.info("全局事务提交完成: XID={}", xid);
                } catch (Exception commitException) {
                    log.error("全局事务提交失败: XID={}", xid, commitException);
                }
            }
        }
    }
}
