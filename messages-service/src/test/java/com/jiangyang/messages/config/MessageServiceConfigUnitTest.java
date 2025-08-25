package com.jiangyang.messages.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageServiceConfig单元测试
 * 直接测试配置类的功能，不依赖Spring Boot配置加载
 */
public class MessageServiceConfigUnitTest {

    @Test
    public void testKafkaConfigCreation() {
        // 创建配置对象
        MessageServiceConfig config = new MessageServiceConfig();
        
        // 创建Kafka配置
        MessageServiceConfig.Kafka kafka = new MessageServiceConfig.Kafka();
        kafka.setEnabled(true);
        kafka.setBootstrapServers("localhost:9092");
        
        // 创建消费者配置
        MessageServiceConfig.Kafka.Consumer consumer = new MessageServiceConfig.Kafka.Consumer();
        consumer.setGroupId("test-consumer-group");
        consumer.setClientId("test-consumer-client");
        consumer.setEnableAutoCommit(false);
        consumer.setAutoCommitInterval(1000);
        consumer.setSessionTimeout(30000);
        consumer.setAutoOffsetReset("latest");
        consumer.setKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        consumer.setValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        kafka.setConsumer(consumer);
        
        // 创建生产者配置
        MessageServiceConfig.Kafka.Producer producer = new MessageServiceConfig.Kafka.Producer();
        producer.setBootstrapServers("localhost:9092");
        producer.setAcks("1");
        producer.setRetries(3);
        producer.setBatchSize(8192);
        producer.setLingerMs(10);
        producer.setBufferMemory(16777216);
        producer.setKeySerializer("org.apache.kafka.common.serialization.StringSerializer");
        producer.setValueSerializer("org.apache.kafka.common.serialization.StringSerializer");
        kafka.setProducer(producer);
        
        // 创建主题配置
        MessageServiceConfig.Kafka.Topics topics = new MessageServiceConfig.Kafka.Topics();
        topics.setDefaultTopic("test-topic");
        topics.setDlq("test-dlq-topic");
        topics.setRetry("test-retry-topic");
        kafka.setTopics(topics);
        
        // 创建消费配置
        MessageServiceConfig.Kafka.Consume consume = new MessageServiceConfig.Kafka.Consume();
        consume.setDefaultMode("PULL");
        consume.setDefaultType("CLUSTERING");
        consume.setDefaultOrder("CONCURRENT");
        consume.setDefaultBatchSize(100);
        consume.setDefaultMaxRetry(3);
        consume.setDefaultTimeout(30000);
        kafka.setConsume(consume);
        
        // 设置Kafka配置
        config.setKafka(kafka);
        
        // 验证配置
        assertNotNull(config.getKafka(), "Kafka配置不应为null");
        assertTrue(config.getKafka().getEnabled(), "Kafka应该启用");
        assertEquals("localhost:9092", config.getKafka().getBootstrapServers(), "bootstrapServers应该匹配");
        
        // 验证消费者配置
        MessageServiceConfig.Kafka.Consumer testConsumer = config.getKafka().getConsumer();
        assertNotNull(testConsumer, "消费者配置不应为null");
        assertEquals("test-consumer-group", testConsumer.getGroupId(), "groupId应该匹配");
        assertEquals("test-consumer-client", testConsumer.getClientId(), "clientId应该匹配");
        assertFalse(testConsumer.getEnableAutoCommit(), "enableAutoCommit应该为false");
        assertEquals("latest", testConsumer.getAutoOffsetReset(), "autoOffsetReset应该匹配");
        
        // 验证生产者配置
        MessageServiceConfig.Kafka.Producer testProducer = config.getKafka().getProducer();
        assertNotNull(testProducer, "生产者配置不应为null");
        assertEquals("1", testProducer.getAcks(), "acks应该匹配");
        assertEquals(3, testProducer.getRetries(), "retries应该匹配");
        assertEquals(8192, testProducer.getBatchSize(), "batchSize应该匹配");
        
        // 验证主题配置
        MessageServiceConfig.Kafka.Topics testTopics = config.getKafka().getTopics();
        assertNotNull(testTopics, "主题配置不应为null");
        assertEquals("test-topic", testTopics.getDefaultTopic(), "defaultTopic应该匹配");
        assertEquals("test-dlq-topic", testTopics.getDlq(), "dlq应该匹配");
        assertEquals("test-retry-topic", testTopics.getRetry(), "retry应该匹配");
        
        // 验证消费配置
        MessageServiceConfig.Kafka.Consume testConsume = config.getKafka().getConsume();
        assertNotNull(testConsume, "消费配置不应为null");
        assertEquals("PULL", testConsume.getDefaultMode(), "defaultMode应该匹配");
        assertEquals("CLUSTERING", testConsume.getDefaultType(), "defaultType应该匹配");
        assertEquals("CONCURRENT", testConsume.getDefaultOrder(), "defaultOrder应该匹配");
        
        System.out.println("✓ Kafka配置创建和验证成功");
        System.out.println("  - enabled: " + config.getKafka().getEnabled());
        System.out.println("  - bootstrapServers: " + config.getKafka().getBootstrapServers());
        System.out.println("  - consumer.groupId: " + testConsumer.getGroupId());
        System.out.println("  - consumer.clientId: " + testConsumer.getClientId());
        System.out.println("  - producer.acks: " + testProducer.getAcks());
        System.out.println("  - producer.retries: " + testProducer.getRetries());
        System.out.println("  - topics.defaultTopic: " + testTopics.getDefaultTopic());
    }

