package com.jiangyang.messages.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Saga 模块 Mapper 扫描配置，绑定到默认的 sqlSessionFactory。
 * 使用 base-service 提供的 MyBatis-Plus 配置与动态数据源。
 */
@Configuration
@MapperScan(basePackages = {
        "com.jiangyang.messages.saga.mapper"
}, sqlSessionFactoryRef = "sqlSessionFactory")
public class SagaMapperConfig {
}


