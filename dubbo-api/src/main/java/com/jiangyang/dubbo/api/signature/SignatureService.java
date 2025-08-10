package com.jiangyang.dubbo.api.signature;

import com.jiangyang.dubbo.api.signature.dto.*;
import com.jiangyang.dubbo.api.common.Result;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 签名服务 Dubbo 接口
 * 
 * @author jiangyang
 * @version 1.0.0
 */
public interface SignatureService {
    
    /**
     * 生成签名
     * 
     * @param request 签名请求
     * @return 签名响应
     */
    Result<SignatureResponse> generateSignature(SignatureRequest request);
    
    /**
     * 验证签名
     * 
     * @param request 验证请求
     * @return 验证结果
     */
    Result<Boolean> verifySignature(ValidationRequest request);
    
    /**
     * 批量验证签名
     * 
     * @param requests 批量验证请求
     * @return 批量验证结果
     */
    Result<List<Boolean>> batchVerifySignature(List<ValidationRequest> requests);
    
    /**
     * 异步验证签名
     * 
     * @param request 验证请求
     * @return 异步验证结果
     */
    CompletableFuture<Result<Boolean>> verifySignatureAsync(ValidationRequest request);
    
    /**
     * 快速验证签名（跳过时间戳和nonce检查）
     * 
     * @param request 验证请求
     * @return 验证结果
     */
    Result<Boolean> verifySignatureQuick(ValidationRequest request);
    
    /**
     * 生成示例签名参数
     * 
     * @param appId 应用ID
     * @param secret 应用密钥
     * @return 示例签名参数
     */
    Result<SignatureResponse> generateExampleSignature(String appId, String secret);
    
    /**
     * 获取签名统计信息
     * 
     * @param appId 应用ID
     * @return 统计信息
     */
    Result<SignatureStatsResponse> getSignatureStats(String appId);
}
