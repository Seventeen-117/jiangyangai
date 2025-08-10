package com.signature.service.impl;

import com.signature.model.SsoUserInfo;
import com.signature.model.TokenRequest;
import com.signature.model.TokenResponse;
import com.signature.service.TokenService;
import com.signature.service.TokenBlacklistService;
import com.signature.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Token服务实现类
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Value("${sso.security.jwt-expiration:3600}")
    private Long defaultJwtExpiration;

    @Value("${sso.security.refresh-token-expiration:86400}")
    private Long defaultRefreshTokenExpiration;

    @Override
    public TokenResponse generateToken(TokenRequest request) {
        log.info("开始生成Token: userId={}, username={}", request.getUserId(), request.getUsername());
        
        try {
            // 验证请求参数
            validateTokenRequest(request);
            
            // 构建用户信息
            SsoUserInfo userInfo = buildUserInfo(request);
            
            // 确定Token类型
            String tokenType = determineTokenType(request.getTokenType());
            
            // 确定过期时间
            Long expirationSeconds = request.getExpirationSeconds() != null ? 
                    request.getExpirationSeconds() : defaultJwtExpiration;
            
            TokenResponse.TokenResponseBuilder responseBuilder = TokenResponse.builder()
                    .success(true)
                    .userId(userInfo.getUserId())
                    .username(userInfo.getUsername())
                    .tokenType(tokenType)
                    .expiresIn(expirationSeconds)
                    .expiresAt(System.currentTimeMillis() + (expirationSeconds * 1000))
                    .tokenTypeHeader("Bearer");
            
            // 根据Token类型生成相应的Token
            switch (tokenType.toUpperCase()) {
                case "ACCESS":
                    String accessToken = jwtUtils.generateAccessToken(userInfo);
                    responseBuilder.accessToken(accessToken);
                    break;
                    
                case "REFRESH":
                    String refreshToken = jwtUtils.generateRefreshToken(userInfo);
                    responseBuilder.refreshToken(refreshToken);
                    break;
                    
                case "BOTH":
                default:
                    String accessTokenBoth = jwtUtils.generateAccessToken(userInfo);
                    String refreshTokenBoth = jwtUtils.generateRefreshToken(userInfo);
                    responseBuilder.accessToken(accessTokenBoth)
                               .refreshToken(refreshTokenBoth);
                    break;
            }
            
            TokenResponse response = responseBuilder.build();
            log.info("Token生成成功: userId={}, tokenType={}", userInfo.getUserId(), tokenType);
            return response;
            
        } catch (Exception e) {
            log.error("Token生成失败: userId={}", request.getUserId(), e);
            return TokenResponse.builder()
                    .success(false)
                    .errorMessage("Token生成失败: " + e.getMessage())
                    .errorCode("TOKEN_GENERATION_FAILED")
                    .build();
        }
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        log.info("开始刷新Token");
        
        try {
            if (!StringUtils.hasText(refreshToken)) {
                throw new IllegalArgumentException("Refresh token不能为空");
            }
            
            // 验证刷新令牌
            SsoUserInfo userInfo = jwtUtils.validateRefreshToken(refreshToken);
            if (userInfo == null) {
                throw new IllegalArgumentException("无效的刷新令牌");
            }
            
            // 检查令牌是否在黑名单中
            if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
                throw new IllegalArgumentException("刷新令牌已被撤销");
            }
            
            // 生成新的访问令牌
            String newAccessToken = jwtUtils.generateAccessToken(userInfo);
            String newRefreshToken = jwtUtils.generateRefreshToken(userInfo);
            
            // 将旧的刷新令牌加入黑名单
            tokenBlacklistService.blacklistToken(refreshToken);
            
            TokenResponse response = TokenResponse.builder()
                    .success(true)
                    .userId(userInfo.getUserId())
                    .username(userInfo.getUsername())
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("BOTH")
                    .expiresIn(defaultJwtExpiration)
                    .expiresAt(System.currentTimeMillis() + (defaultJwtExpiration * 1000))
                    .tokenTypeHeader("Bearer")
                    .build();
            
            log.info("Token刷新成功: userId={}", userInfo.getUserId());
            return response;
            
        } catch (Exception e) {
            log.error("Token刷新失败", e);
            return TokenResponse.builder()
                    .success(false)
                    .errorMessage("Token刷新失败: " + e.getMessage())
                    .errorCode("TOKEN_REFRESH_FAILED")
                    .build();
        }
    }

    @Override
    public SsoUserInfo validateToken(String accessToken) {
        log.debug("开始验证Token");
        
        try {
            if (!StringUtils.hasText(accessToken)) {
                log.warn("Access token为空");
                return null;
            }
            
            // 检查令牌是否在黑名单中
            if (tokenBlacklistService.isTokenBlacklisted(accessToken)) {
                log.warn("Access token在黑名单中");
                return null;
            }
            
            // 验证访问令牌
            SsoUserInfo userInfo = jwtUtils.validateAccessToken(accessToken);
            if (userInfo != null) {
                log.debug("Token验证成功: userId={}", userInfo.getUserId());
            } else {
                log.warn("Token验证失败: 无效或过期");
            }
            
            return userInfo;
            
        } catch (Exception e) {
            log.error("Token验证异常", e);
            return null;
        }
    }

    @Override
    public boolean revokeToken(String accessToken) {
        log.info("开始撤销Token");
        
        try {
            if (!StringUtils.hasText(accessToken)) {
                log.warn("Access token为空，无法撤销");
                return false;
            }
            
            // 验证令牌有效性
            SsoUserInfo userInfo = jwtUtils.validateAccessToken(accessToken);
            if (userInfo == null) {
                log.warn("无效的access token，无法撤销");
                return false;
            }
            
            // 将令牌加入黑名单
            tokenBlacklistService.blacklistToken(accessToken);
            
            log.info("Token撤销成功: userId={}", userInfo.getUserId());
            return true;
            
        } catch (Exception e) {
            log.error("Token撤销失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getTokenInfo(String accessToken) {
        log.debug("开始获取Token信息");
        
        try {
            if (!StringUtils.hasText(accessToken)) {
                log.warn("Access token为空");
                return null;
            }
            
            // 验证令牌
            SsoUserInfo userInfo = jwtUtils.validateAccessToken(accessToken);
            if (userInfo == null) {
                log.warn("无效的access token");
                return null;
            }
            
            // 检查令牌是否在黑名单中
            boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(accessToken);
            
            // 获取令牌过期时间
            Date expirationDate = jwtUtils.getExpirationDate(accessToken);
            long remainingTime = jwtUtils.getRemainingTime(accessToken);
            
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("userId", userInfo.getUserId());
            tokenInfo.put("username", userInfo.getUsername());
            tokenInfo.put("email", userInfo.getEmail());
            tokenInfo.put("role", userInfo.getRole());
            tokenInfo.put("expirationDate", expirationDate);
            tokenInfo.put("remainingTime", remainingTime);
            tokenInfo.put("isBlacklisted", isBlacklisted);
            tokenInfo.put("isExpiringSoon", jwtUtils.isTokenExpiringSoon(accessToken, 300)); // 5分钟内过期
            
            log.debug("Token信息获取成功: userId={}", userInfo.getUserId());
            return tokenInfo;
            
        } catch (Exception e) {
            log.error("获取Token信息失败", e);
            return null;
        }
    }

    @Override
    public List<TokenResponse> batchGenerateTokens(List<TokenRequest> requests) {
        log.info("开始批量生成Token: {} 个请求", requests.size());
        
        try {
            // 使用并行流处理批量请求
            List<CompletableFuture<TokenResponse>> futures = requests.stream()
                    .map(request -> CompletableFuture.supplyAsync(() -> generateToken(request)))
                    .collect(Collectors.toList());
            
            // 等待所有请求完成
            List<TokenResponse> responses = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            
            log.info("批量Token生成完成: {} 个成功", 
                    responses.stream().filter(TokenResponse::isSuccess).count());
            return responses;
            
        } catch (Exception e) {
            log.error("批量Token生成失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 验证Token请求参数
     */
    private void validateTokenRequest(TokenRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Token请求不能为空");
        }
        
        if (!StringUtils.hasText(request.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
    }

    /**
     * 构建用户信息
     */
    private SsoUserInfo buildUserInfo(TokenRequest request) {
        SsoUserInfo userInfo = new SsoUserInfo();
        userInfo.setUserId(request.getUserId());
        userInfo.setUsername(request.getUsername());
        userInfo.setEmail(request.getEmail());
        userInfo.setRole(request.getRole() != null ? request.getRole() : "USER");
        userInfo.setNickname(request.getNickname());
        userInfo.setAvatar(request.getAvatar());
        userInfo.setDepartment(request.getDepartment());
        userInfo.setPosition(request.getPosition());
        userInfo.setPhone(request.getPhone());
        userInfo.setGender(request.getGender());
        userInfo.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        userInfo.setLocked(request.getLocked() != null ? request.getLocked() : false);
        
        return userInfo;
    }

    /**
     * 确定Token类型
     */
    private String determineTokenType(String tokenType) {
        if (tokenType == null || tokenType.trim().isEmpty()) {
            return "BOTH";
        }
        
        String upperTokenType = tokenType.toUpperCase();
        if ("ACCESS".equals(upperTokenType) || "REFRESH".equals(upperTokenType) || "BOTH".equals(upperTokenType)) {
            return upperTokenType;
        }
        
        log.warn("未知的Token类型: {}，使用默认类型BOTH", tokenType);
        return "BOTH";
    }
}
