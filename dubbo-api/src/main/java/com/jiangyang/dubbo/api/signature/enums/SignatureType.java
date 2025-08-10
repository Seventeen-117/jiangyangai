package com.jiangyang.dubbo.api.signature.enums;

/**
 * 签名类型枚举
 * 
 * @author jiangyang
 * @version 1.0.0
 */
public enum SignatureType {
    
    /**
     * HMAC-SHA256签名
     */
    HMAC_SHA256("HMAC-SHA256", "HmacSHA256"),
    
    /**
     * HMAC-SHA1签名
     */
    HMAC_SHA1("HMAC-SHA1", "HmacSHA1"),
    
    /**
     * MD5签名
     */
    MD5("MD5", "MD5");
    
    /**
     * 显示名称
     */
    private final String displayName;
    
    /**
     * 算法名称
     */
    private final String algorithm;
    
    SignatureType(String displayName, String algorithm) {
        this.displayName = displayName;
        this.algorithm = algorithm;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    /**
     * 根据显示名称获取枚举
     */
    public static SignatureType fromDisplayName(String displayName) {
        for (SignatureType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的签名类型: " + displayName);
    }
    
    /**
     * 根据算法名称获取枚举
     */
    public static SignatureType fromAlgorithm(String algorithm) {
        for (SignatureType type : values()) {
            if (type.algorithm.equals(algorithm)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的签名算法: " + algorithm);
    }
}
