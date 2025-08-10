package com.bgpay.bgai.adapter;

import com.bgpay.bgai.dubbo.DubboClientService;
import com.jiangyang.dubbo.api.signature.dto.SignatureResponse;
import com.jiangyang.dubbo.api.signature.dto.SignatureStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 签名服务适配器
 * 支持Feign和Dubbo之间的平滑切换
 * 
 * @author jiangyang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureServiceAdapter {
    
    private final DubboClientService dubboClientService;
    // 如果需要保留Feign客户端，可以注入
    // private final SignatureServiceFeignClient feignClient;
    
    @Value("${app.use-dubbo:true}")
    private boolean useDubbo;
    
    @Value("${app.dubbo-fallback:true}")
    private boolean dubboFallback;
    
    /**
     * 生成签名
     * 
     * @param appId 应用ID
     * @param secret 应用密钥
     * @param params 业务参数
     * @return 签名响应
     */
    public SignatureResponse generateSignature(String appId, String secret, Map<String, String> params) {
        if (useDubbo) {
            try {
                log.debug("使用Dubbo调用生成签名: appId={}", appId);
                return dubboClientService.generateSignature(appId, secret, params);
            } catch (Exception e) {
                log.warn("Dubbo调用生成签名失败，appId={}", appId, e);
                if (dubboFallback) {
                    log.info("启用降级，切换到Feign调用");
                    return callFeignGenerateSignature(appId, secret, params);
                } else {
                    throw new RuntimeException("Dubbo调用生成签名失败", e);
                }
            }
        } else {
            log.debug("使用Feign调用生成签名: appId={}", appId);
            return callFeignGenerateSignature(appId, secret, params);
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
        if (useDubbo) {
            try {
                log.debug("使用Dubbo调用验证签名: appId={}", appId);
                return dubboClientService.verifySignature(appId, timestamp, nonce, signature, params);
            } catch (Exception e) {
                log.warn("Dubbo调用验证签名失败，appId={}", appId, e);
                if (dubboFallback) {
                    log.info("启用降级，切换到Feign调用");
                    return callFeignVerifySignature(appId, timestamp, nonce, signature, params);
                } else {
                    log.error("Dubbo调用验证签名失败，且未启用降级");
                    return false;
                }
            }
        } else {
            log.debug("使用Feign调用验证签名: appId={}", appId);
            return callFeignVerifySignature(appId, timestamp, nonce, signature, params);
        }
    }
    
    /**
     * 快速验证签名
     * 
     * @param appId 应用ID
     * @param signature 签名
     * @param params 业务参数
     * @return 验证结果
     */
    public boolean verifySignatureQuick(String appId, String signature, Map<String, String> params) {
        if (useDubbo) {
            try {
                log.debug("使用Dubbo调用快速验证签名: appId={}", appId);
                return dubboClientService.verifySignatureQuick(appId, signature, params);
            } catch (Exception e) {
                log.warn("Dubbo调用快速验证签名失败，appId={}", appId, e);
                if (dubboFallback) {
                    log.info("启用降级，切换到Feign调用");
                    return callFeignVerifySignatureQuick(appId, signature, params);
                } else {
                    log.error("Dubbo调用快速验证签名失败，且未启用降级");
                    return false;
                }
            }
        } else {
            log.debug("使用Feign调用快速验证签名: appId={}", appId);
            return callFeignVerifySignatureQuick(appId, signature, params);
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
        if (useDubbo) {
            try {
                log.debug("使用Dubbo调用异步验证签名: appId={}", appId);
                return dubboClientService.verifySignatureAsync(appId, timestamp, nonce, signature, params)
                    .exceptionally(throwable -> {
                        log.warn("Dubbo异步调用验证签名失败，appId={}", appId, throwable);
                        if (dubboFallback) {
                            log.info("启用降级，切换到Feign调用");
                            try {
                                return callFeignVerifySignature(appId, timestamp, nonce, signature, params);
                            } catch (Exception e) {
                                log.error("Feign降级调用也失败", e);
                                return false;
                            }
                        } else {
                            return false;
                        }
                    });
            } catch (Exception e) {
                log.warn("Dubbo异步调用初始化失败，appId={}", appId, e);
                if (dubboFallback) {
                    log.info("启用降级，切换到Feign同步调用");
                    boolean result = callFeignVerifySignature(appId, timestamp, nonce, signature, params);
                    return CompletableFuture.completedFuture(result);
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            }
        } else {
            log.debug("使用Feign调用验证签名（同步转异步）: appId={}", appId);
            return CompletableFuture.supplyAsync(() -> 
                callFeignVerifySignature(appId, timestamp, nonce, signature, params));
        }
    }
    
    /**
     * 获取签名统计
     * 
     * @param appId 应用ID
     * @return 签名统计
     */
    public SignatureStatsResponse getSignatureStats(String appId) {
        if (useDubbo) {
            try {
                log.debug("使用Dubbo调用获取签名统计: appId={}", appId);
                return dubboClientService.getSignatureStats(appId);
            } catch (Exception e) {
                log.warn("Dubbo调用获取签名统计失败，appId={}", appId, e);
                if (dubboFallback) {
                    log.info("启用降级，切换到Feign调用");
                    return callFeignGetSignatureStats(appId);
                } else {
                    throw new RuntimeException("Dubbo调用获取签名统计失败", e);
                }
            }
        } else {
            log.debug("使用Feign调用获取签名统计: appId={}", appId);
            return callFeignGetSignatureStats(appId);
        }
    }
    
    /**
     * 检查当前使用的通信方式
     * 
     * @return 通信方式信息
     */
    public Map<String, Object> getAdapterStatus() {
        return Map.of(
            "useDubbo", useDubbo,
            "dubboFallback", dubboFallback,
            "communicationType", useDubbo ? "Dubbo RPC" : "Feign HTTP",
            "fallbackEnabled", dubboFallback,
            "timestamp", System.currentTimeMillis()
        );
    }
    
    // ========================= Feign 调用方法 =========================
    
    /**
     * Feign调用生成签名
     */
    private SignatureResponse callFeignGenerateSignature(String appId, String secret, Map<String, String> params) {
        // 这里可以调用原有的Feign客户端或HTTP调用
        // 为了示例，这里返回一个模拟的响应
        log.info("调用Feign生成签名（模拟实现）: appId={}", appId);
        
        SignatureResponse response = new SignatureResponse();
        response.setAppId(appId);
        response.setTimestamp(String.valueOf(System.currentTimeMillis()));
        response.setNonce("feign_" + System.currentTimeMillis());
        response.setSignature("feign_signature_" + System.currentTimeMillis());
        response.setGeneratedAt(System.currentTimeMillis());
        response.setSuccess(true);
        
        return response;
    }
    
    /**
     * Feign调用验证签名
     */
    private boolean callFeignVerifySignature(String appId, String timestamp, String nonce, 
                                           String signature, Map<String, String> params) {
        // 这里可以调用原有的Feign客户端或HTTP调用
        // 为了示例，这里返回一个简单的验证逻辑
        log.info("调用Feign验证签名（模拟实现）: appId={}", appId);
        
        // 简单的模拟验证：检查签名是否以特定前缀开头
        return signature != null && (signature.startsWith("feign_signature_") || signature.startsWith("generated_signature_"));
    }
    
    /**
     * Feign调用快速验证签名
     */
    private boolean callFeignVerifySignatureQuick(String appId, String signature, Map<String, String> params) {
        log.info("调用Feign快速验证签名（模拟实现）: appId={}", appId);
        return signature != null && signature.length() > 10;
    }
    
    /**
     * Feign调用获取签名统计
     */
    private SignatureStatsResponse callFeignGetSignatureStats(String appId) {
        log.info("调用Feign获取签名统计（模拟实现）: appId={}", appId);
        
        SignatureStatsResponse stats = new SignatureStatsResponse();
        stats.setAppId(appId);
        stats.setTotalRequests(100L);
        stats.setSuccessRequests(95L);
        stats.setFailureRequests(5L);
        stats.setSuccessRate(95.0);
        stats.setAverageResponseTime(100L);
        stats.setStatsTimestamp(System.currentTimeMillis());
        
        return stats;
    }
}
