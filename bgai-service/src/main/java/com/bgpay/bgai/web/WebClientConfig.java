package com.bgpay.bgai.web;

import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebClient配置类，支持服务发现和动态路由
 */
@Slf4j
@Configuration
@ConditionalOnClass(WebClient.class)
public class WebClientConfig {

    // 条件化注入，只有在LoadBalancer可用时才注入
    @Autowired(required = false)
    private ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter;
    
    @Autowired(required = false)
    private LoadBalancerClient loadBalancerClient;

    /**
     * 配置默认的WebClient，用于调用DeepSeek API
     */
    @Bean
    @Primary
    public WebClient webClient(WebClient.Builder builder) {
        // 配置内存限制，支持大响应体
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
                
        return builder
                .baseUrl("https://api.deepseek.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .filter(logRequest())
                .build();
    }
    
    /**
     * 配置支持服务发现的WebClient，用于调用注册到Nacos的服务
     */
    @Bean(name = "loadBalancedWebClient")
    public WebClient loadBalancedWebClient(WebClient.Builder builder) {
        WebClient.Builder webClientBuilder = builder.filter(logRequest());
        
        // 只有在LoadBalancer可用时才添加负载均衡过滤器
        if (loadBalancerFilter != null) {
            webClientBuilder = webClientBuilder.filter(loadBalancerFilter);
        }
        
        return webClientBuilder.build();
    }
    
    /**
     * 创建直接使用服务发现的WebClient构建器
     * 适用于需要动态指定服务名称的场景
     */
    @Bean
    public WebClient.Builder discoveryWebClientBuilder() {
        return WebClient.builder()
                .filter(logRequest());
    }
    
    /**
     * 日志请求过滤器，记录请求详情
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", 
                    clientRequest.method(), 
                    clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
    
    /**
     * 根据服务ID获取服务URL
     * 用于非响应式环境中获取服务地址
     * 
     * @param serviceId 服务ID
     * @return 服务地址
     */
    public String getServiceUrl(String serviceId) {
        if (loadBalancerClient == null) {
            log.warn("LoadBalancerClient not available, returning service ID as URL: {}", serviceId);
            return serviceId; // 返回服务ID作为URL，让调用方处理
        }
        
        ServiceInstance serviceInstance = loadBalancerClient.choose(serviceId);
        if (serviceInstance == null) {
            throw new IllegalStateException("No instances available for service: " + serviceId);
        }
        
        return UriComponentsBuilder.fromUri(serviceInstance.getUri())
                .build()
                .toUriString();
    }
    
    /**
     * 创建针对特定服务的WebClient
     * 
     * @param serviceId 服务ID
     * @return 配置好的WebClient
     */
    public WebClient createWebClientForService(String serviceId) {
        WebClient.Builder builder = WebClient.builder()
                .filter(logRequest());
        
        // 只有在LoadBalancer可用时才使用lb://协议和负载均衡过滤器
        if (loadBalancerFilter != null) {
            builder = builder.baseUrl("lb://" + serviceId)
                    .filter(loadBalancerFilter);
        } else {
            log.warn("LoadBalancer not available, using service ID as base URL: {}", serviceId);
            builder = builder.baseUrl(serviceId);
        }
        
        return builder.build();
    }
}