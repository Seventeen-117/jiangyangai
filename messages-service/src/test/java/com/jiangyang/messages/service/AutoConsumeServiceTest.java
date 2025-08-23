package com.jiangyang.messages.service;

import com.jiangyang.messages.config.KafkaConfig;
import com.jiangyang.messages.entity.MessageConsumerConfig;
import com.jiangyang.messages.service.impl.AutoConsumeServiceImpl;
import com.jiangyang.messages.utils.ConsumeMode;
import com.jiangyang.messages.utils.ConsumeOrder;
import com.jiangyang.messages.utils.ConsumeType;
import com.jiangyang.messages.utils.MessageServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AutoConsumeService测试类
 */
class AutoConsumeServiceTest {

    @Mock
    private MessageConsumerConfigService messageConsumerConfigService;

    @Mock
    private KafkaConfig kafkaConfig;

    @InjectMocks
    private AutoConsumeServiceImpl autoConsumeService;

    private MessageConsumerConfig kafkaConfig1;
    private MessageConsumerConfig kafkaConfig2;

    @BeforeEach
    void setUp() {
        // 创建测试配置
        kafkaConfig1 = createTestKafkaConfig("user-service", "KAFKA", "CLUSTERING", "CONCURRENT");
        kafkaConfig2 = createTestKafkaConfig("order-service", "KAFKA", "BROADCASTING", "ORDERLY");
        
        // 设置Kafka配置
        setupKafkaConfig();
    }

    /**
     * 创建测试用的Kafka消费配置
     */
    private MessageConsumerConfig createTestKafkaConfig(String serviceName, String messageQueueType, 
                                                      String consumeType, String consumeOrder) {
        MessageConsumerConfig config = new MessageConsumerConfig();
        config.setId(1L);
        config.setServiceName(serviceName);
        config.setInstanceId("instance-001");
        config.setMessageQueueType(messageQueueType);
        config.setConsumeMode("AUTO");
        config.setConsumeType(consumeType);
        config.setConsumeOrder(consumeOrder);
        config.setTopic("test-topic");
        config.setConsumerGroup("test-consumer-group");
        config.setBatchSize(100);
        config.setMaxRetryTimes(3);
        config.setTimeout(30000L);
        config.setEnabled(true);
        return config;
    }

    /**
     * 设置Kafka配置
     */
    private void setupKafkaConfig() {
        // 设置bootstrap servers
        KafkaConfig.BootstrapServers bootstrapServers = new KafkaConfig.BootstrapServers();
        bootstrapServers.setServers("localhost:9092");
        when(kafkaConfig.getBootstrapServers()).thenReturn(bootstrapServers);

        // 设置consumer配置
        KafkaConfig.Consumer consumer = new KafkaConfig.Consumer();
        consumer.setGroupId("default-consumer-group");
        consumer.setEnableAutoCommit(true);
        consumer.setAutoCommitInterval(1000);
        consumer.setSessionTimeout(30000);
        consumer.setHeartbeatInterval(10000);
        consumer.setRequestTimeout(40000);
        consumer.setRetryBackoff(1000);
        consumer.setMaxPollInterval(300000);
        consumer.setMaxPollRecords(500);
        consumer.setPartitionAssignmentStrategy("org.apache.kafka.clients.consumer.RangeAssignor");
        consumer.setKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        consumer.setValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        when(kafkaConfig.getConsumer()).thenReturn(consumer);
    }

    @Test
    void testStartAutoConsume() {
        // 准备测试数据
        List<MessageConsumerConfig> configs = Arrays.asList(kafkaConfig1, kafkaConfig2);
        when(messageConsumerConfigService.getEnabledConfigs()).thenReturn(configs);

        // 执行测试
        assertDoesNotThrow(() -> autoConsumeService.startAutoConsume());

        // 验证调用
        verify(messageConsumerConfigService, times(1)).getEnabledConfigs();
    }

    @Test
    void testStartConsumeByService() {
        // 准备测试数据
        List<MessageConsumerConfig> configs = Arrays.asList(kafkaConfig1);
        when(messageConsumerConfigService.getConfigsByService("user-service")).thenReturn(configs);

        // 执行测试
        assertDoesNotThrow(() -> autoConsumeService.startConsumeByService("user-service"));

        // 验证调用
        verify(messageConsumerConfigService, times(1)).getConfigsByService("user-service");
    }

    @Test
    void testStopConsumeByService() {
        // 先启动消费
        List<MessageConsumerConfig> configs = Arrays.asList(kafkaConfig1);
        when(messageConsumerConfigService.getEnabledConfigs()).thenReturn(configs);
        autoConsumeService.startAutoConsume();

        // 执行停止测试
        assertDoesNotThrow(() -> autoConsumeService.stopConsumeByService("user-service"));
    }

    @Test
    void testIsConsuming() {
        // 初始状态应该是false
        assertFalse(autoConsumeService.isConsuming("user-service"));

        // 启动消费后应该是true
        List<MessageConsumerConfig> configs = Arrays.asList(kafkaConfig1);
        when(messageConsumerConfigService.getEnabledConfigs()).thenReturn(configs);
        autoConsumeService.startAutoConsume();

        assertTrue(autoConsumeService.isConsuming("user-service"));
    }

