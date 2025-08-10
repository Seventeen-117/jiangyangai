package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.PathConfig;
import com.signature.mapper.PathConfigMapper;
import com.signature.service.PathConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 路径配置服务实现类
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class PathConfigServiceImpl extends ServiceImpl<PathConfigMapper, PathConfig> implements PathConfigService {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public List<PathConfig> findByPathTypeAndEnabled(String pathType) {
        if (!StringUtils.hasText(pathType)) {
            log.warn("findByPathTypeAndEnabled: pathType is empty");
            return List.of();
        }

        LambdaQueryWrapper<PathConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PathConfig::getPathType, pathType)
                .eq(PathConfig::getStatus, 1)
                .orderByAsc(PathConfig::getSortOrder);

        List<PathConfig> result = this.list(wrapper);
        log.info("findByPathTypeAndEnabled: pathType={}, count={}", pathType, result.size());
        return result;
    }

    @Override
    public List<PathConfig> findByPathType(String pathType) {
        if (!StringUtils.hasText(pathType)) {
            log.warn("findByPathType: pathType is empty");
            return List.of();
        }

        LambdaQueryWrapper<PathConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PathConfig::getPathType, pathType)
                .orderByAsc(PathConfig::getSortOrder);

        List<PathConfig> result = this.list(wrapper);
        log.info("findByPathType: pathType={}, count={}", pathType, result.size());
        return result;
    }

    @Override
    public List<PathConfig> findByPathTypeAndHttpMethod(String pathType, String httpMethod) {
        if (!StringUtils.hasText(pathType) || !StringUtils.hasText(httpMethod)) {
            log.warn("findByPathTypeAndHttpMethod: pathType or httpMethod is empty");
            return List.of();
        }

        LambdaQueryWrapper<PathConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PathConfig::getPathType, pathType)
                .eq(PathConfig::getStatus, 1)
                .and(w -> w.isNull(PathConfig::getHttpMethods)
                        .or()
                        .eq(PathConfig::getHttpMethods, "")
                        .or()
                        .like(PathConfig::getHttpMethods, httpMethod))
                .orderByAsc(PathConfig::getSortOrder);

        List<PathConfig> result = this.list(wrapper);
        log.info("findByPathTypeAndHttpMethod: pathType={}, httpMethod={}, count={}", 
                pathType, httpMethod, result.size());
        return result;
    }

    @Override
    public List<PathConfig> findAllExcludedPaths() {
        return findByPathTypeAndEnabled(PathConfig.PATH_TYPE_EXCLUDED);
    }

    @Override
    public List<PathConfig> findAllStrictPaths() {
        return findByPathTypeAndEnabled(PathConfig.PATH_TYPE_STRICT);
    }

    @Override
    public List<PathConfig> findAllInternalPaths() {
        return findByPathTypeAndEnabled(PathConfig.PATH_TYPE_INTERNAL);
    }

    @Override
    @Transactional
    public PathConfig createPathConfig(PathConfig pathConfig) {
        if (pathConfig == null) {
            log.error("createPathConfig: pathConfig is null");
            return null;
        }

        // 验证必需字段
        if (!StringUtils.hasText(pathConfig.getPathPattern())) {
            log.error("createPathConfig: pathPattern is required");
            return null;
        }

        if (!StringUtils.hasText(pathConfig.getPathType())) {
            log.error("createPathConfig: pathType is required");
            return null;
        }

        // 检查路径模式是否已存在
        if (existsByPathPatternAndType(pathConfig.getPathPattern(), pathConfig.getPathType())) {
            log.error("createPathConfig: pathPattern already exists: {} with type: {}", 
                    pathConfig.getPathPattern(), pathConfig.getPathType());
            return null;
        }

        // 设置默认值
        if (pathConfig.getStatus() == null) {
            pathConfig.setStatus(1); // 默认启用
        }
        if (pathConfig.getSortOrder() == null) {
            pathConfig.setSortOrder(100); // 默认排序
        }

        boolean saved = this.save(pathConfig);
        if (saved) {
            log.info("createPathConfig: created config id={}, pathPattern={}", 
                    pathConfig.getId(), pathConfig.getPathPattern());
            return pathConfig;
        } else {
            log.error("createPathConfig: failed to save config");
            return null;
        }
    }

    @Override
    @Transactional
    public PathConfig updatePathConfig(PathConfig pathConfig) {
        if (pathConfig == null || pathConfig.getId() == null) {
            log.error("updatePathConfig: pathConfig or id is null");
            return null;
        }

        PathConfig existing = this.getById(pathConfig.getId());
        if (existing == null) {
            log.error("updatePathConfig: config not found, id={}", pathConfig.getId());
            return null;
        }

        // 如果更新了路径模式或类型，检查是否与其他配置冲突
        if ((StringUtils.hasText(pathConfig.getPathPattern()) && 
             !pathConfig.getPathPattern().equals(existing.getPathPattern())) ||
            (StringUtils.hasText(pathConfig.getPathType()) && 
             !pathConfig.getPathType().equals(existing.getPathType()))) {
            
            String newPattern = StringUtils.hasText(pathConfig.getPathPattern()) ? 
                    pathConfig.getPathPattern() : existing.getPathPattern();
            String newType = StringUtils.hasText(pathConfig.getPathType()) ? 
                    pathConfig.getPathType() : existing.getPathType();
            
            if (existsByPathPatternAndType(newPattern, newType)) {
                log.error("updatePathConfig: pathPattern already exists: {} with type: {}", 
                        newPattern, newType);
                return null;
            }
        }

        boolean updated = this.updateById(pathConfig);
        if (updated) {
            log.info("updatePathConfig: updated config id={}, pathPattern={}", 
                    pathConfig.getId(), pathConfig.getPathPattern());
            return pathConfig;
        } else {
            log.error("updatePathConfig: failed to update config");
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deletePathConfig(Long id) {
        if (id == null) {
            log.warn("deletePathConfig: id is null");
            return false;
        }

        boolean deleted = this.removeById(id);
        log.info("deletePathConfig: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            log.warn("updateStatus: id or status is null");
            return false;
        }

        LambdaUpdateWrapper<PathConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PathConfig::getId, id)
                .set(PathConfig::getStatus, status)
                .set(PathConfig::getUpdatedTime, LocalDateTime.now());

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

        LambdaUpdateWrapper<PathConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(PathConfig::getId, ids)
                .set(PathConfig::getStatus, status)
                .set(PathConfig::getUpdatedTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        int updatedCount = updated ? ids.size() : 0;
        log.info("batchUpdateStatus: ids={}, status={}, updated={}", ids, status, updatedCount);
        return updatedCount;
    }

    @Override
    public List<PathConfig> findByCategoryId(Long categoryId) {
        if (categoryId == null) {
            log.warn("findByCategoryId: categoryId is null");
            return List.of();
        }

        LambdaQueryWrapper<PathConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PathConfig::getCategoryId, categoryId)
                .eq(PathConfig::getStatus, 1)
                .orderByAsc(PathConfig::getSortOrder);

        List<PathConfig> result = this.list(wrapper);
        log.info("findByCategoryId: categoryId={}, count={}", categoryId, result.size());
        return result;
    }

    @Override
    public boolean existsByPathPatternAndType(String pathPattern, String pathType) {
        if (!StringUtils.hasText(pathPattern) || !StringUtils.hasText(pathType)) {
            log.warn("existsByPathPatternAndType: pathPattern or pathType is empty");
            return false;
        }

        LambdaQueryWrapper<PathConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PathConfig::getPathPattern, pathPattern)
                .eq(PathConfig::getPathType, pathType);

        long count = this.count(wrapper);
        boolean exists = count > 0;
        log.info("existsByPathPatternAndType: pathPattern={}, pathType={}, exists={}", 
                pathPattern, pathType, exists);
        return exists;
    }

    @Override
    public List<PathConfig> findByPathPatternLike(String pathPattern) {
        if (!StringUtils.hasText(pathPattern)) {
            log.warn("findByPathPatternLike: pathPattern is empty");
            return List.of();
        }

        LambdaQueryWrapper<PathConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(PathConfig::getPathPattern, pathPattern)
                .eq(PathConfig::getStatus, 1)
                .orderByAsc(PathConfig::getSortOrder);

        List<PathConfig> result = this.list(wrapper);
        log.info("findByPathPatternLike: pathPattern={}, count={}", pathPattern, result.size());
        return result;
    }

    @Override
    public boolean isPathMatched(String path, String method, String pathType) {
        if (!StringUtils.hasText(path) || !StringUtils.hasText(method) || !StringUtils.hasText(pathType)) {
            log.warn("isPathMatched: path, method or pathType is empty");
            return false;
        }

        List<PathConfig> configs = findByPathTypeAndHttpMethod(pathType, method);
        
        for (PathConfig config : configs) {
            if (antPathMatcher.match(config.getPathPattern(), path)) {
                log.info("isPathMatched: path={} matched pattern={} for type={}", 
                        path, config.getPathPattern(), pathType);
                return true;
            }
        }

        log.debug("isPathMatched: path={} not matched for type={} and method={}", path, pathType, method);
        return false;
    }

    @Override
    public List<PathConfig> findAllEnabledOrderBySortOrder() {
        LambdaQueryWrapper<PathConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PathConfig::getStatus, 1)
                .orderByAsc(PathConfig::getSortOrder)
                .orderByAsc(PathConfig::getId);

        List<PathConfig> result = this.list(wrapper);
        log.info("findAllEnabledOrderBySortOrder: count={}", result.size());
        return result;
    }
}
