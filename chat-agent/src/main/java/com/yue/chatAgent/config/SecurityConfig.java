package com.yue.chatAgent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 * 为 Spring AI 工具调用提供必要的安全配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll() // 允许所有请求
            )
            .csrf(csrf -> csrf.disable()) // 禁用CSRF
            .headers(headers -> headers.frameOptions().disable()); // 禁用X-Frame-Options
        
        return http.build();
    }
}
