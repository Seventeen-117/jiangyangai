package com.bgpay.bgai.controller;

import com.bgpay.bgai.dubbo.DubboClientService;
import com.jiangyang.dubbo.api.signature.dto.SignatureResponse;
import com.jiangyang.dubbo.api.signature.dto.SignatureStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Dubbo测试控制器
 * 用于测试与signature-service的Dubbo通信
 * 
 * @author jiangyang
 */
@Slf4j
@RestController
@RequestMapping("/api/test/dubbo")
@RequiredArgsConstructor
public class DubboTestController {
    
    private final DubboClientService dubboClientService;
    
    /**
     * 测试生成签名
     */
    @PostMapping("/signature/generate")
    public Map<String, Object> generateSignature(@RequestBody Map<String, Object> request) {
        try {
            String appId = (String) request.get("appId");
            String secret = (String) request.get("secret");
            @SuppressWarnings("unchecked")
            Map<String, String> params = (Map<String, String>) request.get("params");
            
            SignatureResponse response = dubboClientService.generateSignature(appId, secret, params);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Dubbo调用成功");
            result.put("data", response);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("生成签名测试成功: appId={}, signature={}", appId, response.getSignature());
            return result;
            
        } catch (Exception e) {
            log.error("测试生成签名失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Dubbo调用失败: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
    }
    
    /**
     * 测试验证签名
     */
    @PostMapping("/signature/verify")
    public Map<String, Object> verifySignature(@RequestBody Map<String, Object> request) {
        try {
            String appId = (String) request.get("appId");
            String timestamp = (String) request.get("timestamp");
            String nonce = (String) request.get("nonce");
            String signature = (String) request.get("signature");
            @SuppressWarnings("unchecked")
            Map<String, String> params = (Map<String, String>) request.get("params");
            
            boolean isValid = dubboClientService.verifySignature(appId, timestamp, nonce, signature, params);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Dubbo调用成功");
            result.put("valid", isValid);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("验证签名测试完成: appId={}, valid={}", appId, isValid);
            return result;
            
        } catch (Exception e) {
            log.error("测试验证签名失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Dubbo调用失败: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
    }
    
    /**
     * 测试快速验证签名
     */
    @PostMapping("/signature/verify-quick")
    public Map<String, Object> verifySignatureQuick(@RequestBody Map<String, Object> request) {
        try {
            String appId = (String) request.get("appId");
            String signature = (String) request.get("signature");
            @SuppressWarnings("unchecked")
            Map<String, String> params = (Map<String, String>) request.get("params");
            
            boolean isValid = dubboClientService.verifySignatureQuick(appId, signature, params);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Dubbo快速验证调用成功");
            result.put("valid", isValid);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("快速验证签名测试完成: appId={}, valid={}", appId, isValid);
            return result;
            
        } catch (Exception e) {
            log.error("测试快速验证签名失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Dubbo快速验证调用失败: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
    }
    
    /**
     * 测试异步验证签名
     */
    @PostMapping("/signature/verify-async")
    public CompletableFuture<Map<String, Object>> verifySignatureAsync(@RequestBody Map<String, Object> request) {
        try {
            String appId = (String) request.get("appId");
            String timestamp = (String) request.get("timestamp");
            String nonce = (String) request.get("nonce");
            String signature = (String) request.get("signature");
            @SuppressWarnings("unchecked")
            Map<String, String> params = (Map<String, String>) request.get("params");
            
            return dubboClientService.verifySignatureAsync(appId, timestamp, nonce, signature, params)
                .thenApply(isValid -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Dubbo异步调用成功");
                    result.put("valid", isValid);
                    result.put("timestamp", System.currentTimeMillis());
                    
                    log.info("异步验证签名测试完成: appId={}, valid={}", appId, isValid);
                    return result;
                })
                .exceptionally(throwable -> {
                    log.error("测试异步验证签名失败", throwable);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("message", "Dubbo异步调用失败: " + throwable.getMessage());
                    result.put("timestamp", System.currentTimeMillis());
                    return result;
                });
                
        } catch (Exception e) {
            log.error("测试异步验证签名失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Dubbo异步调用失败: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return CompletableFuture.completedFuture(result);
        }
    }
    
    /**
     * 测试生成示例签名
     */
    @GetMapping("/signature/example")
    public Map<String, Object> generateExampleSignature(@RequestParam String appId, 
                                                       @RequestParam String secret) {
        try {
            SignatureResponse response = dubboClientService.generateExampleSignature(appId, secret);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Dubbo生成示例签名调用成功");
            result.put("data", response);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("生成示例签名测试成功: appId={}", appId);
            return result;
            
        } catch (Exception e) {
            log.error("测试生成示例签名失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Dubbo生成示例签名调用失败: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
    }
    
    /**
     * 测试获取签名统计
     */
    @GetMapping("/signature/stats")
    public Map<String, Object> getSignatureStats(@RequestParam String appId) {
        try {
            SignatureStatsResponse stats = dubboClientService.getSignatureStats(appId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Dubbo获取签名统计调用成功");
            result.put("data", stats);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("获取签名统计测试成功: appId={}, totalRequests={}", appId, stats.getTotalRequests());
            return result;
            
        } catch (Exception e) {
            log.error("测试获取签名统计失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Dubbo获取签名统计调用失败: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("message", "Dubbo客户端服务正常");
        result.put("timestamp", System.currentTimeMillis());
        
        // 简单检查Dubbo连接状态
        try {
            // 尝试调用一个轻量级方法
            dubboClientService.generateExampleSignature("health-check", "test-secret");
            result.put("dubboStatus", "CONNECTED");
        } catch (Exception e) {
            result.put("dubboStatus", "DISCONNECTED");
            result.put("dubboError", e.getMessage());
        }
        
        return result;
    }
}
