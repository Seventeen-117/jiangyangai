package com.jiangyang.messages.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiangyang.messages.audit.entity.MessageLifecycleLog;
import com.jiangyang.messages.audit.mapper.MessageLifecycleLogMapper;
import com.jiangyang.messages.audit.service.MessageLifecycleService;
import com.jiangyang.base.datasource.annotation.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息生命周期服务实现类
 */
@Slf4j
@Service
@DataSource("audit")
public class MessageLifecycleServiceImpl extends ServiceImpl<MessageLifecycleLogMapper, MessageLifecycleLog> implements MessageLifecycleService {

    @Override
    public MessageLifecycleLog getByMessageId(String messageId) {
        LambdaQueryWrapper<MessageLifecycleLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageLifecycleLog::getMessageId, messageId)
               .orderByDesc(MessageLifecycleLog::getCreateTime)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public MessageLifecycleLog getByMessageIdAndStage(String messageId, String lifecycleStage) {
        LambdaQueryWrapper<MessageLifecycleLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageLifecycleLog::getMessageId, messageId)
               .eq(MessageLifecycleLog::getLifecycleStage, lifecycleStage)
               .orderByDesc(MessageLifecycleLog::getCreateTime)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public List<MessageLifecycleLog> getByBusinessMessageId(String businessMessageId) {
        LambdaQueryWrapper<MessageLifecycleLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageLifecycleLog::getBusinessMessageId, businessMessageId)
               .orderByAsc(MessageLifecycleLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<MessageLifecycleLog> getByLifecycleStage(String lifecycleStage) {
        LambdaQueryWrapper<MessageLifecycleLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageLifecycleLog::getLifecycleStage, lifecycleStage)
               .orderByDesc(MessageLifecycleLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<MessageLifecycleLog> getByStageStatus(String stageStatus) {
        LambdaQueryWrapper<MessageLifecycleLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageLifecycleLog::getStageStatus, stageStatus)
               .orderByDesc(MessageLifecycleLog::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public boolean updateStageStatus(String messageId, String stageStatus, String errorMessage) {
        try {
            MessageLifecycleLog lifecycleLog = getByMessageId(messageId);
            if (lifecycleLog != null) {
                lifecycleLog.setStageStatus(stageStatus);
                lifecycleLog.setErrorMessage(errorMessage);
                return updateById(lifecycleLog);
            }
            return false;
        } catch (Exception e) {
            log.error("更新生命周期日志状态失败: messageId={}, stageStatus={}, error={}", messageId, stageStatus, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean saveBatch(List<MessageLifecycleLog> lifecycleLogs) {
        try {
            return super.saveBatch(lifecycleLogs);
        } catch (Exception e) {
            log.error("批量保存生命周期日志失败: error={}", e.getMessage(), e);
            return false;
        }
    }
}
