package com.jiangyang.dubbo.api.transaction.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事务事件响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEventResponse implements Serializable {

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
    private LocalDateTime processTime;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 详细信息
     */
    private Map<String, Object> details;
}
