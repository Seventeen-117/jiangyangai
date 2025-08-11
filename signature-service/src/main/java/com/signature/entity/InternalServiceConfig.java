package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 内部服务配置实体类
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("internal_service_config")
public class InternalServiceConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务编码
     */
    @TableField("service_code")
    private String serviceCode;

    /**
     * 服务名称
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 服务类型：MICROSERVICE-微服务，GATEWAY-网关，EXTERNAL-外部服务
     */
    @TableField("service_type")
    private String serviceType;

    /**
     * 请求前缀
     */
    @TableField("request_prefix")
    private String requestPrefix;

    /**
     * API密钥
     */
    @TableField("api_key")
    private String apiKey;

    /**
     * 服务描述
     */
    @TableField("description")
    private String description;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    // ========== 服务类型常量 ==========
    
    public static final String SERVICE_TYPE_MICROSERVICE = "MICROSERVICE";
    public static final String SERVICE_TYPE_GATEWAY = "GATEWAY";
    public static final String SERVICE_TYPE_EXTERNAL = "EXTERNAL";
}