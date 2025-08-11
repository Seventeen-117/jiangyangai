package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.ApiKeyPermission;
import com.signature.mapper.ApiKeyPermissionMapper;
import com.signature.service.ApiKeyPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API密钥权限关联Service实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class ApiKeyPermissionServiceImpl extends ServiceImpl<ApiKeyPermissionMapper, ApiKeyPermission> 
        implements ApiKeyPermissionService {

    @Override
    public List<ApiKeyPermission> findByApiKeyId(Long apiKeyId) {
        if (apiKeyId == null) {
            log.warn("findByApiKeyId: apiKeyId is null");
            return List.of();
        }

        LambdaQueryWrapper<ApiKeyPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyPermission::getApiKeyId, apiKeyId)
               .eq(ApiKeyPermission::getStatus, 1)
               .orderByDesc(ApiKeyPermission::getGrantedAt);

        List<ApiKeyPermission> result = this.list(wrapper);
        log.info("findByApiKeyId: apiKeyId={}, count={}", apiKeyId, result.size());
        return result;
    }

    @Override
    public List<ApiKeyPermission> findByApiKeyId(Long apiKeyId, LocalDateTime currentTime) {
        if (apiKeyId == null) {
            log.warn("findByApiKeyId: apiKeyId is null");
            return List.of();
        }

        LambdaQueryWrapper<ApiKeyPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyPermission::getApiKeyId, apiKeyId)
               .eq(ApiKeyPermission::getStatus, 1)
               .and(w -> w.isNull(ApiKeyPermission::getExpiresAt)
                        .or()
                        .gt(ApiKeyPermission::getExpiresAt, currentTime != null ? currentTime : LocalDateTime.now()))
               .orderByDesc(ApiKeyPermission::getGrantedAt);

        List<ApiKeyPermission> result = this.list(wrapper);
        log.info("findByApiKeyId: apiKeyId={}, currentTime={}, count={}", apiKeyId, currentTime, result.size());
        return result;
    }

    @Override
    public List<ApiKeyPermission> findByPermissionId(Long permissionId) {
        if (permissionId == null) {
            log.warn("findByPermissionId: permissionId is null");
            return List.of();
        }

        LambdaQueryWrapper<ApiKeyPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyPermission::getPermissionId, permissionId)
               .eq(ApiKeyPermission::getStatus, 1)
               .orderByDesc(ApiKeyPermission::getGrantedAt);

        List<ApiKeyPermission> result = this.list(wrapper);
        log.info("findByPermissionId: permissionId={}, count={}", permissionId, result.size());
        return result;
    }

    @Override
    public boolean hasPermission(Long apiKeyId, Long permissionId) {
        return hasPermission(apiKeyId, permissionId, LocalDateTime.now());
    }

    @Override
    public boolean hasPermission(Long apiKeyId, Long permissionId, LocalDateTime currentTime) {
        if (apiKeyId == null || permissionId == null) {
            log.warn("hasPermission: apiKeyId or permissionId is null");
            return false;
        }

        LambdaQueryWrapper<ApiKeyPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyPermission::getApiKeyId, apiKeyId)
               .eq(ApiKeyPermission::getPermissionId, permissionId)
               .eq(ApiKeyPermission::getStatus, 1)
               .and(w -> w.isNull(ApiKeyPermission::getExpiresAt)
                        .or()
                        .gt(ApiKeyPermission::getExpiresAt, currentTime != null ? currentTime : LocalDateTime.now()));

        long count = this.count(wrapper);
        boolean hasPermission = count > 0;
        log.info("hasPermission: apiKeyId={}, permissionId={}, hasPermission={}", apiKeyId, permissionId, hasPermission);
        return hasPermission;
    }

    @Override
    @Transactional
    public boolean grantPermission(Long apiKeyId, Long permissionId, String grantedBy, LocalDateTime expiresAt) {
        if (apiKeyId == null || permissionId == null) {
            log.warn("grantPermission: apiKeyId or permissionId is null");
            return false;
        }

        // 检查是否已授权
        if (hasPermission(apiKeyId, permissionId)) {
            log.warn("grantPermission: Permission already granted");
            return false;
        }

        ApiKeyPermission permission = new ApiKeyPermission();
        permission.setApiKeyId(apiKeyId);
        permission.setPermissionId(permissionId);
        permission.setGrantedAt(LocalDateTime.now());
        permission.setGrantedBy(grantedBy);
        permission.setExpiresAt(expiresAt);
        permission.setStatus(1);

        boolean saved = this.save(permission);
        log.info("grantPermission: apiKeyId={}, permissionId={}, granted={}", apiKeyId, permissionId, saved);
        return saved;
    }

    @Override
    @Transactional
    public boolean revokePermission(Long apiKeyId, Long permissionId) {
        if (apiKeyId == null || permissionId == null) {
            log.warn("revokePermission: apiKeyId or permissionId is null");
            return false;
        }

        LambdaUpdateWrapper<ApiKeyPermission> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiKeyPermission::getApiKeyId, apiKeyId)
               .eq(ApiKeyPermission::getPermissionId, permissionId)
               .set(ApiKeyPermission::getStatus, 0)
               .set(ApiKeyPermission::getUpdatedAt, LocalDateTime.now());

        boolean revoked = this.update(wrapper);
        log.info("revokePermission: apiKeyId={}, permissionId={}, revoked={}", apiKeyId, permissionId, revoked);
        return revoked;
    }

    @Override
    @Transactional
    public boolean grantPermissions(Long apiKeyId, List<Long> permissionIds, String grantedBy, LocalDateTime expiresAt) {
        if (apiKeyId == null || permissionIds == null || permissionIds.isEmpty()) {
            log.warn("grantPermissions: Invalid parameters");
            return false;
        }

        boolean allGranted = true;
        for (Long permissionId : permissionIds) {
            if (!grantPermission(apiKeyId, permissionId, grantedBy, expiresAt)) {
                allGranted = false;
            }
        }

        log.info("grantPermissions: apiKeyId={}, permissionIds={}, allGranted={}", apiKeyId, permissionIds.size(), allGranted);
        return allGranted;
    }

    @Override
    @Transactional
    public boolean revokePermissions(Long apiKeyId, List<Long> permissionIds) {
        if (apiKeyId == null || permissionIds == null || permissionIds.isEmpty()) {
            log.warn("revokePermissions: Invalid parameters");
            return false;
        }

        boolean allRevoked = true;
        for (Long permissionId : permissionIds) {
            if (!revokePermission(apiKeyId, permissionId)) {
                allRevoked = false;
            }
        }

        log.info("revokePermissions: apiKeyId={}, permissionIds={}, allRevoked={}", apiKeyId, permissionIds.size(), allRevoked);
        return allRevoked;
    }

    @Override
    @Transactional
    public int cleanupExpiredPermissions() {
        LambdaUpdateWrapper<ApiKeyPermission> wrapper = new LambdaUpdateWrapper<>();
        wrapper.lt(ApiKeyPermission::getExpiresAt, LocalDateTime.now())
               .eq(ApiKeyPermission::getStatus, 1)
               .set(ApiKeyPermission::getStatus, 0)
               .set(ApiKeyPermission::getUpdatedAt, LocalDateTime.now());

        int cleaned = this.getBaseMapper().update(null, wrapper);
        log.info("cleanupExpiredPermissions: cleaned={}", cleaned);
        return cleaned;
    }

    @Override
    public List<ApiKeyPermission> findByGrantedBy(String grantedBy) {
        if (grantedBy == null || grantedBy.trim().isEmpty()) {
            log.warn("findByGrantedBy: grantedBy is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiKeyPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyPermission::getGrantedBy, grantedBy)
               .eq(ApiKeyPermission::getStatus, 1)
               .orderByDesc(ApiKeyPermission::getGrantedAt);

        List<ApiKeyPermission> result = this.list(wrapper);
        log.info("findByGrantedBy: grantedBy={}, count={}", grantedBy, result.size());
        return result;
    }

    @Override
    public long countByApiKeyId(Long apiKeyId) {
        if (apiKeyId == null) {
            return 0;
        }

        LambdaQueryWrapper<ApiKeyPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyPermission::getApiKeyId, apiKeyId)
               .eq(ApiKeyPermission::getStatus, 1);

        long count = this.count(wrapper);
        log.info("countByApiKeyId: apiKeyId={}, count={}", apiKeyId, count);
        return count;
    }

    @Override
    public long countByPermissionId(Long permissionId) {
        if (permissionId == null) {
            return 0;
        }

        LambdaQueryWrapper<ApiKeyPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyPermission::getPermissionId, permissionId)
               .eq(ApiKeyPermission::getStatus, 1);

        long count = this.count(wrapper);
        log.info("countByPermissionId: permissionId={}, count={}", permissionId, count);
        return count;
    }
}
