package com.yue.chatAgent.service.impl;

import com.yue.chatAgent.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI聊天服务实现类
 *
 * @author yue
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    // 使用懒加载方式注入AI模型，避免启动时创建Bean失败
    private OpenAiChatModel openAiChatModel;
    private AzureOpenAiChatModel azureOpenAiChatModel;
    private OllamaChatModel ollamaChatModel;

    @Override
    public Map<String, Object> chat(String message, String type) {
        Map<String, Object> response = new HashMap<>();

        try {
            ChatClient chatClient = getChatClient(type);
            if (chatClient == null) {
                response.put("code", 400);
                response.put("message", "不支持的AI类型或AI服务未配置: " + type);
                response.put("data", Map.of(
                    "type", type,
                    "message", message,
                    "response", "请配置相应的AI服务API密钥",
                    "timestamp", System.currentTimeMillis()
                ));
                return response;
            }

            PromptTemplate promptTemplate = new PromptTemplate("""
                你是一个智能AI助手，请回答用户的问题。
                用户问题: {message}
                请提供准确、有用的回答。
                """);

            Prompt prompt = promptTemplate.create(Map.of("message", message));
            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();

            String result = String.valueOf(chatResponse.getResult().getOutput());

            response.put("code", 200);
            response.put("message", "聊天成功");
            response.put("data", Map.of(
                "type", type,
                "message", message,
                "response", result,
                "timestamp", System.currentTimeMillis()
            ));

            log.info("AI聊天成功，类型: {}, 消息: {}", type, message);

        } catch (Exception e) {
            log.error("AI聊天失败，类型: {}, 消息: {}", type, message, e);
            response.put("code", 500);
            response.put("message", "聊天失败: " + e.getMessage());
            response.put("data", Map.of(
                "type", type,
                "message", message,
                "response", "AI服务暂时不可用，请稍后重试",
                "timestamp", System.currentTimeMillis()
            ));
        }

        return response;
    }

    @Override
    public String streamChat(String message, String type) {
        try {
            ChatClient chatClient = getChatClient(type);
            if (chatClient == null) {
                return "不支持的AI类型或AI服务未配置: " + type + "。请配置相应的AI服务API密钥。";
            }

            PromptTemplate promptTemplate = new PromptTemplate("""
                你是一个智能AI助手，请回答用户的问题。
                用户问题: {message}
                请提供准确、有用的回答。
                """);

            Prompt prompt = promptTemplate.create(Map.of("message", message));

            // 这里可以实现流式响应
            // 暂时返回普通响应
            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            return String.valueOf(chatResponse.getResult().getOutput());

        } catch (Exception e) {
            log.error("AI流式聊天失败，类型: {}, 消息: {}", type, message, e);
            return "聊天失败: " + e.getMessage() + "。AI服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 根据类型获取对应的聊天客户端
     */
    private ChatClient getChatClient(String type) {
        return switch (type.toLowerCase()) {
            case "openai" -> {
                try {
                    yield openAiChatModel != null ? ChatClient.create(openAiChatModel) : null;
                } catch (Exception e) {
                    log.warn("OpenAI聊天模型未配置或初始化失败: {}", e.getMessage());
                    yield null;
                }
            }
            case "azure" -> {
                try {
                    yield azureOpenAiChatModel != null ? ChatClient.create(azureOpenAiChatModel) : null;
                } catch (Exception e) {
                    log.warn("Azure OpenAI聊天模型未配置或初始化失败: {}", e.getMessage());
                    yield null;
                }
            }
            case "ollama" -> {
                try {
                    yield ollamaChatModel != null ? ChatClient.create(ollamaChatModel) : null;
                } catch (Exception e) {
                    log.warn("Ollama聊天模型未配置或初始化失败: {}", e.getMessage());
                    yield null;
                }
            }
            default -> null;
        };
    }
}
