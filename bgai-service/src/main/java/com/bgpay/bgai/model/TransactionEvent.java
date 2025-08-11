package com.bgpay.bgai.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事务事件模型
 * 用于接收来自 messages-service 的事务状态信息
 */
@Data
@Builder
@AllArgsConstructor
@Schema(description = "事务事件模型")
public class TransactionEvent {

    @Schema(description = "事件ID", example = "evt_123456789")
    private String eventId;

    @Schema(description = "全局事务ID", example = "192.168.1.100:8091:123456789")
    private String globalTransactionId;

    @Schema(description = "业务事务ID", example = "txn_123456789")
    private String transactionId;

    @Schema(description = "分支事务ID", example = "branch_123456789")
    private String branchTransactionId;

    @Schema(description = "事务状态", example = "BEGIN", allowableValues = {"BEGIN", "PROCESSING", "SUCCESS", "FAILED", "ROLLBACK"})
    private String status;

    @Schema(description = "操作状态", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED", "PROCESSING", "TIMEOUT"})
    private String operationStatus;

    @Schema(description = "服务名称", example = "messages-service")
    private String serviceName;

    @Schema(description = "操作类型", example = "TRANSACTION_BEGIN", allowableValues = {
            "TRANSACTION_BEGIN", "TRANSACTION_COMMIT", "TRANSACTION_ROLLBACK",
            "MESSAGE_SEND", "MESSAGE_CONSUME", "MESSAGE_CONFIRM",
            "SAGA_EXECUTE", "SAGA_COMPENSATE"
    })
    private String operationType;

    @Schema(description = "消息类型", example = "ORDER_CREATE", allowableValues = {
            "ORDER_CREATE", "PAYMENT_CONFIRM", "INVENTORY_UPDATE", "USER_NOTIFICATION"
    })
    private String messageType;

    @Schema(description = "操作链ID", example = "chain_123456789")
    private String operationChainId;

    @Schema(description = "请求参数", example = "{\"orderId\":\"123\",\"amount\":100.00}")
    private String requestParams;

    @Schema(description = "响应结果", example = "{\"success\":true,\"orderId\":\"123\"}")
    private String responseResult;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "执行时间（毫秒）", example = "150")
    private Long executionTime;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "扩展字段")
    private Map<String, Object> extraData;

    @Schema(description = "事件来源", example = "messages-service")
    private String source;

    @Schema(description = "事件版本", example = "1.0")
    private String version;

    @Schema(description = "事件优先级", example = "HIGH", allowableValues = {"LOW", "NORMAL", "HIGH", "URGENT"})
    private String priority;

    @Schema(description = "是否重试", example = "false")
    private Boolean isRetry;

    @Schema(description = "重试次数", example = "0")
    private Integer retryCount;

    @Schema(description = "最大重试次数", example = "3")
    private Integer maxRetryCount;

    @Schema(description = "事件标签", example = "[\"order\",\"payment\",\"critical\"]")
    private String[] tags;

    @Schema(description = "业务数据")
    private Map<String, Object> businessData;

    // 新增字段，与Dubbo模型保持一致
    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务ID")
    private String businessId;

    @Schema(description = "事件描述")
    private String description;

    @Schema(description = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processTime;

    @Schema(description = "是否异步处理")
    private Boolean asyncProcess;

    @Schema(description = "回调URL")
    private String callbackUrl;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "修改者")
    private String modifier;

    @Schema(description = "备注")
    private String remark;

    /**
     * 构造函数
     */
    public TransactionEvent() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.isRetry = false;
        this.retryCount = 0;
        this.maxRetryCount = 3;
        this.priority = "NORMAL";
        this.version = "1.0";
        this.asyncProcess = false;
    }

    /**
     * 创建事务开始事件
     */
    public static TransactionEvent createBeginEvent(String transactionId, String serviceName) {
        return TransactionEvent.builder()
                .transactionId(transactionId)
                .serviceName(serviceName)
                .status("BEGIN")
                .operationType("TRANSACTION_BEGIN")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建事务提交事件
     */
    public static TransactionEvent createCommitEvent(String transactionId, String serviceName) {
        return TransactionEvent.builder()
                .transactionId(transactionId)
                .serviceName(serviceName)
                .status("SUCCESS")
                .operationType("TRANSACTION_COMMIT")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建事务回滚事件
     */
    public static TransactionEvent createRollbackEvent(String transactionId, String serviceName, String errorMessage) {
        return TransactionEvent.builder()
                .transactionId(transactionId)
                .serviceName(serviceName)
                .status("ROLLBACK")
                .operationType("TRANSACTION_ROLLBACK")
                .errorMessage(errorMessage)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建消息发送事件
     */
    public static TransactionEvent createMessageSendEvent(String messageId, String messageType, String serviceName) {
        return TransactionEvent.builder()
                .eventId(messageId)
                .messageType(messageType)
                .serviceName(serviceName)
                .status("PROCESSING")
                .operationType("MESSAGE_SEND")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建消息消费事件
     */
    public static TransactionEvent createMessageConsumeEvent(String messageId, String messageType, String serviceName) {
        return TransactionEvent.builder()
                .eventId(messageId)
                .messageType(messageType)
                .serviceName(serviceName)
                .status("PROCESSING")
                .operationType("MESSAGE_CONSUME")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 设置执行时间（从开始时间计算）
     */
    public void setExecutionTimeFromStart() {
        if (this.startTime != null && this.endTime != null) {
            this.executionTime = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    /**
     * 标记为成功
     */
    public void markAsSuccess() {
        this.status = "SUCCESS";
        this.operationStatus = "SUCCESS";
        this.endTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.setExecutionTimeFromStart();
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.operationStatus = "FAILED";
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.setExecutionTimeFromStart();
    }

    /**
     * 添加标签
     */
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new String[0];
        }
        String[] newTags = new String[this.tags.length + 1];
        System.arraycopy(this.tags, 0, newTags, 0, this.tags.length);
        newTags[this.tags.length] = tag;
        this.tags = newTags;
    }

    /**
     * 添加扩展数据
     */
    public void addExtraData(String key, Object value) {
        if (this.extraData == null) {
            this.extraData = new java.util.HashMap<>();
        }
        this.extraData.put(key, value);
    }

    /**
     * 添加业务数据
     */
    public void addBusinessData(String key, Object value) {
        if (this.businessData == null) {
            this.businessData = new java.util.HashMap<>();
        }
        this.businessData.put(key, value);
    }
}
