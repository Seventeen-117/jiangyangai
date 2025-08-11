package com.bgpay.bgai.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo配置类
 * 
 * @author jiangyang
 */
@Configuration
@EnableDubbo
public class DubboConfig {

    @Value("${dubbo.application.name:bgai-service}")
    private String applicationName;

    @Value("${dubbo.application.version:1.0.0}")
    private String applicationVersion;

    @Value("${dubbo.registry.address:nacos://8.133.246.113:8848}")
    private String registryAddress;

    @Value("${dubbo.registry.group:DEFAULT_GROUP}")
    private String registryGroup;

    @Value("${dubbo.consumer.timeout:5000}")
    private Integer consumerTimeout;

    @Value("${dubbo.consumer.retries:2}")
    private Integer consumerRetries;

    @Value("${dubbo.consumer.loadbalance:roundrobin}")
    private String consumerLoadbalance;

    @Value("${dubbo.consumer.cluster:failover}")
    private String consumerCluster;

    @Value("${dubbo.consumer.check:false}")
    private Boolean consumerCheck;

    /**
     * 应用配置
     */
    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName(applicationName);
        config.setVersion(applicationVersion);
        config.setOwner("jiangyang");
        config.setOrganization("jiangyang-tech");
        return config;
    }

    /**
     * 注册中心配置
     * 注意：Dubbo 3.x中某些方法已被移除
     */
    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig config = new RegistryConfig();
        config.setAddress(registryAddress);
        // 在Dubbo 3.x中，namespace通过address参数传递
        // 例如：nacos://8.133.246.113:8848?namespace=dubbo
        config.setGroup(registryGroup);
        config.setTimeout(10000);
        config.setCheck(false);
        return config;
    }

    /**
     * 消费者配置
     * 注意：Dubbo 3.x中某些方法已被移除
     */
    @Bean
    public ConsumerConfig consumerConfig() {
        ConsumerConfig config = new ConsumerConfig();
        config.setTimeout(consumerTimeout);
        config.setRetries(consumerRetries);
        config.setLoadbalance(consumerLoadbalance);
        config.setCluster(consumerCluster);
        config.setCheck(consumerCheck);
        // 在Dubbo 3.x中，serialization通过protocol配置或全局配置设置
        // 添加重试策略
        config.setRetries(2);
        // 添加负载均衡策略
        config.setLoadbalance("roundrobin");
        // 添加集群容错策略
        config.setCluster("failover");
        // 添加超时配置
        config.setTimeout(5000);
        // 添加连接超时配置
        config.setConnections(1);
        // 添加重连配置
        config.setReconnect("true");
        return config;
    }
}
