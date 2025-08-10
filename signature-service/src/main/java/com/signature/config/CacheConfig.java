package com.signature.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 为动态配置服务提供缓存支持
 * 
 * @author signature-service
 * @since 2025-01-01
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置缓存管理器
     * 使用ConcurrentMapCacheManager作为简单的内存缓存
     * 在生产环境中可以替换为Redis缓存
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "pathConfig",             // 路径配置缓存
                "validationRule",         // 验证规则缓存
                "internalService",        // 内部服务配置缓存
                "configStats",            // 配置统计缓存
                "validation_results",     // 验证结果缓存
                "api_key_validation",     // API Key验证缓存
                "signature_validation",   // 签名验证缓存
                "permission_validation",  // 权限验证缓存
                "jwt_validation",         // JWT验证缓存
                "app_secret_cache",       // 应用密钥缓存
                "nonce_cache",            // Nonce缓存
                "api_key_cache",          // API密钥缓存
                "user_session_cache"      // 用户会话缓存
        );
    }
}