    @Test
    public void testCommonConfigCreation() {
        // 创建配置对象
        MessageServiceConfig config = new MessageServiceConfig();
        
        // 创建通用配置
        MessageServiceConfig.Common common = new MessageServiceConfig.Common();
        common.setDefaultType("kafka");
        common.setDefaultMessageType("normal");
        common.setDefaultTopic("test-topic");
        
        // 创建发送配置
        MessageServiceConfig.Common.Send send = new MessageServiceConfig.Common.Send();
        send.setAsyncEnabled(true);
        send.setMaxRetries(3);
        send.setRetryInterval(1000);
        send.setConfirmEnabled(true);
        send.setConfirmTimeout(5000);
        common.setSend(send);
        
        // 创建消费配置
        MessageServiceConfig.Common.Consume consume = new MessageServiceConfig.Common.Consume();
        consume.setDefaultMode("PUSH");
        consume.setDefaultType("CLUSTERING");
        consume.setDefaultOrder("CONCURRENT");
        consume.setMaxConcurrency(10);
        consume.setBatchSize(100);
        consume.setTimeout(30000);
        common.setConsume(consume);
        
        // 创建重试配置
        MessageServiceConfig.Common.Retry retry = new MessageServiceConfig.Common.Retry();
        retry.setEnabled(true);
        retry.setMaxRetries(3);
        retry.setRetryInterval(1000);
        retry.setBackoffStrategy("EXPONENTIAL");
        retry.setBackoffMultiplier(2.0);
        retry.setMaxRetryInterval(60000);
        common.setRetry(retry);
        
        // 创建监控配置
        MessageServiceConfig.Common.Monitoring monitoring = new MessageServiceConfig.Common.Monitoring();
        monitoring.setEnabled(true);
        monitoring.setInterval(30);
        monitoring.setVerboseLogging(false);
        monitoring.setMetricsBackend("prometheus");
        common.setMonitoring(monitoring);
        
        // 设置通用配置
        config.setCommon(common);
        
        // 验证配置
        assertNotNull(config.getCommon(), "通用配置不应为null");
        assertEquals("kafka", config.getCommon().getDefaultType(), "defaultType应该匹配");
        assertEquals("normal", config.getCommon().getDefaultMessageType(), "defaultMessageType应该匹配");
        assertEquals("test-topic", config.getCommon().getDefaultTopic(), "defaultTopic应该匹配");
        
        // 验证发送配置
        MessageServiceConfig.Common.Send testSend = config.getCommon().getSend();
        assertNotNull(testSend, "发送配置不应为null");
        assertTrue(testSend.getAsyncEnabled(), "asyncEnabled应该为true");
        assertEquals(3, testSend.getMaxRetries(), "maxRetries应该匹配");
        assertEquals(1000, testSend.getRetryInterval(), "retryInterval应该匹配");
        
        // 验证消费配置
        MessageServiceConfig.Common.Consume testConsume = config.getCommon().getConsume();
        assertNotNull(testConsume, "消费配置不应为null");
        assertEquals("PUSH", testConsume.getDefaultMode(), "defaultMode应该匹配");
        assertEquals("CLUSTERING", testConsume.getDefaultType(), "defaultType应该匹配");
        assertEquals("CONCURRENT", testConsume.getDefaultOrder(), "defaultOrder应该匹配");
        assertEquals(10, testConsume.getMaxConcurrency(), "maxConcurrency应该匹配");
        
        // 验证重试配置
        MessageServiceConfig.Common.Retry testRetry = config.getCommon().getRetry();
        assertNotNull(testRetry, "重试配置不应为null");
        assertTrue(testRetry.getEnabled(), "enabled应该为true");
        assertEquals(3, testRetry.getMaxRetries(), "maxRetries应该匹配");
        assertEquals("EXPONENTIAL", testRetry.getBackoffStrategy(), "backoffStrategy应该匹配");
        assertEquals(2.0, testRetry.getBackoffMultiplier(), "backoffMultiplier应该匹配");
        
        // 验证监控配置
        MessageServiceConfig.Common.Monitoring testMonitoring = config.getCommon().getMonitoring();
        assertNotNull(testMonitoring, "监控配置不应为null");
        assertTrue(testMonitoring.getEnabled(), "enabled应该为true");
        assertEquals(30, testMonitoring.getInterval(), "interval应该匹配");
        assertEquals("prometheus", testMonitoring.getMetricsBackend(), "metricsBackend应该匹配");
        
        System.out.println("✓ 通用配置创建和验证成功");
        System.out.println("  - defaultType: " + config.getCommon().getDefaultType());
        System.out.println("  - defaultMessageType: " + config.getCommon().getDefaultMessageType());
        System.out.println("  - defaultTopic: " + config.getCommon().getDefaultTopic());
        System.out.println("  - send.asyncEnabled: " + testSend.getAsyncEnabled());
        System.out.println("  - consume.defaultMode: " + testConsume.getDefaultMode());
        System.out.println("  - retry.enabled: " + testRetry.getEnabled());
        System.out.println("  - monitoring.enabled: " + testMonitoring.getEnabled());
    }

