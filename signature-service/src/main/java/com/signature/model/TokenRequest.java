package com.signature.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * Token请求模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {
    
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
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 用户部门
     */
    private String department;
    
    /**
     * 用户职位
     */
    private String position;
    
    /**
     * 用户手机号
     */
    private String phone;
    
    /**
     * 用户性别
     */
    private String gender;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 是否锁定
     */
    private Boolean locked;
    
    /**
     * Token类型 (ACCESS, REFRESH, BOTH)
     */
    private String tokenType;
    
    /**
     * Token过期时间（秒），如果为null则使用默认配置
     */
    private Long expirationSeconds;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 客户端密钥
     */
    private String clientSecret;
    
    /**
     * 额外信息
     */
    private Map<String, Object> extraInfo;
}
