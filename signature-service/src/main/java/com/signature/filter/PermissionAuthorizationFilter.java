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
 * 权限验证过滤器
 * 验证用户权限和访问控制
 */
@Slf4j
@Component
public class PermissionAuthorizationFilter extends OncePerRequestFilter {

    @Autowired
    private ValidationService validationService;

    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("PermissionAuthorizationFilter processing: {} {}", method, path);
        
        // 检查权限验证是否启用
        if (!dynamicConfigService.isPermissionValidationEnabled()) {
            log.debug("Permission validation disabled by dynamic config, skipping: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 跳过不需要权限验证的路径
        if (isSkipPermissionValidation(path, method)) {
            log.debug("Skipping permission validation for path: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取用户信息
        String userId = (String) request.getAttribute("userId");
        if (!StringUtils.hasText(userId)) {
            log.debug("No user ID found, skipping permission validation for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        

        
        // 验证权限
        ValidationResult result = validationService.validatePermission(userId, path, method.toLowerCase());
        if (!result.isValid()) {
            log.warn("Permission validation failed for user: {}, path: {}, reason: {}", 
                    userId, path, result.getErrorMessage());
            sendForbiddenResponse(response, result.getErrorMessage());
            return;
        }
        
        log.debug("Permission validation passed for user: {}, path: {}", userId, path);
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否需要跳过权限验证
     * 优先使用数据库配置，如果数据库配置不可用则回退到静态配置
     */
    private boolean isSkipPermissionValidation(String path, String method) {
        try {
            // 优先使用动态配置（数据库）
            boolean isExcluded = dynamicConfigService.isPermissionExcludedPath(path, method);
            if (isExcluded) {
                log.debug("Path excluded from permission validation by dynamic config: {} {}", method, path);
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking dynamic permission excluded paths, falling back to static config: {}", e.getMessage());
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
            log.debug("Path excluded from permission validation by static config: {} {}", method, path);
        }
        
        return isStaticExcluded;
    }



    /**
     * 发送禁止访问响应
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        
        String jsonResponse = com.alibaba.fastjson.JSON.toJSONString(result);
        response.getWriter().write(jsonResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // 检查权限验证是否全局禁用
        try {
            if (!dynamicConfigService.isPermissionValidationEnabled()) {
                log.debug("Permission validation globally disabled by dynamic config");
                return true;
            }
        } catch (Exception e) {
            log.warn("Error checking permission validation enabled status: {}", e.getMessage());
        }
        
        // 检查是否为排除路径
        return isSkipPermissionValidation(path, method);
    }
}
