package com.jiangyang.messages.entity.elasticsearch;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Elasticsearch消息历史文档实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "message_history")
public class MessageHistoryDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String messageId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private String operation;

    @Field(type = FieldType.Keyword)
    private String operationType;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private Long timestamp;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private String createTime;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String sessionId;

    @Field(type = FieldType.Object)
    private Object details;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Keyword)
    private String target;
}
