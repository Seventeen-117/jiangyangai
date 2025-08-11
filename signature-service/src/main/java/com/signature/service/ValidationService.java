package com.signature.service;

import com.signature.model.ValidationRequest;
import com.signature.model.ValidationResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 统一验证服务接口
 * 负责API Key验证、签名验证、权限验证等
 */
public interface ValidationService {

    /**
     * 验证请求
     *
     * @param request 验证请求
     * @return 验证结果
     */
    ValidationResult validateRequest(ValidationRequest request);

    /**
     * 异步验证请求
     *
     * @param request 验证请求
     * @return 异步验证结果
     */
    CompletableFuture<ValidationResult> validateRequestAsync(ValidationRequest request);

    /**
     * 批量验证请求
     *
     * @param requests 验证请求列表
     * @return 验证结果列表
     */
    List<ValidationResult> validateRequests(List<ValidationRequest> requests);

    /**
     * 验证API Key
     *
     * @param apiKey API Key
     * @return 验证结果
     */
    ValidationResult validateApiKey(String apiKey);

    /**
     * 验证签名
     *
     * @param request 包含签名信息的请求
     * @return 验证结果
     */
    ValidationResult validateSignature(ValidationRequest request);

    /**
     * 验证权限
     *
     * @param userId 用户ID
     * @param resource 资源
     * @param action 操作
     * @return 验证结果
     */
    ValidationResult validatePermission(String userId, String resource, String action);

    /**
     * 验证JWT Token
     *
     * @param token JWT Token
     * @return 验证结果
     */
    ValidationResult validateJwtToken(String token);

    /**
     * 刷新验证缓存
     */
    void refreshValidationCache();
}
