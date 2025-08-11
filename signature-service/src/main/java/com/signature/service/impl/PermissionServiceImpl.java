package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.signature.entity.ApiPermission;
import com.signature.entity.ApiKeyPermission;
import com.signature.entity.ClientPermission;
import com.signature.service.ApiPermissionService;
import com.signature.service.ApiKeyPermissionService;
import com.signature.service.ClientPermissionService;
import com.signature.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final ApiPermissionService apiPermissionService;
    private final ApiKeyPermissionService apiKeyPermissionService;
    private final ClientPermissionService clientPermissionService;

    @Override
    @Transactional
    public boolean grantApiKeyPermission(Long apiKeyId, String permissionCode, String grantedBy, LocalDateTime expiresAt) {
        try {
            if (apiKeyId == null || !StringUtils.hasText(permissionCode)) {
                log.warn("grantApiKeyPermission: Invalid parameters");
                return false;
            }

            // 检查权限是否存在
            ApiPermission permission = apiPermissionService.findByPermissionCode(permissionCode);
            if (permission == null) {
                log.warn("grantApiKeyPermission: Permission '{}' not found", permissionCode);
                return false;
            }

            // 检查是否已经授权
            LambdaQueryWrapper<ApiKeyPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiKeyPermission::getApiKeyId, apiKeyId);
            wrapper.eq(ApiKeyPermission::getPermissionId, permission.getId());
            wrapper.eq(ApiKeyPermission::getStatus, 1);

            ApiKeyPermission existingPermission = apiKeyPermissionService.getOne(wrapper);
            if (existingPermission != null) {
                log.warn("grantApiKeyPermission: Permission already granted to API Key");
                return false;
            }

            // 创建权限关联
            ApiKeyPermission apiKeyPermission = new ApiKeyPermission();
            apiKeyPermission.setApiKeyId(apiKeyId);
            apiKeyPermission.setPermissionId(permission.getId());
            apiKeyPermission.setGrantedAt(LocalDateTime.now());
            apiKeyPermission.setGrantedBy(grantedBy);
            apiKeyPermission.setExpiresAt(expiresAt);
            apiKeyPermission.setStatus(1);
            apiKeyPermission.setCreatedAt(LocalDateTime.now());
            apiKeyPermission.setUpdatedAt(LocalDateTime.now());

            boolean saved = apiKeyPermissionService.save(apiKeyPermission);
            if (saved) {
                log.info("grantApiKeyPermission: Granted permission '{}' to API Key {}", permissionCode, apiKeyId);
            }

            return saved;
        } catch (Exception e) {
            log.error("grantApiKeyPermission: Error granting permission", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean grantClientPermission(String clientId, String permissionCode, String grantedBy, LocalDateTime expiresAt) {
        try {
            if (!StringUtils.hasText(clientId) || !StringUtils.hasText(permissionCode)) {
                log.warn("grantClientPermission: Invalid parameters");
                return false;
            }

            // 检查权限是否存在
            ApiPermission permission = apiPermissionService.findByPermissionCode(permissionCode);
            if (permission == null) {
                log.warn("grantClientPermission: Permission '{}' not found", permissionCode);
                return false;
            }

            // 检查是否已经授权
            LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ClientPermission::getClientId, clientId);
            wrapper.eq(ClientPermission::getPermissionId, permission.getId());
            wrapper.eq(ClientPermission::getStatus, 1);

            ClientPermission existingPermission = clientPermissionService.getOne(wrapper);
            if (existingPermission != null) {
                log.warn("grantClientPermission: Permission already granted to client");
                return false;
            }

            // 创建权限关联
            ClientPermission clientPermission = new ClientPermission();
            clientPermission.setClientId(clientId);
            clientPermission.setPermissionId(permission.getId());
            clientPermission.setGrantedAt(LocalDateTime.now());
            clientPermission.setGrantedBy(grantedBy);
            clientPermission.setExpiresAt(expiresAt);
            clientPermission.setStatus(1);
            clientPermission.setCreatedAt(LocalDateTime.now());
            clientPermission.setUpdatedAt(LocalDateTime.now());

            boolean saved = clientPermissionService.save(clientPermission);
            if (saved) {
                log.info("grantClientPermission: Granted permission '{}' to client {}", permissionCode, clientId);
            }

            return saved;
        } catch (Exception e) {
            log.error("grantClientPermission: Error granting permission", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean revokeApiKeyPermission(Long apiKeyId, String permissionCode) {
        try {
            if (apiKeyId == null || !StringUtils.hasText(permissionCode)) {
                log.warn("revokeApiKeyPermission: Invalid parameters");
                return false;
            }

            // 查找权限
            ApiPermission permission = apiPermissionService.findByPermissionCode(permissionCode);
            if (permission == null) {
                log.warn("revokeApiKeyPermission: Permission '{}' not found", permissionCode);
                return false;
            }

            // 撤销权限
            LambdaUpdateWrapper<ApiKeyPermission> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ApiKeyPermission::getApiKeyId, apiKeyId);
            wrapper.eq(ApiKeyPermission::getPermissionId, permission.getId());
            wrapper.set(ApiKeyPermission::getStatus, 0);
            wrapper.set(ApiKeyPermission::getUpdatedAt, LocalDateTime.now());

            boolean updated = apiKeyPermissionService.update(null, wrapper);
            if (updated) {
                log.info("revokeApiKeyPermission: Revoked permission '{}' from API Key {}", permissionCode, apiKeyId);
            }

            return updated;
        } catch (Exception e) {
            log.error("revokeApiKeyPermission: Error revoking permission", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean revokeClientPermission(String clientId, String permissionCode) {
        try {
            if (!StringUtils.hasText(clientId) || !StringUtils.hasText(permissionCode)) {
                log.warn("revokeClientPermission: Invalid parameters");
                return false;
            }

            // 查找权限
            ApiPermission permission = apiPermissionService.findByPermissionCode(permissionCode);
            if (permission == null) {
                log.warn("revokeClientPermission: Permission '{}' not found", permissionCode);
                return false;
            }

            // 撤销权限
            LambdaUpdateWrapper<ClientPermission> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ClientPermission::getClientId, clientId);
            wrapper.eq(ClientPermission::getPermissionId, permission.getId());
            wrapper.set(ClientPermission::getStatus, 0);
            wrapper.set(ClientPermission::getUpdatedAt, LocalDateTime.now());

            boolean updated = clientPermissionService.update(null, wrapper);
            if (updated) {
                log.info("revokeClientPermission: Revoked permission '{}' from client {}", permissionCode, clientId);
            }

            return updated;
        } catch (Exception e) {
            log.error("revokeClientPermission: Error revoking permission", e);
            return false;
        }
    }

    @Override
    public List<ApiPermission> getApiKeyPermissions(Long apiKeyId) {
        try {
            if (apiKeyId == null) {
                log.warn("getApiKeyPermissions: Invalid API Key ID");
                return List.of();
            }

            // 查询API密钥的权限
            List<ApiKeyPermission> apiKeyPermissions = apiKeyPermissionService.findByApiKeyId(apiKeyId, LocalDateTime.now());
            
            // 获取权限详情
            return apiKeyPermissions.stream()
                    .map(akp -> apiPermissionService.getById(akp.getPermissionId()))
                    .filter(permission -> permission != null && permission.getStatus() == 1)
                    .toList();

        } catch (Exception e) {
            log.error("getApiKeyPermissions: Error getting API Key permissions", e);
            return List.of();
        }
    }

    @Override
    public List<ApiPermission> getClientPermissions(String clientId) {
        try {
            if (!StringUtils.hasText(clientId)) {
                log.warn("getClientPermissions: Invalid client ID");
                return List.of();
            }

            // 查询客户端的权限
            List<ClientPermission> clientPermissions = clientPermissionService.findByClientId(clientId, LocalDateTime.now());
            
            // 获取权限详情
            return clientPermissions.stream()
                    .map(cp -> apiPermissionService.getById(cp.getPermissionId()))
                    .filter(permission -> permission != null && permission.getStatus() == 1)
                    .toList();

        } catch (Exception e) {
            log.error("getClientPermissions: Error getting client permissions", e);
            return List.of();
        }
    }

    @Override
    public List<ApiPermission> getAllPermissions() {
        try {
            return apiPermissionService.findAllEnabled();
        } catch (Exception e) {
            log.error("getAllPermissions: Error getting all permissions", e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public boolean createPermission(ApiPermission permission) {
        try {
            if (permission == null || !StringUtils.hasText(permission.getPermissionCode())) {
                log.warn("createPermission: Invalid permission");
                return false;
            }

            permission.setCreatedAt(LocalDateTime.now());
            permission.setUpdatedAt(LocalDateTime.now());
            if (permission.getStatus() == null) {
                permission.setStatus(1);
            }

            boolean saved = apiPermissionService.save(permission);
            if (saved) {
                log.info("createPermission: Created permission '{}'", permission.getPermissionCode());
            }

            return saved;
        } catch (Exception e) {
            log.error("createPermission: Error creating permission", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean updatePermission(ApiPermission permission) {
        try {
            if (permission == null || permission.getId() == null) {
                log.warn("updatePermission: Invalid permission");
                return false;
            }

            permission.setUpdatedAt(LocalDateTime.now());

            boolean updated = apiPermissionService.updateById(permission);
            if (updated) {
                log.info("updatePermission: Updated permission '{}'", permission.getPermissionCode());
            }

            return updated;
        } catch (Exception e) {
            log.error("updatePermission: Error updating permission", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deletePermission(String permissionCode) {
        try {
            if (!StringUtils.hasText(permissionCode)) {
                log.warn("deletePermission: Invalid permission code");
                return false;
            }

            ApiPermission permission = apiPermissionService.findByPermissionCode(permissionCode);
            if (permission == null) {
                log.warn("deletePermission: Permission '{}' not found", permissionCode);
                return false;
            }

            // 软删除权限
            permission.setStatus(0);
            permission.setUpdatedAt(LocalDateTime.now());

            boolean updated = apiPermissionService.updateById(permission);
            if (updated) {
                log.info("deletePermission: Deleted permission '{}'", permissionCode);
            }

            return updated;
        } catch (Exception e) {
            log.error("deletePermission: Error deleting permission", e);
            return false;
        }
    }

    @Override
    @Transactional
    public int batchGrantApiKeyPermissions(Long apiKeyId, List<String> permissionCodes, String grantedBy, LocalDateTime expiresAt) {
        try {
            if (apiKeyId == null || permissionCodes == null || permissionCodes.isEmpty()) {
                log.warn("batchGrantApiKeyPermissions: Invalid parameters");
                return 0;
            }

            int successCount = 0;
            for (String permissionCode : permissionCodes) {
                if (grantApiKeyPermission(apiKeyId, permissionCode, grantedBy, expiresAt)) {
                    successCount++;
                }
            }

            log.info("batchGrantApiKeyPermissions: Granted {} permissions to API Key {}", successCount, apiKeyId);
            return successCount;
        } catch (Exception e) {
            log.error("batchGrantApiKeyPermissions: Error batch granting permissions", e);
            return 0;
        }
    }

    @Override
    @Transactional
    public int batchGrantClientPermissions(String clientId, List<String> permissionCodes, String grantedBy, LocalDateTime expiresAt) {
        try {
            if (!StringUtils.hasText(clientId) || permissionCodes == null || permissionCodes.isEmpty()) {
                log.warn("batchGrantClientPermissions: Invalid parameters");
                return 0;
            }

            int successCount = 0;
            for (String permissionCode : permissionCodes) {
                if (grantClientPermission(clientId, permissionCode, grantedBy, expiresAt)) {
                    successCount++;
                }
            }

            log.info("batchGrantClientPermissions: Granted {} permissions to client {}", successCount, clientId);
            return successCount;
        } catch (Exception e) {
            log.error("batchGrantClientPermissions: Error batch granting permissions", e);
            return 0;
        }
    }
}
