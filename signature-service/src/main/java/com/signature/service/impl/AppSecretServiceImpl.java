package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.AppSecret;
import com.signature.mapper.AppSecretMapper;
import com.signature.service.AppSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用密钥服务实现类
 *
 * @author bgpay
 * @since 2024-01-01
 */
@Slf4j
@Service
public class AppSecretServiceImpl extends ServiceImpl<AppSecretMapper, AppSecret> implements AppSecretService {

    @Override
    public AppSecret selectByAppId(String appId) {
        if (!StringUtils.hasText(appId)) {
            log.warn("selectByAppId: appId is empty");
            return null;
        }

        LambdaQueryWrapper<AppSecret> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppSecret::getAppId, appId)
                .orderByDesc(AppSecret::getCreateTime)
                .last("LIMIT 1");

        AppSecret result = this.getOne(wrapper);
        log.info("selectByAppId: appId={}, found={}", appId, result != null);
        return result;
    }

    @Override
    public AppSecret findEnabledByAppId(String appId) {
        if (!StringUtils.hasText(appId)) {
            log.warn("findEnabledByAppId: appId is empty");
            return null;
        }

        LambdaQueryWrapper<AppSecret> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppSecret::getAppId, appId)
                .eq(AppSecret::getStatus, 1)
                .eq(AppSecret::getDeleted, 0);

        AppSecret result = this.getOne(wrapper);
        log.info("findEnabledByAppId: appId={}, found={}", appId, result != null);
        return result;
    }

    @Override
    public boolean validateCredentials(String appId, String appSecret) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            log.warn("validateCredentials: appId or appSecret is empty");
            return false;
        }

        LambdaQueryWrapper<AppSecret> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppSecret::getAppId, appId)
                .eq(AppSecret::getAppSecret, appSecret)
                .eq(AppSecret::getStatus, 1)
                .eq(AppSecret::getDeleted, 0);

        long count = this.count(wrapper);
        boolean valid = count > 0;
        log.info("validateCredentials: appId={}, valid={}", appId, valid);
        return valid;
    }

    @Override
    public List<AppSecret> findAllEnabled() {
        LambdaQueryWrapper<AppSecret> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppSecret::getStatus, 1)
                .eq(AppSecret::getDeleted, 0)
                .orderByDesc(AppSecret::getCreateTime);

        List<AppSecret> result = this.list(wrapper);
        log.info("findAllEnabled: count={}", result.size());
        return result;
    }

    @Override
    public List<AppSecret> findByStatus(Integer status) {
        if (status == null) {
            log.warn("findByStatus: status is null");
            return List.of();
        }

        LambdaQueryWrapper<AppSecret> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppSecret::getStatus, status)
                .eq(AppSecret::getDeleted, 0)
                .orderByDesc(AppSecret::getCreateTime);

        List<AppSecret> result = this.list(wrapper);
        log.info("findByStatus: status={}, count={}", status, result.size());
        return result;
    }

    @Override
    @Transactional
    public AppSecret createAppSecret(AppSecret appSecret) {
        if (appSecret == null) {
            log.error("createAppSecret: appSecret is null");
            return null;
        }

        // 验证必需字段
        if (!StringUtils.hasText(appSecret.getAppId())) {
            log.error("createAppSecret: appId is required");
            return null;
        }

        if (!StringUtils.hasText(appSecret.getAppSecret())) {
            log.error("createAppSecret: appSecret is required");
            return null;
        }

        if (!StringUtils.hasText(appSecret.getAppName())) {
            log.error("createAppSecret: appName is required");
            return null;
        }

        // 检查应用ID是否已存在
        if (existsByAppId(appSecret.getAppId())) {
            log.error("createAppSecret: appId already exists: {}", appSecret.getAppId());
            return null;
        }

        // 设置默认值
        if (appSecret.getStatus() == null) {
            appSecret.setStatus(1); // 默认启用
        }
        if (appSecret.getDeleted() == null) {
            appSecret.setDeleted(0);
        }

        boolean saved = this.save(appSecret);
        if (saved) {
            log.info("createAppSecret: created app id={}, appId={}", 
                    appSecret.getId(), appSecret.getAppId());
            return appSecret;
        } else {
            log.error("createAppSecret: failed to save app");
            return null;
        }
    }

    @Override
    @Transactional
    public AppSecret updateAppSecret(AppSecret appSecret) {
        if (appSecret == null || appSecret.getId() == null) {
            log.error("updateAppSecret: appSecret or id is null");
            return null;
        }

        AppSecret existing = this.getById(appSecret.getId());
        if (existing == null) {
            log.error("updateAppSecret: app not found, id={}", appSecret.getId());
            return null;
        }

        // 如果更新了appId，检查是否与其他应用冲突
        if (StringUtils.hasText(appSecret.getAppId()) && 
            !appSecret.getAppId().equals(existing.getAppId())) {
            
            if (existsByAppId(appSecret.getAppId())) {
                log.error("updateAppSecret: appId already exists: {}", appSecret.getAppId());
                return null;
            }
        }

        boolean updated = this.updateById(appSecret);
        if (updated) {
            log.info("updateAppSecret: updated app id={}, appId={}", 
                    appSecret.getId(), appSecret.getAppId());
            return appSecret;
        } else {
            log.error("updateAppSecret: failed to update app");
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteAppSecret(Long id) {
        if (id == null) {
            log.warn("deleteAppSecret: id is null");
            return false;
        }

        // 使用逻辑删除
        boolean deleted = this.removeById(id);
        log.info("deleteAppSecret: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            log.warn("updateStatus: id or status is null");
            return false;
        }

        LambdaUpdateWrapper<AppSecret> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AppSecret::getId, id)
                .set(AppSecret::getStatus, status)
                .set(AppSecret::getUpdateTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        log.info("updateStatus: id={}, status={}, updated={}", id, status, updated);
        return updated;
    }

    @Override
    @Transactional
    public int batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty() || status == null) {
            log.warn("batchUpdateStatus: ids is empty or status is null");
            return 0;
        }

        LambdaUpdateWrapper<AppSecret> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(AppSecret::getId, ids)
                .set(AppSecret::getStatus, status)
                .set(AppSecret::getUpdateTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        int updatedCount = updated ? ids.size() : 0;
        log.info("batchUpdateStatus: ids={}, status={}, updated={}", ids, status, updatedCount);
        return updatedCount;
    }

    @Override
    public List<AppSecret> findByAppNameLike(String appName) {
        if (!StringUtils.hasText(appName)) {
            log.warn("findByAppNameLike: appName is empty");
            return List.of();
        }

        LambdaQueryWrapper<AppSecret> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(AppSecret::getAppName, appName)
                .eq(AppSecret::getDeleted, 0)
                .orderByDesc(AppSecret::getCreateTime);

        List<AppSecret> result = this.list(wrapper);
        log.info("findByAppNameLike: appName={}, count={}", appName, result.size());
        return result;
    }

    @Override
    public boolean existsByAppId(String appId) {
        if (!StringUtils.hasText(appId)) {
            log.warn("existsByAppId: appId is empty");
            return false;
        }

        LambdaQueryWrapper<AppSecret> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppSecret::getAppId, appId)
                .eq(AppSecret::getDeleted, 0);

        long count = this.count(wrapper);
        boolean exists = count > 0;
        log.info("existsByAppId: appId={}, exists={}", appId, exists);
        return exists;
    }

    @Override
    public Long countByStatus(Integer status) {
        if (status == null) {
            log.warn("countByStatus: status is null");
            return 0L;
        }

        LambdaQueryWrapper<AppSecret> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppSecret::getStatus, status)
                .eq(AppSecret::getDeleted, 0);

        long count = this.count(wrapper);
        log.info("countByStatus: status={}, count={}", status, count);
        return count;
    }
}
