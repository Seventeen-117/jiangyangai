package com.signature.service.impl;

import com.signature.model.ValidationRequest;
import com.signature.model.ValidationResult;
import com.signature.service.ApiKeyService;
import com.signature.service.DynamicConfigService;
import com.signature.service.SsoService;
import com.signature.service.ValidationService;
import com.signature.utils.JwtUtils;
import com.signature.utils.Sha256Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 统一验证服务实现
 */
@Slf4j
@Service
public class ValidationServiceImpl implements ValidationService {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private SsoService ssoService;

    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    @Cacheable(value = "validation_results", key = "#request.hashCode()")
    public ValidationResult validateRequest(ValidationRequest request) {
        log.info("开始验证请求: path={}, method={}, apiKey={}", 
                request.getPath(), request.getMethod(), 
                request.getApiKey() != null ? "***" : "null");

        // 1. 检查是否为排除路径
        if (isExcludedPath(request.getPath())) {
            log.info("路径被排除，跳过验证: {}", request.getPath());
            return ValidationResult.success();
        }
        
        // 2. 检查是否为验证端点本身
        if ("/api/validation/validate".equals(request.getPath())) {
            log.info("验证端点本身，跳过验证: {}", request.getPath());
            return ValidationResult.success();
        }
        
        // 3. 检查是否为 token 或 keys 相关端点
        if (request.getPath().startsWith("/api/token/") || request.getPath().startsWith("/api/keys/")) {
            log.info("Token/Keys 端点，跳过验证: {}", request.getPath());
            return ValidationResult.success();
        }

        // 3. API Key验证
        if (StringUtils.hasText(request.getApiKey())) {
            ValidationResult apiKeyResult = validateApiKey(request.getApiKey());
            if (!apiKeyResult.isValid()) {
                log.warn("API Key验证失败: {}", apiKeyResult.getErrorMessage());
                return apiKeyResult;
            }
        }

        // 4. 签名验证
        if (StringUtils.hasText(request.getSignature())) {
            ValidationResult signatureResult = validateSignature(request);
            if (!signatureResult.isValid()) {
                log.warn("签名验证失败: {}", signatureResult.getErrorMessage());
                return signatureResult;
            }
        }

        // 5. JWT Token验证
        if (StringUtils.hasText(request.getJwtToken())) {
            ValidationResult tokenResult = validateJwtToken(request.getJwtToken());
            if (!tokenResult.isValid()) {
                log.warn("JWT Token验证失败: {}", tokenResult.getErrorMessage());
                return tokenResult;
            }
        }

        // 6. 权限验证
        if (StringUtils.hasText(request.getUserId()) && StringUtils.hasText(request.getResource())) {
            ValidationResult permissionResult = validatePermission(
                    request.getUserId(), 
                    request.getResource(), 
                    request.getAction()
            );
            if (!permissionResult.isValid()) {
                log.warn("权限验证失败: {}", permissionResult.getErrorMessage());
                return permissionResult;
            }
        }

        log.info("请求验证成功: {}", request.getPath());
        return ValidationResult.success();
    }

    @Override
    @Async
    public CompletableFuture<ValidationResult> validateRequestAsync(ValidationRequest request) {
        return CompletableFuture.completedFuture(validateRequest(request));
    }

