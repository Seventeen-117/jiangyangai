package com.jiangyang.messages.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息消费配置实体
 * 用于配置其他服务的消息消费参数
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message_consumer_config")
public class MessageConsumerConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务实例ID
     */
    private String instanceId;

    /**
     * 消息中间件类型：ROCKETMQ、KAFKA、RABBITMQ
     */
    private String messageQueueType;

    /**
     * 消费模式：PUSH（推模式）、PULL（拉模式）
     */
    private String consumeMode;

    /**
     * 消费类型：CLUSTERING（集群消费）、BROADCASTING（广播消费）
     */
    private String consumeType;

    /**
     * 顺序性：CONCURRENT（并发消费）、ORDERLY（顺序消费）
     */
    private String consumeOrder;

    /**
     * 主题/队列名称
     */
    private String topic;

    /**
     * 标签（RocketMQ专用）
     */
    private String tag;

    /**
     * 消费组名称
     */
    private String consumerGroup;

    /**
     * 分区键（Kafka专用）
     */
    private String partitionKey;

    /**
     * 交换机名称（RabbitMQ专用）
     */
    private String exchange;

    /**
     * 路由键（RabbitMQ专用）
     */
    private String routingKey;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 消费间隔（毫秒，拉模式专用）
     */
    private Long consumeInterval;

    /**
     * 批量消费大小
     */
    private Integer batchSize;

    /**
     * 最大重试次数
     */
    private Integer maxRetryTimes;

    /**
     * 超时时间（毫秒）
     */
    private Long timeout;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;
}
