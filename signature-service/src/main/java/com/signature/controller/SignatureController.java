package com.signature.controller;

import com.signature.model.SignatureVerificationRequest;
import com.signature.service.SignatureVerificationService;
import com.signature.utils.SignatureUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.TreeMap;

/**
 * 签名控制器
 * 提供签名生成和验证的REST API
 */
@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
@Slf4j
public class SignatureController {

    private final SignatureVerificationService signatureVerificationService;

    /**
     * 生成签名
     * 
     * @param request 签名生成请求
     * @return 包含签名的响应
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateSignature(@RequestBody Map<String, Object> request) {
        try {
            String appId = (String) request.get("appId");
            String secret = (String) request.get("secret");
            @SuppressWarnings("unchecked")
            Map<String, String> businessParams = (Map<String, String>) request.get("params");

            if (appId == null || secret == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing required parameters: appId and secret"
                ));
            }

            // 生成签名参数
            Map<String, String> signatureParams = SignatureUtils.generateSignatureParams(appId, secret, businessParams);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Signature generated successfully",
                "data", signatureParams
            ));

        } catch (Exception e) {
            log.error("Error generating signature", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error generating signature: " + e.getMessage()
            ));
        }
    }

    /**
     * 验证签名
     * 
     * @param request 签名验证请求
     * @return 验证结果
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifySignature(@RequestBody Map<String, Object> request) {
        try {
            String appId = (String) request.get("appId");
            String timestamp = (String) request.get("timestamp");
            String nonce = (String) request.get("nonce");
            String sign = (String) request.get("sign");
            @SuppressWarnings("unchecked")
            Map<String, String> params = (Map<String, String>) request.get("params");

            if (appId == null || timestamp == null || nonce == null || sign == null || params == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing required parameters"
                ));
            }

            // 验证时间戳
            if (!signatureVerificationService.validateTimestamp(timestamp, 300)) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Timestamp expired"
                ));
            }

            // 验证nonce
            if (!signatureVerificationService.validateNonce(nonce, 1800)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Replay attack detected"
                ));
            }

            // 验证签名
            boolean isValid = signatureVerificationService.verifySignature(params, sign, appId);
            if (!isValid) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Signature verification failed"
                ));
            }

            // 保存nonce
            signatureVerificationService.saveNonce(nonce, 1800);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Signature verification passed",
                "appId", appId,
                "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error verifying signature: " + e.getMessage()
            ));
        }
    }

    /**
     * 快速验证签名（仅验证签名，不验证时间戳和nonce）
     * 
     * @param request 签名验证请求
     * @return 验证结果
     */
    @PostMapping("/verify-quick")
    public ResponseEntity<Map<String, Object>> verifySignatureQuick(@RequestBody Map<String, Object> request) {
        try {
            String appId = (String) request.get("appId");
            String sign = (String) request.get("sign");
            @SuppressWarnings("unchecked")
            Map<String, String> params = (Map<String, String>) request.get("params");

            if (appId == null || sign == null || params == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing required parameters"
                ));
            }

            // 快速验证签名
            boolean isValid = signatureVerificationService.verifySignatureQuick(params, appId);

            return ResponseEntity.ok(Map.of(
                "success", isValid,
                "message", isValid ? "Signature verification passed" : "Signature verification failed",
                "appId", appId
            ));

        } catch (Exception e) {
            log.error("Error during quick signature verification", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error during quick signature verification: " + e.getMessage()
            ));
        }
    }

    /**
     * 生成示例签名参数（用于测试）
     * 
     * @param appId 应用ID
     * @param secret 应用密钥
     * @return 示例签名参数
     */
    @GetMapping("/example")
    public ResponseEntity<Map<String, Object>> generateExample(@RequestParam String appId, @RequestParam String secret) {
        try {
            Map<String, String> exampleParams = SignatureUtils.generateExampleParams(appId, secret);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Example signature parameters generated",
                "data", exampleParams
            ));

        } catch (Exception e) {
            log.error("Error generating example signature parameters", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error generating example: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取签名验证统计信息
     * 
     * @param appId 应用ID
     * @return 统计信息
     */
    @GetMapping("/stats/{appId}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String appId) {
        try {
            var stats = signatureVerificationService.getAppStats(appId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Statistics retrieved successfully",
                "data", stats
            ));

        } catch (Exception e) {
            log.error("Error retrieving statistics for appId: {}", appId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error retrieving statistics: " + e.getMessage()
            ));
        }
    }
} 