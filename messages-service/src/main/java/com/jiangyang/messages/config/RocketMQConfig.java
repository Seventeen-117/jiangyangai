package com.jiangyang.messages.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * RocketMQ配置类
 * 手动创建RocketMQTemplate Bean，确保正确初始化
 */
@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${message.service.rocketmq.name-server}")
    private String nameServer;

    @Value("${message.service.rocketmq.producer-group}")
    private String producerGroup;

    /**
     * RocketMQ消息转换器
     * 设置为Primary，避免与其他MessageConverter冲突
     */
    @Bean
    @Primary
    public MessageConverter rocketMQMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        log.info("RocketMQ消息转换器初始化完成");
        return converter;
    }

    /**
     * RocketMQ生产者
     * 不手动启动，让RocketMQTemplate自己管理
     */
    @Bean
    public DefaultMQProducer defaultMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(3000);
        producer.setRetryTimesWhenSendFailed(3);
        producer.setRetryTimesWhenSendAsyncFailed(3);
        producer.setCompressMsgBodyOverHowmuch(4096);
        producer.setMaxMessageSize(4194304);
        
        log.info("RocketMQ生产者配置完成: nameServer={}, producerGroup={}", nameServer, producerGroup);
        return producer;
    }

    /**
     * RocketMQ模板
     */
    @Bean
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer producer, MessageConverter messageConverter) {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducer(producer);
        rocketMQTemplate.setMessageConverter(messageConverter);
        log.info("RocketMQ模板初始化完成");
        return rocketMQTemplate;
    }
}
