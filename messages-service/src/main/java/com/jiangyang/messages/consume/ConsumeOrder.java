package com.jiangyang.messages.consume;

/**
 * 消费顺序性枚举
 * 定义RocketMQ的消费顺序性
 */
public enum ConsumeOrder {

    /**
     * 并发消费（Concurrent）
     * 消费者多线程并发处理消息，不保证消息顺序
     */
    CONCURRENT("CONCURRENT", "并发消费"),

    /**
     * 顺序消费（Orderly）
     * 保证消息按发送顺序被消费
     */
    ORDERLY("ORDERLY", "顺序消费");

    private final String code;
    private final String description;

    ConsumeOrder(String code, String description) {
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
    public static ConsumeOrder fromCode(String code) {
        for (ConsumeOrder order : values()) {
            if (order.getCode().equalsIgnoreCase(code)) {
                return order;
            }
        }
        throw new IllegalArgumentException("不支持的消费顺序性: " + code);
    }

    /**
     * 检查是否为有效的消费顺序性
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
