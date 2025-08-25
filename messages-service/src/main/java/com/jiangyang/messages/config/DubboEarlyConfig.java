package com.jiangyang.messages.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * Dubbo 早期配置类
 * 使用 @Order(1) 和 @DependsOn 确保在 Dubbo 初始化之前就设置好所有配置
 */
@Slf4j
@Configuration
@Order(1)
@ConditionalOnClass(name = "org.apache.dubbo.config.spring.context.annotation.EnableDubbo")
@DependsOn
public class DubboEarlyConfig {

    @PostConstruct
    public void init() {
        try {
            // 在 Bean 初始化后立即设置系统属性
            setDubboSystemProperties();
            log.info("Dubbo 早期配置初始化完成");
        } catch (Exception e) {
            log.error("Dubbo 早期配置初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 设置 Dubbo 系统属性
     */
    private void setDubboSystemProperties() {
        try {
            // 核心缓存禁用配置
            System.setProperty("dubbo.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.cache.file.enabled", "false");
            System.setProperty("dubbo.registry.cache.file.enabled", "false");
            System.setProperty("dubbo.service.name.mapping.enabled", "false");
            System.setProperty("dubbo.mapping.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.mapping.cache.file.enabled", "false");
            
            // 禁用服务发现缓存 - 针对 Nacos
            System.setProperty("dubbo.registry.use-as-config-center", "false");
            System.setProperty("dubbo.registry.use-as-metadata-center", "false");
            System.setProperty("dubbo.registry.cache.enabled", "false");
            System.setProperty("dubbo.registry.cache.file.enabled", "false");
            System.setProperty("dubbo.registry.cache.memory.enabled", "false");
            
            // 禁用元数据缓存 - 针对 Nacos 服务发现
            System.setProperty("dubbo.metadata.cache.enabled", "false");
            System.setProperty("dubbo.metadata.cache.file.enabled", "false");
            System.setProperty("dubbo.metadata.cache.memory.enabled", "false");
            System.setProperty("dubbo.metadata.cache.store.enabled", "false");
            System.setProperty("dubbo.metadata.cache.store.file.enabled", "false");
            
            // 禁用服务发现缓存
            System.setProperty("dubbo.service.discovery.cache.enabled", "false");
            System.setProperty("dubbo.service.discovery.cache.file.enabled", "false");
            System.setProperty("dubbo.service.discovery.cache.memory.enabled", "false");
            System.setProperty("dubbo.service.discovery.cache.store.enabled", "false");
            System.setProperty("dubbo.service.discovery.cache.store.file.enabled", "false");
            
            // 禁用 Nacos 特定的缓存
            System.setProperty("dubbo.nacos.cache.enabled", "false");
            System.setProperty("dubbo.nacos.cache.file.enabled", "false");
            System.setProperty("dubbo.nacos.cache.memory.enabled", "false");
            System.setProperty("dubbo.nacos.cache.store.enabled", "false");
            System.setProperty("dubbo.nacos.cache.store.file.enabled", "false");
            
            // 禁用监控和追踪
            System.setProperty("dubbo.monitor.enabled", "false");
            System.setProperty("dubbo.tracing.enabled", "false");
            System.setProperty("dubbo.observability.enabled", "false");
            
            // 设置元数据类型为本地
            System.setProperty("dubbo.metadata.type", "local");
            System.setProperty("dubbo.metadata.store.type", "local");
            
            // 禁用配置中心文件缓存
            System.setProperty("dubbo.config-center.file", "false");
            System.setProperty("dubbo.config-center.cache.enabled", "false");
            System.setProperty("dubbo.config-center.cache.file.enabled", "false");
            
            // 禁用 Dubbo 监控过滤器
            System.setProperty("dubbo.consumer.filter", "-monitor,-observationsender");
            System.setProperty("dubbo.provider.filter", "-monitor,-observationsender");
            System.setProperty("dubbo.cluster.filter", "-observationsender");
            
            // 为当前应用设置独立的缓存目录
            String tempDir = System.getProperty("java.io.tmpdir");
            String appCacheDir = tempDir + File.separator + "dubbo" + File.separator + "messages-service";
            File cacheDirectory = new File(appCacheDir);
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs();
            }
            System.setProperty("dubbo.cache.file.directory", appCacheDir);
            
            // 设置应用环境变量，确保唯一性
            System.setProperty("dubbo.application.environment", "dev");
            System.setProperty("dubbo.application.qos.enable", "false");
            
            // 设置应用名称，确保唯一性
            String appName = "messages-service-" + System.currentTimeMillis();
            System.setProperty("dubbo.application.name", appName);
            
            // 强制禁用所有缓存相关的功能
            System.setProperty("dubbo.cache.enabled", "false");
            System.setProperty("dubbo.cache.memory.enabled", "false");
            System.setProperty("dubbo.cache.store.enabled", "false");
            System.setProperty("dubbo.cache.store.file.enabled", "false");
            
            // 禁用文件存储
            System.setProperty("dubbo.file.store.enabled", "false");
            System.setProperty("dubbo.file.store.cache.enabled", "false");
            
            log.info("Dubbo 早期系统属性设置完成，应用名称: {}, 缓存目录: {}", appName, appCacheDir);
            
        } catch (Exception e) {
            log.error("设置 Dubbo 早期系统属性时发生错误: {}", e.getMessage(), e);
        }
    }
}
