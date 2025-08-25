package com.jiangyang.messages.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Properties;

/**
 * Dubbo 启动配置类
 * 确保在 Dubbo 启动之前就设置好所有必要的系统属性
 */
@Slf4j
@Configuration
public class DubboBootstrapConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @PostConstruct
    public void init() {
        try {
            // 在 Bean 初始化后立即设置系统属性
            setDubboSystemProperties();
            log.info("Dubbo 启动配置初始化完成");
        } catch (Exception e) {
            log.error("Dubbo 启动配置初始化失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        try {
            // 在应用环境准备完成后，再次确保系统属性设置
            setDubboSystemProperties();
            
            // 将 Dubbo 配置添加到环境属性中
            addDubboPropertiesToEnvironment(event.getEnvironment());
            
            log.info("Dubbo 环境配置完成");
        } catch (Exception e) {
            log.error("Dubbo 环境配置失败: {}", e.getMessage(), e);
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
            
            log.info("Dubbo 系统属性设置完成，应用名称: {}, 缓存目录: {}", appName, appCacheDir);
            
        } catch (Exception e) {
            log.error("设置 Dubbo 系统属性时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 将 Dubbo 配置添加到环境属性中
     */
    private void addDubboPropertiesToEnvironment(ConfigurableEnvironment environment) {
        try {
            Properties dubboProps = new Properties();
            
            // 添加 Dubbo 应用配置
            dubboProps.setProperty("dubbo.application.name", System.getProperty("dubbo.application.name"));
            dubboProps.setProperty("dubbo.application.qos-enable", "false");
            dubboProps.setProperty("dubbo.application.metadata-type", "local");
            dubboProps.setProperty("dubbo.application.cache-file", "false");
            dubboProps.setProperty("dubbo.application.environment", "develop");
            
            // 添加 Dubbo 协议配置
            dubboProps.setProperty("dubbo.protocol.name", "dubbo");
            dubboProps.setProperty("dubbo.protocol.port", "-1");
            dubboProps.setProperty("dubbo.protocol.cache-file", "false");
            
            // 添加 Dubbo 注册中心配置
            dubboProps.setProperty("dubbo.registry.address", "nacos://8.133.246.113:8848");
            dubboProps.setProperty("dubbo.registry.timeout", "10000");
            dubboProps.setProperty("dubbo.registry.cache-file", "false");
            dubboProps.setProperty("dubbo.registry.file", "false");
            dubboProps.setProperty("dubbo.registry.use-as-config-center", "false");
            dubboProps.setProperty("dubbo.registry.use-as-metadata-center", "false");
            dubboProps.setProperty("dubbo.registry.cache.enabled", "false");
            dubboProps.setProperty("dubbo.registry.cache.memory.enabled", "false");
            
            // 添加 Dubbo 消费者配置
            dubboProps.setProperty("dubbo.consumer.timeout", "5000");
            dubboProps.setProperty("dubbo.consumer.retries", "2");
            dubboProps.setProperty("dubbo.consumer.check", "false");
            dubboProps.setProperty("dubbo.consumer.filter", "-monitor,-observationsender");
            dubboProps.setProperty("dubbo.consumer.cache-file", "false");
            dubboProps.setProperty("dubbo.consumer.metadata-type", "local");
            
            // 添加 Dubbo 提供者配置
            dubboProps.setProperty("dubbo.provider.timeout", "5000");
            dubboProps.setProperty("dubbo.provider.retries", "2");
            dubboProps.setProperty("dubbo.provider.cache-file", "false");
            dubboProps.setProperty("dubbo.provider.metadata-type", "local");
            dubboProps.setProperty("dubbo.provider.filter", "-monitor,-observationsender");
            
            // 添加 Dubbo 缓存配置
            dubboProps.setProperty("dubbo.cache.file.enabled", "false");
            dubboProps.setProperty("dubbo.cache.memory.enabled", "false");
            dubboProps.setProperty("dubbo.cache.store.enabled", "false");
            dubboProps.setProperty("dubbo.cache.store.file.enabled", "false");
            
            // 添加 Dubbo 元数据缓存配置
            dubboProps.setProperty("dubbo.metadata.cache.enabled", "false");
            dubboProps.setProperty("dubbo.metadata.cache.file.enabled", "false");
            dubboProps.setProperty("dubbo.metadata.cache.memory.enabled", "false");
            dubboProps.setProperty("dubbo.metadata.cache.store.enabled", "false");
            dubboProps.setProperty("dubbo.metadata.cache.store.file.enabled", "false");
            dubboProps.setProperty("dubbo.metadata.store.type", "local");
            
            // 添加 Dubbo 服务发现缓存配置
            dubboProps.setProperty("dubbo.service.discovery.cache.enabled", "false");
            dubboProps.setProperty("dubbo.service.discovery.cache.file.enabled", "false");
            dubboProps.setProperty("dubbo.service.discovery.cache.memory.enabled", "false");
            dubboProps.setProperty("dubbo.service.discovery.cache.store.enabled", "false");
            dubboProps.setProperty("dubbo.service.discovery.cache.store.file.enabled", "false");
            
            // 添加 Dubbo Nacos 缓存配置
            dubboProps.setProperty("dubbo.nacos.cache.enabled", "false");
            dubboProps.setProperty("dubbo.nacos.cache.file.enabled", "false");
            dubboProps.setProperty("dubbo.nacos.cache.memory.enabled", "false");
            dubboProps.setProperty("dubbo.nacos.cache.store.enabled", "false");
            dubboProps.setProperty("dubbo.nacos.cache.store.file.enabled", "false");
            
            // 添加 Dubbo 服务名称映射配置
            dubboProps.setProperty("dubbo.service-name-mapping.enabled", "false");
            dubboProps.setProperty("dubbo.service-name-mapping.cache.file.enabled", "false");
            
            // 创建属性源并添加到环境中
            PropertiesPropertySource dubboPropertySource = new PropertiesPropertySource("dubbo-bootstrap", dubboProps);
            MutablePropertySources propertySources = environment.getPropertySources();
            
            // 将 Dubbo 配置添加到环境属性的最前面，确保优先级
            propertySources.addFirst(dubboPropertySource);
            
            log.info("Dubbo 配置已添加到环境属性中");
            
        } catch (Exception e) {
            log.error("添加 Dubbo 配置到环境属性时发生错误: {}", e.getMessage(), e);
        }
    }
}
