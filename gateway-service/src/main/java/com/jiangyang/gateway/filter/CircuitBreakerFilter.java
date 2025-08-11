package com.jiangyang.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.jiangyang.gateway.config.CustomGatewayProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔断过滤器
 * 实现熔断降级功能，在服务故障时快速失败，避免雪崩效应
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private final CustomGatewayProperties gatewayProperties;
    
    // 缓存熔断器实例
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        log.debug("CircuitBreakerFilter processing path: {}", path);
        
        // 跳过不需要熔断器检查的路径
        if (isSkipCircuitBreaker(path)) {
            log.debug("Skipping circuit breaker check for path: {}", path);
            return chain.filter(exchange);
        }
        
        // 获取路径对应的熔断器配置
        CustomGatewayProperties.CircuitBreakerConfig config = getCircuitBreakerConfig(path);
        if (config == null) {
            log.debug("No circuit breaker config found for path: {}", path);
            return chain.filter(exchange);
        }

        // 获取或创建熔断器
        CircuitBreaker circuitBreaker = getCircuitBreaker(path, config);
        
        // 使用熔断器包装请求
        return chain.filter(exchange)
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("Circuit breaker triggered for path: {}", path, throwable);
                    return fallbackResponse(exchange, config);
                });
    }

    /**
     * 判断是否需要跳过熔断器检查
     */
    private boolean isSkipCircuitBreaker(String path) {
        // 从配置中读取白名单路径
        if (gatewayProperties.getSkipCircuitBreakerPaths() != null) {
            return gatewayProperties.getSkipCircuitBreakerPaths().stream()
                    .anyMatch(skipPath -> {
                        // 移除通配符进行匹配
                        String cleanSkipPath = skipPath.replace("**", "");
                        return path.startsWith(cleanSkipPath);
                    });
        }
        return false;
    }

    /**
     * 获取路径对应的熔断器配置
     */
    private CustomGatewayProperties.CircuitBreakerConfig getCircuitBreakerConfig(String path) {
        return gatewayProperties.getCircuitBreakerConfigs().stream()
                .filter(config -> path.startsWith(config.getPath()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取或创建熔断器
     */
    private CircuitBreaker getCircuitBreaker(String path, CustomGatewayProperties.CircuitBreakerConfig config) {
        return circuitBreakers.computeIfAbsent(path, key -> {
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                    .slidingWindowSize(config.getRingBufferSizeInClosedState())
                    .failureRateThreshold((float) config.getFailureRateThreshold())
                    .waitDurationInOpenState(Duration.ofSeconds(config.getWaitDurationInOpenState()))
                    .permittedNumberOfCallsInHalfOpenState(config.getRingBufferSizeInHalfOpenState())
                    .build();
            
            return CircuitBreaker.of(path, circuitBreakerConfig);
        });
    }

    /**
     * 熔断降级响应
     */
    private Mono<Void> fallbackResponse(ServerWebExchange exchange, CustomGatewayProperties.CircuitBreakerConfig config) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", config.getFallbackMessage());
        result.put("timestamp", System.currentTimeMillis());

        byte[] bytes = JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -70; // 在限流过滤器之后执行
    }
} 