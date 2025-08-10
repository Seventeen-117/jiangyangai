package com.signature.controller;

import com.signature.entity.ApiConfig;
import com.signature.service.ApiConfigService;
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
 * API配置控制器
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
public class ApiConfigController {

    @Autowired
    private ApiConfigService apiConfigService;

    /**
     * 获取用户最新的配置
     */
    @GetMapping("/latest/{userId}")
    public ResponseEntity<Map<String, Object>> getLatestConfig(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApiConfig config = apiConfigService.getLatestConfig(userId);
            if (config != null) {
                response.put("success", true);
                response.put("config", config);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No config found for user: " + userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to get latest config for user: {}", userId, e);
            response.put("success", false);
            response.put("error", "Failed to get latest config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查找匹配的配置
     */
    @PostMapping("/find-matching")
    public ResponseEntity<Map<String, Object>> findMatchingConfig(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userId = request.get("userId");
            String apiUrl = request.get("apiUrl");
            String apiKey = request.get("apiKey");
            String modelName = request.get("modelName");

            ApiConfig config = apiConfigService.findMatchingConfig(userId, apiUrl, apiKey, modelName);
            if (config != null) {
                response.put("success", true);
                response.put("config", config);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No matching config found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to find matching config", e);
            response.put("success", false);
            response.put("error", "Failed to find matching config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查找替代配置
     */
    @GetMapping("/alternative/{userId}")
    public ResponseEntity<Map<String, Object>> findAlternativeConfig(
            @PathVariable String userId,
            @RequestParam(value = "currentModel", required = false) String currentModel) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApiConfig config = apiConfigService.findAlternativeConfig(userId, currentModel);
            if (config != null) {
                response.put("success", true);
                response.put("config", config);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No alternative config found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to find alternative config for user: {}", userId, e);
            response.put("success", false);
            response.put("error", "Failed to find alternative config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有API配置
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllApiConfigs() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ApiConfig> configs = apiConfigService.getAllApiConfigs();
            response.put("success", true);
            response.put("configs", configs);
            response.put("total", configs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get all API configs", e);
            response.put("success", false);
            response.put("error", "Failed to get all API configs");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据ID获取API配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getApiConfigById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApiConfig config = apiConfigService.getApiConfigById(id);
            if (config != null) {
                response.put("success", true);
                response.put("config", config);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Config not found with id: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to get API config by id: {}", id, e);
            response.put("success", false);
            response.put("error", "Failed to get API config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建API配置
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createApiConfig(@RequestBody ApiConfig apiConfig) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApiConfig created = apiConfigService.createApiConfig(apiConfig);
            if (created != null) {
                response.put("success", true);
                response.put("config", created);
                response.put("message", "API config created successfully");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                response.put("success", false);
                response.put("error", "Failed to create API config");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to create API config", e);
            response.put("success", false);
            response.put("error", "Failed to create API config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新API配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateApiConfig(
            @PathVariable Long id,
            @RequestBody ApiConfig apiConfig) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            apiConfig.setId(id);
            ApiConfig updated = apiConfigService.updateApiConfig(apiConfig);
            if (updated != null) {
                response.put("success", true);
                response.put("config", updated);
                response.put("message", "API config updated successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Failed to update API config");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to update API config with id: {}", id, e);
            response.put("success", false);
            response.put("error", "Failed to update API config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除API配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteApiConfig(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean deleted = apiConfigService.deleteApiConfig(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "API config deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Failed to delete API config");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to delete API config with id: {}", id, e);
            response.put("success", false);
            response.put("error", "Failed to delete API config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据模型类型获取API配置
     */
    @GetMapping("/model-type/{modelType}")
    public ResponseEntity<Map<String, Object>> getApiConfigByModelType(@PathVariable String modelType) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApiConfig config = apiConfigService.getApiConfigByModelType(modelType);
            if (config != null) {
                response.put("success", true);
                response.put("config", config);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No config found for model type: " + modelType);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to get API config by model type: {}", modelType, e);
            response.put("success", false);
            response.put("error", "Failed to get API config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取默认API配置
     */
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> getDefaultApiConfig() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ApiConfig config = apiConfigService.getDefaultApiConfig();
            if (config != null) {
                response.put("success", true);
                response.put("config", config);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No default config found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to get default API config", e);
            response.put("success", false);
            response.put("error", "Failed to get default API config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 设置默认API配置
     */
    @PostMapping("/{id}/set-default")
    public ResponseEntity<Map<String, Object>> setDefaultApiConfig(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean set = apiConfigService.setDefaultApiConfig(id);
            if (set) {
                response.put("success", true);
                response.put("message", "Default API config set successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Failed to set default API config");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to set default API config with id: {}", id, e);
            response.put("success", false);
            response.put("error", "Failed to set default API config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据用户ID获取配置列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getConfigsByUserId(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ApiConfig> configs = apiConfigService.getConfigsByUserId(userId);
            response.put("success", true);
            response.put("configs", configs);
            response.put("total", configs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get configs by user id: {}", userId, e);
            response.put("success", false);
            response.put("error", "Failed to get configs by user id");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据API类型获取配置
     */
    @GetMapping("/user/{userId}/api-type/{apiType}")
    public ResponseEntity<Map<String, Object>> getConfigsByApiType(
            @PathVariable String userId,
            @PathVariable String apiType) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ApiConfig> configs = apiConfigService.getConfigsByApiType(userId, apiType);
            response.put("success", true);
            response.put("configs", configs);
            response.put("total", configs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get configs by API type: {} for user: {}", apiType, userId, e);
            response.put("success", false);
            response.put("error", "Failed to get configs by API type");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取高优先级配置
     */
    @GetMapping("/user/{userId}/high-priority")
    public ResponseEntity<Map<String, Object>> getHighPriorityConfigs(
            @PathVariable String userId,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ApiConfig> configs = apiConfigService.getHighPriorityConfigs(userId, limit);
            response.put("success", true);
            response.put("configs", configs);
            response.put("total", configs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get high priority configs for user: {}", userId, e);
            response.put("success", false);
            response.put("error", "Failed to get high priority configs");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证API配置
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateApiConfig(@RequestBody ApiConfig apiConfig) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean valid = apiConfigService.validateApiConfig(apiConfig);
            response.put("success", true);
            response.put("valid", valid);
            response.put("message", valid ? "API config is valid" : "API config validation failed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to validate API config", e);
            response.put("success", false);
            response.put("error", "Failed to validate API config");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 测试API连接
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testApiConnection(@RequestBody ApiConfig apiConfig) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = apiConfigService.testApiConnection(apiConfig);
            response.put("success", true);
            response.put("connection_success", success);
            response.put("message", success ? "API connection test successful" : "API connection test failed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to test API connection", e);
            response.put("success", false);
            response.put("error", "Failed to test API connection");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量更新配置状态
     */
    @PostMapping("/batch-update-status")
    public ResponseEntity<Map<String, Object>> batchUpdateStatus(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) request.get("ids");
            Boolean enabled = (Boolean) request.get("enabled");
            
            if (ids == null || enabled == null) {
                response.put("success", false);
                response.put("error", "ids and enabled are required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            int updated = apiConfigService.batchUpdateStatus(ids, enabled);
            response.put("success", true);
            response.put("updated_count", updated);
            response.put("message", "Batch update completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to batch update status", e);
            response.put("success", false);
            response.put("error", "Failed to batch update status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取配置统计信息
     */
    @GetMapping("/statistics/{userId}")
    public ResponseEntity<Map<String, Object>> getConfigStatistics(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Object statistics = apiConfigService.getConfigStatistics(userId);
            response.put("success", true);
            response.put("statistics", statistics);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get config statistics for user: {}", userId, e);
            response.put("success", false);
            response.put("error", "Failed to get config statistics");
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
        response.put("service", "api-config");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
