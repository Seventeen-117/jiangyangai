package com.jiangyang.dubbo.api.signature.dto;

import com.jiangyang.dubbo.api.signature.enums.SignatureType;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

/**
 * 签名生成请求DTO
 * 
 * @author jiangyang
 * @version 1.0.0
 */
@Data
public class SignatureRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 应用ID
     */
    @NotBlank(message = "应用ID不能为空")
    private String appId;
    
    /**
     * 应用密钥
     */
    @NotBlank(message = "应用密钥不能为空")
    private String secret;
    
    /**
     * 业务参数
     */
    private Map<String, String> params;
    
    /**
     * 签名类型
     */
    @NotNull(message = "签名类型不能为空")
    private SignatureType signatureType = SignatureType.HMAC_SHA256;
    
    /**
     * 是否包含时间戳
     */
    private Boolean includeTimestamp = true;
    
    /**
     * 是否包含随机数
     */
    private Boolean includeNonce = true;
    
    /**
     * 自定义时间戳（可选）
     */
    private String customTimestamp;
    
    /**
     * 自定义随机数（可选）
     */
    private String customNonce;
    
    /**
     * 签名有效期（秒，默认300秒）
     */
    private Long expireSeconds = 300L;
    
    /**
     * 客户端IP（用于日志记录）
     */
    private String clientIp;
    
    /**
     * 用户代理（用于日志记录）
     */
    private String userAgent;
    
    /**
     * 请求来源
     */
    private String source;
}
