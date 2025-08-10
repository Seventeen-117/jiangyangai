package com.jiangyang.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.jiangyang.gateway.config.CustomGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 限流过滤器
 * 实现流量控制和配额管理，防止突发流量打垮服务
 */
@Slf4j
@Component
@ConditionalOnBean(ReactiveRedisTemplate.class)
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final CustomGatewayProperties gatewayProperties;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        log.debug("RateLimitFilter processing path: {}", path);
        
        // 跳过不需要限流检查的路径
        if (isSkipRateLimit(path)) {
            log.debug("Skipping rate limit check for path: {}", path);
            return chain.filter(exchange);
        }
        
        // 获取客户端标识（IP地址或用户ID）
        String clientId = getClientId(request);
        
        // 检查限流配置
        CustomGatewayProperties.RateLimitConfig rateLimitConfig = getRateLimitConfig(path);
        if (rateLimitConfig != null) {
            // 执行限流检查
            if (!checkRateLimit(clientId, path, rateLimitConfig)) {
                log.warn("Rate limit exceeded for path: {}, clientId: {}", path, clientId);
                return rateLimitExceeded(exchange, "Rate limit exceeded");
            }
        }

        log.debug("RateLimitFilter passed for path: {}", path);
        return chain.filter(exchange);
    }

    /**
     * 判断是否需要跳过限流检查
     */
    private boolean isSkipRateLimit(String path) {
        // 从配置中读取白名单路径
        if (gatewayProperties.getSkipRateLimitPaths() != null) {
            return gatewayProperties.getSkipRateLimitPaths().stream()
                    .anyMatch(skipPath -> {
                        // 移除通配符进行匹配
                        String cleanSkipPath = skipPath.replace("**", "");
                        return path.startsWith(cleanSkipPath);
                    });
        }
        return false;
    }

    /**
     * 获取客户端标识
     */
    private String getClientId(ServerHttpRequest request) {
        // 优先使用X-Forwarded-For头（代理场景）
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        
        // 使用真实IP地址
        String remoteAddress = request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        
        return remoteAddress;
    }

    /**
     * 获取路径对应的限流配置
     */
    private CustomGatewayProperties.RateLimitConfig getRateLimitConfig(String path) {
        return gatewayProperties.getRateLimitConfigs().stream()
                .filter(config -> path.startsWith(config.getPath()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 检查限流
     */
    private boolean checkRateLimit(String clientId, String path, CustomGatewayProperties.RateLimitConfig config) {
        String key = String.format("rate_limit:%s:%s", clientId, path);
        
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        // 第一次访问，设置过期时间
                        return redisTemplate.expire(key, Duration.ofSeconds(config.getTimeWindow()))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .map(count -> count <= config.getMaxRequests())
                .defaultIfEmpty(true)
                .block();
    }

    /**
     * 返回限流响应
     */
    private Mono<Void> rateLimitExceeded(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add("Retry-After", "60");

        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());

        byte[] bytes = JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -80; // 在权限控制过滤器之后执行
    }
} 