    @Override
    public List<ValidationResult> validateRequests(List<ValidationRequest> requests) {
        return requests.stream()
                .map(this::validateRequest)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "api_key_validation", key = "#apiKey")
    public ValidationResult validateApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return ValidationResult.invalidApiKey();
        }

        try {
            var result = apiKeyService.validateApiKeyStatus(apiKey);
            if (result.getStatus().name().equals("VALID")) {
                var keyInfo = apiKeyService.getApiKeyInfo(apiKey);
                if (keyInfo != null && keyInfo.getActive() != null && keyInfo.getActive() == 1) {
                    return ValidationResult.success(
                            keyInfo.getClientId(),
                            keyInfo.getClientName(),
                            "USER"
                    );
                }
            }
            return ValidationResult.invalidApiKey();
        } catch (Exception e) {
            log.error("API Key验证异常", e);
            return ValidationResult.invalidApiKey();
        }
    }

    @Override
    @Cacheable(value = "signature_validation", key = "#request.hashCode()")
    public ValidationResult validateSignature(ValidationRequest request) {
        if (!StringUtils.hasText(request.getSignature()) || 
            !StringUtils.hasText(request.getAppId()) ||
            request.getTimestamp() == null ||
            !StringUtils.hasText(request.getNonce())) {
            return ValidationResult.invalidSignature();
        }

        try {
            // 检查时间戳是否过期
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - request.getTimestamp());
            if (timeDiff > 300000) { // 5分钟
                return ValidationResult.failure("TIMESTAMP_EXPIRED", "Request timestamp is expired");
            }

            // 构建签名参数
            StringBuilder signStr = new StringBuilder();
            signStr.append("appId=").append(request.getAppId());
            signStr.append("&timestamp=").append(request.getTimestamp());
            signStr.append("&nonce=").append(request.getNonce());

            // 添加请求参数
            if (request.getParameters() != null) {
                request.getParameters().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> signStr.append("&").append(entry.getKey()).append("=").append(entry.getValue()));
            }

            // 计算签名
            String expectedSignature = Sha256Util.hash(signStr.toString());
            if (expectedSignature.equals(request.getSignature())) {
                return ValidationResult.success();
            } else {
                return ValidationResult.invalidSignature();
            }
        } catch (Exception e) {
            log.error("签名验证异常", e);
            return ValidationResult.invalidSignature();
        }
    }

    @Override
    @Cacheable(value = "permission_validation", key = "#userId + '_' + #resource + '_' + #action")
    public ValidationResult validatePermission(String userId, String resource, String action) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(resource)) {
            return ValidationResult.insufficientPermission();
        }

        try {
            // 这里应该调用权限服务进行验证
            // 暂时返回成功，实际应该根据用户权限进行验证
            boolean hasPermission = apiKeyService.validateApiKeyPermission(userId, resource + ":" + action);
            if (hasPermission) {
                return ValidationResult.success(userId, null, "USER");
            } else {
                return ValidationResult.insufficientPermission();
            }
        } catch (Exception e) {
            log.error("权限验证异常", e);
            return ValidationResult.insufficientPermission();
        }
    }

    @Override
    @Cacheable(value = "jwt_validation", key = "#token")
    public ValidationResult validateJwtToken(String token) {
        if (!StringUtils.hasText(token)) {
            return ValidationResult.invalidToken();
        }

        try {
            // 使用JwtUtils验证token
            var userInfo = jwtUtils.validateAccessToken(token);
            if (userInfo != null) {
                return ValidationResult.success(
                        userInfo.getUserId(),
                        userInfo.getUsername(),
                        userInfo.getRole()
                );
            } else {
                return ValidationResult.invalidToken();
            }
        } catch (Exception e) {
            log.error("JWT Token验证异常", e);
            return ValidationResult.invalidToken();
        }
    }

    @Override
    public void refreshValidationCache() {
        log.info("刷新验证缓存");
        // 这里应该清除相关的缓存
        // 可以通过CacheManager来清除特定的缓存
    }

    /**
     * 检查是否为排除路径
     */
    private boolean isExcludedPath(String path) {
        try {
            boolean isExcluded = dynamicConfigService.isPathMatchType(path, "EXCLUDED");
            log.info("ValidationServiceImpl - Path exclusion check for '{}': {}", path, isExcluded);
            return isExcluded;
        } catch (Exception e) {
            log.warn("Error checking excluded path in ValidationServiceImpl for: {}, error: {}", path, e.getMessage());
            // 如果检查失败，默认不排除
            return false;
        }
    }
}
