package com.bgpay.bgai.config;

import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.BGAIService;
import com.bgpay.bgai.service.TransactionLogService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.FileProcessor;
import com.bgpay.bgai.service.impl.FallbackService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.bgpay.bgai.web.RequestAttributesProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import lombok.extern.slf4j.Slf4j;

/**
 * MockMvc测试配置类
 * 提供测试环境需要的Mock Bean
 */
@TestConfiguration
@Slf4j
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.data.redis.database=0"
})
public class MockMvcTestConfig {

    /**
     * 提供ApiConfigService的Mock实现
     */
    @Bean
    @Primary
    public ApiConfigService apiConfigService() {
        return org.mockito.Mockito.mock(ApiConfigService.class);
    }

    /**
     * 提供BGAIService的Mock实现
     */
    @Bean
    @Primary
    public BGAIService bgaiService() {
        return org.mockito.Mockito.mock(BGAIService.class);
    }

    /**
     * 提供TransactionLogService的Mock实现
     */
    @Bean
    @Primary
    public TransactionLogService transactionLogService() {
        return org.mockito.Mockito.mock(TransactionLogService.class);
    }

    /**
     * 提供DeepSeekService的Mock实现
     */
    @Bean
    @Primary
    public DeepSeekService deepSeekService() {
        return org.mockito.Mockito.mock(DeepSeekService.class);
    }

    /**
     * 提供FileProcessor的Mock实现
     */
    @Bean
    @Primary
    public FileProcessor fileProcessor() {
        return org.mockito.Mockito.mock(FileProcessor.class);
    }

    /**
     * 提供FallbackService的Mock实现
     */
    @Bean
    @Primary
    public FallbackService fallbackService() {
        return org.mockito.Mockito.mock(FallbackService.class);
    }

    /**
     * 提供TransactionCoordinator的Mock实现
     */
    @Bean
    @Primary
    public TransactionCoordinator transactionCoordinator() {
        return org.mockito.Mockito.mock(TransactionCoordinator.class);
    }

    /**
     * 提供RequestAttributesProvider的Mock实现
     */
    @Bean
    @Primary
    public RequestAttributesProvider requestAttributesProvider() {
        return org.mockito.Mockito.mock(RequestAttributesProvider.class);
    }
} 