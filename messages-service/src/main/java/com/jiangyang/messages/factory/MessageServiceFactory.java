package com.jiangyang.messages.factory;

import com.jiangyang.messages.utils.MessageServiceConfig;
import com.jiangyang.messages.utils.MessageServiceException;
import com.jiangyang.messages.utils.MessageServiceType;
import com.jiangyang.messages.kafka.KafkaMessageService;
import com.jiangyang.messages.rabbitmq.RabbitMQMessageService;
import com.jiangyang.messages.rocketmq.RocketMQTemplateService;
import com.jiangyang.messages.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息服务工厂类
 * 根据配置选择不同的消息中间件实现
 */
@Slf4j
@Component
public class MessageServiceFactory {

    @Autowired
    private MessageServiceConfig config;

    @Autowired(required = false)
    private RocketMQTemplateService rocketMQMessageService;

    @Autowired(required = false)
    private KafkaMessageService kafkaMessageService;

    @Autowired(required = false)
    private RabbitMQMessageService rabbitMQMessageService;

    /**
     * 获取默认的消息服务
     * @return 消息服务
     */
    public MessageService getDefaultMessageService() {
        return getMessageService(MessageServiceType.valueOf(config.getDefaultType().toUpperCase()));
    }

    /**
     * 根据类型获取消息服务
     * @param type 消息服务类型
     * @return 消息服务
     */
    public MessageService getMessageService(MessageServiceType type) {
        switch (type) {
            case ROCKETMQ:
                if (rocketMQMessageService == null) {
                    throw new MessageServiceException("RocketMQ service is not available");
                }
                if (!config.getRocketmq().isEnabled()) {
                    log.warn("RocketMQ service is not enabled, using it may cause issues");
                }
                return rocketMQMessageService;
            case KAFKA:
                if (kafkaMessageService == null) {
                    throw new MessageServiceException("Kafka service is not available");
                }
                if (!config.getKafka().isEnabled()) {
                    log.warn("Kafka service is not enabled, using it may cause issues");
                }
                return kafkaMessageService;
            case RABBITMQ:
                if (rabbitMQMessageService == null) {
                    throw new MessageServiceException("RabbitMQ service is not available");
                }
                if (!config.getRabbitmq().isEnabled()) {
                    log.warn("RabbitMQ service is not enabled, using it may cause issues");
                }
                return rabbitMQMessageService;
            default:
                throw new MessageServiceException("Unsupported message service type: " + type);
        }
    }

    /**
     * 检查某种类型的消息服务是否可用
     * @param type 消息服务类型
     * @return 是否可用
     */
    public boolean isServiceAvailable(MessageServiceType type) {
        switch (type) {
            case ROCKETMQ:
                return rocketMQMessageService != null && config.getRocketmq().isEnabled();
            case KAFKA:
                return kafkaMessageService != null && config.getKafka().isEnabled();
            case RABBITMQ:
                return rabbitMQMessageService != null && config.getRabbitmq().isEnabled();
            default:
                return false;
        }
    }
}
