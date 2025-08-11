package com.signature.controller;

import com.signature.entity.ApiKey;
import com.signature.entity.ApiKeyInfo;
import com.signature.model.ApiKeyValidationResult;
import com.signature.service.ApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * API密钥控制器
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/keys")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    /**
     * 生成API密钥
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateApiKey(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String clientId = request.get("clientId");
            String clientName = request.get("clientName");
            String description = request.get("description");
            
            if (clientId == null || clientId.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "clientId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            ApiKeyInfo apiKeyInfo = apiKeyService.generateApiKey(clientId, clientName, description);
            response.put("success", true);
            response.put("apiKeyInfo", apiKeyInfo);
            response.put("message", "API Key generated successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to generate API Key: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Failed to generate API Key", e);
            response.put("success", false);
            response.put("error", "Failed to generate API Key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 撤销API密钥
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, Object>> revokeApiKey(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String apiKey = request.get("apiKey");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "apiKey is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            apiKeyService.revokeApiKey(apiKey);
            response.put("success", true);
            response.put("message", "API Key revoked successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to revoke API Key", e);
            response.put("success", false);
            response.put("error", "Failed to revoke API Key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证API密钥状态
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateApiKey(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String apiKey = request.get("apiKey");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "apiKey is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            ApiKeyValidationResult result = apiKeyService.validateApiKeyStatus(apiKey);
            response.put("success", true);
            response.put("validationResult", result);
            response.put("valid", result.getStatus().name().equals("VALID"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to validate API Key", e);
            response.put("success", false);
            response.put("error", "Failed to validate API Key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有API密钥
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllApiKeys() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ApiKey> apiKeys = apiKeyService.getAllApiKeys();
            response.put("success", true);
            response.put("apiKeys", apiKeys);
            response.put("total", apiKeys.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get all API Keys", e);
            response.put("success", false);
            response.put("error", "Failed to get all API Keys");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取API密钥信息
     */
    @PostMapping("/info")
    public ResponseEntity<Map<String, Object>> getApiKeyInfo(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String apiKey = request.get("apiKey");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "apiKey is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            ApiKey apiKeyInfo = apiKeyService.getApiKeyInfo(apiKey);
            if (apiKeyInfo != null) {
                response.put("success", true);
                response.put("apiKeyInfo", apiKeyInfo);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "API Key not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to get API Key info", e);
            response.put("success", false);
            response.put("error", "Failed to get API Key info");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新API密钥状态
     */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> updateApiKeyStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String apiKey = (String) request.get("apiKey");
            Boolean active = (Boolean) request.get("active");

            if (apiKey == null || apiKey.trim().isEmpty() || active == null) {
                response.put("success", false);
                response.put("error", "apiKey and active are required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            apiKeyService.updateApiKeyStatus(apiKey, active);
            response.put("success", true);
            response.put("message", "API Key status updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update API Key status", e);
            response.put("success", false);
            response.put("error", "Failed to update API Key status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据客户端ID获取API密钥列表
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> getApiKeysByClientId(@PathVariable String clientId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ApiKey> apiKeys = apiKeyService.getApiKeysByClientId(clientId);
            response.put("success", true);
            response.put("apiKeys", apiKeys);
            response.put("total", apiKeys.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get API Keys by client ID: {}", clientId, e);
            response.put("success", false);
            response.put("error", "Failed to get API Keys by client ID");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量撤销API密钥
     */
    @PostMapping("/batch-revoke")
    public ResponseEntity<Map<String, Object>> batchRevokeApiKeys(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<String> apiKeys = (List<String>) request.get("apiKeys");

            if (apiKeys == null || apiKeys.isEmpty()) {
                response.put("success", false);
                response.put("error", "apiKeys list is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            int revokedCount = apiKeyService.batchRevokeApiKeys(apiKeys);
            response.put("success", true);
            response.put("revokedCount", revokedCount);
            response.put("message", "Batch revoke completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to batch revoke API Keys", e);
            response.put("success", false);
            response.put("error", "Failed to batch revoke API Keys");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量更新API密钥状态
     */
    @PostMapping("/batch-status")
    public ResponseEntity<Map<String, Object>> batchUpdateApiKeyStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<String> apiKeys = (List<String>) request.get("apiKeys");
            Boolean active = (Boolean) request.get("active");

            if (apiKeys == null || apiKeys.isEmpty() || active == null) {
                response.put("success", false);
                response.put("error", "apiKeys list and active are required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            int updatedCount = apiKeyService.batchUpdateApiKeyStatus(apiKeys, active);
            response.put("success", true);
            response.put("updatedCount", updatedCount);
            response.put("message", "Batch update completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to batch update API Key status", e);
            response.put("success", false);
            response.put("error", "Failed to batch update API Key status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 检查API密钥是否有效
     */
    @PostMapping("/check-valid")
    public ResponseEntity<Map<String, Object>> checkApiKeyValid(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String apiKey = request.get("apiKey");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "apiKey is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            boolean isValid = apiKeyService.isApiKeyValid(apiKey);
            response.put("success", true);
            response.put("valid", isValid);
            response.put("message", isValid ? "API Key is valid" : "API Key is invalid");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to check API Key validity", e);
            response.put("success", false);
            response.put("error", "Failed to check API Key validity");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取API密钥统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getApiKeyStatistics(
            @RequestParam(value = "clientId", required = false) String clientId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Object statistics = apiKeyService.getApiKeyStatistics(clientId);
            response.put("success", true);
            response.put("statistics", statistics);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get API Key statistics", e);
            response.put("success", false);
            response.put("error", "Failed to get API Key statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 刷新API密钥
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshApiKey(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String apiKey = request.get("apiKey");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "apiKey is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            ApiKeyInfo newApiKeyInfo = apiKeyService.refreshApiKey(apiKey);
            response.put("success", true);
            response.put("newApiKeyInfo", newApiKeyInfo);
            response.put("message", "API Key refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to refresh API Key: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Failed to refresh API Key", e);
            response.put("success", false);
            response.put("error", "Failed to refresh API Key");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 延长API密钥有效期
     */
    @PostMapping("/extend")
    public ResponseEntity<Map<String, Object>> extendApiKeyExpiration(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String apiKey = (String) request.get("apiKey");
            Integer daysToAdd = (Integer) request.get("daysToAdd");

            if (apiKey == null || apiKey.trim().isEmpty() || daysToAdd == null || daysToAdd <= 0) {
                response.put("success", false);
                response.put("error", "apiKey and daysToAdd (positive integer) are required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            boolean extended = apiKeyService.extendApiKeyExpiration(apiKey, daysToAdd);
            if (extended) {
                response.put("success", true);
                response.put("message", "API Key expiration extended successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Failed to extend API Key expiration");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to extend API Key expiration", e);
            response.put("success", false);
            response.put("error", "Failed to extend API Key expiration");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证API密钥权限
     */
    @PostMapping("/validate-permission")
    public ResponseEntity<Map<String, Object>> validateApiKeyPermission(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String apiKey = request.get("apiKey");
            String permission = request.get("permission");

            if (apiKey == null || apiKey.trim().isEmpty() || permission == null || permission.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "apiKey and permission are required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            boolean hasPermission = apiKeyService.validateApiKeyPermission(apiKey, permission);
            response.put("success", true);
            response.put("hasPermission", hasPermission);
            response.put("message", hasPermission ? "Permission granted" : "Permission denied");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to validate API Key permission", e);
            response.put("success", false);
            response.put("error", "Failed to validate API Key permission");
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
        response.put("service", "api-key");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
} 