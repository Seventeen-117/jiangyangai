package com.jiangyang.messages.config;

import com.jiangyang.messages.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageServiceConfig配置测试类
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
public class MessageServiceConfigTest {

    @Autowired
    private MessageServiceConfig config;

    @TestConfiguration
    static class TestConfig {
        
        @Bean
        @Primary
        public MessageServiceConfig messageServiceConfig() {
            MessageServiceConfig config = new MessageServiceConfig();
            
            // 设置Kafka配置
            MessageServiceConfig.Kafka kafka = new MessageServiceConfig.Kafka();
            kafka.setEnabled(true);
            kafka.setBootstrapServers("localhost:9092");
            
            MessageServiceConfig.Kafka.Consumer consumer = new MessageServiceConfig.Kafka.Consumer();
            consumer.setGroupId("test-consumer-group");
            consumer.setClientId("test-consumer-client");
            kafka.setConsumer(consumer);
            
            MessageServiceConfig.Kafka.Producer producer = new MessageServiceConfig.Kafka.Producer();
            producer.setAcks("1");
            producer.setRetries(3);
            kafka.setProducer(producer);
            
            config.setKafka(kafka);
            
            // 设置通用配置
            MessageServiceConfig.Common common = new MessageServiceConfig.Common();
            common.setDefaultType("kafka");
            common.setDefaultMessageType("normal");
            common.setDefaultTopic("test-topic");
            config.setCommon(common);
            
            return config;
        }
    }

    @Test
    public void testKafkaConfig() {
        assertNotNull(config, "MessageServiceConfig should not be null");
        
        MessageServiceConfig.Kafka kafka = config.getKafka();
        assertNotNull(kafka, "Kafka config should not be null");
        
        // 验证基本配置
        assertTrue(kafka.getEnabled(), "Kafka should be enabled");
        assertNotNull(kafka.getBootstrapServers(), "Bootstrap servers should not be null");
        assertFalse(kafka.getBootstrapServers().trim().isEmpty(), "Bootstrap servers should not be empty");
        
        // 验证消费者配置
        MessageServiceConfig.Kafka.Consumer consumer = kafka.getConsumer();
        assertNotNull(consumer, "Consumer config should not be null");
        assertNotNull(consumer.getGroupId(), "Consumer group ID should not be null");
        assertNotNull(consumer.getClientId(), "Consumer client ID should not be null");
        
        // 验证生产者配置
        MessageServiceConfig.Kafka.Producer producer = kafka.getProducer();
        assertNotNull(producer, "Producer config should not be null");
        assertNotNull(producer.getAcks(), "Producer acks should not be null");
        assertNotNull(producer.getRetries(), "Producer retries should not be null");
        
        System.out.println("Kafka配置验证通过:");
        System.out.println("  - enabled: " + kafka.getEnabled());
        System.out.println("  - bootstrapServers: " + kafka.getBootstrapServers());
        System.out.println("  - consumer.groupId: " + consumer.getGroupId());
        System.out.println("  - consumer.clientId: " + consumer.getClientId());
        System.out.println("  - producer.acks: " + producer.getAcks());
        System.out.println("  - producer.retries: " + producer.getRetries());
    }

    @Test
    public void testRocketMQConfig() {
        assertNotNull(config, "MessageServiceConfig should not be null");
        
        MessageServiceConfig.RocketMQ rocketmq = config.getRocketmq();
        assertNotNull(rocketmq, "RocketMQ config should not be null");
        
        assertTrue(rocketmq.getEnabled(), "RocketMQ should be enabled");
        assertNotNull(rocketmq.getNameServer(), "Name server should not be null");
        
        System.out.println("RocketMQ配置验证通过:");
        System.out.println("  - enabled: " + rocketmq.getEnabled());
        System.out.println("  - nameServer: " + rocketmq.getNameServer());
    }

    @Test
    public void testCommonConfig() {
        assertNotNull(config, "MessageServiceConfig should not be null");
        
        MessageServiceConfig.Common common = config.getCommon();
        assertNotNull(common, "Common config should not be null");
        
        assertNotNull(common.getDefaultType(), "Default type should not be null");
        assertNotNull(common.getDefaultMessageType(), "Default message type should not be null");
        assertNotNull(common.getDefaultTopic(), "Default topic should not be null");
        
        System.out.println("通用配置验证通过:");
        System.out.println("  - defaultType: " + common.getDefaultType());
        System.out.println("  - defaultMessageType: " + common.getDefaultMessageType());
        System.out.println("  - defaultTopic: " + common.getDefaultTopic());
    }
}
