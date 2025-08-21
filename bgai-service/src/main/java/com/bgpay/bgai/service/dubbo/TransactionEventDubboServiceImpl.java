package com.bgpay.bgai.service.dubbo;

import com.bgpay.bgai.service.TransactionEventService;
import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.dubbo.api.common.Result;
import com.jiangyang.dubbo.api.transaction.model.TransactionEvent;
import com.jiangyang.dubbo.api.transaction.model.TransactionEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 事务事件Dubbo服务实现类
 * 实现dubbo-api中定义的接口
 */
@Slf4j
@Service
@DataSource("master")
@DubboService(version = "1.0.0", timeout = 5000, retries = 2)
public class TransactionEventDubboServiceImpl implements com.jiangyang.dubbo.api.transaction.TransactionEventService {

    @Autowired
    private TransactionEventService localTransactionEventService;

    @Override
    public Result<TransactionEventResponse> processTransactionEvent(TransactionEvent event) {
        try {
            log.info("Dubbo服务接收到事务事件: transactionId={}, operationType={}, status={}",
                    event.getTransactionId(), event.getOperationType(), event.getStatus());

            // 转换为本地模型
            com.bgpay.bgai.model.TransactionEvent localEvent = convertToLocalModel(event);
            
            // 调用本地服务处理
            com.bgpay.bgai.model.TransactionEventResponse localResponse = 
                    localTransactionEventService.processTransactionEvent(localEvent);
            
            // 转换为Dubbo响应模型
            TransactionEventResponse response = convertToDubboResponse(localResponse);
            
            log.info("事务事件处理成功: transactionId={}", event.getTransactionId());
            return Result.success("事务事件处理成功", response);
            
        } catch (Exception e) {
            log.error("处理事务事件异常: transactionId={}, error={}", 
                    event.getTransactionId(), e.getMessage(), e);
            return Result.failure("处理事务事件异常: " + e.getMessage());
        }
    }

    @Override
    public Result<TransactionEventResponse> processBatchTransactionEvents(List<TransactionEvent> events) {
        try {
            log.info("Dubbo服务接收到批量事务事件，数量: {}", events.size());

            // 转换为本地模型列表
            List<com.bgpay.bgai.model.TransactionEvent> localEvents = 
                    events.stream().map(this::convertToLocalModel).toList();
            
            // 调用本地服务处理
            com.bgpay.bgai.model.TransactionEventResponse localResponse = 
                    localTransactionEventService.processBatchTransactionEvents(localEvents);
            
            // 转换为Dubbo响应模型
            TransactionEventResponse response = convertToDubboResponse(localResponse);
            
            log.info("批量事务事件处理成功，数量: {}", events.size());
            return Result.success("批量事务事件处理成功", response);
            
        } catch (Exception e) {
            log.error("处理批量事务事件异常，数量: {}, error={}", 
                    events.size(), e.getMessage(), e);
            return Result.failure("处理批量事务事件异常: " + e.getMessage());
        }
    }

    @Override
    public Result<List<TransactionEvent>> getTransactionEventHistory(String transactionId) {
        try {
            log.info("Dubbo服务查询事务事件历史: transactionId={}", transactionId);

            List<com.bgpay.bgai.model.TransactionEvent> localEvents = 
                    localTransactionEventService.getTransactionEventHistory(transactionId);
            
            // 转换为Dubbo模型列表
            List<TransactionEvent> events = localEvents.stream()
                    .map(this::convertToDubboModel)
                    .toList();
            
            log.info("查询事务事件历史成功，数量: {}", events.size());
            return Result.success("查询事务事件历史成功", events);
            
        } catch (Exception e) {
            log.error("查询事务事件历史异常: transactionId={}, error={}", 
                    transactionId, e.getMessage(), e);
            return Result.failure("查询事务事件历史异常: " + e.getMessage());
        }
    }

    @Override
    public Result<List<TransactionEvent>> getTransactionEventsByTimeRange(String startTime, String endTime, String status) {
        try {
            log.info("Dubbo服务根据时间范围查询事务事件: startTime={}, endTime={}, status={}", 
                    startTime, endTime, status);

            List<com.bgpay.bgai.model.TransactionEvent> localEvents = 
                    localTransactionEventService.getTransactionEventsByTimeRange(startTime, endTime, status);
            
            // 转换为Dubbo模型列表
            List<TransactionEvent> events = localEvents.stream()
                    .map(this::convertToDubboModel)
                    .toList();
            
            log.info("根据时间范围查询事务事件成功，数量: {}", events.size());
            return Result.success("根据时间范围查询事务事件成功", events);
            
        } catch (Exception e) {
            log.error("根据时间范围查询事务事件异常: startTime={}, endTime={}, status={}, error={}", 
                    startTime, endTime, status, e.getMessage(), e);
            return Result.failure("根据时间范围查询事务事件异常: " + e.getMessage());
        }
    }

    @Override
    public Result<Object> getTransactionEventStatistics(String timeRange) {
        try {
            log.info("Dubbo服务获取事务事件统计信息: timeRange={}", timeRange);

            Object statistics = localTransactionEventService.getTransactionEventStatistics(timeRange);
            
            log.info("获取事务事件统计信息成功");
            return Result.success("获取事务事件统计信息成功", statistics);
            
        } catch (Exception e) {
            log.error("获取事务事件统计信息异常: timeRange={}, error={}", 
                    timeRange, e.getMessage(), e);
            return Result.failure("获取事务事件统计信息异常: " + e.getMessage());
        }
    }

