package com.signature.config;


import com.signature.service.DynamicConfigService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 认证配置类
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "signature.authentication")
public class AuthenticationConfig {

    @Autowired
    private DynamicConfigService dynamicConfigService;

    /**
     * 是否启用认证
     */
    private boolean enabled = true;

    /**
     * 认证头名称
     */
    private String authorizationHeaderName = "Authorization";

    /**
     * Bearer前缀
     */
    private String bearerPrefix = "Bearer ";

    /**
     * API密钥头名称
     */
    private String apiKeyHeaderName = "X-API-Key";

    /**
     * 请求来源头名称
     */
    private String requestFromHeaderName = "X-Request-From";

    /**
     * 测试API密钥
     */
    private String testApiKey = "test-api-key-123";

    /**
     * 内部服务名称
     */
    private String internalServiceName = "bgtech-ai";

    /**
     * 是否启用严格验证
     */
    private boolean strictValidation = true;

    /**
     * 是否记录认证日志
     */
    private boolean enableLogging = true;

    /**
     * 是否允许内部服务跳过验证
     */
    private boolean allowInternalServiceSkip = true;

    /**
     * 获取排除路径列表（从数据库动态获取）
     */
    public List<String> getExcludedPaths() {
        return dynamicConfigService.getExcludedPaths();
    }

    /**
     * 获取严格验证路径列表（从数据库动态获取）
     */
    public List<String> getStrictValidationPaths() {
        return dynamicConfigService.getStrictValidationPaths();
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
     * 获取内部服务列表
     */
    public List<String> getInternalServices() {
        return dynamicConfigService.getInternalServices().stream()
                .map(service -> service.getServiceName())
                .collect(Collectors.toList());
    }

    /**
     * 获取内部路径列表
     */
    public List<String> getInternalPaths() {
        return dynamicConfigService.getInternalPaths();
    }

    /**
     * JWT相关配置
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * JWT配置内部类
     */
    @Data
    public static class JwtConfig {
        /**
         * JWT密钥
         */
        private String secret = "your-jwt-secret-key";

        /**
         * JWT过期时间（秒）
         */
        private long expirationSeconds = 3600;

        /**
         * JWT刷新过期时间（秒）
         */
        private long refreshExpirationSeconds = 86400;

        /**
         * JWT签发者
         */
        private String issuer = "signature-service";

        /**
         * JWT受众
         */
        private String audience = "signature-api";
    }
}
