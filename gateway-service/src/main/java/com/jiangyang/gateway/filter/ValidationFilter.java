package com.jiangyang.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.jiangyang.gateway.config.CustomGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 验证过滤器工厂
 * 调用signature-service进行验证
 */
@Slf4j
@Component
public class ValidationFilter extends AbstractGatewayFilterFactory<ValidationFilter.Config> {

    private final CustomGatewayProperties gatewayProperties;
    private final WebClient webClient;

    public ValidationFilter(CustomGatewayProperties gatewayProperties, WebClient webClient) {
        super(Config.class);
        this.gatewayProperties = gatewayProperties;
        this.webClient = webClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            log.info("ValidationFilter processing path: {}", path);
            
            // 跳过不需要验证的路径
            if (isSkipValidation(path)) {
                log.debug("Skipping validation for path: {}", path);
                return chain.filter(exchange);
            }
        
        // 构建验证请求
        Map<String, Object> validationRequest = buildValidationRequest(exchange);
        
        // 获取signature-service配置
        CustomGatewayProperties.SignatureServiceConfig signatureConfig = gatewayProperties.getSignatureService();
        if (signatureConfig == null) {
            log.error("Signature service configuration not found");
            return validationFailed(exchange, "Signature service configuration error");
        }
        
        // 构建完整的验证URL
        String validationUrl = signatureConfig.getBaseUrl() + signatureConfig.getValidationEndpoint();
        
        // 获取重试配置
        int retryAttempts = signatureConfig.getRetryAttempts();
        
        // 调用signature-service进行验证（带重试机制）
        return webClient.post()
                .uri(validationUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validationRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.fixedDelay(retryAttempts, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            // 只对网络错误和超时进行重试
                            log.warn("Retrying validation request for path: {}, attempt: {}", path, throwable.getMessage());
                            return true;
                        }))
                .flatMap(result -> {
                    log.info("收到signature-service验证响应: {}", result);
                    
                    // 检查响应格式
                    if (result == null) {
                        log.error("Validation service returned null response for path: {}", path);
                        return validationFailed(exchange, "Invalid validation response");
                    }
                    
                    Object validObj = result.get("valid");
                    if (validObj == null) {
                        log.error("Validation service response missing 'valid' field for path: {}, response: {}", path, result);
                        return validationFailed(exchange, "Invalid validation response format");
                    }
                    
                    boolean isValid = Boolean.TRUE.equals(validObj);
                    log.info("验证结果解析: valid={}, path={}", isValid, path);
                    
                    if (isValid) {
                        log.info("Validation passed for path: {}", path);
                        return chain.filter(exchange);
                    } else {
                        String errorMessage = (String) result.get("errorMessage");
                        if (errorMessage == null) {
                            errorMessage = "Validation failed";
                        }
                        log.warn("Validation failed for path: {}, reason: {}", path, errorMessage);
                        return validationFailed(exchange, errorMessage);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("Validation service error for path: {}, url: {}, after {} retries", 
                            path, validationUrl, retryAttempts, throwable);
                    return validationFailed(exchange, "Validation service unavailable");
                });
        };
    }

    /**
     * 判断是否需要跳过验证
     */
    private boolean isSkipValidation(String path) {
        // 只跳过健康检查、监控、签名服务自身等路径
        return path.startsWith("/actuator") || 
               path.startsWith("/health") || 
               path.startsWith("/metrics") ||
               path.startsWith("/public") ||
               path.startsWith("/api/validation") ||
               path.startsWith("/api/signature") ||
               path.startsWith("/api/token") ||
               path.startsWith("/api/keys") ||
               path.startsWith("/api/auth") ||
               path.startsWith("/api/sso");
    }

    /**
     * 构建验证请求
     */
    private Map<String, Object> buildValidationRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        
        Map<String, Object> validationRequest = new HashMap<>();
        validationRequest.put("path", path);
        validationRequest.put("method", method);
        
        // 提取API Key
        String apiKey = request.getHeaders().getFirst("X-API-Key");
        if (StringUtils.hasText(apiKey)) {
            validationRequest.put("apiKey", apiKey);
        }
        
        // 提取签名信息
        String signature = request.getHeaders().getFirst("X-Signature");
        if (StringUtils.hasText(signature)) {
            validationRequest.put("signature", signature);
        }
        
        // 提取时间戳
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        if (StringUtils.hasText(timestamp)) {
            validationRequest.put("timestamp", Long.parseLong(timestamp));
        }
        
        // 提取随机数
        String nonce = request.getHeaders().getFirst("X-Nonce");
        if (StringUtils.hasText(nonce)) {
            validationRequest.put("nonce", nonce);
        }
        
        // 提取应用ID
        String appId = request.getHeaders().getFirst("X-App-Id");
        if (StringUtils.hasText(appId)) {
            validationRequest.put("appId", appId);
        }
        
        // 提取JWT Token
        String jwtToken = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(jwtToken) && jwtToken.startsWith("Bearer ")) {
            validationRequest.put("jwtToken", jwtToken.substring(7));
        }
        
        // 提取客户端IP
        String clientIp = getClientIp(request);
        validationRequest.put("clientIp", clientIp);
        
        // 提取用户代理
        String userAgent = request.getHeaders().getFirst("User-Agent");
        if (StringUtils.hasText(userAgent)) {
            validationRequest.put("userAgent", userAgent);
        }
        
        return validationRequest;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * 验证失败响应
     */
    private Mono<Void> validationFailed(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());

        byte[] bytes = JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 配置类
     */
    public static class Config {
        // 可以添加配置参数
    }
}
