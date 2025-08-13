package com.jiangyang.messages.repository.elasticsearch;

import com.jiangyang.messages.entity.elasticsearch.MessageHistoryDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Elasticsearch消息历史文档仓库接口
 */
@Repository
public interface MessageHistoryDocumentRepository extends ElasticsearchRepository<MessageHistoryDocument, String> {

    /**
     * 根据消息ID查找历史记录
     * 
     * @param messageId 消息ID
     * @return 历史记录列表
     */
    List<MessageHistoryDocument> findByMessageId(String messageId);

    /**
     * 根据操作类型查找历史记录
     * 
     * @param operation 操作类型
     * @return 历史记录列表
     */
    List<MessageHistoryDocument> findByOperation(String operation);

    /**
     * 根据状态查找历史记录
     * 
     * @param status 状态
     * @return 历史记录列表
     */
    List<MessageHistoryDocument> findByStatus(String status);

    /**
     * 根据消息ID和操作类型查找历史记录
     * 
     * @param messageId 消息ID
     * @param operation 操作类型
     * @return 历史记录列表
     */
    List<MessageHistoryDocument> findByMessageIdAndOperation(String messageId, String operation);

    /**
     * 根据创建时间范围查找历史记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 历史记录列表
     */
    List<MessageHistoryDocument> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据消息ID和操作类型查找最新的历史记录
     * 
     * @param messageId 消息ID
     * @param operation 操作类型
     * @return 最新的历史记录
     */
    Optional<MessageHistoryDocument> findFirstByMessageIdAndOperationOrderByCreateTimeDesc(String messageId, String operation);
}