    @Test
    void testGetAllConsumeConfigs() {
        // 准备测试数据
        List<MessageConsumerConfig> configs = Arrays.asList(kafkaConfig1, kafkaConfig2);
        when(messageConsumerConfigService.getAllConfigs()).thenReturn(configs);

        // 执行测试
        List<MessageConsumerConfig> result = autoConsumeService.getAllConsumeConfigs();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(messageConsumerConfigService, times(1)).getAllConfigs();
    }

    @Test
    void testGetConsumeConfigsByService() {
        // 准备测试数据
        List<MessageConsumerConfig> configs = Arrays.asList(kafkaConfig1);
        when(messageConsumerConfigService.getConfigsByService("user-service")).thenReturn(configs);

        // 执行测试
        List<MessageConsumerConfig> result = autoConsumeService.getConsumeConfigsByService("user-service");

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user-service", result.get(0).getServiceName());
        verify(messageConsumerConfigService, times(1)).getConfigsByService("user-service");
    }

    @Test
    void testGetConsumeStatistics() {
        // 先启动消费
        List<MessageConsumerConfig> configs = Arrays.asList(kafkaConfig1);
        when(messageConsumerConfigService.getEnabledConfigs()).thenReturn(configs);
        autoConsumeService.startAutoConsume();

        // 执行测试
        Object statistics = autoConsumeService.getConsumeStatistics("user-service");

        // 验证结果
        assertNotNull(statistics);
        assertTrue(statistics instanceof java.util.Map);
    }

    @Test
    void testReloadConsumeConfig() {
        // 先启动消费
        List<MessageConsumerConfig> configs = Arrays.asList(kafkaConfig1);
        when(messageConsumerConfigService.getEnabledConfigs()).thenReturn(configs);
        autoConsumeService.startAutoConsume();

        // 执行重新加载测试
        assertDoesNotThrow(() -> autoConsumeService.reloadConsumeConfig());
    }

    @Test
    void testStartAutoConsumeWithException() {
        // 模拟异常情况
        when(messageConsumerConfigService.getEnabledConfigs()).thenThrow(new RuntimeException("测试异常"));

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> autoConsumeService.startAutoConsume());
        assertEquals("启动自动消费服务失败", exception.getMessage());
    }

    @Test
    void testStartConsumeByServiceWithException() {
        // 模拟异常情况
        when(messageConsumerConfigService.getConfigsByService(anyString()))
            .thenThrow(new RuntimeException("测试异常"));

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> autoConsumeService.startConsumeByService("user-service"));
        assertEquals("启动消费配置失败", exception.getMessage());
    }

    @Test
    void testKafkaConfigValidation() {
        // 验证Kafka配置的有效性
        assertNotNull(kafkaConfig.getBootstrapServers());
        assertEquals("localhost:9092", kafkaConfig.getBootstrapServers().getServers());
        
        assertNotNull(kafkaConfig.getConsumer());
        assertEquals("default-consumer-group", kafkaConfig.getConsumer().getGroupId());
        assertTrue(kafkaConfig.getConsumer().isEnableAutoCommit());
        assertEquals(500, kafkaConfig.getConsumer().getMaxPollRecords());
    }

    @Test
    void testMessageConsumerConfigValidation() {
        // 验证消息消费配置的有效性
        assertNotNull(kafkaConfig1);
        assertEquals("user-service", kafkaConfig1.getServiceName());
        assertEquals("KAFKA", kafkaConfig1.getMessageQueueType());
        assertEquals("CLUSTERING", kafkaConfig1.getConsumeType());
        assertEquals("CONCURRENT", kafkaConfig1.getConsumeOrder());
        assertTrue(kafkaConfig1.getEnabled());
        
        assertNotNull(kafkaConfig2);
        assertEquals("order-service", kafkaConfig2.getServiceName());
        assertEquals("BROADCASTING", kafkaConfig2.getConsumeType());
        assertEquals("ORDERLY", kafkaConfig2.getConsumeOrder());
    }

    @Test
    void testConsumeModeEnum() {
        // 验证消费模式枚举
        assertEquals("PUSH", ConsumeMode.PUSH.name());
        assertEquals("PULL", ConsumeMode.PULL.name());
    }

    @Test
    void testConsumeTypeEnum() {
        // 验证消费类型枚举
        assertEquals("CLUSTERING", ConsumeType.CLUSTERING.name());
        assertEquals("BROADCASTING", ConsumeType.BROADCASTING.name());
    }

    @Test
    void testConsumeOrderEnum() {
        // 验证消费顺序枚举
        assertEquals("CONCURRENT", ConsumeOrder.CONCURRENT.name());
        assertEquals("ORDERLY", ConsumeOrder.ORDERLY.name());
    }

    @Test
    void testMessageServiceTypeEnum() {
        // 验证消息服务类型枚举
        assertEquals("KAFKA", MessageServiceType.KAFKA.name());
        assertEquals("ROCKETMQ", MessageServiceType.ROCKETMQ.name());
        assertEquals("RABBITMQ", MessageServiceType.RABBITMQ.name());
    }
}
