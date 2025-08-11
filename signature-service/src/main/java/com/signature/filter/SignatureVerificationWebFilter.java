package com.signature.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signature.config.SignatureConfig;
import com.signature.service.SignatureVerificationService;
import com.signature.service.DynamicConfigService;
import com.signature.utils.PathMatcherUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * WebFlux环境的接口签名验证过滤器
 * 实现HMAC-SHA256签名验证、时间戳验证和nonce防重放攻击
 * </p>
 *
 * @author signature-service
 * @since 2025-01-01
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3) // 在API Key认证之后执行
public class SignatureVerificationWebFilter implements WebFilter {

    @Autowired
    private SignatureVerificationService signatureVerificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SignatureConfig signatureConfig;

    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.debug("Processing signature verification for request: {} {}", method, path);

        // 预检OPTIONS请求直接通过
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            log.debug("OPTIONS request, skipping signature verification: {}", path);
            return chain.filter(exchange);
        }

        // 检查是否为排除的路径
        if (isExcludedPath(path, method)) {
            log.debug("Excluded path, skipping signature verification: {}", path);
            return chain.filter(exchange);
        }

        // 检查签名验证规则是否启用
        if (!dynamicConfigService.isSignatureValidationEnabled()) {
            log.debug("Signature validation disabled by dynamic config, skipping: {}", path);
            return chain.filter(exchange);
        }

        // 如果签名验证被禁用，直接通过
        if (!signatureConfig.isEnabled()) {
            log.debug("Signature verification disabled by static config, skipping: {}", path);
            return chain.filter(exchange);
        }

        // 对于有请求体的请求，需要缓存请求体以支持JSON参数解析
        if (hasRequestBody(exchange.getRequest())) {
            return cacheRequestBodyAndProcess(exchange, chain);
        } else {
            // 没有请求体的请求（GET等），直接处理
            return processSignatureVerification(exchange, chain, Mono.just(new HashMap<>()));
        }
    }

    /**
     * 检查请求是否有请求体
     */
    private boolean hasRequestBody(ServerHttpRequest request) {
        return (HttpMethod.POST.equals(request.getMethod()) || 
                HttpMethod.PUT.equals(request.getMethod()) || 
                HttpMethod.PATCH.equals(request.getMethod())) &&
               request.getHeaders().getContentLength() > 0;
    }

    /**
     * 缓存请求体并处理签名验证
     */
    private Mono<Void> cacheRequestBodyAndProcess(ServerWebExchange exchange, WebFilterChain chain) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .cast(DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0])
                .flatMap(bodyBytes -> {
                    // 解析请求体参数
                    Mono<Map<String, String>> bodyParamsMono = parseRequestBodyParams(bodyBytes, exchange);
                    
                    // 创建新的请求对象，恢复请求体
                    ServerHttpRequest cachedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            if (bodyBytes.length > 0) {
                                return Flux.just(exchange.getResponse().bufferFactory().wrap(bodyBytes));
                            } else {
                                return Flux.empty();
                            }
                        }
                    };
                    
                    ServerWebExchange cachedExchange = exchange.mutate().request(cachedRequest).build();
                    
                    return processSignatureVerification(cachedExchange, chain, bodyParamsMono);
                });
    }

    /**
     * 处理签名验证逻辑
     */
    private Mono<Void> processSignatureVerification(ServerWebExchange exchange, WebFilterChain chain, Mono<Map<String, String>> bodyParamsMono) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        return bodyParamsMono.flatMap(bodyParams -> {
            // 提取签名参数
            String appId = getParameter(exchange, "appId");
            String timestamp = getParameter(exchange, "timestamp");
            String nonce = getParameter(exchange, "nonce");
            String sign = getParameter(exchange, "sign");

            // 验证必需参数
            if (!StringUtils.hasText(appId) || !StringUtils.hasText(timestamp) || 
                !StringUtils.hasText(nonce) || !StringUtils.hasText(sign)) {
                log.warn("Missing required signature parameters for path: {} {}", method, path);
                return handleSignatureError(exchange, HttpStatus.BAD_REQUEST, "Missing required signature parameters");
            }

            // 验证时间戳
            if (!signatureVerificationService.validateTimestamp(timestamp, signatureConfig.getTimestampExpireSeconds())) {
                log.warn("Timestamp expired for path: {} {}, timestamp: {}", method, path, timestamp);
                return handleSignatureError(exchange, HttpStatus.UNAUTHORIZED, "Timestamp expired");
            }

            // 验证nonce防重放攻击
            if (!signatureVerificationService.validateNonce(nonce, signatureConfig.getNonceCacheExpireSeconds())) {
                log.warn("Nonce already used (replay attack detected) for path: {} {}, nonce: {}", method, path, nonce);
                return handleSignatureError(exchange, HttpStatus.FORBIDDEN, "Replay attack detected");
            }

            // 获取所有请求参数（包括URL参数和请求体参数）
            Map<String, String> allParams = getAllRequestParameters(exchange, bodyParams);
            
            // 验证签名
            if (!signatureVerificationService.verifySignature(allParams, sign, appId)) {
                log.warn("Signature verification failed for path: {} {}, appId: {}", method, path, appId);
                return handleSignatureError(exchange, HttpStatus.UNAUTHORIZED, "Signature verification failed");
            }

            // 保存nonce到缓存
            signatureVerificationService.saveNonce(nonce, signatureConfig.getNonceCacheExpireSeconds());

            log.debug("Signature verification passed for path: {} {}, appId: {}", method, path, appId);
            return chain.filter(exchange);
        }).onErrorResume(e -> {
            log.error("Error during signature verification for path: {} {}: {}", method, path, e.getMessage(), e);
            return handleSignatureError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Signature verification error");
        });
    }

    /**
     * 检查是否为排除的路径
     * 优先使用数据库配置，如果数据库配置不可用则回退到静态配置
     */
    private boolean isExcludedPath(String path, String method) {
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
        boolean isStaticExcluded = PathMatcherUtil.matchesAny(path, signatureConfig.getExcludedPaths()) || 
                                  PathMatcherUtil.containsAny(path, "/public/", "/health", "/actuator", 
                                                            "/api/validation/", "/api/metrics/", "/api/token/", 
                                                            "/api/keys/", "/api/signature/");
        
        if (isStaticExcluded) {
            log.debug("Path excluded by static config: {} {}", method, path);
        }
        
        return isStaticExcluded;
    }

    /**
     * 从请求中获取参数值
     */
    private String getParameter(ServerWebExchange exchange, String paramName) {
        List<String> values = exchange.getRequest().getQueryParams().get(paramName);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    /**
     * 解析请求体参数
     */
    private Mono<Map<String, String>> parseRequestBodyParams(byte[] bodyBytes, ServerWebExchange exchange) {
        if (bodyBytes.length == 0) {
            return Mono.just(new HashMap<>());
        }

        String contentType = exchange.getRequest().getHeaders().getContentType() != null ? 
                exchange.getRequest().getHeaders().getContentType().toString() : "";

        return Mono.fromCallable(() -> {
            Map<String, String> params = new HashMap<>();
            String bodyContent = new String(bodyBytes, StandardCharsets.UTF_8);
            
            try {
                if (contentType.contains("application/json")) {
                    // 解析JSON请求体
                    parseJsonParams(bodyContent, params);
                } else if (contentType.contains("application/x-www-form-urlencoded")) {
                    // 解析表单参数
                    parseFormParams(bodyContent, params);
                } else {
                    // 对于其他类型，尝试作为JSON解析
                    if (bodyContent.trim().startsWith("{")) {
                        parseJsonParams(bodyContent, params);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse request body as structured data, treating as raw content: {}", e.getMessage());
                // 如果解析失败，将整个body作为一个参数
                params.put("_body", bodyContent);
            }
            
            return params;
        }).onErrorReturn(new HashMap<>());
    }

    /**
     * 解析JSON参数
     */
    @SuppressWarnings("unchecked")
    private void parseJsonParams(String jsonContent, Map<String, String> params) {
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, Map.class);
            flattenJsonParams("", jsonMap, params);
        } catch (Exception e) {
            log.warn("Failed to parse JSON content: {}", e.getMessage());
            throw new RuntimeException("Invalid JSON format", e);
        }
    }

    /**
     * 扁平化JSON参数
     */
    private void flattenJsonParams(String prefix, Map<String, Object> map, Map<String, String> params) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                params.put(key, "");
            } else if (value instanceof Map) {
                // 递归处理嵌套对象
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenJsonParams(key, nestedMap, params);
            } else if (value instanceof List) {
                // 处理数组
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nestedMap = (Map<String, Object>) item;
                        flattenJsonParams(key + "[" + i + "]", nestedMap, params);
                    } else {
                        params.put(key + "[" + i + "]", item != null ? item.toString() : "");
                    }
                }
            } else {
                // 基本类型直接转换为字符串
                params.put(key, value.toString());
            }
        }
    }

    /**
     * 解析表单参数
     */
    private void parseFormParams(String formContent, Map<String, String> params) {
        String[] pairs = formContent.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    log.warn("Failed to decode form parameter: {}", pair);
                }
            }
        }
    }

    /**
     * 获取所有请求参数（包括URL参数和请求体参数）
     */
    private Map<String, String> getAllRequestParameters(ServerWebExchange exchange, Map<String, String> bodyParams) {
        Map<String, String> allParams = new HashMap<>();
        
        // 首先添加URL参数
        exchange.getRequest().getQueryParams().forEach((key, values) -> {
            if (!values.isEmpty() && StringUtils.hasText(values.get(0))) {
                allParams.put(key, values.get(0));
            }
        });

        // 然后添加请求体参数
        allParams.putAll(bodyParams);

        return allParams;
    }

    /**
     * 处理签名验证错误
     */
    private Mono<Void> handleSignatureError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "SIGNATURE_VERIFICATION_FAILED",
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        
        try {
            byte[] responseBytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBytes))
            );
        } catch (Exception e) {
            log.error("Error writing error response", e);
            return exchange.getResponse().setComplete();
        }
    }
}
