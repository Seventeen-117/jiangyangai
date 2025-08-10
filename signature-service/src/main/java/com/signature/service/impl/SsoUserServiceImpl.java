package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.SsoUser;
import com.signature.mapper.SsoUserMapper;
import com.signature.service.SsoUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SSO用户服务实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class SsoUserServiceImpl extends ServiceImpl<SsoUserMapper, SsoUser> implements SsoUserService {

    @Override
    public SsoUser findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            log.warn("findByUsername: username is empty");
            return null;
        }

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getUsername, username)
                .eq(SsoUser::getEnabled, true)
                .eq(SsoUser::getLocked, false);

        SsoUser result = this.getOne(wrapper);
        log.info("findByUsername: username={}, found={}", username, result != null);
        return result;
    }

    @Override
    public SsoUser findByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            log.warn("findByUserId: userId is empty");
            return null;
        }

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getUserId, userId)
                .eq(SsoUser::getEnabled, true)
                .eq(SsoUser::getLocked, false);

        SsoUser result = this.getOne(wrapper);
        log.info("findByUserId: userId={}, found={}", userId, result != null);
        return result;
    }

    @Override
    public SsoUser findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            log.warn("findByEmail: email is empty");
            return null;
        }

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getEmail, email)
                .eq(SsoUser::getEnabled, true)
                .eq(SsoUser::getLocked, false);

        SsoUser result = this.getOne(wrapper);
        log.info("findByEmail: email={}, found={}", email, result != null);
        return result;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("validateCredentials: username or password is empty");
            return false;
        }

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getUsername, username)
                .eq(SsoUser::getPassword, password)
                .eq(SsoUser::getEnabled, true)
                .eq(SsoUser::getLocked, false);

        long count = this.count(wrapper);
        boolean valid = count > 0;
        log.info("validateCredentials: username={}, valid={}", username, valid);
        return valid;
    }

    @Override
    @Transactional
    public boolean updateLastLogin(String userId, LocalDateTime lastLoginAt, String lastLoginIp) {
        if (!StringUtils.hasText(userId)) {
            log.warn("updateLastLogin: userId is empty");
            return false;
        }

        LambdaUpdateWrapper<SsoUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SsoUser::getUserId, userId)
                .set(SsoUser::getLastLoginAt, lastLoginAt)
                .set(SsoUser::getLastLoginIp, lastLoginIp)
                .set(SsoUser::getUpdatedAt, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        log.info("updateLastLogin: userId={}, updated={}", userId, updated);
        return updated;
    }

    @Override
    @Transactional
    public SsoUser createSsoUser(SsoUser ssoUser) {
        if (ssoUser == null) {
            log.error("createSsoUser: ssoUser is null");
            return null;
        }

        // 验证必需字段
        if (!StringUtils.hasText(ssoUser.getUsername())) {
            log.error("createSsoUser: username is required");
            return null;
        }

        if (!StringUtils.hasText(ssoUser.getEmail())) {
            log.error("createSsoUser: email is required");
            return null;
        }

        if (!StringUtils.hasText(ssoUser.getUserId())) {
            log.error("createSsoUser: userId is required");
            return null;
        }

        // 检查用户名是否已存在
        if (existsByUsername(ssoUser.getUsername())) {
            log.error("createSsoUser: username already exists: {}", ssoUser.getUsername());
            return null;
        }

        // 检查邮箱是否已存在
        if (existsByEmail(ssoUser.getEmail())) {
            log.error("createSsoUser: email already exists: {}", ssoUser.getEmail());
            return null;
        }

        // 设置默认值
        if (ssoUser.getEnabled() == null) {
            ssoUser.setEnabled(true);
        }
        if (ssoUser.getLocked() == null) {
            ssoUser.setLocked(false);
        }
        if (ssoUser.getCreatedAt() == null) {
            ssoUser.setCreatedAt(LocalDateTime.now());
        }
        if (ssoUser.getUpdatedAt() == null) {
            ssoUser.setUpdatedAt(LocalDateTime.now());
        }

        boolean saved = this.save(ssoUser);
        if (saved) {
            log.info("createSsoUser: created user id={}, username={}", 
                    ssoUser.getId(), ssoUser.getUsername());
            return ssoUser;
        } else {
            log.error("createSsoUser: failed to save user");
            return null;
        }
    }

    @Override
    @Transactional
    public SsoUser updateSsoUser(SsoUser ssoUser) {
        if (ssoUser == null || ssoUser.getId() == null) {
            log.error("updateSsoUser: ssoUser or id is null");
            return null;
        }

        SsoUser existing = this.getById(ssoUser.getId());
        if (existing == null) {
            log.error("updateSsoUser: user not found, id={}", ssoUser.getId());
            return null;
        }

        // 如果更新了用户名，检查是否与其他用户冲突
        if (StringUtils.hasText(ssoUser.getUsername()) && 
            !ssoUser.getUsername().equals(existing.getUsername())) {
            
            if (existsByUsername(ssoUser.getUsername())) {
                log.error("updateSsoUser: username already exists: {}", ssoUser.getUsername());
                return null;
            }
        }

        // 如果更新了邮箱，检查是否与其他用户冲突
        if (StringUtils.hasText(ssoUser.getEmail()) && 
            !ssoUser.getEmail().equals(existing.getEmail())) {
            
            if (existsByEmail(ssoUser.getEmail())) {
                log.error("updateSsoUser: email already exists: {}", ssoUser.getEmail());
                return null;
            }
        }

        ssoUser.setUpdatedAt(LocalDateTime.now());
        boolean updated = this.updateById(ssoUser);
        if (updated) {
            log.info("updateSsoUser: updated user id={}, username={}", 
                    ssoUser.getId(), ssoUser.getUsername());
            return ssoUser;
        } else {
            log.error("updateSsoUser: failed to update user");
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteSsoUser(Long id) {
        if (id == null) {
            log.warn("deleteSsoUser: id is null");
            return false;
        }

        boolean deleted = this.removeById(id);
        log.info("deleteSsoUser: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean updateUserStatus(Long id, Boolean enabled) {
        if (id == null || enabled == null) {
            log.warn("updateUserStatus: id or enabled is null");
            return false;
        }

        LambdaUpdateWrapper<SsoUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SsoUser::getId, id)
                .set(SsoUser::getEnabled, enabled)
                .set(SsoUser::getUpdatedAt, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        log.info("updateUserStatus: id={}, enabled={}, updated={}", id, enabled, updated);
        return updated;
    }

    @Override
    @Transactional
    public boolean updateUserLockStatus(Long id, Boolean locked) {
        if (id == null || locked == null) {
            log.warn("updateUserLockStatus: id or locked is null");
            return false;
        }

        LambdaUpdateWrapper<SsoUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SsoUser::getId, id)
                .set(SsoUser::getLocked, locked)
                .set(SsoUser::getUpdatedAt, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        log.info("updateUserLockStatus: id={}, locked={}, updated={}", id, locked, updated);
        return updated;
    }

    @Override
    public List<SsoUser> findByDepartment(String department) {
        if (!StringUtils.hasText(department)) {
            log.warn("findByDepartment: department is empty");
            return List.of();
        }

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getDepartment, department)
                .eq(SsoUser::getEnabled, true)
                .orderByDesc(SsoUser::getCreatedAt);

        List<SsoUser> result = this.list(wrapper);
        log.info("findByDepartment: department={}, count={}", department, result.size());
        return result;
    }

    @Override
    public List<SsoUser> findByRole(String role) {
        if (!StringUtils.hasText(role)) {
            log.warn("findByRole: role is empty");
            return List.of();
        }

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getRole, role)
                .eq(SsoUser::getEnabled, true)
                .orderByDesc(SsoUser::getCreatedAt);

        List<SsoUser> result = this.list(wrapper);
        log.info("findByRole: role={}, count={}", role, result.size());
        return result;
    }

    @Override
    public List<SsoUser> findAllEnabled() {
        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getEnabled, true)
                .orderByDesc(SsoUser::getCreatedAt);

        List<SsoUser> result = this.list(wrapper);
        log.info("findAllEnabled: count={}", result.size());
        return result;
    }

    @Override
    public List<SsoUser> findAllUnlocked() {
        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getLocked, false)
                .eq(SsoUser::getEnabled, true)
                .orderByDesc(SsoUser::getCreatedAt);

        List<SsoUser> result = this.list(wrapper);
        log.info("findAllUnlocked: count={}", result.size());
        return result;
    }

    @Override
    @Transactional
    public int batchUpdateUserStatus(List<Long> ids, Boolean enabled) {
        if (ids == null || ids.isEmpty() || enabled == null) {
            log.warn("batchUpdateUserStatus: ids is empty or enabled is null");
            return 0;
        }

        LambdaUpdateWrapper<SsoUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(SsoUser::getId, ids)
                .set(SsoUser::getEnabled, enabled)
                .set(SsoUser::getUpdatedAt, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        int updatedCount = updated ? ids.size() : 0;
        log.info("batchUpdateUserStatus: ids={}, enabled={}, updated={}", ids, enabled, updatedCount);
        return updatedCount;
    }

    @Override
    public boolean existsByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            log.warn("existsByUsername: username is empty");
            return false;
        }

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getUsername, username);

        long count = this.count(wrapper);
        boolean exists = count > 0;
        log.info("existsByUsername: username={}, exists={}", username, exists);
        return exists;
    }

    @Override
    public boolean existsByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            log.warn("existsByEmail: email is empty");
            return false;
        }

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoUser::getEmail, email);

        long count = this.count(wrapper);
        boolean exists = count > 0;
        log.info("existsByEmail: email={}, exists={}", email, exists);
        return exists;
    }

    @Override
    @Transactional
    public boolean changePassword(String userId, String newPassword) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(newPassword)) {
            log.warn("changePassword: userId or newPassword is empty");
            return false;
        }

        LambdaUpdateWrapper<SsoUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SsoUser::getUserId, userId)
                .set(SsoUser::getPassword, newPassword)
                .set(SsoUser::getUpdatedAt, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        log.info("changePassword: userId={}, updated={}", userId, updated);
        return updated;
    }
}
