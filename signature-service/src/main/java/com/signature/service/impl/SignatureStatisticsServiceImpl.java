package com.signature.service.impl;

import com.signature.listener.SignatureVerificationEventListener;
import com.signature.listener.SignatureVerificationEventListener.SignatureVerificationStats;
import com.signature.service.SignatureStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 签名验证统计服务实现类
 * 提供签名验证相关的统计信息查询功能
 */
@Slf4j
@Service
public class SignatureStatisticsServiceImpl implements SignatureStatisticsService {

    @Autowired
    private SignatureVerificationEventListener eventListener;

    // 缓存所有应用的统计信息，避免频繁计算
    private final Map<String, SignatureVerificationStats> statsCache = new ConcurrentHashMap<>();

    @Override
    public SignatureVerificationStats getAppStats(String appId) {
        try {
            // 从事件监听器获取统计信息
            SignatureVerificationStats stats = eventListener.getAppStats(appId);
            
            // 缓存统计信息
            statsCache.put(appId, stats);
            
            return stats;
        } catch (Exception e) {
            log.warn("Failed to get app stats for appId: {}, returning empty stats", appId, e);
            // 如果获取失败，返回空的统计信息
            return SignatureVerificationStats.builder()
                    .appId(appId)
                    .successCount(0)
                    .failureCount(0)
                    .replayAttackCount(0)
                    .totalCount(0)
                    .failureRate(0.0)
                    .averageVerificationTime(0)
                    .build();
        }
    }

    @Override
    public Map<String, SignatureVerificationStats> getAllAppStats() {
        try {
            // 这里可以根据需要实现获取所有应用统计信息的逻辑
            // 目前返回缓存中的统计信息
            return new HashMap<>(statsCache);
        } catch (Exception e) {
            log.error("Failed to get all app stats", e);
            return new HashMap<>();
        }
    }

    @Override
    public void resetAppStats(String appId) {
        try {
            // 清除缓存中的统计信息
            statsCache.remove(appId);
            log.info("Reset app stats for appId: {}", appId);
        } catch (Exception e) {
            log.error("Failed to reset app stats for appId: {}", appId, e);
        }
    }

    @Override
    public void resetAllStats() {
        try {
            // 清除所有缓存的统计信息
            statsCache.clear();
            log.info("Reset all app stats");
        } catch (Exception e) {
            log.error("Failed to reset all app stats", e);
        }
    }
}
