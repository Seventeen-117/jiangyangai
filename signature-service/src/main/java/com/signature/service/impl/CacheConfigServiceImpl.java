package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.CacheConfig;
import com.signature.mapper.CacheConfigMapper;
import com.signature.service.CacheConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存配置服务实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class CacheConfigServiceImpl extends ServiceImpl<CacheConfigMapper, CacheConfig> implements CacheConfigService {

    @Override
    public CacheConfig findByCacheKey(String cacheKey) {
        if (!StringUtils.hasText(cacheKey)) {
            log.warn("findByCacheKey: cacheKey is empty");
            return null;
        }

        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheConfig::getCacheKey, cacheKey)
                .eq(CacheConfig::getStatus, 1);

        CacheConfig result = this.getOne(wrapper);
        log.info("findByCacheKey: cacheKey={}, found={}", cacheKey, result != null);
        return result;
    }

    @Override
    public List<CacheConfig> findByCacheType(String cacheType) {
        if (!StringUtils.hasText(cacheType)) {
            log.warn("findByCacheType: cacheType is empty");
            return List.of();
        }

        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheConfig::getCacheType, cacheType)
                .eq(CacheConfig::getStatus, 1)
                .orderByAsc(CacheConfig::getCacheKey);

        List<CacheConfig> result = this.list(wrapper);
        log.info("findByCacheType: cacheType={}, count={}", cacheType, result.size());
        return result;
    }

    @Override
    public List<CacheConfig> findAllEnabled() {
        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheConfig::getStatus, 1)
                .orderByAsc(CacheConfig::getCacheKey);

        List<CacheConfig> result = this.list(wrapper);
        log.info("findAllEnabled: count={}", result.size());
        return result;
    }

    @Override
    public List<CacheConfig> findByStatus(Integer status) {
        if (status == null) {
            log.warn("findByStatus: status is null");
            return List.of();
        }

        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheConfig::getStatus, status)
                .orderByAsc(CacheConfig::getCacheKey);

        List<CacheConfig> result = this.list(wrapper);
        log.info("findByStatus: status={}, count={}", status, result.size());
        return result;
    }

    @Override
    public List<CacheConfig> findByCacheNameLike(String cacheName) {
        if (!StringUtils.hasText(cacheName)) {
            log.warn("findByCacheNameLike: cacheName is empty");
            return List.of();
        }

        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(CacheConfig::getCacheName, cacheName)
                .eq(CacheConfig::getStatus, 1)
                .orderByAsc(CacheConfig::getCacheKey);

        List<CacheConfig> result = this.list(wrapper);
        log.info("findByCacheNameLike: cacheName={}, count={}", cacheName, result.size());
        return result;
    }

    @Override
    @Transactional
    public CacheConfig createCacheConfig(CacheConfig cacheConfig) {
        if (cacheConfig == null) {
            log.error("createCacheConfig: cacheConfig is null");
            return null;
        }

        // 验证必需字段
        if (!StringUtils.hasText(cacheConfig.getCacheKey())) {
            log.error("createCacheConfig: cacheKey is required");
            return null;
        }

        if (!StringUtils.hasText(cacheConfig.getCacheName())) {
            log.error("createCacheConfig: cacheName is required");
            return null;
        }

        // 检查缓存键是否已存在
        if (existsByCacheKey(cacheConfig.getCacheKey())) {
            log.error("createCacheConfig: cacheKey already exists: {}", cacheConfig.getCacheKey());
            return null;
        }

        // 设置默认值
        if (cacheConfig.getStatus() == null) {
            cacheConfig.setStatus(1); // 默认启用
        }
        if (cacheConfig.getExpireSeconds() == null) {
            cacheConfig.setExpireSeconds(3600); // 默认1小时
        }
        if (cacheConfig.getMaxSize() == null) {
            cacheConfig.setMaxSize(1000); // 默认最大1000条
        }

        boolean saved = this.save(cacheConfig);
        if (saved) {
            log.info("createCacheConfig: created config id={}, cacheKey={}", 
                    cacheConfig.getId(), cacheConfig.getCacheKey());
            return cacheConfig;
        } else {
            log.error("createCacheConfig: failed to save config");
            return null;
        }
    }

    @Override
    @Transactional
    public CacheConfig updateCacheConfig(CacheConfig cacheConfig) {
        if (cacheConfig == null || cacheConfig.getId() == null) {
            log.error("updateCacheConfig: cacheConfig or id is null");
            return null;
        }

        CacheConfig existing = this.getById(cacheConfig.getId());
        if (existing == null) {
            log.error("updateCacheConfig: config not found, id={}", cacheConfig.getId());
            return null;
        }

        // 如果更新了cacheKey，检查是否与其他配置冲突
        if (StringUtils.hasText(cacheConfig.getCacheKey()) && 
            !cacheConfig.getCacheKey().equals(existing.getCacheKey())) {
            
            if (existsByCacheKey(cacheConfig.getCacheKey())) {
                log.error("updateCacheConfig: cacheKey already exists: {}", cacheConfig.getCacheKey());
                return null;
            }
        }

        boolean updated = this.updateById(cacheConfig);
        if (updated) {
            log.info("updateCacheConfig: updated config id={}, cacheKey={}", 
                    cacheConfig.getId(), cacheConfig.getCacheKey());
            return cacheConfig;
        } else {
            log.error("updateCacheConfig: failed to update config");
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteCacheConfig(Long id) {
        if (id == null) {
            log.warn("deleteCacheConfig: id is null");
            return false;
        }

        boolean deleted = this.removeById(id);
        log.info("deleteCacheConfig: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            log.warn("updateStatus: id or status is null");
            return false;
        }

        LambdaUpdateWrapper<CacheConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CacheConfig::getId, id)
                .set(CacheConfig::getStatus, status)
                .set(CacheConfig::getUpdatedTime, LocalDateTime.now());

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

        LambdaUpdateWrapper<CacheConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(CacheConfig::getId, ids)
                .set(CacheConfig::getStatus, status)
                .set(CacheConfig::getUpdatedTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        int updatedCount = updated ? ids.size() : 0;
        log.info("batchUpdateStatus: ids={}, status={}, updated={}", ids, status, updatedCount);
        return updatedCount;
    }

    @Override
    public boolean existsByCacheKey(String cacheKey) {
        if (!StringUtils.hasText(cacheKey)) {
            log.warn("existsByCacheKey: cacheKey is empty");
            return false;
        }

        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheConfig::getCacheKey, cacheKey);

        long count = this.count(wrapper);
        boolean exists = count > 0;
        log.info("existsByCacheKey: cacheKey={}, exists={}", cacheKey, exists);
        return exists;
    }

    @Override
    public CacheConfig findByCacheKeyAndType(String cacheKey, String cacheType) {
        if (!StringUtils.hasText(cacheKey) || !StringUtils.hasText(cacheType)) {
            log.warn("findByCacheKeyAndType: cacheKey or cacheType is empty");
            return null;
        }

        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheConfig::getCacheKey, cacheKey)
                .eq(CacheConfig::getCacheType, cacheType)
                .eq(CacheConfig::getStatus, 1);

        CacheConfig result = this.getOne(wrapper);
        log.info("findByCacheKeyAndType: cacheKey={}, cacheType={}, found={}", 
                cacheKey, cacheType, result != null);
        return result;
    }

    @Override
    public Object getCacheConfigStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 统计总配置数
        long totalCount = this.count();
        statistics.put("total", totalCount);

        // 统计启用配置数
        LambdaQueryWrapper<CacheConfig> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(CacheConfig::getStatus, 1);
        long enabledCount = this.count(enabledWrapper);
        statistics.put("enabled", enabledCount);

        // 统计禁用配置数
        LambdaQueryWrapper<CacheConfig> disabledWrapper = new LambdaQueryWrapper<>();
        disabledWrapper.eq(CacheConfig::getStatus, 0);
        long disabledCount = this.count(disabledWrapper);
        statistics.put("disabled", disabledCount);

        // 按缓存类型统计
        List<CacheConfig> configs = this.list();
        Map<String, Long> cacheTypeStats = configs.stream()
                .filter(config -> StringUtils.hasText(config.getCacheType()))
                .collect(java.util.stream.Collectors.groupingBy(
                        CacheConfig::getCacheType,
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("byCacheType", cacheTypeStats);

        log.info("getCacheConfigStatistics: statistics={}", statistics);
        return statistics;
    }

    @Override
    public List<CacheConfig> findByExpireSecondsRange(Integer minExpireSeconds, Integer maxExpireSeconds) {
        if (minExpireSeconds == null && maxExpireSeconds == null) {
            log.warn("findByExpireSecondsRange: both minExpireSeconds and maxExpireSeconds are null");
            return List.of();
        }

        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheConfig::getStatus, 1);
        
        if (minExpireSeconds != null) {
            wrapper.ge(CacheConfig::getExpireSeconds, minExpireSeconds);
        }
        if (maxExpireSeconds != null) {
            wrapper.le(CacheConfig::getExpireSeconds, maxExpireSeconds);
        }
        
        wrapper.orderByAsc(CacheConfig::getExpireSeconds);

        List<CacheConfig> result = this.list(wrapper);
        log.info("findByExpireSecondsRange: min={}, max={}, count={}", 
                minExpireSeconds, maxExpireSeconds, result.size());
        return result;
    }

    @Override
    @Transactional
    public int cleanExpiredConfigs() {
        // 这里可以根据业务需求定义"过期"的逻辑
        // 例如：删除创建时间超过一定时间且状态为禁用的配置
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(30);
        
        LambdaQueryWrapper<CacheConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheConfig::getStatus, 0) // 禁用状态
                .lt(CacheConfig::getCreatedTime, thresholdTime);

        List<CacheConfig> expiredConfigs = this.list(wrapper);
        int cleanedCount = 0;
        
        if (!expiredConfigs.isEmpty()) {
            List<Long> idsToDelete = expiredConfigs.stream()
                    .map(CacheConfig::getId)
                    .toList();
            
            boolean deleted = this.removeByIds(idsToDelete);
            cleanedCount = deleted ? expiredConfigs.size() : 0;
        }

        log.info("cleanExpiredConfigs: cleaned {} expired configs", cleanedCount);
        return cleanedCount;
    }
}
