package com.jiangyang.messages.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事务审计日志实体
 * 用于记录分布式事务的完整生命周期和业务处理轨迹
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("transaction_audit_log")
public class TransactionAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 全局事务ID (Seata XID)
     */
    @TableField("global_transaction_id")
    private String globalTransactionId;

    /**
     * 分支事务ID
     */
    @TableField("branch_transaction_id")
    private String branchTransactionId;

    /**
     * 业务事务ID (业务方传入)
     */
    @TableField("business_transaction_id")
    private String businessTransactionId;

    /**
     * 服务名称
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 操作类型 (SEND, CONSUME, COMPENSATE, etc.)
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 消息类型 (ROCKETMQ, KAFKA, RABBITMQ)
     */
    @TableField("message_type")
    private String messageType;

    /**
     * 消息主题
     */
    @TableField("topic")
    private String topic;

    /**
     * 消息标签
     */
    @TableField("tags")
    private String tags;

    /**
     * 消息键
     */
    @TableField("message_key")
    private String messageKey;

    /**
     * 消息ID
     */
    @TableField("message_id")
    private String messageId;

    /**
     * 事务状态 (BEGIN, SUCCESS, FAILED, COMPENSATED)
     */
    @TableField("transaction_status")
    private String transactionStatus;

    /**
     * 操作状态 (PENDING, PROCESSING, SUCCESS, FAILED)
     */
    @TableField("operation_status")
    private String operationStatus;

    /**
     * 请求参数 (JSON格式)
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 响应结果 (JSON格式)
     */
    @TableField("response_result")
    private String responseResult;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @TableField("max_retry_count")
    private Integer maxRetryCount;

    /**
     * 执行耗时 (毫秒)
     */
    @TableField("execution_time")
    private Long executionTime;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 父操作ID (用于构建操作链)
     */
    @TableField("parent_operation_id")
    private String parentOperationId;

    /**
     * 操作链ID (用于关联同一业务链的所有操作)
     */
    @TableField("operation_chain_id")
    private String operationChainId;

    /**
     * 操作顺序 (在同一链中的顺序)
     */
    @TableField("operation_order")
    private Integer operationOrder;

    /**
     * 业务上下文 (JSON格式，存储业务相关上下文信息)
     */
    @TableField("business_context")
    private String businessContext;

    /**
     * 扩展字段 (JSON格式)
     */
    @TableField("extended_fields")
    private String extendedFields;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 逻辑删除标识 (0-未删除, 1-已删除)
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
