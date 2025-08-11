package com.jiangyang.messages.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息生命周期日志实体
 * 用于记录消息从生产到消费的完整生命周期
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("message_lifecycle_log")
public class MessageLifecycleLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 消息ID (消息中间件生成的唯一ID)
     */
    @TableField("message_id")
    private String messageId;

    /**
     * 业务消息ID (业务方传入的消息ID)
     */
    @TableField("business_message_id")
    private String businessMessageId;

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
     * 消息队列
     */
    @TableField("queue_name")
    private String queueName;

    /**
     * 分区ID (Kafka专用)
     */
    @TableField("partition_id")
    private Integer partitionId;

    /**
     * 偏移量 (Kafka专用)
     */
    @TableField("offset")
    private Long offset;

    /**
     * 生命周期阶段 (PRODUCE, SEND, STORE, CONSUME, ACK, FAIL)
     */
    @TableField("lifecycle_stage")
    private String lifecycleStage;

    /**
     * 阶段状态 (PENDING, PROCESSING, SUCCESS, FAILED, TIMEOUT)
     */
    @TableField("stage_status")
    private String stageStatus;

    /**
     * 生产者服务名
     */
    @TableField("producer_service")
    private String producerService;

    /**
     * 消费者服务名
     */
    @TableField("consumer_service")
    private String consumerService;

    /**
     * 消费者组
     */
    @TableField("consumer_group")
    private String consumerGroup;

    /**
     * 消息大小 (字节)
     */
    @TableField("message_size")
    private Long messageSize;

    /**
     * 消息内容 (可选，大消息可能只存储摘要)
     */
    @TableField("message_content")
    private String messageContent;

    /**
     * 消息摘要 (消息内容的MD5值)
     */
    @TableField("message_digest")
    private String messageDigest;

    /**
     * 消息属性 (JSON格式)
     */
    @TableField("message_properties")
    private String messageProperties;

    /**
     * 延迟级别 (RocketMQ专用)
     */
    @TableField("delay_level")
    private Integer delayLevel;

    /**
     * 延迟时间 (毫秒)
     */
    @TableField("delay_time")
    private Long delayTime;

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
     * 死信队列标识 (0-正常, 1-死信)
     */
    @TableField("dead_letter_flag")
    private Integer deadLetterFlag;

    /**
     * 死信原因
     */
    @TableField("dead_letter_reason")
    private String deadLetterReason;

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
     * 阶段开始时间
     */
    @TableField("stage_start_time")
    private LocalDateTime stageStartTime;

    /**
     * 阶段结束时间
     */
    @TableField("stage_end_time")
    private LocalDateTime stageEndTime;

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
