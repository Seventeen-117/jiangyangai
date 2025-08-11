package com.signature.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.signature.entity.AppSecret;
import com.signature.event.SignatureVerificationEvent;
import com.signature.listener.SignatureVerificationEventListener.SignatureVerificationStats;
import com.signature.service.AppSecretService;
import com.signature.model.SignatureVerificationRequest;
import com.signature.service.SignatureStatisticsService;
import com.signature.service.SignatureVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 签名验证服务实现类
 * 实现HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击
 * 支持异步验证、批量处理和事件驱动架构
 */
@Slf4j
@Service
public class SignatureVerificationServiceImpl implements SignatureVerificationService, ApplicationContextAware {

    private final StringRedisTemplate redisTemplate;
    private final AppSecretService appSecretService;
    
    @Autowired
    private ApplicationContext applicationContext;

    // Redis key前缀
    private static final String NONCE_CACHE_PREFIX = "signature:nonce:";
    private static final String APP_SECRET_PREFIX = "signature:app_secret:";

    // 异步处理线程池
    private final ExecutorService asyncExecutor;
    
    // 批量处理线程池
    private final ExecutorService batchExecutor;

    // Manual constructor for final fields
    public SignatureVerificationServiceImpl(StringRedisTemplate redisTemplate, AppSecretService appSecretService) {
        this.redisTemplate = redisTemplate;
        this.appSecretService = appSecretService;
        // 初始化线程池
        this.asyncExecutor = Executors.newFixedThreadPool(10);
        this.batchExecutor = Executors.newFixedThreadPool(5);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean validateTimestamp(String timestamp, long expireSeconds) {
        try {
            long timestampLong = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long diff = Math.abs(currentTime - timestampLong);
            return diff <= expireSeconds * 1000;
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }

    @Override
    public boolean validateNonce(String nonce, long cacheExpireSeconds) {
        String key = NONCE_CACHE_PREFIX + nonce;
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", 
            java.time.Duration.ofSeconds(cacheExpireSeconds));
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void saveNonce(String nonce, long expireSeconds) {
        String key = NONCE_CACHE_PREFIX + nonce;
        redisTemplate.opsForValue().set(key, "1", 
            java.time.Duration.ofSeconds(expireSeconds));
    }

    @Override
    public boolean verifySignature(Map<String, String> params, String sign, String appId) {
        try {
            // 获取应用密钥
            String appSecret = getAppSecret(appId);
            if (StrUtil.isBlank(appSecret)) {
                log.warn("App secret not found for appId: {}", appId);
                return false;
            }

            // 构造待签名字符串
            String stringToSign = buildStringToSign(params);
            
            // 计算签名
            String calculatedSign = calculateSignature(stringToSign, appSecret);
            
            // 比较签名
            boolean isValid = calculatedSign.equals(sign);
            
            if (!isValid) {
                log.warn("Signature verification failed for appId: {}, expected: {}, actual: {}", 
                    appId, calculatedSign, sign);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error during signature verification for appId: {}", appId, e);
            return false;
        }
    }

    @Override
    public String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String generateTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    // ========== 异步验证方法实现 ==========
    @Override
    public CompletableFuture<Boolean> verifySignatureAsync(Map<String, String> params, String sign, String appId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            boolean success = false;
            String errorMessage = null;
            
            try {
                // 获取应用密钥
                String appSecret = getAppSecret(appId);
                if (StrUtil.isBlank(appSecret)) {
                    errorMessage = "App secret not found";
                    log.warn("App secret not found for appId: {}", appId);
                    return false;
                }

                // 构造待签名字符串
                String stringToSign = buildStringToSign(params);
                
                // 计算签名
                String calculatedSign = calculateSignature(stringToSign, appSecret);
                
                // 比较签名
                success = StrUtil.equalsIgnoreCase(sign, calculatedSign);
                
                if (!success) {
                    errorMessage = "Signature verification failed";
                    log.warn("Signature verification failed for appId: {}, expected: {}, actual: {}", 
                            appId, calculatedSign, sign);
                }
                
                return success;
            } catch (Exception e) {
                errorMessage = "System error during signature verification";
                log.error("Error during signature verification for appId: {}", appId, e);
                return false;
            } finally {
                // 发布验证事件
                publishVerificationEvent(appId, "async", "unknown", params, success, errorMessage, 
                        System.currentTimeMillis() - startTime);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Boolean> verifySignatureFast(Map<String, String> params, String sign, String appId) {
        // 快速基础验证
        if (!verifySignatureQuick(params, appId)) {
            return CompletableFuture.completedFuture(false);
        }
        
        // 异步详细验证
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            boolean result = verifySignature(params, sign, appId);
            long verificationTime = System.currentTimeMillis() - startTime;
            
            publishVerificationEvent(appId, "hybrid", "unknown", params, result, 
                result ? "Success" : "Failed", verificationTime);
            
            return result;
        }, asyncExecutor);
    }

    @Override
    public boolean verifySignatureQuick(Map<String, String> params, String appId) {
        // 快速验证：只检查基本参数和时间戳
        String timestamp = params.get("timestamp");
        if (StrUtil.isBlank(timestamp)) {
            return false;
        }
        
        // 简化的时间戳验证（5分钟）
        return validateTimestamp(timestamp, 300);
    }

    @Override
    public CompletableFuture<List<Boolean>> verifySignatureBatch(List<SignatureVerificationRequest> verificationRequests) {
        return CompletableFuture.supplyAsync(() -> {
            // 按客户端ID分组，便于批量获取密钥和nonce验证
            Map<String, List<SignatureVerificationRequest>> groupedRequests = verificationRequests.stream()
                    .collect(Collectors.groupingBy(SignatureVerificationRequest::getAppId));
            
            List<Boolean> results = new ArrayList<>();
            
            // 并行处理每个分组
            groupedRequests.forEach((appId, requests) -> {
                // 批量预加载客户端密钥
                String appSecret = getAppSecret(appId);
                
                // 利用Java 8 Stream的并行流特性，充分利用多核CPU
                List<Boolean> groupResults = requests.parallelStream()
                        .map(request -> {
                            try {
                                return verifySignature(request.getParams(), request.getSign(), appId);
                            } catch (Exception e) {
                                log.error("Error during batch verification for request: {}", request.getRequestId(), e);
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                
                results.addAll(groupResults);
            });
            
            return results;
        }, batchExecutor);
    }

    @Override
    public CompletableFuture<Void> saveNonceAsync(String nonce, long expireSeconds) {
        return CompletableFuture.runAsync(() -> {
            saveNonce(nonce, expireSeconds);
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Boolean> validateNonceAsync(String nonce, long cacheExpireSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            return validateNonce(nonce, cacheExpireSeconds);
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Boolean> validateTimestampAsync(String timestamp, long expireSeconds) {
        return CompletableFuture.supplyAsync(() -> validateTimestamp(timestamp, expireSeconds));
    }

    // ========== 统计方法实现 ==========

    @Override
    public SignatureVerificationStats getAppStats(String appId) {
        try {
            // 通过ApplicationContext获取SignatureStatisticsService实例
            // 这样可以避免循环依赖问题，并使用专门的统计服务
            SignatureStatisticsService statisticsService = applicationContext.getBean(SignatureStatisticsService.class);
            return statisticsService.getAppStats(appId);
        } catch (Exception e) {
            log.warn("Failed to get app stats for appId: {}, returning empty stats", appId, e);
            // 如果获取失败，返回空的统计信息
            return SignatureVerificationStats.builder()
                    .appId(appId)
                    .successCount(0)
                    .failureCount(0)
                    .replayAttackCount(0)
                    .totalCount(0)
                    .failureRate(0.0)
                    .averageVerificationTime(0)
                    .build();
        }
    }

    // ========== 私有辅助方法 ==========
    private String getAppSecret(String appId) {
        // 先从Redis缓存获取
        String cacheKey = APP_SECRET_PREFIX + appId;
        String cachedSecret = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedSecret)) {
            return cachedSecret;
        }

        // 从数据库查询
        AppSecret appSecret = appSecretService.findEnabledByAppId(appId);
        if (appSecret != null) {
            // 缓存到Redis（1小时）
            redisTemplate.opsForValue().set(cacheKey, appSecret.getAppSecret(), 
                java.time.Duration.ofHours(1));
            return appSecret.getAppSecret();
        }

        return null;
    }

    private String buildStringToSign(Map<String, String> params) {
        // 创建排序的Map
        TreeMap<String, String> sortedParams = new TreeMap<>();
        
        // 过滤掉sign参数，只保留其他参数
        params.forEach((key, value) -> {
            if (!"sign".equals(key) && value != null && !value.trim().isEmpty()) {
                sortedParams.put(key, value);
            }
        });
        
        // 构造key=value&格式的字符串
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return sb.toString();
    }

    private String calculateSignature(String stringToSign, String secret) {
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        byte[] digest = hMac.digest(stringToSign.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void publishVerificationEvent(String appId, String path, String clientIp,
                                          Map<String, String> params, boolean success,
                                          String errorMessage, long verificationTime) {
        try {
            SignatureVerificationEvent.EventType eventType = success ?
                    SignatureVerificationEvent.EventType.VERIFICATION_SUCCESS :
                    SignatureVerificationEvent.EventType.VERIFICATION_FAILED;

            SignatureVerificationEvent event = SignatureVerificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .appId(appId)
                    .path(path)
                    .clientIp(clientIp)
                    .params(params)
                    .success(success)
                    .errorMessage(errorMessage)
                    .verificationTime(verificationTime)
                    .timestamp(LocalDateTime.now())
                    .build();

            applicationContext.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish verification event", e);
        }
    }
} 