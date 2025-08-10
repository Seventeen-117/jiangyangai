package com.signature.service;

import com.signature.entity.AuthorizationCode;
import com.signature.entity.OAuthClient;
import com.signature.entity.SsoUser;
import com.signature.service.AuthorizationCodeService;
import com.signature.service.OAuthClientService;
import com.signature.service.SsoUserService;
import com.signature.model.SsoAuthRequest;
import com.signature.model.SsoAuthResponse;
import com.signature.model.SsoUserInfo;
import com.signature.utils.JwtUtils;
import com.signature.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SSO服务类
 * 处理单点登录相关的业务逻辑
 */
@Slf4j
@Service
public class SsoService {

    @Value("${sso.client-id}")
    private String clientId;

    @Value("${sso.client-secret}")
    private String clientSecret;

    @Value("${sso.redirect-uri}")
    private String redirectUri;

    @Value("${sso.authorize-url}")
    private String authorizeUrl;

    @Value("${sso.token-url}")
    private String tokenUrl;

    @Value("${sso.user-info-url}")
    private String userInfoUrl;

    @Value("${sso.logout-url}")
    private String logoutUrl;

    @Value("${sso.security.jwt-secret}")
    private String jwtSecret;

    @Value("${sso.security.jwt-expiration}")
    private Long jwtExpiration;

    @Value("${sso.security.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private AuthorizationCodeService authorizationCodeService;

    @Autowired
    private OAuthClientService oAuthClientService;

    @Autowired
    private SsoUserService ssoUserService;

    @Autowired
    private PasswordUtils passwordUtils;

    /**
     * 构建授权URL
     */
    public String buildAuthorizeUrl(String redirectUri, String state, HttpServletRequest request) {
        String finalRedirectUri = redirectUri != null ? redirectUri : this.redirectUri;
        String finalState = state != null ? state : generateState();
        
        return UriComponentsBuilder.fromHttpUrl(authorizeUrl)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", finalRedirectUri)
                .queryParam("state", finalState)
                .queryParam("scope", "openid profile email")
                .build()
                .toUriString();
    }

    /**
     * 处理授权回调
     */
    public SsoAuthResponse handleCallback(String code, String state, HttpServletRequest request) {
        log.info("Handling SSO callback with code: {}, state: {}", code, state);
        
        // 验证授权码
        if (!validateAuthorizationCode(code)) {
            throw new RuntimeException("Invalid authorization code");
        }
        
        // 交换访问令牌
        SsoAuthResponse authResponse = exchangeToken(code);
        
        // 获取用户信息
        SsoUserInfo userInfo = getUserInfoByCode(code);
        authResponse.setUserId(userInfo.getUserId());
        authResponse.setUsername(userInfo.getUsername());
        authResponse.setEmail(userInfo.getEmail());
        authResponse.setRole(userInfo.getRole());
        
        // 生成JWT令牌
        String jwtToken = generateJwtToken(userInfo);
        authResponse.setAccessToken(jwtToken);
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(jwtExpiration / 1000);
        
        // 生成刷新令牌
        String refreshToken = generateRefreshToken(userInfo);
        authResponse.setRefreshToken(refreshToken);
        
        log.info("SSO callback completed successfully for user: {}", userInfo.getUsername());
        return authResponse;
    }

    /**
     * 获取访问令牌
     */
    public SsoAuthResponse getToken(SsoAuthRequest authRequest) {
        log.info("Getting token for client: {}", authRequest.getClientId());
        
        // 验证客户端凭据
        if (!validateClientCredentials(authRequest.getClientId(), authRequest.getClientSecret())) {
            throw new RuntimeException("Invalid client credentials");
        }
        
        // 根据授权类型处理
        switch (authRequest.getGrantType()) {
            case "authorization_code":
                return handleAuthorizationCodeGrant(authRequest);
            case "refresh_token":
                return handleRefreshTokenGrant(authRequest);
            case "password":
                return handlePasswordGrant(authRequest);
            default:
                throw new RuntimeException("Unsupported grant type: " + authRequest.getGrantType());
        }
    }

    /**
     * 刷新访问令牌
     */
    public SsoAuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");
        
        // 验证刷新令牌
        SsoUserInfo userInfo = validateRefreshToken(refreshToken);
        if (userInfo == null) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        // 生成新的访问令牌
        String newAccessToken = generateJwtToken(userInfo);
        String newRefreshToken = generateRefreshToken(userInfo);
        
        SsoAuthResponse response = new SsoAuthResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtExpiration / 1000);
        response.setUserId(userInfo.getUserId());
        response.setUsername(userInfo.getUsername());
        response.setEmail(userInfo.getEmail());
        response.setRole(userInfo.getRole());
        
