package com.jiangyang.messages.audit.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiangyang.messages.audit.entity.BusinessTraceLog;
import com.jiangyang.messages.audit.entity.MessageLifecycleLog;
import com.jiangyang.messages.audit.entity.TransactionAuditLog;
import com.jiangyang.messages.audit.mapper.BusinessTraceLogMapper;
import com.jiangyang.messages.audit.mapper.MessageLifecycleLogMapper;
import com.jiangyang.messages.audit.mapper.TransactionAuditLogMapper;
import com.jiangyang.messages.audit.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 审计日志服务实现类
 */
@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private TransactionAuditLogMapper transactionAuditLogMapper;

    @Autowired
    private MessageLifecycleLogMapper messageLifecycleLogMapper;

    @Autowired
    private BusinessTraceLogMapper businessTraceLogMapper;

    @Override
    @Async("auditLogExecutor")
    public void recordTransactionAuditLog(TransactionAuditLog auditLog) {
        try {
            // 设置默认值
            if (auditLog.getCreateTime() == null) {
                auditLog.setCreateTime(LocalDateTime.now());
            }
            if (auditLog.getUpdateTime() == null) {
                auditLog.setUpdateTime(LocalDateTime.now());
            }
            
            transactionAuditLogMapper.insert(auditLog);
            log.debug("记录事务审计日志成功: {}", auditLog.getGlobalTransactionId());
        } catch (Exception e) {
            log.error("记录事务审计日志失败: {}", auditLog, e);
        }
    }

    @Override
    @Async("auditLogExecutor")
    public void recordMessageLifecycleLog(MessageLifecycleLog lifecycleLog) {
        try {
            // 设置默认值
            if (lifecycleLog.getCreateTime() == null) {
                lifecycleLog.setCreateTime(LocalDateTime.now());
            }
            if (lifecycleLog.getUpdateTime() == null) {
                lifecycleLog.setUpdateTime(LocalDateTime.now());
            }
            
            // 计算消息摘要
            if (StrUtil.isNotBlank(lifecycleLog.getMessageContent())) {
                lifecycleLog.setMessageDigest(DigestUtil.md5Hex(lifecycleLog.getMessageContent()));
            }
            
            messageLifecycleLogMapper.insert(lifecycleLog);
            log.debug("记录消息生命周期日志成功: {}", lifecycleLog.getMessageId());
        } catch (Exception e) {
            log.error("记录消息生命周期日志失败: {}", lifecycleLog, e);
        }
    }

    @Override
    @Async("auditLogExecutor")
    public void recordBusinessTraceLog(BusinessTraceLog traceLog) {
        try {
            // 设置默认值
            if (traceLog.getCreateTime() == null) {
                traceLog.setCreateTime(LocalDateTime.now());
            }
            if (traceLog.getUpdateTime() == null) {
                traceLog.setUpdateTime(LocalDateTime.now());
            }
            
            businessTraceLogMapper.insert(traceLog);
            log.debug("记录业务轨迹日志成功: {}", traceLog.getTraceId());
        } catch (Exception e) {
            log.error("记录业务轨迹日志失败: {}", traceLog, e);
        }
    }

    @Override
    @Async("auditLogExecutor")
    public void batchRecordTransactionAuditLog(List<TransactionAuditLog> auditLogs) {
        if (auditLogs == null || auditLogs.isEmpty()) {
            return;
        }
        
        try {
            auditLogs.forEach(auditLog -> {
                if (auditLog.getCreateTime() == null) {
                    auditLog.setCreateTime(LocalDateTime.now());
                }
                if (auditLog.getUpdateTime() == null) {
                    auditLog.setUpdateTime(LocalDateTime.now());
                }
            });
            
            // 分批插入，避免单次插入过多数据
            int batchSize = 100;
            for (int i = 0; i < auditLogs.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, auditLogs.size());
                List<TransactionAuditLog> batch = auditLogs.subList(i, endIndex);
                batch.forEach(transactionAuditLogMapper::insert);
            }
            
            log.debug("批量记录事务审计日志成功，数量: {}", auditLogs.size());
        } catch (Exception e) {
            log.error("批量记录事务审计日志失败", e);
        }
    }

    @Override
    @Async("auditLogExecutor")
    public void batchRecordMessageLifecycleLog(List<MessageLifecycleLog> lifecycleLogs) {
        if (lifecycleLogs == null || lifecycleLogs.isEmpty()) {
            return;
        }
        
        try {
            lifecycleLogs.forEach(lifecycleLog -> {
                if (lifecycleLog.getCreateTime() == null) {
                    lifecycleLog.setCreateTime(LocalDateTime.now());
                }
                if (lifecycleLog.getUpdateTime() == null) {
                    lifecycleLog.setUpdateTime(LocalDateTime.now());
                }
                
                // 计算消息摘要
                if (StrUtil.isNotBlank(lifecycleLog.getMessageContent())) {
                    lifecycleLog.setMessageDigest(DigestUtil.md5Hex(lifecycleLog.getMessageContent()));
                }
            });
            
            // 分批插入
            int batchSize = 100;
            for (int i = 0; i < lifecycleLogs.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, lifecycleLogs.size());
                List<MessageLifecycleLog> batch = lifecycleLogs.subList(i, endIndex);
                batch.forEach(messageLifecycleLogMapper::insert);
            }
            
            log.debug("批量记录消息生命周期日志成功，数量: {}", lifecycleLogs.size());
        } catch (Exception e) {
            log.error("批量记录消息生命周期日志失败", e);
        }
    }

    @Override
    @Async("auditLogExecutor")
    public void batchRecordBusinessTraceLog(List<BusinessTraceLog> traceLogs) {
        if (traceLogs == null || traceLogs.isEmpty()) {
            return;
        }
        
        try {
            traceLogs.forEach(traceLog -> {
                if (traceLog.getCreateTime() == null) {
                    traceLog.setCreateTime(LocalDateTime.now());
                }
                if (traceLog.getUpdateTime() == null) {
                    traceLog.setUpdateTime(LocalDateTime.now());
                }
            });
            
            // 分批插入
            int batchSize = 100;
            for (int i = 0; i < traceLogs.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, traceLogs.size());
                List<BusinessTraceLog> batch = traceLogs.subList(i, endIndex);
                batch.forEach(businessTraceLogMapper::insert);
            }
            
            log.debug("批量记录业务轨迹日志成功，数量: {}", traceLogs.size());
        } catch (Exception e) {
            log.error("批量记录业务轨迹日志失败", e);
        }
    }

    @Override
    public Map<String, Object> getTransactionTrace(String globalTransactionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询事务审计日志
            List<TransactionAuditLog> transactionLogs = transactionAuditLogMapper.selectByGlobalTransactionId(globalTransactionId);
            
            // 查询相关的消息生命周期日志
            List<MessageLifecycleLog> lifecycleLogs = new ArrayList<>();
            List<BusinessTraceLog> traceLogs = new ArrayList<>();
            
            for (TransactionAuditLog transactionLog : transactionLogs) {
                if (StrUtil.isNotBlank(transactionLog.getMessageId())) {
                    lifecycleLogs.addAll(messageLifecycleLogMapper.selectByMessageId(transactionLog.getMessageId()));
                }
                if (StrUtil.isNotBlank(transactionLog.getOperationChainId())) {
                    traceLogs.addAll(businessTraceLogMapper.selectByTraceId(transactionLog.getOperationChainId()));
                }
            }
            
            result.put("globalTransactionId", globalTransactionId);
            result.put("transactionLogs", transactionLogs);
            result.put("lifecycleLogs", lifecycleLogs);
            result.put("traceLogs", traceLogs);
            result.put("totalCount", transactionLogs.size() + lifecycleLogs.size() + traceLogs.size());
            
        } catch (Exception e) {
            log.error("查询事务轨迹失败: {}", globalTransactionId, e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getBusinessTransactionTrace(String businessTransactionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询事务审计日志
            List<TransactionAuditLog> transactionLogs = transactionAuditLogMapper.selectByBusinessTransactionId(businessTransactionId);
            
            // 查询相关的消息生命周期日志和业务轨迹日志
            List<MessageLifecycleLog> lifecycleLogs = new ArrayList<>();
            List<BusinessTraceLog> traceLogs = new ArrayList<>();
            
            for (TransactionAuditLog transactionLog : transactionLogs) {
                if (StrUtil.isNotBlank(transactionLog.getMessageId())) {
                    lifecycleLogs.addAll(messageLifecycleLogMapper.selectByMessageId(transactionLog.getMessageId()));
                }
                if (StrUtil.isNotBlank(transactionLog.getOperationChainId())) {
                    traceLogs.addAll(businessTraceLogMapper.selectByTraceId(transactionLog.getOperationChainId()));
                }
            }
            
            result.put("businessTransactionId", businessTransactionId);
            result.put("transactionLogs", transactionLogs);
            result.put("lifecycleLogs", lifecycleLogs);
            result.put("traceLogs", traceLogs);
            result.put("totalCount", transactionLogs.size() + lifecycleLogs.size() + traceLogs.size());
            
        } catch (Exception e) {
            log.error("查询业务事务轨迹失败: {}", businessTransactionId, e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getMessageLifecycleTrace(String messageId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询消息生命周期日志
            List<MessageLifecycleLog> lifecycleLogs = messageLifecycleLogMapper.selectByMessageId(messageId);
            
            // 按阶段排序
            lifecycleLogs.sort(Comparator.comparing(MessageLifecycleLog::getStageStartTime));
            
            result.put("messageId", messageId);
            result.put("lifecycleLogs", lifecycleLogs);
            result.put("totalStages", lifecycleLogs.size());
            
            // 计算总耗时
            if (!lifecycleLogs.isEmpty()) {
                LocalDateTime startTime = lifecycleLogs.get(0).getStageStartTime();
                LocalDateTime endTime = lifecycleLogs.get(lifecycleLogs.size() - 1).getStageEndTime();
                if (startTime != null && endTime != null) {
                    long totalDuration = java.time.Duration.between(startTime, endTime).toMillis();
                    result.put("totalDuration", totalDuration);
                }
            }
            
        } catch (Exception e) {
            log.error("查询消息生命周期轨迹失败: {}", messageId, e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getBusinessTraceChain(String traceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询完整的调用链
            List<BusinessTraceLog> traceLogs = businessTraceLogMapper.selectCompleteCallChain(traceId);
            
            // 构建调用树
            Map<String, List<BusinessTraceLog>> traceTree = traceLogs.stream()
                    .collect(Collectors.groupingBy(traceLog -> 
                            StrUtil.isBlank(traceLog.getParentSpanId()) ? "root" : traceLog.getParentSpanId()));
            
            result.put("traceId", traceId);
            result.put("traceLogs", traceLogs);
            result.put("traceTree", traceTree);
            result.put("totalSpans", traceLogs.size());
            
            // 计算调用链总耗时
            if (!traceLogs.isEmpty()) {
                LocalDateTime startTime = traceLogs.stream()
                        .map(BusinessTraceLog::getStartTime)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);
                        
                LocalDateTime endTime = traceLogs.stream()
                        .map(BusinessTraceLog::getEndTime)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
                        
                if (startTime != null && endTime != null) {
                    long totalDuration = java.time.Duration.between(startTime, endTime).toMillis();
                    result.put("totalDuration", totalDuration);
                }
            }
            
        } catch (Exception e) {
            log.error("查询业务调用轨迹失败: {}", traceId, e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getTransactionStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long totalCount = transactionAuditLogMapper.countTransactionsByTimeRange(startTime, endTime);
            Long failedCount = transactionAuditLogMapper.countFailedTransactionsByTimeRange(startTime, endTime);
            
            result.put("startTime", startTime);
            result.put("endTime", endTime);
            result.put("totalCount", totalCount);
            result.put("failedCount", failedCount);
            result.put("successCount", totalCount - failedCount);
            result.put("successRate", totalCount > 0 ? (double) (totalCount - failedCount) / totalCount : 0.0);
            
        } catch (Exception e) {
            log.error("查询事务统计失败", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getMessageStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long totalCount = messageLifecycleLogMapper.countMessagesByTimeRange(startTime, endTime);
            Long deadLetterCount = messageLifecycleLogMapper.countDeadLetterMessagesByTimeRange(startTime, endTime);
            Long timeoutCount = messageLifecycleLogMapper.countTimeoutMessagesByTimeRange(startTime, endTime);
            
            result.put("startTime", startTime);
            result.put("endTime", endTime);
            result.put("totalCount", totalCount);
            result.put("deadLetterCount", deadLetterCount);
            result.put("timeoutCount", timeoutCount);
            result.put("successCount", totalCount - deadLetterCount - timeoutCount);
            
        } catch (Exception e) {
            log.error("查询消息统计失败", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getCallStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long totalCount = businessTraceLogMapper.countCallsByTimeRange(startTime, endTime);
            Long failedCount = businessTraceLogMapper.countFailedCallsByTimeRange(startTime, endTime);
            Long timeoutCount = businessTraceLogMapper.countTimeoutCallsByTimeRange(startTime, endTime);
            
            result.put("startTime", startTime);
            result.put("endTime", endTime);
            result.put("totalCount", totalCount);
            result.put("failedCount", failedCount);
            result.put("timeoutCount", timeoutCount);
            result.put("successCount", totalCount - failedCount - timeoutCount);
            result.put("successRate", totalCount > 0 ? (double) (totalCount - failedCount - timeoutCount) / totalCount : 0.0);
            
        } catch (Exception e) {
            log.error("查询调用统计失败", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @Override
    public List<TransactionAuditLog> getFailedTransactions(LocalDateTime startTime, LocalDateTime endTime) {
        return transactionAuditLogMapper.selectFailedTransactions(startTime, endTime);
    }

    @Override
    public List<MessageLifecycleLog> getDeadLetterMessages(LocalDateTime startTime, LocalDateTime endTime) {
        return messageLifecycleLogMapper.selectDeadLetterMessages(startTime, endTime);
    }

    @Override
    public List<BusinessTraceLog> getFailedCalls(LocalDateTime startTime, LocalDateTime endTime) {
        return businessTraceLogMapper.selectFailedCalls(startTime, endTime);
    }

    @Override
    @Transactional
    public void cleanExpiredAuditLogs(int retentionDays) {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
            
            // 清理过期的事务审计日志
            LambdaQueryWrapper<TransactionAuditLog> transactionWrapper = new LambdaQueryWrapper<>();
            transactionWrapper.lt(TransactionAuditLog::getCreateTime, expireTime);
            transactionAuditLogMapper.delete(transactionWrapper);
            
            // 清理过期的消息生命周期日志
            LambdaQueryWrapper<MessageLifecycleLog> lifecycleWrapper = new LambdaQueryWrapper<>();
            lifecycleWrapper.lt(MessageLifecycleLog::getCreateTime, expireTime);
            messageLifecycleLogMapper.delete(lifecycleWrapper);
            
            // 清理过期的业务轨迹日志
            LambdaQueryWrapper<BusinessTraceLog> traceWrapper = new LambdaQueryWrapper<>();
            traceWrapper.lt(BusinessTraceLog::getCreateTime, expireTime);
            businessTraceLogMapper.delete(traceWrapper);
            
            log.info("清理过期审计日志成功，保留天数: {}", retentionDays);
        } catch (Exception e) {
            log.error("清理过期审计日志失败", e);
        }
    }

    @Override
    public byte[] exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String format) {
        try {
            // 查询所有类型的日志
            List<TransactionAuditLog> transactionLogs = transactionAuditLogMapper.selectByTimeRange(startTime, endTime);
            List<MessageLifecycleLog> lifecycleLogs = messageLifecycleLogMapper.selectByTimeRange(startTime, endTime);
            List<BusinessTraceLog> traceLogs = businessTraceLogMapper.selectByTimeRange(startTime, endTime);
            
            // 根据格式导出
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> exportData = new HashMap<>();
                exportData.put("transactionLogs", transactionLogs);
                exportData.put("lifecycleLogs", lifecycleLogs);
                exportData.put("traceLogs", traceLogs);
                exportData.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                return JSON.toJSONString(exportData).getBytes();
            } else {
                // 默认CSV格式
                StringBuilder csv = new StringBuilder();
                csv.append("LogType,ID,CreateTime,Status,Details\n");
                
                // 添加事务日志
                for (TransactionAuditLog log : transactionLogs) {
                    csv.append(String.format("Transaction,%s,%s,%s,%s\n",
                            log.getGlobalTransactionId(),
                            log.getCreateTime(),
                            log.getTransactionStatus(),
                            log.getErrorMessage()));
                }
                
                // 添加生命周期日志
                for (MessageLifecycleLog log : lifecycleLogs) {
                    csv.append(String.format("Lifecycle,%s,%s,%s,%s\n",
                            log.getMessageId(),
                            log.getCreateTime(),
                            log.getStageStatus(),
                            log.getErrorMessage()));
                }
                
                // 添加轨迹日志
                for (BusinessTraceLog log : traceLogs) {
                    csv.append(String.format("Trace,%s,%s,%s,%s\n",
                            log.getTraceId(),
                            log.getCreateTime(),
                            log.getCallStatus(),
                            log.getErrorMessage()));
                }
                
                return csv.toString().getBytes();
            }
        } catch (Exception e) {
            log.error("导出审计日志失败", e);
            return new byte[0];
        }
    }
}
