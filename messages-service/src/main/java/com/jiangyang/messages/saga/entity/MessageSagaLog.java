package com.jiangyang.messages.saga.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息Saga日志实体
 * 用于记录Saga事务的执行日志和状态
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("message_saga_log")
public class MessageSagaLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 业务ID (消息ID、批次ID、事务ID等)
     */
    @TableField("business_id")
    private String businessId;

    /**
     * 操作类型 (SEND, CONFIRM, RECEIVE, BUSINESS_PROCESS, etc.)
     */
    @TableField("operation")
    private String operation;

    /**
     * 操作状态 (PROCESSING, SUCCESS, FAILED, COMPENSATED)
     */
    @TableField("status")
    private String status;

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
     * 操作开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 操作结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 处理耗时 (毫秒)
     */
    @TableField("processing_time")
    private Long processingTime;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 错误堆栈
     */
    @TableField("error_stack_trace")
    private String errorStackTrace;

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
     * 补偿状态 (0-未补偿, 1-已补偿, 2-补偿失败)
     */
    @TableField("compensate_status")
    private Integer compensateStatus;

    /**
     * 补偿时间
     */
    @TableField("compensate_time")
    private LocalDateTime compensateTime;

    /**
     * 补偿次数
     */
    @TableField("compensate_count")
    private Integer compensateCount;

    /**
     * 最大补偿次数
     */
    @TableField("max_compensate_count")
    private Integer maxCompensateCount;

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
     * 业务上下文 (JSON格式)
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
