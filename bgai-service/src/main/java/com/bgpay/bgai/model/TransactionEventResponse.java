package com.bgpay.bgai.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事务事件响应模型
 */
@Data
@Builder
@AllArgsConstructor
@Schema(description = "事务事件响应模型")
public class TransactionEventResponse {

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "响应消息", example = "事务事件处理成功")
    private String message;

    @Schema(description = "响应代码", example = "200")
    private String code;

    @Schema(description = "事务ID", example = "txn_123456789")
    private String transactionId;

    @Schema(description = "事件ID", example = "evt_123456789")
    private String eventId;

    @Schema(description = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processTime;

    @Schema(description = "处理结果详情")
    private Map<String, Object> details;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "错误代码")
    private String errorCode;

    @Schema(description = "重试建议", example = "建议重试")
    private String retrySuggestion;

    @Schema(description = "下次重试时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextRetryTime;

    @Schema(description = "扩展字段")
    private Map<String, Object> extraData;

    /**
     * 构造函数
     */
    public TransactionEventResponse() {
        this.success = true;
        this.code = "200";
        this.processTime = LocalDateTime.now();
    }

    /**
     * 创建成功响应
     */
    public static TransactionEventResponse success(String message) {
        return TransactionEventResponse.builder()
                .success(true)
                .message(message)
                .code("200")
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建成功响应（带事务ID）
     */
    public static TransactionEventResponse success(String message, String transactionId) {
        return TransactionEventResponse.builder()
                .success(true)
                .message(message)
                .code("200")
                .transactionId(transactionId)
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败响应
     */
    public static TransactionEventResponse failure(String message) {
        return TransactionEventResponse.builder()
                .success(false)
                .message(message)
                .code("500")
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败响应（带错误代码）
     */
    public static TransactionEventResponse failure(String message, String errorCode) {
        return TransactionEventResponse.builder()
                .success(false)
                .message(message)
                .code(errorCode)
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败响应（带事务ID和错误信息）
     */
    public static TransactionEventResponse failure(String message, String transactionId, String errorMessage) {
        return TransactionEventResponse.builder()
                .success(false)
                .message(message)
                .code("500")
                .transactionId(transactionId)
                .errorMessage(errorMessage)
                .processTime(LocalDateTime.now())
                .build();
    }

    /**
     * 添加详情信息
     */
    public void addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new java.util.HashMap<>();
        }
        this.details.put(key, value);
    }

    /**
     * 添加扩展数据
     */
    public void addExtraData(String key, Object value) {
        if (this.extraData == null) {
            this.extraData = new java.util.HashMap<>();
        }
        this.extraData.put(key, value);
    }

    /**
     * 设置重试建议
     */
    public void setRetrySuggestion(String suggestion, LocalDateTime nextRetryTime) {
        this.retrySuggestion = suggestion;
        this.nextRetryTime = nextRetryTime;
    }
}
