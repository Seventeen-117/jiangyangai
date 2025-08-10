package com.signature.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import com.signature.model.SignatureVerificationRequest;
import com.signature.listener.SignatureVerificationEventListener.SignatureVerificationStats;

/**
 * 签名验证服务接口
 * 定义HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击的方法
 */
public interface SignatureVerificationService {

    boolean validateTimestamp(String timestamp, long expireSeconds);
    boolean validateNonce(String nonce, long cacheExpireSeconds);
    void saveNonce(String nonce, long expireSeconds);
    boolean verifySignature(Map<String, String> params, String sign, String appId);
    String generateNonce();
    String generateTimestamp();

    // ========== 异步验证方法 ==========
    CompletableFuture<Boolean> verifySignatureAsync(Map<String, String> params, String sign, String appId);
    CompletableFuture<Boolean> verifySignatureFast(Map<String, String> params, String sign, String appId);
    boolean verifySignatureQuick(Map<String, String> params, String appId);
    CompletableFuture<List<Boolean>> verifySignatureBatch(List<SignatureVerificationRequest> verificationRequests);
    CompletableFuture<Void> saveNonceAsync(String nonce, long expireSeconds);
    CompletableFuture<Boolean> validateNonceAsync(String nonce, long cacheExpireSeconds);
    CompletableFuture<Boolean> validateTimestampAsync(String timestamp, long expireSeconds);

    // ========== 统计方法 ==========
    SignatureVerificationStats getAppStats(String appId);
} 