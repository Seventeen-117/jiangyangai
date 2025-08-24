package com.jiangyang.messages.audit.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 审计日志模块 Mapper 扫描配置
 * 使用 base-service 提供的 MyBatis-Plus 配置与动态数据源
 */
@Configuration
@MapperScan(basePackages = {
        "com.jiangyang.messages.audit.mapper"
}, sqlSessionFactoryRef = "sqlSessionFactory")
public class AuditMapperConfig {
}
