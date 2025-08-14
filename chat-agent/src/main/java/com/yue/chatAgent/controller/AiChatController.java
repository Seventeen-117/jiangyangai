package com.yue.chatAgent.controller;

import com.yue.chatAgent.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * AI聊天控制器
 * 
 * @author yue
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/aiChat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * 发送聊天消息
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String type = request.getOrDefault("type", "openai");
        
        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", "消息内容不能为空");
            return response;
        }
        
        log.info("收到聊天请求，类型: {}, 消息: {}", type, message);
        return aiChatService.chat(message, type);
    }

    /**
     * 流式聊天
     */
    @PostMapping("/stream")
    public String streamChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String type = request.getOrDefault("type", "openai");
        
        if (message == null || message.trim().isEmpty()) {
            return "消息内容不能为空";
        }
        
        log.info("收到流式聊天请求，类型: {}, 消息: {}", type, message);
        return aiChatService.streamChat(message, type);
    }

    /**
     * 快速聊天（GET方式）
     */
    @GetMapping("/quick")
    public Map<String, Object> quickChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "openai") String type) {
        
        log.info("收到快速聊天请求，类型: {}, 消息: {}", type, message);
        return aiChatService.chat(message, type);
    }
}