    @Test
    public void testKafkaConsumerProperties() {
        // 创建Kafka配置
        MessageServiceConfig.Kafka kafka = new MessageServiceConfig.Kafka();
        kafka.setBootstrapServers("localhost:9092");
        
        MessageServiceConfig.Kafka.Consumer consumer = new MessageServiceConfig.Kafka.Consumer();
        consumer.setGroupId("test-group");
        consumer.setClientId("test-client");
        consumer.setEnableAutoCommit(false);
        consumer.setAutoCommitInterval(1000);
        consumer.setSessionTimeout(30000);
        consumer.setHeartbeatInterval(10000);
        consumer.setRequestTimeout(40000);
        consumer.setMaxPollInterval(300000);
        consumer.setMaxPollRecords(500);
        consumer.setAutoOffsetReset("latest");
        consumer.setKeyDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        consumer.setValueDeserializer("org.apache.kafka.common.serialization.StringDeserializer");
        kafka.setConsumer(consumer);
        
        // 测试获取消费者属性
        var props = kafka.getConsumerKafkaProperties();
        
        assertNotNull(props, "消费者属性不应为null");
        assertEquals("localhost:9092", props.get("bootstrap.servers"), "bootstrap.servers应该匹配");
        assertEquals("test-group", props.get("group.id"), "group.id应该匹配");
        assertEquals("test-client", props.get("client.id"), "client.id应该匹配");
        assertEquals(false, props.get("enable.auto.commit"), "enable.auto.commit应该匹配");
        assertEquals(1000, props.get("auto.commit.interval.ms"), "auto.commit.interval.ms应该匹配");
        assertEquals(30000, props.get("session.timeout.ms"), "session.timeout.ms应该匹配");
        assertEquals(10000, props.get("heartbeat.interval.ms"), "heartbeat.interval.ms应该匹配");
        assertEquals(40000, props.get("request.timeout.ms"), "request.timeout.ms应该匹配");
        assertEquals(300000, props.get("max.poll.interval.ms"), "max.poll.interval.ms应该匹配");
        assertEquals(500, props.get("max.poll.records"), "max.poll.records应该匹配");
        assertEquals("latest", props.get("auto.offset.reset"), "auto.offset.reset应该匹配");
        assertEquals("org.apache.kafka.common.serialization.StringDeserializer", props.get("key.deserializer"), "key.deserializer应该匹配");
        assertEquals("org.apache.kafka.common.serialization.StringDeserializer", props.get("value.deserializer"), "value.deserializer应该匹配");
        
        System.out.println("✓ Kafka消费者属性生成成功");
        System.out.println("  生成了 " + props.size() + " 个属性");
    }

