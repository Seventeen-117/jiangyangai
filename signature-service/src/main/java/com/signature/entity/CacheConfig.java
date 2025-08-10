package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 缓存配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cache_config")
public class CacheConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("cache_key")
    private String cacheKey;

    @TableField("cache_name")
    private String cacheName;

    @TableField("cache_type")
    private String cacheType;

    @TableField("expire_seconds")
    private Integer expireSeconds;

    @TableField("max_size")
    private Integer maxSize;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
