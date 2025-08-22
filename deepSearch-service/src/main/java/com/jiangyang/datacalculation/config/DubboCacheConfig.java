package com.jiangyang.datacalculation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.PostConstruct;

/**
 * Dubbo缓存配置类
 * 确保deepSearch-service使用唯一的缓存路径，避免与其他服务冲突
 * 
 * @author jiangyang
 */
@Configuration
@DependsOn("dubboConfig")
public class DubboCacheConfig {

    @PostConstruct
    public void init() {
        // 设置Dubbo缓存相关系统属性，避免与其他服务冲突
        String userHome = System.getProperty("user.home");
        String cachePath = userHome + "/.dubbo/deepSearch-service";
        
        // 设置缓存文件路径
        System.setProperty("dubbo.cache.file.path", cachePath);
        
        // 设置元数据缓存路径
        System.setProperty("dubbo.metadata.cache.file.path", cachePath + "/metadata");
        
        // 设置服务发现缓存路径
        System.setProperty("dubbo.service.discovery.cache.file.path", cachePath + "/discovery");
        
        // 设置配置中心缓存路径
        System.setProperty("dubbo.config.cache.file.path", cachePath + "/config");
        
        // 设置注册中心缓存路径
        System.setProperty("dubbo.registry.cache.file.path", cachePath + "/registry");
        
        // 设置映射缓存路径 - 这是关键配置
        System.setProperty("dubbo.mapping.cache.file.path", cachePath + "/mapping");
        
        // 设置应用名称相关的缓存路径
        System.setProperty("dubbo.application.cache.file.path", cachePath + "/application");
        
        // 设置服务名称映射缓存路径
        System.setProperty("dubbo.service.name.mapping.cache.file.path", cachePath + "/service-mapping");
        
        // 设置元数据服务名称映射缓存路径
        System.setProperty("dubbo.metadata.service.name.mapping.cache.file.path", cachePath + "/metadata-service-mapping");
        
        // 禁用QOS，避免端口冲突
        System.setProperty("dubbo.application.qos-enable", "false");
        System.setProperty("dubbo.application.qos-port", "22222");
        System.setProperty("dubbo.application.qos-accept-foreign-ip", "false");
        
        // 设置应用名称，确保唯一性
        System.setProperty("dubbo.application.name", "deepSearch-service");
        
        // 设置缓存存储类型为内存，避免文件冲突
        System.setProperty("dubbo.cache.store.type", "memory");
        
        // 设置元数据缓存存储类型
        System.setProperty("dubbo.metadata.cache.store.type", "memory");
        
        // 设置服务发现缓存存储类型
        System.setProperty("dubbo.service.discovery.cache.store.type", "memory");
        
        System.out.println("Dubbo缓存路径已设置为: " + System.getProperty("dubbo.cache.file.path"));
        System.out.println("Dubbo缓存存储类型已设置为: " + System.getProperty("dubbo.cache.store.type"));
    }
}
