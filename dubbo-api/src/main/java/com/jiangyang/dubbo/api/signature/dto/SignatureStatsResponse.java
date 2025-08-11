package com.jiangyang.dubbo.api.signature.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 签名统计响应DTO
 * 
 * @author jiangyang
 * @version 1.0.0
 */
@Data
public class SignatureStatsResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 总请求数
     */
    private Long totalRequests;
    
    /**
     * 成功请求数
     */
    private Long successRequests;
    
    /**
     * 失败请求数
     */
    private Long failureRequests;
    
    /**
     * 成功率（百分比）
     */
    private Double successRate;
    
    /**
     * 平均响应时间（毫秒）
     */
    private Long averageResponseTime;
    
    /**
     * 最大响应时间（毫秒）
     */
    private Long maxResponseTime;
    
    /**
     * 最小响应时间（毫秒）
     */
    private Long minResponseTime;
    
    /**
     * 今日请求数
     */
    private Long todayRequests;
    
    /**
     * 本周请求数
     */
    private Long weekRequests;
    
    /**
     * 本月请求数
     */
    private Long monthRequests;
    
    /**
     * 统计时间戳
     */
    private Long statsTimestamp;
}
