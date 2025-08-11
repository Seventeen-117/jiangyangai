package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * API权限实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("api_permission")
public class ApiPermission {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("permission_code")
    private String permissionCode;

    @TableField("permission_name")
    private String permissionName;

    @TableField("permission_type")
    private String permissionType;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
