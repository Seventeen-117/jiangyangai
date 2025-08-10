package com.signature.service;

import com.signature.listener.SignatureVerificationEventListener.SignatureVerificationStats;

/**
 * 签名验证统计服务接口
 * 提供签名验证相关的统计信息查询功能
 */
public interface SignatureStatisticsService {

    /**
     * 获取应用签名验证统计信息
     * 
     * @param appId 应用ID
     * @return 统计信息
     */
    SignatureVerificationStats getAppStats(String appId);

    /**
     * 获取所有应用的统计信息
     * 
     * @return 所有应用的统计信息映射
     */
    java.util.Map<String, SignatureVerificationStats> getAllAppStats();

    /**
     * 重置应用统计信息
     * 
     * @param appId 应用ID
     */
    void resetAppStats(String appId);

    /**
     * 重置所有应用统计信息
     */
    void resetAllStats();
}
