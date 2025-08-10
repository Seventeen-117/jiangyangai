package com.jiangyang.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.jiangyang.gateway.config.CustomGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防御性策略过滤器
 * 实现防爬虫、防重放攻击、请求体校验等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefensiveFilter implements GlobalFilter, Ordered {

    private final CustomGatewayProperties gatewayProperties;
    
    // 缓存已使用的nonce，防止重放攻击
    private final Map<String, Long> usedNonces = new ConcurrentHashMap<>();
    
    // 缓存IP访问频率，防止爬虫
    private final Map<String, AccessRecord> ipAccessRecords = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String clientIp = getClientIp(request);
        
        log.debug("DefensiveFilter processing path: {}, clientIp: {}", path, clientIp);
        
        // 跳过不需要防御性检查的路径
        if (isSkipDefensiveCheck(path)) {
            log.debug("Skipping defensive check for path: {}", path);
            return chain.filter(exchange);
        }
        
        // 1. 防爬虫检查
        if (gatewayProperties.getDefensiveConfig().isEnableAntiCrawler() && isCrawlerAttack(clientIp, path)) {
            log.warn("Crawler attack detected for path: {}, clientIp: {}", path, clientIp);
            return blockRequest(exchange, "Crawler attack detected");
        }
        
        // 2. 防重放攻击检查
        if (gatewayProperties.getDefensiveConfig().isEnableAntiReplay() && isReplayAttack(request)) {
            log.warn("Replay attack detected for path: {}", path);
            return blockRequest(exchange, "Replay attack detected");
        }
        
        // 3. 请求体校验
        if (gatewayProperties.getDefensiveConfig().isEnableMaliciousContentDetection() && hasMaliciousContent(request)) {
            log.warn("Malicious content detected for path: {}", path);
            return blockRequest(exchange, "Malicious content detected");
        }
        
        // 4. 更新访问记录
        updateAccessRecord(clientIp, path);
        
        log.debug("DefensiveFilter passed for path: {}", path);
        return chain.filter(exchange);
    }

    /**
     * 判断是否需要跳过防御性检查
     */
    private boolean isSkipDefensiveCheck(String path) {
        // 从配置中读取白名单路径
        if (gatewayProperties.getSkipDefensivePaths() != null) {
            return gatewayProperties.getSkipDefensivePaths().stream()
                    .anyMatch(skipPath -> {
                        // 移除通配符进行匹配
                        String cleanSkipPath = skipPath.replace("**", "");
                        return path.startsWith(cleanSkipPath);
                    });
        }
        return false;
    }

    /**
     * 防爬虫检查
     */
    private boolean isCrawlerAttack(String clientIp, String path) {
        AccessRecord record = ipAccessRecords.get(clientIp);
        if (record == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeWindow = 60000; // 1分钟窗口
        
        // 清理过期记录
        record.cleanupExpired(currentTime - timeWindow);
        
        // 检查访问频率
        int requestCount = record.getRequestCount();
        int maxRequests = gatewayProperties.getDefensiveConfig().getMaxRequestsPerMinute();
        
        if (requestCount > maxRequests) {
            log.warn("Crawler attack detected from IP: {}, requests: {}", clientIp, requestCount);
            return true;
        }
        
        return false;
    }

    /**
     * 防重放攻击检查
     */
    private boolean isReplayAttack(ServerHttpRequest request) {
        String nonce = request.getHeaders().getFirst("X-Nonce");
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        
        if (!StringUtils.hasText(nonce) || !StringUtils.hasText(timestamp)) {
            // 如果没有nonce和timestamp，不进行重放攻击检查
            return false;
        }
        
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = Instant.now().getEpochSecond();
            
            // 检查时间戳是否在有效范围内（5分钟）
            if (Math.abs(currentTime - requestTime) > 300) {
                log.warn("Request timestamp expired: {}", timestamp);
                return true;
            }
            
            // 检查nonce是否已被使用
            String nonceKey = nonce + "_" + timestamp;
            if (usedNonces.containsKey(nonceKey)) {
                log.warn("Replay attack detected with nonce: {}", nonce);
                return true;
            }
            
            // 记录已使用的nonce
            usedNonces.put(nonceKey, currentTime);
            
            // 清理过期的nonce记录
            cleanupExpiredNonces(currentTime - 300);
            
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return true;
        }
        
        return false;
    }

    /**
     * 请求体恶意内容检查
     */
    private boolean hasMaliciousContent(ServerHttpRequest request) {
        // 检查User-Agent
        String userAgent = request.getHeaders().getFirst("User-Agent");
        if (StringUtils.hasText(userAgent)) {
            String lowerUserAgent = userAgent.toLowerCase();
            
            // 检查常见的恶意User-Agent
            String[] maliciousPatterns = {
                "sqlmap", "nmap", "nikto", "dirbuster", "gobuster",
                "w3af", "burp", "zap", "acunetix", "nessus"
            };
            
            for (String pattern : maliciousPatterns) {
                if (lowerUserAgent.contains(pattern)) {
                    log.warn("Malicious User-Agent detected: {}", userAgent);
                    return true;
                }
            }
        }
        
        // 检查请求参数中的SQL注入
        String query = request.getURI().getQuery();
        if (StringUtils.hasText(query)) {
            if (containsSqlInjection(query)) {
                log.warn("SQL injection attempt detected in query: {}", query);
                return true;
            }
        }
        
        return false;
    }

    /**
     * 检查是否包含SQL注入
     */
    private boolean containsSqlInjection(String input) {
        String lowerInput = input.toLowerCase();
        
        // SQL注入检测模式
        String[] sqlPatterns = {
            "union select", "union all select", "drop table", "delete from",
            "insert into", "update set", "alter table", "create table",
            "exec ", "execute ", "xp_", "sp_", "waitfor delay",
            "benchmark(", "sleep(", "load_file(", "into outfile",
            "information_schema", "sys.tables", "sys.columns"
        };
        
        for (String pattern : sqlPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 更新访问记录
     */
    private void updateAccessRecord(String clientIp, String path) {
        ipAccessRecords.compute(clientIp, (key, record) -> {
            if (record == null) {
                record = new AccessRecord();
            }
            record.addRequest(path, System.currentTimeMillis());
            return record;
        });
    }

    /**
     * 清理过期的nonce记录
     */
    private void cleanupExpiredNonces(long expireTime) {
        usedNonces.entrySet().removeIf(entry -> entry.getValue() < expireTime);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }

    /**
     * 阻止请求
     */
    private Mono<Void> blockRequest(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", reason);
        result.put("timestamp", System.currentTimeMillis());

        byte[] bytes = JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -40; // 在请求转换过滤器之前执行
    }

    /**
     * 访问记录类
     */
    private static class AccessRecord {
        private final Map<String, Long> requests = new ConcurrentHashMap<>();
        
        public void addRequest(String path, long timestamp) {
            requests.put(path + "_" + timestamp, timestamp);
        }
        
        public int getRequestCount() {
            return requests.size();
        }
        
        public void cleanupExpired(long expireTime) {
            requests.entrySet().removeIf(entry -> entry.getValue() < expireTime);
        }
    }
} 