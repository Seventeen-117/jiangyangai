package com.jiangyang.testservice.tests;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = {
        "com.jiangyang.base", // reuse base-service beans
        "com.jiangyang.testservice"
})
@ComponentScan(
        basePackages = {"com.jiangyang.base", "com.jiangyang.testservice"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.jiangyang\\.base\\.seata\\..*")
        }
)
@EnableDiscoveryClient
public class TestServiceTestApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(TestServiceTestApplication.class)
                .profiles("test")
                .run(args);
    }
}


