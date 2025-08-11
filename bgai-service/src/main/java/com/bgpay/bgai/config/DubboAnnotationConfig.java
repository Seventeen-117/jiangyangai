package com.bgpay.bgai.config;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Dubbo注解配置类
 * 解决注解识别问题
 * 
 * @author jiangyang
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DubboAnnotationConfig {
    
    // 这个类主要用于确保Dubbo注解能够被正确识别
    // 如果仍然有问题，可以尝试以下解决方案：
    
    // 1. 清理IDE缓存：File -> Invalidate Caches and Restart
    // 2. 重新导入Maven项目：右键项目 -> Maven -> Reload Project
    // 3. 检查Maven依赖是否正确下载
    // 4. 确保Dubbo版本兼容性
    
    // 常见问题解决方案：
    // - 确保pom.xml中包含正确的Dubbo依赖
    // - 检查Spring Boot和Dubbo版本兼容性
    // - 验证包扫描配置是否正确
}
