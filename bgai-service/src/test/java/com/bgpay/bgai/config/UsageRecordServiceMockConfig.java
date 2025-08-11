package com.bgpay.bgai.config;

import com.bgpay.bgai.service.UsageRecordService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class UsageRecordServiceMockConfig {
    @Bean
    @Primary
    public UsageRecordService usageRecordService() {
        return Mockito.mock(UsageRecordService.class);
    }
} 