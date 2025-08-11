package com.signature.service;

import com.signature.model.SsoUserInfo;
import com.signature.model.TokenRequest;
import com.signature.model.TokenResponse;

import java.util.List;
import java.util.Map;

/**
 * Token服务接口
 * 提供Token生成、验证、刷新等功能
 */
public interface TokenService {
    
    /**
     * 生成Token
     * 
     * @param request Token请求
     * @return Token响应
     */
    TokenResponse generateToken(TokenRequest request);
    
    /**
     * 刷新Token
     * 
     * @param refreshToken 刷新令牌
     * @return 新的Token响应
     */
    TokenResponse refreshToken(String refreshToken);
    
    /**
     * 验证Token
     * 
     * @param accessToken 访问令牌
     * @return 用户信息，如果验证失败返回null
     */
    SsoUserInfo validateToken(String accessToken);
    
    /**
     * 撤销Token
     * 
     * @param accessToken 访问令牌
     * @return 是否成功撤销
     */
    boolean revokeToken(String accessToken);
    
    /**
     * 获取Token信息
     * 
     * @param accessToken 访问令牌
     * @return Token信息
     */
    Map<String, Object> getTokenInfo(String accessToken);
    
    /**
     * 批量生成Token
     * 
     * @param requests Token请求列表
     * @return Token响应列表
     */
    List<TokenResponse> batchGenerateTokens(List<TokenRequest> requests);
}
