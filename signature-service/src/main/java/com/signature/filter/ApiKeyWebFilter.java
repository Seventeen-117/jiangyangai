package com.signature.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signature.config.ApiKeyConfig;
import com.signature.model.ApiKeyValidationResult;
import com.signature.service.ApiKeyService;
import com.signature.service.DynamicConfigService;
import com.signature.utils.PathMatcherUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * API密钥验证WebFilter，用于WebFlux环境下的API密钥验证
 * 特别针对signature-service端点进行严格的API密钥状态验证
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // 在CORS过滤器之后，但在其他业务过滤器之前执行
public class ApiKeyWebFilter implements WebFilter {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyConfig apiKeyConfig;

    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.info("Processing WebFlux request: {} {}", method, path);

        // 预检OPTIONS请求直接通过
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            log.debug("OPTIONS request, skipping API Key validation: {}", path);
            return chain.filter(exchange);
        }

        // Swagger 与静态资源直接放行，避免DB访问与鉴权拦截
        if (path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/webjars")) {
            log.debug("Swagger path, skipping API Key validation: {}", path);
            return chain.filter(exchange);
        }

        // 检查API Key验证规则是否启用
        if (!dynamicConfigService.isApiKeyValidationEnabled()) {
            log.debug("API Key validation disabled by dynamic config, skipping: {}", path);
            return chain.filter(exchange);
        }

        // 检查是否为排除的路径
        if (isExcludedPath(path, method)) {
            log.info("Excluded path, skipping API Key validation: {}", path);
            return chain.filter(exchange);
        }

        // 如果API Key校验被禁用，直接通过
        if (!apiKeyConfig.isEnabled()) {
            log.debug("API Key validation disabled by static config, skipping: {}", path);
            return chain.filter(exchange);
        }

        // 获取API Key
        List<String> apiKeyHeaders = exchange.getRequest().getHeaders().get(apiKeyConfig.getHeaderName());
        String apiKey = apiKeyHeaders != null && !apiKeyHeaders.isEmpty() ? apiKeyHeaders.get(0) : null;

        // API Key不存在
        if (!StringUtils.hasText(apiKey)) {
            log.warn("API Key is missing for path: {} {} - Headers: {}", method, path, exchange.getRequest().getHeaders());
            return handleUnauthorized(exchange, "API Key is required");
        }
        
        // 如果是测试API Key且请求来源是内部服务，则通过验证
        List<String> requestFromHeaders = exchange.getRequest().getHeaders().get("X-Request-From");
        String requestFrom = requestFromHeaders != null && !requestFromHeaders.isEmpty() ? requestFromHeaders.get(0) : null;
        
        if (apiKeyConfig.getTestKey().equals(apiKey) && isInternalServiceRequest(exchange, requestFrom, path)) {
            log.debug("Test API key accepted for internal service call: {} {}", method, path);
            return chain.filter(exchange);
        }

        // 检查是否需要严格验证API Key状态
        boolean isStrictValidationPath = isStrictValidationPath(path, method);

        // 验证API Key
        try {
            ApiKeyValidationResult result = apiKeyService.validateApiKeyStatus(apiKey);
            
            if (result.getStatus().name().equals("VALID")) {
                // 对于严格验证路径，额外检查API Key的active状态
                if (isStrictValidationPath) {
                    // 获取完整的API Key信息以检查active状态
                    var keyInfo = apiKeyService.getApiKeyInfo(apiKey);
                    if (keyInfo == null || keyInfo.getActive() == null || keyInfo.getActive() == 0) {
                        log.warn("Inactive API Key used for strict validation path: {} {}", method, path);
                        return handleUnauthorized(exchange, "API Key is inactive");
                    }
                    log.info("Strict API Key validation passed for path: {} {}", method, path);
                } else {
                    log.debug("API Key validation passed for path: {} {}", method, path);
                }
            } else {
                log.warn("Invalid API Key provided for path: {} {}, reason: {}, strict validation: {}", 
                        method, path, result.getErrorMessage(), isStrictValidationPath);
                return handleUnauthorized(exchange, result.getErrorMessage() != null ? result.getErrorMessage() : "Invalid API Key");
            }
        } catch (Exception e) {
            log.error("Error validating API key for path: {} {}: {}", method, path, e.getMessage(), e);
            return handleUnauthorized(exchange, "API Key validation error");
        }

        // API Key有效，继续处理请求
        return chain.filter(exchange);
    }

    /**
     * 检查是否为排除的路径
     * 优先使用数据库配置，如果数据库配置不可用则回退到静态配置
     */
    private boolean isExcludedPath(String path, String method) {
        try {
            // 优先使用动态配置（数据库）
            boolean isExcluded = dynamicConfigService.isExcludedPath(path, method);
            if (isExcluded) {
                log.info("Path excluded by dynamic config: {} {}", method, path);
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking dynamic excluded paths, falling back to static config: {}", e.getMessage());
        }
        
        // 回退到静态配置
        try {
            boolean isStaticExcluded = PathMatcherUtil.matchesAny(path, apiKeyConfig.getExcludedPaths()) || 
                   PathMatcherUtil.containsAny(path, "/public/", "/health", "/actuator");
            log.info("Path exclusion check for '{}': {} (excluded paths: {})", 
                    path, isStaticExcluded, apiKeyConfig.getExcludedPaths());
            return isStaticExcluded;
        } catch (Exception e) {
            log.warn("Error checking static excluded path for: {}, error: {}", path, e.getMessage());
            // 如果检查失败，默认不排除
            return false;
        }
    }
    
    /**
     * 检查是否为需要严格验证API Key状态的路径
     * 优先使用数据库配置，如果数据库配置不可用则回退到静态配置
     */
    private boolean isStrictValidationPath(String path, String method) {
        try {
            // 优先使用动态配置（数据库）
            boolean isStrict = dynamicConfigService.isStrictValidationPath(path, method);
            if (isStrict) {
                log.debug("Strict validation required by dynamic config: {} {}", method, path);
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking dynamic strict validation paths, falling back to static config: {}", e.getMessage());
        }
        
        // 回退到静态配置
        try {
            boolean isStaticStrict = PathMatcherUtil.matchesAny(path, apiKeyConfig.getStrictValidationPaths());
            if (isStaticStrict) {
                log.debug("Strict validation required by static config: {} {}", method, path);
            }
            return isStaticStrict;
        } catch (Exception e) {
            log.warn("Error checking static strict validation path for: {}, error: {}", path, e.getMessage());
            // 如果检查失败，默认不需要严格验证
            return false;
        }
    }

    /**
     * 检查是否为内部服务请求
     * 优先使用数据库配置，如果数据库配置不可用则回退到静态配置
     */
    private boolean isInternalServiceRequest(ServerWebExchange exchange, String requestFrom, String path) {
        if (!apiKeyConfig.isAllowInternalServiceSkip()) {
            return false;
        }
        
        // 检查请求来源头是否为内部服务
        if (StringUtils.hasText(requestFrom)) {
            try {
                // 优先使用动态配置
                boolean isInternal = dynamicConfigService.isInternalService(requestFrom);
                if (isInternal) {
                    log.debug("Internal service identified by dynamic config: {}", requestFrom);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Error checking dynamic internal services, falling back to static config: {}", e.getMessage());
            }
            
            // 回退到静态配置
            boolean isStaticInternal = PathMatcherUtil.equalsAny(requestFrom, apiKeyConfig.getInternalServices());
            if (isStaticInternal) {
                log.debug("Internal service identified by static config: {}", requestFrom);
                return true;
            }
        }
        
        // 检查User-Agent
        List<String> userAgentHeaders = exchange.getRequest().getHeaders().get("User-Agent");
        if (userAgentHeaders != null && !userAgentHeaders.isEmpty()) {
            String userAgent = userAgentHeaders.get(0);
            if (StringUtils.hasText(userAgent)) {
                boolean isUserAgentInternal = PathMatcherUtil.containsAny(userAgent, apiKeyConfig.getInternalServices());
                if (isUserAgentInternal) {
                    log.debug("Internal service identified by User-Agent: {}", userAgent);
                    return true;
                }
            }
        }
        
        // 检查请求路径是否为内部路径
        try {
            // 优先使用动态配置
            String method = exchange.getRequest().getMethod().name();
            boolean isInternalPath = dynamicConfigService.isInternalPath(path, method);
            if (isInternalPath) {
                log.debug("Internal path identified by dynamic config: {} {}", method, path);
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking dynamic internal paths, falling back to static config: {}", e.getMessage());
        }
        
        // 回退到静态配置
        boolean isStaticInternalPath = PathMatcherUtil.matchesAny(path, apiKeyConfig.getInternalPaths());
        if (isStaticInternalPath) {
            log.debug("Internal path identified by static config: {}", path);
        }
        
        return isStaticInternalPath;
    }

    /**
     * 处理未授权请求
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "UNAUTHORIZED",
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        
        try {
            byte[] responseBytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBytes))
            );
        } catch (Exception e) {
            log.error("Error writing error response", e);
            return exchange.getResponse().setComplete();
        }
    }
}
