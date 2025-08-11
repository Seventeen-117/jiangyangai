package com.signature.controller;

import com.signature.entity.PathConfig;
import com.signature.entity.ValidationRuleConfig;
import com.signature.entity.InternalServiceConfig;
import com.signature.service.DynamicConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动态配置管理控制器
 * 提供配置查询和缓存管理接口
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class DynamicConfigController {

    private final DynamicConfigService dynamicConfigService;

    // ========== 路径配置相关 ==========

    /**
     * 获取所有排除路径
     */
    @GetMapping("/paths/excluded")
    public Map<String, Object> getExcludedPaths(@RequestParam(required = false) String httpMethod) {
        try {
            List<String> paths = httpMethod != null ? 
                dynamicConfigService.getExcludedPaths(httpMethod) : 
                dynamicConfigService.getExcludedPaths();
            
            return Map.of(
                "success", true,
                "data", paths,
                "message", "排除路径获取成功"
            );
        } catch (Exception e) {
            log.error("获取排除路径失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "获取排除路径失败"
            );
        }
    }

    /**
     * 获取所有严格验证路径
     */
    @GetMapping("/paths/strict")
    public Map<String, Object> getStrictValidationPaths(@RequestParam(required = false) String httpMethod) {
        try {
            List<String> paths = httpMethod != null ? 
                dynamicConfigService.getStrictValidationPaths(httpMethod) : 
                dynamicConfigService.getStrictValidationPaths();
            
            return Map.of(
                "success", true,
                "data", paths,
                "message", "严格验证路径获取成功"
            );
        } catch (Exception e) {
            log.error("获取严格验证路径失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "获取严格验证路径失败"
            );
        }
    }

    /**
     * 检查路径是否为排除路径
     */
    @GetMapping("/paths/check/excluded")
    public Map<String, Object> checkExcludedPath(@RequestParam String path, 
                                                 @RequestParam(defaultValue = "GET") String httpMethod) {
        try {
            boolean isExcluded = dynamicConfigService.isExcludedPath(path, httpMethod);
            
            return Map.of(
                "success", true,
                "data", Map.of(
                    "path", path,
                    "httpMethod", httpMethod,
                    "isExcluded", isExcluded
                ),
                "message", "路径检查完成"
            );
        } catch (Exception e) {
            log.error("检查排除路径失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "检查路径失败"
            );
        }
    }

    // ========== 验证规则配置相关 ==========

    /**
     * 获取验证规则状态
     */
    @GetMapping("/validation/status")
    public Map<String, Object> getValidationStatus() {
        try {
            return Map.of(
                "success", true,
                "data", Map.of(
                    "apiKeyValidationEnabled", dynamicConfigService.isApiKeyValidationEnabled(),
                    "signatureValidationEnabled", dynamicConfigService.isSignatureValidationEnabled(),
                    "authenticationValidationEnabled", dynamicConfigService.isAuthenticationValidationEnabled()
                ),
                "message", "验证规则状态获取成功"
            );
        } catch (Exception e) {
            log.error("获取验证规则状态失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "获取验证规则状态失败"
            );
        }
    }

    /**
     * 获取验证规则配置
     */
    @GetMapping("/validation/rule/{ruleCode}")
    public Map<String, Object> getValidationRule(@PathVariable String ruleCode) {
        try {
            ValidationRuleConfig rule = dynamicConfigService.getValidationRule(ruleCode);
            if (rule == null) {
                return Map.of(
                    "success", false,
                    "message", "验证规则不存在: " + ruleCode
                );
            }
            
            return Map.of(
                "success", true,
                "data", rule,
                "message", "验证规则获取成功"
            );
        } catch (Exception e) {
            log.error("获取验证规则失败: {}", ruleCode, e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "获取验证规则失败"
            );
        }
    }

    // ========== 内部服务配置相关 ==========

    /**
     * 获取所有内部服务配置
     */
    @GetMapping("/services/internal")
    public Map<String, Object> getAllInternalServices() {
        try {
            List<InternalServiceConfig> services = dynamicConfigService.getAllInternalServices();
            
            return Map.of(
                "success", true,
                "data", services,
                "message", "内部服务配置获取成功"
            );
        } catch (Exception e) {
            log.error("获取内部服务配置失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "获取内部服务配置失败"
            );
        }
    }

    /**
     * 检查是否为内部服务
     */
    @GetMapping("/services/check/internal")
    public Map<String, Object> checkInternalService(@RequestParam String serviceCode) {
        try {
            boolean isInternal = dynamicConfigService.isInternalService(serviceCode);
            InternalServiceConfig service = dynamicConfigService.getInternalService(serviceCode);
            
            return Map.of(
                "success", true,
                "data", Map.of(
                    "serviceCode", serviceCode,
                    "isInternal", isInternal,
                    "serviceInfo", service
                ),
                "message", "内部服务检查完成"
            );
        } catch (Exception e) {
            log.error("检查内部服务失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "检查内部服务失败"
            );
        }
    }

    // ========== 缓存管理相关 ==========

    /**
     * 刷新所有配置缓存
     */
    @PostMapping("/cache/refresh")
    public Map<String, Object> refreshConfigCache() {
        try {
            dynamicConfigService.refreshConfigCache();
            log.info("配置缓存刷新成功");
            
            return Map.of(
                "success", true,
                "message", "配置缓存刷新成功"
            );
        } catch (Exception e) {
            log.error("刷新配置缓存失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "刷新配置缓存失败"
            );
        }
    }

    /**
     * 刷新路径配置缓存
     */
    @PostMapping("/cache/refresh/paths")
    public Map<String, Object> refreshPathConfigCache() {
        try {
            dynamicConfigService.refreshPathConfigCache();
            log.info("路径配置缓存刷新成功");
            
            return Map.of(
                "success", true,
                "message", "路径配置缓存刷新成功"
            );
        } catch (Exception e) {
            log.error("刷新路径配置缓存失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "刷新路径配置缓存失败"
            );
        }
    }

    /**
     * 刷新验证规则缓存
     */
    @PostMapping("/cache/refresh/validation")
    public Map<String, Object> refreshValidationRuleCache() {
        try {
            dynamicConfigService.refreshValidationRuleCache();
            log.info("验证规则缓存刷新成功");
            
            return Map.of(
                "success", true,
                "message", "验证规则缓存刷新成功"
            );
        } catch (Exception e) {
            log.error("刷新验证规则缓存失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "刷新验证规则缓存失败"
            );
        }
    }

    /**
     * 清除所有配置缓存
     */
    @DeleteMapping("/cache/clear")
    public Map<String, Object> clearConfigCache() {
        try {
            dynamicConfigService.clearConfigCache();
            log.info("配置缓存清除成功");
            
            return Map.of(
                "success", true,
                "message", "配置缓存清除成功"
            );
        } catch (Exception e) {
            log.error("清除配置缓存失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "清除配置缓存失败"
            );
        }
    }

    /**
     * 配置诊断接口
     */
    @GetMapping("/diagnostic")
    public Map<String, Object> configDiagnostic(@RequestParam String path, 
                                               @RequestParam(defaultValue = "GET") String httpMethod) {
        try {
            return Map.of(
                "success", true,
                "data", Map.of(
                    "path", path,
                    "httpMethod", httpMethod,
                    "isExcludedPath", dynamicConfigService.isExcludedPath(path, httpMethod),
                    "isStrictValidationPath", dynamicConfigService.isStrictValidationPath(path, httpMethod),
                    "isInternalPath", dynamicConfigService.isInternalPath(path, httpMethod),
                    "apiKeyValidationEnabled", dynamicConfigService.isApiKeyValidationEnabled(),
                    "signatureValidationEnabled", dynamicConfigService.isSignatureValidationEnabled(),
                    "authenticationValidationEnabled", dynamicConfigService.isAuthenticationValidationEnabled()
                ),
                "message", "配置诊断完成"
            );
        } catch (Exception e) {
            log.error("配置诊断失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "配置诊断失败"
            );
        }
    }
}
