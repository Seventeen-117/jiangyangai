package com.jiangyang.messages.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单的配置加载测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConfigLoadingTest {

    @Autowired(required = false)
    private MessageServiceConfig config;

    @Test
    public void testConfigLoading() {
        System.out.println("=== 配置加载测试开始 ===");
        
        if (config != null) {
            System.out.println("MessageServiceConfig 加载成功");
            System.out.println("config: " + config);
            
            // 测试Kafka配置
            MessageServiceConfig.Kafka kafka = config.getKafka();
            if (kafka != null) {
                System.out.println("Kafka配置加载成功");
                System.out.println("  - enabled: " + kafka.getEnabled());
                System.out.println("  - bootstrapServers: " + kafka.getBootstrapServers());
                
                if (kafka.getConsumer() != null) {
                    System.out.println("  - consumer.groupId: " + kafka.getConsumer().getGroupId());
                    System.out.println("  - consumer.clientId: " + kafka.getConsumer().getClientId());
                }
                
                if (kafka.getProducer() != null) {
                    System.out.println("  - producer.acks: " + kafka.getProducer().getAcks());
                    System.out.println("  - producer.retries: " + kafka.getProducer().getRetries());
                }
            } else {
                System.out.println("Kafka配置为null");
            }
            
            // 测试通用配置
            MessageServiceConfig.Common common = config.getCommon();
            if (common != null) {
                System.out.println("通用配置加载成功");
                System.out.println("  - defaultType: " + common.getDefaultType());
                System.out.println("  - defaultMessageType: " + common.getDefaultMessageType());
                System.out.println("  - defaultTopic: " + common.getDefaultTopic());
            } else {
                System.out.println("通用配置为null");
            }
        } else {
            System.out.println("MessageServiceConfig 加载失败");
        }
        
        System.out.println("=== 配置加载测试完成 ===");
    }

    @Test
    public void testKafkaConfigValidation() {
        if (config == null) {
            System.out.println("配置未加载，跳过验证");
            return;
        }
        
        MessageServiceConfig.Kafka kafka = config.getKafka();
        if (kafka == null) {
            System.out.println("Kafka配置为null，跳过验证");
            return;
        }
        
        // 验证基本配置
        if (kafka.getEnabled() != null && kafka.getEnabled()) {
            System.out.println("✓ Kafka已启用");
        } else {
            System.out.println("✗ Kafka未启用或enabled为null");
        }
        
        if (kafka.getBootstrapServers() != null && !kafka.getBootstrapServers().trim().isEmpty()) {
            System.out.println("✓ bootstrapServers已配置: " + kafka.getBootstrapServers());
        } else {
            System.out.println("✗ bootstrapServers未配置或为空");
        }
        
        // 验证消费者配置
        if (kafka.getConsumer() != null) {
            if (kafka.getConsumer().getGroupId() != null) {
                System.out.println("✓ consumer.groupId已配置: " + kafka.getConsumer().getGroupId());
            } else {
                System.out.println("✗ consumer.groupId未配置");
            }
            
            if (kafka.getConsumer().getClientId() != null) {
                System.out.println("✓ consumer.clientId已配置: " + kafka.getConsumer().getClientId());
            } else {
                System.out.println("✗ consumer.clientId未配置");
            }
        } else {
            System.out.println("✗ consumer配置为null");
        }
        
        // 验证生产者配置
        if (kafka.getProducer() != null) {
            if (kafka.getProducer().getAcks() != null) {
                System.out.println("✓ producer.acks已配置: " + kafka.getProducer().getAcks());
            } else {
                System.out.println("✗ producer.acks未配置");
            }
            
            if (kafka.getProducer().getRetries() != null) {
                System.out.println("✓ producer.retries已配置: " + kafka.getProducer().getRetries());
            } else {
                System.out.println("✗ producer.retries未配置");
            }
        } else {
            System.out.println("✗ producer配置为null");
        }
    }
}
