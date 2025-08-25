package com.jiangyang.messages.rocketmq;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * RocketMQ环境后处理器
 * 在环境准备的最早阶段动态添加RocketMQ配置
 */
public class RocketMQEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 从你的自定义配置路径读取配置
        String nameServer = environment.getProperty("message.service.rocketmq.name-server");
        String producerGroup = environment.getProperty("message.service.rocketmq.producer-group");
        String consumerGroup = environment.getProperty("message.service.rocketmq.consumer-group");

        if (nameServer != null || producerGroup != null || consumerGroup != null) {
            Properties props = new Properties();

            if (nameServer != null) {
                props.setProperty("rocketmq.name-server", nameServer);
                props.setProperty("spring.rocketmq.name-server", nameServer);
            }
            if (producerGroup != null) {
                props.setProperty("rocketmq.producer.group", producerGroup);
                props.setProperty("spring.rocketmq.producer.group", producerGroup);
            }
            if (consumerGroup != null) {
                props.setProperty("rocketmq.consumer.group", consumerGroup);
                props.setProperty("spring.rocketmq.consumer.group", consumerGroup);
            }

            // 将配置添加到环境属性源的最前面，确保优先级最高
            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(new PropertiesPropertySource("rocketmq-config", props));

            System.out.println("RocketMQ环境配置已添加:");
            System.out.println("  - name-server: " + nameServer);
            System.out.println("  - producer.group: " + producerGroup);
            System.out.println("  - consumer.group: " + consumerGroup);
        }
    }
}