package com.jiangyang.dubbo.api.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 认证请求DTO
 * 
 * @author jiangyang
 * @version 1.0.0
 */
@Data
public class AuthRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 登录类型（password, sms, email等）
     */
    private String loginType = "password";
    
    /**
     * 验证码
     */
    private String captcha;
    
    /**
     * 验证码Key
     */
    private String captchaKey;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 记住我
     */
    private Boolean rememberMe = false;
}
