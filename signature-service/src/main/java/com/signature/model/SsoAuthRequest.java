package com.signature.model;

import lombok.Data;

/**
 * SSO 认证请求模型
 */
@Data
public class SsoAuthRequest {
    
    /**
     * 授权码
     */
    private String code;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 客户端密钥
     */
    private String clientSecret;
    
    /**
     * 重定向URI
     */
    private String redirectUri;
    
    /**
     * 授权类型
     */
    private String grantType;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 状态参数
     */
    private String state;
    
    /**
     * 作用域
     */
    private String scope;
    
    /**
     * 用户名（密码模式）
     */
    private String username;
    
    /**
     * 密码（密码模式）
     */
    private String password;
} 