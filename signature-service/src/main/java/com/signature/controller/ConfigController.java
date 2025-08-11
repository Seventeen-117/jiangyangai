package com.signature.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.signature.entity.*;
import com.signature.service.*;
import com.signature.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
public class ConfigController {

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

    /**
     * 获取配置分类列表
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) Integer status) {
        
        Page<ConfigCategory> pageParam = new Page<>(page, size);
        QueryWrapper<ConfigCategory> queryWrapper = new QueryWrapper<>();
        
        if (categoryCode != null && !categoryCode.isEmpty()) {
            queryWrapper.like("category_code", categoryCode);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("sort_order", "id");
        
        Page<ConfigCategory> result = configCategoryService.page(pageParam, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取配置分类详情
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<ConfigCategory> getCategory(@PathVariable Long id) {
        ConfigCategory category = configCategoryService.getById(id);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(category);
    }

    /**
     * 创建配置分类
     */
    @PostMapping("/categories")
    public ResponseEntity<ConfigCategory> createCategory(@RequestBody ConfigCategory category) {
        configCategoryService.save(category);
        return ResponseEntity.ok(category);
    }

    /**
     * 更新配置分类
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<ConfigCategory> updateCategory(@PathVariable Long id, @RequestBody ConfigCategory category) {
        category.setId(id);
        configCategoryService.updateById(category);
        return ResponseEntity.ok(category);
    }

    /**
     * 删除配置分类
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        configCategoryService.removeById(id);
        return ResponseEntity.ok().build();
    }

    // ==================== 路径配置管理 ====================

    /**
     * 获取路径配置列表
     */
    @GetMapping("/paths")
    public ResponseEntity<Map<String, Object>> getPaths(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String pathType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        
        Page<PathConfig> pageParam = new Page<>(page, size);
        QueryWrapper<PathConfig> queryWrapper = new QueryWrapper<>();
        
        if (pathType != null && !pathType.isEmpty()) {
            queryWrapper.eq("path_type", pathType);
        }
        if (categoryId != null) {
            queryWrapper.eq("category_id", categoryId);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("sort_order", "id");
        
        Page<PathConfig> result = pathConfigService.page(pageParam, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取路径配置详情
     */
    @GetMapping("/paths/{id}")
    public ResponseEntity<PathConfig> getPath(@PathVariable Long id) {
        PathConfig path = pathConfigService.getById(id);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(path);
    }

    /**
     * 创建路径配置
     */
    @PostMapping("/paths")
    public ResponseEntity<PathConfig> createPath(@RequestBody PathConfig path) {
        pathConfigService.save(path);
        return ResponseEntity.ok(path);
    }

    /**
     * 更新路径配置
     */
    @PutMapping("/paths/{id}")
    public ResponseEntity<PathConfig> updatePath(@PathVariable Long id, @RequestBody PathConfig path) {
        path.setId(id);
        pathConfigService.updateById(path);
        return ResponseEntity.ok(path);
    }

    /**
     * 删除路径配置
     */
    @DeleteMapping("/paths/{id}")
    public ResponseEntity<Void> deletePath(@PathVariable Long id) {
        pathConfigService.removeById(id);
        return ResponseEntity.ok().build();
    }

    // ==================== 内部服务配置管理 ====================

    /**
     * 获取内部服务配置列表
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getServices(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) Integer status) {
        
        Page<InternalServiceConfig> pageParam = new Page<>(page, size);
        QueryWrapper<InternalServiceConfig> queryWrapper = new QueryWrapper<>();
        
        if (serviceType != null && !serviceType.isEmpty()) {
            queryWrapper.eq("service_type", serviceType);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("sort_order", "id");
        
        Page<InternalServiceConfig> result = internalServiceConfigService.page(pageParam, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取内部服务配置详情
     */
    @GetMapping("/services/{id}")
    public ResponseEntity<InternalServiceConfig> getService(@PathVariable Long id) {
        InternalServiceConfig service = internalServiceConfigService.getById(id);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service);
    }

    /**
     * 创建内部服务配置
     */
    @PostMapping("/services")
    public ResponseEntity<InternalServiceConfig> createService(@RequestBody InternalServiceConfig service) {
        internalServiceConfigService.save(service);
        return ResponseEntity.ok(service);
    }

    /**
     * 更新内部服务配置
     */
    @PutMapping("/services/{id}")
    public ResponseEntity<InternalServiceConfig> updateService(@PathVariable Long id, @RequestBody InternalServiceConfig service) {
        service.setId(id);
        internalServiceConfigService.updateById(service);
        return ResponseEntity.ok(service);
    }

    /**
     * 删除内部服务配置
     */
    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        internalServiceConfigService.removeById(id);
        return ResponseEntity.ok().build();
    }

    // ==================== 验证规则配置管理 ====================

    /**
     * 获取验证规则配置列表
     */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getRules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Integer status) {
        
        Page<ValidationRuleConfig> pageParam = new Page<>(page, size);
        QueryWrapper<ValidationRuleConfig> queryWrapper = new QueryWrapper<>();
        
        if (ruleType != null && !ruleType.isEmpty()) {
            queryWrapper.eq("rule_type", ruleType);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("sort_order", "id");
        
        Page<ValidationRuleConfig> result = validationRuleConfigService.page(pageParam, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取验证规则配置详情
     */
    @GetMapping("/rules/{id}")
    public ResponseEntity<ValidationRuleConfig> getRule(@PathVariable Long id) {
        ValidationRuleConfig rule = validationRuleConfigService.getById(id);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }

    /**
     * 创建验证规则配置
     */
    @PostMapping("/rules")
    public ResponseEntity<ValidationRuleConfig> createRule(@RequestBody ValidationRuleConfig rule) {
        validationRuleConfigService.save(rule);
        return ResponseEntity.ok(rule);
    }

    /**
     * 更新验证规则配置
     */
    @PutMapping("/rules/{id}")
    public ResponseEntity<ValidationRuleConfig> updateRule(@PathVariable Long id, @RequestBody ValidationRuleConfig rule) {
        rule.setId(id);
        validationRuleConfigService.updateById(rule);
        return ResponseEntity.ok(rule);
    }

    /**
     * 删除验证规则配置
     */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        validationRuleConfigService.removeById(id);
        return ResponseEntity.ok().build();
    }

    // ==================== 缓存配置管理 ====================

    /**
     * 获取缓存配置列表
     */
    @GetMapping("/caches")
    public ResponseEntity<Map<String, Object>> getCaches(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String cacheType,
            @RequestParam(required = false) Integer status) {
        
        Page<CacheConfig> pageParam = new Page<>(page, size);
        QueryWrapper<CacheConfig> queryWrapper = new QueryWrapper<>();
        
        if (cacheType != null && !cacheType.isEmpty()) {
            queryWrapper.eq("cache_type", cacheType);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByAsc("id");
        
        Page<CacheConfig> result = cacheConfigService.page(pageParam, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取缓存配置详情
     */
    @GetMapping("/caches/{id}")
    public ResponseEntity<CacheConfig> getCache(@PathVariable Long id) {
        CacheConfig cache = cacheConfigService.getById(id);
        if (cache == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cache);
    }

    /**
     * 创建缓存配置
     */
    @PostMapping("/caches")
    public ResponseEntity<CacheConfig> createCache(@RequestBody CacheConfig cache) {
        cacheConfigService.save(cache);
        return ResponseEntity.ok(cache);
    }

    /**
     * 更新缓存配置
     */
    @PutMapping("/caches/{id}")
    public ResponseEntity<CacheConfig> updateCache(@PathVariable Long id, @RequestBody CacheConfig cache) {
        cache.setId(id);
        cacheConfigService.updateById(cache);
        return ResponseEntity.ok(cache);
    }

    /**
     * 删除缓存配置
     */
    @DeleteMapping("/caches/{id}")
    public ResponseEntity<Void> deleteCache(@PathVariable Long id) {
        cacheConfigService.removeById(id);
        return ResponseEntity.ok().build();
    }

    // ==================== 动态配置查询 ====================

    /**
     * 获取排除路径列表
     */
    @GetMapping("/dynamic/excluded-paths")
    public ResponseEntity<List<String>> getExcludedPaths() {
        return ResponseEntity.ok(dynamicConfigService.getExcludedPaths());
    }

    /**
     * 获取严格验证路径列表
     */
    @GetMapping("/dynamic/strict-paths")
    public ResponseEntity<List<String>> getStrictPaths() {
        return ResponseEntity.ok(dynamicConfigService.getStrictValidationPaths());
    }

    /**
     * 获取内部路径列表
     */
    @GetMapping("/dynamic/internal-paths")
    public ResponseEntity<List<String>> getInternalPaths() {
        return ResponseEntity.ok(dynamicConfigService.getInternalPaths());
    }

    /**
     * 获取内部服务列表
     */
    @GetMapping("/dynamic/services")
    public ResponseEntity<List<InternalServiceConfig>> getInternalServices() {
        return ResponseEntity.ok(dynamicConfigService.getInternalServices());
    }

    /**
     * 根据服务编码获取内部服务
     */
    @GetMapping("/dynamic/services/code/{serviceCode}")
    public ResponseEntity<InternalServiceConfig> getInternalServiceByCode(@PathVariable String serviceCode) {
        InternalServiceConfig service = dynamicConfigService.getInternalServiceByCode(serviceCode);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service);
    }

    /**
     * 根据API密钥获取内部服务
     */
    @GetMapping("/dynamic/services/api-key/{apiKey}")
    public ResponseEntity<InternalServiceConfig> getInternalServiceByApiKey(@PathVariable String apiKey) {
        InternalServiceConfig service = dynamicConfigService.getInternalServiceByApiKey(apiKey);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service);
    }

    /**
     * 检查路径是否匹配指定类型
     */
    @GetMapping("/dynamic/check-path")
    public ResponseEntity<Map<String, Object>> checkPath(
            @RequestParam String path,
            @RequestParam String pathType) {
        
        boolean isMatch = dynamicConfigService.isPathMatchType(path, pathType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("path", path);
        response.put("pathType", pathType);
        response.put("isMatch", isMatch);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查是否为内部服务调用
     */
    @GetMapping("/dynamic/check-internal-service")
    public ResponseEntity<Map<String, Object>> checkInternalService(
            @RequestParam String apiKey,
            @RequestParam(required = false) String requestFrom) {
        
        boolean isInternal = dynamicConfigService.isInternalServiceCall(apiKey, requestFrom);
        
        Map<String, Object> response = new HashMap<>();
        response.put("apiKey", apiKey);
        response.put("requestFrom", requestFrom);
        response.put("isInternal", isInternal);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 刷新配置缓存
     */
    @PostMapping("/dynamic/refresh-cache")
    public ResponseEntity<Map<String, Object>> refreshCache() {
        dynamicConfigService.refreshConfigCache();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "配置缓存刷新成功");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取配置统计信息
     */
    @GetMapping("/dynamic/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(dynamicConfigService.getConfigStatistics());
    }
}
