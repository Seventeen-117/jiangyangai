package com.jiangyang.messages.utils;

/**
 * 消息类型枚举
 * 定义系统支持的消息类型
 */
public enum MessageType {

    // ==================== RocketMQ 消息类型 ====================
    /**
     * 普通消息
     */
    NORMAL("NORMAL", "普通消息"),

    /**
     * 定时消息
     */
    DELAY("DELAY", "定时消息"),

    /**
     * 顺序消息
     */
    ORDERED("ORDERED", "顺序消息"),

    /**
     * 事务消息
     */
    TRANSACTION("TRANSACTION", "事务消息"),

    // ==================== Kafka 消息类型 ====================
    /**
     * 同步发送 - 阻塞
     */
    SYNC_BLOCKING("SYNC_BLOCKING", "同步发送-阻塞"),

    /**
     * 异步发送 - 带回调
     */
    ASYNC_CALLBACK("ASYNC_CALLBACK", "异步发送-带回调"),

    /**
     * 异步发送 - 无回调
     */
    ASYNC_NO_CALLBACK("ASYNC_NO_CALLBACK", "异步发送-无回调"),

    // ==================== RabbitMQ 消息类型 ====================
    /**
     * 直接交换机消息
     */
    DIRECT("DIRECT", "直接交换机消息"),

    /**
     * 主题交换机消息
     */
    TOPIC("TOPIC", "主题交换机消息"),

    /**
     * 扇形交换机消息
     */
    FANOUT("FANOUT", "扇形交换机消息"),

    /**
     * 首部交换机消息
     */
    HEADERS("HEADERS", "首部交换机消息"),

    /**
     * 持久化消息
     */
    PERSISTENT("PERSISTENT", "持久化消息"),

    /**
     * 过期消息
     */
    TTL("TTL", "过期消息"),

    /**
     * 优先级消息
     */
    PRIORITY("PRIORITY", "优先级消息"),

    /**
     * 死信消息
     */
    DEAD_LETTER("DEAD_LETTER", "死信消息");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
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
    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的消息类型: " + code);
    }

    /**
     * 检查是否为有效的消息类型
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

    /**
     * 获取RocketMQ支持的消息类型
     */
    public static MessageType[] getRocketMQTypes() {
        return new MessageType[]{NORMAL, DELAY, ORDERED, TRANSACTION};
    }

    /**
     * 获取Kafka支持的消息类型
     */
    public static MessageType[] getKafkaTypes() {
        return new MessageType[]{SYNC_BLOCKING, ASYNC_CALLBACK, ASYNC_NO_CALLBACK};
    }

    /**
     * 获取RabbitMQ支持的消息类型
     */
    public static MessageType[] getRabbitMQTypes() {
        return new MessageType[]{DIRECT, TOPIC, FANOUT, HEADERS, PERSISTENT, TTL, PRIORITY, DEAD_LETTER};
    }

    /**
     * 检查是否为RocketMQ支持的消息类型
     */
    public boolean isRocketMQSupported() {
        return this == NORMAL || this == DELAY || this == ORDERED || this == TRANSACTION;
    }

    /**
     * 检查是否为Kafka支持的消息类型
     */
    public boolean isKafkaSupported() {
        return this == SYNC_BLOCKING || this == ASYNC_CALLBACK || this == ASYNC_NO_CALLBACK;
    }

    /**
     * 检查是否为RabbitMQ支持的消息类型
     */
    public boolean isRabbitMQSupported() {
        return this == DIRECT || this == TOPIC || this == FANOUT || this == HEADERS || 
               this == PERSISTENT || this == TTL || this == PRIORITY || this == DEAD_LETTER;
    }
}
