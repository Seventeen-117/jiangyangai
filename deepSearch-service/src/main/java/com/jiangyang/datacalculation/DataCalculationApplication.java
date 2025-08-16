package com.jiangyang.datacalculation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 深度搜索服务主应用类
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.jiangyang.datacalculation",
        "com.jiangyang.base"
    }
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.jiangyang.datacalculation")
public class DataCalculationApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCalculationApplication.class, args);
    }
}
