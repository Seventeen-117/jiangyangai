package com.jiangyang.dubbo.api.signature.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

/**
 * 签名验证请求DTO
 * 
 * @author jiangyang
 * @version 1.0.0
 */
@Data
public class ValidationRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 应用ID
     */
    @NotBlank(message = "应用ID不能为空")
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
     * 待验证的签名
     */
    @NotBlank(message = "签名不能为空")
    private String signature;
    
    /**
     * 业务参数
     */
    private Map<String, String> params;
    
    /**
     * 是否验证时间戳
     */
    private Boolean validateTimestamp = true;
    
    /**
     * 是否验证随机数（防重放）
     */
    private Boolean validateNonce = true;
    
    /**
     * 自定义时间戳有效期（秒）
     */
    private Long timestampExpireSeconds;
    
    /**
     * 客户端IP（用于日志记录）
     */
    private String clientIp;
    
    /**
     * 用户代理（用于日志记录）
     */
    private String userAgent;
    
    /**
     * 请求路径（用于日志记录）
     */
    private String requestPath;
    
    /**
     * HTTP方法（用于日志记录）
     */
    private String httpMethod;
    
    /**
     * 请求来源
     */
    private String source;
    
    /**
     * 是否跳过缓存验证
     */
    private Boolean skipCache = false;
}
