package com.signature.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.signature.entity.*;

import java.util.List;
import java.util.Map;

/**
 * 配置管理服务接口
 */
public interface ConfigManagementService {

    // ==================== 配置分类管理 ====================

    /**
     * 分页查询配置分类
     */
    Page<ConfigCategory> getCategoriesPage(Integer page, Integer size, String categoryCode, Integer status);

    /**
     * 获取配置分类详情
     */
    ConfigCategory getCategoryById(Long id);

    /**
     * 根据分类编码获取配置分类
     */
    ConfigCategory getCategoryByCode(String categoryCode);

    /**
     * 创建配置分类
     */
    ConfigCategory createCategory(ConfigCategory category);

    /**
     * 更新配置分类
     */
    ConfigCategory updateCategory(Long id, ConfigCategory category);

    /**
     * 删除配置分类
     */
    boolean deleteCategory(Long id);

    /**
     * 获取所有启用的配置分类
     */
    List<ConfigCategory> getAllEnabledCategories();

    // ==================== 路径配置管理 ====================

    /**
     * 分页查询路径配置
     */
    Page<PathConfig> getPathsPage(Integer page, Integer size, String pathType, Long categoryId, Integer status);

    /**
     * 获取路径配置详情
     */
    PathConfig getPathById(Long id);

    /**
     * 创建路径配置
     */
    PathConfig createPath(PathConfig path);

    /**
     * 更新路径配置
     */
    PathConfig updatePath(Long id, PathConfig path);

    /**
     * 删除路径配置
     */
    boolean deletePath(Long id);

    /**
     * 根据路径类型获取路径配置
     */
    List<PathConfig> getPathsByType(String pathType);

    /**
     * 根据分类ID获取路径配置
     */
    List<PathConfig> getPathsByCategoryId(Long categoryId);

    // ==================== 内部服务配置管理 ====================

    /**
     * 分页查询内部服务配置
     */
    Page<InternalServiceConfig> getServicesPage(Integer page, Integer size, String serviceType, Integer status);

    /**
     * 获取内部服务配置详情
     */
    InternalServiceConfig getServiceById(Long id);

    /**
     * 根据服务编码获取内部服务配置
     */
    InternalServiceConfig getServiceByCode(String serviceCode);

    /**
     * 根据API密钥获取内部服务配置
     */
    InternalServiceConfig getServiceByApiKey(String apiKey);

    /**
     * 创建内部服务配置
     */
    InternalServiceConfig createService(InternalServiceConfig service);

    /**
     * 更新内部服务配置
     */
    InternalServiceConfig updateService(Long id, InternalServiceConfig service);

    /**
     * 删除内部服务配置
     */
    boolean deleteService(Long id);

    /**
     * 根据服务类型获取内部服务配置
     */
    List<InternalServiceConfig> getServicesByType(String serviceType);

    // ==================== 验证规则配置管理 ====================

    /**
     * 分页查询验证规则配置
     */
    Page<ValidationRuleConfig> getRulesPage(Integer page, Integer size, String ruleType, Integer status);

    /**
     * 获取验证规则配置详情
     */
    ValidationRuleConfig getRuleById(Long id);

    /**
     * 根据规则编码获取验证规则配置
     */
    ValidationRuleConfig getRuleByCode(String ruleCode);

    /**
     * 创建验证规则配置
     */
    ValidationRuleConfig createRule(ValidationRuleConfig rule);

    /**
     * 更新验证规则配置
     */
    ValidationRuleConfig updateRule(Long id, ValidationRuleConfig rule);

    /**
     * 删除验证规则配置
     */
    boolean deleteRule(Long id);

    /**
     * 根据规则类型获取验证规则配置
     */
    List<ValidationRuleConfig> getRulesByType(String ruleType);

    /**
     * 获取所有启用的验证规则配置
     */
    List<ValidationRuleConfig> getAllEnabledRules();

    // ==================== 缓存配置管理 ====================

    /**
     * 分页查询缓存配置
     */
    Page<CacheConfig> getCachesPage(Integer page, Integer size, String cacheType, Integer status);

    /**
     * 获取缓存配置详情
     */
    CacheConfig getCacheById(Long id);

    /**
     * 根据缓存键获取缓存配置
     */
    CacheConfig getCacheByKey(String cacheKey);

    /**
     * 创建缓存配置
     */
    CacheConfig createCache(CacheConfig cache);

    /**
     * 更新缓存配置
     */
    CacheConfig updateCache(Long id, CacheConfig cache);

    /**
     * 删除缓存配置
     */
    boolean deleteCache(Long id);

    /**
     * 根据缓存类型获取缓存配置
     */
    List<CacheConfig> getCachesByType(String cacheType);

    /**
     * 根据缓存名称模糊查询
     */
    List<CacheConfig> getCachesByNameLike(String cacheName);

    // ==================== 批量操作 ====================

    /**
     * 批量更新配置状态
     */
    boolean batchUpdateStatus(String tableName, List<Long> ids, Integer status);

    /**
     * 批量删除配置
     */
    boolean batchDelete(String tableName, List<Long> ids);

    /**
     * 导入配置数据
     */
    Map<String, Object> importConfigData(Map<String, Object> data);

    /**
     * 导出配置数据
     */
    Map<String, Object> exportConfigData(String tableName, List<Long> ids);

    // ==================== 配置验证 ====================

    /**
     * 验证配置数据
     */
    Map<String, Object> validateConfig(String tableName, Object config);

    /**
     * 检查配置冲突
     */
    Map<String, Object> checkConfigConflict(String tableName, Object config);

    // ==================== 配置统计 ====================

    /**
     * 获取配置统计信息
     */
    Map<String, Object> getConfigStatistics();

    /**
     * 获取配置变更历史
     */
    List<Map<String, Object>> getConfigChangeHistory(String tableName, Long configId);
}
