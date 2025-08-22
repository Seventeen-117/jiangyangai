package com.jiangyang.datacalculation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

/**
 * 深度搜索服务主应用类
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.jiangyang.datacalculation",
        "com.jiangyang.base"
    }
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.jiangyang.datacalculation")
@EnableDubbo
public class DataCalculationApplication {

    public static void main(String[] args) {
        // 在Spring Boot启动之前设置系统属性，确保最早生效
        setupSystemProperties();
        
        SpringApplication.run(DataCalculationApplication.class, args);
    }
    
    /**
     * 设置系统属性，禁用不必要的输出
     */
    private static void setupSystemProperties() {
        // 设置Dubbo缓存配置，避免与其他服务冲突
        setupDubboCacheConfig();
        
        // 禁用条件评估报告
        System.setProperty("logging.level.org.springframework.boot.autoconfigure.condition", "OFF");
        System.setProperty("logging.level.org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener", "OFF");
        
        // 禁用启动信息
        System.setProperty("spring.main.log-startup-info", "false");
        
        // 禁用banner
        System.setProperty("spring.main.banner-mode", "off");
        
        System.out.println("系统属性已设置，条件评估报告已禁用");
    }
    
    /**
     * 设置Dubbo缓存配置，避免与其他服务冲突
     */
    private static void setupDubboCacheConfig() {
        String userHome = System.getProperty("user.home");
        String cachePath = userHome + "/.dubbo/deepSearch-service";
        
        // 设置Dubbo缓存相关系统属性
        System.setProperty("dubbo.cache.file.path", cachePath);
        System.setProperty("dubbo.metadata.cache.file.path", cachePath + "/metadata");
        System.setProperty("dubbo.service.discovery.cache.file.path", cachePath + "/discovery");
        System.setProperty("dubbo.config.cache.file.path", cachePath + "/config");
        System.setProperty("dubbo.registry.cache.file.path", cachePath + "/registry");
        System.setProperty("dubbo.mapping.cache.file.path", cachePath + "/mapping");
        System.setProperty("dubbo.application.cache.file.path", cachePath + "/application");
        System.setProperty("dubbo.service.name.mapping.cache.file.path", cachePath + "/service-mapping");
        System.setProperty("dubbo.metadata.service.name.mapping.cache.file.path", cachePath + "/metadata-service-mapping");
        
        // 设置缓存存储类型为内存，避免文件冲突
        System.setProperty("dubbo.cache.store.type", "memory");
        System.setProperty("dubbo.metadata.cache.store.type", "memory");
        System.setProperty("dubbo.service.discovery.cache.store.type", "memory");
        
        // 设置应用名称，确保唯一性
        System.setProperty("dubbo.application.name", "deepSearch-service");
        
        // 禁用QOS，避免端口冲突
        System.setProperty("dubbo.application.qos-enable", "false");
        System.setProperty("dubbo.application.qos-port", "22222");
        System.setProperty("dubbo.application.qos-accept-foreign-ip", "false");
        
        System.out.println("Dubbo缓存配置已设置，路径: " + cachePath);
        System.out.println("Dubbo缓存存储类型: " + System.getProperty("dubbo.cache.store.type"));
    }
}
