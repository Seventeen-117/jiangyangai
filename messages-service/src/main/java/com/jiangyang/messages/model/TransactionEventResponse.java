package com.jiangyang.messages.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 事务事件响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEventResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应代码
     */
    private String code;

    /**
     * 事务ID
     */
    private String transactionId;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 扩展详情
     */
    private Map<String, Object> details;

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功响应
     */
    public static TransactionEventResponse success(String message) {
        return TransactionEventResponse.builder()
                .success(true)
                .message(message)
                .code("200")
                .processTime(LocalDateTime.now())
                .details(new HashMap<>())
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
                .details(new HashMap<>())
                .build();
    }

    /**
     * 创建成功响应（带事件ID和事务ID）
     */
    public static TransactionEventResponse success(String message, String transactionId, String eventId) {
        return TransactionEventResponse.builder()
                .success(true)
                .message(message)
                .code("200")
                .transactionId(transactionId)
                .eventId(eventId)
                .processTime(LocalDateTime.now())
                .details(new HashMap<>())
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
                .details(new HashMap<>())
                .build();
    }

    /**
     * 创建失败响应（带事务ID）
     */
    public static TransactionEventResponse failure(String message, String transactionId) {
        return TransactionEventResponse.builder()
                .success(false)
                .message(message)
                .code("500")
                .transactionId(transactionId)
                .processTime(LocalDateTime.now())
                .details(new HashMap<>())
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
                .details(new HashMap<>())
                .build();
    }

    /**
     * 创建失败响应（带错误代码）
     */
    public static TransactionEventResponse failure(String message, String transactionId, String errorMessage, String code) {
        return TransactionEventResponse.builder()
                .success(false)
                .message(message)
                .code(code)
                .transactionId(transactionId)
                .errorMessage(errorMessage)
                .processTime(LocalDateTime.now())
                .details(new HashMap<>())
                .build();
    }

    // ==================== 工具方法 ====================

    /**
     * 添加详情信息
     */
    public TransactionEventResponse addDetail(String key, Object value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
        return this;
    }

    /**
     * 设置响应代码
     */
    public TransactionEventResponse setCode(String code) {
        this.code = code;
        return this;
    }

    /**
     * 设置错误信息
     */
    public TransactionEventResponse setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = false;
        return this;
    }

    /**
     * 设置成功状态
     */
    public TransactionEventResponse setSuccess() {
        this.success = true;
        this.code = "200";
        return this;
    }

    /**
     * 设置失败状态
     */
    public TransactionEventResponse setFailure() {
        this.success = false;
        this.code = "500";
        return this;
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(this.success);
    }

    /**
     * 检查是否失败
     */
    public boolean isFailure() {
        return !isSuccess();
    }
}
