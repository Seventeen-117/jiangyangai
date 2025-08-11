package com.signature.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * 验证指标服务
 * 用于监控验证成功率和响应时间
 */
@Slf4j
@Service
public class ValidationMetricsService {

    private final Map<String, AtomicLong> successCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failureCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> totalResponseTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();

    /**
     * 记录验证成功
     */
    public void recordSuccess(String validationType, long responseTime) {
        successCounts.computeIfAbsent(validationType, k -> new AtomicLong(0)).incrementAndGet();
        totalResponseTimes.computeIfAbsent(validationType, k -> new AtomicLong(0)).addAndGet(responseTime);
        requestCounts.computeIfAbsent(validationType, k -> new AtomicLong(0)).incrementAndGet();
        
        log.debug("Validation success recorded: type={}, responseTime={}ms", validationType, responseTime);
    }

    /**
     * 记录验证失败
     */
    public void recordFailure(String validationType, long responseTime) {
        failureCounts.computeIfAbsent(validationType, k -> new AtomicLong(0)).incrementAndGet();
        totalResponseTimes.computeIfAbsent(validationType, k -> new AtomicLong(0)).addAndGet(responseTime);
        requestCounts.computeIfAbsent(validationType, k -> new AtomicLong(0)).incrementAndGet();
        
        log.debug("Validation failure recorded: type={}, responseTime={}ms", validationType, responseTime);
    }

    /**
     * 获取验证成功率
     */
    public double getSuccessRate(String validationType) {
        AtomicLong successCount = successCounts.get(validationType);
        AtomicLong failureCount = failureCounts.get(validationType);
        
        if (successCount == null && failureCount == null) {
            return 0.0;
        }
        
        long total = (successCount != null ? successCount.get() : 0) + 
                    (failureCount != null ? failureCount.get() : 0);
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) (successCount != null ? successCount.get() : 0) / total * 100;
    }

    /**
     * 获取平均响应时间
     */
    public double getAverageResponseTime(String validationType) {
        AtomicLong totalTime = totalResponseTimes.get(validationType);
        AtomicLong totalCount = requestCounts.get(validationType);
        
        if (totalTime == null || totalCount == null || totalCount.get() == 0) {
            return 0.0;
        }
        
        return (double) totalTime.get() / totalCount.get();
    }

    /**
     * 获取验证统计信息
     */
    public Map<String, Object> getValidationStats(String validationType) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        AtomicLong successCount = successCounts.get(validationType);
        AtomicLong failureCount = failureCounts.get(validationType);
        AtomicLong totalCount = requestCounts.get(validationType);
        
        stats.put("validationType", validationType);
        stats.put("successCount", successCount != null ? successCount.get() : 0);
        stats.put("failureCount", failureCount != null ? failureCount.get() : 0);
        stats.put("totalCount", totalCount != null ? totalCount.get() : 0);
        stats.put("successRate", getSuccessRate(validationType));
        stats.put("averageResponseTime", getAverageResponseTime(validationType));
        
        return stats;
    }

    /**
     * 获取所有验证类型的统计信息
     */
    public Map<String, Map<String, Object>> getAllValidationStats() {
        Map<String, Map<String, Object>> allStats = new ConcurrentHashMap<>();
        
        // 获取所有验证类型的统计
        String[] validationTypes = {"api_key", "signature", "permission", "jwt", "overall"};
        
        for (String type : validationTypes) {
            allStats.put(type, getValidationStats(type));
        }
        
        return allStats;
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        successCounts.clear();
        failureCounts.clear();
        totalResponseTimes.clear();
        requestCounts.clear();
        
        log.info("Validation metrics reset");
    }
}
