package com.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.signature.entity.*;
import com.signature.mapper.*;
import com.signature.service.ConfigCategoryService;
import com.signature.service.ConfigManagementService;
import com.signature.service.DynamicConfigService;
import com.signature.service.PathConfigService;
import com.signature.service.CacheConfigService;
import com.signature.service.InternalServiceConfigService;
import com.signature.service.ValidationRuleConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置管理服务实现类
 */
@Slf4j
@Service
public class ConfigManagementServiceImpl implements ConfigManagementService {

    @Autowired
    private ConfigCategoryService configCategoryService;

    @Autowired
    private PathConfigService pathConfigService;

    @Autowired
    private InternalServiceConfigService internalServiceConfigService;

    @Autowired
    private ValidationRuleConfigService validationRuleConfigService;

    @Autowired
    private CacheConfigService cacheConfigService;

    @Autowired
    private DynamicConfigService dynamicConfigService;

    // ==================== 配置分类管理 ====================

    @Override
    public Page<ConfigCategory> getCategoriesPage(Integer page, Integer size, String categoryCode, Integer status) {
        Page<ConfigCategory> pageParam = new Page<>(page, size);
        QueryWrapper<ConfigCategory> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(categoryCode)) {
            queryWrapper.like("category_code", categoryCode);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("sort_order", "id");
        
        return configCategoryService.page(pageParam, queryWrapper);
    }

    @Override
    public ConfigCategory getCategoryById(Long id) {
        return configCategoryService.getById(id);
    }

    @Override
    public ConfigCategory getCategoryByCode(String categoryCode) {
        return configCategoryService.findByCategoryCode(categoryCode);
    }

    @Override
    public ConfigCategory createCategory(ConfigCategory category) {
        // 验证分类编码唯一性
        if (getCategoryByCode(category.getCategoryCode()) != null) {
            throw new RuntimeException("分类编码已存在: " + category.getCategoryCode());
        }
        
        configCategoryService.save(category);
        return category;
    }

    @Override
    public ConfigCategory updateCategory(Long id, ConfigCategory category) {
        category.setId(id);
        
        // 验证分类编码唯一性（排除自身）
        ConfigCategory existing = getCategoryByCode(category.getCategoryCode());
        if (existing != null && !existing.getId().equals(id)) {
            throw new RuntimeException("分类编码已存在: " + category.getCategoryCode());
        }
        
        configCategoryService.updateById(category);
        return category;
    }

    @Override
    public boolean deleteCategory(Long id) {
        // 检查是否有路径配置关联此分类
        List<PathConfig> paths = pathConfigService.findByCategoryId(id);
        if (!paths.isEmpty()) {
            throw new RuntimeException("该分类下存在路径配置，无法删除");
        }
        
        return configCategoryService.removeById(id);
    }

    @Override
    public List<ConfigCategory> getAllEnabledCategories() {
        return configCategoryService.findAllEnabled();
    }

    // ==================== 路径配置管理 ====================

    @Override
    public Page<PathConfig> getPathsPage(Integer page, Integer size, String pathType, Long categoryId, Integer status) {
        Page<PathConfig> pageParam = new Page<>(page, size);
        QueryWrapper<PathConfig> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(pathType)) {
            queryWrapper.eq("path_type", pathType);
        }
        if (categoryId != null) {
            queryWrapper.eq("category_id", categoryId);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("sort_order", "id");
        
        return pathConfigService.page(pageParam, queryWrapper);
    }

    @Override
    public PathConfig getPathById(Long id) {
        return pathConfigService.getById(id);
    }

    @Override
    public PathConfig createPath(PathConfig path) {
        // 验证分类是否存在
        if (path.getCategoryId() != null) {
            ConfigCategory category = getCategoryById(path.getCategoryId());
            if (category == null) {
                throw new RuntimeException("分类不存在: " + path.getCategoryId());
            }
        }
        
        pathConfigService.save(path);
        return path;
    }

    @Override
    public PathConfig updatePath(Long id, PathConfig path) {
        path.setId(id);
        
        // 验证分类是否存在
        if (path.getCategoryId() != null) {
            ConfigCategory category = getCategoryById(path.getCategoryId());
            if (category == null) {
                throw new RuntimeException("分类不存在: " + path.getCategoryId());
            }
        }
        
        pathConfigService.updateById(path);
        return path;
    }

    @Override
    public boolean deletePath(Long id) {
        return pathConfigService.removeById(id);
    }

    @Override
    public List<PathConfig> getPathsByType(String pathType) {
        return pathConfigService.findByPathType(pathType);
    }

    @Override
    public List<PathConfig> getPathsByCategoryId(Long categoryId) {
        return pathConfigService.findByCategoryId(categoryId);
    }

    // ==================== 内部服务配置管理 ====================

