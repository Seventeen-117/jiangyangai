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
 * 参考bgai-service的配置方式，手动创建RocketMQTemplate
 */
@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${message.rocketmq.producer.group}")
    private String producerGroup;

    @Value("${message.rocketmq.name-server}")
    private String nameServer;

    @Bean
    public MessageConverter rocketMQMessageConverter() {
        return new MappingJackson2MessageConverter();
    }

    @Bean(name = "defaultMQProducer", destroyMethod = "shutdown")
    public DefaultMQProducer defaultMQProducer() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(5000);
        producer.setRetryTimesWhenSendFailed(2);
        producer.setCompressMsgBodyOverHowmuch(4096);
        producer.setMaxMessageSize(4194304);
        producer.setVipChannelEnabled(true);
        producer.start();
        log.info("DefaultMQProducer启动成功: nameServer={}, producerGroup={}", nameServer, producerGroup);
        return producer;
    }

    @Bean
    @Primary
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer defaultMQProducer, MessageConverter messageConverter) {
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(defaultMQProducer);
        template.setMessageConverter(messageConverter);
        log.info("RocketMQTemplate创建成功");
        return template;
    }
}
