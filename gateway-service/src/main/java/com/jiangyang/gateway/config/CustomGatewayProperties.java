package com.jiangyang.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关配置属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway")
public class CustomGatewayProperties {

    /**
     * 跳过限流的路径
     */
    private List<String> skipRateLimitPaths;

    /**
     * 跳过熔断的路径
     */
    private List<String> skipCircuitBreakerPaths;

    /**
     * 跳过防御检查的路径
     */
    private List<String> skipDefensivePaths;
    
    /**
     * signature-service配置
     */
    private SignatureServiceConfig signatureService;

    /**
     * 路径权限配置
     */
    private List<PathPermission> pathPermissions;

    /**
     * 限流配置
     */
    private List<RateLimitConfig> rateLimitConfigs;

    /**
     * 熔断器配置
     */
    private List<CircuitBreakerConfig> circuitBreakerConfigs;

    /**
     * 防御性策略配置
     */
    private DefensiveConfig defensiveConfig;

    /**
     * 灰度发布配置
     */
    private CanaryConfig canary;

    /**
     * 灰度发布路径
     */
    private List<String> canaryPaths;

    /**
     * 灰度发布服务配置
     */
    private List<CanaryService> canaryServices;

    /**
     * 路径权限配置
     */
    @Data
    public static class PathPermission {
        private String path;
        private List<String> methods;
        private List<String> roles;
    }

    /**
     * 限流配置
     */
    @Data
    public static class RateLimitConfig {
        private String path;
        private int maxRequests;
        private int timeWindow;
    }

    /**
     * 熔断器配置
     */
    @Data
    public static class CircuitBreakerConfig {
        private String path;
        private double failureRateThreshold;
        private int waitDurationInOpenState;
        private int ringBufferSizeInHalfOpenState;
        private int ringBufferSizeInClosedState;
        private String fallbackMessage;
    }

    /**
     * 防御性策略配置
     */
    @Data
    public static class DefensiveConfig {
        /**
         * 每分钟最大请求数
         */
        private int maxRequestsPerMinute = 1000;

        /**
         * 是否启用防爬虫
         */
        private boolean enableAntiCrawler = true;

        /**
         * 是否启用防重放攻击
         */
        private boolean enableAntiReplay = true;

        /**
         * 是否启用恶意内容检测
         */
        private boolean enableMaliciousContentDetection = true;

        /**
         * 时间戳有效期（秒）
         */
        private int timestampValiditySeconds = 300;

        /**
         * 黑名单IP列表
         */
        private List<String> blacklistedIps;

        /**
         * 白名单IP列表
         */
        private List<String> whitelistedIps;
    }
    
    /**
     * signature-service配置类
     */
    @Data
    public static class SignatureServiceConfig {
        /**
         * 基础URL
         */
        private String baseUrl;
        
        /**
         * 验证端点
         */
        private String validationEndpoint;
        
        /**
         * 超时时间（毫秒）
         */
        private int timeout = 5000;
        
        /**
         * 重试次数
         */
        private int retryAttempts = 3;
    }

    /**
     * 灰度发布配置
     */
    @Data
    public static class CanaryConfig {
        /**
         * 是否启用灰度发布
         */
        private boolean enabled = false;

        /**
         * 灰度权重
         */
        private int weight = 10;

        /**
         * 灰度Header名称
         */
        private String header = "X-Canary";

        /**
         * 灰度Header值
         */
        private String value = "true";
    }

    /**
     * 灰度发布服务配置
     */
    @Data
    public static class CanaryService {
        /**
         * 服务名称
         */
        private String name;

        /**
         * 灰度版本
         */
        private String canaryVersion;

        /**
         * 权重
         */
        private int weight;
    }
} 