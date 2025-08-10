package com.signature.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 验证请求模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRequest {

    /**
     * 请求路径
     */
    private String path;

    /**
     * 请求方法
     */
    private String method;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 签名
     */
    private String signature;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 随机数
     */
    private String nonce;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * JWT Token
     */
    private String jwtToken;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 资源
     */
    private String resource;

    /**
     * 操作
     */
    private String action;

    /**
     * 请求参数
     */
    private Map<String, String> parameters;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 请求来源
     */
    private String requestFrom;
}
