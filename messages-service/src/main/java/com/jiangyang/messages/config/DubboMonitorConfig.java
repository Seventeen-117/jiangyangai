package com.jiangyang.messages.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo监控配置类
 * 禁用Dubbo监控功能，避免MonitorService连接错误
 * 
 * @author jiangyang
 * @version 1.0.0
 */
@Configuration
public class DubboMonitorConfig {

    /**
     * 配置Dubbo监控，禁用监控功能
     */
    @Bean
    public MonitorConfig monitorConfig() {
        MonitorConfig monitorConfig = new MonitorConfig();
        // 设置协议为registry但不启用
        monitorConfig.setProtocol("registry");
        // 通过设置地址为空来禁用监控
        monitorConfig.setAddress("");
        return monitorConfig;
    }
}
