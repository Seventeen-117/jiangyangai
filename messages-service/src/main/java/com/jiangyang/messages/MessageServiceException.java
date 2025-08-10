package com.jiangyang.messages;

/**
 * 消息服务异常
 */
public class MessageServiceException extends RuntimeException {
    
    public MessageServiceException(String message) {
        super(message);
    }
    
    public MessageServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
