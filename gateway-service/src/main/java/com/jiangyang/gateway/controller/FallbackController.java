package com.jiangyang.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 熔断器回退控制器
 * 处理服务不可用时的回退逻辑
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * 签名服务回退处理
     */
    @GetMapping("/signature-service")
    public Mono<ResponseEntity<Map<String, Object>>> signatureServiceFallback(ServerWebExchange exchange) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "Signature service is temporarily unavailable");
        response.put("service", "signature-service");
        response.put("fallback", true);
        response.put("path", exchange.getRequest().getPath().value());
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * bgai服务回退处理
     */
    @GetMapping("/bgai-service")
    public Mono<ResponseEntity<Map<String, Object>>> bgaiServiceFallback(ServerWebExchange exchange) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "BGAI service is temporarily unavailable");
        response.put("service", "bgai-service");
        response.put("fallback", true);
        response.put("path", exchange.getRequest().getPath().value());
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * 默认服务回退处理
     */
    @GetMapping("/default")
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback(ServerWebExchange exchange) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "Service is temporarily unavailable");
        response.put("service", "unknown");
        response.put("fallback", true);
        response.put("path", exchange.getRequest().getPath().value());
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * 聊天API回退处理
     */
    @GetMapping("/chat-api")
    public Mono<ResponseEntity<Map<String, Object>>> chatApiFallback(ServerWebExchange exchange) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "Chat API is temporarily unavailable");
        response.put("service", "chat-api");
        response.put("fallback", true);
        response.put("path", exchange.getRequest().getPath().value());
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * 认证API回退处理
     */
    @GetMapping("/auth-api")
    public Mono<ResponseEntity<Map<String, Object>>> authApiFallback(ServerWebExchange exchange) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "Auth API is temporarily unavailable");
        response.put("service", "auth-api");
        response.put("fallback", true);
        response.put("path", exchange.getRequest().getPath().value());
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
} 