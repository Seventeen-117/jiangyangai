package com.signature.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signature.model.SsoUserInfo;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和验证JWT令牌
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${sso.security.jwt-secret}")
    private String jwtSecret;

    @Value("${sso.security.jwt-expiration}")
    private Long jwtExpiration;

    @Value("${sso.security.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成JWT访问令牌
     *
     * @param userInfo 用户信息
     * @return JWT令牌
     */
    public String generateAccessToken(SsoUserInfo userInfo) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpiration);

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userInfo.getUserId());
            claims.put("username", userInfo.getUsername());
            claims.put("email", userInfo.getEmail());
            claims.put("role", userInfo.getRole());
            claims.put("nickname", userInfo.getNickname());
            claims.put("avatar", userInfo.getAvatar());
            claims.put("department", userInfo.getDepartment());
            claims.put("position", userInfo.getPosition());
            claims.put("phone", userInfo.getPhone());
            claims.put("gender", userInfo.getGender());
            claims.put("enabled", userInfo.getEnabled());
            claims.put("locked", userInfo.getLocked());

            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .setIssuer("signature-service")
                    .setAudience("api-users")
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Failed to generate access token for user: {}", userInfo.getUserId(), e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    /**
     * 生成刷新令牌
     *
     * @param userInfo 用户信息
     * @return 刷新令牌
     */
    public String generateRefreshToken(SsoUserInfo userInfo) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userInfo.getUserId());
            claims.put("username", userInfo.getUsername());
            claims.put("type", "refresh");

            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .setIssuer("signature-service")
                    .setAudience("api-users")
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Failed to generate refresh token for user: {}", userInfo.getUserId(), e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    /**
     * 验证JWT访问令牌
     *
     * @param token JWT令牌
     * @return 用户信息，如果验证失败返回null
     */
    public SsoUserInfo validateAccessToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 检查令牌是否过期
            if (claims.getExpiration().before(new Date())) {
                log.warn("Token has expired");
                return null;
            }

            // 检查令牌类型（确保不是刷新令牌）
            if ("refresh".equals(claims.get("type"))) {
                log.warn("Invalid token type: refresh token used as access token");
                return null;
            }

            return extractUserInfoFromClaims(claims);
        } catch (ExpiredJwtException e) {
            log.warn("Token has expired: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return null;
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error validating JWT token", e);
            return null;
        }
    }

    /**
     * 验证刷新令牌
     *
     * @param token 刷新令牌
     * @return 用户信息，如果验证失败返回null
     */
    public SsoUserInfo validateRefreshToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 检查令牌是否过期
            if (claims.getExpiration().before(new Date())) {
                log.warn("Refresh token has expired");
                return null;
            }

            // 检查令牌类型（确保是刷新令牌）
            if (!"refresh".equals(claims.get("type"))) {
                log.warn("Invalid token type: access token used as refresh token");
                return null;
            }

            return extractUserInfoFromClaims(claims);
        } catch (ExpiredJwtException e) {
            log.warn("Refresh token has expired: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return null;
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error validating refresh token", e);
            return null;
        }
    }

    /**
     * 从JWT声明中提取用户信息
     *
     * @param claims JWT声明
     * @return 用户信息
     */
    private SsoUserInfo extractUserInfoFromClaims(Claims claims) {
        SsoUserInfo userInfo = new SsoUserInfo();
        
        userInfo.setUserId(claims.get("userId", String.class));
        userInfo.setUsername(claims.get("username", String.class));
        userInfo.setEmail(claims.get("email", String.class));
        userInfo.setRole(claims.get("role", String.class));
        userInfo.setNickname(claims.get("nickname", String.class));
        userInfo.setAvatar(claims.get("avatar", String.class));
        userInfo.setDepartment(claims.get("department", String.class));
        userInfo.setPosition(claims.get("position", String.class));
        userInfo.setPhone(claims.get("phone", String.class));
        userInfo.setGender(claims.get("gender", String.class));
        
        // 处理布尔值
        Object enabledObj = claims.get("enabled");
        if (enabledObj != null) {
            userInfo.setEnabled(Boolean.valueOf(enabledObj.toString()));
        }
        
        Object lockedObj = claims.get("locked");
        if (lockedObj != null) {
            userInfo.setLocked(Boolean.valueOf(lockedObj.toString()));
        }

        return userInfo;
    }

    /**
     * 获取签名密钥
     *
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从令牌中获取过期时间
     *
     * @param token JWT令牌
     * @return 过期时间，如果解析失败返回null
     */
    public Date getExpirationDate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            log.warn("Failed to get expiration date from token", e);
            return null;
        }
    }

    /**
     * 检查令牌是否即将过期（在指定时间内）
     *
     * @param token JWT令牌
     * @param thresholdSeconds 阈值秒数
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token, long thresholdSeconds) {
        Date expiration = getExpirationDate(token);
        if (expiration == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = expiration.getTime();
        long thresholdMillis = thresholdSeconds * 1000;
        
        return (expirationTime - currentTime) <= thresholdMillis;
    }

    /**
     * 获取令牌的剩余有效时间（毫秒）
     *
     * @param token JWT令牌
     * @return 剩余有效时间，如果令牌无效返回-1
     */
    public long getRemainingTime(String token) {
        Date expiration = getExpirationDate(token);
        if (expiration == null) {
            return -1;
        }
        
        long currentTime = System.currentTimeMillis();
        long expirationTime = expiration.getTime();
        
        return Math.max(0, expirationTime - currentTime);
    }
}
