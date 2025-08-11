package com.signature.model;

import lombok.Data;

/**
 * SSO 认证响应模型
 */
@Data
public class SsoAuthResponse {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型
     */
    private String tokenType;
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 作用域
     */
    private String scope;
    
    /**
     * 状态参数
     */
    private String state;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 错误描述
     */
    private String errorDescription;
    
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
     * 创建时间
     */
    private Long createdAt;
    
    /**
     * 令牌ID
     */
    private String tokenId;
} 