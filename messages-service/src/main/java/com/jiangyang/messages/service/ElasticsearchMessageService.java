package com.jiangyang.messages.service;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.entity.elasticsearch.MessageDocument;

import java.util.Map;

@DataSource("master")
public interface ElasticsearchMessageService {
    boolean storeMessageToES(String messageId, String content, String messageType, String topic);

    boolean storeMessageToES(String messageId, Map<String, Object> documentData);

    String syncMessageFromES(String messageId);

    boolean updateMessageStatusInES(String messageId, String status);

    boolean storeMessageHistory(String messageId, String content, String operation);

    boolean isESAvailable();

    java.util.List<MessageDocument> findMessagesByStatus(String status);

    java.util.List<MessageDocument> findMessagesByTopic(String topic);
}
