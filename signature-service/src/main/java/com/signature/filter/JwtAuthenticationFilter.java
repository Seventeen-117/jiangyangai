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
 * JWT认证过滤器
 * 验证JWT Token的有效性
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private ValidationService validationService;

    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("JwtAuthenticationFilter processing: {} {}", method, path);
        
        // 检查JWT验证是否启用
        if (!dynamicConfigService.isJwtValidationEnabled()) {
            log.debug("JWT validation disabled by dynamic config, skipping: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 跳过不需要JWT验证的路径
        if (isSkipJwtValidation(path, method)) {
            log.debug("Skipping JWT validation for path: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 提取JWT Token
        String jwtToken = extractJwtToken(request);
        if (!StringUtils.hasText(jwtToken)) {
            log.debug("JWT Token not found, skipping validation for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 验证JWT Token
        ValidationResult result = validationService.validateJwtToken(jwtToken);
        if (!result.isValid()) {
            log.warn("JWT Token validation failed for path: {}, reason: {}", path, result.getErrorMessage());
            sendUnauthorizedResponse(response, result.getErrorMessage());
            return;
        }
        
        // 将用户信息添加到请求属性中
        request.setAttribute("userId", result.getUserId());
        request.setAttribute("username", result.getUsername());
        request.setAttribute("role", result.getRole());
        request.setAttribute("email", result.getEmail());
        
        log.debug("JWT Token validation passed for user: {}", result.getUserId());
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否需要跳过JWT验证
     * 优先使用数据库配置，如果数据库配置不可用则回退到静态配置
     */
    private boolean isSkipJwtValidation(String path, String method) {
        try {
            // 优先使用动态配置（数据库）
            boolean isExcluded = dynamicConfigService.isJwtExcludedPath(path, method);
            if (isExcluded) {
                log.debug("Path excluded from JWT validation by dynamic config: {} {}", method, path);
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking dynamic JWT excluded paths, falling back to static config: {}", e.getMessage());
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
            log.debug("Path excluded from JWT validation by static config: {} {}", method, path);
        }
        
        return isStaticExcluded;
    }

    /**
     * 提取JWT Token
     */
    private String extractJwtToken(HttpServletRequest request) {
        // 从Authorization头中提取（Bearer格式）
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        
        // 从自定义头中提取
        String jwtToken = request.getHeader("X-JWT-Token");
        if (StringUtils.hasText(jwtToken)) {
            return jwtToken;
        }
        
        // 从查询参数中提取
        jwtToken = request.getParameter("token");
        if (StringUtils.hasText(jwtToken)) {
            return jwtToken;
        }
        
        return null;
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
        
        // 检查JWT验证是否全局禁用
        try {
            if (!dynamicConfigService.isJwtValidationEnabled()) {
                log.debug("JWT validation globally disabled by dynamic config");
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking JWT validation enabled status: {}", e.getMessage());
        }
        
        // 检查是否为排除路径
        return isSkipJwtValidation(path, method);
    }
}
