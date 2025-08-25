package com.jiangyang.testservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.jiangyang.testservice",
                "com.jiangyang.testservice.tools",
                "com.jiangyang.base"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.jiangyang\\.base\\.seata\\..*")
        }
)
public class TestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestServiceApplication.class, args);
    }
}