        return response;
    }

    /**
     * 获取用户信息
     */
    public SsoUserInfo getUserInfo(String authorization) {
        String token = extractToken(authorization);
        if (token == null) {
            throw new RuntimeException("Invalid authorization header");
        }
        
        // 验证JWT令牌
        SsoUserInfo userInfo = validateJwtToken(token);
        if (userInfo == null) {
            throw new RuntimeException("Invalid access token");
        }
        
        return userInfo;
    }

    /**
     * 验证令牌
     */
    public boolean verifyToken(String accessToken) {
        try {
            SsoUserInfo userInfo = validateJwtToken(accessToken);
            return userInfo != null;
        } catch (Exception e) {
            log.warn("Token verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 注销登录
     */
    public void logout(String accessToken, HttpServletRequest request, HttpServletResponse response) {
        log.info("Logging out user");
        
        // 清除会话
        clearSession(request);
        
        // 清除Cookie
        clearAuthCookies(response);
        
        // 如果提供了访问令牌，将其加入黑名单
        if (accessToken != null) {
            blacklistToken(accessToken);
        }
    }

    /**
     * 设置认证Cookie
     */
    public void setAuthCookies(HttpServletResponse response, SsoAuthResponse authResponse) {
        // 设置访问令牌Cookie
        Cookie accessTokenCookie = new Cookie("access_token", authResponse.getAccessToken());
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false); // 生产环境应该设置为true
        accessTokenCookie.setMaxAge(authResponse.getExpiresIn().intValue());
        response.addCookie(accessTokenCookie);
        
        // 设置刷新令牌Cookie
        Cookie refreshTokenCookie = new Cookie("refresh_token", authResponse.getRefreshToken());
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // 生产环境应该设置为true
        refreshTokenCookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        response.addCookie(refreshTokenCookie);
    }

    /**
     * 获取SSO配置信息
     */
    public Map<String, Object> getSsoConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("client_id", clientId);
        config.put("authorize_url", authorizeUrl);
        config.put("token_url", tokenUrl);
        config.put("user_info_url", userInfoUrl);
        config.put("logout_url", logoutUrl);
        config.put("redirect_uri", redirectUri);
        return config;
    }

    /**
     * 获取会话状态
     */
    public Map<String, Object> getSessionStatus(HttpServletRequest request) {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        // 从Cookie中获取令牌
        Cookie[] cookies = request.getCookies();
        String accessToken = null;
        String refreshToken = null;
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                } else if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }
        
        sessionInfo.put("has_access_token", accessToken != null);
        sessionInfo.put("has_refresh_token", refreshToken != null);
        
        if (accessToken != null) {
            try {
                SsoUserInfo userInfo = validateJwtToken(accessToken);
                sessionInfo.put("authenticated", true);
                sessionInfo.put("user_id", userInfo.getUserId());
                sessionInfo.put("username", userInfo.getUsername());
                sessionInfo.put("role", userInfo.getRole());
            } catch (Exception e) {
                sessionInfo.put("authenticated", false);
                sessionInfo.put("error", "Invalid access token");
            }
        } else {
            sessionInfo.put("authenticated", false);
        }
        
        return sessionInfo;
    }

    // 私有辅助方法

    private String generateState() {
        return UUID.randomUUID().toString();
    }

    private boolean validateAuthorizationCode(String code) {
        try {
            if (code == null || code.trim().isEmpty()) {
                log.warn("Authorization code is null or empty");
                return false;
            }

            // 查询授权码
            AuthorizationCode authCode = authorizationCodeService.findValidByCode(code);
            if (authCode == null) {
                log.warn("Authorization code not found or expired: {}", code);
                return false;
            }

            // 检查是否已使用
            if (authCode.getUsed()) {
                log.warn("Authorization code already used: {}", code);
                return false;
            }

            // 检查客户端ID是否匹配
            if (!clientId.equals(authCode.getClientId())) {
                log.warn("Client ID mismatch for authorization code: {}", code);
                return false;
            }

            log.info("Authorization code validated successfully: {}", code);
            return true;
        } catch (Exception e) {
            log.error("Error validating authorization code: {}", code, e);
            return false;
        }
    }

    private SsoAuthResponse exchangeToken(String code) {
        try {
            // 验证授权码
            if (!validateAuthorizationCode(code)) {
                log.warn("Invalid authorization code: {}", code);
                return null;
            }

            // 获取授权码信息
            AuthorizationCode authCode = authorizationCodeService.findValidByCode(code);
            if (authCode == null) {
                log.warn("Authorization code not found: {}", code);
                return null;
            }

            // 获取用户信息
            SsoUser ssoUser = ssoUserService.findByUserId(authCode.getUserId());
            if (ssoUser == null) {
                log.warn("User not found for authorization code: {}", code);
                return null;
            }

            // 转换为SsoUserInfo
            SsoUserInfo userInfo = convertToSsoUserInfo(ssoUser);

            // 生成令牌
            String accessToken = generateJwtToken(userInfo);
            String refreshToken = generateRefreshToken(userInfo);

            // 标记授权码为已使用
            authorizationCodeService.markAsUsed(code);

            // 更新用户最后登录时间
            ssoUserService.updateLastLogin(ssoUser.getUserId(), LocalDateTime.now(), "unknown");

            // 构建响应
            SsoAuthResponse response = new SsoAuthResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType("Bearer");
            response.setExpiresIn(jwtExpiration / 1000);
            response.setUserId(userInfo.getUserId());
            response.setUsername(userInfo.getUsername());
            response.setEmail(userInfo.getEmail());
            response.setRole(userInfo.getRole());
            response.setTokenId(UUID.randomUUID().toString());
            response.setCreatedAt(System.currentTimeMillis());

            log.info("Token exchange completed for user: {}", userInfo.getUserId());
            return response;
        } catch (Exception e) {
            log.error("Error exchanging token for code: {}", code, e);
            return null;
        }
    }

    private SsoUserInfo getUserInfoByCode(String code) {
        try {
            // 验证授权码
            if (!validateAuthorizationCode(code)) {
                log.warn("Invalid authorization code for user info: {}", code);
                return null;
            }

            // 获取授权码信息
            AuthorizationCode authCode = authorizationCodeService.findValidByCode(code);
            if (authCode == null) {
                log.warn("Authorization code not found for user info: {}", code);
                return null;
            }

            // 获取用户信息
            SsoUser ssoUser = ssoUserService.findByUserId(authCode.getUserId());
            if (ssoUser == null) {
                log.warn("User not found for authorization code: {}", code);
                return null;
            }

            return convertToSsoUserInfo(ssoUser);
        } catch (Exception e) {
            log.error("Error getting user info by code: {}", code, e);
            return null;
        }
    }

    private String generateJwtToken(SsoUserInfo userInfo) {
        return jwtUtils.generateAccessToken(userInfo);
    }

    private String generateRefreshToken(SsoUserInfo userInfo) {
        return jwtUtils.generateRefreshToken(userInfo);
    }

    private boolean validateClientCredentials(String clientId, String clientSecret) {
        try {
            if (clientId == null || clientSecret == null) {
                log.warn("Client credentials are null");
                return false;
            }

            // 从数据库验证客户端凭据
            int count = oAuthClientService.validateCredentials(clientId, clientSecret);
            if (count > 0) {
                log.info("Client credentials validated successfully: {}", clientId);
                return true;
            }

            // 如果数据库中没有找到，使用配置的凭据作为备用
            if (this.clientId.equals(clientId) && this.clientSecret.equals(clientSecret)) {
                log.info("Client credentials validated using configuration: {}", clientId);
                return true;
            }

            log.warn("Invalid client credentials: {}", clientId);
            return false;
        } catch (Exception e) {
            log.error("Error validating client credentials: {}", clientId, e);
            return false;
        }
    }

    private SsoAuthResponse handleAuthorizationCodeGrant(SsoAuthRequest authRequest) {
        try {
            // 验证客户端凭据
            if (!validateClientCredentials(authRequest.getClientId(), authRequest.getClientSecret())) {
                log.warn("Invalid client credentials for authorization code grant");
                return null;
            }

            // 处理授权码授权
            return handleCallback(authRequest.getCode(), authRequest.getState(), null);
        } catch (Exception e) {
            log.error("Error handling authorization code grant", e);
            return null;
        }
    }

    private SsoAuthResponse handleRefreshTokenGrant(SsoAuthRequest authRequest) {
        try {
            // 验证客户端凭据
            if (!validateClientCredentials(authRequest.getClientId(), authRequest.getClientSecret())) {
                log.warn("Invalid client credentials for refresh token grant");
                return null;
            }

            // 处理刷新令牌授权
            return refreshToken(authRequest.getRefreshToken());
        } catch (Exception e) {
            log.error("Error handling refresh token grant", e);
            return null;
        }
    }

    private SsoAuthResponse handlePasswordGrant(SsoAuthRequest authRequest) {
        try {
            // 验证客户端凭据
            if (!validateClientCredentials(authRequest.getClientId(), authRequest.getClientSecret())) {
                log.warn("Invalid client credentials for password grant");
                return null;
            }

            // 验证用户名和密码
            SsoUser ssoUser = ssoUserService.findByUsername(authRequest.getUsername());
            if (ssoUser == null) {
                log.warn("User not found for password grant: {}", authRequest.getUsername());
                return null;
            }

            // 验证密码
            if (!passwordUtils.verifyPassword(authRequest.getPassword(), ssoUser.getPassword())) {
                log.warn("Invalid password for user: {}", authRequest.getUsername());
                return null;
            }

            // 检查用户状态
            if (!ssoUser.getEnabled() || ssoUser.getLocked()) {
                log.warn("User account is disabled or locked: {}", authRequest.getUsername());
                return null;
            }

            // 转换为SsoUserInfo
            SsoUserInfo userInfo = convertToSsoUserInfo(ssoUser);

            // 更新最后登录时间
            ssoUserService.updateLastLogin(ssoUser.getUserId(), LocalDateTime.now(), "unknown");

            // 生成令牌
            String accessToken = generateJwtToken(userInfo);
            String refreshToken = generateRefreshToken(userInfo);

            // 构建响应
            SsoAuthResponse response = new SsoAuthResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType("Bearer");
            response.setExpiresIn(jwtExpiration / 1000);
            response.setUserId(userInfo.getUserId());
            response.setUsername(userInfo.getUsername());
            response.setEmail(userInfo.getEmail());
            response.setRole(userInfo.getRole());

            log.info("Password grant completed for user: {}", userInfo.getUserId());
            return response;
        } catch (Exception e) {
            log.error("Error handling password grant", e);
            return null;
        }
    }

    private SsoUserInfo validateRefreshToken(String refreshToken) {
        // 首先检查是否在黑名单中
        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            log.warn("Refresh token is blacklisted");
            return null;
        }
        
        return jwtUtils.validateRefreshToken(refreshToken);
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    private SsoUserInfo validateJwtToken(String token) {
        // 首先检查是否在黑名单中
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.warn("JWT token is blacklisted");
            return null;
        }
        
        return jwtUtils.validateAccessToken(token);
    }

    private void clearSession(HttpServletRequest request) {
        // 清除HTTP会话
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
    }

    private void clearAuthCookies(HttpServletResponse response) {
        // 清除访问令牌Cookie
        Cookie accessTokenCookie = new Cookie("access_token", "");
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        response.addCookie(accessTokenCookie);
        
        // 清除刷新令牌Cookie
        Cookie refreshTokenCookie = new Cookie("refresh_token", "");
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
    }

    private void blacklistToken(String token) {
        tokenBlacklistService.blacklistToken(token);
    }

    /**
     * 将SsoUser转换为SsoUserInfo
     */
    private SsoUserInfo convertToSsoUserInfo(SsoUser ssoUser) {
        SsoUserInfo userInfo = new SsoUserInfo();
        userInfo.setUserId(ssoUser.getUserId());
        userInfo.setUsername(ssoUser.getUsername());
        userInfo.setEmail(ssoUser.getEmail());
        userInfo.setNickname(ssoUser.getNickname());
        userInfo.setAvatar(ssoUser.getAvatar());
        userInfo.setRole(ssoUser.getRole());
        userInfo.setDepartment(ssoUser.getDepartment());
        userInfo.setPosition(ssoUser.getPosition());
        userInfo.setPhone(ssoUser.getPhone());
        userInfo.setGender(ssoUser.getGender());
        userInfo.setStatus(ssoUser.getStatus());
        userInfo.setEnabled(ssoUser.getEnabled());
        userInfo.setLocked(ssoUser.getLocked());
        userInfo.setLastLoginAt(ssoUser.getLastLoginAt() != null ? 
            ssoUser.getLastLoginAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() : null);
        userInfo.setLastLoginIp(ssoUser.getLastLoginIp());
        userInfo.setCreatedAt(ssoUser.getCreatedAt() != null ? 
            ssoUser.getCreatedAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() : null);
        return userInfo;
    }
} 