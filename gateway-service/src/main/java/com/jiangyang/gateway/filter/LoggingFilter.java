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
import java.util.UUID;

/**
 * 日志过滤器
 * 记录所有请求的访问日志（URL、状态码、延迟）
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = getOrGenerateRequestId(request);
        
        // 记录请求开始时间
        exchange.getAttributes().put(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        
        // 记录请求日志
        logRequest(request, requestId);
        
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // 记录响应日志
                    logResponse(exchange, requestId);
                });
    }

    /**
     * 获取或生成请求ID
     */
    private String getOrGenerateRequestId(ServerHttpRequest request) {
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    /**
     * 记录请求日志
     */
    private void logRequest(ServerHttpRequest request, String requestId) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String method = request.getMethod().name();
        String path = request.getPath().value();
        String query = request.getURI().getQuery();
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String clientIp = getClientIp(request);
        
        log.info("[REQUEST] [{}] [{}] {} {} {} {} {}",
                timestamp, requestId, method, path,
                query != null ? "?" + query : "",
                clientIp, userAgent != null ? userAgent : "");
    }

    /**
     * 记录响应日志
     */
    private void logResponse(ServerWebExchange exchange, String requestId) {
        Long startTime = exchange.getAttribute(START_TIME_ATTRIBUTE);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        int statusCode = exchange.getResponse().getStatusCode() != null ? 
                exchange.getResponse().getStatusCode().value() : 0;
        
        log.info("[RESPONSE] [{}] [{}] {} {} {} {}ms",
                timestamp, requestId, method, path, statusCode, duration);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        // 优先使用X-Forwarded-For头（代理场景）
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        
        // 使用真实IP地址
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -60; // 在熔断过滤器之后执行
    }
} 