    @Override
    public Result<Boolean> updateTransactionEventStatus(String eventId, String status, String message) {
        try {
            log.info("Dubbo服务更新事务事件状态: eventId={}, status={}, message={}", eventId, status, message);

            localTransactionEventService.updateTransactionEventStatus(eventId, status, message);
            
            log.info("更新事务事件状态成功");
            return Result.success("更新事务事件状态成功", true);
            
        } catch (Exception e) {
            log.error("更新事务事件状态异常: eventId={}, error={}", eventId, e.getMessage(), e);
            return Result.failure("更新事务事件状态异常: " + e.getMessage());
        }
    }

    @Override
    public Result<Boolean> deleteExpiredTransactionEvents(int daysToKeep) {
        try {
            log.info("Dubbo服务删除过期事务事件: daysToKeep={}", daysToKeep);

            localTransactionEventService.deleteExpiredTransactionEvents(daysToKeep);
            
            log.info("删除过期事务事件成功");
            return Result.success("删除过期事务事件成功", true);
            
        } catch (Exception e) {
            log.error("删除过期事务事件异常: daysToKeep={}, error={}", daysToKeep, e.getMessage(), e);
            return Result.failure("删除过期事务事件异常: " + e.getMessage());
        }
    }

    /**
     * 将Dubbo模型转换为本地模型
     */
    private com.bgpay.bgai.model.TransactionEvent convertToLocalModel(TransactionEvent dubboEvent) {
        return com.bgpay.bgai.model.TransactionEvent.builder()
                .eventId(dubboEvent.getEventId())
                .globalTransactionId(dubboEvent.getGlobalTransactionId())
                .transactionId(dubboEvent.getTransactionId())
                .branchTransactionId(dubboEvent.getBranchTransactionId())
                .status(dubboEvent.getStatus())
                .operationType(dubboEvent.getOperationType())
                .messageType(dubboEvent.getMessageType())
                .serviceName(dubboEvent.getServiceName())
                .businessType(dubboEvent.getBusinessType())
                .businessId(dubboEvent.getBusinessId())
                .errorMessage(dubboEvent.getErrorMessage())
                .source(dubboEvent.getSource())
                .description(dubboEvent.getDescription())
                .extraData(dubboEvent.getExtraData())
                .createTime(dubboEvent.getCreateTime())
                .updateTime(dubboEvent.getUpdateTime())
                .processTime(dubboEvent.getProcessTime())
                .retryCount(dubboEvent.getRetryCount())
                .priority(String.valueOf(dubboEvent.getPriority()))
                .asyncProcess(dubboEvent.getAsyncProcess())
                .callbackUrl(dubboEvent.getCallbackUrl())
                .tags(dubboEvent.getTags() != null ? dubboEvent.getTags().split(",") : null)
                .version(dubboEvent.getVersion())
                .creator(dubboEvent.getCreator())
                .modifier(dubboEvent.getModifier())
                .remark(dubboEvent.getRemark())
                .build();
    }

    /**
     * 将本地模型转换为Dubbo模型
     */
    private TransactionEvent convertToDubboModel(com.bgpay.bgai.model.TransactionEvent localEvent) {
        return TransactionEvent.builder()
                .eventId(localEvent.getEventId())
                .globalTransactionId(localEvent.getGlobalTransactionId())
                .transactionId(localEvent.getTransactionId())
                .branchTransactionId(localEvent.getBranchTransactionId())
                .status(localEvent.getStatus())
                .operationType(localEvent.getOperationType())
                .messageType(localEvent.getMessageType())
                .serviceName(localEvent.getServiceName())
                .businessType(localEvent.getBusinessType())
                .businessId(localEvent.getBusinessId())
                .errorMessage(localEvent.getErrorMessage())
                .source(localEvent.getSource())
                .description(localEvent.getDescription())
                .extraData(localEvent.getExtraData())
                .createTime(localEvent.getCreateTime())
                .updateTime(localEvent.getUpdateTime())
                .processTime(localEvent.getProcessTime())
                .retryCount(localEvent.getRetryCount())
                .priority(Integer.valueOf(localEvent.getPriority()))
                .asyncProcess(localEvent.getAsyncProcess())
                .callbackUrl(localEvent.getCallbackUrl())
                .tags(localEvent.getTags() != null ? String.join(",", localEvent.getTags()) : null)
                .version(localEvent.getVersion())
                .creator(localEvent.getCreator())
                .modifier(localEvent.getModifier())
                .remark(localEvent.getRemark())
                .build();
    }

    /**
     * 将本地响应模型转换为Dubbo响应模型
     */
    private TransactionEventResponse convertToDubboResponse(com.bgpay.bgai.model.TransactionEventResponse localResponse) {
        return TransactionEventResponse.builder()
                .success(localResponse.getSuccess())
                .message(localResponse.getMessage())
                .code(localResponse.getCode())
                .transactionId(localResponse.getTransactionId())
                .eventId(localResponse.getEventId())
                .processTime(localResponse.getProcessTime())
                .errorMessage(localResponse.getErrorMessage())
                .details(localResponse.getDetails())
                .build();
    }
}
