package com.jiangyang.messages.consume;

/**
 * 消费类型枚举
 * 定义RocketMQ的消费类型
 */
public enum ConsumeType {

    /**
     * 集群消费（Clustering）
     * 同消费组内的多个消费者共同分担消息，每条消息仅被组内一个消费者消费一次
     */
    CLUSTERING("CLUSTERING", "集群消费"),

    /**
     * 广播消费（Broadcasting）
     * 同消费组内的所有消费者都会收到并处理同一条消息
     */
    BROADCASTING("BROADCASTING", "广播消费");

    private final String code;
    private final String description;

    ConsumeType(String code, String description) {
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
    public static ConsumeType fromCode(String code) {
        for (ConsumeType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的消费类型: " + code);
    }

    /**
     * 检查是否为有效的消费类型
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
