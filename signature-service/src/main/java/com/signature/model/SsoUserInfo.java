package com.signature.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SSO 用户信息模型
 */
@Data
public class SsoUserInfo {
    
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
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 用户角色
     */
    private String role;
    
    /**
     * 用户权限列表
     */
    private List<String> permissions;
    
    /**
     * 用户状态
     */
    private String status;
    
    /**
     * 用户创建时间
     */
    private Long createdAt;
    
    /**
     * 用户最后登录时间
     */
    private Long lastLoginAt;
    
    /**
     * 用户最后登录IP
     */
    private String lastLoginIp;
    
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
     * 用户生日
     */
    private String birthday;
    
    /**
     * 用户地址
     */
    private String address;
    
    /**
     * 用户扩展信息
     */
    private Map<String, Object> extraInfo;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 是否锁定
     */
    private Boolean locked;
    
    /**
     * 账户过期时间
     */
    private Long accountExpiredAt;
    
    /**
     * 密码过期时间
     */
    private Long passwordExpiredAt;
} 