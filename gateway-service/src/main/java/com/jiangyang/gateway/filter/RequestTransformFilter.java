package com.jiangyang.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 请求转换过滤器
 * 实现Header管理和请求转换功能
 */
@Slf4j
@Component
public class RequestTransformFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 转换请求
        ServerHttpRequest transformedRequest = transformRequest(request);
        
        // 创建新的exchange
        ServerWebExchange transformedExchange = exchange.mutate()
                .request(transformedRequest)
                .build();
        
        return chain.filter(transformedExchange);
    }

    /**
     * 转换请求
     */
    private ServerHttpRequest transformRequest(ServerHttpRequest request) {
        ServerHttpRequest.Builder builder = request.mutate();
        
        // 添加或更新请求头
        addRequestHeaders(builder, request);
        
        // 移除敏感请求头
        removeSensitiveHeaders(builder);
        
        return builder.build();
    }

    /**
     * 添加请求头
     */
    private void addRequestHeaders(ServerHttpRequest.Builder builder, ServerHttpRequest request) {
        // 只在请求头不存在时才添加
        if (!request.getHeaders().containsKey(REQUEST_ID_HEADER)) {
            String requestId = UUID.randomUUID().toString();
            builder.header(REQUEST_ID_HEADER, requestId);
            
            // 添加跟踪ID（用于链路追踪）
            builder.header(TRACE_ID_HEADER, requestId);
        }
        
        // 添加时间戳（如果不存在）
        if (!request.getHeaders().containsKey(TIMESTAMP_HEADER)) {
            builder.header(TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()));
        }
        
        // 添加用户ID（从JWT中解析）
        String userId = extractUserIdFromToken(request);
        if (userId != null && !request.getHeaders().containsKey(USER_ID_HEADER)) {
            builder.header(USER_ID_HEADER, userId);
        }
        
        // 添加其他业务相关的请求头（如果不存在）
        if (!request.getHeaders().containsKey("X-Gateway-Version")) {
            builder.header("X-Gateway-Version", "1.0.0");
        }
        if (!request.getHeaders().containsKey("X-Service-Name")) {
            builder.header("X-Service-Name", "gateway-service");
        }
    }

    /**
     * 移除敏感请求头
     */
    private void removeSensitiveHeaders(ServerHttpRequest.Builder builder) {
        // 移除可能包含敏感信息的请求头
        builder.headers(httpHeaders -> {
            httpHeaders.remove("X-Forwarded-For");
            httpHeaders.remove("X-Real-IP");
            httpHeaders.remove("X-Forwarded-Proto");
        });
    }

    /**
     * 从JWT令牌中提取用户ID
     */
    private String extractUserIdFromToken(ServerHttpRequest request) {
        // TODO: 实现从JWT令牌中提取用户ID的逻辑
        // 这里应该解析JWT令牌并提取用户ID
        // 暂时返回null，实际项目中需要实现真实的JWT解析
        return null;
    }

    @Override
    public int getOrder() {
        return -50; // 在日志过滤器之后执行
    }
} 