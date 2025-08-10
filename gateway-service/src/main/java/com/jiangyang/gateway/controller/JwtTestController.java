package com.jiangyang.gateway.controller;

import com.jiangyang.gateway.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT测试控制器
 * 用于生成和验证JWT token进行测试
 */
@Slf4j
@RestController
@RequestMapping("/api/jwt-test")
@RequiredArgsConstructor
public class JwtTestController {

    private final JwtUtils jwtUtils;

    /**
     * 生成JWT token
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateToken(
            @RequestParam(defaultValue = "test-user") String userId,
            @RequestParam(defaultValue = "USER") String userType,
            @RequestParam(defaultValue = "default-tenant") String tenantId) {
        
        try {
            String token = jwtUtils.generateToken(userId, userType, tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("userId", userId);
            response.put("userType", userType);
            response.put("tenantId", tenantId);
            response.put("message", "JWT token generated successfully");
            
            log.info("Generated JWT token for user: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to generate JWT token", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Failed to generate JWT token");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 验证JWT token
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        
        try {
            Map<String, Object> userInfo = jwtUtils.parseToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("valid", !userInfo.containsKey("error"));
            response.put("userInfo", userInfo);
            
            if (userInfo.containsKey("error")) {
                response.put("message", "Token validation failed");
                response.put("error", userInfo.get("error"));
                response.put("errorMessage", userInfo.get("errorMessage"));
            } else {
                response.put("message", "Token validation successful");
            }
            
            log.info("Validated JWT token for user: {}", userInfo.get("userId"));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to validate JWT token", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("valid", false);
            response.put("error", e.getMessage());
            response.put("message", "Failed to validate JWT token");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取JWT配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "JWT configuration info");
        response.put("note", "This endpoint shows JWT configuration for testing purposes");
        response.put("warning", "Do not expose this endpoint in production");
        
        return ResponseEntity.ok(response);
    }
} 