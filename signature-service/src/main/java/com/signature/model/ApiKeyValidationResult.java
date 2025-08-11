package com.signature.model;

import com.signature.enums.ApiKeyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p>
 * API密钥验证结果模型
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyValidationResult {
    
    /**
     * API密钥状态
     */
    private ApiKeyStatus status;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 客户端名称
     */
    private String clientName;
    
    /**
     * 验证时间
     */
    private LocalDateTime validationTime;
    
    /**
     * 构造函数
     */
    public ApiKeyValidationResult(ApiKeyStatus status, LocalDateTime expiresAt, String errorMessage, String clientId) {
        this.status = status;
        this.expiresAt = expiresAt;
        this.errorMessage = errorMessage;
        this.clientId = clientId;
        this.validationTime = LocalDateTime.now();
    }
}
