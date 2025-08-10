package com.jiangyang.dubbo.api.auth.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 认证响应DTO
 * 
 * @author jiangyang
 * @version 1.0.0
 */
@Data
public class AuthResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 访问Token
     */
    private String accessToken;
    
    /**
     * 刷新Token
     */
    private String refreshToken;
    
    /**
     * Token类型
     */
    private String tokenType = "Bearer";
    
    /**
     * Token过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 用户信息
     */
    private UserInfo userInfo;
    
    /**
     * 权限列表
     */
    private java.util.List<String> permissions;
    
    /**
     * 角色列表
     */
    private java.util.List<String> roles;
    
    /**
     * 是否首次登录
     */
    private Boolean firstLogin = false;
    
    /**
     * 是否需要修改密码
     */
    private Boolean needChangePassword = false;
    
    /**
     * 登录时间
     */
    private Long loginTime;
    
    /**
     * 登录IP
     */
    private String loginIp;
}