    @Test
    public void testKafkaConsumerTypeProperties() {
        // 创建Kafka配置
        MessageServiceConfig.Kafka kafka = new MessageServiceConfig.Kafka();
        kafka.setBootstrapServers("localhost:9092");
        
        MessageServiceConfig.Kafka.Consumer consumer = new MessageServiceConfig.Kafka.Consumer();
        consumer.setGroupId("test-group");
        consumer.setClientId("test-client");
        consumer.setBatchSize(100);
        kafka.setConsumer(consumer);
        
        // 测试不同消费类型的属性
        var clusterProps = kafka.getConsumerProperties(MessageServiceConfig.Kafka.ConsumerType.CLUSTER);
        var broadcastProps = kafka.getConsumerProperties(MessageServiceConfig.Kafka.ConsumerType.BROADCAST);
        var orderlyProps = kafka.getConsumerProperties(MessageServiceConfig.Kafka.ConsumerType.ORDERLY);
        var batchProps = kafka.getConsumerProperties(MessageServiceConfig.Kafka.ConsumerType.BATCH);
        
        // 验证集群消费配置
        assertEquals(false, clusterProps.get("enable.auto.commit"), "集群消费应该关闭自动提交");
        assertEquals("earliest", clusterProps.get("auto.offset.reset"), "集群消费应该从头开始");
        
        // 验证广播消费配置
        assertEquals(true, broadcastProps.get("enable.auto.commit"), "广播消费应该开启自动提交");
        assertEquals("latest", broadcastProps.get("auto.offset.reset"), "广播消费应该从最新位置开始");
        
        // 验证顺序消费配置
        assertEquals(false, orderlyProps.get("enable.auto.commit"), "顺序消费应该关闭自动提交");
        assertEquals(1, orderlyProps.get("max.poll.records"), "顺序消费应该每次只拉取一条消息");
        
        // 验证批量消费配置
        assertEquals(false, batchProps.get("enable.auto.commit"), "批量消费应该关闭自动提交");
        assertEquals(100, batchProps.get("max.poll.records"), "批量消费应该设置批量大小");
        
        System.out.println("✓ Kafka不同消费类型属性生成成功");
        System.out.println("  - 集群消费: " + clusterProps.size() + " 个属性");
        System.out.println("  - 广播消费: " + broadcastProps.size() + " 个属性");
        System.out.println("  - 顺序消费: " + orderlyProps.size() + " 个属性");
        System.out.println("  - 批量消费: " + batchProps.size() + " 个属性");
    }
}
