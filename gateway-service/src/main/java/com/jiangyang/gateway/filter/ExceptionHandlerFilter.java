package com.jiangyang.gateway.filter;

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

/**
 * 异常处理过滤器
 * 统一处理网关异常，返回友好的错误信息
 * 
 * @author jiangyang
 * @since 2024-01-01
 */
@Slf4j
@Component
public class ExceptionHandlerFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(throwable -> {
                    log.error("网关处理请求时发生异常", throwable);
                    
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    
                    String errorMessage = String.format(
                        "{\"code\":500,\"message\":\"网关服务异常\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                        System.currentTimeMillis(),
                        exchange.getRequest().getPath()
                    );
                    
                    DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(Mono.just(buffer));
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
} 