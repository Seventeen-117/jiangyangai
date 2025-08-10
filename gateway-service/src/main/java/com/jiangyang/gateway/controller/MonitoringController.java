package com.jiangyang.gateway.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网关监控控制器
 * 提供熔断器、限流器、重试等状态信息
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private RateLimiterRegistry rateLimiterRegistry;

    @Autowired(required = false)
    private RetryRegistry retryRegistry;

    /**
     * 获取所有熔断器状态
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakers() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("circuitBreakers", new HashMap<>());

        if (circuitBreakerRegistry != null) {
            Map<String, Object> breakers = circuitBreakerRegistry.getAllCircuitBreakers()
                    .stream()
                    .collect(Collectors.toMap(
                            CircuitBreaker::getName,
                            breaker -> {
                                Map<String, Object> breakerInfo = new HashMap<>();
                                breakerInfo.put("state", breaker.getState().name());
                                breakerInfo.put("failureRate", breaker.getMetrics().getFailureRate());
                                breakerInfo.put("slowCallRate", breaker.getMetrics().getSlowCallRate());
                                breakerInfo.put("numberOfFailedCalls", breaker.getMetrics().getNumberOfFailedCalls());
                                breakerInfo.put("numberOfSlowCalls", breaker.getMetrics().getNumberOfSlowCalls());
                                breakerInfo.put("numberOfSuccessfulCalls", breaker.getMetrics().getNumberOfSuccessfulCalls());
                                breakerInfo.put("numberOfNotPermittedCalls", breaker.getMetrics().getNumberOfNotPermittedCalls());
                                return breakerInfo;
                            }
                    ));
            response.put("circuitBreakers", breakers);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有限流器状态
     */
    @GetMapping("/rate-limiters")
    public ResponseEntity<Map<String, Object>> getRateLimiters() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("rateLimiters", new HashMap<>());

        if (rateLimiterRegistry != null) {
            Map<String, Object> limiters = rateLimiterRegistry.getAllRateLimiters()
                    .stream()
                    .collect(Collectors.toMap(
                            RateLimiter::getName,
                            limiter -> {
                                Map<String, Object> limiterInfo = new HashMap<>();
                                limiterInfo.put("availablePermissions", limiter.getMetrics().getAvailablePermissions());
                                limiterInfo.put("numberOfWaitingThreads", limiter.getMetrics().getNumberOfWaitingThreads());
                                return limiterInfo;
                            }
                    ));
            response.put("rateLimiters", limiters);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有重试器状态
     */
    @GetMapping("/retries")
    public ResponseEntity<Map<String, Object>> getRetries() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("retries", new HashMap<>());

        if (retryRegistry != null) {
            Map<String, Object> retries = retryRegistry.getAllRetries()
                    .stream()
                    .collect(Collectors.toMap(
                            Retry::getName,
                            retry -> {
                                Map<String, Object> retryInfo = new HashMap<>();
                                retryInfo.put("numberOfSuccessfulCallsWithoutRetryAttempt", 
                                    retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
                                retryInfo.put("numberOfSuccessfulCallsWithRetryAttempt", 
                                    retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
                                retryInfo.put("numberOfFailedCallsWithoutRetryAttempt", 
                                    retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
                                retryInfo.put("numberOfFailedCallsWithRetryAttempt", 
                                    retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
                                return retryInfo;
                            }
                    ));
            response.put("retries", retries);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取网关整体状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getGatewayStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("gateway", "gateway-service");
        response.put("status", "UP");
        
        Map<String, Object> components = new HashMap<>();
        components.put("circuitBreakerRegistry", circuitBreakerRegistry != null);
        components.put("rateLimiterRegistry", rateLimiterRegistry != null);
        components.put("retryRegistry", retryRegistry != null);
        response.put("components", components);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取特定熔断器状态
     */
    @GetMapping("/circuit-breakers/{name}")
    public ResponseEntity<Map<String, Object>> getCircuitBreaker(String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("name", name);

        if (circuitBreakerRegistry != null) {
            CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(name);
            if (breaker != null) {
                Map<String, Object> breakerInfo = new HashMap<>();
                breakerInfo.put("state", breaker.getState().name());
                breakerInfo.put("failureRate", breaker.getMetrics().getFailureRate());
                breakerInfo.put("slowCallRate", breaker.getMetrics().getSlowCallRate());
                breakerInfo.put("numberOfFailedCalls", breaker.getMetrics().getNumberOfFailedCalls());
                breakerInfo.put("numberOfSlowCalls", breaker.getMetrics().getNumberOfSlowCalls());
                breakerInfo.put("numberOfSuccessfulCalls", breaker.getMetrics().getNumberOfSuccessfulCalls());
                breakerInfo.put("numberOfNotPermittedCalls", breaker.getMetrics().getNumberOfNotPermittedCalls());
                response.put("circuitBreaker", breakerInfo);
            } else {
                response.put("error", "Circuit breaker not found: " + name);
            }
        } else {
            response.put("error", "Circuit breaker registry not available");
        }

        return ResponseEntity.ok(response);
    }
} 