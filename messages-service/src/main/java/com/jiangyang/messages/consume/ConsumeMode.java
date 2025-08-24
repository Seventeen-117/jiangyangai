package com.jiangyang.messages.consume;

/**
 * 消费模式枚举
 * 定义RocketMQ的消费模式
 */
public enum ConsumeMode {

    /**
     * 推模式（Push Consumer）
     * Broker主动将消息推送给消费者
     */
    PUSH("PUSH", "推模式"),

    /**
     * 拉模式（Pull Consumer）
     * 消费者主动从Broker拉取消息
     */
    PULL("PULL", "拉模式");

    private final String code;
    private final String description;

    ConsumeMode(String code, String description) {
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
     */
    public static ConsumeMode fromCode(String code) {
        for (ConsumeMode mode : values()) {
            if (mode.getCode().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("不支持的消费模式: " + code);
    }

    /**
     * 检查是否为有效的消费模式
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
