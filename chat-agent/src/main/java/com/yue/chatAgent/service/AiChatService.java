package com.yue.chatAgent.service;

import java.util.Map;

/**
 * AI聊天服务接口
 * 
 * @author yue
 * @version 1.0.0
 */
public interface AiChatService {

    /**
     * 发送聊天消息
     * 
     * @param message 消息内容
     * @param type AI类型 (openai, azure, ollama)
     * @return 响应结果
     */
    Map<String, Object> chat(String message, String type);

    /**
     * 流式聊天
     * 
     * @param message 消息内容
     * @param type AI类型
     * @return 流式响应
     */
    String streamChat(String message, String type);
}
