package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 授权码实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("authorization_code")
public class AuthorizationCode {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("code")
    private String code;

    @TableField("client_id")
    private String clientId;

    @TableField("user_id")
    private String userId;

    @TableField("redirect_uri")
    private String redirectUri;

    @TableField("scope")
    private String scope;

    @TableField("state")
    private String state;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("used")
    private Boolean used;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
