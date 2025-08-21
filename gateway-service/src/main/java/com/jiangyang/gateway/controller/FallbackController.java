package com.jiangyang.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 降级处理器Controller
 * 用于处理服务不可用时的降级响应
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * bgai-service降级处理
     */
    @GetMapping("/bgai-service")
    public Mono<ResponseEntity<Map<String, Object>>> bgaiServiceFallback(ServerWebExchange exchange) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", 503);
        response.put("message", "bgai-service暂时不可用，请稍后重试");
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", exchange.getRequest().getPath().value());
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "30")
                .body(response));
    }

    /**
     * 通用降级处理
     */
    @GetMapping("/default")
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback(ServerWebExchange exchange) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", 503);
        response.put("message", "服务暂时不可用，请稍后重试");
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", exchange.getRequest().getPath().value());
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "30")
                .body(response));
    }
} 