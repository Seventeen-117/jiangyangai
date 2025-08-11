package com.jiangyang.messages.utils;

/**
 * 消息服务类型枚举
 * 定义系统支持的消息中间件类型
 */
public enum MessageServiceType {

    /**
     * RocketMQ消息中间件
     */
    ROCKETMQ("RocketMQ", "阿里云RocketMQ消息中间件"),

    /**
     * Kafka消息中间件
     */
    KAFKA("Kafka", "Apache Kafka消息中间件"),

    /**
     * RabbitMQ消息中间件
     */
    RABBITMQ("RabbitMQ", "RabbitMQ消息中间件"),

    /**
     * 内存消息队列 (用于测试)
     */
    MEMORY("Memory", "内存消息队列，仅用于测试");

    private final String code;
    private final String description;

    MessageServiceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举值
     * @param code 代码
     * @return 枚举值
     */
    public static MessageServiceType fromCode(String code) {
        for (MessageServiceType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的消息服务类型: " + code);
    }

    /**
     * 检查是否为有效的消息服务类型
     * @param code 代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
