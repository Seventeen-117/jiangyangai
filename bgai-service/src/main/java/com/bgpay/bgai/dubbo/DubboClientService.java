package com.bgpay.bgai.dubbo;

import com.jiangyang.dubbo.api.signature.SignatureService;
import com.jiangyang.dubbo.api.signature.dto.*;
import com.jiangyang.dubbo.api.signature.enums.SignatureType;
import com.jiangyang.dubbo.api.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Dubbo客户端服务
 * 封装对signature-service的Dubbo调用
 * 
 * @author jiangyang
 */
@Slf4j
@Service
public class DubboClientService {
    
    @DubboReference(
        version = "1.0.0",
        group = "signature",
        timeout = 5000,
        retries = 2,
        loadbalance = "roundrobin",
        cluster = "failover",
        check = false
    )
    private SignatureService signatureService;
    
    /**
     * 生成签名
     * 
     * @param appId 应用ID
     * @param secret 应用密钥
     * @param params 业务参数
     * @return 签名响应
     */
    public SignatureResponse generateSignature(String appId, String secret, Map<String, String> params) {
        try {
            log.info("Dubbo客户端 - 调用生成签名: appId={}", appId);
            
            SignatureRequest request = new SignatureRequest();
            request.setAppId(appId);
            request.setSecret(secret);
            request.setParams(params);
            request.setSignatureType(SignatureType.HMAC_SHA256);
            request.setIncludeTimestamp(true);
            request.setIncludeNonce(true);
            
            Result<SignatureResponse> result = signatureService.generateSignature(request);
            
            if (result.isSuccess()) {
                log.info("Dubbo客户端 - 生成签名成功: appId={}", appId);
                return result.getData();
            } else {
                log.error("Dubbo客户端 - 生成签名失败: appId={}, error={}", appId, result.getMessage());
                throw new RuntimeException("生成签名失败: " + result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Dubbo客户端异常 - 生成签名失败: appId={}", appId, e);
            throw new RuntimeException("Dubbo调用生成签名失败", e);
        }
    }
    
    /**
     * 验证签名
     * 
     * @param appId 应用ID
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param signature 签名
     * @param params 业务参数
     * @return 验证结果
     */
    public boolean verifySignature(String appId, String timestamp, String nonce, 
                                  String signature, Map<String, String> params) {
        try {
            log.info("Dubbo客户端 - 调用验证签名: appId={}", appId);
            
            ValidationRequest request = new ValidationRequest();
            request.setAppId(appId);
            request.setTimestamp(timestamp);
            request.setNonce(nonce);
            request.setSignature(signature);
            request.setParams(params);
            request.setValidateTimestamp(true);
            request.setValidateNonce(true);
            
            Result<Boolean> result = signatureService.verifySignature(request);
            
            if (result.isSuccess()) {
                log.info("Dubbo客户端 - 验证签名完成: appId={}, valid={}", appId, result.getData());
                return result.getData();
            } else {
                log.error("Dubbo客户端 - 验证签名失败: appId={}, error={}", appId, result.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            log.error("Dubbo客户端异常 - 验证签名失败: appId={}", appId, e);
            return false;
        }
    }
    
    /**
     * 快速验证签名（跳过时间戳和nonce检查）
     * 
     * @param appId 应用ID
     * @param signature 签名
     * @param params 业务参数
     * @return 验证结果
     */
    public boolean verifySignatureQuick(String appId, String signature, Map<String, String> params) {
        try {
            log.info("Dubbo客户端 - 调用快速验证签名: appId={}", appId);
            
            ValidationRequest request = new ValidationRequest();
            request.setAppId(appId);
            request.setSignature(signature);
            request.setParams(params);
            request.setValidateTimestamp(false);
            request.setValidateNonce(false);
            
            Result<Boolean> result = signatureService.verifySignatureQuick(request);
            
            if (result.isSuccess()) {
                log.info("Dubbo客户端 - 快速验证签名完成: appId={}, valid={}", appId, result.getData());
                return result.getData();
            } else {
                log.error("Dubbo客户端 - 快速验证签名失败: appId={}, error={}", appId, result.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            log.error("Dubbo客户端异常 - 快速验证签名失败: appId={}", appId, e);
            return false;
        }
    }
    
    /**
     * 异步验证签名
     * 
     * @param appId 应用ID
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param signature 签名
     * @param params 业务参数
     * @return 异步验证结果
     */
    public CompletableFuture<Boolean> verifySignatureAsync(String appId, String timestamp, 
                                                          String nonce, String signature, 
                                                          Map<String, String> params) {
        try {
            log.info("Dubbo客户端 - 异步调用验证签名: appId={}", appId);
            
            ValidationRequest request = new ValidationRequest();
            request.setAppId(appId);
            request.setTimestamp(timestamp);
            request.setNonce(nonce);
            request.setSignature(signature);
            request.setParams(params);
            
            return signatureService.verifySignatureAsync(request)
                .thenApply(result -> {
                    if (result.isSuccess()) {
                        log.info("Dubbo客户端 - 异步验证签名完成: appId={}, valid={}", appId, result.getData());
                        return result.getData();
                    } else {
                        log.error("Dubbo客户端 - 异步验证签名失败: appId={}, error={}", appId, result.getMessage());
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Dubbo客户端异常 - 异步验证签名失败: appId={}", appId, throwable);
                    return false;
                });
                
        } catch (Exception e) {
            log.error("Dubbo客户端异常 - 异步验证签名调用失败: appId={}", appId, e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 生成示例签名
     * 
     * @param appId 应用ID
     * @param secret 应用密钥
     * @return 示例签名响应
     */
    public SignatureResponse generateExampleSignature(String appId, String secret) {
        try {
            log.info("Dubbo客户端 - 调用生成示例签名: appId={}", appId);
            
            Result<SignatureResponse> result = signatureService.generateExampleSignature(appId, secret);
            
            if (result.isSuccess()) {
                log.info("Dubbo客户端 - 生成示例签名成功: appId={}", appId);
                return result.getData();
            } else {
                log.error("Dubbo客户端 - 生成示例签名失败: appId={}, error={}", appId, result.getMessage());
                throw new RuntimeException("生成示例签名失败: " + result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Dubbo客户端异常 - 生成示例签名失败: appId={}", appId, e);
            throw new RuntimeException("Dubbo调用生成示例签名失败", e);
        }
    }
    
    /**
     * 获取签名统计信息
     * 
     * @param appId 应用ID
     * @return 统计信息
     */
    public SignatureStatsResponse getSignatureStats(String appId) {
        try {
            log.info("Dubbo客户端 - 调用获取签名统计: appId={}", appId);
            
            Result<SignatureStatsResponse> result = signatureService.getSignatureStats(appId);
            
            if (result.isSuccess()) {
                log.info("Dubbo客户端 - 获取签名统计成功: appId={}", appId);
                return result.getData();
            } else {
                log.error("Dubbo客户端 - 获取签名统计失败: appId={}, error={}", appId, result.getMessage());
                throw new RuntimeException("获取签名统计失败: " + result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Dubbo客户端异常 - 获取签名统计失败: appId={}", appId, e);
            throw new RuntimeException("Dubbo调用获取签名统计失败", e);
        }
    }
}
