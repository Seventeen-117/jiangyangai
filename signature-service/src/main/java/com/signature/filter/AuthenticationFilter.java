package com.signature.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signature.config.AuthenticationConfig;
import com.signature.utils.PathMatcherUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * <p>
 * 认证过滤器
 * 处理Bearer Token认证和内部服务调用验证
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // 在API Key过滤器之后执行
public class AuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationConfig authenticationConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("Processing authentication for request: {} {}", method, path);

        // 预检OPTIONS请求直接通过
        if (HttpMethod.OPTIONS.name().equals(method)) {
            log.debug("OPTIONS request, skipping authentication: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否为排除的路径
        if (isExcludedPath(path)) {
            log.debug("Excluded path, skipping authentication: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否为内部Feign调用
        if (isInternalServiceCall(request)) {
            log.debug("Internal service call, allowing authentication bypass: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        // 获取认证头
        String authHeader = request.getHeader(authenticationConfig.getAuthorizationHeaderName());

        // 认证头不存在
        if (authHeader == null || authHeader.isEmpty()) {
            log.debug("Missing authorization header for path: {} {}", method, path);
            handleUnauthorized(response, "Missing authorization header");
            return;
        }

        // 验证Bearer token格式
        if (!isValidBearerToken(authHeader)) {
            log.debug("Invalid authorization header format for path: {} {}", method, path);
            handleUnauthorized(response, "Invalid authorization header format");
            return;
        }

        // 这里只做简单格式验证，实际应用中应该验证token的有效性
        // 可以通过注入JWT服务或其他认证服务来验证token
        log.debug("Authentication passed for path: {} {}", method, path);
        filterChain.doFilter(request, response);
    }

    /**
     * 检查是否为排除的路径
     */
    private boolean isExcludedPath(String path) {
        return authenticationConfig.isExcludedPath(path) || 
               PathMatcherUtil.containsAny(path, "/public/", "/health", "/actuator", "/api/validation/", "/api/metrics/", "/api/token/", "/api/keys/");
    }

    /**
     * 检查是否为内部服务调用
     */
    private boolean isInternalServiceCall(HttpServletRequest request) {
        String apiKey = request.getHeader(authenticationConfig.getApiKeyHeaderName());
        String requestFrom = request.getHeader(authenticationConfig.getRequestFromHeaderName());
        
        // 检查是否为测试API密钥且来自内部服务
        if (authenticationConfig.getTestApiKey().equals(apiKey) && 
            authenticationConfig.getInternalServiceName().equals(requestFrom)) {
            return true;
        }

        // 检查是否为内部服务标识
        if (requestFrom != null && PathMatcherUtil.containsAny(requestFrom, authenticationConfig.getInternalServices())) {
            return true;
        }

        return false;
    }

    /**
     * 验证Bearer token格式
     */
    private boolean isValidBearerToken(String authHeader) {
        return authHeader.startsWith(authenticationConfig.getBearerPrefix());
    }

    /**
     * 处理未授权请求
     */
    private void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "UNAUTHORIZED",
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
