package com.signature.enums;

/**
 * <p>
 * API密钥状态枚举
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
public enum ApiKeyStatus {
    /**
     * 有效
     */
    VALID,
    
    /**
     * 无效
     */
    INVALID,
    
    /**
     * 已禁用
     */
    DISABLED,
    
    /**
     * 已过期
     */
    EXPIRED,
    
    /**
     * 未找到
     */
    NOT_FOUND,
    
    /**
     * 已撤销
     */
    REVOKED
}
