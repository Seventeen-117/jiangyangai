package com.jiangyang.datacalculation.config;

import feign.Logger;
import feign.Request;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign客户端配置类
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@Configuration
public class FeignConfig {

    @Value("${service.ai-agent.timeout:30000}")
    private int aiAgentTimeout;

    @Value("${service.bgai.timeout:30000}")
    private int bgaiTimeout;

    /**
     * 配置Feign日志级别
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 配置AI代理服务Feign客户端选项
     */
    @Bean
    public Request.Options aiAgentRequestOptions() {
        return new Request.Options(
            aiAgentTimeout, 
            TimeUnit.MILLISECONDS, 
            aiAgentTimeout, 
            TimeUnit.MILLISECONDS, 
            true
        );
    }

    /**
     * 配置BGAI服务Feign客户端选项
     */
    @Bean
    public Request.Options bgaiRequestOptions() {
        return new Request.Options(
            bgaiTimeout, 
            TimeUnit.MILLISECONDS, 
            bgaiTimeout, 
            TimeUnit.MILLISECONDS, 
            true
        );
    }

    /**
     * 配置Feign编码器
     */
    @Bean
    public Encoder feignEncoder() {
        return new JacksonEncoder();
    }

    /**
     * 配置Feign解码器
     */
    @Bean
    public Decoder feignDecoder() {
        return new JacksonDecoder();
    }
}
