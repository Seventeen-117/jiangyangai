package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 验证规则配置实体类
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("validation_rule_config")
public class ValidationRuleConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 规则编码
     */
    @TableField("rule_code")
    private String ruleCode;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 规则类型：SIGNATURE-签名验证，API_KEY-API密钥验证，AUTHENTICATION-认证验证
     */
    @TableField("rule_type")
    private String ruleType;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @TableField("enabled")
    private Integer enabled;

    /**
     * 严格模式：0-非严格，1-严格
     */
    @TableField("strict_mode")
    private Integer strictMode;

    /**
     * 超时时间（毫秒）
     */
    @TableField("timeout_ms")
    private Integer timeoutMs;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 重试间隔（毫秒）
     */
    @TableField("retry_interval_ms")
    private Integer retryIntervalMs;

    /**
     * 规则描述
     */
    @TableField("description")
    private String description;

    /**
     * 规则配置（JSON格式）
     */
    @TableField("config_json")
    private String configJson;

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

    // ========== 辅助方法 ==========

    /**
     * 检查规则是否启用
     */
    public boolean isEnabled() {
        return enabled != null && enabled == 1 && status != null && status == 1;
    }

    /**
     * 检查是否为严格模式
     */
    public boolean isStrictMode() {
        return strictMode != null && strictMode == 1;
    }

    // ========== 规则类型常量 ==========
    
    public static final String RULE_TYPE_SIGNATURE = "SIGNATURE";
    public static final String RULE_TYPE_API_KEY = "API_KEY";
    public static final String RULE_TYPE_AUTHENTICATION = "AUTHENTICATION";
    public static final String RULE_TYPE_JWT = "JWT";
    public static final String RULE_TYPE_PERMISSION = "PERMISSION";

    // ========== 规则编码常量 ==========
    
    public static final String RULE_CODE_API_KEY_VALIDATION = "API_KEY_VALIDATION";
    public static final String RULE_CODE_SIGNATURE_VALIDATION = "SIGNATURE_VALIDATION";
    public static final String RULE_CODE_AUTHENTICATION_VALIDATION = "AUTHENTICATION_VALIDATION";
    public static final String RULE_CODE_JWT_VALIDATION = "JWT_VALIDATION";
    public static final String RULE_CODE_PERMISSION_VALIDATION = "PERMISSION_VALIDATION";
    public static final String RULE_CODE_API_KEY_AUTH_VALIDATION = "API_KEY_AUTH_VALIDATION";
}