package com.signature.service;

import com.signature.entity.InternalServiceConfig;

import com.signature.entity.ValidationRuleConfig;

import java.util.List;

/**
 * 动态配置服务接口
 * 提供基于数据库的配置管理，替代硬编码逻辑
 * 
 * @author signature-service
 * @since 2025-01-01
 */
public interface DynamicConfigService {

    // ========== 路径配置相关 ==========

    /**
     * 获取所有排除路径
     */
    List<String> getExcludedPaths();

    /**
     * 获取指定HTTP方法的排除路径
     */
    List<String> getExcludedPaths(String httpMethod);

    /**
     * 获取所有严格验证路径
     */
    List<String> getStrictValidationPaths();

    /**
     * 获取指定HTTP方法的严格验证路径
     */
    List<String> getStrictValidationPaths(String httpMethod);

    /**
     * 获取所有内部路径
     */
    List<String> getInternalPaths();

    /**
     * 检查路径是否为排除路径
     */
    boolean isExcludedPath(String path, String httpMethod);

    /**
     * 检查路径是否需要严格验证
     */
    boolean isStrictValidationPath(String path, String httpMethod);

    /**
     * 检查路径是否为内部路径
     */
    boolean isInternalPath(String path, String httpMethod);

    /**
     * 检查路径是否需要跳过API Key认证验证
     */
    boolean isApiKeyAuthExcludedPath(String path, String httpMethod);

    /**
     * 检查路径是否需要跳过JWT验证
     */
    boolean isJwtExcludedPath(String path, String httpMethod);

    /**
     * 检查路径是否需要跳过权限验证
     */
    boolean isPermissionExcludedPath(String path, String httpMethod);

    /**
     * 检查路径是否匹配指定类型（通用方法）
     */
    boolean isPathMatchType(String path, String pathType);

    /**
     * 获取内部服务配置列表
     */
    List<com.signature.entity.InternalServiceConfig> getInternalServices();

    /**
     * 根据服务编码获取内部服务配置
     */
    com.signature.entity.InternalServiceConfig getInternalServiceByCode(String serviceCode);

    /**
     * 检查是否为内部服务调用
     */
    boolean isInternalServiceCall(String serviceCode, String apiKey);

    /**
     * 获取配置统计信息
     */
    java.util.Map<String, Object> getConfigStatistics();

    // ========== 验证规则配置相关 ==========

    /**
     * 获取验证规则配置
     */
    ValidationRuleConfig getValidationRule(String ruleCode);

    /**
     * 检查验证规则是否启用
     */
    boolean isValidationRuleEnabled(String ruleCode);

    /**
     * 检查API密钥验证是否启用
     */
    boolean isApiKeyValidationEnabled();

    /**
     * 检查签名验证是否启用
     */
    boolean isSignatureValidationEnabled();

    /**
     * 检查认证验证是否启用
     */
    boolean isAuthenticationValidationEnabled();

    /**
     * 检查JWT验证是否启用
     */
    boolean isJwtValidationEnabled();

    /**
     * 检查权限验证是否启用
     */
    boolean isPermissionValidationEnabled();

    /**
     * 检查API Key认证验证是否启用
     */
    boolean isApiKeyAuthValidationEnabled();

    /**
     * 获取验证规则的JSON配置
     */
    String getValidationRuleConfig(String ruleCode);

    // ========== 内部服务配置相关 ==========

    /**
     * 获取所有内部服务配置
     */
    List<InternalServiceConfig> getAllInternalServices();

    /**
     * 根据服务编码获取内部服务配置
     */
    InternalServiceConfig getInternalService(String serviceCode);

    /**
     * 根据API密钥获取内部服务配置
     */
    InternalServiceConfig getInternalServiceByApiKey(String apiKey);

    /**
     * 检查是否为内部服务
     */
    boolean isInternalService(String serviceCode);

    /**
     * 检查API密钥是否属于内部服务
     */
    boolean isInternalServiceApiKey(String apiKey);

    /**
     * 根据请求前缀识别内部服务
     */
    InternalServiceConfig getInternalServiceByPrefix(String requestPath);

    // ========== 缓存管理相关 ==========

    /**
     * 刷新配置缓存
     */
    void refreshConfigCache();

    /**
     * 刷新路径配置缓存
     */
    void refreshPathConfigCache();

    /**
     * 刷新验证规则缓存
     */
    void refreshValidationRuleCache();

    /**
     * 刷新内部服务配置缓存
     */
    void refreshInternalServiceCache();

    /**
     * 清除所有配置缓存
     */
    void clearConfigCache();
}