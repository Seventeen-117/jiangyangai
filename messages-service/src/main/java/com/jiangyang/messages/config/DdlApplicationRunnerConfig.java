package com.jiangyang.messages.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DDL应用运行器配置
 * 提供一个空的ApplicationRunner来替代缺失的ddlApplicationRunner
 */
@Configuration
public class DdlApplicationRunnerConfig {

    /**
     * 提供一个空的ApplicationRunner来替代缺失的ddlApplicationRunner
     */
    @Bean(name = "ddlApplicationRunner")
    public ApplicationRunner ddlApplicationRunner() {
        return args -> {
            // 空实现，只是为了满足Spring Boot的Bean要求
        };
    }
}
