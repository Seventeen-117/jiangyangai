package com.signature.filter;

import com.signature.model.ValidationRequest;
import com.signature.model.ValidationResult;
import com.signature.service.ValidationService;
import com.signature.service.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 签名验证过滤器
 * 验证请求签名的正确性，防止请求被篡改
 */
@Slf4j
@Component
public class SignatureVerificationFilter extends OncePerRequestFilter {

    @Autowired
    private ValidationService validationService;

    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("SignatureVerificationFilter processing: {} {}", method, path);
        
        // 检查签名验证是否启用
        if (!dynamicConfigService.isSignatureValidationEnabled()) {
            log.debug("Signature validation disabled by dynamic config, skipping: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 跳过不需要签名验证的路径
        if (isSkipSignatureValidation(path, method)) {
            log.debug("Skipping signature validation for path: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 检查是否包含签名信息
        String signature = request.getHeader("X-Signature");
        String appId = request.getHeader("X-App-Id");
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(appId) || 
            !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
            log.debug("Signature information incomplete, skipping validation for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 构建验证请求
        ValidationRequest validationRequest = ValidationRequest.builder()
                .path(path)
                .method(method)
                .signature(signature)
                .appId(appId)
                .timestamp(Long.parseLong(timestamp))
                .nonce(nonce)
                .clientIp(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .build();
        
        // 验证签名
        ValidationResult result = validationService.validateSignature(validationRequest);
        if (!result.isValid()) {
            log.warn("Signature validation failed for path: {}, reason: {}", path, result.getErrorMessage());
            sendUnauthorizedResponse(response, result.getErrorMessage());
            return;
        }
        
        log.debug("Signature validation passed for path: {}", path);
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否需要跳过签名验证
     * 优先使用数据库配置，如果数据库配置不可用则回退到静态配置
     */
    private boolean isSkipSignatureValidation(String path, String method) {
        try {
            // 优先使用动态配置（数据库）
            boolean isExcluded = dynamicConfigService.isExcludedPath(path, method);
            if (isExcluded) {
                log.debug("Path excluded by dynamic config: {} {}", method, path);
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking dynamic excluded paths, falling back to static config: {}", e.getMessage());
        }
        
        // 回退到静态配置
        boolean isStaticExcluded = path.startsWith("/actuator") || 
                                  path.startsWith("/health") || 
                                  path.startsWith("/metrics") ||
                                  path.startsWith("/public") ||
                                  path.startsWith("/api/validation") ||
                                  path.startsWith("/api/metrics") ||
                                  path.startsWith("/api/token") ||
                                  path.startsWith("/api/keys") ||
                                  path.startsWith("/api/signature") ||
                                  path.startsWith("/swagger-ui") ||
                                  path.startsWith("/v3/api-docs");
        
        if (isStaticExcluded) {
            log.debug("Path excluded by static config: {} {}", method, path);
        }
        
        return isStaticExcluded;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        
        String jsonResponse = com.alibaba.fastjson.JSON.toJSONString(result);
        response.getWriter().write(jsonResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // 检查签名验证是否全局禁用
        try {
            if (!dynamicConfigService.isSignatureValidationEnabled()) {
                log.debug("Signature validation globally disabled by dynamic config");
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking signature validation enabled status: {}", e.getMessage());
        }
        
        // 检查是否为排除路径
        return isSkipSignatureValidation(path, method);
    }
}
