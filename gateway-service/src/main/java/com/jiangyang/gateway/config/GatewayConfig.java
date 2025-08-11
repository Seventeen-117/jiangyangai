package com.jiangyang.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关配置类
 * 定义静态路由规则和负载均衡策略
 * 动态路由通过DynamicRouteService管理
 *
 * @author jiangyang
 * @since 2024-01-01
 */
@Configuration
public class GatewayConfig {

    /**
     * 配置静态路由规则
     * 这些是基础路由，动态路由通过API管理
     *
     * @param builder RouteLocatorBuilder
     * @return RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 健康检查路由
                .route("health-check", r -> r
                        .path("/actuator/health/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway-Source", "gateway-service"))
                        .uri("http://localhost:8080"))

                // 网关管理路由
                .route("gateway-management", r -> r
                        .path("/gateway/**")
                        .filters(f -> f
                                .addRequestHeader("X-Gateway-Source", "gateway-service"))
                        .uri("http://localhost:8080"))

                .build();
    }
} 