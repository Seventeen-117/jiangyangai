package com.jiangyang.messages.saga.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息Saga状态实体
 * 用于记录Saga事务的状态信息和状态转换
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("message_saga_state")
public class MessageSagaState implements Serializable {

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
     * 当前状态
     */
    @TableField("current_state")
    private String currentState;

    /**
     * 上一个状态
     */
    @TableField("previous_state")
    private String previousState;

    /**
     * 目标状态
     */
    @TableField("target_state")
    private String targetState;

    /**
     * 状态机名称
     */
    @TableField("state_machine_name")
    private String stateMachineName;

    /**
     * 状态转换事件
     */
    @TableField("transition_event")
    private String transitionEvent;

    /**
     * 状态转换时间
     */
    @TableField("transition_time")
    private LocalDateTime transitionTime;

    /**
     * 状态转换原因
     */
    @TableField("transition_reason")
    private String transitionReason;

    /**
     * 状态转换参数 (JSON格式)
     */
    @TableField("transition_params")
    private String transitionParams;

    /**
     * 状态转换结果 (SUCCESS, FAILED, TIMEOUT)
     */
    @TableField("transition_result")
    private String transitionResult;

    /**
     * 状态转换耗时 (毫秒)
     */
    @TableField("transition_duration")
    private Long transitionDuration;

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
     * 超时时间 (毫秒)
     */
    @TableField("timeout")
    private Long timeout;

    /**
     * 是否超时 (0-未超时, 1-已超时)
     */
    @TableField("is_timeout")
    private Integer isTimeout;

    /**
     * 超时时间
     */
    @TableField("timeout_time")
    private LocalDateTime timeoutTime;

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
