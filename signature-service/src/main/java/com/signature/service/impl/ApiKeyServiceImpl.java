package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.ApiClient;
import com.signature.entity.ApiKey;
import com.signature.entity.ApiKeyInfo;
import com.signature.entity.ApiPermission;
import com.signature.entity.ApiKeyPermission;
import com.signature.entity.ClientPermission;
import com.signature.enums.ApiKeyStatus;
import com.signature.mapper.ApiClientMapper;
import com.signature.mapper.ApiKeyMapper;
import com.signature.mapper.ApiPermissionMapper;
import com.signature.mapper.ApiKeyPermissionMapper;
import com.signature.mapper.ClientPermissionMapper;
import com.signature.model.ApiKeyValidationResult;
import com.signature.service.*;
import com.signature.utils.Sha256Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * API密钥服务实现类
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKey> implements ApiKeyService {

    private final ApiClientService apiClientService;
    private final ApiPermissionService apiPermissionService;
    private final ApiKeyPermissionService apiKeyPermissionService;
    private final ClientPermissionService clientPermissionService;


    @Override
    @Transactional
    public ApiKeyInfo generateApiKey(String clientId, String clientName, String description) {
        if (!StringUtils.hasText(clientId)) {
            log.error("generateApiKey: clientId is empty");
            throw new IllegalArgumentException("clientId cannot be empty");
        }

        // 校验 clientId 是否存在且启用
        ApiClient client = apiClientService.findEnabledByClientId(clientId);

        if (client == null) {
            log.error("generateApiKey: Invalid or disabled clientId: {}", clientId);
            throw new IllegalArgumentException("Invalid or disabled clientId");
        }

        log.info("generateApiKey: Found client: {}", client.getClientName());

        // 将用户现有的所有API Key置为无效
        LambdaQueryWrapper<ApiKey> existingKeysWrapper = new LambdaQueryWrapper<>();
        existingKeysWrapper.eq(ApiKey::getClientId, clientId);
        existingKeysWrapper.eq(ApiKey::getActive, 1);
        List<ApiKey> existingKeys = this.list(existingKeysWrapper);

        if (!existingKeys.isEmpty()) {
            log.info("generateApiKey: Setting {} existing API Keys to inactive for client: {}", 
                    existingKeys.size(), clientId);
            
            LambdaUpdateWrapper<ApiKey> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ApiKey::getClientId, clientId);
            updateWrapper.eq(ApiKey::getActive, 1);
            updateWrapper.set(ApiKey::getActive, 0);
            this.update(null, updateWrapper);
        }

        // 生成明文API Key
        String plainApiKey = UUID.randomUUID().toString().replace("-", "");
        // 用SHA-256哈希后存库
        String hashedApiKey = Sha256Util.hash(plainApiKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setApiKey(hashedApiKey);
        apiKey.setClientId(clientId);
        apiKey.setClientName(client.getClientName());
        apiKey.setDescription(description != null ? description : "");
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setExpiresAt(LocalDateTime.now().plusYears(1));
        apiKey.setActive(1);

        boolean saved = this.save(apiKey);
        if (!saved) {
            log.error("generateApiKey: Failed to save API Key for client: {}", clientId);
            throw new RuntimeException("Failed to save API Key");
        }

        log.info("generateApiKey: Generated new API Key for client: {} (SHA-256 hashed storage)", clientId);

        // 使用 ApiKeyInfo 代替 ApiKey 作为返回值，避免返回 id 字段
        ApiKeyInfo result = ApiKeyInfo.builder()
                .apiKey(plainApiKey)
                .clientId(clientId)
                .clientName(client.getClientName())
                .description(description)
                .createdAt(apiKey.getCreatedAt())
                .expiresAt(apiKey.getExpiresAt())
                .active(apiKey.getActive() == 1)
                .build();

        return result;
    }

    @Override
    @Transactional
    public void revokeApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("revokeApiKey: apiKey is empty");
            return;
        }

        String hashedApiKey = Sha256Util.hash(apiKey);
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, hashedApiKey);
        wrapper.eq(ApiKey::getActive, 1);

        ApiKey key = this.getOne(wrapper);
        if (key != null) {
            key.setActive(0);
            this.updateById(key);
            log.info("revokeApiKey: Revoked API Key for client: {}", key.getClientId());
        } else {
            log.warn("revokeApiKey: API Key not found or already revoked");
        }
    }

    @Override
    public ApiKeyValidationResult validateApiKeyStatus(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("validateApiKeyStatus: apiKey is empty");
            return new ApiKeyValidationResult(ApiKeyStatus.INVALID, null, "API Key is empty", null);
        }

        String hashedApiKey = Sha256Util.hash(apiKey);
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, hashedApiKey);

        ApiKey key = this.getOne(wrapper);
        if (key == null) {
            log.warn("validateApiKeyStatus: API Key not found");
            return new ApiKeyValidationResult(ApiKeyStatus.NOT_FOUND, null, "API Key not found", null);
        }

        if (key.getActive() == null || key.getActive() == 0) {
            log.warn("validateApiKeyStatus: API Key is disabled for client: {}", key.getClientId());
            return new ApiKeyValidationResult(ApiKeyStatus.DISABLED, key.getExpiresAt(), 
                    "API Key is disabled", key.getClientId());
        }

        if (key.getExpiresAt() == null || key.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("validateApiKeyStatus: API Key is expired for client: {}", key.getClientId());
            return new ApiKeyValidationResult(ApiKeyStatus.EXPIRED, key.getExpiresAt(), 
                    "API Key is expired", key.getClientId());
        }

        log.info("validateApiKeyStatus: API Key is valid for client: {}", key.getClientId());
        return new ApiKeyValidationResult(ApiKeyStatus.VALID, key.getExpiresAt(), null, key.getClientId());
    }

    @Override
    public List<ApiKey> getAllApiKeys() {
        // 不返回明文apiKey，apiKey字段为hash
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ApiKey::getCreatedAt);
        return this.list(wrapper);
    }

    @Override
    public ApiKey getApiKeyInfo(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("getApiKeyInfo: apiKey is empty");
            return null;
        }

        String hashedApiKey = Sha256Util.hash(apiKey);
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, hashedApiKey);
        return this.getOne(wrapper);
    }

    @Override
    @Transactional
    public void updateApiKeyStatus(String apiKey, boolean active) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("updateApiKeyStatus: apiKey is empty");
            return;
        }

        String hashedApiKey = Sha256Util.hash(apiKey);
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, hashedApiKey);

        ApiKey key = this.getOne(wrapper);
        if (key != null) {
            key.setActive(active ? 1 : 0);
            this.updateById(key);
            log.info("updateApiKeyStatus: Updated API Key status for client: {} -> {}", 
                    key.getClientId(), active);
        } else {
            log.warn("updateApiKeyStatus: API Key not found");
        }
    }

    @Override
    public List<ApiKey> getApiKeysByClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("getApiKeysByClientId: clientId is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getClientId, clientId);
        wrapper.orderByDesc(ApiKey::getCreatedAt);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public int batchRevokeApiKeys(List<String> apiKeys) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            log.warn("batchRevokeApiKeys: apiKeys is empty");
            return 0;
        }

        int revokedCount = 0;
        for (String apiKey : apiKeys) {
            try {
                revokeApiKey(apiKey);
                revokedCount++;
            } catch (Exception e) {
                log.error("batchRevokeApiKeys: Failed to revoke API Key: {}", apiKey, e);
            }
        }

        log.info("batchRevokeApiKeys: Revoked {} API Keys", revokedCount);
        return revokedCount;
    }

    @Override
    @Transactional
    public int batchUpdateApiKeyStatus(List<String> apiKeys, boolean active) {
        if (apiKeys == null || apiKeys.isEmpty()) {
            log.warn("batchUpdateApiKeyStatus: apiKeys is empty");
            return 0;
        }

        int updatedCount = 0;
        for (String apiKey : apiKeys) {
            try {
                updateApiKeyStatus(apiKey, active);
                updatedCount++;
            } catch (Exception e) {
                log.error("batchUpdateApiKeyStatus: Failed to update API Key: {}", apiKey, e);
            }
        }

        log.info("batchUpdateApiKeyStatus: Updated {} API Keys to status: {}", updatedCount, active);
        return updatedCount;
    }

    @Override
    public boolean isApiKeyValid(String apiKey) {
        ApiKeyValidationResult result = validateApiKeyStatus(apiKey);
        return result.getStatus() == ApiKeyStatus.VALID;
    }

    @Override
    public Object getApiKeyStatistics(String clientId) {
        Map<String, Object> statistics = new HashMap<>();

        if (StringUtils.hasText(clientId)) {
            // 统计特定客户端的API密钥
            LambdaQueryWrapper<ApiKey> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.eq(ApiKey::getClientId, clientId);
            long totalCount = this.count(totalWrapper);
            statistics.put("total", totalCount);

            LambdaQueryWrapper<ApiKey> activeWrapper = new LambdaQueryWrapper<>();
            activeWrapper.eq(ApiKey::getClientId, clientId);
            activeWrapper.eq(ApiKey::getActive, 1);
            long activeCount = this.count(activeWrapper);
            statistics.put("active", activeCount);

            LambdaQueryWrapper<ApiKey> expiredWrapper = new LambdaQueryWrapper<>();
            expiredWrapper.eq(ApiKey::getClientId, clientId);
            expiredWrapper.lt(ApiKey::getExpiresAt, LocalDateTime.now());
            long expiredCount = this.count(expiredWrapper);
            statistics.put("expired", expiredCount);
        } else {
            // 统计所有API密钥
            long totalCount = this.count(null);
            statistics.put("total", totalCount);

            LambdaQueryWrapper<ApiKey> activeWrapper = new LambdaQueryWrapper<>();
            activeWrapper.eq(ApiKey::getActive, 1);
            long activeCount = this.count(activeWrapper);
            statistics.put("active", activeCount);

            LambdaQueryWrapper<ApiKey> expiredWrapper = new LambdaQueryWrapper<>();
            expiredWrapper.lt(ApiKey::getExpiresAt, LocalDateTime.now());
            long expiredCount = this.count(expiredWrapper);
            statistics.put("expired", expiredCount);
        }

        log.info("getApiKeyStatistics: clientId={}, statistics={}", clientId, statistics);
        return statistics;
    }

    @Override
    @Transactional
    public ApiKeyInfo refreshApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            log.error("refreshApiKey: apiKey is empty");
            throw new IllegalArgumentException("apiKey cannot be empty");
        }

        ApiKey existingKey = getApiKeyInfo(apiKey);
        if (existingKey == null) {
            log.error("refreshApiKey: API Key not found");
            throw new IllegalArgumentException("API Key not found");
        }

        // 撤销旧密钥
        revokeApiKey(apiKey);

        // 生成新密钥
        return generateApiKey(existingKey.getClientId(), existingKey.getClientName(), 
                existingKey.getDescription());
    }

    @Override
    @Transactional
    public boolean extendApiKeyExpiration(String apiKey, int daysToAdd) {
        if (!StringUtils.hasText(apiKey) || daysToAdd <= 0) {
            log.warn("extendApiKeyExpiration: Invalid parameters");
            return false;
        }

        String hashedApiKey = Sha256Util.hash(apiKey);
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKey::getApiKey, hashedApiKey);

        ApiKey key = this.getOne(wrapper);
        if (key == null) {
            log.warn("extendApiKeyExpiration: API Key not found");
            return false;
        }

        LocalDateTime newExpiration = key.getExpiresAt() != null ? 
                key.getExpiresAt().plusDays(daysToAdd) : LocalDateTime.now().plusDays(daysToAdd);
        key.setExpiresAt(newExpiration);

        boolean updated = this.updateById(key);
        if (updated) {
            log.info("extendApiKeyExpiration: Extended API Key expiration for client: {} by {} days", 
                    key.getClientId(), daysToAdd);
        }

        return updated;
    }

    @Override
    public boolean validateApiKeyPermission(String apiKey, String permission) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(permission)) {
            log.warn("validateApiKeyPermission: Invalid parameters");
            return false;
        }

        try {
            log.info("validateApiKeyPermission: Validating permission '{}' for API Key", permission);

            // 1. 验证API密钥是否存在且有效
            String hashedApiKey = Sha256Util.hash(apiKey);
            LambdaQueryWrapper<ApiKey> keyWrapper = new LambdaQueryWrapper<>();
            keyWrapper.eq(ApiKey::getApiKey, hashedApiKey);
            keyWrapper.eq(ApiKey::getActive, 1);
            keyWrapper.gt(ApiKey::getExpiresAt, LocalDateTime.now());
            
            ApiKey apiKeyEntity = this.getOne(keyWrapper);
            if (apiKeyEntity == null) {
                log.warn("validateApiKeyPermission: API Key not found or invalid");
                return false;
            }

            // 2. 验证权限是否存在且启用
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiPermission> permissionWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            permissionWrapper.eq(ApiPermission::getPermissionCode, permission)
                             .eq(ApiPermission::getStatus, 1);
            ApiPermission apiPermission = apiPermissionService.getOne(permissionWrapper);
            if (apiPermission == null) {
                log.warn("validateApiKeyPermission: Permission '{}' not found or disabled", permission);
                return false;
            }

            // 3. 检查API密钥是否有直接权限
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiKeyPermission> keyPermWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            keyPermWrapper.eq(ApiKeyPermission::getApiKeyId, apiKeyEntity.getId())
                         .eq(ApiKeyPermission::getPermissionId, apiPermission.getId())
                         .eq(ApiKeyPermission::getStatus, 1)
                         .and(w -> w.isNull(ApiKeyPermission::getExpiresAt)
                                  .or()
                                  .gt(ApiKeyPermission::getExpiresAt, LocalDateTime.now()));
            long directPermissionCount = apiKeyPermissionService.count(keyPermWrapper);
            
            if (directPermissionCount > 0) {
                log.info("validateApiKeyPermission: API Key has direct permission '{}'", permission);
                return true;
            }

            // 4. 检查客户端是否有权限（继承权限）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClientPermission> clientPermWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            clientPermWrapper.eq(ClientPermission::getClientId, apiKeyEntity.getClientId())
                            .eq(ClientPermission::getPermissionId, apiPermission.getId())
                            .eq(ClientPermission::getStatus, 1)
                            .and(w -> w.isNull(ClientPermission::getExpiresAt)
                                     .or()
                                     .gt(ClientPermission::getExpiresAt, LocalDateTime.now()));
            long clientPermissionCount = clientPermissionService.count(clientPermWrapper);
            
            if (clientPermissionCount > 0) {
                log.info("validateApiKeyPermission: API Key inherits permission '{}' from client", permission);
                return true;
            }

            // 5. 检查是否有通配符权限
            if (hasWildcardPermission(apiKeyEntity, permission)) {
                log.info("validateApiKeyPermission: API Key has wildcard permission for '{}'", permission);
                return true;
            }

            log.warn("validateApiKeyPermission: API Key does not have permission '{}'", permission);
            return false;

        } catch (Exception e) {
            log.error("validateApiKeyPermission: Error validating permission '{}' for API Key", permission, e);
            return false;
        }
    }

    /**
     * 检查是否有通配符权限
     * 例如：如果有 api:* 权限，就可以访问所有 api: 开头的权限
     */
    private boolean hasWildcardPermission(ApiKey apiKey, String permission) {
        try {
            // 提取权限前缀（例如：api:read -> api:*）
            String permissionPrefix = permission.substring(0, permission.lastIndexOf(":") + 1) + "*";
            
            // 查找通配符权限
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiPermission> wildcardPermWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wildcardPermWrapper.eq(ApiPermission::getPermissionCode, permissionPrefix)
                              .eq(ApiPermission::getStatus, 1);
            ApiPermission wildcardPermission = apiPermissionService.getOne(wildcardPermWrapper);
            
            if (wildcardPermission == null) {
                return false;
            }
            
            // 检查API密钥是否有通配符权限
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiKeyPermission> keyWildcardWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            keyWildcardWrapper.eq(ApiKeyPermission::getApiKeyId, apiKey.getId())
                             .eq(ApiKeyPermission::getPermissionId, wildcardPermission.getId())
                             .eq(ApiKeyPermission::getStatus, 1)
                             .and(w -> w.isNull(ApiKeyPermission::getExpiresAt)
                                      .or()
                                      .gt(ApiKeyPermission::getExpiresAt, LocalDateTime.now()));
            long apiKeyWildcardCount = apiKeyPermissionService.count(keyWildcardWrapper);
            
            if (apiKeyWildcardCount > 0) {
                return true;
            }

            // 检查客户端是否有通配符权限
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClientPermission> clientWildcardWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            clientWildcardWrapper.eq(ClientPermission::getClientId, apiKey.getClientId())
                                .eq(ClientPermission::getPermissionId, wildcardPermission.getId())
                                .eq(ClientPermission::getStatus, 1)
                                .and(w -> w.isNull(ClientPermission::getExpiresAt)
                                         .or()
                                         .gt(ClientPermission::getExpiresAt, LocalDateTime.now()));
            long clientWildcardCount = clientPermissionService.count(clientWildcardWrapper);
            
            return clientWildcardCount > 0;

        } catch (Exception e) {
            log.error("hasWildcardPermission: Error checking wildcard permission", e);
            return false;
        }
    }
} 