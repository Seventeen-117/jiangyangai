package com.jiangyang.messages.rocketmq;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RocketMQ消息发送模板
 * 提供统一的消息发送接口，简化消息发送操作
 */
@Slf4j
@Component
public class RocketMQTemplate implements InitializingBean {

    private DefaultMQProducer producer;
    private String nameServer;
    private String producerGroup;
    private boolean initialized = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("RocketMQTemplate initialized");
    }

    /**
     * 设置NameServer地址
     */
    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    /**
     * 设置生产者组
     */
    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    /**
     * 初始化生产者
     */
    public void initializeProducer() {
        if (initialized || producer != null) {
            return;
        }

        try {
            producer = new DefaultMQProducer(producerGroup);
            producer.setNamesrvAddr(nameServer);
            producer.setSendMsgTimeout(3000);
            producer.setRetryTimesWhenSendFailed(3);
            producer.start();
            initialized = true;
            log.info("RocketMQ producer initialized with nameServer: {}, producerGroup: {}", nameServer, producerGroup);
        } catch (Exception e) {
            log.error("Failed to initialize RocketMQ producer", e);
            throw new RuntimeException("Failed to initialize RocketMQ producer", e);
        }
    }

    /**
     * 同步发送消息
     */
    public <T> SendResult syncSend(String topic, T message) {
        return syncSend(topic, message, null);
    }

    /**
     * 同步发送消息（带标签）
     */
    public <T> SendResult syncSend(String topic, T message, String tags) {
        try {
            initializeProducer();
            Message msg = createMessage(topic, message, tags);
            return producer.send(msg);
        } catch (Exception e) {
            log.error("Failed to sync send message to topic: {}", topic, e);
            throw new RuntimeException("Failed to sync send message", e);
        }
    }

    /**
     * 异步发送消息
     */
    public <T> void asyncSend(String topic, T message, SendCallback callback) {
        asyncSend(topic, message, null, callback);
    }

    /**
     * 异步发送消息（带标签）
     */
    public <T> void asyncSend(String topic, T message, String tags, SendCallback callback) {
        try {
            initializeProducer();
            Message msg = createMessage(topic, message, tags);
            producer.send(msg, callback);
        } catch (Exception e) {
            log.error("Failed to async send message to topic: {}", topic, e);
            if (callback != null) {
                callback.onException(e);
            }
        }
    }

    /**
     * 单向发送消息
     */
    public <T> void sendOneway(String topic, T message) {
        sendOneway(topic, message, null);
    }

    /**
     * 单向发送消息（带标签）
     */
    public <T> void sendOneway(String topic, T message, String tags) {
        try {
            initializeProducer();
            Message msg = createMessage(topic, message, tags);
            producer.sendOneway(msg);
        } catch (Exception e) {
            log.error("Failed to send oneway message to topic: {}", topic, e);
            throw new RuntimeException("Failed to send oneway message", e);
        }
    }

    /**
     * 批量发送消息
     */
    public <T> SendResult sendBatch(String topic, List<T> messages) {
        return sendBatch(topic, messages, null);
    }

    /**
     * 批量发送消息（带标签）
     */
    public <T> SendResult sendBatch(String topic, List<T> messages, String tags) {
        try {
            initializeProducer();
            List<Message> msgList = messages.stream()
                    .map(msg -> createMessage(topic, msg, tags))
                    .toList();
            return producer.send(msgList);
        } catch (Exception e) {
            log.error("Failed to send batch messages to topic: {}", topic, e);
            throw new RuntimeException("Failed to send batch messages", e);
        }
    }

    /**
     * 创建消息对象
     */
    private <T> Message createMessage(String topic, T message, String tags) {
        String messageBody = JSON.toJSONString(message);
        Message msg = new Message(topic, tags, messageBody.getBytes(StandardCharsets.UTF_8));
        return msg;
    }

    /**
     * 关闭生产者
     */
    public void shutdown() {
        if (producer != null) {
            producer.shutdown();
            initialized = false;
            log.info("RocketMQ producer shutdown");
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}
