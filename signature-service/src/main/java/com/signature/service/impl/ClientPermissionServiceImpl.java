package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.ClientPermission;
import com.signature.mapper.ClientPermissionMapper;
import com.signature.service.ClientPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户端权限Service实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class ClientPermissionServiceImpl extends ServiceImpl<ClientPermissionMapper, ClientPermission> 
        implements ClientPermissionService {

    @Override
    public List<ClientPermission> findByClientId(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            log.warn("findByClientId: clientId is empty");
            return List.of();
        }

        LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClientPermission::getClientId, clientId)
               .eq(ClientPermission::getStatus, 1)
               .orderByDesc(ClientPermission::getGrantedAt);

        List<ClientPermission> result = this.list(wrapper);
        log.info("findByClientId: clientId={}, count={}", clientId, result.size());
        return result;
    }

    @Override
    public List<ClientPermission> findByClientId(String clientId, LocalDateTime currentTime) {
        if (clientId == null || clientId.trim().isEmpty()) {
            log.warn("findByClientId: clientId is empty");
            return List.of();
        }

        LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClientPermission::getClientId, clientId)
               .eq(ClientPermission::getStatus, 1)
               .and(w -> w.isNull(ClientPermission::getExpiresAt)
                        .or()
                        .gt(ClientPermission::getExpiresAt, currentTime != null ? currentTime : LocalDateTime.now()))
               .orderByDesc(ClientPermission::getGrantedAt);

        List<ClientPermission> result = this.list(wrapper);
        log.info("findByClientId: clientId={}, currentTime={}, count={}", clientId, currentTime, result.size());
        return result;
    }

    @Override
    public List<ClientPermission> findByPermissionId(Long permissionId) {
        if (permissionId == null) {
            log.warn("findByPermissionId: permissionId is null");
            return List.of();
        }

        LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClientPermission::getPermissionId, permissionId)
               .eq(ClientPermission::getStatus, 1)
               .orderByDesc(ClientPermission::getGrantedAt);

        List<ClientPermission> result = this.list(wrapper);
        log.info("findByPermissionId: permissionId={}, count={}", permissionId, result.size());
        return result;
    }

    @Override
    public boolean hasPermission(String clientId, Long permissionId) {
        return hasPermission(clientId, permissionId, LocalDateTime.now());
    }

    @Override
    public boolean hasPermission(String clientId, Long permissionId, LocalDateTime currentTime) {
        if (clientId == null || clientId.trim().isEmpty() || permissionId == null) {
            log.warn("hasPermission: clientId or permissionId is invalid");
            return false;
        }

        LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClientPermission::getClientId, clientId)
               .eq(ClientPermission::getPermissionId, permissionId)
               .eq(ClientPermission::getStatus, 1)
               .and(w -> w.isNull(ClientPermission::getExpiresAt)
                        .or()
                        .gt(ClientPermission::getExpiresAt, currentTime != null ? currentTime : LocalDateTime.now()));

        long count = this.count(wrapper);
        boolean hasPermission = count > 0;
        log.info("hasPermission: clientId={}, permissionId={}, hasPermission={}", clientId, permissionId, hasPermission);
        return hasPermission;
    }

    @Override
    @Transactional
    public boolean grantPermission(String clientId, Long permissionId, String grantedBy, LocalDateTime expiresAt) {
        if (clientId == null || clientId.trim().isEmpty() || permissionId == null) {
            log.warn("grantPermission: clientId or permissionId is invalid");
            return false;
        }

        // 检查是否已授权
        if (hasPermission(clientId, permissionId)) {
            log.warn("grantPermission: Permission already granted");
            return false;
        }

        ClientPermission permission = new ClientPermission();
        permission.setClientId(clientId);
        permission.setPermissionId(permissionId);
        permission.setGrantedAt(LocalDateTime.now());
        permission.setGrantedBy(grantedBy);
        permission.setExpiresAt(expiresAt);
        permission.setStatus(1);

        boolean saved = this.save(permission);
        log.info("grantPermission: clientId={}, permissionId={}, granted={}", clientId, permissionId, saved);
        return saved;
    }

    @Override
    @Transactional
    public boolean revokePermission(String clientId, Long permissionId) {
        if (clientId == null || clientId.trim().isEmpty() || permissionId == null) {
            log.warn("revokePermission: clientId or permissionId is invalid");
            return false;
        }

        LambdaUpdateWrapper<ClientPermission> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ClientPermission::getClientId, clientId)
               .eq(ClientPermission::getPermissionId, permissionId)
               .set(ClientPermission::getStatus, 0)
               .set(ClientPermission::getUpdatedAt, LocalDateTime.now());

        boolean revoked = this.update(wrapper);
        log.info("revokePermission: clientId={}, permissionId={}, revoked={}", clientId, permissionId, revoked);
        return revoked;
    }

    @Override
    @Transactional
    public boolean grantPermissions(String clientId, List<Long> permissionIds, String grantedBy, LocalDateTime expiresAt) {
        if (clientId == null || clientId.trim().isEmpty() || permissionIds == null || permissionIds.isEmpty()) {
            log.warn("grantPermissions: Invalid parameters");
            return false;
        }

        boolean allGranted = true;
        for (Long permissionId : permissionIds) {
            if (!grantPermission(clientId, permissionId, grantedBy, expiresAt)) {
                allGranted = false;
            }
        }

        log.info("grantPermissions: clientId={}, permissionIds={}, allGranted={}", clientId, permissionIds.size(), allGranted);
        return allGranted;
    }

    @Override
    @Transactional
    public boolean revokePermissions(String clientId, List<Long> permissionIds) {
        if (clientId == null || clientId.trim().isEmpty() || permissionIds == null || permissionIds.isEmpty()) {
            log.warn("revokePermissions: Invalid parameters");
            return false;
        }

        boolean allRevoked = true;
        for (Long permissionId : permissionIds) {
            if (!revokePermission(clientId, permissionId)) {
                allRevoked = false;
            }
        }

        log.info("revokePermissions: clientId={}, permissionIds={}, allRevoked={}", clientId, permissionIds.size(), allRevoked);
        return allRevoked;
    }

    @Override
    @Transactional
    public int cleanupExpiredPermissions() {
        LambdaUpdateWrapper<ClientPermission> wrapper = new LambdaUpdateWrapper<>();
        wrapper.lt(ClientPermission::getExpiresAt, LocalDateTime.now())
               .eq(ClientPermission::getStatus, 1)
               .set(ClientPermission::getStatus, 0)
               .set(ClientPermission::getUpdatedAt, LocalDateTime.now());

        int cleaned = this.getBaseMapper().update(null, wrapper);
        log.info("cleanupExpiredPermissions: cleaned={}", cleaned);
        return cleaned;
    }

    @Override
    public List<ClientPermission> findByGrantedBy(String grantedBy) {
        if (grantedBy == null || grantedBy.trim().isEmpty()) {
            log.warn("findByGrantedBy: grantedBy is empty");
            return List.of();
        }

        LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClientPermission::getGrantedBy, grantedBy)
               .eq(ClientPermission::getStatus, 1)
               .orderByDesc(ClientPermission::getGrantedAt);

        List<ClientPermission> result = this.list(wrapper);
        log.info("findByGrantedBy: grantedBy={}, count={}", grantedBy, result.size());
        return result;
    }

    @Override
    public long countByClientId(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return 0;
        }

        LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClientPermission::getClientId, clientId)
               .eq(ClientPermission::getStatus, 1);

        long count = this.count(wrapper);
        log.info("countByClientId: clientId={}, count={}", clientId, count);
        return count;
    }

    @Override
    public long countByPermissionId(Long permissionId) {
        if (permissionId == null) {
            return 0;
        }

        LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClientPermission::getPermissionId, permissionId)
               .eq(ClientPermission::getStatus, 1);

        long count = this.count(wrapper);
        log.info("countByPermissionId: permissionId={}, count={}", permissionId, count);
        return count;
    }

    @Override
    public List<ClientPermission> findByClientIds(List<String> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            log.warn("findByClientIds: clientIds is empty");
            return List.of();
        }

        LambdaQueryWrapper<ClientPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ClientPermission::getClientId, clientIds)
               .eq(ClientPermission::getStatus, 1)
               .orderByDesc(ClientPermission::getGrantedAt);

        List<ClientPermission> result = this.list(wrapper);
        log.info("findByClientIds: clientIds={}, count={}", clientIds.size(), result.size());
        return result;
    }
}
