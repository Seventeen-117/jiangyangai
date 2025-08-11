package com.jiangyang.messages.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 事务事件模型
 * 用于向 bgai-service 发送事务状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 全局事务ID
     */
    private String globalTransactionId;

    /**
     * 业务事务ID
     */
    private String transactionId;

    /**
     * 分支事务ID
     */
    private String branchTransactionId;

    /**
     * 事务状态
     */
    private String status;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 事件来源
     */
    private String source;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 扩展信息
     */
    private Map<String, Object> extraData;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否异步处理
     */
    private Boolean asyncProcess;

    /**
     * 回调URL
     */
    private String callbackUrl;

    /**
     * 标签
     */
    private String tags;

    /**
     * 版本号
     */
    private String version;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 修改者
     */
    private String modifier;

    /**
     * 备注
     */
    private String remark;

    // ==================== 静态工厂方法 ====================

    /**
     * 创建事务开始事件
     */
    public static TransactionEvent createTransactionBeginEvent(String globalTransactionId, String transactionId, 
                                                           String serviceName, String businessType, String businessId) {
        return TransactionEvent.builder()
                .globalTransactionId(globalTransactionId)
                .transactionId(transactionId)
                .status("BEGIN")
                .operationType("TRANSACTION_BEGIN")
                .serviceName(serviceName)
                .businessType(businessType)
                .businessId(businessId)
                .source("messages-service")
                .description("事务开始")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .retryCount(0)
                .priority(1)
                .asyncProcess(false)
                .version("1.0")
                .creator("system")
                .build();
    }

    /**
     * 创建事务提交事件
     */
    public static TransactionEvent createTransactionCommitEvent(String globalTransactionId, String transactionId, 
                                                            String serviceName, String businessType, String businessId) {
        return TransactionEvent.builder()
                .globalTransactionId(globalTransactionId)
                .transactionId(transactionId)
                .status("COMMITTED")
                .operationType("TRANSACTION_COMMIT")
                .serviceName(serviceName)
                .businessType(businessType)
                .businessId(businessId)
                .source("messages-service")
                .description("事务提交成功")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .retryCount(0)
                .priority(1)
                .asyncProcess(false)
                .version("1.0")
                .creator("system")
                .build();
    }

    /**
     * 创建事务回滚事件
     */
    public static TransactionEvent createTransactionRollbackEvent(String globalTransactionId, String transactionId, 
                                                              String serviceName, String businessType, String businessId, 
                                                              String errorMessage) {
        return TransactionEvent.builder()
                .globalTransactionId(globalTransactionId)
                .transactionId(transactionId)
                .status("ROLLBACK")
                .operationType("TRANSACTION_ROLLBACK")
                .serviceName(serviceName)
                .businessType(businessType)
                .businessId(businessId)
                .errorMessage(errorMessage)
                .source("messages-service")
                .description("事务回滚")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .retryCount(0)
                .priority(2)
                .asyncProcess(false)
                .version("1.0")
                .creator("system")
                .build();
    }

    /**
     * 创建消息发送事件
     */
    public static TransactionEvent createMessageSendEvent(String globalTransactionId, String transactionId, 
                                                       String messageId, String content) {
        return TransactionEvent.builder()
                .globalTransactionId(globalTransactionId)
                .transactionId(transactionId)
                .status("SENT")
                .operationType("MESSAGE_SEND")
                .messageType("text")
                .serviceName("messages-service")
                .businessType("message-send")
                .businessId(messageId)
                .source("messages-service")
                .description("消息发送: " + content)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .retryCount(0)
                .priority(1)
                .asyncProcess(false)
                .version("1.0")
                .creator("system")
                .build();
    }

    /**
     * 创建消息消费事件
     */
    public static TransactionEvent createMessageConsumeEvent(String globalTransactionId, String transactionId, 
                                                          String messageId, String content) {
        return TransactionEvent.builder()
                .globalTransactionId(globalTransactionId)
                .transactionId(transactionId)
                .status("CONSUMED")
                .operationType("MESSAGE_CONSUME")
                .messageType("text")
                .serviceName("messages-service")
                .businessType("message-consume")
                .businessId(messageId)
                .source("messages-service")
                .description("消息消费: " + content)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .retryCount(0)
                .priority(1)
                .asyncProcess(false)
                .version("1.0")
                .creator("system")
                .build();
    }

    /**
     * 创建Saga执行事件
     */
    public static TransactionEvent createSagaExecuteEvent(String globalTransactionId, String transactionId, 
                                                        String businessType, String businessId, String sagaStep) {
        return TransactionEvent.builder()
                .globalTransactionId(globalTransactionId)
                .transactionId(transactionId)
                .status("EXECUTING")
                .operationType("SAGA_EXECUTE")
                .serviceName("messages-service")
                .businessType(businessType)
                .businessId(businessId)
                .source("messages-service")
                .description("Saga执行: " + sagaStep)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .retryCount(0)
                .priority(1)
                .asyncProcess(false)
                .version("1.0")
                .creator("system")
                .build();
    }

    /**
     * 创建Saga补偿事件
     */
    public static TransactionEvent createSagaCompensateEvent(String globalTransactionId, String transactionId, 
                                                           String businessType, String businessId, String sagaStep) {
        return TransactionEvent.builder()
                .globalTransactionId(globalTransactionId)
                .transactionId(transactionId)
                .status("COMPENSATING")
                .operationType("SAGA_COMPENSATE")
                .serviceName("messages-service")
                .businessType(businessType)
                .businessId(businessId)
                .source("messages-service")
                .description("Saga补偿: " + sagaStep)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .retryCount(0)
                .priority(2)
                .asyncProcess(false)
                .version("1.0")
                .creator("system")
                .build();
    }

    // ==================== 工具方法 ====================

    /**
     * 添加扩展数据
     */
    public TransactionEvent addExtraData(String key, Object value) {
        if (this.extraData == null) {
            this.extraData = new HashMap<>();
        }
        this.extraData.put(key, value);
        return this;
    }

    /**
     * 设置处理时间
     */
    public TransactionEvent setProcessTime() {
        this.processTime = LocalDateTime.now();
        return this;
    }

    /**
     * 增加重试次数
     */
    public TransactionEvent incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        return this;
    }

    /**
     * 设置错误信息
     */
    public TransactionEvent setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = "ERROR";
        this.updateTime = LocalDateTime.now();
        return this;
    }

    /**
     * 设置成功状态
     */
    public TransactionEvent setSuccess() {
        this.status = "SUCCESS";
        this.updateTime = LocalDateTime.now();
        return this;
    }
}
