package com.jiangyang.dubbo.api.signature.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 签名生成响应DTO
 * 
 * @author jiangyang
 * @version 1.0.0
 */
@Data
public class SignatureResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 时间戳
     */
    private String timestamp;
    
    /**
     * 随机数
     */
    private String nonce;
    
    /**
     * 生成的签名
     */
    private String signature;
    
    /**
     * 完整的签名参数（包含业务参数）
     */
    private Map<String, String> allParams;
    
    /**
     * 签名有效期（秒）
     */
    private Long expireSeconds;
    
    /**
     * 签名过期时间戳
     */
    private Long expireTimestamp;
    
    /**
     * 签名算法
     */
    private String algorithm;
    
    /**
     * 参数排序字符串（用于调试）
     */
    private String sortedParams;
    
    /**
     * 签名生成时间
     */
    private Long generatedAt;
    
    /**
     * 是否成功
     */
    private Boolean success = true;
    
    /**
     * 错误消息（如果有）
     */
    private String errorMessage;
}
