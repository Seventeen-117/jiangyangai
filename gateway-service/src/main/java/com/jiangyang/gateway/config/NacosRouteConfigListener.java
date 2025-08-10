package com.jiangyang.gateway.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiangyang.gateway.service.DynamicRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Nacos路由配置监听器
 * 监听Nacos中的路由配置变化，自动更新Gateway路由
 * 
 * @author jiangyang
 * @since 2024-01-01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "gateway.dynamic-routes.enabled", havingValue = "true", matchIfMissing = false)
public class NacosRouteConfigListener {

    private static final String ROUTE_DATA_ID = "gateway-routes";
    private static final String ROUTE_GROUP = "DEFAULT_GROUP";

    @Value("${spring.cloud.nacos.config.server-addr:8.133.246.113:8848}")
    private String serverAddr;
    
    @Value("${spring.cloud.nacos.config.namespace:d750d92e-152f-4055-a641-3bc9dda85a29}")
    private String namespace;

    @Autowired
    private DynamicRouteService dynamicRouteService;
    
    @Autowired
    private ApplicationEventPublisher publisher;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("初始化Nacos路由配置监听器");
        try {
            // 初始化Nacos配置服务
            Properties properties = new Properties();
            properties.setProperty("serverAddr", serverAddr);
            if (namespace != null && !namespace.isEmpty()) {
                properties.setProperty("namespace", namespace);
            }
            ConfigService configService = NacosFactory.createConfigService(properties);
            
            // 首次加载配置
            String configInfo = configService.getConfig(ROUTE_DATA_ID, ROUTE_GROUP, 5000);
            if (configInfo != null && !configInfo.isEmpty()) {
                log.info("首次加载Nacos路由配置");
                updateRouteDefinitions(configInfo);
            } else {
                log.warn("Nacos中未找到路由配置数据");
            }
            
            // 注册监听器，监听配置变化
            configService.addListener(ROUTE_DATA_ID, ROUTE_GROUP, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null; // 使用默认的执行器
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("接收到Nacos路由配置变更通知");
                    updateRouteDefinitions(configInfo);
                }
            });
            
        } catch (NacosException e) {
            log.error("初始化Nacos路由配置监听器失败", e);
        }
    }

    /**
     * 更新路由定义
     */
    private void updateRouteDefinitions(String configInfo) {
        try {
            // 将JSON字符串转换为路由定义列表
            List<RouteDefinition> routeDefinitions = objectMapper.readValue(
                    configInfo, new TypeReference<List<RouteDefinition>>() {});
            
            log.info("从Nacos加载到{}条路由配置", routeDefinitions.size());
            
            // 更新路由
            for (RouteDefinition definition : routeDefinitions) {
                dynamicRouteService.update(definition).subscribe();
            }
            
            // 发布Spring Cloud Gateway的刷新路由事件
            publisher.publishEvent(new RefreshRoutesEvent(this));
            
        } catch (Exception e) {
            log.error("解析或更新路由配置失败", e);
        }
    }
} 