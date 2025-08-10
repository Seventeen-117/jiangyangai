package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * API密钥权限关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("api_key_permission")
public class ApiKeyPermission {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("api_key_id")
    private Long apiKeyId;

    @TableField("permission_id")
    private Long permissionId;

    @TableField("granted_at")
    private LocalDateTime grantedAt;

    @TableField("granted_by")
    private String grantedBy;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
