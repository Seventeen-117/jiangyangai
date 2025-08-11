package com.signature.controller;


import com.signature.entity.AppSecret;
import com.signature.service.AppSecretService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

/**
 * 应用密钥管理控制器
 * 
 * @author bgpay
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/app-secret")
@RequiredArgsConstructor
public class AppSecretController {

    private final AppSecretService appSecretService;

    /**
     * 创建应用密钥
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAppSecret(@RequestBody AppSecret appSecret) {
        try {
            AppSecret created = appSecretService.createAppSecret(appSecret);
            if (created != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "App secret created successfully",
                    "data", created
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to create app secret"
                ));
            }
        } catch (Exception e) {
            log.error("Error creating app secret", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * 获取所有应用密钥
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAppSecrets() {
        try {
            List<AppSecret> appSecrets = appSecretService.list();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", appSecrets,
                "count", appSecrets.size()
            ));
        } catch (Exception e) {
            log.error("Error getting app secrets", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * 根据应用ID获取应用密钥
     */
    @GetMapping("/{appId}")
    public ResponseEntity<Map<String, Object>> getAppSecretByAppId(@PathVariable String appId) {
        try {
            AppSecret appSecret = appSecretService.selectByAppId(appId);
            if (appSecret != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", appSecret
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting app secret by appId: {}", appId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * 更新应用密钥
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAppSecret(@PathVariable Long id, @RequestBody AppSecret appSecret) {
        try {
            appSecret.setId(id);
            AppSecret updated = appSecretService.updateAppSecret(appSecret);
            if (updated != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "App secret updated successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update app secret"
                ));
            }
        } catch (Exception e) {
            log.error("Error updating app secret", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    /**
     * 删除应用密钥
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAppSecret(@PathVariable Long id) {
        try {
            boolean result = appSecretService.deleteAppSecret(id);
            if (result) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "App secret deleted successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to delete app secret"
                ));
            }
        } catch (Exception e) {
            log.error("Error deleting app secret", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }
} 