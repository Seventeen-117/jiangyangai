package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.signature.entity.ApiConfig;
import com.signature.mapper.ApiConfigMapper;
import com.signature.service.ApiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * API配置服务实现类
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
public class ApiConfigServiceImpl extends ServiceImpl<ApiConfigMapper, ApiConfig> implements ApiConfigService {


    @Override
    public ApiConfig getLatestConfig(String userId) {
        if (!StringUtils.hasText(userId)) {
            log.warn("getLatestConfig: userId is empty");
            return null;
        }

        LambdaQueryWrapper<ApiConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiConfig::getUserId, userId);
        queryWrapper.eq(ApiConfig::getEnabled, true);
        queryWrapper.orderByDesc(ApiConfig::getId);
        queryWrapper.last("LIMIT 1");

        ApiConfig result = this.getOne(queryWrapper);
        log.info("getLatestConfig: userId={}, found={}", userId, result != null);
        return result;
    }

    @Override
    public ApiConfig findMatchingConfig(String userId, String apiUrl, String apiKey, String modelName) {
        if (!StringUtils.hasText(userId)) {
            log.warn("findMatchingConfig: userId is empty");
            return null;
        }

        LambdaQueryWrapper<ApiConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiConfig::getUserId, userId);
        queryWrapper.eq(ApiConfig::getEnabled, true);

        log.info("findMatchingConfig: userId={}", userId);

        // 根据传入的参数添加条件，如果某个参数为null或空字符串则不添加该条件
        if (StringUtils.hasText(apiUrl)) {
            queryWrapper.eq(ApiConfig::getApiUrl, apiUrl);
            log.info("findMatchingConfig: added apiUrl condition");
        }

        if (StringUtils.hasText(apiKey)) {
            queryWrapper.eq(ApiConfig::getApiKey, apiKey);
            log.info("findMatchingConfig: added apiKey condition");
        }

        if (StringUtils.hasText(modelName)) {
            queryWrapper.eq(ApiConfig::getModelName, modelName);
            log.info("findMatchingConfig: added modelName condition");
        }

        // 按优先级和ID降序排列，获取最佳匹配的配置
        queryWrapper.orderByAsc(ApiConfig::getPriority);
        queryWrapper.orderByDesc(ApiConfig::getId);
        queryWrapper.last("LIMIT 1");

        ApiConfig result = this.getOne(queryWrapper, false);
        if (result != null) {
            log.info("findMatchingConfig: found config id={}, modelName={}, priority={}", 
                    result.getId(), result.getModelName(), result.getPriority());
        } else {
            log.info("findMatchingConfig: no matching config found");
        }

        return result;
    }

    @Override
    public ApiConfig findAlternativeConfig(String userId, String currentModelName) {
        if (!StringUtils.hasText(userId)) {
            log.warn("findAlternativeConfig: userId is empty");
            return null;
        }

        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getUserId, userId);
        wrapper.eq(ApiConfig::getEnabled, true);
        
        if (StringUtils.hasText(currentModelName)) {
            wrapper.ne(ApiConfig::getModelName, currentModelName);
        }
        
        wrapper.orderByAsc(ApiConfig::getPriority);
        wrapper.orderByDesc(ApiConfig::getId);
        wrapper.last("LIMIT 1");

        ApiConfig result = this.getOne(wrapper);
        log.info("findAlternativeConfig: userId={}, currentModel={}, found={}", 
                userId, currentModelName, result != null);
        return result;
    }

    @Override
    public List<ApiConfig> getAllApiConfigs() {
        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ApiConfig::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public ApiConfig getApiConfigById(Long id) {
        if (id == null) {
            log.warn("getApiConfigById: id is null");
            return null;
        }
        return this.getById(id);
    }

    @Override
    @Transactional
    public ApiConfig createApiConfig(ApiConfig apiConfig) {
        if (apiConfig == null) {
            log.error("createApiConfig: apiConfig is null");
            return null;
        }

        // 验证配置
        if (!validateApiConfig(apiConfig)) {
            log.error("createApiConfig: validation failed");
            return null;
        }

        // 设置默认值
        apiConfig.setCreateTime(LocalDateTime.now());
        apiConfig.setUpdateTime(LocalDateTime.now());
        apiConfig.setEnabled(apiConfig.getEnabled() != null ? apiConfig.getEnabled() : true);
        apiConfig.setIsDefault(apiConfig.getIsDefault() != null ? apiConfig.getIsDefault() : false);
        apiConfig.setPriority(apiConfig.getPriority() != null ? apiConfig.getPriority() : 100);

        // 如果设置为默认配置，先清除其他默认配置
        if (Boolean.TRUE.equals(apiConfig.getIsDefault())) {
            clearDefaultConfig(apiConfig.getUserId());
        }

        boolean saved = this.save(apiConfig);
        if (saved) {
            log.info("createApiConfig: created config id={}, userId={}, modelName={}", 
                    apiConfig.getId(), apiConfig.getUserId(), apiConfig.getModelName());
            return apiConfig;
        } else {
            log.error("createApiConfig: failed to save config");
            return null;
        }
    }

    @Override
    @Transactional
    public ApiConfig updateApiConfig(ApiConfig apiConfig) {
        if (apiConfig == null || apiConfig.getId() == null) {
            log.error("updateApiConfig: apiConfig or id is null");
            return null;
        }

        ApiConfig existing = this.getById(apiConfig.getId());
        if (existing == null) {
            log.error("updateApiConfig: config not found, id={}", apiConfig.getId());
            return null;
        }

        // 验证配置
        if (!validateApiConfig(apiConfig)) {
            log.error("updateApiConfig: validation failed");
            return null;
        }

        apiConfig.setUpdateTime(LocalDateTime.now());

        // 如果设置为默认配置，先清除其他默认配置
        if (Boolean.TRUE.equals(apiConfig.getIsDefault())) {
            clearDefaultConfig(apiConfig.getUserId());
        }

        boolean updated = this.updateById(apiConfig);
        if (updated) {
            log.info("updateApiConfig: updated config id={}, userId={}, modelName={}", 
                    apiConfig.getId(), apiConfig.getUserId(), apiConfig.getModelName());
            return apiConfig;
        } else {
            log.error("updateApiConfig: failed to update config");
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteApiConfig(Long id) {
        if (id == null) {
            log.warn("deleteApiConfig: id is null");
            return false;
        }

        boolean deleted = this.removeById(id);
        log.info("deleteApiConfig: id={}, deleted={}", id, deleted);
        return deleted;
    }

    @Override
    public ApiConfig getApiConfigByModelType(String modelType) {
        if (!StringUtils.hasText(modelType)) {
            log.warn("getApiConfigByModelType: modelType is empty");
            return null;
        }

        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getModelType, modelType);
        wrapper.eq(ApiConfig::getEnabled, true);
        wrapper.orderByAsc(ApiConfig::getPriority);
        wrapper.last("LIMIT 1");

        return this.getOne(wrapper);
    }

    @Override
    public ApiConfig getDefaultApiConfig() {
        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getIsDefault, true);
        wrapper.eq(ApiConfig::getEnabled, true);
        return this.getOne(wrapper);
    }

    @Override
    @Transactional
    public boolean setDefaultApiConfig(Long id) {
        if (id == null) {
            log.warn("setDefaultApiConfig: id is null");
            return false;
        }

        ApiConfig config = this.getById(id);
        if (config == null) {
            log.error("setDefaultApiConfig: config not found, id={}", id);
            return false;
        }

        // 先将所有配置设为非默认
        clearDefaultConfig(config.getUserId());

        // 将指定ID的配置设为默认
        config.setIsDefault(true);
        config.setUpdateTime(LocalDateTime.now());
        boolean updated = this.updateById(config);

        log.info("setDefaultApiConfig: id={}, userId={}, updated={}", id, config.getUserId(), updated);
        return updated;
    }

    @Override
    public List<ApiConfig> getConfigsByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            log.warn("getConfigsByUserId: userId is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getUserId, userId);
        wrapper.orderByDesc(ApiConfig::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<ApiConfig> getConfigsByApiType(String userId, String apiType) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(apiType)) {
            log.warn("getConfigsByApiType: userId or apiType is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getUserId, userId);
        wrapper.eq(ApiConfig::getApiType, apiType);
        wrapper.eq(ApiConfig::getEnabled, true);
        wrapper.orderByAsc(ApiConfig::getPriority);
        return this.list(wrapper);
    }

    @Override
    public List<ApiConfig> getHighPriorityConfigs(String userId, Integer limit) {
        if (!StringUtils.hasText(userId)) {
            log.warn("getHighPriorityConfigs: userId is empty");
            return List.of();
        }

        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getUserId, userId);
        wrapper.eq(ApiConfig::getEnabled, true);
        wrapper.orderByAsc(ApiConfig::getPriority);
        wrapper.orderByDesc(ApiConfig::getCreateTime);
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }

        return this.list(wrapper);
    }

    @Override
    public boolean validateApiConfig(ApiConfig apiConfig) {
        if (apiConfig == null) {
            log.error("validateApiConfig: apiConfig is null");
            return false;
        }

        // 验证必需字段
        if (!StringUtils.hasText(apiConfig.getUserId())) {
            log.error("validateApiConfig: userId is required");
            return false;
        }

        if (!StringUtils.hasText(apiConfig.getApiUrl())) {
            log.error("validateApiConfig: apiUrl is required");
            return false;
        }

        if (!StringUtils.hasText(apiConfig.getApiKey())) {
            log.error("validateApiConfig: apiKey is required");
            return false;
        }

        if (!StringUtils.hasText(apiConfig.getModelName())) {
            log.error("validateApiConfig: modelName is required");
            return false;
        }

        // 验证数值字段
        if (apiConfig.getMaxTokens() != null && apiConfig.getMaxTokens() <= 0) {
            log.error("validateApiConfig: maxTokens must be positive");
            return false;
        }

        if (apiConfig.getTemperature() != null && (apiConfig.getTemperature() < 0 || apiConfig.getTemperature() > 2)) {
            log.error("validateApiConfig: temperature must be between 0 and 2");
            return false;
        }

        if (apiConfig.getPriority() != null && apiConfig.getPriority() < 0) {
            log.error("validateApiConfig: priority must be non-negative");
            return false;
        }

        log.info("validateApiConfig: validation passed for userId={}, modelName={}", 
                apiConfig.getUserId(), apiConfig.getModelName());
        return true;
    }

    @Override
    public boolean testApiConnection(ApiConfig apiConfig) {
        if (apiConfig == null) {
            log.error("testApiConnection: apiConfig is null");
            return false;
        }

        // 这里应该实现真实的API连接测试
        // 目前返回模拟结果
        log.info("testApiConnection: testing connection for userId={}, modelName={}", 
                apiConfig.getUserId(), apiConfig.getModelName());
        
        // 模拟连接测试
        try {
            Thread.sleep(100); // 模拟网络延迟
            boolean success = Math.random() > 0.1; // 90%成功率
            log.info("testApiConnection: test result={}", success);
            return success;
        } catch (InterruptedException e) {
            log.error("testApiConnection: interrupted", e);
            return false;
        }
    }

    @Override
    @Transactional
    public int batchUpdateStatus(List<Long> ids, Boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            log.warn("batchUpdateStatus: ids is empty");
            return 0;
        }

        if (enabled == null) {
            log.warn("batchUpdateStatus: enabled is null");
            return 0;
        }

        LambdaUpdateWrapper<ApiConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ApiConfig::getId, ids);
        wrapper.set(ApiConfig::getEnabled, enabled);
        wrapper.set(ApiConfig::getUpdateTime, LocalDateTime.now());

        boolean updated = this.update(wrapper);
        int updatedCount = updated ? 1 : 0;
        log.info("batchUpdateStatus: ids={}, enabled={}, updated={}", ids, enabled, updatedCount);
        return updatedCount;
    }

    @Override
    public Object getConfigStatistics(String userId) {
        if (!StringUtils.hasText(userId)) {
            log.warn("getConfigStatistics: userId is empty");
            return Map.of();
        }

        Map<String, Object> statistics = new HashMap<>();
        
        // 统计总配置数
        LambdaQueryWrapper<ApiConfig> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(ApiConfig::getUserId, userId);
        long totalCount = this.count(totalWrapper);
        statistics.put("total", totalCount);

        // 统计启用配置数
        LambdaQueryWrapper<ApiConfig> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(ApiConfig::getUserId, userId);
        enabledWrapper.eq(ApiConfig::getEnabled, true);
        long enabledCount = this.count(enabledWrapper);
        statistics.put("enabled", enabledCount);

        // 统计默认配置数
        LambdaQueryWrapper<ApiConfig> defaultWrapper = new LambdaQueryWrapper<>();
        defaultWrapper.eq(ApiConfig::getUserId, userId);
        defaultWrapper.eq(ApiConfig::getIsDefault, true);
        long defaultCount = this.count(defaultWrapper);
        statistics.put("default", defaultCount);

        // 按API类型统计
        List<ApiConfig> configs = getConfigsByUserId(userId);
        Map<String, Long> apiTypeStats = configs.stream()
                .filter(config -> StringUtils.hasText(config.getApiType()))
                .collect(java.util.stream.Collectors.groupingBy(
                        ApiConfig::getApiType,
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("byApiType", apiTypeStats);

        // 按模型类型统计
        Map<String, Long> modelTypeStats = configs.stream()
                .filter(config -> StringUtils.hasText(config.getModelType()))
                .collect(java.util.stream.Collectors.groupingBy(
                        ApiConfig::getModelType,
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("byModelType", modelTypeStats);

        log.info("getConfigStatistics: userId={}, statistics={}", userId, statistics);
        return statistics;
    }

    /**
     * 清除用户的默认配置
     */
    private void clearDefaultConfig(String userId) {
        LambdaUpdateWrapper<ApiConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiConfig::getUserId, userId);
        wrapper.set(ApiConfig::getIsDefault, false);
        wrapper.set(ApiConfig::getUpdateTime, LocalDateTime.now());
        this.update(wrapper);
        log.info("clearDefaultConfig: cleared default configs for userId={}", userId);
    }
}
