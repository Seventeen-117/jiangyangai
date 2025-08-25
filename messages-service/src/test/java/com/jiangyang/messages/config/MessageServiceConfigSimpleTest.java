package com.jiangyang.messages.config;

import com.jiangyang.messages.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageServiceConfig简单配置测试类
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
public class MessageServiceConfigSimpleTest {

    @Autowired
    private MessageServiceConfig config;

    @Test
    public void testConfigLoading() {
        // 基本测试：配置对象是否被正确创建
        assertNotNull(config, "MessageServiceConfig should not be null");
        
        System.out.println("=== 配置加载测试 ===");
        System.out.println("config: " + config);
        
        // 测试Kafka配置
        MessageServiceConfig.Kafka kafka = config.getKafka();
        System.out.println("kafka: " + kafka);
        
        if (kafka != null) {
            System.out.println("kafka.enabled: " + kafka.getEnabled());
            System.out.println("kafka.bootstrapServers: " + kafka.getBootstrapServers());
            
            if (kafka.getConsumer() != null) {
                System.out.println("kafka.consumer.groupId: " + kafka.getConsumer().getGroupId());
                System.out.println("kafka.consumer.clientId: " + kafka.getConsumer().getClientId());
            }
            
            if (kafka.getProducer() != null) {
                System.out.println("kafka.producer.acks: " + kafka.getProducer().getAcks());
                System.out.println("kafka.producer.retries: " + kafka.getProducer().getRetries());
            }
        }
        
        // 测试通用配置
        MessageServiceConfig.Common common = config.getCommon();
        System.out.println("common: " + common);
        
        if (common != null) {
            System.out.println("common.defaultType: " + common.getDefaultType());
            System.out.println("common.defaultMessageType: " + common.getDefaultMessageType());
            System.out.println("common.defaultTopic: " + common.getDefaultTopic());
        }
        
        System.out.println("=== 配置加载测试完成 ===");
    }

    @Test
    public void testKafkaConfigNotNull() {
        assertNotNull(config, "MessageServiceConfig should not be null");
        
        MessageServiceConfig.Kafka kafka = config.getKafka();
        assertNotNull(kafka, "Kafka config should not be null");
        
        // 验证基本配置
        assertTrue(kafka.getEnabled(), "Kafka should be enabled");
        assertNotNull(kafka.getBootstrapServers(), "Bootstrap servers should not be null");
        assertFalse(kafka.getBootstrapServers().trim().isEmpty(), "Bootstrap servers should not be empty");
        
        System.out.println("Kafka配置验证通过:");
        System.out.println("  - enabled: " + kafka.getEnabled());
        System.out.println("  - bootstrapServers: " + kafka.getBootstrapServers());
    }
}
