package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * OAuth客户端实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("oauth_client")
public class OAuthClient {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("client_id")
    private String clientId;

    @TableField("client_secret")
    private String clientSecret;

    @TableField("client_name")
    private String clientName;

    @TableField("redirect_uri")
    private String redirectUri;

    @TableField("scope")
    private String scope;

    @TableField("grant_types")
    private String grantTypes;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