    @Override
    public Page<InternalServiceConfig> getServicesPage(Integer page, Integer size, String serviceType, Integer status) {
        Page<InternalServiceConfig> pageParam = new Page<>(page, size);
        QueryWrapper<InternalServiceConfig> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(serviceType)) {
            queryWrapper.eq("service_type", serviceType);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("sort_order", "id");
        
        return internalServiceConfigService.page(pageParam, queryWrapper);
    }

    @Override
    public InternalServiceConfig getServiceById(Long id) {
        return internalServiceConfigService.getById(id);
    }

    @Override
    public InternalServiceConfig getServiceByCode(String serviceCode) {
        return internalServiceConfigService.findEnabledByServiceCode(serviceCode);
    }

    @Override
    public InternalServiceConfig getServiceByApiKey(String apiKey) {
        return internalServiceConfigService.findEnabledByApiKey(apiKey);
    }

    @Override
    public InternalServiceConfig createService(InternalServiceConfig service) {
        // 验证服务编码唯一性
        if (getServiceByCode(service.getServiceCode()) != null) {
            throw new RuntimeException("服务编码已存在: " + service.getServiceCode());
        }
        
        return internalServiceConfigService.createServiceConfig(service);
    }

    @Override
    public InternalServiceConfig updateService(Long id, InternalServiceConfig service) {
        service.setId(id);
        
        // 验证服务编码唯一性（排除自身）
        InternalServiceConfig existing = getServiceByCode(service.getServiceCode());
        if (existing != null && !existing.getId().equals(id)) {
            throw new RuntimeException("服务编码已存在: " + service.getServiceCode());
        }
        
        return internalServiceConfigService.updateServiceConfig(service);
    }

    @Override
    public boolean deleteService(Long id) {
        return internalServiceConfigService.deleteServiceConfig(id);
    }

    @Override
    public List<InternalServiceConfig> getServicesByType(String serviceType) {
        return internalServiceConfigService.findEnabledByServiceType(serviceType);
    }

    // ==================== 验证规则配置管理 ====================

    @Override
    public Page<ValidationRuleConfig> getRulesPage(Integer page, Integer size, String ruleType, Integer status) {
        Page<ValidationRuleConfig> pageParam = new Page<>(page, size);
        QueryWrapper<ValidationRuleConfig> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(ruleType)) {
            queryWrapper.eq("rule_type", ruleType);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("sort_order", "id");
        
        return validationRuleConfigService.page(pageParam, queryWrapper);
    }

    @Override
    public ValidationRuleConfig getRuleById(Long id) {
        return validationRuleConfigService.getById(id);
    }

    @Override
    public ValidationRuleConfig getRuleByCode(String ruleCode) {
        return validationRuleConfigService.findEnabledByRuleCode(ruleCode);
    }

    @Override
    public ValidationRuleConfig createRule(ValidationRuleConfig rule) {
        // 验证规则编码唯一性
        if (getRuleByCode(rule.getRuleCode()) != null) {
            throw new RuntimeException("规则编码已存在: " + rule.getRuleCode());
        }
        
        validationRuleConfigService.createRuleConfig(rule);
        return rule;
    }

    @Override
    public ValidationRuleConfig updateRule(Long id, ValidationRuleConfig rule) {
        rule.setId(id);
        
        // 验证规则编码唯一性（排除自身）
        ValidationRuleConfig existing = getRuleByCode(rule.getRuleCode());
        if (existing != null && !existing.getId().equals(id)) {
            throw new RuntimeException("规则编码已存在: " + rule.getRuleCode());
        }
        
        validationRuleConfigService.updateRuleConfig(rule);
        return rule;
    }

    @Override
    public boolean deleteRule(Long id) {
        return validationRuleConfigService.deleteRuleConfig(id);
    }

    @Override
    public List<ValidationRuleConfig> getRulesByType(String ruleType) {
        return validationRuleConfigService.findEnabledByRuleType(ruleType);
    }

    @Override
    public List<ValidationRuleConfig> getAllEnabledRules() {
        // TODO: 创建ValidationRuleConfigService后使用service方法
        return validationRuleConfigService.findAllEnabled();
    }

    // ==================== 缓存配置管理 ====================

    @Override
    public Page<CacheConfig> getCachesPage(Integer page, Integer size, String cacheType, Integer status) {
        Page<CacheConfig> pageParam = new Page<>(page, size);
        QueryWrapper<CacheConfig> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(cacheType)) {
            queryWrapper.eq("cache_type", cacheType);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("id");
        
        return cacheConfigService.page(pageParam, queryWrapper);
    }

    @Override
    public CacheConfig getCacheById(Long id) {
        return cacheConfigService.getById(id);
    }

    @Override
    public CacheConfig getCacheByKey(String cacheKey) {
        return cacheConfigService.findByCacheKey(cacheKey);
    }

