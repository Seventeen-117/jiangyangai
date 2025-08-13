package com.jiangyang.messages.repository.elasticsearch;

import com.jiangyang.messages.entity.elasticsearch.MessageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Elasticsearch消息文档仓库接口
 */
@Repository
public interface MessageDocumentRepository extends ElasticsearchRepository<MessageDocument, String> {

    /**
     * 根据消息ID查找消息
     * 
     * @param messageId 消息ID
     * @return 消息文档
     */
    Optional<MessageDocument> findByMessageId(String messageId);

    /**
     * 根据状态查找消息
     * 
     * @param status 状态
     * @return 消息列表
     */
    List<MessageDocument> findByStatus(String status);

    /**
     * 根据主题查找消息
     * 
     * @param topic 主题
     * @return 消息列表
     */
    List<MessageDocument> findByTopic(String topic);

    /**
     * 根据消息类型查找消息
     * 
     * @param messageType 消息类型
     * @return 消息列表
     */
    List<MessageDocument> findByMessageType(String messageType);

    /**
     * 根据消息ID和状态查找消息
     * 
     * @param messageId 消息ID
     * @param status 状态
     * @return 消息文档
     */
    Optional<MessageDocument> findByMessageIdAndStatus(String messageId, String status);

    /**
     * 根据创建时间范围查找消息
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    List<MessageDocument> findByCreateTimeBetween(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);
}
