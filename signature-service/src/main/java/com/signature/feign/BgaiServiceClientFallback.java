package com.signature.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Bgai服务Feign客户端降级处理
 * 
 * @author bgpay
 * @since 2024-01-01
 */
@Slf4j
@Component
public class BgaiServiceClientFallback implements BgaiServiceClient {

    @Override
    public Map<String, Object> validateApiKey(String apiKey) {
        log.warn("BgaiServiceClient validateApiKey fallback triggered for apiKey: {}", apiKey);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "Service unavailable");
        result.put("fallback", true);
        return result;
    }

    @Override
    public Map<String, Object> getUserInfo(String authorization) {
        log.warn("BgaiServiceClient getUserInfo fallback triggered");
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "Service unavailable");
        result.put("fallback", true);
        return result;
    }

    @Override
    public Map<String, Object> health() {
        log.warn("BgaiServiceClient health fallback triggered");
        Map<String, Object> result = new HashMap<>();
        result.put("status", "DOWN");
        result.put("message", "Service unavailable");
        result.put("fallback", true);
        return result;
    }
} 