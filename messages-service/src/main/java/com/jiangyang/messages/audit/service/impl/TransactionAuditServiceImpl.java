package com.jiangyang.messages.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiangyang.messages.audit.entity.TransactionAuditLog;
import com.jiangyang.messages.audit.mapper.TransactionAuditLogMapper;
import com.jiangyang.messages.audit.service.TransactionAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 事务审计服务实现类
 */
@Slf4j
@Service
public class TransactionAuditServiceImpl extends ServiceImpl<TransactionAuditLogMapper, TransactionAuditLog> implements TransactionAuditService {

    @Override
    public TransactionAuditLog getByTransactionId(String transactionId) {
        LambdaQueryWrapper<TransactionAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionAuditLog::getBusinessTransactionId, transactionId)
               .orderByDesc(TransactionAuditLog::getCreateTime)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public List<TransactionAuditLog> getByGlobalTransactionId(String globalTransactionId) {
        LambdaQueryWrapper<TransactionAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionAuditLog::getGlobalTransactionId, globalTransactionId)
               .orderByAsc(TransactionAuditLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<TransactionAuditLog> getByBusinessTransactionId(String businessTransactionId) {
        LambdaQueryWrapper<TransactionAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionAuditLog::getBusinessTransactionId, businessTransactionId)
               .orderByAsc(TransactionAuditLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<TransactionAuditLog> getByOperationType(String operationType) {
        LambdaQueryWrapper<TransactionAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionAuditLog::getOperationType, operationType)
               .orderByDesc(TransactionAuditLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<TransactionAuditLog> getByTransactionStatus(String transactionStatus) {
        LambdaQueryWrapper<TransactionAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionAuditLog::getTransactionStatus, transactionStatus)
               .orderByDesc(TransactionAuditLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public boolean updateTransactionStatus(String transactionId, String transactionStatus) {
        try {
            TransactionAuditLog auditLog = getByTransactionId(transactionId);
            if (auditLog != null) {
                auditLog.setTransactionStatus(transactionStatus);
                return updateById(auditLog);
            }
            return false;
        } catch (Exception e) {
            log.error("更新事务状态失败: transactionId={}, transactionStatus={}, error={}", transactionId, transactionStatus, e.getMessage(), e);
            return false;
        }
    }
}
