package com.jiangyang.messages.controller;

import com.jiangyang.messages.config.ServiceCallbackConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务回调地址配置管理控制器
 * 用于查看和管理各个服务的回调地址配置
 */
@Slf4j
@RestController
@RequestMapping("/api/config/service-callback")
public class ServiceCallbackConfigController {

    @Autowired
    private ServiceCallbackConfig serviceCallbackConfig;

    /**
     * 获取所有已配置的服务回调地址
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllCallbackUrls() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // 获取所有已配置的服务
            java.util.Set<String> configuredServices = serviceCallbackConfig.getConfiguredServices();
            
            Map<String, String> callbackUrls = new HashMap<>();
            for (String serviceName : configuredServices) {
                String callbackUrl = serviceCallbackConfig.getCallbackUrl(serviceName);
                if (callbackUrl != null) {
                    callbackUrls.put(serviceName, callbackUrl);
                }
            }
            
            response.put("success", true);
            response.put("configuredServices", configuredServices);
            response.put("callbackUrls", callbackUrls);
            response.put("totalCount", callbackUrls.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取服务回调地址配置失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取配置失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取特定服务的回调地址
     */
    @GetMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> getServiceCallbackUrl(@PathVariable String serviceName) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            String callbackUrl = serviceCallbackConfig.getCallbackUrl(serviceName);
            boolean hasConfig = serviceCallbackConfig.hasCallbackUrl(serviceName);
            
            response.put("success", true);
            response.put("serviceName", serviceName);
            response.put("hasConfig", hasConfig);
            response.put("callbackUrl", callbackUrl);
            response.put("timestamp", System.currentTimeMillis());
            
            if (hasConfig) {
                response.put("message", "服务已配置回调地址");
            } else {
                response.put("message", "服务未配置回调地址");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取服务 {} 的回调地址配置失败: {}", serviceName, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取配置失败: " + e.getMessage());
            errorResponse.put("serviceName", serviceName);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 检查服务回调地址配置状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConfigStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            java.util.Set<String> configuredServices = serviceCallbackConfig.getConfiguredServices();
            
            response.put("success", true);
            response.put("status", "CONFIGURED");
            response.put("configuredServices", configuredServices);
            response.put("totalCount", configuredServices.size());
            response.put("timestamp", System.currentTimeMillis());
            
            if (configuredServices.isEmpty()) {
                response.put("status", "NOT_CONFIGURED");
                response.put("message", "未配置任何服务的回调地址");
            } else {
                response.put("message", String.format("已配置 %d 个服务的回调地址", configuredServices.size()));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取配置状态失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "获取配置状态失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "ServiceCallbackConfig");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "服务回调地址配置服务运行正常");
        
        return ResponseEntity.ok(health);
    }
}
