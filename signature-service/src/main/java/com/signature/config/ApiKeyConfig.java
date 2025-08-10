package com.signature.config;

import com.signature.entity.InternalServiceConfig;
import com.signature.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API密钥配置类
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "signature.api-key")
public class ApiKeyConfig {

    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Autowired
    private DefaultPathConfig defaultPathConfig;

    private boolean enabled = true;
    private String headerName = "X-API-Key";
    private String testKey = "test-api-key-123";
    private boolean strictValidation = true;
    private boolean enableLogging = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getTestKey() {
        return testKey;
    }

    public void setTestKey(String testKey) {
        this.testKey = testKey;
    }

    public boolean isStrictValidation() {
        return strictValidation;
    }

    public void setStrictValidation(boolean strictValidation) {
        this.strictValidation = strictValidation;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    /**
     * 获取排除路径列表（从数据库动态获取，如果失败则使用默认配置）
     */
    public List<String> getExcludedPaths() {
        try {
            List<String> paths = dynamicConfigService.getExcludedPaths();
            if (paths != null && !paths.isEmpty()) {
                return paths;
            }
        } catch (Exception e) {
            // 如果数据库获取失败，使用默认配置
        }
        return defaultPathConfig.getDefaultExcludedPaths();
    }

    /**
     * 获取严格验证路径列表（从数据库动态获取，如果失败则使用默认配置）
     */
    public List<String> getStrictValidationPaths() {
        try {
            List<String> paths = dynamicConfigService.getStrictValidationPaths();
            if (paths != null && !paths.isEmpty()) {
                return paths;
            }
        } catch (Exception e) {
            // 如果数据库获取失败，使用默认配置
        }
        return defaultPathConfig.getDefaultStrictValidationPaths();
    }

    /**
     * 检查路径是否被排除
     */
    public boolean isExcludedPath(String path) {
        return dynamicConfigService.isPathMatchType(path, "EXCLUDED");
    }

    /**
     * 检查路径是否需要严格验证
     */
    public boolean isStrictValidationPath(String path) {
        return dynamicConfigService.isPathMatchType(path, "STRICT");
    }

    /**
     * 检查是否允许内部服务跳过验证
     */
    public boolean isAllowInternalServiceSkip() {
        return true; // 默认允许内部服务跳过验证
    }

    /**
     * 获取内部服务列表（从数据库动态获取，如果失败则使用默认配置）
     */
    public List<String> getInternalServices() {
        try {
            List<InternalServiceConfig> services = dynamicConfigService.getInternalServices();
            if (services != null && !services.isEmpty()) {
                return services.stream()
                        .map(service -> service.getServiceName())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // 如果数据库获取失败，使用默认配置
        }
        return defaultPathConfig.getDefaultInternalServices();
    }

    /**
     * 获取内部路径列表（从数据库动态获取，如果失败则使用默认配置）
     */
    public List<String> getInternalPaths() {
        try {
            List<String> paths = dynamicConfigService.getInternalPaths();
            if (paths != null && !paths.isEmpty()) {
                return paths;
            }
        } catch (Exception e) {
            // 如果数据库获取失败，使用默认配置
        }
        return defaultPathConfig.getDefaultInternalPaths();
    }
}
