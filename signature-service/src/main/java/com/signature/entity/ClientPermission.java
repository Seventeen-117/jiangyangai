package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 客户端权限实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("client_permission")
public class ClientPermission {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("client_id")
    private String clientId;

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
