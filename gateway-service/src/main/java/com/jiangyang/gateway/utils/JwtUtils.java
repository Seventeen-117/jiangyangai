package com.jiangyang.gateway.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 用于JWT token的解析、验证和生成
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret:default-secret-key-for-development-only}")
    private String jwtSecret;

    @Value("${jwt.issuer:jiangyang-gateway}")
    private String jwtIssuer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析JWT token并提取用户信息
     * 
     * @param token JWT token字符串
     * @return 用户信息Map
     */
    public Map<String, Object> parseToken(String token) {
        Map<String, Object> userInfo = new HashMap<>();
        
        try {
            // 1. 解码JWT payload
            Claims claims = parseJwtClaims(token);
            
            // 2. 提取用户信息
            userInfo.put("userId", extractUserId(claims));
            userInfo.put("userType", extractUserType(claims));
            userInfo.put("tenantId", extractTenantId(claims));
            userInfo.put("username", extractUsername(claims));
            userInfo.put("email", extractEmail(claims));
            userInfo.put("roles", extractRoles(claims));
            userInfo.put("permissions", extractPermissions(claims));
            userInfo.put("issuedAt", claims.getIssuedAt());
            userInfo.put("expiresAt", claims.getExpiration());
            userInfo.put("issuer", claims.getIssuer());
            
            // 3. 验证数据完整性
            validateTokenIntegrity(token, claims);
            
            log.debug("Successfully parsed JWT token for user: {}", userInfo.get("userId"));
            
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            userInfo.put("error", "TOKEN_EXPIRED");
            userInfo.put("errorMessage", "Token has expired");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            userInfo.put("error", "UNSUPPORTED_TOKEN");
            userInfo.put("errorMessage", "Unsupported token format");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            userInfo.put("error", "MALFORMED_TOKEN");
            userInfo.put("errorMessage", "Malformed token");
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            userInfo.put("error", "INVALID_SIGNATURE");
            userInfo.put("errorMessage", "Invalid token signature");
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is null or empty: {}", e.getMessage());
            userInfo.put("error", "INVALID_TOKEN");
            userInfo.put("errorMessage", "Invalid token");
        } catch (Exception e) {
            log.error("Unexpected error during JWT parsing", e);
            userInfo.put("error", "PARSING_ERROR");
            userInfo.put("errorMessage", "Token parsing failed: " + e.getMessage());
        }
        
        return userInfo;
    }

    /**
     * 验证JWT token的有效性
     * 
     * @param token JWT token字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseJwtClaims(token);
            
            // 验证过期时间
            if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
                log.warn("JWT token has expired");
                return false;
            }
            
            // 验证发行者（可选）
            if (jwtIssuer != null && claims.getIssuer() != null && 
                !jwtIssuer.equals(claims.getIssuer())) {
                log.warn("JWT token issuer mismatch: expected {}, got {}", 
                        jwtIssuer, claims.getIssuer());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析JWT Claims
     */
    private Claims parseJwtClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证token完整性
     */
    private void validateTokenIntegrity(String token, Claims claims) {
        // 验证必要字段
        if (claims.getSubject() == null && claims.get("userId") == null) {
            throw new IllegalArgumentException("JWT token missing user identifier");
        }
        
        // 验证时间戳
        if (claims.getIssuedAt() == null) {
            throw new IllegalArgumentException("JWT token missing issued at timestamp");
        }
        
        // 验证过期时间
        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException("JWT token missing expiration timestamp");
        }
        
        log.debug("JWT token integrity validation passed");
    }

    /**
     * 提取用户ID
     */
    private String extractUserId(Claims claims) {
        // 尝试多种可能的字段名
        String userId = claims.getSubject(); // 标准JWT sub字段
        if (userId != null) {
            return userId;
        }
        
        userId = claims.get("userId", String.class);
        if (userId != null) {
            return userId;
        }
        
        userId = claims.get("user_id", String.class);
        if (userId != null) {
            return userId;
        }
        
        userId = claims.get("id", String.class);
        if (userId != null) {
            return userId;
        }
        
        // 如果都没有，使用sub字段或生成默认值
        return claims.getSubject() != null ? claims.getSubject() : "unknown_user";
    }

    /**
     * 提取用户类型
     */
    private String extractUserType(Claims claims) {
        String userType = claims.get("userType", String.class);
        if (userType != null) {
            return userType;
        }
        
        userType = claims.get("user_type", String.class);
        if (userType != null) {
            return userType;
        }
        
        userType = claims.get("type", String.class);
        if (userType != null) {
            return userType;
        }
        
        return "USER"; // 默认用户类型
    }

    /**
     * 提取租户ID
     */
    private String extractTenantId(Claims claims) {
        String tenantId = claims.get("tenantId", String.class);
        if (tenantId != null) {
            return tenantId;
        }
        
        tenantId = claims.get("tenant_id", String.class);
        if (tenantId != null) {
            return tenantId;
        }
        
        tenantId = claims.get("tid", String.class);
        if (tenantId != null) {
            return tenantId;
        }
        
        return "default_tenant"; // 默认租户
    }

    /**
     * 提取用户名
     */
    private String extractUsername(Claims claims) {
        String username = claims.get("username", String.class);
        if (username != null) {
            return username;
        }
        
        username = claims.get("name", String.class);
        if (username != null) {
            return username;
        }
        
        username = claims.get("preferred_username", String.class);
        if (username != null) {
            return username;
        }
        
        return extractUserId(claims); // 如果没有用户名，使用用户ID
    }

    /**
     * 提取邮箱
     */
    private String extractEmail(Claims claims) {
        String email = claims.get("email", String.class);
        if (email != null) {
            return email;
        }
        
        email = claims.get("mail", String.class);
        if (email != null) {
            return email;
        }
        
        return null;
    }

    /**
     * 提取角色
     */
    private String extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles != null) {
            return roles.toString();
        }
        
        Object realmAccess = claims.get("realm_access");
        if (realmAccess != null) {
            try {
                Map<String, Object> realmMap = objectMapper.convertValue(realmAccess, Map.class);
                Object rolesObj = realmMap.get("roles");
                if (rolesObj != null) {
                    return rolesObj.toString();
                }
            } catch (Exception e) {
                log.debug("Failed to parse realm_access roles", e);
            }
        }
        
        return "USER"; // 默认角色
    }

    /**
     * 提取权限
     */
    private String extractPermissions(Claims claims) {
        Object permissions = claims.get("permissions");
        if (permissions != null) {
            return permissions.toString();
        }
        
        Object resourceAccess = claims.get("resource_access");
        if (resourceAccess != null) {
            try {
                Map<String, Object> resourceMap = objectMapper.convertValue(resourceAccess, Map.class);
                // 这里可以根据具体的资源访问配置来提取权限
                return resourceAccess.toString();
            } catch (Exception e) {
                log.debug("Failed to parse resource_access permissions", e);
            }
        }
        
        return ""; // 默认无权限
    }

    /**
     * 生成JWT token（用于测试）
     */
    public String generateToken(String userId, String userType, String tenantId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .setSubject(userId)
                .claim("userId", userId)
                .claim("userType", userType)
                .claim("tenantId", tenantId)
                .claim("username", userId)
                .claim("roles", "USER")
                .setIssuer(jwtIssuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1小时过期
                .signWith(key)
                .compact();
    }
} 