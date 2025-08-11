package com.signature.service.impl;

import com.signature.entity.InternalServiceConfig;
import com.signature.entity.PathConfig;
import com.signature.entity.ValidationRuleConfig;
import com.signature.service.InternalServiceConfigService;
import com.signature.service.PathConfigService;
import com.signature.service.ValidationRuleConfigService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.signature.service.DynamicConfigService;
import com.signature.utils.PathMatcherUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import java.util.stream.Collectors;

/**
 * 动态配置服务实现类
 * 基于数据库的配置管理，替代硬编码逻辑
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicConfigServiceImpl implements DynamicConfigService {

    private final PathConfigService pathConfigService;
    private final ValidationRuleConfigService validationRuleConfigService;
    private final InternalServiceConfigService internalServiceConfigService;

    // ========== 路径配置相关 ==========

    @Override
    @Cacheable(value = "pathConfig", key = "'excludedPaths'")
    public List<String> getExcludedPaths() {
        log.debug("Loading excluded paths from database");
        List<PathConfig> pathConfigs = pathConfigService.findAllExcludedPaths();
        return pathConfigs.stream()
                .map(PathConfig::getPathPattern)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "pathConfig", key = "'excludedPaths:' + #httpMethod")
    public List<String> getExcludedPaths(String httpMethod) {
        log.debug("Loading excluded paths for HTTP method: {}", httpMethod);
        List<PathConfig> pathConfigs = pathConfigService.findByPathTypeAndHttpMethod(
                PathConfig.PATH_TYPE_EXCLUDED, httpMethod);
        return pathConfigs.stream()
                .map(PathConfig::getPathPattern)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "pathConfig", key = "'strictPaths'")
    public List<String> getStrictValidationPaths() {
        log.debug("Loading strict validation paths from database");
        List<PathConfig> pathConfigs = pathConfigService.findAllStrictPaths();
        return pathConfigs.stream()
                .map(PathConfig::getPathPattern)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "pathConfig", key = "'strictPaths:' + #httpMethod")
    public List<String> getStrictValidationPaths(String httpMethod) {
        log.debug("Loading strict validation paths for HTTP method: {}", httpMethod);
        List<PathConfig> pathConfigs = pathConfigService.findByPathTypeAndHttpMethod(
                PathConfig.PATH_TYPE_STRICT, httpMethod);
        return pathConfigs.stream()
                .map(PathConfig::getPathPattern)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "pathConfig", key = "'internalPaths'")
    public List<String> getInternalPaths() {
        log.debug("Loading internal paths from database");
        List<PathConfig> pathConfigs = pathConfigService.findAllInternalPaths();
        return pathConfigs.stream()
                .map(PathConfig::getPathPattern)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isExcludedPath(String path, String httpMethod) {
        try {
            List<String> excludedPaths = getExcludedPaths(httpMethod);
            boolean isExcluded = PathMatcherUtil.matchesAny(path, excludedPaths.toArray(new String[0]));
            log.debug("Path exclusion check: path={}, httpMethod={}, isExcluded={}", path, httpMethod, isExcluded);
            return isExcluded;
        } catch (Exception e) {
            log.error("Error checking excluded path: path={}, httpMethod={}", path, httpMethod, e);
            return false;
        }
    }

    @Override
    public boolean isStrictValidationPath(String path, String httpMethod) {
        try {
            List<String> strictPaths = getStrictValidationPaths(httpMethod);
            boolean isStrict = PathMatcherUtil.matchesAny(path, strictPaths.toArray(new String[0]));
            log.debug("Strict validation check: path={}, httpMethod={}, isStrict={}", path, httpMethod, isStrict);
            return isStrict;
        } catch (Exception e) {
            log.error("Error checking strict validation path: path={}, httpMethod={}", path, httpMethod, e);
            return false;
        }
    }

    @Override
    public boolean isInternalPath(String path, String httpMethod) {
        try {
            List<String> internalPaths = getInternalPaths();
            boolean isInternal = PathMatcherUtil.matchesAny(path, internalPaths.toArray(new String[0]));
            log.debug("Internal path check: path={}, httpMethod={}, isInternal={}", path, httpMethod, isInternal);
            return isInternal;
        } catch (Exception e) {
            log.error("Error checking internal path: path={}, httpMethod={}", path, httpMethod, e);
            return false;
        }
    }

    @Override
    @Cacheable(value = "pathConfig", key = "'apiKeyAuthExcluded:' + #httpMethod")
    public boolean isApiKeyAuthExcludedPath(String path, String httpMethod) {
        try {
            List<PathConfig> pathConfigs = pathConfigService.findByPathTypeAndHttpMethod(
                    PathConfig.PATH_TYPE_API_KEY_EXCLUDED, httpMethod);
            List<String> excludedPaths = pathConfigs.stream()
                    .map(PathConfig::getPathPattern)
                    .collect(Collectors.toList());
            boolean isExcluded = PathMatcherUtil.matchesAny(path, excludedPaths.toArray(new String[0]));
            log.debug("API Key auth exclusion check: path={}, httpMethod={}, isExcluded={}", path, httpMethod, isExcluded);
            return isExcluded;
        } catch (Exception e) {
            log.error("Error checking API Key auth excluded path: path={}, httpMethod={}", path, httpMethod, e);
            return false;
        }
    }

    @Override
    @Cacheable(value = "pathConfig", key = "'jwtExcluded:' + #httpMethod")
    public boolean isJwtExcludedPath(String path, String httpMethod) {
        try {
            List<PathConfig> pathConfigs = pathConfigService.findByPathTypeAndHttpMethod(
                    PathConfig.PATH_TYPE_JWT_EXCLUDED, httpMethod);
            List<String> excludedPaths = pathConfigs.stream()
                    .map(PathConfig::getPathPattern)
                    .collect(Collectors.toList());
            boolean isExcluded = PathMatcherUtil.matchesAny(path, excludedPaths.toArray(new String[0]));
            log.debug("JWT exclusion check: path={}, httpMethod={}, isExcluded={}", path, httpMethod, isExcluded);
            return isExcluded;
        } catch (Exception e) {
            log.error("Error checking JWT excluded path: path={}, httpMethod={}", path, httpMethod, e);
            return false;
        }
    }

    @Override
    @Cacheable(value = "pathConfig", key = "'permissionExcluded:' + #httpMethod")
    public boolean isPermissionExcludedPath(String path, String httpMethod) {
        try {
            List<PathConfig> pathConfigs = pathConfigService.findByPathTypeAndHttpMethod(
                    PathConfig.PATH_TYPE_PERMISSION_EXCLUDED, httpMethod);
            List<String> excludedPaths = pathConfigs.stream()
                    .map(PathConfig::getPathPattern)
                    .collect(Collectors.toList());
            boolean isExcluded = PathMatcherUtil.matchesAny(path, excludedPaths.toArray(new String[0]));
            log.debug("Permission exclusion check: path={}, httpMethod={}, isExcluded={}", path, httpMethod, isExcluded);
            return isExcluded;
        } catch (Exception e) {
            log.error("Error checking permission excluded path: path={}, httpMethod={}", path, httpMethod, e);
            return false;
        }
    }

    // ========== 验证规则配置相关 ==========

    @Override
    @Cacheable(value = "validationRule", key = "#ruleCode")
    public ValidationRuleConfig getValidationRule(String ruleCode) {
        log.debug("Loading validation rule: {}", ruleCode);
        return validationRuleConfigService.findByRuleCodeAndEnabled(ruleCode);
    }

    @Override
    public boolean isValidationRuleEnabled(String ruleCode) {
        ValidationRuleConfig rule = getValidationRule(ruleCode);
        boolean enabled = rule != null && rule.isEnabled();
        log.debug("Validation rule enabled check: ruleCode={}, enabled={}", ruleCode, enabled);
        return enabled;
    }

    @Override
    public boolean isApiKeyValidationEnabled() {
        return isValidationRuleEnabled(ValidationRuleConfig.RULE_CODE_API_KEY_VALIDATION);
    }

    @Override
    public boolean isSignatureValidationEnabled() {
        return isValidationRuleEnabled(ValidationRuleConfig.RULE_CODE_SIGNATURE_VALIDATION);
    }

    @Override
    public boolean isAuthenticationValidationEnabled() {
        return isValidationRuleEnabled(ValidationRuleConfig.RULE_CODE_AUTHENTICATION_VALIDATION);
    }

    @Override
    public boolean isJwtValidationEnabled() {
        return isValidationRuleEnabled(ValidationRuleConfig.RULE_CODE_JWT_VALIDATION);
    }

    @Override
    public boolean isPermissionValidationEnabled() {
        return isValidationRuleEnabled(ValidationRuleConfig.RULE_CODE_PERMISSION_VALIDATION);
    }

    @Override
    public boolean isApiKeyAuthValidationEnabled() {
        return isValidationRuleEnabled(ValidationRuleConfig.RULE_CODE_API_KEY_AUTH_VALIDATION);
    }

    @Override
    @Cacheable(value = "validationRule", key = "'config:' + #ruleCode")
    public String getValidationRuleConfig(String ruleCode) {
        log.debug("Loading validation rule config: {}", ruleCode);
        ValidationRuleConfig rule = validationRuleConfigService.findByRuleCodeAndEnabled(ruleCode);
        return rule != null ? rule.getConfigJson() : null;
    }

    // ========== 内部服务配置相关 ==========

    @Override
    @Cacheable(value = "internalService", key = "'allServices'")
    public List<InternalServiceConfig> getAllInternalServices() {
        log.debug("Loading all internal services from database");
        return internalServiceConfigService.findAllEnabled();
    }

    @Override
    @Cacheable(value = "internalService", key = "'service:' + #serviceCode")
    public InternalServiceConfig getInternalService(String serviceCode) {
        log.debug("Loading internal service: {}", serviceCode);
        return internalServiceConfigService.findEnabledByServiceCode(serviceCode);
    }

    @Override
    @Cacheable(value = "internalService", key = "'apiKey:' + #apiKey")
    public InternalServiceConfig getInternalServiceByApiKey(String apiKey) {
        log.debug("Loading internal service by API key: {}", apiKey);
        return internalServiceConfigService.findEnabledByApiKey(apiKey);
    }

    @Override
    public boolean isInternalService(String serviceCode) {
        InternalServiceConfig service = getInternalService(serviceCode);
        boolean isInternal = service != null;
        log.debug("Internal service check: serviceCode={}, isInternal={}", serviceCode, isInternal);
        return isInternal;
    }

    @Override
    public boolean isInternalServiceApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        InternalServiceConfig service = getInternalServiceByApiKey(apiKey);
        boolean isInternal = service != null;
        log.debug("Internal service API key check: apiKey={}, isInternal={}", apiKey, isInternal);
        return isInternal;
    }

    @Override
    public InternalServiceConfig getInternalServiceByPrefix(String requestPath) {
        if (requestPath == null || requestPath.trim().isEmpty()) {
            return null;
        }
        
        List<InternalServiceConfig> services = getAllInternalServices();
        for (InternalServiceConfig service : services) {
            if (service.getRequestPrefix() != null && 
                requestPath.startsWith(service.getRequestPrefix())) {
                log.debug("Found internal service by prefix: path={}, service={}", requestPath, service.getServiceCode());
                return service;
            }
        }
        
        log.debug("No internal service found for path: {}", requestPath);
        return null;
    }

    // ========== 缓存管理相关 ==========

    @Override
    @CacheEvict(value = {"pathConfig", "validationRule", "internalService"}, allEntries = true)
    public void refreshConfigCache() {
        log.info("Refreshing all configuration caches");
    }

    @Override
    @CacheEvict(value = "pathConfig", allEntries = true)
    public void refreshPathConfigCache() {
        log.info("Refreshing path configuration cache");
    }

    @Override
    @CacheEvict(value = "validationRule", allEntries = true)
    public void refreshValidationRuleCache() {
        log.info("Refreshing validation rule cache");
    }

    @Override
    @CacheEvict(value = "internalService", allEntries = true)
    public void refreshInternalServiceCache() {
        log.info("Refreshing internal service cache");
    }

    @Override
    @CacheEvict(value = {"pathConfig", "validationRule", "internalService"}, allEntries = true)
    public void clearConfigCache() {
        log.info("Clearing all configuration caches");
    }

    @Override
    @Cacheable(value = "pathConfig", key = "'pathMatch:' + #pathType + ':' + #path")
    public boolean isPathMatchType(String path, String pathType) {
        try {
            List<PathConfig> pathConfigs = pathConfigService.findByPathType(pathType);
            List<String> paths = pathConfigs.stream()
                    .filter(config -> config.getStatus() != null && config.getStatus() == 1)
                    .map(PathConfig::getPathPattern)
                    .collect(Collectors.toList());
            boolean isMatch = PathMatcherUtil.matchesAny(path, paths.toArray(new String[0]));
            log.debug("Path match type check: path={}, pathType={}, isMatch={}", path, pathType, isMatch);
            return isMatch;
        } catch (Exception e) {
            log.error("Error checking path match type: path={}, pathType={}", path, pathType, e);
            return false;
        }
    }

    @Override
    @Cacheable(value = "internalService")
    public List<InternalServiceConfig> getInternalServices() {
        try {
            List<InternalServiceConfig> services = internalServiceConfigService.findAllEnabled();
            log.debug("Loaded {} internal services from database", services.size());
            return services;
        } catch (Exception e) {
            log.error("Error loading internal services from database", e);
            return new java.util.ArrayList<>();
        }
    }

    @Override
    @Cacheable(value = "internalService", key = "'serviceCode:' + #serviceCode")
    public InternalServiceConfig getInternalServiceByCode(String serviceCode) {
        try {
            InternalServiceConfig service = internalServiceConfigService.findEnabledByServiceCode(serviceCode);
            log.debug("Found internal service by code: serviceCode={}, found={}", serviceCode, service != null);
            return service;
        } catch (Exception e) {
            log.error("Error finding internal service by code: serviceCode={}", serviceCode, e);
            return null;
        }
    }

    @Override
    public boolean isInternalServiceCall(String serviceCode, String apiKey) {
        try {
            // 首先通过服务编码检查
            if (serviceCode != null && !serviceCode.trim().isEmpty()) {
                InternalServiceConfig service = getInternalServiceByCode(serviceCode);
                if (service != null) {
                    log.debug("Internal service call verified by service code: {}", serviceCode);
                    return true;
                }
            }
            
            // 然后通过API Key检查
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                InternalServiceConfig service = internalServiceConfigService.findEnabledByApiKey(apiKey);
                if (service != null) {
                    log.debug("Internal service call verified by API key");
                    return true;
                }
            }
            
            log.debug("Internal service call verification failed: serviceCode={}, apiKey present={}", 
                     serviceCode, apiKey != null && !apiKey.trim().isEmpty());
            return false;
        } catch (Exception e) {
            log.error("Error checking internal service call: serviceCode={}", serviceCode, e);
            return false;
        }
    }

    @Override
    @Cacheable(value = "configStats", key = "'statistics'")
    public java.util.Map<String, Object> getConfigStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        try {
            // 路径配置统计
            long totalPaths = pathConfigService.count();
            long enabledPaths = pathConfigService.count(
                new QueryWrapper<PathConfig>().eq("status", 1)
            );
            stats.put("totalPaths", totalPaths);
            stats.put("enabledPaths", enabledPaths);
            
            // 验证规则统计
            long totalRules = validationRuleConfigService.count();
            long enabledRules = validationRuleConfigService.count(
                new QueryWrapper<ValidationRuleConfig>()
                    .eq("status", 1)
                    .eq("enabled", 1)
            );
            stats.put("totalRules", totalRules);
            stats.put("enabledRules", enabledRules);
            
            // 内部服务统计
            long totalServices = internalServiceConfigService.count();
            long enabledServices = internalServiceConfigService.count(
                new QueryWrapper<InternalServiceConfig>().eq("status", 1)
            );
            stats.put("totalServices", totalServices);
            stats.put("enabledServices", enabledServices);
            
            stats.put("lastUpdated", java.time.LocalDateTime.now());
            log.debug("Generated config statistics: {}", stats);
            
        } catch (Exception e) {
            log.error("Error generating config statistics", e);
            stats.put("error", "Failed to generate statistics");
        }
        return stats;
    }
}