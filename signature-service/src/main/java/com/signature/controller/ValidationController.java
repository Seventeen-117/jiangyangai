package com.signature.controller;

import com.signature.model.ValidationRequest;
import com.signature.model.ValidationResult;
import com.signature.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

/**
 * 统一验证控制器
 * 提供API Key验证、签名验证、权限验证等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    @Autowired
    private ValidationService validationService;

    /**
     * 验证请求
     *
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateRequest(@RequestBody ValidationRequest request) {
        log.info("收到验证请求: path={}, method={}, apiKey={}", 
                request.getPath(), request.getMethod(), 
                request.getApiKey() != null ? "***" : "null");
        
        try {
            ValidationResult result = validationService.validateRequest(request);
            log.info("验证完成: path={}, 结果: {}, 错误信息: {}", 
                    request.getPath(), result.isValid(), 
                    result.getErrorMessage() != null ? result.getErrorMessage() : "无");
            
            // 确保返回的响应格式正确
            if (result.isValid()) {
                log.info("验证成功，返回成功响应");
            } else {
                log.warn("验证失败，返回失败响应: {}", result.getErrorMessage());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("验证请求异常: path={}", request.getPath(), e);
            ValidationResult errorResult = ValidationResult.failure("VALIDATION_ERROR", e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * 异步验证请求
     *
     * @param request 验证请求
     * @return 异步验证结果
     */
    @PostMapping("/validate/async")
    public CompletableFuture<ResponseEntity<ValidationResult>> validateRequestAsync(@RequestBody ValidationRequest request) {
        log.info("收到异步验证请求: {}", request.getPath());
        
        return validationService.validateRequestAsync(request)
                .thenApply(result -> {
                    log.info("异步验证完成: {}, 结果: {}", request.getPath(), result.isValid());
                    return ResponseEntity.ok(result);
                })
                .exceptionally(throwable -> {
                    log.error("异步验证请求异常: {}", request.getPath(), throwable);
                    return ResponseEntity.ok(ValidationResult.failure("VALIDATION_ERROR", throwable.getMessage()));
                });
    }

    /**
     * 批量验证请求
     *
     * @param requests 验证请求列表
     * @return 验证结果列表
     */
    @PostMapping("/validate/batch")
    public ResponseEntity<List<ValidationResult>> validateRequests(@RequestBody List<ValidationRequest> requests) {
        log.info("收到批量验证请求: {} 个请求", requests.size());
        
        try {
            List<ValidationResult> results = validationService.validateRequests(requests);
            log.info("批量验证完成: {} 个请求", requests.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("批量验证请求异常", e);
            return ResponseEntity.ok(List.of(ValidationResult.failure("VALIDATION_ERROR", e.getMessage())));
        }
    }

    /**
     * 验证API Key
     *
     * @param apiKey API Key
     * @return 验证结果
     */
    @GetMapping("/api-key/{apiKey}")
    public ResponseEntity<ValidationResult> validateApiKey(@PathVariable String apiKey) {
        log.info("收到API Key验证请求: {}", apiKey);
        
        try {
            ValidationResult result = validationService.validateApiKey(apiKey);
            log.info("API Key验证完成: {}, 结果: {}", apiKey, result.isValid());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("API Key验证异常: {}", apiKey, e);
            return ResponseEntity.ok(ValidationResult.failure("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 验证签名
     *
     * @param request 包含签名信息的请求
     * @return 验证结果
     */
    @PostMapping("/signature")
    public ResponseEntity<ValidationResult> validateSignature(@RequestBody ValidationRequest request) {
        log.info("收到签名验证请求: {}", request.getPath());
        
        try {
            ValidationResult result = validationService.validateSignature(request);
            log.info("签名验证完成: {}, 结果: {}", request.getPath(), result.isValid());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("签名验证异常: {}", request.getPath(), e);
            return ResponseEntity.ok(ValidationResult.failure("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 验证权限
     *
     * @param userId 用户ID
     * @param resource 资源
     * @param action 操作
     * @return 验证结果
     */
    @GetMapping("/permission")
    public ResponseEntity<ValidationResult> validatePermission(
            @RequestParam String userId,
            @RequestParam String resource,
            @RequestParam String action) {
        log.info("收到权限验证请求: userId={}, resource={}, action={}", userId, resource, action);
        
        try {
            ValidationResult result = validationService.validatePermission(userId, resource, action);
            log.info("权限验证完成: userId={}, resource={}, action={}, 结果: {}", 
                    userId, resource, action, result.isValid());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("权限验证异常: userId={}, resource={}, action={}", userId, resource, action, e);
            return ResponseEntity.ok(ValidationResult.failure("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 验证JWT Token
     *
     * @param token JWT Token
     * @return 验证结果
     */
    @GetMapping("/jwt/{token}")
    public ResponseEntity<ValidationResult> validateJwtToken(@PathVariable String token) {
        log.info("收到JWT Token验证请求");
        
        try {
            ValidationResult result = validationService.validateJwtToken(token);
            log.info("JWT Token验证完成: {}", result.isValid());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("JWT Token验证异常", e);
            return ResponseEntity.ok(ValidationResult.failure("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 刷新验证缓存
     *
     * @return 刷新结果
     */
    @PostMapping("/cache/refresh")
    public ResponseEntity<Map<String, Object>> refreshValidationCache() {
        log.info("收到刷新验证缓存请求");
        
        try {
            validationService.refreshValidationCache();
            log.info("验证缓存刷新完成");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "验证缓存刷新成功",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("刷新验证缓存异常", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "验证缓存刷新失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
