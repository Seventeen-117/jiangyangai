package com.signature.config;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 默认路径配置
 * 提供基本的路径配置，避免数据库依赖问题
 */
@Component
public class DefaultPathConfig {

    /**
     * 获取默认的排除路径
     */
    public List<String> getDefaultExcludedPaths() {
        return Arrays.asList(
            "/actuator/**",
            "/health/**",
            "/metrics/**",
            "/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/validation/**",
            "/api/metrics/**",
            "/api/token/**",
            "/api/keys/**"
        );
    }

    /**
     * 获取默认的严格验证路径
     */
    public List<String> getDefaultStrictValidationPaths() {
        return Arrays.asList(
            "/api/admin/**",
            "/api/sso/admin/**"
        );
    }

    /**
     * 获取默认的内部路径
     */
    public List<String> getDefaultInternalPaths() {
        return Arrays.asList(
            "/api/internal/**",
            "/api/gateway/**"
        );
    }

    /**
     * 获取默认的内部服务列表
     */
    public List<String> getDefaultInternalServices() {
        return Arrays.asList(
            "gateway-service",
            "signature-service"
        );
    }
}
