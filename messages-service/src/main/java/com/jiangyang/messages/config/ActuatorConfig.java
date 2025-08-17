package com.jiangyang.messages.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator配置类
 * 禁用数据源健康检查，避免与动态数据源冲突
 */
@Configuration
public class ActuatorConfig {
    // 通过配置文件禁用数据源健康检查
}
