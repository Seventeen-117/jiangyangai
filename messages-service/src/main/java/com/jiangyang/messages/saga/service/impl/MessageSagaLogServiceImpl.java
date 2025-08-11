package com.jiangyang.messages.saga.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiangyang.messages.saga.entity.MessageSagaLog;
import com.jiangyang.messages.saga.mapper.MessageSagaLogMapper;
import com.jiangyang.messages.saga.service.MessageSagaLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息Saga日志服务实现类
 */
@Slf4j
@Service
public class MessageSagaLogServiceImpl extends ServiceImpl<MessageSagaLogMapper, MessageSagaLog> implements MessageSagaLogService {

    @Override
    public MessageSagaLog getByBusinessId(String businessId) {
        LambdaQueryWrapper<MessageSagaLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageSagaLog::getBusinessId, businessId)
               .orderByDesc(MessageSagaLog::getCreateTime)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public List<MessageSagaLog> getByGlobalTransactionId(String globalTransactionId) {
        LambdaQueryWrapper<MessageSagaLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageSagaLog::getGlobalTransactionId, globalTransactionId)
               .orderByAsc(MessageSagaLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<MessageSagaLog> getByOperation(String operation) {
        LambdaQueryWrapper<MessageSagaLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageSagaLog::getOperation, operation)
               .orderByDesc(MessageSagaLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<MessageSagaLog> getByStatus(String status) {
        LambdaQueryWrapper<MessageSagaLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageSagaLog::getStatus, status)
               .orderByDesc(MessageSagaLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public boolean updateStatus(String businessId, String status, String errorMessage) {
        try {
            MessageSagaLog sagaLog = getByBusinessId(businessId);
            if (sagaLog != null) {
                sagaLog.setStatus(status);
                sagaLog.setErrorMessage(errorMessage);
                return updateById(sagaLog);
            }
            return false;
        } catch (Exception e) {
            log.error("更新Saga日志状态失败: businessId={}, status={}, error={}", businessId, status, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean saveBatch(List<MessageSagaLog> sagaLogs) {
        try {
            return super.saveBatch(sagaLogs);
        } catch (Exception e) {
            log.error("批量保存Saga日志失败: error={}", e.getMessage(), e);
            return false;
        }
    }
}
