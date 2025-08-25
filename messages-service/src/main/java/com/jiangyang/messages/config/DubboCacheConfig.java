package com.jiangyang.messages.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Dubbo缓存配置类
 * 用于设置Dubbo缓存控制系统属性，避免多实例缓存冲突
 */
@Component
public class DubboCacheConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DubboCacheConfig.class);

    /**
     * 在Bean初始化后设置Dubbo系统属性
     */
    @PostConstruct
    public void initDubboProperties() {
        log.info("初始化Dubbo缓存控制系统属性...");
        
        // 禁用所有文件缓存
        setSystemProperty("dubbo.cache.file", "false");
        setSystemProperty("dubbo.metadata.cache.file", "false");
        setSystemProperty("dubbo.registry.cache.file", "false");
        setSystemProperty("dubbo.consumer.cache.file", "false");
        setSystemProperty("dubbo.provider.cache.file", "false");
        
        // 设置缓存类型为内存
        setSystemProperty("dubbo.cache.type", "memory");
        setSystemProperty("dubbo.metadata.cache.type", "memory");
        
        // 设置唯一的缓存目录
        String userHome = System.getProperty("user.home");
        String cacheDir = userHome + "/.dubbo/messages-service";
        setSystemProperty("dubbo.cache.directory", cacheDir);
        setSystemProperty("dubbo.metadata.cache.directory", cacheDir + "/metadata");
        setSystemProperty("dubbo.registry.cache.directory", cacheDir + "/registry");
        setSystemProperty("dubbo.consumer.cache.directory", cacheDir + "/consumer");
        setSystemProperty("dubbo.provider.cache.directory", cacheDir + "/provider");
        
        // 设置元数据类型
        setSystemProperty("dubbo.metadata.type", "local");
        
        // 设置应用环境
        setSystemProperty("dubbo.application.environment", "develop");
        
        // 禁用监控和追踪
        setSystemProperty("dubbo.monitor.enabled", "false");
        setSystemProperty("dubbo.tracing.enabled", "false");
        setSystemProperty("dubbo.observability.enabled", "false");
        
        log.info("Dubbo缓存控制系统属性设置完成，缓存目录: {}", cacheDir);
    }

    /**
     * 设置系统属性
     */
    private void setSystemProperty(String key, String value) {
        try {
            System.setProperty(key, value);
            log.debug("设置系统属性: {} = {}", key, value);
        } catch (Exception e) {
            log.warn("设置系统属性失败: {} = {}, 错误: {}", key, value, e.getMessage());
        }
    }

    /**
     * 在应用启动后执行
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("Dubbo缓存配置初始化完成");
        
        // 验证关键系统属性
        log.info("验证Dubbo系统属性:");
        log.info("  dubbo.cache.file: {}", System.getProperty("dubbo.cache.file"));
        log.info("  dubbo.cache.type: {}", System.getProperty("dubbo.cache.type"));
        log.info("  dubbo.metadata.type: {}", System.getProperty("dubbo.metadata.type"));
        log.info("  dubbo.cache.directory: {}", System.getProperty("dubbo.cache.directory"));
    }
}
