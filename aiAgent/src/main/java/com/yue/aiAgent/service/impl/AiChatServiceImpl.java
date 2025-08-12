//package com.yue.aiAgent.service.impl;
//
//import com.yue.aiAgent.service.AiChatService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.model.ChatResponse;
//import org.springframework.ai.chat.prompt.Prompt;
//import org.springframework.ai.chat.prompt.PromptTemplate;
//import org.springframework.ai.openai.client.OpenAiChatClient;
//import org.springframework.ai.azure.openai.client.AzureOpenAiChatClient;
//import org.springframework.ai.ollama.client.OllamaChatClient;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * AI聊天服务实现类
// *
// * @author yue
// * @version 1.0.0
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AiChatServiceImpl implements AiChatService {
//
//    private final OpenAiChatClient openAiChatClient;
//    private final AzureOpenAiChatClient azureOpenAiChatClient;
//    private final OllamaChatClient ollamaChatClient;
//
//    @Override
//    public Map<String, Object> chat(String message, String type) {
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            ChatClient chatClient = getChatClient(type);
//            if (chatClient == null) {
//                response.put("code", 400);
//                response.put("message", "不支持的AI类型: " + type);
//                return response;
//            }
//
//            PromptTemplate promptTemplate = new PromptTemplate("""
//                你是一个智能AI助手，请回答用户的问题。
//                用户问题: {message}
//                请提供准确、有用的回答。
//                """);
//
//            Prompt prompt = promptTemplate.create(Map.of("message", message));
//            ChatResponse chatResponse = chatClient.call(prompt);
//
//            String result = chatResponse.getResult().getOutput().getContent();
//
//            response.put("code", 200);
//            response.put("message", "聊天成功");
//            response.put("data", Map.of(
//                "type", type,
//                "message", message,
//                "response", result,
//                "timestamp", System.currentTimeMillis()
//            ));
//
//            log.info("AI聊天成功，类型: {}, 消息: {}", type, message);
//
//        } catch (Exception e) {
//            log.error("AI聊天失败，类型: {}, 消息: {}", type, message, e);
//            response.put("code", 500);
//            response.put("message", "聊天失败: " + e.getMessage());
//        }
//
//        return response;
//    }
//
//    @Override
//    public String streamChat(String message, String type) {
//        try {
//            ChatClient chatClient = getChatClient(type);
//            if (chatClient == null) {
//                return "不支持的AI类型: " + type;
//            }
//
//            PromptTemplate promptTemplate = new PromptTemplate("""
//                你是一个智能AI助手，请回答用户的问题。
//                用户问题: {message}
//                请提供准确、有用的回答。
//                """);
//
//            Prompt prompt = promptTemplate.create(Map.of("message", message));
//
//            // 这里可以实现流式响应
//            // 暂时返回普通响应
//            ChatResponse chatResponse = chatClient.call(prompt);
//            return chatResponse.getResult().getOutput().getContent();
//
//        } catch (Exception e) {
//            log.error("AI流式聊天失败，类型: {}, 消息: {}", type, message, e);
//            return "聊天失败: " + e.getMessage();
//        }
//    }
//
//    /**
//     * 根据类型获取对应的聊天客户端
//     */
//    private ChatClient getChatClient(String type) {
//        return switch (type.toLowerCase()) {
//            case "openai" -> openAiChatClient;
//            case "azure" -> azureOpenAiChatClient;
//            case "ollama" -> ollamaChatClient;
//            default -> null;
//        };
//    }
//}
