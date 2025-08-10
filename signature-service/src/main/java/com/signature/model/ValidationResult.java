package com.signature.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 验证结果模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * 验证是否成功
     */
    private boolean valid;

    /**
     * 验证状态码
     */
    private String statusCode;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 过期时间
     */
    private Long expiresAt;

    /**
     * 验证时间
     */
    private Long validatedAt;

    /**
     * 额外信息
     */
    private Map<String, Object> extraInfo;

    /**
     * 创建成功结果
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .statusCode("SUCCESS")
                .validatedAt(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建成功结果（带用户信息）
     */
    public static ValidationResult success(String userId, String username, String role) {
        return ValidationResult.builder()
                .valid(true)
                .statusCode("SUCCESS")
                .userId(userId)
                .username(username)
                .role(role)
                .validatedAt(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ValidationResult failure(String statusCode, String errorMessage) {
        return ValidationResult.builder()
                .valid(false)
                .statusCode(statusCode)
                .errorMessage(errorMessage)
                .validatedAt(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建API Key无效结果
     */
    public static ValidationResult invalidApiKey() {
        return failure("INVALID_API_KEY", "API Key is invalid or expired");
    }

    /**
     * 创建签名无效结果
     */
    public static ValidationResult invalidSignature() {
        return failure("INVALID_SIGNATURE", "Request signature verification failed");
    }

    /**
     * 创建权限不足结果
     */
    public static ValidationResult insufficientPermission() {
        return failure("INSUFFICIENT_PERMISSION", "User does not have required permissions");
    }

    /**
     * 创建Token无效结果
     */
    public static ValidationResult invalidToken() {
        return failure("INVALID_TOKEN", "JWT token is invalid or expired");
    }
}
