package com.signature.controller;

import com.signature.service.ApiSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * API 安全控制器
 * 处理API密钥验证、权限管理等安全相关功能
 */
@Slf4j
@RestController
@RequestMapping("/api/security")
public class ApiSecurityController {

    @Autowired
    private ApiSecurityService apiSecurityService;

    /**
     * 验证API密钥
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyApiKey(
            @RequestHeader("X-API-Key") String apiKey,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isValid = apiSecurityService.verifyApiKey(apiKey);
            if (isValid) {
                String clientId = apiSecurityService.getClientIdByApiKey(apiKey);
                Map<String, Object> permissions = apiSecurityService.getClientPermissions(clientId);
                
                response.put("success", true);
                response.put("valid", true);
                response.put("client_id", clientId);
                response.put("permissions", permissions);
                response.put("rate_limit", apiSecurityService.getRateLimitInfo(clientId));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("valid", false);
                response.put("error", "Invalid API key");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            log.error("API key verification failed", e);
            response.put("success", false);
            response.put("error", "Verification failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 检查API权限
     */
    @PostMapping("/check-permission")
    public ResponseEntity<Map<String, Object>> checkPermission(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestParam("permission") String permission,
            @RequestParam("resource") String resource) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean hasPermission = apiSecurityService.checkPermission(apiKey, permission, resource);
            response.put("success", true);
            response.put("has_permission", hasPermission);
            response.put("permission", permission);
            response.put("resource", resource);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Permission check failed", e);
            response.put("success", false);
            response.put("error", "Permission check failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取客户端信息
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> getClientInfo(@PathVariable String clientId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> clientInfo = apiSecurityService.getClientInfo(clientId);
            if (clientInfo != null) {
                response.put("success", true);
                response.put("client", clientInfo);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Client not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to get client info", e);
            response.put("success", false);
            response.put("error", "Failed to get client info");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有客户端列表
     */
    @GetMapping("/clients")
    public ResponseEntity<Map<String, Object>> getAllClients() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> clients = apiSecurityService.getAllClients();
            response.put("success", true);
            response.put("clients", clients);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get clients", e);
            response.put("success", false);
            response.put("error", "Failed to get clients");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建新的API密钥
     */
    @PostMapping("/api-key")
    public ResponseEntity<Map<String, Object>> createApiKey(
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "description", required = false) String description) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> apiKeyInfo = apiSecurityService.createApiKey(clientId, description);
            response.put("success", true);
            response.put("api_key", apiKeyInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create API key", e);
            response.put("success", false);
            response.put("error", "Failed to create API key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 撤销API密钥
     */
    @DeleteMapping("/api-key/{apiKey}")
    public ResponseEntity<Map<String, Object>> revokeApiKey(@PathVariable String apiKey) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean revoked = apiSecurityService.revokeApiKey(apiKey);
            if (revoked) {
                response.put("success", true);
                response.put("message", "API key revoked successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "API key not found or already revoked");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to revoke API key", e);
            response.put("success", false);
            response.put("error", "Failed to revoke API key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取API使用统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getApiStats(
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "days", defaultValue = "7") Integer days) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> stats = apiSecurityService.getApiStats(clientId, days);
            response.put("success", true);
            response.put("stats", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get API stats", e);
            response.put("success", false);
            response.put("error", "Failed to get API stats");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取限流信息
     */
    @GetMapping("/rate-limit/{clientId}")
    public ResponseEntity<Map<String, Object>> getRateLimitInfo(@PathVariable String clientId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> rateLimitInfo = apiSecurityService.getRateLimitInfo(clientId);
            response.put("success", true);
            response.put("rate_limit", rateLimitInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get rate limit info", e);
            response.put("success", false);
            response.put("error", "Failed to get rate limit info");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新客户端权限
     */
    @PutMapping("/client/{clientId}/permissions")
    public ResponseEntity<Map<String, Object>> updateClientPermissions(
            @PathVariable String clientId,
            @RequestBody Map<String, Object> permissions) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean updated = apiSecurityService.updateClientPermissions(clientId, permissions);
            if (updated) {
                response.put("success", true);
                response.put("message", "Permissions updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Client not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to update client permissions", e);
            response.put("success", false);
            response.put("error", "Failed to update client permissions");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "UP");
        response.put("service", "api-security");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
} 