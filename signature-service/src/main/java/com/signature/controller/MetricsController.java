package com.signature.controller;

import com.signature.service.ValidationMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 监控控制器
 * 提供验证性能监控接口
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @Autowired
    private ValidationMetricsService metricsService;

    /**
     * 获取所有验证统计信息
     */
    @GetMapping("/validation")
    public ResponseEntity<Map<String, Map<String, Object>>> getValidationMetrics() {
        log.info("Getting validation metrics");
        
        Map<String, Map<String, Object>> metrics = metricsService.getAllValidationStats();
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * 获取特定验证类型的统计信息
     */
    @GetMapping("/validation/{type}")
    public ResponseEntity<Map<String, Object>> getValidationMetricsByType(@PathVariable String type) {
        log.info("Getting validation metrics for type: {}", type);
        
        Map<String, Object> metrics = metricsService.getValidationStats(type);
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * 重置统计信息
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        log.info("Resetting validation metrics");
        
        metricsService.resetStats();
        
        Map<String, Object> result = Map.of(
            "success", true,
            "message", "Validation metrics reset successfully",
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取健康检查信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        log.debug("Getting health check");
        
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "signature-service",
            "timestamp", System.currentTimeMillis(),
            "version", "1.0.0"
        );
        
        return ResponseEntity.ok(health);
    }
}
