package com.bgpay.bgai.config;

import com.jiangyang.dubbo.api.signature.SignatureService;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Dubbo服务配置类
 * 提供替代的Dubbo服务引用方式
 * 
 * @author jiangyang
 */
@Configuration
public class DubboServiceConfig {

    @Value("${dubbo.registry.address:nacos://8.133.246.113:8848}")
    private String registryAddress;

    @Value("${dubbo.registry.group:DEFAULT_GROUP}")
    private String registryGroup;

    @Value("${dubbo.application.name:bgai-service}")
    private String applicationName;

    @Value("${dubbo.application.version:1.0.0}")
    private String applicationVersion;

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
     * 配置SignatureService引用
     * 如果@DubboReference注解无法识别，可以使用这种方式
     * 注意：Dubbo 3.x中使用setRegistries而不是setRegistry
     */
    @Bean
    public ReferenceConfig<SignatureService> signatureServiceReference() {
        ReferenceConfig<SignatureService> reference = new ReferenceConfig<>();
        reference.setInterface(SignatureService.class);
        reference.setVersion("1.0.0");
        reference.setGroup("signature");
        reference.setTimeout(5000);
        reference.setRetries(2);
        reference.setLoadbalance("roundrobin");
        reference.setCluster("failover");
        reference.setCheck(false);
        reference.setLazy(true);
        // 在Dubbo 3.x中，使用setRegistries而不是setRegistry
        reference.setRegistries((List<? extends RegistryConfig>) registryConfig());
        reference.setApplication(applicationConfig());
        return reference;
    }

    /**
     * 获取SignatureService实例
     * 在需要的地方注入这个Bean
     */
    @Bean
    public SignatureService signatureService() {
        try {
            ReferenceConfig<SignatureService> reference = signatureServiceReference();
            return reference.get();
        } catch (Exception e) {
            // 如果获取失败，返回null，让Spring处理异常
            throw new RuntimeException("无法创建SignatureService: " + e.getMessage(), e);
        }
    }
}
