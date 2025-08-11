package com.bgpay.bgai.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 事务事件Dubbo配置类
 * 
 * @author jiangyang
 */
@Configuration
public class TransactionEventDubboConfig {

    @Value("${dubbo.application.name:bgai-service}")
    private String applicationName;

    @Value("${dubbo.application.version:1.0.0}")
    private String applicationVersion;

    @Value("${dubbo.registry.address:nacos://8.133.246.113:8848}")
    private String registryAddress;

    @Value("${dubbo.registry.group:DEFAULT_GROUP}")
    private String registryGroup;

    @Value("${dubbo.protocol.port:20880}")
    private Integer protocolPort;

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
     * 注意：Dubbo 3.x中namespace通过address参数传递
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
     * 协议配置
     * 注意：Dubbo 3.x中某些方法已被移除
     */
    @Bean
    public ProtocolConfig protocolConfig() {
        ProtocolConfig config = new ProtocolConfig();
        config.setName("dubbo");
        config.setPort(protocolPort);
        config.setHost("localhost");
        config.setThreads(200);
        config.setAccepts(0);
        config.setDispatcher("message");
        return config;
    }
}
