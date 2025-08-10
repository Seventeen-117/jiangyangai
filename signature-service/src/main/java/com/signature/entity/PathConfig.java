package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 路径配置实体类
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("path_config")
public class PathConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 路径模式（支持Ant风格）
     */
    @TableField("path_pattern")
    private String pathPattern;

    /**
     * 路径名称
     */
    @TableField("path_name")
    private String pathName;

    /**
     * 路径类型：EXCLUDED-排除路径，STRICT-严格验证路径，INTERNAL-内部路径
     */
    @TableField("path_type")
    private String pathType;

    /**
     * HTTP方法（GET,POST,PUT,DELETE等，多个用逗号分隔）
     */
    @TableField("http_methods")
    private String httpMethods;

    /**
     * 路径描述
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

    // ========== 辅助方法 ==========

    /**
     * 获取HTTP方法列表
     */
    public List<String> getHttpMethodList() {
        if (httpMethods == null || httpMethods.trim().isEmpty()) {
            return Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        }
        return Arrays.asList(httpMethods.split(","));
    }

    /**
     * 检查是否匹配指定的HTTP方法
     */
    public boolean matchesHttpMethod(String method) {
        if (httpMethods == null || httpMethods.trim().isEmpty()) {
            return true; // 如果没有指定方法，则匹配所有方法
        }
        return getHttpMethodList().contains(method.toUpperCase());
    }

    // ========== 路径类型常量 ==========
    
    public static final String PATH_TYPE_EXCLUDED = "EXCLUDED";
    public static final String PATH_TYPE_STRICT = "STRICT";
    public static final String PATH_TYPE_INTERNAL = "INTERNAL";
    public static final String PATH_TYPE_API_KEY_EXCLUDED = "API_KEY_EXCLUDED";
    public static final String PATH_TYPE_JWT_EXCLUDED = "JWT_EXCLUDED";
    public static final String PATH_TYPE_PERMISSION_EXCLUDED = "PERMISSION_EXCLUDED";
}