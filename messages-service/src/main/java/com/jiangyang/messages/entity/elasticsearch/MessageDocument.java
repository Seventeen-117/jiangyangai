package com.jiangyang.messages.entity.elasticsearch;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * Elasticsearch消息文档实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "messages")
public class MessageDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String messageId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private String messageType;

    @Field(type = FieldType.Keyword)
    private String topic;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private Long timestamp;

    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    @Field(type = FieldType.Date)
    private LocalDateTime updateTime;

    @Field(type = FieldType.Integer)
    private Integer version;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Keyword)
    private String target;

    @Field(type = FieldType.Object)
    private Object metadata;
}
