package com.jiangyang.messages.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务轨迹日志实体
 * 用于记录服务间调用链路和业务处理轨迹
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("business_trace_log")
public class BusinessTraceLog implements Serializable {

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
     * 业务事务ID (业务方传入)
     */
    @TableField("business_transaction_id")
    private String businessTransactionId;

    /**
     * 调用链ID (用于关联同一调用链的所有节点)
     */
    @TableField("trace_id")
    private String traceId;

    /**
     * 父节点ID (用于构建调用树)
     */
    @TableField("parent_span_id")
    private String parentSpanId;

    /**
     * 当前节点ID
     */
    @TableField("span_id")
    private String spanId;

    /**
     * 服务名称
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 服务实例ID
     */
    @TableField("service_instance_id")
    private String serviceInstanceId;

    /**
     * 操作名称
     */
    @TableField("operation_name")
    private String operationName;

    /**
     * 操作类型 (HTTP, RPC, MQ, DB, etc.)
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 调用方向 (INBOUND, OUTBOUND, INTERNAL)
     */
    @TableField("call_direction")
    private String callDirection;

    /**
     * 目标服务名
     */
    @TableField("target_service")
    private String targetService;

    /**
     * 目标方法
     */
    @TableField("target_method")
    private String targetMethod;

    /**
     * 请求URL (HTTP调用)
     */
    @TableField("request_url")
    private String requestUrl;

    /**
     * 请求方法 (GET, POST, etc.)
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求参数 (JSON格式)
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 请求头 (JSON格式)
     */
    @TableField("request_headers")
    private String requestHeaders;

    /**
     * 响应结果 (JSON格式)
     */
    @TableField("response_result")
    private String responseResult;

    /**
     * 响应状态码
     */
    @TableField("response_status")
    private Integer responseStatus;

    /**
     * 调用状态 (PENDING, PROCESSING, SUCCESS, FAILED, TIMEOUT)
     */
    @TableField("call_status")
    private String callStatus;

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
     * 调用耗时 (毫秒)
     */
    @TableField("call_duration")
    private Long callDuration;

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
     * 业务上下文 (JSON格式)
     */
    @TableField("business_context")
    private String businessContext;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * IP地址
     */
    @TableField("client_ip")
    private String clientIp;

    /**
     * 用户代理
     */
    @TableField("user_agent")
    private String userAgent;

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
