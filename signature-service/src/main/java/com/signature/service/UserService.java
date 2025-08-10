package com.signature.service;

import com.signature.entity.User;
import com.signature.entity.UserToken;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 通过SSO登录
     * 
     * @param code SSO授权码
     * @return 用户令牌
     */
    UserToken loginWithSSO(String code);
    
    /**
     * 验证令牌有效性
     * 
     * @param accessToken 访问令牌
     * @return 用户令牌，如果无效返回null
     */
    UserToken validateToken(String accessToken);
    
    /**
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户实体
     */
    User getUserInfo(String userId);
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的用户令牌
     */
    UserToken refreshToken(String refreshToken);
    
    /**
     * 根据用户ID刷新token
     * 
     * @param userId 用户ID
     * @return 新的用户令牌
     */
    UserToken refreshTokenByUserId(String userId);
    
    /**
     * 登出
     * 
     * @param accessToken 访问令牌
     */
    void logout(String accessToken);
} 