package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * API配置实体类
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("api_config")
public class ApiConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * API配置名称
     */
    @TableField("config_name")
    private String configName;

    /**
     * API类型 (openai, azure, anthropic, etc.)
     */
    @TableField("api_type")
    private String apiType;

    /**
     * 模型类型 (gpt-3.5-turbo, gpt-4, claude-3, etc.)
     */
    @TableField("model_type")
    private String modelType;

    /**
     * 模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * API基础URL
     */
    @TableField("api_url")
    private String apiUrl;

    /**
     * API密钥
     */
    @TableField("api_key")
    private String apiKey;

    /**
     * 组织ID (OpenAI)
     */
    @TableField("organization_id")
    private String organizationId;

    /**
     * 项目ID (Azure)
     */
    @TableField("project_id")
    private String projectId;

    /**
     * 部署名称 (Azure)
     */
    @TableField("deployment_name")
    private String deploymentName;

    /**
     * API版本
     */
    @TableField("api_version")
    private String apiVersion;

    /**
     * 最大令牌数
     */
    @TableField("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数
     */
    @TableField("temperature")
    private Double temperature;

    /**
     * 优先级 (数字越小优先级越高)
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 是否默认配置
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 配置描述
     */
    @TableField("description")
    private String description;

    /**
     * 扩展配置 (JSON格式)
     */
    @TableField("extra_config")
    private String extraConfig;

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
     * 创建者
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新者
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 是否删除 (逻辑删除)
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;

    /**
     * 版本号 (乐观锁)
     */
    @Version
    @TableField("version")
    private Integer version;
}
