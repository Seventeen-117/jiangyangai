package com.signature.controller;

import com.signature.model.SsoUserInfo;
import com.signature.model.TokenRequest;
import com.signature.model.TokenResponse;
import com.signature.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Token生成控制器
 * 提供用户token生成、刷新、验证等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/token")
public class TokenController {

    @Autowired
    private TokenService tokenService;

    /**
     * 生成用户Token
     * 
     * @param request Token请求
     * @return Token响应
     */
    @PostMapping("/generate")
    public ResponseEntity<TokenResponse> generateToken(@RequestBody TokenRequest request) {
        log.info("收到Token生成请求: userId={}, username={}", request.getUserId(), request.getUsername());
        
        try {
            TokenResponse response = tokenService.generateToken(request);
            log.info("Token生成成功: userId={}, tokenType={}", request.getUserId(), response.getTokenType());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token生成失败: userId={}", request.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TokenResponse.builder()
                            .success(false)
                            .errorMessage("Token生成失败: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 刷新Token
     * 
     * @param refreshToken 刷新令牌
     * @return 新的Token响应
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("收到Token刷新请求");
        
        try {
            TokenResponse response = tokenService.refreshToken(refreshToken);
            log.info("Token刷新成功: userId={}", response.getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token刷新失败", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(TokenResponse.builder()
                            .success(false)
                            .errorMessage("Token刷新失败: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 验证Token
     * 
     * @param accessToken 访问令牌
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String accessToken) {
        log.info("收到Token验证请求");
        
        try {
            SsoUserInfo userInfo = tokenService.validateToken(accessToken);
            if (userInfo != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("userId", userInfo.getUserId());
                response.put("username", userInfo.getUsername());
                response.put("email", userInfo.getEmail());
                response.put("role", userInfo.getRole());
                log.info("Token验证成功: userId={}", userInfo.getUserId());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Invalid or expired token");
                log.warn("Token验证失败: 无效或过期");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            log.error("Token验证异常", e);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "Token validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 撤销Token
     * 
     * @param accessToken 访问令牌
     * @return 撤销结果
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, Object>> revokeToken(@RequestParam String accessToken) {
        log.info("收到Token撤销请求");
        
        try {
            boolean success = tokenService.revokeToken(accessToken);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Token撤销成功" : "Token撤销失败");
            log.info("Token撤销结果: {}", success);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token撤销异常", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token撤销失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取Token信息
     * 
     * @param accessToken 访问令牌
     * @return Token信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(@RequestParam String accessToken) {
        log.info("收到Token信息查询请求");
        
        try {
            Map<String, Object> tokenInfo = tokenService.getTokenInfo(accessToken);
            if (tokenInfo != null) {
                log.info("Token信息查询成功");
                return ResponseEntity.ok(tokenInfo);
            } else {
                log.warn("Token信息查询失败: Token无效");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token"));
            }
        } catch (Exception e) {
            log.error("Token信息查询异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token info query failed: " + e.getMessage()));
        }
    }

    /**
     * 批量生成Token
     * 
     * @param requests Token请求列表
     * @return Token响应列表
     */
    @PostMapping("/batch-generate")
    public ResponseEntity<Map<String, Object>> batchGenerateTokens(@RequestBody java.util.List<TokenRequest> requests) {
        log.info("收到批量Token生成请求: {} 个请求", requests.size());
        
        try {
            java.util.List<TokenResponse> responses = tokenService.batchGenerateTokens(requests);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", responses.size());
            result.put("tokens", responses);
            log.info("批量Token生成成功: {} 个", responses.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("批量Token生成失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "批量Token生成失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}
