package com.signature.controller;

import com.signature.listener.SignatureVerificationEventListener.SignatureVerificationStats;
import com.signature.service.SignatureStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 签名验证统计控制器
 * 提供签名验证相关的统计信息查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/signature/stats")
public class SignatureStatisticsController {

    @Autowired
    private SignatureStatisticsService statisticsService;

    /**
     * 获取应用签名验证统计信息
     * 
     * @param appId 应用ID
     * @return 统计信息
     */
    @GetMapping("/app/{appId}")
    public ResponseEntity<SignatureVerificationStats> getAppStats(@PathVariable String appId) {
        try {
            SignatureVerificationStats stats = statisticsService.getAppStats(appId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get app stats for appId: {}", appId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取所有应用的统计信息
     * 
     * @return 所有应用的统计信息
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, SignatureVerificationStats>> getAllAppStats() {
        try {
            Map<String, SignatureVerificationStats> allStats = statisticsService.getAllAppStats();
            return ResponseEntity.ok(allStats);
        } catch (Exception e) {
            log.error("Failed to get all app stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 重置应用统计信息
     * 
     * @param appId 应用ID
     * @return 操作结果
     */
    @DeleteMapping("/app/{appId}")
    public ResponseEntity<String> resetAppStats(@PathVariable String appId) {
        try {
            statisticsService.resetAppStats(appId);
            return ResponseEntity.ok("App stats reset successfully for appId: " + appId);
        } catch (Exception e) {
            log.error("Failed to reset app stats for appId: {}", appId, e);
            return ResponseEntity.internalServerError().body("Failed to reset app stats");
        }
    }

    /**
     * 重置所有应用统计信息
     * 
     * @return 操作结果
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> resetAllStats() {
        try {
            statisticsService.resetAllStats();
            return ResponseEntity.ok("All app stats reset successfully");
        } catch (Exception e) {
            log.error("Failed to reset all app stats", e);
            return ResponseEntity.internalServerError().body("Failed to reset all app stats");
        }
    }
}
