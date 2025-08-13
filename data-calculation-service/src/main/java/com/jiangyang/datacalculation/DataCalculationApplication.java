package com.jiangyang.datacalculation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 数据计算服务主应用类
 * 
 * @author jiangyang
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class DataCalculationApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCalculationApplication.class, args);
    }
}
