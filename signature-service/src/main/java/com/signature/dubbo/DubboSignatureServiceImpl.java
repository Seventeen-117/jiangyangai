package com.signature.dubbo;

import com.jiangyang.dubbo.api.signature.SignatureService;
import com.jiangyang.dubbo.api.signature.dto.*;
import com.jiangyang.dubbo.api.signature.enums.SignatureType;
import com.jiangyang.dubbo.api.common.Result;
import com.signature.service.impl.SignatureVerificationServiceImpl;
import com.signature.utils.SignatureUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 签名服务 Dubbo 实现
 * 
 * @author jiangyang
 */
@Slf4j
@DubboService(
    version = "1.0.0",
    group = "signature",
    timeout = 5000,
    retries = 0,
    loadbalance = "roundrobin",
    cluster = "failfast"
)
@Component
@RequiredArgsConstructor
public class DubboSignatureServiceImpl implements SignatureService {
    
    private final SignatureVerificationServiceImpl signatureVerificationService;
    
    // 简单的统计计数器
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successRequests = new AtomicLong(0);
    private final AtomicLong failureRequests = new AtomicLong(0);
    
    @Override
    public Result<SignatureResponse> generateSignature(SignatureRequest request) {
        totalRequests.incrementAndGet();
        
        try {
            log.info("Dubbo调用 - 生成签名: appId={}, params={}", request.getAppId(), request.getParams());
            
            // 参数验证
            if (!StringUtils.hasText(request.getAppId()) || !StringUtils.hasText(request.getSecret())) {
                failureRequests.incrementAndGet();
                return Result.failure("应用ID和密钥不能为空", "400");
            }
            
            // 生成时间戳和随机数
            String timestamp = request.getCustomTimestamp();
            if (!StringUtils.hasText(timestamp) && Boolean.TRUE.equals(request.getIncludeTimestamp())) {
                timestamp = String.valueOf(System.currentTimeMillis());
            }
            
            String nonce = request.getCustomNonce();
            if (!StringUtils.hasText(nonce) && Boolean.TRUE.equals(request.getIncludeNonce())) {
                nonce = generateNonce();
            }
            
            // 构建所有参数
            Map<String, String> allParams = new HashMap<>();
            allParams.put("appId", request.getAppId());
            
            if (StringUtils.hasText(timestamp)) {
                allParams.put("timestamp", timestamp);
            }
            if (StringUtils.hasText(nonce)) {
                allParams.put("nonce", nonce);
            }
            
            // 添加业务参数
            if (request.getParams() != null) {
                allParams.putAll(request.getParams());
            }
            
            // 生成签名
            String signature = SignatureUtils.generateSignature(
                allParams, 
                request.getSecret(), 
                request.getSignatureType().getAlgorithm()
            );
            
            // 构建响应
            SignatureResponse response = new SignatureResponse();
            response.setAppId(request.getAppId());
            response.setTimestamp(timestamp);
            response.setNonce(nonce);
            response.setSignature(signature);
            response.setAllParams(allParams);
            response.setExpireSeconds(request.getExpireSeconds());
            response.setExpireTimestamp(System.currentTimeMillis() + (request.getExpireSeconds() * 1000));
            response.setAlgorithm(request.getSignatureType().getAlgorithm());
            response.setSortedParams(SignatureUtils.buildSortedParams(allParams));
            response.setGeneratedAt(System.currentTimeMillis());
            response.setSuccess(true);
            
            successRequests.incrementAndGet();
            log.info("Dubbo调用成功 - 生成签名: appId={}, signature={}", request.getAppId(), signature);
            
            return Result.success(response);
            
        } catch (Exception e) {
            failureRequests.incrementAndGet();
            log.error("Dubbo调用异常 - 生成签名失败: appId={}", request.getAppId(), e);
            return Result.failure("生成签名失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<Boolean> verifySignature(ValidationRequest request) {
        totalRequests.incrementAndGet();
        
        try {
            log.info("Dubbo调用 - 验证签名: appId={}", request.getAppId());
            
            // 参数验证
            if (!StringUtils.hasText(request.getAppId()) || !StringUtils.hasText(request.getSignature())) {
                failureRequests.incrementAndGet();
                return Result.failure("应用ID和签名不能为空", "400");
            }
            
            // 构建验证参数并调用签名验证服务
            Map<String, String> verifyParams = buildVerifyParams(request);
            boolean isValid = signatureVerificationService.verifySignature(
                verifyParams,
                request.getSignature(),
                request.getAppId()
            );
            
            if (isValid) {
                successRequests.incrementAndGet();
            } else {
                failureRequests.incrementAndGet();
            }
            
            log.info("Dubbo调用完成 - 验证签名: appId={}, valid={}", request.getAppId(), isValid);
            return Result.success(isValid);
            
        } catch (Exception e) {
            failureRequests.incrementAndGet();
            log.error("Dubbo调用异常 - 验证签名失败: appId={}", request.getAppId(), e);
            return Result.failure("验证签名失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<List<Boolean>> batchVerifySignature(List<ValidationRequest> requests) {
        totalRequests.addAndGet(requests.size());
        
        try {
            log.info("Dubbo调用 - 批量验证签名: count={}", requests.size());
            
            List<Boolean> results = requests.parallelStream()
                .map(request -> {
                    try {
                        // 构建验证参数并调用签名验证服务
                        Map<String, String> verifyParams = buildVerifyParams(request);
                        boolean isValid = signatureVerificationService.verifySignature(
                            verifyParams,
                            request.getSignature(),
                            request.getAppId()
                        );
                        
                        if (isValid) {
                            successRequests.incrementAndGet();
                        } else {
                            failureRequests.incrementAndGet();
                        }
                        
                        return isValid;
                    } catch (Exception e) {
                        failureRequests.incrementAndGet();
                        log.error("批量验证单个签名失败: appId={}", request.getAppId(), e);
                        return false;
                    }
                })
                .toList();
                
            log.info("Dubbo调用完成 - 批量验证签名: count={}, successCount={}", 
                    requests.size(), results.stream().mapToInt(b -> b ? 1 : 0).sum());
            
            return Result.success(results);
            
        } catch (Exception e) {
            failureRequests.addAndGet(requests.size());
            log.error("Dubbo调用异常 - 批量验证签名失败", e);
            return Result.failure("批量验证签名失败: " + e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<Result<Boolean>> verifySignatureAsync(ValidationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            totalRequests.incrementAndGet();
            
            try {
                log.info("Dubbo异步调用 - 验证签名: appId={}", request.getAppId());
                
                // 构建验证参数并调用签名验证服务
                Map<String, String> verifyParams = buildVerifyParams(request);
                boolean isValid = signatureVerificationService.verifySignature(
                    verifyParams,
                    request.getSignature(),
                    request.getAppId()
                );
                
                if (isValid) {
                    successRequests.incrementAndGet();
                } else {
                    failureRequests.incrementAndGet();
                }
                
                log.info("Dubbo异步调用完成 - 验证签名: appId={}, valid={}", request.getAppId(), isValid);
                return Result.success(isValid);
                
            } catch (Exception e) {
                failureRequests.incrementAndGet();
                log.error("Dubbo异步调用异常 - 验证签名失败: appId={}", request.getAppId(), e);
                return Result.failure("异步验证签名失败: " + e.getMessage());
            }
        });
    }
    
    @Override
    public Result<Boolean> verifySignatureQuick(ValidationRequest request) {
        totalRequests.incrementAndGet();
        
        try {
            log.info("Dubbo调用 - 快速验证签名: appId={}", request.getAppId());
            
            // 快速验证，跳过时间戳和nonce检查
            if (!StringUtils.hasText(request.getAppId()) || !StringUtils.hasText(request.getSignature())) {
                failureRequests.incrementAndGet();
                return Result.failure("应用ID和签名不能为空", "400");
            }
            
            // 简单的签名格式检查
            boolean isValid = request.getSignature().length() > 10 && 
                            !request.getSignature().contains(" ");
            
            if (isValid) {
                successRequests.incrementAndGet();
            } else {
                failureRequests.incrementAndGet();
            }
            
            log.info("Dubbo调用完成 - 快速验证签名: appId={}, valid={}", request.getAppId(), isValid);
            return Result.success(isValid);
            
        } catch (Exception e) {
            failureRequests.incrementAndGet();
            log.error("Dubbo调用异常 - 快速验证签名失败: appId={}", request.getAppId(), e);
            return Result.failure("快速验证签名失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<SignatureResponse> generateExampleSignature(String appId, String secret) {
        try {
            log.info("Dubbo调用 - 生成示例签名: appId={}", appId);
            
            SignatureRequest request = new SignatureRequest();
            request.setAppId(appId);
            request.setSecret(secret);
            request.setSignatureType(SignatureType.HMAC_SHA256);
            request.setIncludeTimestamp(true);
            request.setIncludeNonce(true);
            
            // 添加示例参数
            Map<String, String> params = new HashMap<>();
            params.put("example", "value");
            params.put("test", "data");
            request.setParams(params);
            
            return generateSignature(request);
            
        } catch (Exception e) {
            log.error("Dubbo调用异常 - 生成示例签名失败: appId={}", appId, e);
            return Result.failure("生成示例签名失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<SignatureStatsResponse> getSignatureStats(String appId) {
        try {
            log.info("Dubbo调用 - 获取签名统计: appId={}", appId);
            
            // 构建统计响应
            SignatureStatsResponse stats = new SignatureStatsResponse();
            stats.setAppId(appId);
            stats.setTotalRequests(totalRequests.get());
            stats.setSuccessRequests(successRequests.get());
            stats.setFailureRequests(failureRequests.get());
            
            long total = totalRequests.get();
            if (total > 0) {
                stats.setSuccessRate((double) successRequests.get() / total * 100);
            } else {
                stats.setSuccessRate(0.0);
            }
            
            stats.setAverageResponseTime(50L); // 模拟数据
            stats.setMaxResponseTime(200L);
            stats.setMinResponseTime(10L);
            stats.setTodayRequests(total);
            stats.setWeekRequests(total);
            stats.setMonthRequests(total);
            stats.setStatsTimestamp(System.currentTimeMillis());
            
            log.info("Dubbo调用完成 - 获取签名统计: appId={}, total={}, success={}, failure={}", 
                    appId, total, successRequests.get(), failureRequests.get());
            
            return Result.success(stats);
            
        } catch (Exception e) {
            log.error("Dubbo调用异常 - 获取签名统计失败: appId={}", appId, e);
            return Result.failure("获取签名统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成随机数
     */
    private String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 构建验证参数Map
     * 
     * @param request 验证请求
     * @return 验证参数Map
     */
    private Map<String, String> buildVerifyParams(ValidationRequest request) {
        Map<String, String> verifyParams = new HashMap<>();
        verifyParams.put("appId", request.getAppId());
        
        if (StringUtils.hasText(request.getTimestamp())) {
            verifyParams.put("timestamp", request.getTimestamp());
        }
        if (StringUtils.hasText(request.getNonce())) {
            verifyParams.put("nonce", request.getNonce());
        }
        
        // 添加业务参数
        if (request.getParams() != null) {
            verifyParams.putAll(request.getParams());
        }
        
        return verifyParams;
    }
}
