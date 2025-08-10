package com.signature.feign;

import com.signature.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Bgai服务Feign客户端
 * 用于与原始bgai服务进行通信
 * 
 * @author bgpay
 * @since 2024-01-01
 */
@FeignClient(name = "bgai-service", fallback = BgaiServiceClientFallback.class, configuration = FeignConfig.class)
public interface BgaiServiceClient {

    /**
     * 验证API密钥
     */
    @PostMapping("/api/auth/validate-api-key")
    Map<String, Object> validateApiKey(@RequestParam("apiKey") String apiKey);

    /**
     * 获取用户信息
     */
    @GetMapping("/api/auth/user-info")
    Map<String, Object> getUserInfo(@RequestHeader("Authorization") String authorization);

    /**
     * 健康检查
     */
    @GetMapping("/actuator/health")
    Map<String, Object> health();
} 