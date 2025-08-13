package com.jiangyang.dubbo.api.messages;

import java.util.Map;

/**
 * 消息服务Dubbo API接口
 * 提供消息发送、消费、管理等核心功能
 * 
 * @author jiangyang
 * @since 1.0.0
 */
public interface MessageService {

    /**
     * 发送消息
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param tags 标签
     * @return 发送结果
     */
    MessageResult sendMessage(String topic, String message, String tags);

    /**
     * 发送消息（带延迟）
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param tags 标签
     * @param delayLevel 延迟级别
     * @return 发送结果
     */
    MessageResult sendDelayMessage(String topic, String message, String tags, int delayLevel);

    /**
     * 发送消息（带业务键）
     * 
     * @param topic 主题
     * @param message 消息内容
     * @param tags 标签
     * @param businessKey 业务键
     * @return 发送结果
     */
    MessageResult sendMessageWithBusinessKey(String topic, String message, String tags, String businessKey);

    /**
     * 批量发送消息
     * 
     * @param topic 主题
     * @param messages 消息列表
     * @param tags 标签
     * @return 批量发送结果
     */
    BatchMessageResult sendBatchMessages(String topic, String[] messages, String tags);

    /**
     * 查询消息状态
     * 
     * @param messageId 消息ID
     * @return 消息状态
     */
    MessageStatus queryMessageStatus(String messageId);

    /**
     * 查询消息状态（通过业务键）
     * 
     * @param businessKey 业务键
     * @return 消息状态
     */
    MessageStatus queryMessageStatusByBusinessKey(String businessKey);

    /**
     * 重试发送失败的消息
     * 
     * @param messageId 消息ID
     * @return 重试结果
     */
    MessageResult retryFailedMessage(String messageId);

    /**
     * 取消延迟消息
     * 
     * @param messageId 消息ID
     * @return 取消结果
     */
    MessageResult cancelDelayMessage(String messageId);

    /**
     * 获取消息统计信息
     * 
     * @param topic 主题
     * @param timeRange 时间范围
     * @return 统计信息
     */
    MessageStatistics getMessageStatistics(String topic, String timeRange);

    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    HealthStatus healthCheck();

    /**
     * 消息发送结果
     */
    class MessageResult {
        private boolean success;
        private String messageId;
        private String errorMessage;
        private long sendTime;
        private String topic;
        private String tags;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getSendTime() { return sendTime; }
        public void setSendTime(long sendTime) { this.sendTime = sendTime; }
        
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }

    /**
     * 批量消息发送结果
     */
    class BatchMessageResult {
        private boolean success;
        private String[] messageIds;
        private String[] failedMessages;
        private String errorMessage;
        private long sendTime;
        private int totalCount;
        private int successCount;
        private int failedCount;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String[] getMessageIds() { return messageIds; }
        public void setMessageIds(String[] messageIds) { this.messageIds = messageIds; }
        
        public String[] getFailedMessages() { return failedMessages; }
        public void setFailedMessages(String[] failedMessages) { this.failedMessages = failedMessages; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getSendTime() { return sendTime; }
        public void setSendTime(long sendTime) { this.sendTime = sendTime; }
        
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    }

    /**
     * 消息状态
     */
    class MessageStatus {
        private String messageId;
        private String status; // SENT, DELIVERED, CONSUMED, FAILED, PENDING
        private long sendTime;
        private Long deliverTime;
        private Long consumeTime;
        private String topic;
        private String tags;
        private String businessKey;
        private int retryCount;
        private String errorMessage;

        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public long getSendTime() { return sendTime; }
        public void setSendTime(long sendTime) { this.sendTime = sendTime; }
        
        public Long getDeliverTime() { return deliverTime; }
        public void setDeliverTime(Long deliverTime) { this.deliverTime = deliverTime; }
        
        public Long getConsumeTime() { return consumeTime; }
        public void setConsumeTime(Long consumeTime) { this.consumeTime = consumeTime; }
        
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        
        public String getBusinessKey() { return businessKey; }
        public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
        
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * 消息统计信息
     */
    class MessageStatistics {
        private String topic;
        private long totalMessages;
        private long sentMessages;
        private long deliveredMessages;
        private long consumedMessages;
        private long failedMessages;
        private long pendingMessages;
        private double successRate;
        private double deliveryRate;
        private double consumeRate;
        private Map<String, Long> statusDistribution;

        // Getters and Setters
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        
        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
        
        public long getSentMessages() { return sentMessages; }
        public void setSentMessages(long sentMessages) { this.sentMessages = sentMessages; }
        
        public long getDeliveredMessages() { return deliveredMessages; }
        public void setDeliveredMessages(long deliveredMessages) { this.deliveredMessages = deliveredMessages; }
        
        public long getConsumedMessages() { return consumedMessages; }
        public void setConsumedMessages(long consumedMessages) { this.consumedMessages = consumedMessages; }
        
        public long getFailedMessages() { return failedMessages; }
        public void setFailedMessages(long failedMessages) { this.failedMessages = failedMessages; }
        
        public long getPendingMessages() { return pendingMessages; }
        public void setPendingMessages(long pendingMessages) { this.pendingMessages = pendingMessages; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getDeliveryRate() { return deliveryRate; }
        public void setDeliveryRate(double deliveryRate) { this.deliveryRate = deliveryRate; }
        
        public double getConsumeRate() { return consumeRate; }
        public void setConsumeRate(double consumeRate) { this.consumeRate = consumeRate; }
        
        public Map<String, Long> getStatusDistribution() { return statusDistribution; }
        public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }
    }

    /**
     * 健康状态
     */
    class HealthStatus {
        private String status; // UP, DOWN, UNKNOWN
        private String message;
        private long timestamp;
        private Map<String, Object> details;

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}
