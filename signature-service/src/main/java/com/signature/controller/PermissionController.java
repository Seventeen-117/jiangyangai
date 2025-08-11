package com.signature.controller;

import com.signature.entity.ApiPermission;
import com.signature.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    // ========== 权限管理 ==========

    /**
     * 获取所有权限
     */
    @GetMapping
    public ResponseEntity<List<ApiPermission>> getAllPermissions() {
        try {
            List<ApiPermission> permissions = permissionService.getAllPermissions();
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error getting all permissions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 创建新权限
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPermission(@RequestBody ApiPermission permission) {
        try {
            boolean success = permissionService.createPermission(permission);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("message", "Permission created successfully");
                response.put("permissionCode", permission.getPermissionCode());
            } else {
                response.put("error", "Failed to create permission");
            }
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error creating permission", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * 更新权限
     */
    @PutMapping("/{permissionCode}")
    public ResponseEntity<Map<String, Object>> updatePermission(@PathVariable String permissionCode, 
                                                              @RequestBody ApiPermission permission) {
        try {
            permission.setPermissionCode(permissionCode);
            boolean success = permissionService.updatePermission(permission);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("message", "Permission updated successfully");
            } else {
                response.put("error", "Failed to update permission");
            }
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error updating permission", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{permissionCode}")
    public ResponseEntity<Map<String, Object>> deletePermission(@PathVariable String permissionCode) {
        try {
            boolean success = permissionService.deletePermission(permissionCode);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("message", "Permission deleted successfully");
            } else {
                response.put("error", "Failed to delete permission");
            }
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error deleting permission", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    // ========== API密钥权限管理 ==========

    /**
     * 为API密钥授予权限
     */
    @PostMapping("/api-key/{apiKeyId}/grant")
    public ResponseEntity<Map<String, Object>> grantApiKeyPermission(@PathVariable Long apiKeyId,
                                                                   @RequestBody Map<String, Object> request) {
        try {
            String permissionCode = (String) request.get("permissionCode");
            String grantedBy = (String) request.get("grantedBy");
            String expiresAtStr = (String) request.get("expiresAt");

            if (permissionCode == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "permissionCode is required"));
            }

            LocalDateTime expiresAt = null;
            if (expiresAtStr != null) {
                expiresAt = LocalDateTime.parse(expiresAtStr);
            }

            boolean success = permissionService.grantApiKeyPermission(apiKeyId, permissionCode, grantedBy, expiresAt);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("message", "Permission granted successfully");
            } else {
                response.put("error", "Failed to grant permission");
            }
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error granting API key permission", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * 撤销API密钥权限
     */
    @DeleteMapping("/api-key/{apiKeyId}/revoke")
    public ResponseEntity<Map<String, Object>> revokeApiKeyPermission(@PathVariable Long apiKeyId,
                                                                    @RequestBody Map<String, String> request) {
        try {
            String permissionCode = request.get("permissionCode");
            if (permissionCode == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "permissionCode is required"));
            }

            boolean success = permissionService.revokeApiKeyPermission(apiKeyId, permissionCode);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("message", "Permission revoked successfully");
            } else {
                response.put("error", "Failed to revoke permission");
            }
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error revoking API key permission", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * 获取API密钥的所有权限
     */
    @GetMapping("/api-key/{apiKeyId}")
    public ResponseEntity<List<ApiPermission>> getApiKeyPermissions(@PathVariable Long apiKeyId) {
        try {
            List<ApiPermission> permissions = permissionService.getApiKeyPermissions(apiKeyId);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error getting API key permissions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== 客户端权限管理 ==========

    /**
     * 为客户端授予权限
     */
    @PostMapping("/client/{clientId}/grant")
    public ResponseEntity<Map<String, Object>> grantClientPermission(@PathVariable String clientId,
                                                                  @RequestBody Map<String, Object> request) {
        try {
            String permissionCode = (String) request.get("permissionCode");
            String grantedBy = (String) request.get("grantedBy");
            String expiresAtStr = (String) request.get("expiresAt");

            if (permissionCode == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "permissionCode is required"));
            }

            LocalDateTime expiresAt = null;
            if (expiresAtStr != null) {
                expiresAt = LocalDateTime.parse(expiresAtStr);
            }

            boolean success = permissionService.grantClientPermission(clientId, permissionCode, grantedBy, expiresAt);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("message", "Permission granted successfully");
            } else {
                response.put("error", "Failed to grant permission");
            }
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error granting client permission", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * 撤销客户端权限
     */
    @DeleteMapping("/client/{clientId}/revoke")
    public ResponseEntity<Map<String, Object>> revokeClientPermission(@PathVariable String clientId,
                                                                   @RequestBody Map<String, String> request) {
        try {
            String permissionCode = request.get("permissionCode");
            if (permissionCode == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "permissionCode is required"));
            }

            boolean success = permissionService.revokeClientPermission(clientId, permissionCode);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("message", "Permission revoked successfully");
            } else {
                response.put("error", "Failed to revoke permission");
            }
            
            return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error revoking client permission", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * 获取客户端的所有权限
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ApiPermission>> getClientPermissions(@PathVariable String clientId) {
        try {
            List<ApiPermission> permissions = permissionService.getClientPermissions(clientId);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error getting client permissions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== 批量操作 ==========

    /**
     * 批量授予API密钥权限
     */
    @PostMapping("/api-key/{apiKeyId}/batch-grant")
    public ResponseEntity<Map<String, Object>> batchGrantApiKeyPermissions(@PathVariable Long apiKeyId,
                                                                        @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> permissionCodes = (List<String>) request.get("permissionCodes");
            String grantedBy = (String) request.get("grantedBy");
            String expiresAtStr = (String) request.get("expiresAt");

            if (permissionCodes == null || permissionCodes.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "permissionCodes is required"));
            }

            LocalDateTime expiresAt = null;
            if (expiresAtStr != null) {
                expiresAt = LocalDateTime.parse(expiresAtStr);
            }

            int successCount = permissionService.batchGrantApiKeyPermissions(apiKeyId, permissionCodes, grantedBy, expiresAt);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Batch grant completed");
            response.put("successCount", successCount);
            response.put("totalCount", permissionCodes.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error batch granting API key permissions", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * 批量授予客户端权限
     */
    @PostMapping("/client/{clientId}/batch-grant")
    public ResponseEntity<Map<String, Object>> batchGrantClientPermissions(@PathVariable String clientId,
                                                                        @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> permissionCodes = (List<String>) request.get("permissionCodes");
            String grantedBy = (String) request.get("grantedBy");
            String expiresAtStr = (String) request.get("expiresAt");

            if (permissionCodes == null || permissionCodes.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "permissionCodes is required"));
            }

            LocalDateTime expiresAt = null;
            if (expiresAtStr != null) {
                expiresAt = LocalDateTime.parse(expiresAtStr);
            }

            int successCount = permissionService.batchGrantClientPermissions(clientId, permissionCodes, grantedBy, expiresAt);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Batch grant completed");
            response.put("successCount", successCount);
            response.put("totalCount", permissionCodes.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error batch granting client permissions", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }
}
