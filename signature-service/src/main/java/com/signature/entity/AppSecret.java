package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 应用密钥实体类
 * 
 * @author bgpay
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("app_secret")
public class AppSecret {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 应用ID
     */
    @TableField("app_id")
    private String appId;

    /**
     * 应用密钥
     */
    @TableField("app_secret")
    private String appSecret;

    /**
     * 应用名称
     */
    @TableField("app_name")
    private String appName;

    /**
     * 应用描述
     */
    @TableField("description")
    private String description;

    /**
     * 状态：1-启用，0-禁用
     */
    @TableField("status")
    private Integer status;

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
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 是否删除：1-已删除，0-未删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
} 