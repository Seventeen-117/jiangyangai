package com.signature.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signature.config.SignatureConfig;
import com.signature.service.SignatureVerificationService;
import com.signature.utils.PathMatcherUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 异步签名验证过滤器
 * 演示不同的验证模式：完全异步、混合验证、快速验证
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 4) // 在同步验证过滤器之后执行
public class AsyncSignatureVerificationFilter extends OncePerRequestFilter {

    @Autowired
    private SignatureVerificationService signatureVerificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SignatureConfig signatureConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("Processing async signature verification for request: {} {}", method, path);

        // 预检OPTIONS请求直接通过
        if (HttpMethod.OPTIONS.name().equals(method)) {
            log.debug("OPTIONS request, skipping async signature verification: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否为排除的路径
        if (isExcludedPath(path)) {
            log.debug("Excluded path, skipping async signature verification: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 如果签名验证被禁用，直接通过
        if (!signatureConfig.isEnabled() || !signatureConfig.isAsyncEnabled()) {
            log.debug("Async signature verification disabled, skipping: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 提取签名参数
            String appId = request.getParameter("appId");
            String timestamp = request.getParameter("timestamp");
            String nonce = request.getParameter("nonce");
            String sign = request.getParameter("sign");

            // 验证必需参数
            if (!StringUtils.hasText(appId) || !StringUtils.hasText(timestamp) || 
                !StringUtils.hasText(nonce) || !StringUtils.hasText(sign)) {
                log.warn("Missing required signature parameters for path: {} {}", method, path);
                handleSignatureError(response, HttpStatus.BAD_REQUEST, "Missing required signature parameters");
                return;
            }

            // 获取所有请求参数
            Map<String, String> allParams = getAllRequestParameters(request);
            
            // 根据配置选择验证模式
            boolean verificationResult = performAsyncVerification(allParams, sign, appId, path);
            
            if (!verificationResult) {
                log.warn("Async signature verification failed for path: {} {}, appId: {}", method, path, appId);
                handleSignatureError(response, HttpStatus.UNAUTHORIZED, "Async signature verification failed");
                return;
            }

            // 异步保存nonce到缓存
            signatureVerificationService.saveNonceAsync(nonce, signatureConfig.getNonceCacheExpireSeconds());

            log.debug("Async signature verification passed for path: {} {}, appId: {}", method, path, appId);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error during async signature verification for path: {} {}: {}", method, path, e.getMessage(), e);
            handleSignatureError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Async signature verification error");
        }
    }

    /**
     * 执行异步验证
     */
    private boolean performAsyncVerification(Map<String, String> params, String sign, String appId, String path) {
        try {
            CompletableFuture<Boolean> verificationFuture;
            
            switch (signatureConfig.getAsyncMode().toUpperCase()) {
                case "ASYNC":
                    // 完全异步模式
                    verificationFuture = signatureVerificationService.verifySignatureAsync(params, sign, appId);
                    break;
                    
                case "HYBRID":
                    // 混合验证模式
                    verificationFuture = signatureVerificationService.verifySignatureFast(params, sign, appId);
                    break;
                    
                case "QUICK":
                    // 快速验证模式
                    boolean quickResult = signatureVerificationService.verifySignatureQuick(params, appId);
                    if (quickResult) {
                        // 快速验证通过，启动异步详细验证
                        signatureVerificationService.verifySignatureAsync(params, sign, appId)
                                .thenAccept(detailedResult -> {
                                    if (!detailedResult) {
                                        log.warn("Async detailed verification failed for appId: {}", appId);
                                    }
                                });
                    }
                    return quickResult;
                    
                default:
                    // 默认使用混合模式
                    verificationFuture = signatureVerificationService.verifySignatureFast(params, sign, appId);
                    break;
            }
            
            // 等待异步验证结果，设置超时时间
            return verificationFuture.get(signatureConfig.getAsyncTimeout(), TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            log.error("Error during async verification for appId: {}", appId, e);
            return false;
        }
    }

    /**
     * 检查是否为排除的路径
     */
    private boolean isExcludedPath(String path) {
        return PathMatcherUtil.matchesAny(path, signatureConfig.getExcludedPaths()) || 
               PathMatcherUtil.containsAny(path, "/public/", "/health", "/actuator", "/api/validation/", "/api/metrics/", "/api/token/", "/api/keys/");
    }

    /**
     * 获取所有请求参数
     */
    private Map<String, String> getAllRequestParameters(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        
        // 获取URL参数
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (StringUtils.hasText(paramValue)) {
                params.put(paramName, paramValue);
            }
        }

        return params;
    }

    /**
     * 处理签名验证错误
     */
    private void handleSignatureError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "ASYNC_SIGNATURE_VERIFICATION_FAILED",
                "message", message,
                "timestamp", System.currentTimeMillis(),
                "asyncMode", signatureConfig.getAsyncMode()
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
