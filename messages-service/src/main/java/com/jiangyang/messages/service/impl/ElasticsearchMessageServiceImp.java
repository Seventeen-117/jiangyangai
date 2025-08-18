package com.jiangyang.messages.service.impl;

import com.jiangyang.base.datasource.annotation.DataSource;
import com.jiangyang.messages.entity.elasticsearch.MessageDocument;
import com.jiangyang.messages.entity.elasticsearch.MessageHistoryDocument;
import com.jiangyang.messages.repository.elasticsearch.MessageDocumentRepository;
import com.jiangyang.messages.repository.elasticsearch.MessageHistoryDocumentRepository;
import com.jiangyang.messages.service.ElasticsearchMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Elasticsearch消息服务
 * 负责消息的ES存储和同步功能
 */
@Slf4j
@Service
@DataSource("master")
public class ElasticsearchMessageServiceImp implements ElasticsearchMessageService {

    @Autowired(required = false)
    private MessageDocumentRepository messageDocumentRepository;

    @Autowired(required = false)
    private MessageHistoryDocumentRepository messageHistoryDocumentRepository;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    private static final String MESSAGE_INDEX = "messages";
    private static final String MESSAGE_HISTORY_INDEX = "message_history";

    /**
     * 存储消息到ES
     * 
     * @param messageId 消息ID
     * @param content 消息内容
     * @param messageType 消息类型
     * @param topic 主题
     * @return 是否存储成功
     */
    @Override
    public boolean storeMessageToES(String messageId, String content, String messageType, String topic) {
        try {
            if (messageDocumentRepository == null) {
                log.warn("MessageDocumentRepository不可用，跳过ES存储");
                return false;
            }

            // 构建消息文档
            MessageDocument messageDoc = MessageDocument.builder()
                .messageId(messageId)
                .content(content)
                .messageType(messageType)
                .topic(topic)
                .timestamp(System.currentTimeMillis())
                .createTime(java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()))
                .status("STORED")
                .version(1)
                .build();

            // 存储到ES
            messageDocumentRepository.save(messageDoc);
            
            log.info("消息已存储到ES: messageId={}, index={}", messageId, MESSAGE_INDEX);
            return true;

        } catch (Exception e) {
            log.error("存储消息到ES失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将消息存储到ES（重载方法，支持Map类型数据）
     * 
     * @param messageId 消息ID
     * @param documentData 消息文档数据
     * @return 是否存储成功
     */
    @Override
    public boolean storeMessageToES(String messageId, Map<String, Object> documentData) {
        try {
            if (messageDocumentRepository == null) {
                log.warn("MessageDocumentRepository不可用，跳过ES存储");
                return false;
            }

            // 构建消息文档
            MessageDocument messageDoc = MessageDocument.builder()
                .messageId(messageId)
                .content(documentData.get("content") != null ? documentData.get("content").toString() : "")
                .messageType(documentData.get("messageType") != null ? documentData.get("messageType").toString() : "UNKNOWN")
                .topic(documentData.get("topic") != null ? documentData.get("topic").toString() : "default")
                .timestamp(System.currentTimeMillis())
                .createTime(java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()))
                .status("STORED")
                .version(1)
                .build();

            // 存储到ES
            messageDocumentRepository.save(messageDoc);
            
            log.info("消息已存储到ES: messageId={}, index={}", messageId, MESSAGE_INDEX);
            return true;

        } catch (Exception e) {
            log.error("存储消息到ES失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从ES同步消息到本地数据库
     * 
     * @param messageId 消息ID
     * @return 同步的消息内容
     */
    @Override
    public String syncMessageFromES(String messageId) {
        try {
            if (messageDocumentRepository == null) {
                log.warn("MessageDocumentRepository不可用，跳过ES同步");
                return null;
            }

            Optional<MessageDocument> messageOpt = messageDocumentRepository.findByMessageId(messageId);
            
            if (messageOpt.isPresent()) {
                MessageDocument messageDoc = messageOpt.get();
                String content = messageDoc.getContent();
                log.info("从ES同步消息成功: messageId={}, content={}", messageId, content);
                return content;
            } else {
                log.warn("ES中未找到消息: messageId={}", messageId);
                return null;
            }

        } catch (Exception e) {
            log.error("从ES同步消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 更新ES中消息状态
     * 
     * @param messageId 消息ID
     * @param status 新状态
     * @return 是否更新成功
     */
    @Override
    public boolean updateMessageStatusInES(String messageId, String status) {
        try {
            if (messageDocumentRepository == null) {
                log.warn("MessageDocumentRepository不可用，跳过ES状态更新");
                return false;
            }

            // 使用Repository查找消息
            Optional<MessageDocument> messageOpt = messageDocumentRepository.findByMessageId(messageId);
            
            if (messageOpt.isPresent()) {
                MessageDocument messageDoc = messageOpt.get();
                
                // 更新状态
                messageDoc.setStatus(status);
                messageDoc.setUpdateTime(java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()));
                messageDoc.setVersion(messageDoc.getVersion() + 1);
                
                // 保存更新
                messageDocumentRepository.save(messageDoc);
                
                log.info("ES中消息状态已更新: messageId={}, status={}", messageId, status);
                return true;
            } else {
                log.warn("ES中未找到消息，无法更新状态: messageId={}", messageId);
                return false;
            }

        } catch (Exception e) {
            log.error("更新ES中消息状态失败: messageId={}, status={}, error={}", 
                     messageId, status, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将消息存储到历史索引
     * 
     * @param messageId 消息ID
     * @param content 消息内容
     * @param operation 操作类型
     * @return 是否存储成功
     */
    @Override
    public boolean storeMessageHistory(String messageId, String content, String operation) {
        try {
            if (messageHistoryDocumentRepository == null) {
                log.warn("MessageHistoryDocumentRepository不可用，跳过历史存储");
                return false;
            }

            // 构建历史记录
            MessageHistoryDocument historyDoc = MessageHistoryDocument.builder()
                .messageId(messageId)
                .content(content)
                .operation(operation)
                .timestamp(System.currentTimeMillis())
                .createTime(java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()))
                .status("RECORDED")
                .build();

            // 存储到历史索引
            messageHistoryDocumentRepository.save(historyDoc);
            
            log.info("消息历史已存储到ES: messageId={}, operation={}, index={}", 
                    messageId, operation, MESSAGE_HISTORY_INDEX);
            return true;

        } catch (Exception e) {
            log.error("存储消息历史到ES失败: messageId={}, operation={}, error={}", 
                     messageId, operation, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查ES服务是否可用
     * 
     * @return 是否可用
     */
    @Override
    public boolean isESAvailable() {
        return messageDocumentRepository != null && messageHistoryDocumentRepository != null;
    }

    /**
     * 根据状态查找消息
     * 
     * @param status 状态
     * @return 消息列表
     */
    @Override
    public java.util.List<MessageDocument> findMessagesByStatus(String status) {
        try {
            if (messageDocumentRepository == null) {
                log.warn("MessageDocumentRepository不可用，无法查找消息");
                return java.util.Collections.emptyList();
            }
            return messageDocumentRepository.findByStatus(status);
        } catch (Exception e) {
            log.error("根据状态查找消息失败: status={}, error={}", status, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 根据主题查找消息
     * 
     * @param topic 主题
     * @return 消息列表
     */
    @Override
    public java.util.List<MessageDocument> findMessagesByTopic(String topic) {
        try {
            if (messageDocumentRepository == null) {
                log.warn("MessageDocumentRepository不可用，无法查找消息");
                return java.util.Collections.emptyList();
            }
            return messageDocumentRepository.findByTopic(topic);
        } catch (Exception e) {
            log.error("根据主题查找消息失败: topic={}, error={}", topic, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
}
