package com.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * SSO用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sso_user")
public class SsoUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("email")
    private String email;

    @TableField("nickname")
    private String nickname;

    @TableField("avatar")
    private String avatar;

    @TableField("role")
    private String role;

    @TableField("department")
    private String department;

    @TableField("position")
    private String position;

    @TableField("phone")
    private String phone;

    @TableField("gender")
    private String gender;

    @TableField("status")
    private String status;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("locked")
    private Boolean locked;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
