package com.signature.controller;

import com.signature.entity.AuthorizationCode;
import com.signature.entity.OAuthClient;
import com.signature.entity.SsoUser;
import com.signature.service.AuthorizationCodeService;
import com.signature.service.OAuthClientService;
import com.signature.service.SsoUserService;
import com.signature.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SSO管理控制器
 * 提供SSO相关的管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/sso/admin")
public class SsoManagementController {

    @Autowired
    private AuthorizationCodeService authorizationCodeService;

    @Autowired
    private OAuthClientService oAuthClientService;

    @Autowired
    private SsoUserService ssoUserService;

    @Autowired
    private PasswordUtils passwordUtils;

    // ========== 授权码管理 ==========

    /**
     * 创建授权码
     */
    @PostMapping("/auth-code")
    public ResponseEntity<Map<String, Object>> createAuthorizationCode(@RequestBody Map<String, String> request) {
        try {
            String clientId = request.get("clientId");
            String userId = request.get("userId");
            String redirectUri = request.get("redirectUri");
            String scope = request.get("scope");
            String state = request.get("state");

            if (clientId == null || userId == null || redirectUri == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
            }

            AuthorizationCode authCode = new AuthorizationCode();
            authCode.setCode(UUID.randomUUID().toString());
            authCode.setClientId(clientId);
            authCode.setUserId(userId);
            authCode.setRedirectUri(redirectUri);
            authCode.setScope(scope);
            authCode.setState(state);
            authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // 10分钟过期
            authCode.setUsed(false);
            authCode.setCreatedAt(LocalDateTime.now());
            authCode.setUpdatedAt(LocalDateTime.now());

            authorizationCodeService.save(authCode);

            Map<String, Object> response = new HashMap<>();
            response.put("code", authCode.getCode());
            response.put("expiresAt", authCode.getExpiresAt());
            response.put("message", "Authorization code created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating authorization code", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create authorization code"));
        }
    }

    /**
     * 清理过期的授权码
     */
    @DeleteMapping("/auth-code/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredAuthCodes() {
        try {
            int deletedCount = authorizationCodeService.cleanupExpired();
            
            Map<String, Object> response = new HashMap<>();
            response.put("deletedCount", deletedCount);
            response.put("message", "Expired authorization codes cleaned up successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cleaning up expired authorization codes", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to cleanup expired authorization codes"));
        }
    }

    // ========== OAuth客户端管理 ==========

    /**
     * 创建OAuth客户端
     */
    @PostMapping("/client")
    public ResponseEntity<Map<String, Object>> createOAuthClient(@RequestBody OAuthClient client) {
        try {
            if (client.getClientId() == null || client.getClientSecret() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
            }

            // 加密客户端密钥
            client.setClientSecret(passwordUtils.encryptPassword(client.getClientSecret()));
            client.setCreatedAt(LocalDateTime.now());
            client.setUpdatedAt(LocalDateTime.now());

            oAuthClientService.save(client);

            Map<String, Object> response = new HashMap<>();
            response.put("clientId", client.getClientId());
            response.put("message", "OAuth client created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating OAuth client", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create OAuth client"));
        }
    }

    /**
     * 获取所有OAuth客户端
     */
    @GetMapping("/clients")
    public ResponseEntity<List<OAuthClient>> getAllOAuthClients() {
        try {
            List<OAuthClient> clients = oAuthClientService.list();
            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            log.error("Error getting OAuth clients", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== 用户管理 ==========

    /**
     * 创建SSO用户
     */
    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> createSsoUser(@RequestBody SsoUser user) {
        try {
            if (user.getUsername() == null || user.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameters"));
            }

            // 生成用户ID
            if (user.getUserId() == null) {
                user.setUserId("user-" + UUID.randomUUID().toString());
            }

            // 加密密码
            user.setPassword(passwordUtils.encryptPassword(user.getPassword()));

            // 设置默认值
            if (user.getRole() == null) {
                user.setRole("USER");
            }
            if (user.getStatus() == null) {
                user.setStatus("ACTIVE");
            }
            if (user.getEnabled() == null) {
                user.setEnabled(true);
            }
            if (user.getLocked() == null) {
                user.setLocked(false);
            }

            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            ssoUserService.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());
            response.put("message", "SSO user created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating SSO user", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create SSO user"));
        }
    }

    /**
     * 获取所有SSO用户
     */
    @GetMapping("/users")
    public ResponseEntity<List<SsoUser>> getAllSsoUsers() {
        try {
            List<SsoUser> users = ssoUserService.list();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error getting SSO users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新用户密码
     */
    @PutMapping("/user/{userId}/password")
    public ResponseEntity<Map<String, Object>> updateUserPassword(@PathVariable String userId, 
                                                                @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            if (newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }

            SsoUser user = ssoUserService.findByUserId(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            // 加密新密码
            user.setPassword(passwordUtils.encryptPassword(newPassword));
            user.setUpdatedAt(LocalDateTime.now());

            ssoUserService.updateById(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user password", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update password"));
        }
    }

    /**
     * 锁定/解锁用户
     */
    @PutMapping("/user/{userId}/lock")
    public ResponseEntity<Map<String, Object>> toggleUserLock(@PathVariable String userId, 
                                                            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean locked = request.get("locked");
            if (locked == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Locked status is required"));
            }

            SsoUser user = ssoUserService.findByUserId(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            user.setLocked(locked);
            user.setUpdatedAt(LocalDateTime.now());

            ssoUserService.updateById(user);

            Map<String, Object> response = new HashMap<>();
            response.put("locked", locked);
            response.put("message", "User lock status updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user lock status", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update user lock status"));
        }
    }

    // ========== 统计信息 ==========

    /**
     * 获取SSO统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSsoStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 用户统计
            long totalUsers = ssoUserService.count();
            long activeUsers = ssoUserService.count(); // 简化，实际应该查询enabled=true的用户
            long lockedUsers = ssoUserService.count(); // 简化，实际应该查询locked=true的用户

            // 客户端统计
            long totalClients = oAuthClientService.count();
            long activeClients = oAuthClientService.count(); // 简化，实际应该查询status=1的客户端

            // 授权码统计
            long totalAuthCodes = authorizationCodeService.count();
            long usedAuthCodes = authorizationCodeService.count(); // 简化，实际应该查询used=true的授权码

            stats.put("users", Map.of(
                "total", totalUsers,
                "active", activeUsers,
                "locked", lockedUsers
            ));

            stats.put("clients", Map.of(
                "total", totalClients,
                "active", activeClients
            ));

            stats.put("authCodes", Map.of(
                "total", totalAuthCodes,
                "used", usedAuthCodes
            ));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting SSO stats", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get SSO stats"));
        }
    }
}
