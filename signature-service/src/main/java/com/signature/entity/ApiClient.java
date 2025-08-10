package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * API客户端实体类
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("api_client")
public class ApiClient implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 客户端ID
     */
    @TableField("client_id")
    private String clientId;

    /**
     * 客户端名称
     */
    @TableField("client_name")
    private String clientName;

    /**
     * 客户端描述
     */
    @TableField("description")
    private String description;

    /**
     * 客户端状态 (0: 禁用, 1: 启用)
     */
    @TableField("status")
    private Integer status;

    /**
     * 客户端类型 (web, mobile, api, etc.)
     */
    @TableField("client_type")
    private String clientType;

    /**
     * 客户端密钥
     */
    @TableField("client_secret")
    private String clientSecret;

    /**
     * 重定向URI
     */
    @TableField("redirect_uri")
    private String redirectUri;

    /**
     * 授权类型 (authorization_code, client_credentials, etc.)
     */
    @TableField("grant_type")
    private String grantType;

    /**
     * 作用域
     */
    @TableField("scope")
    private String scope;

    /**
     * 访问令牌有效期（秒）
     */
    @TableField("access_token_validity")
    private Integer accessTokenValidity;

    /**
     * 刷新令牌有效期（秒）
     */
    @TableField("refresh_token_validity")
    private Integer refreshTokenValidity;

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
