package com.signature.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 签名验证配置类
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "signature.verification")
public class SignatureConfig {

    /**
     * 是否启用签名验证
     */
    private boolean enabled = true;

    /**
     * 时间戳过期时间（秒）
     */
    private long timestampExpireSeconds = 300;

    /**
     * nonce缓存过期时间（秒）
     */
    private long nonceCacheExpireSeconds = 1800;

    /**
     * 是否启用严格验证
     */
    private boolean strictValidation = true;

    /**
     * 是否记录签名验证日志
     */
    private boolean enableLogging = true;

    /**
     * 是否允许内部服务跳过验证
     */
    private boolean allowInternalServiceSkip = true;

    /**
     * 排除的路径模式
     */
    private String[] excludedPaths = {
            "/actuator/**",
            "/health/**",
            "/info/**",
            "/metrics/**",
            "/api/auth/**",
            "/api/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/favicon.ico",
            "/error"
    };

    /**
     * 需要严格验证的路径模式
     */
    private String[] strictValidationPaths = {
            "/api/signature/**",
            "/api/security/**",
            "/api/keys/**"
    };

    /**
     * 内部服务标识
     */
    private String[] internalServices = {
            "bgai-service",
            "gateway-service",
            "signature-service"
    };

    /**
     * 内部请求路径前缀
     */
    private String[] internalPaths = {
            "/api/users/**",
            "/api/internal/**",
            "/api/admin/**"
    };

    /**
     * 签名算法
     */
    private String algorithm = "HmacSHA256";

    /**
     * 签名参数名称
     */
    private String[] signatureParams = {
            "appId",
            "timestamp",
            "nonce",
            "sign"
    };

    /**
     * 是否验证请求体签名
     */
    private boolean verifyRequestBody = false;

    /**
     * 请求体签名字段名
     */
    private String bodySignatureField = "bodySign";

    /**
     * 是否启用防重放攻击
     */
    private boolean enableReplayAttackProtection = true;

    /**
     * 是否启用时间戳验证
     */
    private boolean enableTimestampValidation = true;

    // ========== 异步验证配置 ==========

    /**
     * 是否启用异步验证
     */
    private boolean asyncEnabled = false;

    /**
     * 异步验证模式：ASYNC（完全异步）、HYBRID（混合验证）、QUICK（快速验证）
     */
    private String asyncMode = "HYBRID";

    /**
     * 异步验证超时时间（毫秒）
     */
    private long asyncTimeout = 5000;

    /**
     * 异步验证线程池大小
     */
    private int asyncThreadPoolSize = 10;

    /**
     * 批量验证线程池大小
     */
    private int batchThreadPoolSize = 5;

    /**
     * 是否启用异步事件发布
     */
    private boolean enableAsyncEventPublishing = true;

    /**
     * 异步验证重试次数
     */
    private int asyncRetryCount = 3;

    /**
     * 异步验证重试间隔（毫秒）
     */
    private long asyncRetryInterval = 1000;
}
