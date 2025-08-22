package com.jiangyang.datacalculation.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * DeepSearch Service Dubbo配置类
 * 避免与其他服务的缓存冲突
 * 
 * @author jiangyang
 */
@Configuration
@EnableDubbo
public class DubboConfig {

    @Value("${dubbo.application.name}")
    private String applicationName;

    @Value("${dubbo.application.version}")
    private String applicationVersion;

    @Value("${dubbo.registry.address}")
    private String registryAddress;

    @Value("${dubbo.registry.group}")
    private String registryGroup;

    @Value("${dubbo.consumer.timeout}")
    private Integer consumerTimeout;

    @Value("${dubbo.consumer.retries}")
    private Integer consumerRetries;

    @Value("${dubbo.consumer.loadbalance}")
    private String consumerLoadbalance;

    @Value("${dubbo.consumer.cluster}")
    private String consumerCluster;

    @Value("${dubbo.consumer.check}")
    private Boolean consumerCheck;

    /**
     * 应用配置 - 设置唯一的应用名称
     */
    @Bean
    @Primary
    public ApplicationConfig applicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName(applicationName);
        config.setVersion(applicationVersion);
        config.setOwner("jiangyang");
        config.setOrganization("jiangyang-tech");
        
        // 设置QOS配置，避免端口冲突
        config.setQosEnable(false);
        config.setQosPort(22222);
        config.setQosAcceptForeignIp(false);
        
        return config;
    }

    /**
     * 注册中心配置
     */
    @Bean
    @Primary
    public RegistryConfig registryConfig() {
        RegistryConfig config = new RegistryConfig();
        config.setAddress(registryAddress);
        config.setGroup(registryGroup);
        config.setTimeout(10000);
        config.setCheck(false);
        return config;
    }

    /**
     * 消费者配置
     */
    @Bean
    @Primary
    public ConsumerConfig consumerConfig() {
        ConsumerConfig config = new ConsumerConfig();
        config.setTimeout(consumerTimeout);
        config.setRetries(consumerRetries);
        config.setLoadbalance(consumerLoadbalance);
        config.setCluster(consumerCluster);
        config.setCheck(consumerCheck);
        config.setConnections(1);
        config.setReconnect("true");
        return config;
    }
}
