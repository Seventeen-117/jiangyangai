package com.jiangyang.dubbo.api.common;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 通用响应结果包装类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .success(true)
                .message("操作成功")
                .code("200")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .success(true)
                .message(message)
                .code("200")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> failure() {
        return failure("操作失败");
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> failure(String message) {
        return Result.<T>builder()
                .success(false)
                .message(message)
                .code("500")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> failure(String message, String code) {
        return Result.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
