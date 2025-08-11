package com.jiangyang.dubbo.api.auth;

import com.jiangyang.dubbo.api.auth.dto.*;
import com.jiangyang.dubbo.api.common.Result;

/**
 * 认证服务 Dubbo 接口
 * 
 * @author jiangyang
 * @version 1.0.0
 */
public interface AuthService {
    
    /**
     * 验证API密钥
     * 
     * @param apiKey API密钥
     * @return 验证结果
     */
    Result<AuthResponse> validateApiKey(String apiKey);
    
    /**
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    Result<UserInfo> getUserInfo(String userId);
    
    /**
     * 验证JWT Token
     * 
     * @param token JWT Token
     * @return 验证结果
     */
    Result<AuthResponse> validateToken(String token);
    
    /**
     * 刷新Token
     * 
     * @param refreshToken 刷新Token
     * @return 新的Token信息
     */
    Result<AuthResponse> refreshToken(String refreshToken);
    
    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录结果
     */
    Result<AuthResponse> login(AuthRequest request);
    
    /**
     * 用户登出
     * 
     * @param token 用户Token
     * @return 登出结果
     */
    Result<Boolean> logout(String token);
}
