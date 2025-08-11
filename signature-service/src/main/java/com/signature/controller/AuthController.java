package com.signature.controller;

import com.signature.entity.User;
import com.signature.entity.UserToken;
import com.signature.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 身份认证控制器，处理SSO登录相关的请求
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Value("${sso.client-id:default-client-id}")
    private String clientId;

    @Value("${sso.redirect-uri:http://localhost:8080/api/auth/callback}")
    private String redirectUri;

    @Value("${sso.authorize-url:https://sso.example.com/oauth/authorize}")
    private String authorizeUrl;

    /**
     * 获取SSO登录URL
     * 
     * @return 登录URL
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        String loginUrl = authorizeUrl +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=openid email profile";
        
        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", loginUrl);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 处理SSO回调，完成登录流程
     * 
     * @param code SSO授权码
     * @return 登录结果，包含用户令牌
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestParam("code") String code) {
        try {
            UserToken userToken = userService.loginWithSSO(code);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userToken.getUserId());
            response.put("username", userToken.getUsername());
            response.put("accessToken", userToken.getAccessToken());
            response.put("expiresAt", userToken.getTokenExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("SSO callback error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Login failed");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 刷新结果，包含新的用户令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        try {
            UserToken userToken = userService.refreshToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userToken.getUserId());
            response.put("username", userToken.getUsername());
            response.put("accessToken", userToken.getAccessToken());
            response.put("expiresAt", userToken.getTokenExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token refresh failed");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * 验证令牌
     * 
     * @param accessToken 访问令牌
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam("accessToken") String accessToken) {
        try {
            UserToken userToken = userService.validateToken(accessToken);
            
            if (userToken != null && userToken.isValid()) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("userId", userToken.getUserId());
                response.put("username", userToken.getUsername());
                response.put("expiresAt", userToken.getTokenExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Invalid or expired token");
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            log.error("Token validation error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token validation failed");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserInfo(@PathVariable String userId) {
        try {
            User user = userService.getUserInfo(userId);
            
            if (user != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("userId", user.getUserId());
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("avatarUrl", user.getAvatarUrl());
                response.put("status", user.getStatus());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Get user info error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get user info");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 用户登出
     * 
     * @param accessToken 访问令牌
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam("accessToken") String accessToken) {
        try {
            userService.logout(accessToken);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Logout error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Logout failed");
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 