package com.jiangyang.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局日志过滤器
 * 记录所有请求和响应的日志信息
 * 
 * @author jiangyang
 * @since 2024-01-01
 */
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = generateRequestId();
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        
        log.info("=== 网关请求开始 ===");
        log.info("请求ID: {}", requestId);
        log.info("请求时间: {}", LocalDateTime.now().format(FORMATTER));
        log.info("请求方法: {}", request.getMethod());
        log.info("请求路径: {}", request.getPath());
        log.info("请求URI: {}", request.getURI());
        log.info("请求头: {}", request.getHeaders());
        log.info("客户端IP: {}", getClientIp(request));
        
        // 添加请求ID到请求头
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Request-ID", requestId)
                .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .doFinally(signalType -> {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    
                    log.info("=== 网关请求结束 ===");
                    log.info("请求ID: {}", requestId);
                    log.info("响应时间: {}", LocalDateTime.now().format(FORMATTER));
                    log.info("处理耗时: {}ms", duration);
                    log.info("响应状态: {}", exchange.getResponse().getStatusCode());
                    log.info("响应头: {}", exchange.getResponse().getHeaders());
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "GW-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
               request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
} 