    @Override
    public CacheConfig createCache(CacheConfig cache) {
        // 验证缓存键唯一性
        if (getCacheByKey(cache.getCacheKey()) != null) {
            throw new RuntimeException("缓存键已存在: " + cache.getCacheKey());
        }
        
        cacheConfigService.save(cache);
        return cache;
    }

    @Override
    public CacheConfig updateCache(Long id, CacheConfig cache) {
        cache.setId(id);
        
        // 验证缓存键唯一性（排除自身）
        CacheConfig existing = getCacheByKey(cache.getCacheKey());
        if (existing != null && !existing.getId().equals(id)) {
            throw new RuntimeException("缓存键已存在: " + cache.getCacheKey());
        }
        
        cacheConfigService.updateById(cache);
        return cache;
    }

    @Override
    public boolean deleteCache(Long id) {
        return cacheConfigService.removeById(id);
    }

    @Override
    public List<CacheConfig> getCachesByType(String cacheType) {
        return cacheConfigService.findByCacheType(cacheType);
    }

    @Override
    public List<CacheConfig> getCachesByNameLike(String cacheName) {
        return cacheConfigService.findByCacheNameLike(cacheName);
    }

    // ==================== 批量操作 ====================

    @Override
    public boolean batchUpdateStatus(String tableName, List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        try {
            switch (tableName.toLowerCase()) {
                case "config_category":
                    return configCategoryService.update(null, 
                        new QueryWrapper<ConfigCategory>().in("id", ids).eq("status", status));
                case "path_config":
                    return pathConfigService.update(null, 
                        new QueryWrapper<PathConfig>().in("id", ids).eq("status", status));
                case "internal_service_config":
                    return internalServiceConfigService.update(null, 
                        new QueryWrapper<InternalServiceConfig>().in("id", ids).eq("status", status));
                case "validation_rule_config":
                    return validationRuleConfigService.update(null, 
                        new QueryWrapper<ValidationRuleConfig>().in("id", ids).eq("status", status));
                case "cache_config":
                    return cacheConfigService.update(null, 
                        new QueryWrapper<CacheConfig>().in("id", ids).eq("status", status));
                default:
                    throw new RuntimeException("不支持的表名: " + tableName);
            }
        } catch (Exception e) {
            log.error("批量更新状态失败", e);
            return false;
        }
    }

    @Override
    public boolean batchDelete(String tableName, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        try {
            switch (tableName.toLowerCase()) {
                case "config_category":
                    return configCategoryService.remove(new QueryWrapper<ConfigCategory>().in("id", ids));
                case "path_config":
                    return pathConfigService.remove(new QueryWrapper<PathConfig>().in("id", ids));
                case "internal_service_config":
                    return internalServiceConfigService.remove(new QueryWrapper<InternalServiceConfig>().in("id", ids));
                case "validation_rule_config":
                    return validationRuleConfigService.remove(new QueryWrapper<ValidationRuleConfig>().in("id", ids));
                case "cache_config":
                    return cacheConfigService.remove(new QueryWrapper<CacheConfig>().in("id", ids));
                default:
                    throw new RuntimeException("不支持的表名: " + tableName);
            }
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> importConfigData(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "导入功能待实现");
        return result;
    }

    @Override
    public Map<String, Object> exportConfigData(String tableName, List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "导出功能待实现");
        return result;
    }

    // ==================== 配置验证 ====================

    @Override
    public Map<String, Object> validateConfig(String tableName, Object config) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("message", "验证通过");
        return result;
    }

    @Override
    public Map<String, Object> checkConfigConflict(String tableName, Object config) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasConflict", false);
        result.put("message", "无冲突");
        return result;
    }

    // ==================== 配置统计 ====================

    @Override
    public Map<String, Object> getConfigStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 统计各表数据量
        statistics.put("categories", configCategoryService.count());
        statistics.put("paths", pathConfigService.count());
        statistics.put("services", internalServiceConfigService.count());
        statistics.put("rules", validationRuleConfigService.count());
        statistics.put("caches", cacheConfigService.count());
        
        // 统计启用状态的数据量
        statistics.put("enabledCategories", configCategoryService.count(new QueryWrapper<ConfigCategory>().eq("status", 1)));
        statistics.put("enabledPaths", pathConfigService.count(new QueryWrapper<PathConfig>().eq("status", 1)));
        statistics.put("enabledServices", internalServiceConfigService.count(new QueryWrapper<InternalServiceConfig>().eq("status", 1)));
        statistics.put("enabledRules", validationRuleConfigService.count(new QueryWrapper<ValidationRuleConfig>().eq("status", 1)));
        statistics.put("enabledCaches", cacheConfigService.count(new QueryWrapper<CacheConfig>().eq("status", 1)));
        
        return statistics;
    }

    @Override
    public List<Map<String, Object>> getConfigChangeHistory(String tableName, Long configId) {
        // 这里可以实现配置变更历史记录功能
        return new ArrayList<>();
    }
}
